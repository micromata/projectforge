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
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Direct messages as host (PF-CHAT-DM): one conversation per pair of users ([ChatDirectDO], channel [CHANNEL]).
 * Both read it as long as they have system access, and write in it as long as the other one has it too; nobody
 * moderates, administrators have no access. Whom a user may start one with is checked when the pair is created
 * ([ChatDirectService.open]), not afterwards: whoever got a message may answer.
 *
 * Each sees the name of the other one as title. Pairs never change, so they are cached.
 */
@Service
class ChatDirectChatHost : ChatHost {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    private val cache = ConcurrentHashMap<Long, ChatDirectDO>()

    override val hostType: String = HOST_TYPE

    override fun channels(hostId: Long): List<String> {
        return if (pair(hostId) != null) listOf(CHANNEL) else emptyList()
    }

    /** Both names (for whom isn't one of them, e.g. the notifications): the title is the one of [title] with user. */
    override fun title(hostId: Long, channel: String): String? {
        val pair = pair(hostId, channel) ?: return null
        return listOf(pair.user1Id, pair.user2Id).joinToString(", ") { name(it) }
    }

    override fun title(hostId: Long, channel: String, user: PFUserDO): String? {
        val pair = pair(hostId, channel) ?: return null
        return pair.partnerOf(user.id)?.let { name(it) } ?: title(hostId, channel)
    }

    override fun link(hostId: Long): String = ChatRoomChatHost.LINK

    override fun hasReadAccess(user: PFUserDO, hostId: Long, channel: String): Boolean {
        val pair = pair(hostId, channel) ?: return false
        return pair.isMember(user.id) && !user.deleted && user.hasSystemAccess()
    }

    override fun hasWriteAccess(user: PFUserDO, hostId: Long, channel: String): Boolean {
        if (!hasReadAccess(user, hostId, channel)) {
            return false
        }
        val partner = userGroupCache.getUser(pair(hostId)?.partnerOf(user.id)) ?: return false
        return !partner.deleted && partner.hasSystemAccess()
    }

    override fun isModerator(user: PFUserDO, hostId: Long, channel: String): Boolean = false

    override fun participantIds(hostId: Long, channel: String): Set<Long> {
        val pair = pair(hostId, channel) ?: return emptySet()
        return setOfNotNull(pair.user1Id, pair.user2Id)
    }

    /** The other one of the pair, null if the user isn't one of them. */
    fun partnerId(hostId: Long, userId: Long?): Long? = pair(hostId)?.partnerOf(userId)

    /** The pair, null if unknown. */
    fun pair(hostId: Long, channel: String = CHANNEL): ChatDirectDO? {
        if (channel != CHANNEL) {
            return null
        }
        cache[hostId]?.let { return it }
        val pair = persistenceService.find(ChatDirectDO::class.java, hostId) ?: return null
        if (cache.size > MAX_CACHED) {
            cache.clear()
        }
        cache[hostId] = pair
        return pair
    }

    private fun name(userId: Long?): String = userGroupCache.getUser(userId)?.getFullname() ?: "?"

    companion object {
        const val HOST_TYPE = "direct"
        const val CHANNEL = "main"

        private const val MAX_CACHED = 10_000
    }
}
