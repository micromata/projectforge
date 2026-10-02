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

package org.projectforge.plugins.licensemanagement.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.plugins.licensemanagement.LicenseDO
import org.projectforge.plugins.licensemanagement.LicenseStatus
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.User
import java.time.LocalDate

/**
 * A license as the projectforge-next page reads and writes it (see `LicenseEntityRest`).
 *
 * The two stored files ([LicenseDO.file1]/[LicenseDO.file2]) are deliberately no part of it: they are BLOBs of
 * any size, which neither a list nor a form has any use for, and they are changed through the file endpoints
 * of `LicenseEntityRest` only. The DTO carries their names, read only.
 */
class License(
    var organization: String? = null,
    var product: String? = null,
    var version: String? = null,
    var updateFromVersion: String? = null,
    var licenseHolder: String? = null,
    /** Null for a user who may not see it (see [keyVisible]). */
    var key: String? = null,
    var numberOfLicenses: Int? = null,
    /** The owners stored as comma separated user ids in [LicenseDO.ownerIds]. */
    var owners: List<User>? = null,
    var device: String? = null,
    var comment: String? = null,
    var status: LicenseStatus? = null,
    var validSince: LocalDate? = null,
    var validUntil: LocalDate? = null,
) : BaseDTO<LicenseDO>() {
    /** Name of the first stored file, if any. Read only: written by the file endpoints of `LicenseEntityRest`. */
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var filename1: String? = null

    /** Name of the second stored file, if any. Read only, as [filename1]. */
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var filename2: String? = null

    /**
     * Whether the logged-in user may see the [key] and the files of this license
     * (`LicenseManagementRight.isLicenseKeyVisible`: a new license, an administrator or an owner).
     * Read only: the server decides it, a client posting it back changes nothing.
     */
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var keyVisible: Boolean = true

    /**
     * How many of the two file slots hold a file, for the list column. Null for none, so the cell stays
     * empty. Given to every reader, as it says nothing about the files but that they exist.
     */
    @get:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    var numberOfFiles: Int? = null

    override fun copyFrom(src: LicenseDO) {
        super.copyFrom(src)
        owners = User.toUserList(src.ownerIds)
        User.restoreDisplayNames(owners)
    }

    override fun copyTo(dest: LicenseDO) {
        super.copyTo(dest)
        dest.ownerIds = User.toLongList(owners)
    }
}
