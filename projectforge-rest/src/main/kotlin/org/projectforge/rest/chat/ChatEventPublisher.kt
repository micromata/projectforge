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
import java.util.concurrent.Executors

private val log = KotlinLogging.logger {}

/**
 * Pushes the changes of conversations to the open chat pages via server-sent events (PF-CHAT NOT-06). An event only
 * carries the change sequence ([org.projectforge.business.chat.ChatConversationDO.changeSeq], also the event id): the
 * page fetches the changes since the last one it knows with the access check of every request. A heartbeat comment
 * every [HEARTBEAT_MILLIS] keeps proxies from closing the connection; after [TIMEOUT_MILLIS] the page reconnects.
 *
 * The subscriptions are the only state of this singleton (no request or user state otherwise). The read access is
 * checked again before every push: a user who lost it gets no events anymore.
 */
@Service
class ChatEventPublisher : ChatEventListener {
    @Autowired
    private lateinit var chatAccess: ChatAccess

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    private class Subscription(val emitter: SseEmitter, val conversationId: Long, val userId: Long)

    private val subscriptions = ConcurrentHashMap<Long, CopyOnWriteArraySet<Subscription>>()

    // Sends outside of the request threads of the changes: a slow client mustn't delay sending a message.
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ChatEventPublisher").also { it.isDaemon = true }
    }

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
        executor.execute { send(subscription, currentSeq) }
        return emitter
    }

    override fun onChange(conversationId: Long, changeSeq: Long) {
        val subs = subscriptions[conversationId] ?: return
        executor.execute {
            subs.forEach { subscription ->
                val user = userGroupCache.getUser(subscription.userId)
                if (user == null || chatAccess.getContext(conversationId, user) == null) {
                    subscription.emitter.complete()
                    remove(subscription)
                } else {
                    send(subscription, changeSeq)
                }
            }
        }
    }

    override fun isWatching(conversationId: Long, userId: Long): Boolean {
        return subscriptions[conversationId]?.any { it.userId == userId } == true
    }

    @Scheduled(fixedRate = HEARTBEAT_MILLIS)
    fun heartbeat() {
        if (subscriptions.isEmpty()) {
            return
        }
        executor.execute {
            subscriptions.values.flatten().forEach { subscription ->
                try {
                    subscription.emitter.send(SseEmitter.event().comment("ping"))
                } catch (_: Exception) {
                    remove(subscription)
                }
            }
        }
    }

    /** The number of open connections, for the statistics. */
    val size: Int
        get() = subscriptions.values.sumOf { it.size }

    private fun send(subscription: Subscription, changeSeq: Long) {
        try {
            subscription.emitter.send(
                SseEmitter.event().id("$changeSeq").name(EVENT_NAME).data("{\"changeSeq\":$changeSeq}")
            )
        } catch (ex: Exception) {
            log.debug { "Chat event to user #${subscription.userId} not sent (connection closed?): ${ex.message}" }
            remove(subscription)
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
        const val HEARTBEAT_MILLIS = 25_000L
        const val TIMEOUT_MILLIS = 60 * 60_000L
    }
}
