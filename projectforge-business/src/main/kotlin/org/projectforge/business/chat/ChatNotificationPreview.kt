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

import org.apache.commons.lang3.StringUtils
import org.projectforge.business.notification.NotificationDO
import org.projectforge.business.notification.NotificationPreview
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.menu.builder.MenuItemDefId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * The notifications of a chat message ([ChatNotifier]) show its beginning in the app instead of repeating their
 * title. Only for the recipient who may still read the conversation, not for a deleted or hidden message, nor for a
 * host keeping its messages out of the search across conversations (confidential). The author is named first where
 * the title doesn't name them (new messages and replies in a room).
 */
@Service
class ChatNotificationPreview : NotificationPreview {
    @Autowired
    private lateinit var chatAccess: ChatAccess

    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    override fun preview(notification: NotificationDO, recipient: PFUserDO): String? {
        if (notification.menuBadge != MenuItemDefId.CHAT.id || notification.recipientId != recipient.id) {
            return null
        }
        val match = LINK_REGEX.matchEntire(notification.link ?: return null) ?: return null
        val conversationId = match.groupValues[1].toLong()
        val message = chatService.findMessage(match.groupValues[2].toLong()) ?: return null
        if (message.conversationId != conversationId || message.deleted || message.hiddenAt != null) {
            return null
        }
        val context = chatAccess.getContext(conversationId, recipient) ?: return null
        if (!context.host.searchableAcrossConversations) {
            return null
        }
        val text = message.contentText?.replace(WHITESPACE, " ")?.trim()
        if (text.isNullOrEmpty()) {
            return null
        }
        val teaser = StringUtils.abbreviate(text, MAX_LENGTH)
        val titleNamesAuthor = context.conversation.hostType == ChatDirectChatHost.HOST_TYPE ||
                notification.dedupKey?.startsWith("chat:mention:") == true
        if (titleNamesAuthor) {
            return teaser
        }
        val author = userGroupCache.getUser(message.authorId)?.getFullname() ?: return teaser
        return "$author: $teaser"
    }

    companion object {
        /** The link of [ChatNotifier.notify]. */
        private val LINK_REGEX = Regex("""/chat/(\d+)\?message=(\d+)""")

        private val WHITESPACE = Regex("""\s+""")

        internal const val MAX_LENGTH = 140
    }
}
