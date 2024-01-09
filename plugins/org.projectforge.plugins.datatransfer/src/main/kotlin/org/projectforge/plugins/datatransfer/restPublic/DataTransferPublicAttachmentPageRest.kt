/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.jcr.AttachmentsService
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
@RequestMapping("${Rest.PUBLIC_URL}/attachment")
class DataTransferPublicAttachmentPageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var services: AttachmentsServicesRest

  @Autowired
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  @Autowired
  private lateinit var dataTransferPublicServicesRest: DataTransferPublicServicesRest

  @Autowired
  private lateinit var dataTransferPublicSession: DataTransferPublicSession

  private lateinit var dataTransferPublicAccessChecker: DataTransferPublicAccessChecker

  @PostConstruct
  private fun postConstruct() {
    dataTransferPublicAccessChecker = DataTransferPublicAccessChecker(dataTransferPublicSession)
  }

  /**
   * Fails, if the user has no session.
   */
  @GetMapping("dynamic")
  fun getForm(
    @RequestParam("id", required = true) id: Int,
    @RequestParam("category", required = true) category: String,
    @RequestParam("fileId", required = true) fileId: String,
    @RequestParam("listId") listId: String?,
    request: HttpServletRequest
  ): FormLayoutData {
    check(category == DataTransferPlugin.ID)
    check(listId == AttachmentsService.DEFAULT_NODE)
    val data = dataTransferPublicSession.checkLogin(request, id) ?: throw IllegalArgumentException("No valid login.")
    val area = data.first
    val sessionData = data.second

    log.info {
      "User tries to edit/view details of attachment: category=$category, id=$id, fileId=$fileId, listId=$listId)}, user='${
        DataTransferAreaDao.getExternalUserString(request, sessionData.userInfo)
      }'."
    }
    val attachmentData =
      AttachmentsServicesRest.AttachmentData(category = category, id = id, fileId = fileId, listId = listId)
    attachmentData.attachment =
      services.getAttachment(dataTransferAreaPagesRest.jcrPath!!, dataTransferPublicAccessChecker, attachmentData)
    val layout = AttachmentPageRest.createAttachmentLayout(
      id,
      category,
      fileId,
      listId,
      attachmentData.attachment,
      writeAccess = dataTransferPublicAccessChecker.hasUpdateAccess(request, area, fileId),
      restClass = DataTransferPublicServicesRest::class.java
    )
    return FormLayoutData(attachmentData, layout, createServerData(request))
  }
}
