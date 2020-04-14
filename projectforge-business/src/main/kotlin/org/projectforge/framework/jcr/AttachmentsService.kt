/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.jcr

import de.micromata.genome.db.jpa.history.entities.EntityOpType
import mu.KotlinLogging
import org.projectforge.SystemStatus
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter
import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.jcr.FileObject
import org.projectforge.jcr.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * Service for handling attachments of DO's. It's possible to attach files to each [IdObject].
 */
@Service
open class AttachmentsService {
    @Autowired
    private lateinit var emgrFactory: PfEmgrFactory

    @Autowired
    private lateinit var repoService: RepoService

    /**
     * @param path Unique path of data object.
     * @param id Id of data object.
     */
    @JvmOverloads
    open fun getAttachments(path: String, id: Any, subPath: String? = null): List<Attachment>? {
        return repoService.getFileInfos(getPath(path, id), subPath ?: DEFAULT_NODE)?.map { createAttachment(it) }
    }

    /**
     * @param path Unique path of data object.
     * @param id Id of data object.
     */
    @JvmOverloads
    open fun getAttachmentInfo(path: String, id: Any, fileId: String, subPath: String? = null): Attachment? {
        val fileObject = repoService.getFileInfo(
                getPath(path, id),
                subPath ?: DEFAULT_NODE,
                fileId = fileId)
                ?: return null
        return createAttachment(fileObject)
    }

    /**
     * @param path Unique path of data object.
     * @param id Id of data object.
     */
    @JvmOverloads
    open fun getAttachmentContent(path: String, id: Any, fileId: String, subPath: String? = null): ByteArray? {
        val fileObject = repoService.getFileInfo(
                getPath(path, id),
                subPath ?: DEFAULT_NODE,
                fileId = fileId)
                ?: return null
        return if (repoService.retrieveFile(fileObject)) {
            fileObject.content
        } else {
            null
        }
    }

    /**
     * @param path Unique path of data object.
     * @param id Id of data object.
     */
    @JvmOverloads
    open fun getAttachmentInputStream(path: String, id: Any, fileId: String, subPath: String? = null)
            : Pair<FileObject, InputStream>? {
        val fileObject = repoService.getFileInfo(
                getPath(path, id),
                subPath ?: DEFAULT_NODE,
                fileId = fileId)
        val inputStream = if (fileObject != null) {
            repoService.retrieveFileInputStream(fileObject)
        } else {
            null
        }
        if (fileObject == null || inputStream == null) {
            log.error { "Can't download file of ${getPath(path, id)} #$fileId, because user has no access to this object or it doesn't exist." }
            return null
        }
        return Pair(fileObject, inputStream)
    }

    /**
     * @param path Unique path of data object.
     * @param id Id of data object.
     */
    @JvmOverloads
    open fun addAttachment(path: String, id: Any, fileName: String?, content: ByteArray, enableSearchIndex: Boolean, subPath: String? = null): Attachment {
        val fileObject = FileObject(
                getPath(path, id),
                subPath ?: DEFAULT_NODE,
                fileName = fileName)
        developerWarning(path, id, "addAttachment", enableSearchIndex)
        fileObject.content = content
        repoService.storeFile(fileObject, ThreadLocalUserContext.getUserId()!!.toString())
        return createAttachment(fileObject)
    }

    /**
     * @param path Unique path of data object.
     */
    @JvmOverloads
    open fun addAttachment(path: String,
                           fileName: String?,
                           content: ByteArray,
                           baseDao: BaseDao<out ExtendedBaseDO<Int>>,
                           obj: ExtendedBaseDO<Int>,
                           subPath: String? = null)
            : Attachment {
        val attachment = addAttachment(path, obj.id, fileName, content, false, subPath)
        updateAttachmentsInfo(path, baseDao, obj, subPath)
        return attachment
    }

    /**
     * @param path Unique path of data object.
     * @param id Id of data object.
     */
    @JvmOverloads
    open fun addAttachment(path: String, id: Any, fileName: String?, inputStream: InputStream, enableSearchIndex: Boolean, subPath: String? = null): Attachment {
        developerWarning(path, id, "addAttachment", enableSearchIndex)
        repoService.ensureNode(null, getPath(path, id))
        val fileObject = FileObject(getPath(path, id), subPath ?: DEFAULT_NODE, fileName = fileName)
        repoService.storeFile(fileObject, inputStream, ThreadLocalUserContext.getUserId()!!.toString())
        return createAttachment(fileObject)
    }

    /**
     * @param path Unique path of data object.
     */
    @JvmOverloads
    open fun addAttachment(path: String,
                           fileName: String?,
                           inputStream: InputStream,
                           baseDao: BaseDao<out ExtendedBaseDO<Int>>,
                           obj: ExtendedBaseDO<Int>,
                           subPath: String? = null)
            : Attachment {
        val attachment = addAttachment(path, obj.id, fileName, inputStream, false)
        updateAttachmentsInfo(path, baseDao, obj, subPath)
        if (obj is DefaultBaseDO) {
            HistoryBaseDaoAdapter.createHistoryEntry(obj, obj.id, EntityOpType.Insert, ThreadLocalUserContext.getUserId().toString(),
                    subPath ?: DEFAULT_NODE, Attachment::class.java, null, fileName)
        }
        return attachment
    }

    /**
     * @param path Unique path of data object.
     * @param id Id of data object.
     */
    @JvmOverloads
    open fun deleteAttachment(path: String, id: Any, fileId: String, enableSearchIndex: Boolean, subPath: String? = null)
            : Boolean {
        developerWarning(path, id, "deleteAttachment", enableSearchIndex)
        val fileObject = FileObject(getPath(path, id), subPath ?: DEFAULT_NODE, fileId = fileId)
        return repoService.deleteFile(fileObject)
    }

    /**
     * @param path Unique path of data object.
     */
    @JvmOverloads
    open fun deleteAttachment(path: String,
                              fileId: String,
                              baseDao: BaseDao<out ExtendedBaseDO<Int>>,
                              obj: ExtendedBaseDO<Int>,
                              subPath: String? = null)
            : Boolean {
        val fileObject = FileObject(getPath(path, obj.id), subPath ?: DEFAULT_NODE, fileId = fileId)
        val result = repoService.deleteFile(fileObject)
        if (result) {
            HistoryBaseDaoAdapter.createHistoryEntry(obj, obj.id, EntityOpType.Deleted, ThreadLocalUserContext.getUserId().toString(),
                    subPath ?: DEFAULT_NODE, Attachment::class.java, null, fileObject.fileName)
            updateAttachmentsInfo(path, baseDao, obj, subPath)
        }
        return result
    }

    /**
     * @param path Unique path of data object.
     * @param id Id of data object.
     */
    @JvmOverloads
    open fun changeFileInfo(path: String, id: Any, fileId: String, enableSearchIndex: Boolean, newFileName: String?, newDescription: String?, subPath: String? = null)
            : FileObject? {
        developerWarning(path, id, "changeProperty", enableSearchIndex)
        val fileObject = FileObject(getPath(path, id), subPath ?: DEFAULT_NODE, fileId = fileId)
        return repoService.changeFileInfo(fileObject, newFileName, newDescription)
    }

    /**
     * @param path Unique path of data object.
     */
    @JvmOverloads
    open fun changeFileInfo(path: String,
                            fileId: String,
                            baseDao: BaseDao<out ExtendedBaseDO<Int>>,
                            obj: ExtendedBaseDO<Int>,
                            newFileName: String?,
                            newDescription: String?,
                            subPath: String? = null)
            : FileObject? {
        val fileObject = FileObject(getPath(path, obj.id), subPath ?: DEFAULT_NODE, fileId = fileId)
        val result = repoService.changeFileInfo(fileObject, newFileName, newDescription)
        if (result != null) {
            if (!newFileName.isNullOrBlank()) {
                HistoryBaseDaoAdapter.createHistoryEntry(obj, obj.id, EntityOpType.Update, ThreadLocalUserContext.getUserId().toString(),
                        subPath ?: DEFAULT_NODE, Attachment::class.java, fileObject.fileName, newFileName)
            }
            if (newDescription != null) {
                HistoryBaseDaoAdapter.createHistoryEntry(obj, obj.id, EntityOpType.Update, ThreadLocalUserContext.getUserId().toString(),
                        subPath ?: DEFAULT_NODE, Attachment::class.java, fileObject.description, newDescription)
            }
            updateAttachmentsInfo(path, baseDao, obj, subPath)
        }
        return result
    }

    /**
     * Path will be path/id.
     * @return path relative to main node ProjectForge.
     */
    open fun getPath(path: String, id: Any): String {
        return "$path/$id"
    }

    private fun updateAttachmentsInfo(path: String,
                                      baseDao: BaseDao<out ExtendedBaseDO<Int>>,
                                      obj: ExtendedBaseDO<Int>,
                                      subPath: String? = null) {
        if (obj !is AttachmentsInfo) {
            return // Nothing to do.
        }
        val dbObj = baseDao.getById(obj.id)
        if (dbObj is AttachmentsInfo) {
            val attachments = getAttachments(path, obj.id, subPath)
            if (attachments != null) {
                dbObj.attachmentNames = attachments.joinToString(separator = " ") { "${it.name}" }
                dbObj.attachmentIds = attachments.joinToString(separator = " ") { "${it.fileId}" }
                dbObj.numbOfAttachments = attachments.size
            } else {
                dbObj.attachmentNames = null
                dbObj.attachmentIds = null
                dbObj.numbOfAttachments = null
            }
            baseDao.updateAny(dbObj)
        } else {
            val msg = "Can't update search index of ${dbObj::class.java.name}. Dear developer, it's not of type ${AttachmentsInfo::class.java.name}!"
            if (SystemStatus.isDevelopmentMode()) {
                throw UnsupportedOperationException(msg)
            }
            log.warn { msg }
        }
    }

    private fun createAttachment(fileObject: FileObject): Attachment {
        val attachment = Attachment(fileObject)
        NumberHelper.parseInteger(fileObject.createdByUser)?.let {
            attachment.createdByUser = UserGroupCache.tenantInstance.getUser(it)?.getFullname()
        }
        NumberHelper.parseInteger(fileObject.lastUpdateByUser)?.let {
            attachment.lastUpdateByUser = UserGroupCache.tenantInstance.getUser(it)?.getFullname()
        }
        return attachment
    }

    private fun developerWarning(path: String, id: Any, method: String, enableSearchIndex: Boolean) {
        if (enableSearchIndex) {
            val msg = "Can't update search index of ${getPath(path, id)}. Dear developer, call method '$method' with data object and baseDao instead!"
            if (SystemStatus.isDevelopmentMode()) {
                throw UnsupportedOperationException(msg)
            }
            log.warn { msg }
        }
    }

    companion object {
        const val DEFAULT_NODE = "attachments"
    }
}
