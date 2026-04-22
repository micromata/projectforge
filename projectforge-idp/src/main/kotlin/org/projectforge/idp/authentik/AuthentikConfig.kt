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

package org.projectforge.idp.authentik

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Authentik-specific connection properties.
 * Only active when projectforge.idp.provider=authentik.
 *
 * Authentication: Authentik supports API token authentication (preferred) or OAuth2 client credentials.
 */
@Configuration
@ConfigurationProperties(prefix = "projectforge.authentik")
@ConditionalOnProperty(name = ["projectforge.idp.provider"], havingValue = "authentik")
open class AuthentikConfig {

    /** Authentik server base URL, e.g. https://authentik.example.com */
    var serverUrl: String = ""

    /** API token for Authentik (generated in Authentik admin → Directory → Tokens). */
    var apiToken: String = ""

    /** Page size for paginated Authentik API calls. */
    var pageSize: Int = 100

    fun isConfigured() = serverUrl.isNotBlank() && apiToken.isNotBlank()
}
