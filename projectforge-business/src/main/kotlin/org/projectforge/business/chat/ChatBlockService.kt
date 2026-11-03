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

import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/**
 * Users who block each other (PF-CHAT-DM-07). A user blocks another one for themself, which is checked on the server
 * wherever it matters: no direct message between the two ([ChatDirectService], [ChatDirectChatHost]) and, for the
 * blocker, no notifications, mentions, unread counts or search hits of the blocked one ([ChatNotifier],
 * [ChatService]). The blocked one isn't told.
 *
 * Nothing is cached: the blocks are read where they are needed (one indexed query), so a change applies at once.
 */
@Service
open class ChatBlockService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var unreadCounter: ChatUnreadCounter

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /** The users the logged-in user has blocked. */
    open fun list(): List<Long> = blockedIds(ThreadLocalUserContext.requiredLoggedInUserId)

    /**
     * Blocks the user for the logged-in one, nothing happens if they have already.
     * @throws UserException for the user self or an unknown user.
     */
    open fun block(blockedId: Long) {
        val blockerId = ThreadLocalUserContext.requiredLoggedInUserId
        if (blockedId == blockerId) {
            throw UserException(ERROR_SELF)
        }
        if (userGroupCache.getUser(blockedId) == null) {
            throw UserException(ERROR_UNKNOWN_USER)
        }
        if (find(blockerId, blockedId) != null) {
            return
        }
        try {
            persistenceService.runInTransaction { context ->
                context.em.persist(ChatUserBlockDO().also {
                    it.created = Date()
                    it.blockerId = blockerId
                    it.blockedId = blockedId
                })
                context.em.flush()
            }
        } catch (ex: Exception) {
            // Blocked at the same time (unique constraint)?
            find(blockerId, blockedId) ?: throw ex
        }
        unreadCounter.invalidate(blockerId) // The messages of the blocked one no longer count.
    }

    /** Removes the block of the logged-in user, nothing happens if there is none. */
    open fun unblock(blockedId: Long) {
        val blockerId = ThreadLocalUserContext.requiredLoggedInUserId
        persistenceService.runInTransaction { context ->
            context.executeUpdate(
                "delete from ChatUserBlockDO b where b.blockerId = :blockerId and b.blockedId = :blockedId",
                Pair("blockerId", blockerId),
                Pair("blockedId", blockedId),
            )
        }
        unreadCounter.invalidate(blockerId)
    }

    /** Has one of the two blocked the other? */
    open fun isBlocked(userId: Long?, otherUserId: Long?): Boolean {
        if (userId == null || otherUserId == null) {
            return false
        }
        return persistenceService.selectSingleResult(
            "select count(b) from ChatUserBlockDO b where (b.blockerId = :userId and b.blockedId = :otherUserId)" +
                " or (b.blockerId = :otherUserId and b.blockedId = :userId)",
            java.lang.Long::class.java,
            Pair("userId", userId),
            Pair("otherUserId", otherUserId),
        )?.toLong().let { it != null && it > 0 }
    }

    /** Has [blockerId] blocked [blockedId]? */
    open fun hasBlocked(blockerId: Long?, blockedId: Long?): Boolean {
        if (blockerId == null || blockedId == null) {
            return false
        }
        return find(blockerId, blockedId) != null
    }

    /** The users [blockerId] has blocked. */
    open fun blockedIds(blockerId: Long): List<Long> {
        return persistenceService.executeQuery(
            "select b.blockedId from ChatUserBlockDO b where b.blockerId = :blockerId",
            java.lang.Long::class.java,
            Pair("blockerId", blockerId),
        ).map { it.toLong() }
    }

    /** The users who have blocked [blockedId]. */
    open fun blockerIds(blockedId: Long): Set<Long> {
        return persistenceService.executeQuery(
            "select b.blockerId from ChatUserBlockDO b where b.blockedId = :blockedId",
            java.lang.Long::class.java,
            Pair("blockedId", blockedId),
        ).mapTo(mutableSetOf()) { it.toLong() }
    }

    private fun find(blockerId: Long, blockedId: Long): ChatUserBlockDO? {
        return persistenceService.selectSingleResult(
            "from ChatUserBlockDO b where b.blockerId = :blockerId and b.blockedId = :blockedId",
            ChatUserBlockDO::class.java,
            Pair("blockerId", blockerId),
            Pair("blockedId", blockedId),
        )
    }

    companion object {
        const val ERROR_SELF = "chat.block.error.self"
        const val ERROR_UNKNOWN_USER = "chat.block.error.unknownUser"
    }
}
