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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.Attachment
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

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
    val layout = createAttachmentLayout(id, category, fileId, listId)
    return FormLayoutData(data, layout, createServerData(request))
  }

  companion object {
    fun createAttachmentLayout(id: Int, category: String, fileId: String, listId: String?): UILayout {
      val layout = UILayout("attachment")

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
                .add(UIReadOnlyField("attachment.fileId", label = "attachment.fileId"))
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
                .add(UIReadOnlyField("attachment.lastUpdateFormatted", label = "modified"))
                .add(UIReadOnlyField("attachment.lastUpdateByUser", label = "modifiedBy"))
            )
        )

      layout.addAction(
        UIButton(
          "download",
          translate("download"),
          UIColor.LINK,
          responseAction = ResponseAction(
            RestResolver.getRestUrl(
              AttachmentsServicesRest::class.java,
              "download/$category/$id?fileId=$fileId&listId=$listId"
            ),
            targetType = TargetType.DOWNLOAD
          ),
          default = true
        )
      )
      layout.addAction(
        UIButton(
          "delete",
          translate("delete"),
          UIColor.DANGER,
          confirmMessage = translate("file.panel.deleteExistingFile.heading"),
          responseAction = ResponseAction(
            RestResolver.getRestUrl(AttachmentsServicesRest::class.java, "delete"),
            targetType = TargetType.POST
          ),
          default = true
        )
      )
        .addAction(
          UIButton(
            "update",
            translate("update"),
            UIColor.SUCCESS,
            responseAction = ResponseAction(
              RestResolver.getRestUrl(AttachmentsServicesRest::class.java, "modify"),
              targetType = TargetType.POST
            ),
            default = true
          )
        )
      LayoutUtils.process(layout)
      return layout
    }
  }
}
