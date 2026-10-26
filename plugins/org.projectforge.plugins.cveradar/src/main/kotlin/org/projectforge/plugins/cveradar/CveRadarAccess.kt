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

import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Who may do what in the CVE-Radar (R0):
 * - security team (ST): read-write right; sees and edits everything, incl. the devices of all persons (VIS-15),
 * - readers (e.g. management): read-only right; cases and the closure overview with counts only, no persons,
 * - security officer (SO): a member of the configured group ([CveRadarConfig.securityOfficerGroup]) with the
 *   read-write right; closes a case with open items (CLS-05),
 * - everybody: the own devices and the confirmation of client measures (CLS-04).
 */
@Service
class CveRadarAccess {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    fun isSecurityTeam(user: PFUserDO): Boolean {
        return accessChecker.hasRight(user, CveRadarRightId.PLUGIN_CVERADAR, false, UserRightValue.READWRITE)
    }

    fun isReader(user: PFUserDO): Boolean {
        return accessChecker.hasRight(
            user, CveRadarRightId.PLUGIN_CVERADAR, false, UserRightValue.READONLY, UserRightValue.READWRITE,
        )
    }

    fun isSecurityOfficer(user: PFUserDO): Boolean {
        if (!isSecurityTeam(user)) {
            return false
        }
        val name = config.securityOfficerGroup.takeIf { it.isNotBlank() } ?: return false
        val group = userGroupCache.getGroupByName(name) ?: return false
        return userGroupCache.isUserMemberOfGroup(user.id, group.id)
    }

    fun isMemberOf(user: PFUserDO, groupId: Long?): Boolean {
        return groupId != null && userGroupCache.isUserMemberOfGroup(user.id, groupId)
    }

    fun checkSecurityTeam(user: PFUserDO) {
        if (!isSecurityTeam(user)) {
            throw AccessException(user, NO_ACCESS)
        }
    }

    fun checkReader(user: PFUserDO) {
        if (!isReader(user)) {
            throw AccessException(user, NO_ACCESS)
        }
    }

    companion object {
        const val NO_ACCESS = "access.exception.noAccess"
    }
}
