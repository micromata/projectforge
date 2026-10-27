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

package org.projectforge.plugins.datatransfer

import jakarta.annotation.PostConstruct
import org.projectforge.business.chat.ChatHost
import org.projectforge.business.chat.ChatService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.StringHelper
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Every data transfer area is a chat room as well (PF-CHAT-DT): one conversation per area (channel [CHANNEL]), read and
 * written by the users with access inside ProjectForge (admins, access users, members of the access groups),
 * moderated by its admins. The files of its messages go into the area itself.
 *
 * Not for personal boxes (their chat is the direct message with the owner), synthetic areas (e.g. of a chat
 * conversation) and on the gateway (its areas are administered by the main instance). External users of an area
 * never see its chat, only its files (with their descriptions): the public page has nothing of it, the chat says so.
 *
 * Deleting an area deletes its conversation. The areas are cached for [CACHE_MILLIS], as the access is checked several times per request and per user in loops
 * (see ChatRoomChatHost); a change of an area evicts it at once.
 */
@Service
class DataTransferChatHost : ChatHost {
    /** Lazy: the chat service knows the hosts. */
    @Lazy
    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var dataTransferAreaDao: DataTransferAreaDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    private class CachedArea(val area: DataTransferAreaDO?, val loadedAt: Long)

    private val cache = ConcurrentHashMap<Long, CachedArea>()

    /** Incremented by every change of an area: an area loaded before isn't cached. */
    private val generation = AtomicLong()

    override val hostType: String = HOST_TYPE

    @PostConstruct
    private fun postConstruct() {
        dataTransferAreaDao.register(object : BaseDOModifiedListener<DataTransferAreaDO> {
            override fun afterInsertOrModify(obj: DataTransferAreaDO, operationType: OperationType) {
                generation.incrementAndGet()
                val id = obj.id ?: return
                cache.remove(id)
                if (operationType == OperationType.DELETE) {
                    // The conversation goes with the area (CTX-05), its files went with the area already.
                    chatService.deleteConversations(HOST_TYPE, id)
                }
            }
        })
    }

    /** Can the area have a chat: an ordinary area, no personal box, no synthetic area, not on the gateway? */
    fun isChatArea(area: DataTransferAreaDO?): Boolean {
        area ?: return false
        return !area.deleted && !area.isPersonalBox() && dataTransferAreaDao.getAreaHost(area) == null &&
                !dataTransferAreaDao.externalAccessConfig.gatewayMode
    }

    override fun channels(hostId: Long): List<String> {
        return if (area(hostId) != null) listOf(CHANNEL) else emptyList()
    }

    override fun title(hostId: Long, channel: String): String? = area(hostId, channel)?.areaName

    /** External users of the area see its files with their descriptions, never the chat. */
    override fun notice(hostId: Long, channel: String, user: PFUserDO): String? {
        area(hostId, channel)?.takeIf { it.externalAccessEnabled } ?: return null
        return I18nHelper.getLocalizedMessage(user, "plugins.datatransfer.chat.externalNotice")
    }

    override fun link(hostId: Long): String = "/datatransfer/$hostId"

    override fun hasReadAccess(user: PFUserDO, hostId: Long, channel: String): Boolean {
        val area = area(hostId, channel) ?: return false
        return !user.deleted && user.hasSystemAccess() && dataTransferAreaDao.hasInternalAccess(user, area)
    }

    override fun hasWriteAccess(user: PFUserDO, hostId: Long, channel: String): Boolean {
        return hasReadAccess(user, hostId, channel)
    }

    /** The admins of the area. */
    override fun isModerator(user: PFUserDO, hostId: Long, channel: String): Boolean {
        val area = area(hostId, channel) ?: return false
        return user.hasSystemAccess() && user.id in ids(area.adminIds)
    }

    /** All users with access: admins, access users and the members of the access groups. */
    override fun participantIds(hostId: Long, channel: String): Set<Long> {
        val area = area(hostId, channel) ?: return emptySet()
        val userIds = ids(area.adminIds) + ids(area.accessUserIds)
        val groupIds = ids(area.accessGroupIds).filter { userGroupCache.getGroup(it)?.deleted == false }
        return userGroupCache.getMemberIds(groupIds, userIds)
            .filter { userGroupCache.getUser(it)?.let { user -> !user.deleted && user.hasSystemAccess() } == true }
            .toSet()
    }

    /** The files of the messages go into the area itself, as uploaded there. */
    override val usesOwnFileArea: Boolean
        get() = true

    /**
     * The area, null (no files) if its files are on the gateway: no upload into it here, see
     * [DataTransferAccessChecker.checkUploadAccess].
     */
    override fun fileAreaId(hostId: Long, channel: String, user: PFUserDO): Long? {
        val area = area(hostId, channel) ?: return null
        return if (dataTransferAreaDao.externalAccessConfig.isOnGateway(area)) null else area.id
    }

    /** No files if the area is on the gateway, as for [fileAreaId]. */
    override fun filesAllowed(hostId: Long, channel: String): Boolean {
        val area = area(hostId, channel) ?: return false
        return !dataTransferAreaDao.externalAccessConfig.isOnGateway(area)
    }

    private fun area(hostId: Long, channel: String = CHANNEL): DataTransferAreaDO? {
        if (channel != CHANNEL) {
            return null
        }
        val now = System.currentTimeMillis()
        cache[hostId]?.takeIf { now - it.loadedAt < CACHE_MILLIS }?.let { return it.area }
        val loadedGeneration = generation.get()
        val area = dataTransferAreaDao.find(hostId, checkAccess = false)?.takeIf { isChatArea(it) }
        if (generation.get() == loadedGeneration) {
            cache[hostId] = CachedArea(area, now)
            if (cache.size > MAX_CACHED) {
                cache.entries.removeIf { now - it.value.loadedAt >= CACHE_MILLIS }
            }
        }
        return area
    }

    private fun ids(csv: String?): List<Long> = StringHelper.splitToLongs(csv, ",")?.toList() ?: emptyList()

    companion object {
        const val HOST_TYPE = "dataTransfer"
        const val CHANNEL = "main"

        private const val CACHE_MILLIS = 10_000L
        private const val MAX_CACHED = 1000
    }
}
