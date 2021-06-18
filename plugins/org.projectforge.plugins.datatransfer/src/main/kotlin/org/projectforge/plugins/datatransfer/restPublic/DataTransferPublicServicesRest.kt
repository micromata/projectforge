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
import org.projectforge.jcr.FileInfo
import org.projectforge.plugins.datatransfer.*
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaPagesRest
import org.projectforge.plugins.datatransfer.rest.DataTransferRestUtils
import org.projectforge.rest.AttachmentsServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
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
  private lateinit var dataTransferPublicSession: DataTransferPublicSession

  @Autowired
  private lateinit var notificationMailService: NotificationMailService

  private lateinit var attachmentsAccessChecker: DataTransferPublicAccessChecker

  @PostConstruct
  private fun postConstruct() {
    attachmentsAccessChecker = DataTransferPublicAccessChecker(dataTransferAreaDao, dataTransferPublicSession)
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
    check(category == DataTransferPlugin.ID)
    check(listId == AttachmentsService.DEFAULT_NODE)
    val data = dataTransferPublicSession.checkLogin(request, id) ?: return RestUtils.badRequest("No valid login.")
    val area: DataTransferAreaDO = data.first
    val sessionData = data.second
    log.info {
      "User tries to download attachment: category=$category, id=$id, fileId=$fileId, listId=$listId)}, user='${
        getExternalUserString(
          request,
          sessionData.userInfo
        )
      }'."
    }
    if (!attachmentsAccessChecker.hasDownloadAccess(request, area, fileId)) {
      return RestUtils.badRequest("Download not enabled.")
    }
    val result =
      attachmentsService.getAttachmentInputStream(
        dataTransferAreaPagesRest.jcrPath!!,
        id,
        fileId,
        attachmentsAccessChecker,
        data = area,
        attachmentsEventListener = dataTransferAreaDao,
        userString = getExternalUserString(request, sessionData.userInfo)
      )
        ?: throw TechnicalException(
          "File to download not accessible for user or not found: category=$category, id=$id, fileId=$fileId, listId=$listId)}."
        )
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
    check(category == DataTransferPlugin.ID)
    val data = dataTransferPublicSession.checkLogin(request, id) ?: return RestUtils.badRequest("No valid login.")
    val area: DataTransferAreaDO = data.first
    val sessionData = data.second
    log.info {
      "User tries to download all attachments: category=$category, id=$id}, user='${
        getExternalUserString(
          request,
          sessionData.userInfo
        )
      }'."
    }
    if (area.externalDownloadEnabled != true) {
      return RestUtils.badRequest("Download not enabled.")
    }
    val dto = convert(request, area, sessionData.userInfo)
    DataTransferRestUtils.downloadAll(
      response,
      attachmentsService,
      attachmentsAccessChecker,
      notificationMailService,
      area,
      dto.areaName,
      jcrPath = dataTransferAreaPagesRest.jcrPath!!,
      id,
      dto.attachments,
      byExternalUser = getExternalUserString(request, sessionData.userInfo)
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
    check(category == DataTransferPlugin.ID)
    check(listId == AttachmentsService.DEFAULT_NODE)
    val data = dataTransferPublicSession.checkLogin(request, id) ?: return RestUtils.badRequest("No valid login.")
    val area: DataTransferAreaDO = data.first
    val sessionData = data.second
    val filename = file.originalFilename
    log.info {
      "User tries to upload attachment: id='$id', filename='$filename', page='${this::class.java.name}', user='${
        getExternalUserString(
          request,
          sessionData.userInfo
        )
      }'."
    }

    if (area.externalUploadEnabled != true) {
      return RestUtils.badRequest("Upload not enabled.")
    }

    val attachment = attachmentsService.addAttachment(
      dataTransferAreaPagesRest.jcrPath!!,
      fileInfo = FileInfo(file.originalFilename, fileSize = file.size),
      inputStream = file.inputStream,
      baseDao = dataTransferAreaDao,
      obj = area,
      accessChecker = attachmentsAccessChecker,
      userString = getExternalUserString(request, sessionData.userInfo)
    )
    //}

    dataTransferPublicSession.registerFileAsOwner(request, area.id, attachment.fileId, attachment.name)
    val list =
      attachmentsAccessChecker.filterAttachments(
        request,
        area.externalDownloadEnabled,
        area.id!!,
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
    val listId = postData.data.listId
    check(category == DataTransferPlugin.ID)
    check(listId == AttachmentsService.DEFAULT_NODE)
    val data = dataTransferPublicSession.checkLogin(request, id) ?: return RestUtils.badRequest("No valid login.")
    val area: DataTransferAreaDO = data.first
    val sessionData = data.second

    log.info {
      "User tries to delete attachment: id='$id', fileId='${postData.data.fileId}', file=${postData.data.attachment}, user='${
        getExternalUserString(
          request,
          sessionData.userInfo
        )
      }'."
    }

    val fileId = postData.data.fileId

    if (!attachmentsAccessChecker.hasDeleteAccess(request, area, fileId)) {
      return RestUtils.badRequest("Deleting not allowed.")
    }

    attachmentsService.deleteAttachment(
      dataTransferAreaPagesRest.jcrPath!!,
      fileId,
      dataTransferAreaDao,
      area,
      attachmentsAccessChecker,
      listId
    )
    val list =
      attachmentsService.getAttachments(
        dataTransferAreaPagesRest.jcrPath!!,
        id,
        attachmentsAccessChecker,
        listId
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
    val attachment = postData.data.attachment
    val category = postData.data.category
    val id = postData.data.id
    val listId = postData.data.listId
    check(category == DataTransferPlugin.ID)
    check(listId == AttachmentsService.DEFAULT_NODE)
    val data = dataTransferPublicSession.checkLogin(request, id) ?: return RestUtils.badRequest("No valid login.")
    val area: DataTransferAreaDO = data.first
    val sessionData = data.second

    val fileId = postData.data.fileId
    log.info {
      "User tries to modify attachment: id='$id', fileId='$fileId', file=${postData.data.attachment}, user='${
        getExternalUserString(
          request,
          sessionData.userInfo
        )
      }'."
    }

    attachmentsService.changeFileInfo(
      dataTransferAreaPagesRest.jcrPath!!,
      fileId,
      dataTransferAreaDao,
      area,
      attachment.name,
      attachment.description,
      attachmentsAccessChecker,
      listId,
      userString = getExternalUserString(request, sessionData.userInfo)
    )
    val list =
      attachmentsService.getAttachments(
        dataTransferAreaPagesRest.jcrPath!!,
        id,
        attachmentsAccessChecker,
        listId
      )
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
          .addVariable("data", AttachmentsServicesRest.ResponseData(list))
      )
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
      request,
      dto.externalDownloadEnabled,
      dto.id!!,
      attachmentsService.getAttachments(
        dataTransferAreaPagesRest.jcrPath!!,
        dto.id!!,
        attachmentsAccessChecker
      )
    )
    dto.attachments?.forEach {
      it.addExpiryInfo(DataTransferUtils.expiryTimeLeft(it.lastUpdate, dbo.expiryDays))
    }
    dto.userInfo = userInfo
    return dto
  }
}
