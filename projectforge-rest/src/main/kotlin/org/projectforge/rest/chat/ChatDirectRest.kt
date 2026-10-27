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

import org.projectforge.business.chat.ChatDirectChatHost
import org.projectforge.business.chat.ChatDirectService
import org.projectforge.business.chat.ChatService
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The direct messages (PF-CHAT-DM) of the logged-in user for the chat page of projectforge-next: the list and opening
 * one with another user ([ChatDirectService]: only with users the logged-in user may see).
 */
@RestController
@RequestMapping("${Rest.URL}/chatDirect")
class ChatDirectRest {
    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var converter: ChatMessageConverter

    @Autowired
    private lateinit var directHost: ChatDirectChatHost

    @Autowired
    private lateinit var directService: ChatDirectService

    /** The direct messages of the logged-in user (with at least one message), the newest first. */
    @AccessChecked("ChatDirectService.list: the pairs of the logged-in user only")
    @GetMapping("list")
    fun list(): List<ChatDirectEntry> {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val entries = directService.list()
        val unread = chatService.countUnread(entries.mapNotNull { it.conversation.id }, user.id!!)
        return entries.map { entry ->
            val conversationId = entry.conversation.id!!
            ChatDirectEntry(
                conversationId = conversationId,
                partner = converter.ref(entry.partnerId),
                unread = unread[conversationId] ?: 0,
                lastMessageAt = entry.conversation.lastMessageAt,
                canWrite = directHost.hasWriteAccess(user, entry.pair.id!!, ChatDirectChatHost.CHANNEL),
            )
        }.sortedWith(compareByDescending<ChatDirectEntry> { it.lastMessageAt }.thenBy { it.partner.displayName.lowercase() })
    }

    /** The conversation with the user, created if missing. */
    @AccessChecked("ChatDirectService.open: the other user must be visible to the logged-in user (user selection), not the user self")
    @PostMapping("open")
    fun open(@RequestBody request: ChatDirectOpenRequest): ChatDirectOpenResult {
        val userId = request.userId ?: throw AccessException(ThreadLocalUserContext.requiredLoggedInUser, NO_ACCESS)
        return ChatDirectOpenResult(directService.open(userId).conversationId)
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

    companion object {
        private const val NO_ACCESS = "access.exception.noAccess"
    }
}
