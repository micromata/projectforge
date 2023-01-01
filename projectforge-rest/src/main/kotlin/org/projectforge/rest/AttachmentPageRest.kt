/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.Attachment
import org.projectforge.jcr.ZipMode
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

private val log = KotlinLogging.logger {}

/**
 * Modal dialog showing details of an attachment with the functionality to download, modify and delete it.
 */
@RestController
@RequestMapping("${Rest.URL}/attachment")
class AttachmentPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var services: AttachmentsServicesRest

  /**
   * Returns the form for a single attachment, including file properties as well as editable properties such
   * as file name and description.
   * The form supports also the buttons: download, delete and update.
   * The react path of this should look like: 'react/attachment/dynamic/42?category=contract...'
   * @param id: Id of data object with attachments.
   */
  @GetMapping("dynamic")
  fun getForm(
    @RequestParam("id", required = true) id: Int,
    @RequestParam("category", required = true) category: String,
    @RequestParam("fileId", required = true) fileId: String,
    @RequestParam("listId") listId: String?,
    request: HttpServletRequest
  ): FormLayoutData {
    log.info { "User tries to edit/view details of attachment: category='$category', id='$id', listId='$listId', fileId='$fileId', page='${this::class.java.name}'." }
    val pagesRest = services.getPagesRest(category, listId)
    services.getDataObject(pagesRest, id) // Check data object availability.
    val data = AttachmentsServicesRest.AttachmentData(category = category, id = id, fileId = fileId, listId = listId)
    data.attachment = services.getAttachment(pagesRest, data)
    val actionListener = services.getListener(category)
    val layout = actionListener.createAttachmentLayout(
      id,
      category,
      fileId,
      listId,
      attachment = data.attachment,
      encryptionSupport = true,
      data = data,
    )
    return FormLayoutData(data, layout, createServerData(request))
  }

  /**
   * Will be called, if the user wants to see the encryption options.
   */
  @PostMapping(RestPaths.WATCH_FIELDS)
  fun watchFields(@Valid @RequestBody postData: PostData<AttachmentsServicesRest.AttachmentData>): ResponseEntity<ResponseAction> {
    val data = postData.data
    // write access is always true, otherwise watch field wasn't registered.
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable(
          "ui",
          createAttachmentLayout(
            data.id,
            data.category,
            data.fileId,
            data.listId,
            data.attachment,
            writeAccess = true,
            encryptionSupport = true,
            data = data
          )
        )
        .addVariable("data", data)
    )
  }


  companion object {
    fun createAttachmentLayout(
      id: Int,
      category: String,
      fileId: String,
      listId: String?,
      attachment: Attachment,
      writeAccess: Boolean = true,
      restClass: Class<*> = AttachmentsServicesRest::class.java,
      encryptionSupport: Boolean = false,
      data: AttachmentsServicesRest.AttachmentData? = null,
    ): UILayout {
      val layout = UILayout("attachment")

      val info = mutableMapOf<String, Any?>()
      attachment.info = info

      attachment.zipMode?.let {
        info["encryptionStatus"] = translate(it.i18nKey)
      }

      val lc = LayoutContext(Attachment::class.java)

      layout
        .add(lc, "attachment.name")
        .add(UITextArea("attachment.description", lc))
        .add(
          UIRow()
            .add(
              UICol(UILength(md = 6))
                .add(UIReadOnlyField("attachment.sizeHumanReadable", label = "attachment.fileSize"))
            )
            .add(
              UICol(UILength(md = 6))
                .add(UIReadOnlyField("attachment.info.encryptionStatus", label = "attachment.info"))
            )
        )
        .add(
          UIRow()
            .add(
              UICol(UILength(md = 6))
                .add(UIReadOnlyField("attachment.createdFormatted", label = "created"))
                .add(UIReadOnlyField("attachment.createdByUser", label = "createdBy"))
            )
            .add(
              UICol(UILength(md = 6))
                .add(UIReadOnlyField("attachment.lastUpdateTimeAgo", label = "modified"))
                .add(UIReadOnlyField("attachment.lastUpdateByUser", label = "modifiedBy"))
            )
        )
      if (encryptionSupport && writeAccess) {
        if (attachment.encrypted || data!!.showEncryptionOption == true) {
          addShowEncryptionOption(layout, attachment, lc, restClass)
        } else {
          addShowEncryptionCheckbox(layout)
        }
      }
      if (!attachment.checksum.isNullOrBlank()) {
        layout.add(UIReadOnlyField("attachment.checksum", label = "attachment.checksum", canCopy = true))
      }

      if (writeAccess) {
        layout.addAction(
          UIButton.createDeleteButton(
            layout,
            confirmMessage = "file.panel.deleteExistingFile.heading",
            responseAction = ResponseAction(
              RestResolver.getRestUrl(restClass, "delete"),
              targetType = TargetType.POST
            )
          )
        )
        layout.addAction(
          UIButton.createDefaultButton(
            "update",
            title = "update",
            responseAction = ResponseAction(
              RestResolver.getRestUrl(restClass, "modify"),
              targetType = TargetType.POST
            ),
          )
        )
      }
      layout.addAction(
        UIButton.createDownloadButton(
          responseAction = ResponseAction(
            RestResolver.getRestUrl(
              restClass,
              AttachmentsServicesRest.getDownloadUrl(attachment, category = category, id = id, listId = listId)
            ),
            targetType = TargetType.DOWNLOAD
          )
        )
      )
      LayoutUtils.process(layout)
      return layout
    }

    private fun addShowEncryptionOption(
      layout: UILayout,
      attachment: Attachment,
      lc: LayoutContext,
      restClass: Class<*>
    ) {
      // User wants to see encryption options.
      val algoCol = UICol(UILength(md = 6))
      if (!attachment.encrypted) {
        // Show encryption algorithms only, if not yet encrypted.
        attachment.newZipMode = ZipMode.ENCRYPTED_AES256
        val algoSelect = UISelect(
          "attachment.newZipMode",
          layoutContext = lc,
          values = listOf(
            UISelectValue(ZipMode.ENCRYPTED_STANDARD.name, translate(ZipMode.ENCRYPTED_STANDARD.i18nKey)),
            UISelectValue(ZipMode.ENCRYPTED_AES256.name, translate(ZipMode.ENCRYPTED_AES256.i18nKey)),
          ),
        )
        algoCol.add(algoSelect)
      }
      val function = if (attachment.encrypted) "testDecryption" else "encrypt"
      layout.add(
        UIRow()
          .add(algoCol)
          .add(
            UICol(UILength(md = 3))
              .add(
                // Show password for encryption or for test encryption:
                UIInput(
                  "attachment.password",
                  label = "password",
                  tooltip = "attachment.password.info",
                  dataType = UIDataType.PASSWORD,
                  autoComplete = UIInput.AutoCompleteType.OFF,
                )
              )
          )
          .add(
            UICol(UILength(md = 3))
              .add(
                UIButton.createSecondaryButton(
                  layout,
                  responseAction = ResponseAction(
                    RestResolver.getRestUrl(restClass, function),
                    targetType = TargetType.POST
                  ),
                  id = function,
                  title = "attachment.$function",
                  confirmMessage = if (!attachment.encrypted) "attachment.encrypt.question" else null
                )
              )
          )
      )
    }

    private fun addShowEncryptionCheckbox(layout: UILayout) {
      // User doesn't want to see encryption options (yet). Show only checkbox for displaying options:
      layout.add(
        UIRow()
          .add(
            UICol(6)
          )
          .add(
            UICol()
              .add(
                // Show password for encryption or for decryption:
                UICheckbox(
                  "showEncryptionOption",
                  label = "attachment.showEncryptionOption",
                )
              )
          )
      )
      layout.watchFields.add("showEncryptionOption")
    }
  }
}
