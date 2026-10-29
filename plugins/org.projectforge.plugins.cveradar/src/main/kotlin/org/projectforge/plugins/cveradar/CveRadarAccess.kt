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
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.core.PluginAdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Who may do what in the CVE-Radar: the roles are given by membership in PF groups, named in the configuration
 * ([CveRadarConfig.readGroup], [CveRadarConfig.writeGroup], [CveRadarConfig.securityOfficerGroup]). The roles build on
 * each other, [roles] is the only place deciding them:
 * - [CvrRole.READER] (e.g. management): reads the vulnerabilities, cases, ratings, measures, the closure overview with
 *   counts only (no persons), discusses in the case discussion,
 * - [CvrRole.SECURITY_TEAM] (ST): edits everything, sees the devices of all persons (VIS-15), moderates the chats,
 * - [CvrRole.SECURITY_OFFICER] (SO): additionally closes a case with open items (CLS-05) and sets the binding priority
 *   of cases of others,
 * - everybody: the own devices and the confirmation of client measures (CLS-04), the team checks of the own groups.
 *
 * A group that isn't configured or doesn't exist grants nothing; the PF admin role alone grants no access (VIS-05).
 */
@Service
class CveRadarAccess {
    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var pluginAdminService: PluginAdminService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /**
     * Refuses every endpoint of the CVE-Radar while the plugin isn't activated: its tables are created on activation
     * only, and an inactive plugin answers nothing.
     */
    fun checkPluginActive() {
        if (!isPluginActive()) {
            throw AccessException(NO_ACCESS)
        }
    }

    fun isPluginActive(): Boolean = pluginAdminService.isActive(PluginAdminService.PLUGIN_CVE_RADAR_ID)

    /** The roles of the user, each one including the weaker ones. */
    fun roles(user: PFUserDO): Set<CvrRole> {
        val roles = mutableSetOf<CvrRole>()
        if (isMemberOfNamed(user, config.readGroup)) {
            roles.add(CvrRole.READER)
        }
        if (isMemberOfNamed(user, config.writeGroup)) {
            roles.add(CvrRole.READER)
            roles.add(CvrRole.SECURITY_TEAM)
        }
        if (isMemberOfNamed(user, config.securityOfficerGroup)) {
            roles.addAll(CvrRole.entries)
        }
        return roles
    }

    fun isSecurityTeam(user: PFUserDO): Boolean = CvrRole.SECURITY_TEAM in roles(user)

    fun isReader(user: PFUserDO): Boolean = CvrRole.READER in roles(user)

    fun isSecurityOfficer(user: PFUserDO): Boolean = CvrRole.SECURITY_OFFICER in roles(user)

    private fun isMemberOfNamed(user: PFUserDO, groupName: String): Boolean {
        val name = groupName.trim().takeIf { it.isNotEmpty() } ?: return false
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
