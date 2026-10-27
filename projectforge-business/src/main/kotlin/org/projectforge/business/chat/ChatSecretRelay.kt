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

package org.projectforge.business.chat

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * A confidential conversation of a direct chat (like an OTR session): end-to-end encrypted by the two browsers, the
 * server only relays the public keys and the ciphertext. The keys are made by the browsers (WebCrypto, ECDH P-256),
 * the private ones never leave them; this relay only sees what it can't decrypt.
 *
 * Nothing is written anywhere: no database, no log, no chat history. The sessions live in memory only (single node,
 * as the subscriptions of the server-sent events). A message is deleted when the other side took it; the session is
 * deleted when either side ends it, after [IDLE_MILLIS] without a message, after [MAX_MILLIS] at the latest, or when a
 * side isn't seen anymore for [PRESENCE_MILLIS] (page closed or reloaded: its key is gone with it).
 *
 * The flow: the initiator offers with its public key ([offer], recipient: the partner of the direct chat from the
 * database), the recipient accepts with its own ([accept]); then both sides [send] and [take] messages until one ends
 * the session ([cancel]). Each step is allowed to its side only and in its state only.
 *
 * The fingerprint the browsers show covers both public keys: a manipulated server can neither read nor forge a
 * message without the two sides seeing different fingerprints (if they compare them).
 */
@Service
open class ChatSecretRelay {
    @Autowired
    private lateinit var chatAccess: ChatAccess

    @Autowired
    private lateinit var chatNotifier: ChatNotifier

    @Autowired
    private lateinit var directHost: ChatDirectChatHost

    /** Internal: replaced by tests. */
    @Autowired(required = false)
    internal var eventListeners: List<ChatEventListener> = emptyList()

    /** [OFFERED]: waiting for the recipient's key. [OPEN]: both keys known, messages can be sent. */
    enum class State { OFFERED, OPEN }

    /** One encrypted message (iv and ciphertext base64url); [counter] rises per side, it is part of the AES-GCM data. */
    class Message(val counter: Long, val iv: String, val ciphertext: String)

    class Session internal constructor(
        val id: String,
        val conversationId: Long,
        val initiatorId: Long,
        val recipientId: Long,
        /** The public key of the initiator (raw, base64url). */
        val initiatorKey: String,
        internal val created: Long,
    ) {
        @Volatile
        var state: State = State.OFFERED
            internal set

        /** The public key of the recipient (raw, base64url), known after [accept]. */
        @Volatile
        var recipientKey: String? = null
            internal set

        @Volatile
        internal var lastActivity: Long = created

        @Volatile
        internal var initiatorSeen: Long = created

        @Volatile
        internal var recipientSeen: Long = created

        internal val toInitiator = ArrayDeque<Message>()
        internal val toRecipient = ArrayDeque<Message>()

        /** The highest counter sent by the side. */
        internal var initiatorSent = 0L
        internal var recipientSent = 0L

        /** The highest counter of the messages sent by the side the other one has taken. */
        internal var initiatorRead = 0L
        internal var recipientRead = 0L

        fun isInitiator(userId: Long?) = userId == initiatorId

        /** Messages waiting for the user. */
        fun waiting(userId: Long?): Int = synchronized(this) { (if (isInitiator(userId)) toInitiator else toRecipient).size }

        /** The highest counter of the user's messages the partner has taken. */
        fun read(userId: Long?): Long = synchronized(this) { if (isInitiator(userId)) initiatorRead else recipientRead }

        /** The highest counter the user has sent (accepted by the relay). */
        fun sent(userId: Long?): Long = synchronized(this) { if (isInitiator(userId)) initiatorSent else recipientSent }

        /** The end of the session if nothing more is sent. */
        val expiresAt: Long
            get() = minOf(created + MAX_MILLIS, lastActivity + IDLE_MILLIS)
    }

    private val sessions = ConcurrentHashMap<String, Session>()

    private val random = SecureRandom()

    /** The time in millis (replaced by tests). */
    internal var clock: () -> Long = System::currentTimeMillis

    /** The initiator offers the partner of the direct chat a confidential conversation. */
    open fun offer(conversationId: Long?, publicKey: String?): Session {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val context = chatAccess.requireWriteContext(conversationId, user)
        if (context.host.hostType != ChatDirectChatHost.HOST_TYPE) {
            throw AccessException(user, NO_ACCESS)
        }
        val recipientId = directHost.partnerId(context.hostId, user.id) ?: throw AccessException(user, NO_ACCESS)
        checkPublicKey(publicKey, user)
        purge()
        val open = sessions.values.count { it.initiatorId == user.id }
        if (sessions.size >= MAX_SESSIONS || open >= MAX_SESSIONS_PER_USER) {
            throw UserException(ERROR_TOO_MANY)
        }
        val session = Session(newId(), context.conversationId, user.id!!, recipientId, publicKey!!, clock())
        sessions[session.id] = session
        wake(session, recipientId)
        wakeOffers(recipientId)
        chatNotifier.onSecretOffer(session.conversationId, session.initiatorId, recipientId, session.created)
        return session
    }

    /**
     * The session, if the logged-in user is one of its two sides and it is still open; null otherwise. Asking is the
     * sign that the side is still there.
     */
    open fun get(id: String?): Session? {
        val session = sessions[id ?: return null]?.takeIf { !expired(it) } ?: return null
        val userId = ThreadLocalUserContext.requiredLoggedInUserId
        if (userId != session.initiatorId && userId != session.recipientId) {
            return null
        }
        if (session.isInitiator(userId)) session.initiatorSeen = clock() else session.recipientSeen = clock()
        return session
    }

    /** The sessions of the conversation the logged-in user is the recipient of (to accept, or accepted elsewhere). */
    open fun pending(conversationId: Long?): List<Session> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val context = chatAccess.requireContext(conversationId, user)
        return sessions.values.filter {
            it.conversationId == context.conversationId && it.recipientId == user.id && !expired(it)
        }.sortedBy { it.created }
    }

    /**
     * The offers to the logged-in user waiting for an answer, in all of the user's direct chats: to draw the user's
     * attention wherever the app is open (sound, tab title, browser notification), not only in the chat.
     */
    open fun offers(): List<Session> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        return sessions.values.filter {
            it.recipientId == user.id && it.state == State.OFFERED && !expired(it) &&
                    chatAccess.getContext(it.conversationId, user) != null
        }.sortedBy { it.created }
    }

    /**
     * The recipient sees the offers of the conversation in the open chat (where they are accepted): their
     * notification is resolved, it would only point to where the user already is.
     */
    open fun seen(conversationId: Long?) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val context = chatAccess.requireContext(conversationId, user)
        chatNotifier.resolveSecret(context.conversationId, user.id!!)
    }

    /** The recipient accepts with its public key. */
    open fun accept(id: String?, publicKey: String?): Session {
        val (session, user) = requireSide(id, initiator = false)
        checkPublicKey(publicKey, user)
        synchronized(session) {
            checkState(session, State.OFFERED)
            session.recipientKey = publicKey
            session.state = State.OPEN
            session.lastActivity = clock()
        }
        wake(session, session.initiatorId)
        wakeOffers(session.recipientId)
        resolveNotification(session)
        return session
    }

    /** Either side sends a message, encrypted for the key of the other one. */
    open fun send(id: String?, message: Message): Session {
        val (session, user) = requireSide(id, initiator = null)
        chatAccess.requireWriteContext(session.conversationId, user)
        if (decodedSize(message.iv) != IV_BYTES) {
            throw UserException(ERROR_INVALID)
        }
        val size = decodedSize(message.ciphertext)
        if (size == null || size <= TAG_BYTES) {
            throw UserException(ERROR_INVALID)
        }
        if (size > MAX_CIPHERTEXT_BYTES) {
            throw UserException(ERROR_TOO_LARGE)
        }
        val initiator = session.isInitiator(user.id)
        synchronized(session) {
            checkState(session, State.OPEN)
            val sent = if (initiator) session.initiatorSent else session.recipientSent
            if (message.counter <= sent) {
                throw UserException(ERROR_INVALID)
            }
            val queue = if (initiator) session.toRecipient else session.toInitiator
            if (queue.size >= MAX_QUEUED) {
                throw UserException(ERROR_QUEUE_FULL)
            }
            queue.addLast(message)
            if (initiator) session.initiatorSent = message.counter else session.recipientSent = message.counter
            session.lastActivity = clock()
        }
        wake(session, if (initiator) session.recipientId else session.initiatorId)
        return session
    }

    /**
     * Either side takes the messages waiting for it: they are deleted with it, the other side learns they were taken.
     */
    open fun take(id: String?): List<Message> {
        val (session, user) = requireSide(id, initiator = null)
        chatAccess.requireContext(session.conversationId, user)
        val initiator = session.isInitiator(user.id)
        val messages = synchronized(session) {
            checkState(session, State.OPEN)
            val queue = if (initiator) session.toInitiator else session.toRecipient
            queue.toList().also {
                queue.clear()
                val last = it.lastOrNull()?.counter
                if (last != null) {
                    // The partner's messages were taken: its "read" mark.
                    if (initiator) session.recipientRead = last else session.initiatorRead = last
                    session.lastActivity = clock()
                }
            }
        }
        if (messages.isNotEmpty()) {
            wake(session, if (initiator) session.recipientId else session.initiatorId)
        }
        return messages
    }

    /** Either side ends the session. */
    open fun cancel(id: String?) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val session = get(id) ?: return
        sessions.remove(session.id)
        wake(session, if (user.id == session.initiatorId) session.recipientId else session.initiatorId)
        if (session.state == State.OFFERED) {
            wakeOffers(session.recipientId)
        }
        resolveNotification(session)
    }

    @Scheduled(fixedRate = 60_000L)
    open fun purge() {
        val expired = sessions.values.filter { expired(it) }
        expired.forEach { sessions.remove(it.id) }
        expired.forEach { resolveNotification(it) }
        expired.filter { it.state == State.OFFERED }.map { it.recipientId }.distinct().forEach { wakeOffers(it) }
    }

    /** The number of open sessions (tests, statistics). */
    open val size: Int
        get() = sessions.size

    /** [initiator]: true the initiator, false the recipient, null either of them. */
    private fun requireSide(id: String?, initiator: Boolean?): Pair<Session, PFUserDO> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val session = get(id) ?: throw AccessException(user, NO_ACCESS)
        if (initiator != null && session.isInitiator(user.id) != initiator) {
            throw AccessException(user, NO_ACCESS)
        }
        return Pair(session, user)
    }

    private fun checkState(session: Session, expected: State) {
        if (session.state != expected) {
            throw UserException(ERROR_STATE)
        }
    }

    /** A public key of P-256, uncompressed (raw export of WebCrypto: 0x04, x, y). */
    private fun checkPublicKey(key: String?, user: PFUserDO) {
        val bytes = decode(key)
        if (bytes == null || bytes.size != PUBLIC_KEY_BYTES || bytes[0] != 4.toByte()) {
            log.debug { "Invalid public key of a confidential chat by user #${user.id}." }
            throw UserException(ERROR_INVALID)
        }
    }

    private fun decode(value: String?): ByteArray? {
        value ?: return null
        if (value.length > MAX_CIPHERTEXT_BYTES * 2) {
            return null
        }
        return try {
            Base64.getUrlDecoder().decode(value)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun decodedSize(value: String?): Int? = decode(value)?.size

    private fun expired(session: Session): Boolean {
        val now = clock()
        return now > session.created + MAX_MILLIS || now > session.lastActivity + IDLE_MILLIS ||
                now - session.initiatorSeen > PRESENCE_MILLIS ||
                (session.state == State.OPEN && now - session.recipientSeen > PRESENCE_MILLIS)
    }

    private fun newId(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /** The notification about offers to the recipient is resolved once none of the conversation is open anymore. */
    private fun resolveNotification(session: Session) {
        val offered = sessions.values.any {
            it.conversationId == session.conversationId && it.recipientId == session.recipientId &&
                    it.state == State.OFFERED && !expired(it)
        }
        if (!offered) {
            chatNotifier.resolveSecret(session.conversationId, session.recipientId)
        }
    }

    /** Wakes the open chat of the other side (the event carries nothing but that something changed). */
    private fun wake(session: Session, userId: Long) {
        eventListeners.forEach { listener ->
            try {
                listener.onSecret(session.conversationId, userId)
            } catch (ex: Exception) {
                log.warn(ex) { "Chat event listener failed for conversation #${session.conversationId}: ${ex.message}" }
            }
        }
    }

    /** Wakes all open pages of the user: the offers to the user changed (the event carries nothing either). */
    private fun wakeOffers(userId: Long) {
        eventListeners.forEach { listener ->
            try {
                listener.onSecretOffers(userId)
            } catch (ex: Exception) {
                log.warn(ex) { "Chat event listener failed for the offers to user #$userId: ${ex.message}" }
            }
        }
    }

    companion object {
        /** A session ends after this long without a message. */
        const val IDLE_MILLIS = 10 * 60_000L

        /** A session ends after this long, however active. */
        const val MAX_MILLIS = 2 * 60 * 60_000L

        /** A side not asking for this long is gone (closed page, reload); tolerates throttled background tabs. */
        const val PRESENCE_MILLIS = 2 * 60_000L
        const val MAX_CIPHERTEXT_BYTES = 16 * 1024
        const val MAX_QUEUED = 20
        const val ERROR_INVALID = "chat.secret.error.invalid"
        const val ERROR_STATE = "chat.secret.error.state"
        const val ERROR_TOO_LARGE = "chat.secret.error.tooLarge"
        const val ERROR_TOO_MANY = "chat.secret.error.tooMany"
        const val ERROR_QUEUE_FULL = "chat.secret.error.queueFull"

        private const val PUBLIC_KEY_BYTES = 65
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private const val MAX_SESSIONS = 1000
        private const val MAX_SESSIONS_PER_USER = 5
        private const val NO_ACCESS = "access.exception.noAccess"
    }
}
