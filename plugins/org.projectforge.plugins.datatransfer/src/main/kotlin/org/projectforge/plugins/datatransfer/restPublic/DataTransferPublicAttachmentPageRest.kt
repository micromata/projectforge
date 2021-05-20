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

package org.projectforge.plugins.datatransfer.restPublic

import mu.KotlinLogging
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.DataTransferPlugin
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.projectforge.rest.AttachmentPageRest
import org.projectforge.rest.AttachmentsServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * For external anonymous usage via token/password.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/datatransferattachment")
class DataTransferPublicAttachmentPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var services: AttachmentsServicesRest

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  private lateinit var dataTransferPublicAccessChecker: DataTransferPublicAccessChecker

  @PostConstruct
  private fun postConstruct() {
    val baseDao = dataTransferAreaPagesRest.baseDao
    dataTransferPublicAccessChecker = DataTransferPublicAccessChecker(baseDao)
  }

  /**
   * The react path of this should look like: 'react/attachment/dynamic/42?category=contract...'
   * @param id: Id of data object with attachments.
   * @param category [DataTransferPlugin.ID] ("datatransfer") expected
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
    check(category == DataTransferPlugin.ID)
    // services.getDataObject(pagesRest, id) // Check data object availability.
    val data = AttachmentsServicesRest.AttachmentData(category = category, id = id, fileId = fileId, listId = listId)
    data.attachment = services.getAttachment(dataTransferAreaPagesRest.jcrPath!!, dataTransferPublicAccessChecker, data)

    val layout = AttachmentPageRest.createAttachmentLayout(id, category, fileId, listId, data.attachment)
    return FormLayoutData(data, layout, createServerData(request))
  }
}
