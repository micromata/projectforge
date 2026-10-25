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

import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/**
 * Direct messages (PF-CHAT-DM): a conversation of two users ([ChatDirectChatHost]). Anybody may start one with a user
 * they may see in the user selection (DM-02, as the participants of rooms, ROOM-05); afterwards both write in it.
 */
@Service
open class ChatDirectService {
    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /** A conversation of the logged-in user with another one. */
    class Entry(val pair: ChatDirectDO, val partnerId: Long, val conversation: ChatConversationDO)

    /**
     * The conversation of the logged-in user with the other user, created if missing.
     * @throws AccessException if the logged-in user may not see the other one (or has no system access).
     * @throws UserException for the user self.
     */
    open fun open(otherUserId: Long): ChatAccess.Context {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val userId = user.id!!
        if (otherUserId == userId) {
            throw UserException(ERROR_SELF)
        }
        val other = userGroupCache.getUser(otherUserId)
        if (other == null || other.deleted || !other.hasSystemAccess() || !user.hasSystemAccess() ||
            !userDao.hasLoggedInUserSelectAccess(other, false)
        ) {
            throw AccessException(user, NO_ACCESS)
        }
        val pair = findPair(userId, otherUserId) ?: createPair(userId, otherUserId)
        return chatService.open(ChatDirectChatHost.HOST_TYPE, pair.id!!, ChatDirectChatHost.CHANNEL)
            ?: throw AccessException(user, NO_ACCESS)
    }

    /**
     * The direct messages of the logged-in user with at least one message (an opened, but empty one isn't listed,
     * also not at the other one).
     */
    open fun list(): List<Entry> {
        val userId = ThreadLocalUserContext.requiredLoggedInUserId
        val pairs = persistenceService.executeQuery(
            "from ChatDirectDO d where d.user1Id = :userId or d.user2Id = :userId",
            ChatDirectDO::class.java,
            Pair("userId", userId),
        )
        val conversations = chatService.findConversations(ChatDirectChatHost.HOST_TYPE, pairs.mapNotNull { it.id })
        return pairs.mapNotNull { pair ->
            val conversation = conversations[pair.id]?.takeIf { it.lastMessageAt != null } ?: return@mapNotNull null
            Entry(pair, pair.partnerOf(userId)!!, conversation)
        }
    }

    private fun findPair(userId: Long, otherUserId: Long): ChatDirectDO? {
        return persistenceService.selectSingleResult(
            "from ChatDirectDO d where d.user1Id = :user1Id and d.user2Id = :user2Id",
            ChatDirectDO::class.java,
            Pair("user1Id", minOf(userId, otherUserId)),
            Pair("user2Id", maxOf(userId, otherUserId)),
        )
    }

    private fun createPair(userId: Long, otherUserId: Long): ChatDirectDO {
        return try {
            persistenceService.runInTransaction { context ->
                ChatDirectDO().also {
                    it.created = Date()
                    it.user1Id = minOf(userId, otherUserId)
                    it.user2Id = maxOf(userId, otherUserId)
                    context.em.persist(it)
                    context.em.flush()
                }
            }
        } catch (ex: Exception) {
            // Created by the other one at the same time (unique constraint)?
            findPair(userId, otherUserId) ?: throw ex
        }
    }

    companion object {
        const val ERROR_SELF = "chat.direct.error.self"
        private const val NO_ACCESS = "access.exception.noAccess"
    }
}
