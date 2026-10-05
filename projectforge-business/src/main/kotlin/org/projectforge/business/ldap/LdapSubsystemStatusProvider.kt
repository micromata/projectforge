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

package org.projectforge.business.ldap

import org.projectforge.framework.integration.SubsystemProblemMatch
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SubsystemStatusProvider
import org.projectforge.login.LoginService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * LDAP, if a server is configured and used: by an LDAP login handler, or by an IdP login handler delegating to it
 * (the users and groups are synced to the LDAP as well).
 */
@Component
class LdapSubsystemStatusProvider : SubsystemStatusProvider {
    @Autowired
    private lateinit var loginService: LoginService

    @Autowired
    private lateinit var ldapService: LdapService

    override val id = "ldap"

    override val titleKey = "system.admin.adminErrors.subsystem.ldap"

    override val problems = SubsystemProblemMatch(
        codePrefixes = listOf("ldap."),
        locationPrefixes = listOf("Ldap"),
        syncTypes = listOf("ldap-"),
    )

    override fun status(): SubsystemStatus? {
        val server = ldapService.ldapConfig?.server?.takeIf { it.isNotBlank() } ?: return null
        val handler = loginService.loginHandlerName
        if (!handler.startsWith("Ldap") && !handler.startsWith("Idp")) {
            return null
        }
        return SubsystemStatus.ofSyncs(problems.syncTypes, detail = SubsystemStatus.hostOf(server))
    }
}
