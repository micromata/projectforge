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

import mu.KotlinLogging
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.jcr.FileObject
import org.projectforge.jcr.RepoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * Service for handling attachments of DO's.
 */
@Service
open class AttachmentsService {
    @Autowired
    private lateinit var repoService: RepoService

    open fun getAttachments(baseDO: BaseDO<*>, subPath: String? = null): List<Attachment>? {
        return repoService.getFileInfos(getPath(baseDO), subPath)?.map { Attachment(it) }
    }

    open fun getAttachmentInfo(baseDO: BaseDO<*>, subPath: String? = null, id: String? = null, name: String? = null): Attachment? {
        val fileObject = repoService.getFileInfo(getPath(baseDO), subPath, id = id, fileName = name) ?: return null
        return Attachment(fileObject)
    }

    open fun getAttachmentContent(baseDO: BaseDO<*>, subPath: String? = null, id: String? = null, name: String? = null): ByteArray? {
        val fileObject = repoService.getFileInfo(getPath(baseDO), subPath, id = id, fileName = name) ?: return null
        return if (repoService.retrieveFile(fileObject)) {
            fileObject.content
        } else {
            null
        }
    }

    open fun getAttachmentInputStream(baseDO: BaseDO<*>, subPath: String? = null, id: String? = null, name: String? = null): InputStream? {
        val fileObject = repoService.getFileInfo(getPath(baseDO), subPath, id = id, fileName = name) ?: return null
        return repoService.retrieveFileInputStream(fileObject)
    }

    open fun addAttachment(baseDO: BaseDO<*>, subPath: String? = null, name: String, content: ByteArray): Attachment {
        val fileObject = FileObject(getPath(baseDO), subPath, fileName = name)
        fileObject.content = content
        return Attachment(fileObject)

    }

    open fun addAttachment(baseDO: BaseDO<*>, subPath: String? = null, name: String, content: InputStream): Attachment {
        val fileObject = FileObject(getPath(baseDO), subPath, fileName = name)
        repoService.storeFile(fileObject, content)
        return Attachment(fileObject)
    }

    open fun deleteAttachment(baseDO: BaseDO<*>, subPath: String? = null, id: String?): Boolean {
        val fileObject = FileObject(getPath(baseDO), subPath, id = id)
        return repoService.deleteFile(fileObject)
    }

    /**
     * Path will be baseDO.classname/id.
     * @return path relative to main node ProjectForge.
     */
    open fun getPath(baseDO: BaseDO<*>): String {
        return "${baseDO::class.java.name}/${baseDO.id}"
    }
}
