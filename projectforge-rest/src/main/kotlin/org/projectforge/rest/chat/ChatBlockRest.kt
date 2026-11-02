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

import org.projectforge.business.chat.ChatBlockService
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.pub.next.RestError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The users the logged-in user has blocked in the chat (PF-CHAT-DM-07): list, block, unblock. Always for the
 * logged-in user, nothing else can be changed through it.
 */
@RestController
@RequestMapping("${Rest.URL}/chatBlock")
class ChatBlockRest {
    @Autowired
    private lateinit var blockService: ChatBlockService

    @Autowired
    private lateinit var converter: ChatMessageConverter

    @AccessChecked("ChatBlockService.list: the blocks of the logged-in user only")
    @GetMapping
    fun list(): List<ChatRef> = blockService.list().map { converter.ref(it) }

    @AccessChecked("ChatBlockService.block: blocks for the logged-in user only")
    @PostMapping("{userId}")
    fun block(@PathVariable userId: Long): ResponseEntity<Unit> {
        blockService.block(userId)
        return ResponseEntity.noContent().build()
    }

    @AccessChecked("ChatBlockService.unblock: removes a block of the logged-in user only")
    @DeleteMapping("{userId}")
    fun unblock(@PathVariable userId: Long): ResponseEntity<Unit> {
        blockService.unblock(userId)
        return ResponseEntity.noContent().build()
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
