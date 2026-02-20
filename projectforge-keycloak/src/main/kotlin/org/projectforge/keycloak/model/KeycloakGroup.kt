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

package org.projectforge.keycloak.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * DTO matching Keycloak's group representation from the Admin REST API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KeycloakGroup(
    val id: String? = null,
    val name: String? = null,
    val path: String? = null,
    val attributes: Map<String, List<String>>? = null,
    val subGroups: List<KeycloakGroup>? = null
)
