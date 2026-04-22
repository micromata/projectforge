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
 * DTO matching Authentik's group representation from the Core API v3.
 *
 * Authentik API: GET /api/v3/core/groups/
 * Key differences from Keycloak:
 * - Uses `pk` (UUID string) as primary key
 * - `users` is an array of user PKs (integers) — membership is stored on the group
 * - `attributes` is a flat JSON object
 * - `is_superuser` indicates admin groups
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AuthentikGroup(
    val pk: String? = null,
    val name: String? = null,
    @JsonProperty("is_superuser")
    val isSuperuser: Boolean = false,
    val users: List<Int>? = null,
    val attributes: Map<String, Any?>? = null,
    val parent: String? = null,
)
