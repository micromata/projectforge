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

import org.projectforge.business.chat.ChatHost
import org.projectforge.business.chat.ChatService
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct

/**
 * The discussion of a case (PF-CVR DISC-01): one conversation per case, channel [CHANNEL]. For the time being all readers
 * of the CVE-Radar read and write in it; it is read-only once the case is closed (CTX-05) and goes with the case when
 * that is deleted. The security team moderates it (DISC-04). The notice says that no project or system details belong
 * here: they are discussed in the file of a team check ([CvrTeamCheckChatHost]), which is not visible to everybody.
 */
@Service
class CvrCaseChatHost : ChatHost {
    /** Lazy: the chat service knows the hosts. */
    @Lazy
    @Autowired
    private lateinit var chatService: ChatService

    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var caseDao: CvrCaseDao

    @Autowired
    private lateinit var ratingDao: CvrRatingDao

    override val hostType: String = HOST_TYPE

    @PostConstruct
    private fun postConstruct() {
        caseDao.register(object : BaseDOModifiedListener<CvrCaseDO> {
            override fun afterInsertOrModify(obj: CvrCaseDO, operationType: OperationType) {
                if (operationType == OperationType.DELETE) {
                    obj.id?.let { chatService.deleteConversations(HOST_TYPE, it) }
                }
            }
        })
    }

    override fun channels(hostId: Long): List<String> = if (case(hostId) != null) listOf(CHANNEL) else emptyList()

    override fun title(hostId: Long, channel: String): String? {
        val case = case(hostId, channel) ?: return null
        return listOfNotNull(case.code, case.title?.takeIf { it != case.code }).joinToString(" – ")
    }

    /** The discussion is open to all readers, so no details of projects and systems belong here. */
    override fun notice(hostId: Long, channel: String, user: PFUserDO): String? {
        case(hostId, channel) ?: return null
        return I18nHelper.getLocalizedMessage(user, "plugins.cveradar.chat.caseNotice")
    }

    override fun link(hostId: Long): String = "/cveradar/case/$hostId"

    override fun hasReadAccess(user: PFUserDO, hostId: Long, channel: String): Boolean {
        return case(hostId, channel) != null && !user.deleted && user.hasSystemAccess() && access.isReader(user)
    }

    /** Read-only once the case is closed. */
    override fun hasWriteAccess(user: PFUserDO, hostId: Long, channel: String): Boolean {
        val case = case(hostId, channel) ?: return false
        return case.status?.isClosed != true && hasReadAccess(user, hostId, channel)
    }

    override fun isModerator(user: PFUserDO, hostId: Long, channel: String): Boolean {
        return case(hostId, channel) != null && user.hasSystemAccess() && access.isSecurityTeam(user)
    }

    /** The assignee and the persons who rated the case. */
    override fun participantIds(hostId: Long, channel: String): Set<Long> {
        val case = case(hostId, channel) ?: return emptySet()
        val ids = mutableSetOf<Long>()
        case.assigneeId?.let { ids.add(it) }
        ratingDao.selectByCase(hostId).mapNotNullTo(ids) { it.user?.id }
        return ids
    }

    /** Files aren't exchanged in the discussion of a case, as texts and links serve it (and the data transfer needs a plugin). */
    override fun filesAllowed(hostId: Long, channel: String): Boolean = false

    private fun case(hostId: Long, channel: String = CHANNEL): CvrCaseDO? {
        if (channel != CHANNEL || !access.isPluginActive()) {
            return null
        }
        return caseDao.find(hostId, checkAccess = false)?.takeIf { !it.deleted }
    }

    companion object {
        const val HOST_TYPE = "cvrCase"
        const val CHANNEL = "internal"
    }
}
