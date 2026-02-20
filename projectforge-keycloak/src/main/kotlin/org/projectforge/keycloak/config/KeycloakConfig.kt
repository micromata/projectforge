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

package org.projectforge.keycloak.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for the Keycloak integration.
 * Activate via: projectforge.login.handlerClass=KeycloakLoginHandler
 */
@Configuration
@ConfigurationProperties(prefix = "projectforge.keycloak")
open class KeycloakConfig {

    /** Keycloak server base URL, e.g. https://keycloak.example.com */
    var serverUrl: String = ""

    /** Realm name */
    var realm: String = ""

    /** Service account client ID for Admin API access */
    var clientId: String = ""

    /** Service account client secret */
    var clientSecret: String = ""

    /** Page size for paginated Keycloak API calls */
    var pageSize: Int = 100

    fun isConfigured() = serverUrl.isNotBlank() && realm.isNotBlank()
            && clientId.isNotBlank() && clientSecret.isNotBlank()
}
