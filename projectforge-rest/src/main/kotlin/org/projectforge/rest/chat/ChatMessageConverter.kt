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

import org.projectforge.business.chat.ChatAccess
import org.projectforge.business.chat.ChatFileService
import org.projectforge.business.chat.ChatMessageDO
import org.projectforge.business.chat.ChatRoomDO
import org.projectforge.business.chat.ChatService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.FormatterUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Converts messages into what the logged-in user sees ([ChatMessage]), with the files still there in the data
 * transfer (others expired or were deleted there).
 */
@Service
class ChatMessageConverter {
    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var fileService: ChatFileService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    fun convert(context: ChatAccess.Context, messages: List<ChatMessageDO>): List<ChatMessage> {
        val rootIds = messages.filter { it.threadRootId == null && it.replyCount > 0 }.mapNotNull { it.id }
        val replyAuthors = chatService.selectReplyAuthorIds(rootIds)
        val existing = existingFileIds(messages)
        return messages.map { convert(context, it, replyAuthors[it.id].orEmpty(), existing) }
    }

    fun convert(context: ChatAccess.Context, message: ChatMessageDO): ChatMessage {
        val replyAuthors = if (message.threadRootId == null && message.replyCount > 0) {
            chatService.selectReplyAuthorIds(listOf(message.id!!))[message.id].orEmpty()
        } else emptyList()
        return convert(context, message, replyAuthors, existingFileIds(listOf(message)))
    }

    /** The files of the messages still there, asked once per area. */
    private fun existingFileIds(messages: List<ChatMessageDO>): Set<String> {
        val areaIds = messages.flatMap { message -> message.readAttachments().map { it.areaId } }
        return if (areaIds.isEmpty()) emptySet() else fileService.existingFileIds(areaIds)
    }

    private fun convert(
        context: ChatAccess.Context,
        message: ChatMessageDO,
        replyAuthorIds: List<Long>,
        existingFileIds: Set<String>,
    ): ChatMessage {
        val meId = context.user.id
        val showContent = !message.deleted && (!message.isHidden || context.isModerator)
        return ChatMessage(
            id = message.id!!,
            conversationId = message.conversationId!!,
            threadRootId = message.threadRootId,
            author = ref(message.authorId),
            created = message.created,
            editedAt = message.editedAt,
            deleted = message.deleted,
            hidden = message.isHidden,
            hiddenBy = if (message.isHidden && context.isModerator) ref(message.hiddenById).displayName else null,
            hiddenReason = if (message.isHidden && context.isModerator) message.hiddenReason else null,
            content = if (showContent) message.content else null,
            files = if (showContent) message.readAttachments().map {
                ChatFile(it.areaId, it.fileId, it.name, it.size, FormatterUtils.formatBytes(it.size), it.image, it.fileId in existingFileIds)
            } else emptyList(),
            reactions = if (message.deleted) emptyList() else message.readReactions().map { (emoji, userIds) ->
                ChatReaction(emoji, userIds.size, userIds.map { ref(it).displayName }, meId in userIds)
            },
            replyCount = message.replyCount,
            lastReplyAt = message.lastReplyAt,
            replyAuthors = replyAuthorIds.take(MAX_REPLY_AUTHORS).map { ref(it) },
            following = message.threadRootId == null && meId in chatService.threadFollowers(message),
            own = message.authorId == meId,
            mentionsMe = meId != null && meId in ChatRoomDO.toIds(message.mentionedIds),
            changeSeq = message.changeSeq,
        )
    }

    fun ref(userId: Long?): ChatRef {
        val user = userGroupCache.getUser(userId)
        return ChatRef(userId ?: 0, user?.getFullname() ?: "?")
    }

    companion object {
        private const val MAX_REPLY_AUTHORS = 5
    }
}
