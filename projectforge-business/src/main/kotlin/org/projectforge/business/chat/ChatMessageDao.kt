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

import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * The messages of the conversations, used by [ChatService] (for the history of edits, deletion and hiding). The access
 * comes from the conversation of the message stored in the database ([ChatAccess]): readers of the conversation read
 * its messages, writers write own messages, only the author edits and deletes a message (hiding by a moderator is
 * checked by [ChatService]), moderators see the history.
 */
@Service
open class ChatMessageDao : BaseDao<ChatMessageDO>(ChatMessageDO::class.java) {
    @Autowired
    private lateinit var chatAccess: ChatAccess

    override fun newInstance(): ChatMessageDO {
        return ChatMessageDO()
    }

    /** Lists: every message is checked on its own by [hasAccess]. */
    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    /** Checked per message by [hasAccess] (write access to its conversation). */
    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return user.id != null
    }

    override fun hasAccess(
        user: PFUserDO,
        obj: ChatMessageDO?,
        oldObj: ChatMessageDO?,
        operationType: OperationType,
        throwException: Boolean,
    ): Boolean {
        val userId = user.id
        val result = when {
            userId == null -> false
            obj == null -> operationType == OperationType.SELECT || operationType == OperationType.INSERT
            operationType == OperationType.SELECT -> chatAccess.getContext(obj.conversationId, user) != null
            operationType == OperationType.INSERT && obj.id == null ->
                obj.authorId == userId && chatAccess.getContext(obj.conversationId, user)?.canWrite == true
            // Decided by the stored message, never by the posted one: own messages only, not moved elsewhere.
            else -> {
                val dbObj = oldObj ?: obj.id?.let { find(it, checkAccess = false) }
                dbObj != null && dbObj.authorId == userId && obj.authorId == dbObj.authorId &&
                        obj.conversationId == dbObj.conversationId && obj.threadRootId == dbObj.threadRootId &&
                        !dbObj.isHidden && chatAccess.getContext(dbObj.conversationId, user)?.canWrite == true
            }
        }
        if (!result && throwException) {
            throw AccessException(AccessException.I18N_KEY_STANDARD, "ChatMessageDO", operationType)
        }
        return result
    }

    override fun hasHistoryAccess(user: PFUserDO, obj: ChatMessageDO, throwException: Boolean): Boolean {
        val result = chatAccess.getContext(obj.conversationId, user)?.isModerator == true
        if (!result && throwException) {
            throw AccessException(AccessException.I18N_KEY_STANDARD, "ChatMessageDO", OperationType.SELECT)
        }
        return result
    }

    override fun onUpdate(obj: ChatMessageDO, dbObj: ChatMessageDO) {
        // Never moved to another conversation, thread or author (SEC-02).
        obj.conversationId = dbObj.conversationId
        obj.authorId = dbObj.authorId
        obj.threadRootId = dbObj.threadRootId
    }
}
