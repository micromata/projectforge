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
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
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

    open fun getAttachments(idObject: IdObject<*>, subPath: String? = null): List<Attachment>? {
        return repoService.getFileInfos(getPath(idObject), subPath ?: DEFAULT_NODE)?.map { createAttachment(it) }
    }

    open fun getAttachmentInfo(idObject: IdObject<*>, id: String? = null, fileName: String? = null, subPath: String? = null): Attachment? {
        val fileObject = repoService.getFileInfo(getPath(idObject), subPath
                ?: DEFAULT_NODE, id = id, fileName = fileName)
                ?: return null
        return createAttachment(fileObject)
    }

    open fun getAttachmentContent(idObject: IdObject<*>, id: String? = null, fileName: String? = null, subPath: String? = null): ByteArray? {
        val fileObject = repoService.getFileInfo(getPath(idObject), subPath
                ?: DEFAULT_NODE, id = id, fileName = fileName)
                ?: return null
        return if (repoService.retrieveFile(fileObject)) {
            fileObject.content
        } else {
            null
        }
    }

    open fun getAttachmentInputStream(idObject: IdObject<*>, id: String? = null, fileName: String? = null, subPath: String? = null): Pair<FileObject, InputStream>? {
        val fileObject = repoService.getFileInfo(getPath(idObject), subPath
                ?: DEFAULT_NODE, id = id, fileName = fileName)
        val inputStream = if (fileObject != null) {
            repoService.retrieveFileInputStream(fileObject)
        } else {
            null
        }
        if (fileObject == null || inputStream == null) {
            log.error { "Can't download file of ${idObject::class.java.name} #$id, because user has no access to this object or it doesn't exist." }
            return null
        }
        return Pair(fileObject, inputStream)
    }

    open fun addAttachment(idObject: IdObject<*>, fileName: String?, content: ByteArray, subPath: String? = null): Attachment {
        val fileObject = FileObject(getPath(idObject), subPath ?: DEFAULT_NODE, fileName = fileName)
        fileObject.content = content
        return createAttachment(fileObject)
    }

    open fun addAttachment(idObject: IdObject<*>, fileName: String?, inputStream: InputStream, subPath: String? = null): Attachment {
        repoService.ensureNode(null, getPath(idObject))
        val fileObject = FileObject(getPath(idObject), subPath ?: DEFAULT_NODE, fileName = fileName)
        repoService.storeFile(fileObject, inputStream, ThreadLocalUserContext.getUserId()!!.toString())
        if (idObject is DefaultBaseDO) {
            HistoryBaseDaoAdapter.createHistoryEntry(idObject, idObject.id, EntityOpType.Insert, ThreadLocalUserContext.getUserId().toString(),
                    subPath ?: DEFAULT_NODE, Attachment::class.java, null, fileName)
        }
        return createAttachment(fileObject)
    }

    open fun deleteAttachment(idObject: IdObject<*>, id: String?, subPath: String? = null): Boolean {
        val fileObject = FileObject(getPath(idObject), subPath ?: DEFAULT_NODE, id = id)
        val result = repoService.deleteFile(fileObject)
        if (result) {
            if (idObject is DefaultBaseDO) {
                HistoryBaseDaoAdapter.createHistoryEntry(idObject, idObject.id, EntityOpType.Deleted, ThreadLocalUserContext.getUserId().toString(),
                        subPath ?: DEFAULT_NODE, Attachment::class.java, null, fileObject.fileName)
            }
        }
        return result
    }

    /**
     * Path will be idObject.classname/id.
     * @return path relative to main node ProjectForge.
     */
    open fun getPath(idObject: IdObject<*>): String {
        return "${idObject::class.java.name}/${idObject.id}"
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

    companion object {
        const val DEFAULT_NODE = "attachments"
    }
}
