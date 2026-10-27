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
 * Confidential messages of a direct chat (like an OTR session): end-to-end encrypted by the two browsers, the server
 * only relays the public keys and the ciphertext. The keys are made by the browsers (WebCrypto, ECDH P-256), the
 * private ones never leave them; this relay only sees what it can't decrypt.
 *
 * Nothing is written anywhere: no database, no log, no chat history. The sessions live in memory only (single node,
 * as the subscriptions of the server-sent events), are deleted when the recipient took the ciphertext, when cancelled
 * or after [TTL_MILLIS].
 *
 * The flow: the sender offers ([offer], recipient: the partner of the direct chat from the database), the recipient
 * accepts with its public key ([accept]), the sender delivers the ciphertext with its own public key ([deliver]), the
 * recipient takes it once ([take]). Each step is allowed to its side only and in its state only.
 *
 * The fingerprint the browsers show covers the recipient's key, i.e. the confidentiality: a manipulated server can't
 * read the message. The sender's key isn't authenticated, so such a server could replace the message (forge it).
 */
@Service
open class ChatSecretRelay {
    @Autowired
    private lateinit var chatAccess: ChatAccess

    @Autowired
    private lateinit var chatNotifier: ChatNotifier

    @Autowired
    private lateinit var directHost: ChatDirectChatHost

    @Autowired(required = false)
    private var eventListeners: List<ChatEventListener> = emptyList()

    /** [TAKEN]: the recipient has the ciphertext (deleted here), kept until expiry so the sender learns it. */
    enum class State { OFFERED, ACCEPTED, DELIVERED, TAKEN }

    /** The ciphertext with what the recipient needs to decrypt it (all base64url). */
    class Payload(val senderKey: String, val iv: String, val ciphertext: String)

    class Session internal constructor(
        val id: String,
        val conversationId: Long,
        val senderId: Long,
        val recipientId: Long,
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
        internal var payload: Payload? = null

        val expiresAt: Long
            get() = created + TTL_MILLIS
    }

    private val sessions = ConcurrentHashMap<String, Session>()

    private val random = SecureRandom()

    /** The time in millis (replaced by tests). */
    internal var clock: () -> Long = System::currentTimeMillis

    /** The sender offers the partner of the direct chat a confidential message. */
    open fun offer(conversationId: Long?): Session {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val context = chatAccess.requireWriteContext(conversationId, user)
        if (context.host.hostType != ChatDirectChatHost.HOST_TYPE) {
            throw AccessException(user, NO_ACCESS)
        }
        val recipientId = directHost.partnerId(context.hostId, user.id) ?: throw AccessException(user, NO_ACCESS)
        purge()
        val open = sessions.values.count { it.senderId == user.id && it.state != State.TAKEN }
        if (sessions.size >= MAX_SESSIONS || open >= MAX_SESSIONS_PER_USER) {
            throw UserException(ERROR_TOO_MANY)
        }
        val session = Session(newId(), context.conversationId, user.id!!, recipientId, clock())
        sessions[session.id] = session
        wake(session, recipientId)
        chatNotifier.onSecretOffer(session.conversationId, session.senderId, recipientId, session.created)
        return session
    }

    /** The session, if the logged-in user is one of its two sides and it is still open; null otherwise. */
    open fun get(id: String?): Session? {
        val session = sessions[id ?: return null]?.takeIf { !expired(it) } ?: return null
        val userId = ThreadLocalUserContext.requiredLoggedInUserId
        return session.takeIf { userId == it.senderId || userId == it.recipientId }
    }

    /** The sessions of the conversation the logged-in user is the recipient of (to accept or to take). */
    open fun pending(conversationId: Long?): List<Session> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val context = chatAccess.requireContext(conversationId, user)
        return sessions.values.filter {
            it.conversationId == context.conversationId && it.recipientId == user.id && it.state != State.TAKEN &&
                    !expired(it)
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
        val (session, user) = requireSide(id, recipient = true)
        checkPublicKey(publicKey, user)
        synchronized(session) {
            checkState(session, State.OFFERED)
            session.recipientKey = publicKey
            session.state = State.ACCEPTED
        }
        wake(session, session.senderId)
        resolveNotification(session)
        return session
    }

    /** The sender delivers the ciphertext, encrypted for the public key of the recipient. */
    open fun deliver(id: String?, payload: Payload): Session {
        val (session, user) = requireSide(id, recipient = false)
        chatAccess.requireWriteContext(session.conversationId, user)
        checkPublicKey(payload.senderKey, user)
        if (decodedSize(payload.iv) != IV_BYTES) {
            throw UserException(ERROR_INVALID)
        }
        val size = decodedSize(payload.ciphertext)
        if (size == null || size <= TAG_BYTES) {
            throw UserException(ERROR_INVALID)
        }
        if (size > MAX_CIPHERTEXT_BYTES) {
            throw UserException(ERROR_TOO_LARGE)
        }
        synchronized(session) {
            checkState(session, State.ACCEPTED)
            session.payload = payload
            session.state = State.DELIVERED
        }
        wake(session, session.recipientId)
        return session
    }

    /**
     * The recipient takes the ciphertext: only once, it is deleted with it. The session stays (state [State.TAKEN],
     * without anything of the message) until it expires, so the sender can tell "opened" from "ended".
     */
    open fun take(id: String?): Payload {
        val (session, user) = requireSide(id, recipient = true)
        chatAccess.requireContext(session.conversationId, user)
        val payload = synchronized(session) {
            checkState(session, State.DELIVERED)
            session.payload.also {
                session.payload = null
                session.state = State.TAKEN
            }
        } ?: throw AccessException(user, NO_ACCESS)
        wake(session, session.senderId)
        return payload
    }

    /** Either side ends the session. */
    open fun cancel(id: String?) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val session = get(id) ?: return
        sessions.remove(session.id)
        wake(session, if (user.id == session.senderId) session.recipientId else session.senderId)
        resolveNotification(session)
    }

    @Scheduled(fixedRate = 60_000L)
    open fun purge() {
        val expired = sessions.values.filter { expired(it) }
        expired.forEach { sessions.remove(it.id) }
        expired.forEach { resolveNotification(it) }
    }

    /** The number of open sessions (tests, statistics). */
    open val size: Int
        get() = sessions.size

    private fun requireSide(id: String?, recipient: Boolean): Pair<Session, PFUserDO> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val session = get(id) ?: throw AccessException(user, NO_ACCESS)
        if (user.id != if (recipient) session.recipientId else session.senderId) {
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
            log.debug { "Invalid public key of a confidential chat message by user #${user.id}." }
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

    private fun expired(session: Session): Boolean = clock() > session.expiresAt

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

    companion object {
        const val TTL_MILLIS = 10 * 60_000L
        const val MAX_CIPHERTEXT_BYTES = 16 * 1024
        const val ERROR_INVALID = "chat.secret.error.invalid"
        const val ERROR_STATE = "chat.secret.error.state"
        const val ERROR_TOO_LARGE = "chat.secret.error.tooLarge"
        const val ERROR_TOO_MANY = "chat.secret.error.tooMany"

        private const val PUBLIC_KEY_BYTES = 65
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private const val MAX_SESSIONS = 1000
        private const val MAX_SESSIONS_PER_USER = 5
        private const val NO_ACCESS = "access.exception.noAccess"
    }
}
