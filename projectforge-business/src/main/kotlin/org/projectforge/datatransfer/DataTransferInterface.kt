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

package org.projectforge.datatransfer

import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.FileObject
import java.io.InputStream

/**
 * Service for data transfer, if available as plugin. DataTransfer itself isn't accessible by this module.
 */
interface DataTransferInterface {
    fun getPersonalBoxOfUserLink(userId: Long): String?

    fun putFileInUsersInBox(receiver: PFUserDO, filename: String, content: ByteArray, description: String? = null): Boolean

    /** Can other modules store files here: the plugin is active, and this isn't the gateway? */
    fun isFileStorageAvailable(): Boolean

    /** The choices of the expiry of the files of an area, in days. */
    fun getExpiryDaysChoices(): List<Int>

    /**
     * Creates ([areaId] null) or updates a synthetic area of a [DataTransferAreaHost]: name and expiry of its files
     * (null: the default), without external access. No access check: the caller decides.
     * @return The id of the area, null if files can't be stored ([isFileStorageAvailable]).
     */
    fun saveSyntheticArea(areaId: Long?, name: String, expiryDays: Int?): Long?

    /** Deletes the area with its files. No access check. */
    fun deleteArea(areaId: Long)

    /** The id of the personal box of the user (created if missing), null if there are none (gateway). */
    fun getPersonalBoxId(userId: Long): Long?

    /** The largest file the area takes now in bytes (its upload limit and free capacity), null for an unknown area. */
    fun getMaxUploadSize(areaId: Long): Long?

    /** The days the files of the area are kept, null for an unknown area. */
    fun getExpiryDays(areaId: Long): Int?

    /**
     * Stores the file in the area, uploaded by the logged-in user. The size is checked against the area, the access
     * isn't: the caller decides.
     */
    fun addFile(areaId: Long, fileName: String, content: ByteArray): DataTransferFile

    /** The file and its content, null if it doesn't exist (any more). No access check. */
    fun getFile(areaId: Long, fileId: String): Pair<FileObject, InputStream>?

    /** Deletes the file. No access check. */
    fun deleteFile(areaId: Long, fileId: String): Boolean

    /** The ids of the files in the area (expired ones are deleted by the clean-up job). */
    fun getFileIds(areaId: Long): Set<String>
}
