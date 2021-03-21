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
import org.projectforge.framework.api.TechnicalException
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.projectforge.rest.config.Rest
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.nio.charset.StandardCharsets
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * For external anonymous usage via token/password.
 */
@RestController
@RequestMapping("${Rest.PUBLIC_URL}/datatransfer")
class DataTransferPublicServicesRest {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  @Autowired
  private lateinit var dataTransferAreaDao: DataTransferAreaDao

  @Autowired
  private lateinit var dataTransferAreaPageRest: DataTransferAreaPagesRest

  private lateinit var attachmentsAccessChecker: DataTransferPublicAccessChecker

  @PostConstruct
  private fun postConstruct() {
    attachmentsAccessChecker =
      DataTransferPublicAccessChecker(
        dataTransferAreaDao.maxFileSize.toBytes(),
        DataTransferAreaDao.MAX_FILE_SIZE_SPRING_PROPERTY
      )
  }

  @GetMapping("download/{category}/{id}")
  fun download(
    request: HttpServletRequest,
    @PathVariable("category", required = true) category: String,
    @PathVariable("id", required = true) id: Int,
    @RequestParam("fileId", required = true) fileId: String,
    @RequestParam("listId") listId: String?,
    @RequestParam("accessString") accessString: String?
  )
      : ResponseEntity<*> {
    check(category == "datatransfer")
    check(accessString?.contains('|') == true)
    val credentials = accessString!!.split('|')
    check(credentials.size == 2)
    log.info { "User tries to download attachment: category=$category, id=$id, fileId=$fileId, listId=$listId)}." }
    val externalAccessToken = credentials[0]
    val externalPassword = credentials[1]
    val checkAccess =
      attachmentsAccessChecker.checkExternalAccess(dataTransferAreaDao, request, externalAccessToken, externalPassword)
    checkAccess.errorMsg?.let {
      return ResponseEntity.badRequest()
        .contentType(MediaType("text", "plain", StandardCharsets.UTF_8))
        .body(it)
    }
    val data = checkAccess.data!!

    val result =
      attachmentsService.getAttachmentInputStream(dataTransferAreaPageRest.jcrPath!!, id, fileId, attachmentsAccessChecker)
        ?: throw TechnicalException(
          "File to download not accessible for user or not found: category=$category, id=$id, fileId=$fileId, listId=$listId)}."
        )

    val filename = result.first.fileName ?: "file"
    val inputStream = result.second
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("application/octet-stream"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${filename.replace('"', '_')}\"")
      .body(InputStreamResource(inputStream))
  }
}
