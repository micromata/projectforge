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

package org.projectforge.idp.model

/**
 * Provider-neutral user representation used by the shared IdP sync handlers.
 * Each provider's [org.projectforge.idp.IdpAdminClient] converts between its
 * native API DTOs and this neutral model.
 */
data class IdpUser(
    val id: String? = null,
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val enabled: Boolean = true,
    val attributes: Map<String, List<String>>? = null,
)
