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
import org.projectforge.rest.dto.PostData
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
   * User must be logged in before (the accessToken and external password of the user's session are used).
   * @param category [DataTransferPlugin.ID] ("datatransfer") expected
   */
  @GetMapping("download/{category}/{id}")
  fun download(
    request: HttpServletRequest,
    @PathVariable("category", required = true) category: String,
    @PathVariable("id", required = true) id: Int,
    @RequestParam("fileId", required = true) fileId: String,
    @RequestParam("listId") listId: String?,
  )
      : ResponseEntity<*> {
    check(listId == AttachmentsService.DEFAULT_NODE)
    val checkResult = checkAccess(request, category, id)
    checkResult.failedAccess?.let { return it }
    log.info {
      "User tries to download attachment: category=$category, id=$id, fileId=$fileId, listId=$listId)}, user='${
        getExternalUserString(
          request,
          checkResult.userInfo
        )
      }'."
    }
    val result =
      attachmentsService.getAttachmentInputStream(
        dataTransferAreaPagesRest.jcrPath!!,
        id,
        fileId,
        attachmentsAccessChecker,
        data = checkResult.dataTransferArea,
        attachmentsEventListener = dataTransferAreaDao,
        userString = getExternalUserString(request, checkResult.userInfo)
      )
        ?: throw TechnicalException(
          "File to download not accessible for user or not found: category=$category, id=$id, fileId=$fileId, listId=$listId)}."
        )
    if (checkResult.dataTransferArea?.externalDownloadEnabled != true) {
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
  ): ResponseEntity<*>? {
    //Session check as alternative
    val checkResult = checkAccess(request, category, id)
    checkResult.failedAccess?.let { return it }
    log.info {
      "User tries to download all attachments: category=$category, id=$id}, user='${
        getExternalUserString(
          request,
          checkResult.userInfo
        )
      }'."
    }
    val dbObj = checkResult.dataTransferArea!!
    val dto = convert(request, dbObj, checkResult.userInfo)
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
      byExternalUser = getExternalUserString(request, checkResult.userInfo)
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
  )
  //@RequestParam("files") files: Array<MultipartFile>)
      : ResponseEntity<*>? {
    //files.forEach { file ->
    check(listId == AttachmentsService.DEFAULT_NODE)
    val filename = file.originalFilename
    val checkResult = checkAccess(request, category, id)
    checkResult.failedAccess?.let { return it }
    log.info {
      "User tries to upload attachment: id='$id', filename='$filename', page='${this::class.java.name}', user='${
        getExternalUserString(
          request,
          checkResult.userInfo
        )
      }'."
    }

    val obj = dataTransferAreaDao.internalGetById(id)
      ?: throw TechnicalException(
        "Entity with id $id not accessible for category '$category' or doesn't exist.",
        "Entity with id unknown."
      )

    if (obj.externalUploadEnabled != true) {
      return RestUtils.badRequest("Upload not enabled.")
    }

    val attachment = attachmentsService.addAttachment(
      dataTransferAreaPagesRest.jcrPath!!,
      fileInfo = FileInfo(file.originalFilename, fileSize = file.size),
      inputStream = file.inputStream,
      baseDao = dataTransferAreaDao,
      obj = obj,
      accessChecker = attachmentsAccessChecker,
      userString = getExternalUserString(request, checkResult.userInfo)
    )
    //}

    DataTransferPublicSession.registerFileAsOwner(request, obj.id, attachment.fileId, attachment.name)
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

  @PostMapping("delete")
  fun delete(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentsServicesRest.AttachmentData>)
      : ResponseEntity<*>? {
    val category = postData.data.category
    val id = postData.data.id
    val checkResult = checkAccess(request, category, id)
    checkResult.failedAccess?.let { return it }
    log.info {
      "User tries to delete attachment: id='$id', fileId='${postData.data.fileId}', file=${postData.data.attachment}, user='${
        getExternalUserString(
          request,
          checkResult.userInfo
        )
      }'."
    }

    val data = postData.data
    attachmentsService.deleteAttachment(
      dataTransferAreaPagesRest.jcrPath!!,
      data.fileId,
      dataTransferAreaDao,
      checkResult.dataTransferArea!!,
      attachmentsAccessChecker,
      data.listId
    )
    val list =
      attachmentsService.getAttachments(
        dataTransferAreaPagesRest.jcrPath!!,
        data.id,
        attachmentsAccessChecker,
        data.listId
      )
        ?: emptyList() // Client needs empty list to update data of attachments.
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
          .addVariable("data", AttachmentsServicesRest.ResponseData(list))
      )
  }

  @PostMapping("modify")
  fun modify(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentsServicesRest.AttachmentData>)
      : ResponseEntity<*>? {
    val data = postData.data
    val attachment = data.attachment
    val category = data.category
    val id = data.id
    val checkResult = checkAccess(request, category, id)
    checkResult.failedAccess?.let { return it }
    log.info {
      "User tries to modify attachment: id='$id', fileId='${data.fileId}', file=${postData.data.attachment}, user='${
        getExternalUserString(
          request,
          checkResult.userInfo
        )
      }'."
    }

    attachmentsService.changeFileInfo(
      dataTransferAreaPagesRest.jcrPath!!,
      data.fileId,
      dataTransferAreaDao,
      checkResult.dataTransferArea!!,
      attachment.name,
      attachment.description,
      attachmentsAccessChecker,
      data.listId,
      userString = getExternalUserString(request, checkResult.userInfo)
    )
    val list =
      attachmentsService.getAttachments(
        dataTransferAreaPagesRest.jcrPath!!,
        data.id,
        attachmentsAccessChecker,
        data.listId
      )
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
          .addVariable("data", AttachmentsServicesRest.ResponseData(list))
      )
  }

  internal fun checkAccess(
    request: HttpServletRequest,
    category: String,
    areaId: Int
  ): CheckAccessResponse {
    check(category == DataTransferPlugin.ID)
    val sessionData = DataTransferPublicSession.getTransferAreaData(request, areaId)
      ?: return CheckAccessResponse(
        failedAccess = ResponseEntity.badRequest()
          .contentType(MediaType("text", "plain", StandardCharsets.UTF_8))
          .body("Not logged-in.")
      )
    val checkAccess =
      attachmentsAccessChecker.checkExternalAccess(
        dataTransferAreaDao,
        request,
        sessionData.accessToken,
        sessionData.password,
        sessionData.userInfo
      )
    checkAccess.failedAccessMessage?.let {
      return CheckAccessResponse(
        failedAccess = ResponseEntity.badRequest()
          .contentType(MediaType("text", "plain", StandardCharsets.UTF_8))
          .body(it)
      )
    }
    val dbo = checkAccess.dataTransferArea!!
    if (dbo.id != areaId) {
      log.warn { "User tries to use data transfer area by id different from access token!!!" }
      return CheckAccessResponse(
        failedAccess = ResponseEntity.badRequest()
          .contentType(MediaType("text", "plain", StandardCharsets.UTF_8))
          .body(LoginResultStatus.FAILED.localizedMessage)
      )
    }
    return CheckAccessResponse(checkAccess.dataTransferArea, userInfo = sessionData.userInfo)
  }

  internal fun getExternalUserString(request: HttpServletRequest, userString: String?): String {
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

  internal class CheckAccessResponse(
    val dataTransferArea: DataTransferAreaDO? = null,
    val failedAccess: ResponseEntity<String>? = null,
    val userInfo: String? = null,
  )
}
