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

package org.projectforge.idp.authentik.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTO matching Authentik's user representation from the Core API v3.
 *
 * Authentik API: GET /api/v3/core/users/
 * Key differences from Keycloak:
 * - Uses `pk` (integer) as primary key instead of UUID `id`
 * - `name` is the display name (not split into first/last)
 * - `is_active` instead of `enabled`
 * - `attributes` is a flat JSON object (not Map<String, List<String>>)
 * - `groups_obj` contains group details when included via query param
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AuthentikUser(
    val pk: Int? = null,
    val username: String? = null,
    val name: String? = null,
    val email: String? = null,
    @JsonProperty("is_active")
    val isActive: Boolean = true,
    val attributes: Map<String, Any?>? = null,
    val groups: List<String>? = null,
)
