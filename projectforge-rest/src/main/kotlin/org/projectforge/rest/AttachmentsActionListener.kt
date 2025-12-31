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

package org.projectforge.rest

import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.jcr.FileInfo
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.UILayout
import org.springframework.http.ResponseEntity

/**
 * Listener to register. Will be called on actions by attachment service ([AttachmentsServicesRest]).
 */
open class AttachmentsActionListener(
    val attachmentsService: AttachmentsService,
    val allowDuplicateFiles: Boolean = false
) {

    /**
     * Will be called on upload. If ResponseEntity is returned, further processing of this upload will be prevented.
     * Useful e. g. to allow only special file extensions etc.
     */
    open fun onBeforeUpload(fileInfo: FileInfo, obj: ExtendedBaseDO<Long>): ResponseEntity<*>? {
        return null
    }

    /**
     * Will be called after upload.
     */
    open fun afterUpload(
        attachment: Attachment,
        obj: ExtendedBaseDO<Long>,
        jcrPath: String,
        attachmentsAccessChecker: AttachmentsAccessChecker,
        listId: String?
    ): ResponseEntity<*> {
        return ResponseEntity.ok()
            .body(
                ResponseAction(targetType = TargetType.UPDATE, merge = true)
                    .addVariable("data", createResponseData(obj, jcrPath, attachmentsAccessChecker, listId))
            )
    }

    /**
     * Will be called after upload.
     */
    open fun afterModification(
        attachment: Attachment,
        obj: ExtendedBaseDO<Long>,
        jcrPath: String,
        attachmentsAccessChecker: AttachmentsAccessChecker,
        listId: String?
    ): ResponseEntity<*> {
        return ResponseEntity.ok()
            .body(
                ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
                    .addVariable("data", createResponseData(obj, jcrPath, attachmentsAccessChecker, listId))
            )
    }

    /**
     * Will be called after upload.
     */
    open fun afterDeletion(
        obj: ExtendedBaseDO<Long>,
        jcrPath: String,
        attachmentsAccessChecker: AttachmentsAccessChecker,
        listId: String?
    ): ResponseEntity<*> {
        return ResponseEntity.ok()
            .body(
                ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
                    .addVariable("data", createResponseData(obj, jcrPath, attachmentsAccessChecker, listId))
            )
    }

    /**
     * Calls [AttachmentPageRest.createAttachmentLayout] at default. Override this, if you want to manipulate the layout.
     * As an example MerlinAttachmentsActionListener disables encryptionSupport.
     */
    open fun createAttachmentLayout(
        id: Long,
        category: String,
        fileId: String,
        listId: String?,
        attachment: Attachment,
        writeAccess: Boolean = true,
        restClass: Class<*> = AttachmentsServicesRest::class.java,
        encryptionSupport: Boolean = false,
        data: AttachmentsServicesRest.AttachmentData? = null,
    ): UILayout {
        return AttachmentPageRest.createAttachmentLayout(
            id = id,
            category = category,
            listId = listId,
            attachment = attachment,
            writeAccess = writeAccess,
            restClass = restClass,
            encryptionSupport = encryptionSupport,
            data = data
        )
    }

    open fun createResponseData(
        obj: ExtendedBaseDO<Long>,
        jcrPath: String,
        attachmentsAccessChecker: AttachmentsAccessChecker,
        listId: String?,
    ): Any {
        val list = attachmentsService.getAttachments(jcrPath, obj.id!!, attachmentsAccessChecker, listId) ?: emptyList()
        return AttachmentsServicesRest.ResponseData(list)
    }

    open fun createDownloadBasefileName(obj: Any): String {
        return "download"
    }
}
