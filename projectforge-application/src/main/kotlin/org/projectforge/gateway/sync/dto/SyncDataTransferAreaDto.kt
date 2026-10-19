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
 * A data transfer area with external access, administered on the main instance (DataTransferExternalAccessConfig,
 * mode VIA_GATEWAY). Users and groups by name: their ids differ on the gateway.
 *
 * The area keeps its id on the gateway: the gateway has only these areas (it creates none itself, no personal
 * boxes), so the ids can't collide, and the main instance links directly to the area on the gateway.
 */
data class SyncDataTransferAreaDto(
    /** The id of the area on the main instance, also its id on the gateway. */
    val id: Long,
    val areaName: String,
    val description: String? = null,
    val adminUsernames: List<String> = emptyList(),
    val observerUsernames: List<String> = emptyList(),
    val accessUsernames: List<String> = emptyList(),
    val accessGroupNames: List<String> = emptyList(),
    val externalDownloadEnabled: Boolean = false,
    val externalUploadEnabled: Boolean = false,
    val externalAccessToken: String? = null,
    val externalPassword: String? = null,
    val expiryDays: Int? = null,
    val maxUploadSizeKB: Int? = null,
) {
    /** Hides the password (logs). */
    override fun toString(): String {
        return "SyncDataTransferAreaDto(id=$id, areaName=$areaName)"
    }
}
