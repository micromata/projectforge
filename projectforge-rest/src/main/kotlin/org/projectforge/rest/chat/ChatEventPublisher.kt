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

package org.projectforge.rest.chat

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.chat.ChatAccess
import org.projectforge.business.chat.ChatEventListener
import org.projectforge.business.user.UserGroupCache
import org.springframework.beans.factory.annotation.Autowired
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
import java.util.concurrent.atomic.AtomicLong

private val log = KotlinLogging.logger {}

/**
 * Pushes the changes of conversations to the open chat pages via server-sent events (PF-CHAT NOT-06). An event only
 * carries the change sequence ([org.projectforge.business.chat.ChatConversationDO.changeSeq], also the event id): the
 * page fetches the changes since the last one it knows with the access check of every request. A heartbeat comment
 * every [HEARTBEAT_MILLIS] keeps proxies from closing the connection; after [TIMEOUT_MILLIS] the page reconnects.
 *
 * The subscriptions are the only state of this singleton (no request or user state otherwise). The read access is
 * checked again before every push: a user who lost it gets no events anymore.
 *
 * Each subscription is sent to on its own (one send at a time, on a pool of [SEND_THREADS] threads): a client not
 * reading (a stalled connection blocks the send until the write timeout of the servlet container) delays only its own
 * events. Events not yet sent are merged (only the newest sequence counts, a heartbeat isn't queued behind an event).
 */
@Service
class ChatEventPublisher : ChatEventListener {
    @Autowired
    private lateinit var chatAccess: ChatAccess

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    private class Subscription(val emitter: SseEmitter, val conversationId: Long, val userId: Long) {
        /** The next thing to send: [NOTHING], [PING] or a sequence (the higher one wins). */
        val pending = AtomicLong(NOTHING)

        /** Is a confidential message of the user to be announced ([SECRET_EVENT_NAME])? */
        val secret = AtomicBoolean(false)

        /** Is a task of the pool sending to this subscription (scheduled or running)? */
        val sending = AtomicBoolean(false)

        fun idle() = pending.get() == NOTHING && !secret.get()
    }

    private val subscriptions = ConcurrentHashMap<Long, CopyOnWriteArraySet<Subscription>>()

    // Sends outside of the request threads of the changes: a slow client mustn't delay sending a message.
    private val executor = ThreadPoolExecutor(
        SEND_THREADS, SEND_THREADS, 60, TimeUnit.SECONDS, LinkedBlockingQueue(),
        object : ThreadFactory {
            private val counter = AtomicInteger()
            override fun newThread(runnable: Runnable) =
                Thread(runnable, "ChatEventPublisher-${counter.incrementAndGet()}").also { it.isDaemon = true }
        },
    ).also { it.allowCoreThreadTimeOut(true) }

    /**
     * Subscribes the user (checked by the caller: read access) to the conversation. Sends the current sequence at
     * once, so the page fetches what it missed (e.g. since the event id of a reconnect).
     */
    fun subscribe(context: ChatAccess.Context, currentSeq: Long): SseEmitter {
        val emitter = SseEmitter(TIMEOUT_MILLIS)
        val subscription = Subscription(emitter, context.conversationId, context.user.id!!)
        subscriptions.computeIfAbsent(context.conversationId) { CopyOnWriteArraySet() }.add(subscription)
        emitter.onCompletion { remove(subscription) }
        emitter.onTimeout { remove(subscription) }
        emitter.onError { remove(subscription) }
        offer(subscription, currentSeq)
        return emitter
    }

    override fun onChange(conversationId: Long, changeSeq: Long) {
        subscriptions[conversationId]?.forEach { offer(it, changeSeq) }
    }

    override fun onSecret(conversationId: Long, userId: Long) {
        subscriptions[conversationId]?.filter { it.userId == userId }?.forEach { offerSecret(it) }
    }

    override fun isWatching(conversationId: Long, userId: Long): Boolean {
        return subscriptions[conversationId]?.any { it.userId == userId } == true
    }

    @Scheduled(fixedRate = HEARTBEAT_MILLIS)
    fun heartbeat() {
        subscriptions.values.forEach { set -> set.forEach { offer(it, PING) } }
    }

    /** The number of open connections, for the statistics. */
    val size: Int
        get() = subscriptions.values.sumOf { it.size }

    /** Merges [what] into the pending send of the subscription and starts a task, if none is sending to it. */
    private fun offer(subscription: Subscription, what: Long) {
        subscription.pending.accumulateAndGet(what, ::maxOf)
        startSending(subscription)
    }

    private fun offerSecret(subscription: Subscription) {
        subscription.secret.set(true)
        startSending(subscription)
    }

    private fun startSending(subscription: Subscription) {
        if (subscription.sending.compareAndSet(false, true)) {
            executor.execute { drain(subscription) }
        }
    }

    private fun drain(subscription: Subscription) {
        while (true) {
            val what = subscription.pending.getAndSet(NOTHING)
            val secret = subscription.secret.getAndSet(false)
            if (what == NOTHING && !secret) {
                subscription.sending.set(false)
                // Offered meanwhile, but the offer saw this task still sending: go on (if no other task started).
                if (subscription.idle() || !subscription.sending.compareAndSet(false, true)) {
                    return
                }
                continue
            }
            if ((what != NOTHING && !deliver(subscription, what)) || (secret && !deliver(subscription, SECRET))) {
                subscription.pending.set(NOTHING)
                subscription.secret.set(false)
                subscription.sending.set(false)
                return
            }
        }
    }

    /** Sends the heartbeat or the event; false if the subscription is closed. */
    private fun deliver(subscription: Subscription, what: Long): Boolean {
        try {
            if (what == PING) {
                subscription.emitter.send(SseEmitter.event().comment("ping"))
                return true
            }
            val user = userGroupCache.getUser(subscription.userId)
            if (user == null || chatAccess.getContext(subscription.conversationId, user) == null) {
                subscription.emitter.complete()
                remove(subscription)
                return false
            }
            if (what == SECRET) {
                // No id: the id is the change sequence of a reconnect.
                subscription.emitter.send(SseEmitter.event().name(SECRET_EVENT_NAME).data("{}"))
            } else {
                subscription.emitter.send(
                    SseEmitter.event().id("$what").name(EVENT_NAME).data("{\"changeSeq\":$what}")
                )
            }
            return true
        } catch (ex: Exception) {
            log.debug { "Chat event to user #${subscription.userId} not sent (connection closed?): ${ex.message}" }
            remove(subscription)
            return false
        }
    }

    private fun remove(subscription: Subscription) {
        subscriptions.computeIfPresent(subscription.conversationId) { _, set ->
            set.remove(subscription)
            set.takeIf { it.isNotEmpty() }
        }
    }

    companion object {
        const val EVENT_NAME = "change"

        /** Something changed for a confidential message of the user (no content, the page asks). */
        const val SECRET_EVENT_NAME = "secret"
        const val HEARTBEAT_MILLIS = 25_000L
        const val TIMEOUT_MILLIS = 60 * 60_000L

        /** Clients sent to at the same time (a stalled one occupies a thread until the write timeout). */
        private const val SEND_THREADS = 8

        private const val NOTHING = Long.MIN_VALUE
        private const val PING = -1L
        private const val SECRET = -2L
    }
}
