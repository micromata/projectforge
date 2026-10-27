/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.core

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {}

/**
 * Wakes all open pages of a user via server-sent events, whichever page is shown (unlike the events of a chat
 * conversation, [org.projectforge.rest.chat.ChatEventPublisher]). An event carries nothing but its name, e.g.
 * [SECRET_OFFERS]: the page asks what changed, with the access check of that request. A heartbeat comment every
 * [HEARTBEAT_MILLIS] keeps proxies from closing the connection; after [TIMEOUT_MILLIS] the page reconnects.
 *
 * The subscriptions are the only state of this singleton. Each subscription is sent to on its own, on a pool of
 * [SEND_THREADS] threads: a client not reading delays only its own events. Events not yet sent are merged by name.
 */
@Service
class UserEventPublisher {
    private class Subscription(val emitter: SseEmitter, val userId: Long) {
        /** The names of the events to send. */
        val pending: MutableSet<String> = ConcurrentHashMap.newKeySet()

        /** Is a heartbeat to be sent? */
        val ping = AtomicBoolean(false)

        /** Is a task of the pool sending to this subscription (scheduled or running)? */
        val sending = AtomicBoolean(false)

        fun idle() = pending.isEmpty() && !ping.get()
    }

    private val subscriptions = ConcurrentHashMap<Long, CopyOnWriteArraySet<Subscription>>()

    private val executor = ThreadPoolExecutor(
        SEND_THREADS, SEND_THREADS, 60, TimeUnit.SECONDS, LinkedBlockingQueue(),
        object : ThreadFactory {
            private val counter = AtomicInteger()
            override fun newThread(runnable: Runnable) =
                Thread(runnable, "UserEventPublisher-${counter.incrementAndGet()}").also { it.isDaemon = true }
        },
    ).also { it.allowCoreThreadTimeOut(true) }

    /** Subscribes the user (the logged-in user of the request, by the caller). */
    fun subscribe(userId: Long): SseEmitter {
        val emitter = SseEmitter(TIMEOUT_MILLIS)
        val subscription = Subscription(emitter, userId)
        val own = subscriptions.computeIfAbsent(userId) { CopyOnWriteArraySet() }
        own.add(subscription)
        // One per open page; a client opening more (a script, a loop of reconnects) drops its oldest ones.
        while (own.size > MAX_PER_USER) {
            own.firstOrNull { it !== subscription }?.let { close(it) } ?: break
        }
        emitter.onCompletion { remove(subscription) }
        emitter.onTimeout { remove(subscription) }
        emitter.onError { remove(subscription) }
        // At once, so a proxy passes the response on and the page knows the stream is open.
        subscription.ping.set(true)
        startSending(subscription)
        return emitter
    }

    /** Sends the event (a name, no content) to all open pages of the user. */
    fun publish(userId: Long, eventName: String) {
        subscriptions[userId]?.forEach {
            it.pending.add(eventName)
            startSending(it)
        }
    }

    @Scheduled(fixedRate = HEARTBEAT_MILLIS)
    fun heartbeat() {
        subscriptions.values.forEach { set ->
            set.forEach {
                it.ping.set(true)
                startSending(it)
            }
        }
    }

    /** The number of open connections, for the statistics. */
    val size: Int
        get() = subscriptions.values.sumOf { it.size }

    private fun startSending(subscription: Subscription) {
        if (subscription.sending.compareAndSet(false, true)) {
            executor.execute { drain(subscription) }
        }
    }

    private fun drain(subscription: Subscription) {
        while (true) {
            val names = subscription.pending.toList().also { subscription.pending.removeAll(it.toSet()) }
            val ping = subscription.ping.getAndSet(false)
            if (names.isEmpty() && !ping) {
                subscription.sending.set(false)
                // Offered meanwhile, but the offer saw this task still sending: go on (if no other task started).
                if (subscription.idle() || !subscription.sending.compareAndSet(false, true)) {
                    return
                }
                continue
            }
            try {
                if (ping) {
                    subscription.emitter.send(SseEmitter.event().comment("ping"))
                }
                names.forEach { subscription.emitter.send(SseEmitter.event().name(it).data("{}")) }
            } catch (ex: Exception) {
                log.debug { "User event to user #${subscription.userId} not sent (connection closed?): ${ex.message}" }
                close(subscription)
                subscription.pending.clear()
                subscription.ping.set(false)
                subscription.sending.set(false)
                return
            }
        }
    }

    /** Ends the connection (the container may keep an async request open otherwise) and forgets the subscription. */
    private fun close(subscription: Subscription) {
        remove(subscription)
        try {
            subscription.emitter.complete()
        } catch (_: Exception) {
            // Already closed.
        }
    }

    private fun remove(subscription: Subscription) {
        subscriptions.computeIfPresent(subscription.userId) { _, set ->
            set.remove(subscription)
            set.takeIf { it.isNotEmpty() }
        }
    }

    companion object {
        /** The confidential conversations offered to the user changed. */
        const val SECRET_OFFERS = "secretOffers"
        const val HEARTBEAT_MILLIS = 25_000L
        const val TIMEOUT_MILLIS = 60 * 60_000L

        /** Open pages of a user with a stream each; more of them are not a user but a loop. */
        const val MAX_PER_USER = 20

        /** Clients sent to at the same time (a stalled one occupies a thread until the write timeout). */
        private const val SEND_THREADS = 4
    }
}
