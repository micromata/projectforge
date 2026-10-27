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

import jakarta.annotation.PostConstruct
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * The chat rooms as host (PF-CHAT-ROOM): one conversation per room (channel [CHANNEL]), read and written by its
 * participants, moderated by its owner and moderators. A deleted room has no conversation any more.
 *
 * The rooms are cached for [CACHE_MILLIS]: the access is checked several times per request and per user in loops
 * (mention candidates, recipients, live events). A change of a room evicts it at once (after the commit).
 */
@Service
class ChatRoomChatHost : ChatHost {
    @Autowired
    private lateinit var chatRoomDao: ChatRoomDao

    private class CachedRoom(val room: ChatRoomDO?, val loadedAt: Long)

    private val cache = ConcurrentHashMap<Long, CachedRoom>()

    /** Incremented by every change of a room: a room loaded before isn't cached. */
    private val generation = AtomicLong()

    override val hostType: String = HOST_TYPE

    @PostConstruct
    private fun postConstruct() {
        chatRoomDao.register(object : BaseDOModifiedListener<ChatRoomDO> {
            override fun afterInsertOrModify(obj: ChatRoomDO, operationType: OperationType) {
                generation.incrementAndGet()
                obj.id?.let { cache.remove(it) }
            }
        })
    }

    override fun channels(hostId: Long): List<String> {
        return if (room(hostId) != null) listOf(CHANNEL) else emptyList()
    }

    override fun title(hostId: Long, channel: String): String? {
        return room(hostId, channel)?.name
    }

    override fun description(hostId: Long, channel: String): String? {
        return room(hostId, channel)?.description
    }

    override fun link(hostId: Long): String = LINK

    override fun hasReadAccess(user: PFUserDO, hostId: Long, channel: String): Boolean {
        val room = room(hostId, channel) ?: return false
        return user.hasSystemAccess() && chatRoomDao.isParticipant(room, user.id)
    }

    override fun hasWriteAccess(user: PFUserDO, hostId: Long, channel: String): Boolean {
        return hasReadAccess(user, hostId, channel)
    }

    override fun isModerator(user: PFUserDO, hostId: Long, channel: String): Boolean {
        val room = room(hostId, channel) ?: return false
        return user.hasSystemAccess() && chatRoomDao.isModerator(room, user.id)
    }

    override fun participantIds(hostId: Long, channel: String): Set<Long> {
        val room = room(hostId, channel) ?: return emptySet()
        return chatRoomDao.getParticipantIds(room)
    }

    private fun room(hostId: Long, channel: String = CHANNEL): ChatRoomDO? {
        if (channel != CHANNEL) {
            return null
        }
        val now = System.currentTimeMillis()
        cache[hostId]?.takeIf { now - it.loadedAt < CACHE_MILLIS }?.let { return it.room }
        val loadedGeneration = generation.get()
        val room = chatRoomDao.find(hostId, checkAccess = false)?.takeIf { !it.deleted }
        if (generation.get() == loadedGeneration) {
            cache[hostId] = CachedRoom(room, now)
            if (cache.size > MAX_CACHED) {
                cache.entries.removeIf { now - it.value.loadedAt >= CACHE_MILLIS }
            }
        }
        return room
    }

    companion object {
        const val HOST_TYPE = "chatRoom"
        const val CHANNEL = "main"
        const val LINK = "/chat"

        private const val CACHE_MILLIS = 10_000L
        private const val MAX_CACHED = 1000
    }
}
