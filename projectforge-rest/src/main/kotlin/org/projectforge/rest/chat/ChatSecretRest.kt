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

import org.projectforge.business.chat.ChatSecretRelay
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.pub.next.RestError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Date

/**
 * Confidential conversations of direct chats ([ChatSecretRelay]): end-to-end encrypted by the browsers, relayed in
 * memory only. Only the two sides of a session see it, each step only its side; the recipient is the partner of the
 * direct chat from the database. Nothing of it is logged.
 */
@RestController
@RequestMapping("${Rest.URL}/chatSecret")
class ChatSecretRest {
    @Autowired
    private lateinit var converter: ChatMessageConverter

    @Autowired
    private lateinit var relay: ChatSecretRelay

    @AccessChecked("ChatSecretRelay.offer: write access to the conversation, a direct chat only; recipient is its partner from the database")
    @PostMapping("offer")
    fun offer(@RequestBody request: ChatSecretOfferRequest): ChatSecretSession {
        return toDto(relay.offer(request.conversationId, request.publicKey))
    }

    @AccessChecked("ChatSecretRelay.pending: read access to the conversation, sessions of the logged-in user as recipient only")
    @GetMapping("pending")
    fun pending(@RequestParam conversationId: Long): List<ChatSecretSession> {
        return relay.pending(conversationId).map { toDto(it) }
    }

    /** The offers are shown in the open chat: resolves their notification. POST, as it changes the state. */
    @AccessChecked("ChatSecretRelay.seen: read access to the conversation, notifications of the logged-in user only")
    @PostMapping("seen")
    fun seen(@RequestBody request: ChatSecretOfferRequest): ResponseEntity<Void> {
        relay.seen(request.conversationId)
        return ResponseEntity.noContent().build()
    }

    @AccessChecked("ChatSecretRelay.get: the sender or the recipient of the session only")
    @GetMapping("{id}")
    fun get(@PathVariable id: String): ResponseEntity<ChatSecretSession> {
        val session = relay.get(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toDto(session))
    }

    @AccessChecked("ChatSecretRelay.accept: the recipient of the session only")
    @PostMapping("{id}/accept")
    fun accept(@PathVariable id: String, @RequestBody request: ChatSecretAcceptRequest): ChatSecretSession {
        return toDto(relay.accept(id, request.publicKey))
    }

    @AccessChecked("ChatSecretRelay.send: a side of the session only, write access to the conversation")
    @PostMapping("{id}/send")
    fun send(@PathVariable id: String, @RequestBody request: ChatSecretMessage): ChatSecretSession {
        val message = ChatSecretRelay.Message(
            counter = request.counter ?: throw UserException(ChatSecretRelay.ERROR_INVALID),
            iv = request.iv ?: throw UserException(ChatSecretRelay.ERROR_INVALID),
            ciphertext = request.ciphertext ?: throw UserException(ChatSecretRelay.ERROR_INVALID),
        )
        return toDto(relay.send(id, message))
    }

    /** The messages waiting for the logged-in user, only once: they are deleted with it. POST, as it changes the state. */
    @AccessChecked("ChatSecretRelay.take: a side of the session only, read access to the conversation")
    @PostMapping("{id}/take")
    fun take(@PathVariable id: String): List<ChatSecretMessage> {
        return relay.take(id).map { ChatSecretMessage(counter = it.counter, iv = it.iv, ciphertext = it.ciphertext) }
    }

    @AccessChecked("ChatSecretRelay.cancel: the sender or the recipient of the session only")
    @PostMapping("{id}/cancel")
    fun cancel(@PathVariable id: String): ResponseEntity<Void> {
        relay.cancel(id)
        return ResponseEntity.noContent().build()
    }

    private fun toDto(session: ChatSecretRelay.Session): ChatSecretSession {
        val userId = ThreadLocalUserContext.requiredLoggedInUserId
        return ChatSecretSession(
            id = session.id,
            conversationId = session.conversationId,
            state = session.state.name,
            initiator = converter.ref(session.initiatorId),
            recipient = converter.ref(session.recipientId),
            mine = session.isInitiator(userId),
            initiatorKey = session.initiatorKey,
            recipientKey = session.recipientKey,
            waiting = session.waiting(userId),
            readCounter = session.read(userId),
            sentCounter = session.sent(userId),
            expiresAt = Date(session.expiresAt),
        )
    }

    @ExceptionHandler(AccessException::class)
    fun handleAccessException(ex: AccessException): ResponseEntity<RestError> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(RestError(status = HttpStatus.FORBIDDEN.value(), message = translateMsg(ex)))
    }

    @ExceptionHandler(UserException::class)
    fun handleUserException(ex: UserException): ResponseEntity<ChatErrorResult> {
        return ResponseEntity.badRequest().body(ChatErrorResult(translateMsg(ex)))
    }
}
