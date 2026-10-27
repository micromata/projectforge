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
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/** The unread messages of the logged-in user in all their rooms and direct messages, for the badge of the menu. */
@Service
open class ChatUnreadCounter {
    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var chatRoomDao: ChatRoomDao

    @Autowired
    private lateinit var chatDirectService: ChatDirectService

    /** 0 for nobody logged in, or if the counting fails (a menu must not break because of a badge). */
    open fun countUnread(): Int {
        val userId = ThreadLocalUserContext.loggedInUserId ?: return 0
        return try {
            val roomIds = chatRoomDao.selectAllNotDeleted().mapNotNull { it.id }
            val roomConversations = chatService.findConversations(ChatRoomChatHost.HOST_TYPE, roomIds)
            val directConversations = chatDirectService.list().mapNotNull { it.conversation.id }
            val conversationIds = roomConversations.values.mapNotNull { it.id } + directConversations
            chatService.countUnread(conversationIds, userId).values.sum().toInt()
        } catch (ex: Exception) {
            log.warn { "Can't count the unread chat messages: ${ex.message}" }
            0
        }
    }
}
