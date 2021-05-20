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
import org.projectforge.business.login.LoginResultStatus
import org.projectforge.framework.api.TechnicalException
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.jcr.FileInfo
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.DataTransferPlugin
import org.projectforge.plugins.datatransfer.NotificationMailService
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.projectforge.plugins.datatransfer.rest.DataTransferlUtils
import org.projectforge.rest.AttachmentsServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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
  private lateinit var dataTransferAreaPagesRest: DataTransferAreaPagesRest

  @Autowired
  private lateinit var notificationMailService: NotificationMailService

  private lateinit var attachmentsAccessChecker: DataTransferPublicAccessChecker

  @PostConstruct
  private fun postConstruct() {
    attachmentsAccessChecker = DataTransferPublicAccessChecker(dataTransferAreaDao)
  }

  /**
   * @param category [DataTransferPlugin.ID] ("datatransfer") expected
   * @param userInfo See [org.projectorge.plugins.DataTransferPubli
   */
  @GetMapping("download/{category}/{id}")
  fun download(
    request: HttpServletRequest,
    @PathVariable("category", required = true) category: String,
    @PathVariable("id", required = true) id: Int,
    @RequestParam("fileId", required = true) fileId: String,
    @RequestParam("listId") listId: String?,
    @RequestParam("accessString") accessString: String?,
    @RequestParam("userInfo") userInfo: String?
  )
      : ResponseEntity<*> {
    log.info {
      "User tries to download attachment: category=$category, id=$id, fileId=$fileId, listId=$listId)}, user='${
        getExternalUserString(
          request,
          userInfo
        )
      }'."
    }
    val checkResult = checkAccess(request, category, id, accessString, userInfo)
    checkResult.second?.let { return it }
    val result =
      attachmentsService.getAttachmentInputStream(
        dataTransferAreaPagesRest.jcrPath!!,
        id,
        fileId,
        attachmentsAccessChecker,
        data = checkResult.first,
        attachmentsEventListener = dataTransferAreaDao,
        userString = getExternalUserString(request, userInfo)
      )
        ?: throw TechnicalException(
          "File to download not accessible for user or not found: category=$category, id=$id, fileId=$fileId, listId=$listId)}."
        )
    if (checkResult.first?.externalDownloadEnabled != true) {
      val clientIp = RestUtils.getClientIp(request) ?: "NO IP ADDRESS GIVEN. CAN'T SHOW ANY ATTACHMENT."
      if (result.first.createdByUser?.contains(clientIp) != true) {
        return RestUtils.badRequest("Download not enabled.")
      }
    }
    val filename = result.first.fileName ?: "file"
    val inputStream = result.second
    return RestUtils.downloadFile(filename, inputStream)
  }

  @GetMapping("downloadAll/{category}/{id}")
  fun downloadAll(
    request: HttpServletRequest,
    response: HttpServletResponse,
    @PathVariable("category", required = true) category: String,
    @PathVariable("id", required = true) id: Int,
    @RequestParam("accessString") accessString: String?,
    @RequestParam("userInfo") userInfo: String?
  ): ResponseEntity<*>? {
    log.info {
      "User tries to download all attachments: category=$category, id=$id}, user='${
        getExternalUserString(
          request,
          userInfo
        )
      }'."
    }
    val checkResult = checkAccess(request, category, id, accessString, userInfo)
    checkResult.second?.let { return it }
    val dbObj = checkResult.first!!
    val dto = convert(request, dbObj, userInfo)
    DataTransferlUtils.downloadAll(
      response,
      attachmentsService,
      attachmentsAccessChecker,
      notificationMailService,
      dbObj,
      dto.areaName,
      jcrPath = dataTransferAreaPagesRest.jcrPath!!,
      id,
      dto.attachments,
      byExternalUser = getExternalUserString(request, userInfo)
    )
    return null
  }

  @PostMapping("upload/{category}/{id}/{listId}")
  fun uploadAttachment(
    request: HttpServletRequest,
    @PathVariable("category", required = true) category: String,
    @PathVariable("id", required = true) id: Int,
    @PathVariable("listId") listId: String?,
    @RequestParam("file") file: MultipartFile,
    @RequestParam("accessString") accessString: String?,
    @RequestParam("userInfo") userInfo: String?
  )
  //@RequestParam("files") files: Array<MultipartFile>)
      : ResponseEntity<*>? {
    //files.forEach { file ->
    val filename = file.originalFilename
    log.info {
      "User tries to upload attachment: id='$id', filename='$filename', page='${this::class.java.name}', user='${
        getExternalUserString(
          request,
          userInfo
        )
      }'."
    }

    checkAccess(request, category, id, accessString, userInfo).second?.let { return it }

    val obj = dataTransferAreaDao.internalGetById(id)
      ?: throw TechnicalException(
        "Entity with id $id not accessible for category '$category' or doesn't exist.",
        "Entity with id unknown."
      )

    if (obj.externalUploadEnabled != true) {
      return RestUtils.badRequest("Upload not enabled.")
    }

    attachmentsService.addAttachment(
      dataTransferAreaPagesRest.jcrPath!!,
      fileInfo = FileInfo(file.originalFilename, fileSize = file.size),
      inputStream = file.inputStream,
      baseDao = dataTransferAreaDao,
      obj = obj,
      accessChecker = attachmentsAccessChecker,
      userString = getExternalUserString(request, userInfo)
    )
    //}
    val list =
      attachmentsAccessChecker.filterAttachments(
        request,
        obj.externalDownloadEnabled,
        attachmentsService.getAttachments(dataTransferAreaPagesRest.jcrPath!!, id, attachmentsAccessChecker, null)
      )
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = TargetType.UPDATE, merge = true)
          .addVariable("data", AttachmentsServicesRest.ResponseData(list))
      )
  }

  private fun checkAccess(
    request: HttpServletRequest,
    category: String,
    id: Int,
    accessString: String?,
    userInfo: String?
  ): Pair<DataTransferAreaDO?, ResponseEntity<String>?> {
    check(category == DataTransferPlugin.ID)
    val credentials = DataTransferlUtils.splitAccessString(accessString)
    val externalAccessToken = credentials.first
    val externalPassword = credentials.second
    val checkAccess =
      attachmentsAccessChecker.checkExternalAccess(
        dataTransferAreaDao,
        request,
        externalAccessToken,
        externalPassword,
        userInfo
      )
    checkAccess.second?.let {
      return Pair(
        null, ResponseEntity.badRequest()
          .contentType(MediaType("text", "plain", StandardCharsets.UTF_8))
          .body(it)
      )
    }
    val dbo = checkAccess.first!!
    if (dbo.id != id) {
      log.warn { "User tries to use data transfer area by id different from access token!!!" }
      return Pair(
        null, ResponseEntity.badRequest()
          .contentType(MediaType("text", "plain", StandardCharsets.UTF_8))
          .body(LoginResultStatus.FAILED.localizedMessage)
      )
    }
    return Pair(checkAccess.first, null)
  }

  private fun getExternalUserString(request: HttpServletRequest, userString: String?): String {
    return "external: ${RestUtils.getClientIp(request)} ('${userString?.take(255)}')"
  }

  internal fun convert(
    request: HttpServletRequest,
    dbo: DataTransferAreaDO,
    userInfo: String?
  ): DataTransferPublicArea {
    val dto = DataTransferPublicArea()
    dto.copyFrom(dbo)
    dto.attachments = attachmentsAccessChecker.filterAttachments(
      request, dto.externalDownloadEnabled,
      attachmentsService.getAttachments(
        dataTransferAreaPagesRest.jcrPath!!,
        dto.id!!,
        attachmentsAccessChecker
      )
    )
    dto.userInfo = userInfo
    return dto
  }
}
