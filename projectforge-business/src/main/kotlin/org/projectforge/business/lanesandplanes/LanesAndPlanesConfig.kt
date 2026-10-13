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

package org.projectforge.business.lanesandplanes

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Push of the active employees with their bookable cost units (Kost2) to Lanes & Planes (user import of the
 * ExtAPI). Every push replaces the complete user list in Lanes & Planes: users not sent are deactivated there.
 * The technical settings only, read on start; the content (invoice profiles, Kost2) is maintained in the
 * configuration parameter `lanesAndPlanes`, see [LanesAndPlanesSettings].
 */
@Configuration
@ConfigurationProperties(prefix = "projectforge.lanesandplanes")
@ConditionalOnProperty(name = ["projectforge.lanesandplanes.enabled"], havingValue = "true")
open class LanesAndPlanesConfig {
    var enabled: Boolean = false
    var url: String = "https://api.lanes-planes.com/ext/users"
    /** The API access token, created in the personal settings of a Lanes & Planes admin account. */
    var apiKey: String = ""
    /** Builds the payload and writes it to the work directory, but doesn't send it. */
    var dryRun: Boolean = true
    /** Nothing is sent, if fewer users are found (protection against deactivating all users in Lanes & Planes). */
    var minUsers: Int = 1
}
