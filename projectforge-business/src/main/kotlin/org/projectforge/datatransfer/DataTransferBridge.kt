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

import jakarta.annotation.PostConstruct
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.files.FileObject
import org.springframework.stereotype.Service
import java.io.InputStream

/**
 * Bridge for data transfer, if available as plugin. DataTransfer itself isn't accessible from this module.
 */
@Service
class DataTransferBridge {
    private var dataTransferInterface: DataTransferInterface? = null

    private val areaHosts = java.util.concurrent.CopyOnWriteArrayList<DataTransferAreaHost>()

    val available: Boolean
        get() = dataTransferInterface != null

    @PostConstruct
    private fun postConstruct() {
        instance = this
    }

    /**
     * The DataTransferService of the plugin datatransfer must call this method to register itself (null: none, for tests).
     */
    fun register(dataTransferInterface: DataTransferInterface?) {
        this.dataTransferInterface = dataTransferInterface
    }

    fun getPersonalBoxOfUserLink(userId: Long = ThreadLocalUserContext.requiredLoggedInUserId): String? {
        return dataTransferInterface?.getPersonalBoxOfUserLink(userId)
    }

    fun putFileInUsersInBox(
        filename: String,
        content: ByteArray,
        receiver: PFUserDO = ThreadLocalUserContext.requiredLoggedInUser,
        description: String? = null,
    ): Boolean {
        return dataTransferInterface?.putFileInUsersInBox(
            receiver = receiver,
            filename = filename,
            content = content,
            description = description
        ) ?: false
    }

    fun putFileInUsersInBox(
        filename: String,
        content: String,
        receiver: PFUserDO = ThreadLocalUserContext.requiredLoggedInUser,
        description: String? = null,
    ): Boolean {
        return dataTransferInterface?.putFileInUsersInBox(
            receiver = receiver,
            filename = filename,
            content = content.toByteArray(),
            description = description
        ) ?: false
    }

    /** Can files be stored for other modules (see [DataTransferInterface.isFileStorageAvailable])? */
    val fileStorageAvailable: Boolean
        get() = dataTransferInterface?.isFileStorageAvailable() == true

    /** A module with synthetic areas registers itself on start. */
    fun registerAreaHost(host: DataTransferAreaHost) {
        areaHosts += host
    }

    /** The module owning the synthetic area, null for an ordinary area. */
    fun getAreaHost(areaId: Long?): DataTransferAreaHost? {
        areaId ?: return null
        return areaHosts.find { it.owns(areaId) }
    }

    fun getExpiryDaysChoices(): List<Int> = dataTransferInterface?.getExpiryDaysChoices() ?: emptyList()

    /** See [DataTransferInterface.saveSyntheticArea]. */
    fun saveSyntheticArea(areaId: Long?, name: String, expiryDays: Int?): Long? {
        return dataTransferInterface?.takeIf { it.isFileStorageAvailable() }?.saveSyntheticArea(areaId, name, expiryDays)
    }

    fun deleteArea(areaId: Long) {
        dataTransferInterface?.deleteArea(areaId)
    }

    fun getPersonalBoxId(userId: Long): Long? {
        return dataTransferInterface?.takeIf { it.isFileStorageAvailable() }?.getPersonalBoxId(userId)
    }

    fun getMaxUploadSize(areaId: Long): Long? = dataTransferInterface?.getMaxUploadSize(areaId)

    fun getExpiryDays(areaId: Long): Int? = dataTransferInterface?.getExpiryDays(areaId)

    /** See [DataTransferInterface.addFile]. */
    fun addFile(areaId: Long, fileName: String, content: ByteArray): DataTransferFile {
        val dataTransfer = dataTransferInterface?.takeIf { it.isFileStorageAvailable() }
            ?: throw IllegalStateException("Data transfer isn't available for storing files.")
        return dataTransfer.addFile(areaId, fileName, content)
    }

    fun getFile(areaId: Long, fileId: String): Pair<FileObject, InputStream>? {
        return dataTransferInterface?.getFile(areaId, fileId)
    }

    fun deleteFile(areaId: Long, fileId: String): Boolean = dataTransferInterface?.deleteFile(areaId, fileId) ?: false

    fun getFileIds(areaId: Long): Set<String> = dataTransferInterface?.getFileIds(areaId) ?: emptySet()

    companion object {
        lateinit var instance: DataTransferBridge
            private set

        val available: Boolean by lazy { instance.available }
    }
}
