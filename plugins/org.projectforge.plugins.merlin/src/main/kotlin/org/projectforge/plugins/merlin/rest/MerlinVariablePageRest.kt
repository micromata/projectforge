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

import mu.KotlinLogging
import org.projectforge.plugins.merlin.MerlinTemplate
import org.projectforge.plugins.merlin.MerlinTemplateDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.UILayout
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
@RequestMapping("${Rest.URL}/merlin")
class MerlinVariablePageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var merlinTemplateDao: MerlinTemplateDao

  /**
   * For editing variables.
   */
  @PostMapping("editVariable/{variable}")
  fun getForm(
    @PathVariable("variable", required = true) variable: String,
    @Valid @RequestBody dto: MerlinTemplate, request: HttpServletRequest
  ): ResponseEntity<*> {
    log.info { "To be implement: Editing of variable '$variable'." }
    dto.name = "Hurra"
/*    merlinTemplateDao.
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
    )*/
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE)
        .addVariable("data", dto)
    )
  }

  /**
   * Will be called, if the user wants to see the encryption options.
   */
/*  @PostMapping(RestPaths.WATCH_FIELDS)
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

*/
}
