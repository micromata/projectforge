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
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * The access to a conversation, taken from its host ([ChatHost]) for the conversation stored in the database.
 */
@Service
open class ChatAccess {
    @Autowired
    private lateinit var hostRegistry: ChatHostRegistry

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /** A conversation with its host and the rights of a user. */
    class Context(
        val conversation: ChatConversationDO,
        val host: ChatHost,
        val user: PFUserDO,
        /** The title of the host object. */
        val title: String,
        val canWrite: Boolean,
        val isModerator: Boolean,
    ) {
        val conversationId: Long
            get() = conversation.id!!
        val hostId: Long
            get() = conversation.hostId!!
        val channel: String
            get() = conversation.channel!!
    }

    open fun findConversation(id: Long?): ChatConversationDO? {
        id ?: return null
        return persistenceService.find(ChatConversationDO::class.java, id)
    }

    open fun findConversation(hostType: String, hostId: Long, channel: String): ChatConversationDO? {
        return persistenceService.selectSingleResult(
            "from ChatConversationDO c where c.hostType = :hostType and c.hostId = :hostId and c.channel = :channel",
            ChatConversationDO::class.java,
            Pair("hostType", hostType),
            Pair("hostId", hostId),
            Pair("channel", channel),
        )
    }

    /** The context of the conversation, if the user may read it; null otherwise (also for an unknown one). */
    open fun getContext(conversationId: Long?, user: PFUserDO): Context? {
        val conversation = findConversation(conversationId) ?: return null
        return getContext(conversation, user)
    }

    open fun getContext(conversation: ChatConversationDO, user: PFUserDO): Context? {
        val host = hostRegistry.get(conversation.hostType) ?: return null
        val hostId = conversation.hostId ?: return null
        val channel = conversation.channel ?: return null
        if (user.id == null || !host.hasReadAccess(user, hostId, channel)) {
            return null
        }
        val title = host.title(hostId, channel) ?: return null
        return Context(
            conversation = conversation,
            host = host,
            user = user,
            title = title,
            canWrite = host.hasWriteAccess(user, hostId, channel),
            isModerator = host.isModerator(user, hostId, channel),
        )
    }

    /** The context of the conversation; throws an [AccessException], if the user may not read it. */
    open fun requireContext(conversationId: Long?, user: PFUserDO): Context {
        return getContext(conversationId, user) ?: throw AccessException(user, "access.exception.noAccess")
    }

    /** The context of the conversation; throws an [AccessException], if the user may not write in it. */
    open fun requireWriteContext(conversationId: Long?, user: PFUserDO): Context {
        val context = requireContext(conversationId, user)
        if (!context.canWrite) {
            throw AccessException(user, "access.exception.noAccess")
        }
        return context
    }

    /** May the user read the conversation (each one of [ChatHost.participantIds] is checked by it)? */
    open fun hasReadAccess(context: Context, user: PFUserDO): Boolean {
        return user.id != null && !user.deleted && user.hasSystemAccess() &&
                context.host.hasReadAccess(user, context.hostId, context.channel)
    }
}
