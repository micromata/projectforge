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

package org.projectforge.gateway.sync.dto

/**
 * Result of a sync request, returned by the gateway to the main instance.
 *
 * @param icsCacheSize Number of entries in the gateway's ICS cache after the request (only set by the ICS
 * endpoint). 0 tells the main instance that the gateway lost its cache (e.g. after a restart), so all
 * calendars have to be pushed again.
 */
data class SyncResultDto(
    val created: Int = 0,
    val updated: Int = 0,
    val deleted: Int = 0,
    val errors: Int = 0,
    val icsCacheSize: Int? = null,
)
