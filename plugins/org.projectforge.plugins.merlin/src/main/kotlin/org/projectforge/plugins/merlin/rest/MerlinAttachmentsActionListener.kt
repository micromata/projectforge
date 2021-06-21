/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.utils.ReplaceUtils
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.jcr.FileInfo
import org.projectforge.plugins.merlin.MerlinTemplateDao
import org.projectforge.rest.AttachmentsActionListener
import org.projectforge.rest.AttachmentsServicesRest
import org.projectforge.ui.UIColor
import org.projectforge.ui.UILayout
import org.projectforge.ui.UIToast
import org.springframework.http.ResponseEntity

class MerlinAttachmentsActionListener(val attachmentsService: AttachmentsService, val baseDao: MerlinTemplateDao) :
  AttachmentsActionListener(allowDuplicateFiles = true) {

  /**
   * Allows only upload of Word and Excel documents.
   */
  override fun onUpload(fileInfo: FileInfo, obj: ExtendedBaseDO<Int>): ResponseEntity<*>? {
    return if (fileInfo.fileExtension != "docx") {
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
   * Renames all existing docx files to backup files.
   */
  override fun afterUpload(attachment: Attachment, obj: ExtendedBaseDO<Int>, jcrPath: String, attachmentsAccessChecker: AttachmentsAccessChecker, listId: String?) {
    val list = attachmentsService.getAttachments(jcrPath, obj.id, attachmentsAccessChecker, listId)
    val date = ReplaceUtils.encodeFilename(PFDateTime.now().isoString, reducedCharsOnly = true)
    list?.forEach { element ->
      if (element.fileExtension == "docx" && element.fileId != attachment.fileId) {
        // docx and not the current uploaded file. So rename all other docx elements as backup-files.
        val newFileName = "${element.name}-$date.bak"
        attachmentsService.changeFileInfo(jcrPath, element.fileId!!, baseDao = baseDao, obj, newFileName = newFileName, newDescription = null, accessChecker = attachmentsAccessChecker)
      }
    }
    attachmentsService.getAttachmentInputStream(jcrPath, obj.id, attachment.fileId!!, accessChecker = attachmentsAccessChecker, listId)
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
