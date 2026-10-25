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

package org.projectforge.plugins.datatransfer

import jakarta.annotation.PostConstruct
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.configuration.DomainService
import org.projectforge.common.extensions.formatBytes
import org.projectforge.datatransfer.DataTransferBridge
import org.projectforge.datatransfer.DataTransferFile
import org.projectforge.datatransfer.DataTransferInterface
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.FileInfo
import org.projectforge.jcr.FileObject
import org.projectforge.jcr.RepoService
import org.projectforge.plugins.core.PluginAdminService
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaEntityRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream

private val log = KotlinLogging.logger {}

@Service
class DataTransferService : DataTransferInterface {
    @Autowired
    private lateinit var attachmentsService: AttachmentsService

    @Autowired
    private lateinit var dataTransferAreaDao: DataTransferAreaDao

    @Autowired
    private lateinit var dataTransferAreaEntityRest: DataTransferAreaEntityRest

    @Autowired
    private lateinit var dataTransferBridge: DataTransferBridge

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var pluginAdminService: PluginAdminService

    @Autowired
    private lateinit var repoService: RepoService

    val jcrPath: String by lazy { dataTransferAreaEntityRest.jcrPath!! }

    @PostConstruct
    private fun postConstruct() {
        dataTransferBridge.register(this)
    }

    override fun getPersonalBoxOfUserLink(userId: Long): String {
        val personalBox = dataTransferAreaDao.ensurePersonalBox(userId)
            ?: throw IllegalStateException("Personal box not found for user with ID $userId.")
        return getDataTransferAreaLink(personalBox.id)
    }

    fun getDataTransferAreaLink(areaId: Long?): String {
        return domainService.getDomain(
            DataTransferUtils.areaViewPath(areaId)
        )
    }

    override fun putFileInUsersInBox(
        receiver: PFUserDO,
        filename: String,
        content: ByteArray,
        description: String?
    ): Boolean {
        val reveiverId = receiver.id
            ?: throw IllegalStateException("User ID is null for user with display name '${receiver.userDisplayName}'.")
        val personalBox = dataTransferAreaDao.ensurePersonalBox(reveiverId)
            ?: throw IllegalStateException("Personal box not found for user with ID $reveiverId.")
        try {
            attachmentsService.addAttachment(
                dataTransferAreaEntityRest.jcrPath!!,
                fileInfo = FileInfo(
                    filename,
                    fileSize = content.size.toLong(),
                    description = description
                ),
                content = content,
                baseDao = dataTransferAreaDao,
                obj = personalBox,
                accessChecker = dataTransferAreaEntityRest.attachmentsAccessChecker,
            )
            log.info { "Document '$filename' of size ${content.size.formatBytes()} put in the personal box (DataTransfer) of '${receiver.userDisplayName}' with description '$description'." }
            return true
        } catch (ex: Exception) {
            log.error(ex) { "Can't put document '${filename}' of size ${content.size.formatBytes()} into user '${receiver.userDisplayName}' personal box: ${ex.message}" }
        }
        return false
    }

    override fun isFileStorageAvailable(): Boolean {
        return !dataTransferAreaDao.externalAccessConfig.gatewayMode &&
                pluginAdminService.activePlugins.any { it.id == DataTransferPlugin.ID }
    }

    override fun getExpiryDaysChoices(): List<Int> = DataTransferAreaDao.EXPIRY_DAYS_VALUES.keys.toList()

    override fun saveSyntheticArea(areaId: Long?, name: String, expiryDays: Int?): Long? {
        if (!isFileStorageAvailable()) {
            return null
        }
        val area = areaId?.let { dataTransferAreaDao.find(it, checkAccess = false) } ?: DataTransferAreaDO()
        area.areaName = name.take(100)
        area.expiryDays = expiryDays?.takeIf { it in DataTransferAreaDao.EXPIRY_DAYS_VALUES } ?: area.expiryDays
                ?: SYNTHETIC_AREA_EXPIRY_DAYS
        DataTransferAreaDao.secureSyntheticArea(area)
        return if (area.id == null) {
            dataTransferAreaDao.insert(area, checkAccess = false)
        } else {
            dataTransferAreaDao.update(area, checkAccess = false)
            area.id
        }
    }

    override fun deleteArea(areaId: Long) {
        val area = dataTransferAreaDao.find(areaId, checkAccess = false) ?: return
        val deleted = repoService.deleteAllFilesBelow(attachmentsService.getPath(jcrPath, areaId))
        dataTransferAreaDao.delete(area, checkAccess = false) // Not historizable, so not only marked as deleted.
        log.info { "Synthetic data transfer area #$areaId '${area.areaName}' deleted with ${deleted.size} file(s)." }
    }

    override fun getPersonalBoxId(userId: Long): Long? {
        return (dataTransferAreaDao.findPersonalBox(userId) ?: dataTransferAreaDao.ensurePersonalBox(userId))?.id
    }

    override fun getMaxUploadSize(areaId: Long): Long? {
        val area = dataTransferAreaDao.find(areaId, checkAccess = false)?.takeIf { !it.deleted } ?: return null
        return minOf(DataTransferAreaDao.calculateMaxUploadFileSize(area), DataTransferAreaDao.globalMaxFileSize.toBytes())
    }

    override fun getExpiryDays(areaId: Long): Int? {
        return dataTransferAreaDao.find(areaId, checkAccess = false)?.takeIf { !it.deleted }?.expiryDays
    }

    override fun addFile(areaId: Long, fileName: String, content: ByteArray): DataTransferFile {
        val area = dataTransferAreaDao.find(areaId, checkAccess = false)?.takeIf { !it.deleted }
            ?: throw IllegalArgumentException("Data transfer area #$areaId not found.")
        val attachment = attachmentsService.addAttachment(
            jcrPath,
            fileInfo = FileInfo(fileName, fileSize = content.size.toLong()),
            content = content,
            baseDao = dataTransferAreaDao,
            obj = area,
            accessChecker = uploadChecker,
            allowDuplicateFiles = true,
        )
        return DataTransferFile(areaId, attachment.fileId!!, attachment.name ?: fileName, attachment.size ?: content.size.toLong())
    }

    override fun getFile(areaId: Long, fileId: String): Pair<FileObject, InputStream>? {
        val fileObject = repoService.getFileInfo(attachmentsService.getPath(jcrPath, areaId), AttachmentsService.DEFAULT_NODE, fileId = fileId)
            ?: return null
        val inputStream = repoService.retrieveFileInputStream(fileObject) ?: return null
        return Pair(fileObject, inputStream)
    }

    override fun deleteFile(areaId: Long, fileId: String): Boolean {
        val area = dataTransferAreaDao.find(areaId, checkAccess = false) ?: return false
        return attachmentsService.internalDeleteAttachment(jcrPath, fileId, dataTransferAreaDao, area)
    }

    override fun getFileIds(areaId: Long): Set<String> {
        return attachmentsService.internalGetAttachments(jcrPath, areaId).mapNotNullTo(mutableSetOf()) { it.fileId }
    }

    /**
     * The checker of the data transfer with the size check of the area, but without the access check of uploads: the
     * caller ([addFile]) decides, and nobody uploads into synthetic areas in the data transfer itself.
     */
    private val uploadChecker: AttachmentsAccessChecker by lazy {
        val checker = dataTransferAreaEntityRest.attachmentsAccessChecker
        object : AttachmentsAccessChecker by checker {
            override fun checkUploadAccess(user: PFUserDO?, path: String, id: Any, subPath: String?) {
            }
        }
    }

    companion object {
        /** The files of a synthetic area are kept that long by default. */
        const val SYNTHETIC_AREA_EXPIRY_DAYS = 30
    }
}
