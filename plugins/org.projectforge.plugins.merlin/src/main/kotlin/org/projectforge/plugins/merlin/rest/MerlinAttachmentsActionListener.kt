/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.merlin.rest

import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.jcr.FileInfo
import org.projectforge.plugins.merlin.MerlinTemplateDO
import org.projectforge.plugins.merlin.MerlinTemplateDao
import org.projectforge.rest.AttachmentsActionListener
import org.projectforge.rest.AttachmentsServicesRest
import org.projectforge.ui.*
import org.springframework.http.ResponseEntity

// private val log = KotlinLogging.logger {}

/**
 * Listener on template changes.
 */
class MerlinAttachmentsActionListener(
  attachmentsService: AttachmentsService,
  private val baseDao: MerlinTemplateDao,
) :
  AttachmentsActionListener(attachmentsService, allowDuplicateFiles = true) {

  /**
   * Allows only upload of Word and Excel documents.
   */
  override fun onBeforeUpload(fileInfo: FileInfo, obj: ExtendedBaseDO<Int>): ResponseEntity<*>? {
    return if (fileInfo.fileExtension != "docx" && fileInfo.fileExtension != "xlsx") {
      ResponseEntity.ok().body(
        UIToast.createToast(
          translateMsg("plugins.merlin.upload.unsupportedUploadFormat", fileInfo.fileName),
          color = UIColor.DANGER
        )
      )
    } else {
      null
    }
  }

  /**
   * Renames all existing docx and xlsx files to backup files.
   */
  override fun afterUpload(
    attachment: Attachment,
    obj: ExtendedBaseDO<Int>,
    jcrPath: String,
    attachmentsAccessChecker: AttachmentsAccessChecker,
    listId: String?
  ): ResponseEntity<*> {
    val list = attachmentsService.getAttachments(jcrPath, obj.id, attachmentsAccessChecker, listId)
    list?.forEach { element ->
      // If docx/xlsx is uploaded, backup all previous existing docx/xlsx.
      if (element.fileExtension == attachment.fileExtension && element.fileId != attachment.fileId) {
        // docx and not the current uploaded file. So rename all other docx elements as backup-files.
        val newFileName = "${element.name}.backup"
        attachmentsService.changeFileInfo(
          jcrPath,
          element.fileId!!,
          baseDao = baseDao,
          obj,
          newFileName = newFileName,
          newDescription = null,
          accessChecker = attachmentsAccessChecker,
          updateLastUpdateInfo = false,
        )
      }
    }

    return createResponseEntity(obj, list, TargetType.UPDATE)
  }

  override fun afterModification(
    attachment: Attachment,
    obj: ExtendedBaseDO<Int>,
    jcrPath: String,
    attachmentsAccessChecker: AttachmentsAccessChecker,
    listId: String?
  ): ResponseEntity<*> {
    val list = attachmentsService.getAttachments(jcrPath, obj.id, attachmentsAccessChecker, listId)
    return createResponseEntity(obj, list, TargetType.CLOSE_MODAL)
  }

  private fun createResponseEntity(
    obj: ExtendedBaseDO<Int>,
    list: List<Attachment>?,
    targetType: TargetType
  ): ResponseEntity<*> {
    val result = MerlinPagesRest.updateLayoutAndData(obj as MerlinTemplateDO)
    val dto = result.second
    val ui = result.first
    val data = mapOf(
      "attachments" to list,
      "wordTemplateFileName" to (dto.wordTemplateFileName ?: "---"),
    )
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = targetType, merge = true)
          .addVariable("data", data)
          .addVariable("ui", ui)
      )
  }

  override fun createAttachmentLayout(
    id: Int,
    category: String,
    fileId: String,
    listId: String?,
    attachment: Attachment,
    writeAccess: Boolean,
    restClass: Class<*>,
    encryptionSupport: Boolean,
    data: AttachmentsServicesRest.AttachmentData?
  ): UILayout {
    return super.createAttachmentLayout(
      id,
      category,
      fileId,
      listId,
      attachment,
      writeAccess,
      restClass,
      encryptionSupport = false,
      data = data,
    )
  }
}
