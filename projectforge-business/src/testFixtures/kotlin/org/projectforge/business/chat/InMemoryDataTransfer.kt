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

package org.projectforge.business.chat

import org.projectforge.common.i18n.UserException
import org.projectforge.datatransfer.DataTransferFile
import org.projectforge.datatransfer.DataTransferInterface
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.files.FileObject
import java.io.InputStream
import java.util.concurrent.atomic.AtomicLong

/** The data transfer for tests of the chat (the plugin isn't part of this module): areas and files in memory. */
class InMemoryDataTransfer : DataTransferInterface {
    class Area(var name: String, var expiryDays: Int?, val files: MutableMap<String, Pair<String, ByteArray>> = mutableMapOf())

    val areas = mutableMapOf<Long, Area>()

    /** User id -> area id. */
    val personalBoxes = mutableMapOf<Long, Long>()

    var maxUploadSize = Long.MAX_VALUE

    private val ids = AtomicLong(1_000_000)

    override fun getPersonalBoxOfUserLink(userId: Long): String? = null

    override fun putFileInUsersInBox(receiver: PFUserDO, filename: String, content: ByteArray, description: String?) = false

    override fun isFileStorageAvailable() = true

    override fun getExpiryDaysChoices() = listOf(7, 30, 90)

    override fun saveSyntheticArea(areaId: Long?, name: String, expiryDays: Int?): Long {
        val id = areaId ?: ids.incrementAndGet()
        val area = areas.getOrPut(id) { Area(name, 30) }
        area.name = name
        expiryDays?.let { area.expiryDays = it }
        return id
    }

    override fun deleteArea(areaId: Long) {
        areas.remove(areaId)
    }

    override fun getPersonalBoxId(userId: Long): Long {
        return personalBoxes.getOrPut(userId) { saveSyntheticArea(null, "<PERSONAL_BOX>", 60) }
    }

    override fun getMaxUploadSize(areaId: Long): Long? = areas[areaId]?.let { maxUploadSize }

    override fun getExpiryDays(areaId: Long): Int? = areas[areaId]?.expiryDays

    override fun addFile(areaId: Long, fileName: String, content: ByteArray): DataTransferFile {
        if (content.size > maxUploadSize) {
            throw UserException("file.upload.error.maxSizeOfExceeded")
        }
        val fileId = "f${ids.incrementAndGet()}"
        areas[areaId]!!.files[fileId] = Pair(fileName, content)
        return DataTransferFile(areaId, fileId, fileName, content.size.toLong())
    }

    override fun getFile(areaId: Long, fileId: String): Pair<FileObject, InputStream>? {
        val (name, content) = areas[areaId]?.files?.get(fileId) ?: return null
        val fileObject = FileObject().also {
            it.fileId = fileId
            it.fileName = name
        }
        return Pair(fileObject, content.inputStream())
    }

    override fun deleteFile(areaId: Long, fileId: String): Boolean = areas[areaId]?.files?.remove(fileId) != null

    override fun getFileIds(areaId: Long): Set<String> = areas[areaId]?.files?.keys?.toSet() ?: emptySet()
}
