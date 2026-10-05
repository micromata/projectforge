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

package org.projectforge.idp

import org.projectforge.framework.integration.SubsystemProblemMatch
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SubsystemStatusProvider
import org.projectforge.login.LoginService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/** The identity provider (Authentik, Keycloak), if configured and used by an IdP login handler. */
@Component
class IdpSubsystemStatusProvider : SubsystemStatusProvider {
    @Autowired
    private lateinit var loginService: LoginService

    @Autowired
    private lateinit var idpAdminClient: ObjectProvider<IdpAdminClient>

    override val id = "idp"

    override val titleKey = "system.admin.adminErrors.subsystem.idp"

    override val problems = SubsystemProblemMatch(
        codePrefixes = listOf("idp."),
        locationPrefixes = listOf("Idp", "Authentik", "Keycloak"),
        syncTypes = listOf("idp-"),
    )

    override fun status(): SubsystemStatus? {
        if (!loginService.loginHandlerName.startsWith("Idp")) {
            return null
        }
        val client = idpAdminClient.ifAvailable?.takeIf { it.isConfigured() } ?: return null
        return SubsystemStatus.ofSyncs(problems.syncTypes, detail = client.providerName())
    }
}
