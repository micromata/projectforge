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

package org.projectforge.plugins.cveradar

import jakarta.annotation.PostConstruct
import org.projectforge.business.chat.ChatHost
import org.projectforge.business.chat.ChatService
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrTeamCheckDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * The discussion of a team check (CLS-03), the file below the case of a CVE: here projects and systems are named. Only
 * the members of the asked group and the security team read and write; the conversation is not found by the search
 * across all conversations. Read-only once the case is closed, deleted with the check. The security team moderates it.
 */
@Service
class CvrTeamCheckChatHost : ChatHost {
    /** Lazy: the chat service knows the hosts. */
    @Lazy
    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var teamCheckDao: CvrTeamCheckDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    override val hostType: String = HOST_TYPE

    @PostConstruct
    private fun postConstruct() {
        teamCheckDao.register(object : BaseDOModifiedListener<CvrTeamCheckDO> {
            override fun afterInsertOrModify(obj: CvrTeamCheckDO, operationType: OperationType) {
                if (operationType == OperationType.DELETE) {
                    obj.id?.let { chatService.deleteConversations(HOST_TYPE, it) }
                }
            }
        })
    }

    override fun channels(hostId: Long): List<String> = if (check(hostId) != null) listOf(CHANNEL) else emptyList()

    override fun title(hostId: Long, channel: String): String? {
        val check = check(hostId, channel) ?: return null
        val code = caseDao.find(check.caseId, checkAccess = false)?.code
        return listOfNotNull(code, check.subject).joinToString(": ")
    }

    override fun link(hostId: Long): String = "/cveradar/teamcheck/$hostId"

    override fun hasReadAccess(user: PFUserDO, hostId: Long, channel: String): Boolean {
        val check = check(hostId, channel) ?: return false
        return !user.deleted && user.hasSystemAccess() &&
                (access.isSecurityTeam(user) || access.isMemberOf(user, check.groupId))
    }

    override fun hasWriteAccess(user: PFUserDO, hostId: Long, channel: String): Boolean {
        val check = check(hostId, channel) ?: return false
        val closed = caseDao.find(check.caseId, checkAccess = false)?.status?.isClosed == true
        return !closed && hasReadAccess(user, hostId, channel)
    }

    override fun isModerator(user: PFUserDO, hostId: Long, channel: String): Boolean {
        return check(hostId, channel) != null && user.hasSystemAccess() && access.isSecurityTeam(user)
    }

    /** The members of the asked group. */
    override fun participantIds(hostId: Long, channel: String): Set<Long> {
        val check = check(hostId, channel) ?: return emptySet()
        val groupId = check.groupId ?: return emptySet()
        return userGroupCache.getMemberIds(listOf(groupId), emptyList())
            .filter { userGroupCache.getUser(it)?.let { user -> !user.deleted && user.hasSystemAccess() } == true }
            .toSet()
    }


    /** Names projects and systems: not found by the search across all conversations. */
    override val searchableAcrossConversations: Boolean
        get() = false

    override fun fileExpiryDays(hostId: Long, channel: String): Int = CvrCaseChatHost.FILE_EXPIRY_DAYS

    private fun check(hostId: Long, channel: String = CHANNEL): CvrTeamCheckDO? {
        if (channel != CHANNEL || !access.isPluginActive()) {
            return null
        }
        return teamCheckDao.find(hostId, checkAccess = false)?.takeIf { !it.deleted }
    }

    companion object {
        const val HOST_TYPE = "cvrTeamCheck"
        const val CHANNEL = "internal"
    }
}
