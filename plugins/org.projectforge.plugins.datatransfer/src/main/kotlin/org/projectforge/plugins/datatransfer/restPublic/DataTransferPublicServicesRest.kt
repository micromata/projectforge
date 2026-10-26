/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.common.NumberOfBytes
import org.projectforge.framework.api.TechnicalException
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.jcr.FileInfo
import org.projectforge.jcr.FileObject
import org.projectforge.login.LoginService
import org.projectforge.plugins.datatransfer.DataTransferAreaDO
import org.projectforge.plugins.datatransfer.DataTransferAreaDao
import org.projectforge.plugins.datatransfer.DataTransferAreaDao.Companion.getExternalUserString
import org.projectforge.plugins.datatransfer.DataTransferPlugin
import org.projectforge.plugins.datatransfer.DataTransferUtils
import org.projectforge.plugins.datatransfer.rest.DataTransferAreaEntityRest
import org.projectforge.plugins.datatransfer.rest.DataTransferRestUtils
import org.projectforge.rest.AttachmentsServicesRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

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
    private lateinit var attachmentsServicesRest: AttachmentsServicesRest

    @Autowired
    private lateinit var dataTransferAreaEntityRest: DataTransferAreaEntityRest

    @Autowired
    private lateinit var dataTransferPublicSession: DataTransferPublicSession

    private lateinit var attachmentsAccessChecker: DataTransferPublicAccessChecker

    @PostConstruct
    private fun postConstruct() {
        attachmentsAccessChecker = DataTransferPublicAccessChecker(dataTransferPublicSession)
    }

    /**
     * User must be logged in before (the accessToken and external password of the user's session are used).
     * @param category [DataTransferPlugin.ID] ("datatransfer") expected
     */
    @GetMapping("download/{category}/{id}")
    @AccessChecked("PUBLIC: valid login of the area in the session (checkLogin) and download access to the file (hasDownloadAccess)")
    fun download(
        request: HttpServletRequest,
        @PathVariable("category", required = true) category: String,
        @PathVariable("id", required = true) id: Long,
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
            "User tries to download attachment: ${createLogInfo(request, sessionData, category, id, listId, fileId)}."
        }
        if (!attachmentsAccessChecker.hasDownloadAccess(request, area, fileId)) {
            return RestUtils.badRequest("Download not enabled.")
        }
        val result =
            attachmentsService.getAttachmentInputStream(
                dataTransferAreaEntityRest.jcrPath!!,
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
    @AccessChecked("PUBLIC: valid login of the area in the session (checkLogin) and external download enabled (isDownloadAllAvailable)")
    fun downloadAll(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @PathVariable("category", required = true) category: String,
        @PathVariable("id", required = true) id: Long,
    ): ResponseEntity<*>? {
        check(category == DataTransferPlugin.ID)
        val data = dataTransferPublicSession.checkLogin(request, id) ?: return RestUtils.badRequest("No valid login.")
        val area: DataTransferAreaDO = data.first
        val sessionData = data.second
        log.info {
            "User tries to download all attachments: ${createLogInfo(request, sessionData, category, id)}."
        }
        if (!isDownloadAllAvailable(area)) {
            return RestUtils.badRequest("Download of all files not available.")
        }
        val dto = convert(request, area, sessionData.userInfo)
        DataTransferRestUtils.multiDownload(
            response,
            attachmentsService,
            attachmentsAccessChecker,
            area,
            dto.areaName,
            jcrPath = dataTransferAreaEntityRest.jcrPath!!,
            id,
            dto.attachments,
            byExternalUser = getExternalUserString(request, sessionData.userInfo)
        )
        return null
    }

    /**
     * @param fileIds csv of fileIds of attachments to download. For preserving url length, fileIds may also be shortened
     * (e. g. first 4 chars).
     */
    @GetMapping("multiDownload/{category}/{id}")
    @AccessChecked("PUBLIC: valid login of the area in the session (checkLogin) and external download enabled")
    fun multiDownload(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @PathVariable("category", required = true) category: String,
        @PathVariable("id", required = true) id: Long,
        @RequestParam("fileIds", required = true) fileIds: String,
        @RequestParam("listId") listId: String?
    ) {
        check(category == DataTransferPlugin.ID)
        check(listId == AttachmentsService.DEFAULT_NODE)
        val data = dataTransferPublicSession.checkLogin(request, id) ?: return // No valid login.
        val area: DataTransferAreaDO = data.first
        val sessionData = data.second
        log.info {
            "User tries to download multiple attachments: ${createLogInfo(request, sessionData, category, id, listId)}."
        }
        if (area.externalDownloadEnabled != true) {
            return // Download not enabled
        }
        val dto = convert(request, area, sessionData.userInfo)
        val fileIdList = fileIds.split(",")
        val attachments = attachmentsService.getAttachments(
            dataTransferAreaEntityRest.jcrPath!!,
            id,
            attachmentsAccessChecker,
        )
            ?.filter { attachment ->
                fileIdList.any { attachment.fileId?.startsWith(it) == true }
            }
        // The same limit as for downloading all: a selection of every file must not get around it.
        if (attachments.orEmpty().sumOf { it.size ?: 0L } > MAX_ZIP_SIZE) {
            log.info { "Selected attachments exceed $MAX_ZIP_SIZE bytes, ZIP download refused." }
            response.status = HttpServletResponse.SC_BAD_REQUEST
            return
        }
        DataTransferRestUtils.multiDownload(
            response,
            attachmentsService,
            attachmentsAccessChecker,
            area,
            dto.areaName,
            jcrPath = dataTransferAreaEntityRest.jcrPath!!,
            id,
            attachments,
            byExternalUser = getExternalUserString(request, sessionData.userInfo)
        )
    }

    @PostMapping("multiDelete")
    @AccessChecked("PUBLIC: valid login of the area in the session (checkLogin), external upload enabled and own files (hasDeleteAccess)")
    fun multiDelete(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentsServicesRest.FileListData>)
            : ResponseEntity<*>? {
        val data = postData.data
        val category = data.category
        val id = data.id
        val listId = data.listId
        val fileIds = data.fileIds
        check(category == DataTransferPlugin.ID)
        check(listId == AttachmentsService.DEFAULT_NODE)
        checkNotNull(id)
        checkNotNull(fileIds)

        val loginResult =
            dataTransferPublicSession.checkLogin(request, id) ?: return RestUtils.badRequest("No valid login.")
        val area: DataTransferAreaDO = loginResult.first
        val sessionData = loginResult.second
        log.info {
            "User tries to delete multiple attachments: ${
                createLogInfo(
                    request,
                    sessionData,
                    category,
                    id,
                    listId,
                    fileIds.joinToString()
                )
            }."
        }
        if (area.externalUploadEnabled != true) {
            return RestUtils.badRequest("Deleting not enabled.")
        }
        val selectedAttachments =
            attachmentsService.getAttachments(
                dataTransferAreaEntityRest.jcrPath!!,
                id,
                attachmentsAccessChecker
            )
                ?.filter { fileIds.contains(it.fileId) }
        selectedAttachments?.forEach {
            it.fileId?.let { fileId ->
                if (!attachmentsAccessChecker.hasDeleteAccess(request, area, fileId)) {
                    log.info {
                        "Deleting attachment not allowed: ${
                            createLogInfo(
                                request,
                                sessionData,
                                category,
                                id,
                                listId,
                                fileId
                            )
                        }."
                    }
                } else {
                    attachmentsService.deleteAttachment(
                        dataTransferAreaEntityRest.jcrPath!!,
                        fileId,
                        dataTransferAreaDao,
                        area,
                        attachmentsAccessChecker,
                        data.listId,
                        userString = getExternalUserString(request, sessionData.userInfo)
                    )
                }
            }
        }
        val list = getClientAttachments(request, area)
        return ResponseEntity.ok()
            .body(
                ResponseAction(targetType = TargetType.UPDATE, merge = true)
                    .addVariable("data", AttachmentsServicesRest.ResponseData(list))
            )
    }

    @PostMapping("upload/{category}/{id}/{listId}")
    @AccessChecked("PUBLIC: valid login of the area in the session (checkLogin) and external upload enabled")
    fun uploadAttachment(
        request: HttpServletRequest,
        @PathVariable("category", required = true) category: String,
        @PathVariable("id", required = true) id: Long,
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
            "User tries to upload attachment: ${createLogInfo(request, sessionData, category, id, listId, filename)}."
        }

        if (area.externalUploadEnabled != true) {
            return RestUtils.badRequest("Upload not enabled.")
        }

        val attachment = file.inputStream.use { inputStream ->
            attachmentsService.addAttachment(
                dataTransferAreaEntityRest.jcrPath!!,
                fileInfo = FileInfo(file.originalFilename, fileSize = file.size),
                inputStream = inputStream,
                baseDao = dataTransferAreaDao,
                obj = area,
                accessChecker = attachmentsAccessChecker,
                userString = getExternalUserString(request, sessionData.userInfo)
            )
        }

        dataTransferPublicSession.registerFileAsOwner(request, area.id, attachment.fileId, attachment.name)
        val list = getClientAttachments(request, area)
        return ResponseEntity.ok()
            .body(
                ResponseAction(targetType = TargetType.UPDATE, merge = true)
                    .addVariable("data", AttachmentsServicesRest.ResponseData(list))
            )
    }

    @PostMapping("delete")
    @AccessChecked("PUBLIC: valid login of the area in the session (checkLogin) and own file with upload access (hasDeleteAccess)")
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
            "User tries to delete attachment: ${
                createLogInfo(
                    request,
                    sessionData,
                    category,
                    id,
                    listId,
                    file = postData.data.attachment,
                )
            }."
        }

        val fileId = postData.data.fileId

        if (!attachmentsAccessChecker.hasDeleteAccess(request, area, fileId)) {
            return RestUtils.badRequest("Deleting not allowed.")
        }

        attachmentsService.deleteAttachment(
            dataTransferAreaEntityRest.jcrPath!!,
            fileId,
            dataTransferAreaDao,
            area,
            attachmentsAccessChecker,
            listId,
            userString = getExternalUserString(request, sessionData.userInfo)
        )
        val list = getClientAttachments(request, area) ?: emptyList() // Client needs empty list to update data of attachments.
        return ResponseEntity.ok()
            .body(
                ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
                    .addVariable("data", AttachmentsServicesRest.ResponseData(list))
            )
    }

    @PostMapping("modify")
    @AccessChecked("PUBLIC: valid login of the area in the session (checkLogin) and own file with upload access (hasUpdateAccess)")
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
            "User tries to modify attachment: ${
                createLogInfo(request, sessionData, category, id, listId, file = postData.data.attachment)
            }."
        }

        if (!attachmentsAccessChecker.hasUpdateAccess(request, area, fileId)) {
            return RestUtils.badRequest("Modification not allowed.")
        }

        attachmentsService.changeFileInfo(
            dataTransferAreaEntityRest.jcrPath!!,
            fileId,
            dataTransferAreaDao,
            area,
            attachment.name,
            attachment.description,
            attachmentsAccessChecker,
            listId,
            userString = getExternalUserString(request, sessionData.userInfo)
        )
        val list = getClientAttachments(request, area)
        return ResponseEntity.ok()
            .body(
                ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
                    .addVariable("data", AttachmentsServicesRest.ResponseData(list))
            )
    }

    /**
     * Replaces an own file (uploaded in this session, with upload access) by a password protected ZIP of it, as
     * [AttachmentsServicesRest.encrypt] does for logged-in users. The ZIP has a new file id, which is registered as
     * owned by this session like an upload: otherwise its uploader could neither change it nor, without download
     * access, even see it any more.
     */
    @PostMapping("encrypt")
    @AccessChecked("PUBLIC: valid login of the area in the session (checkLogin) and own file with upload access (hasUpdateAccess)")
    fun encrypt(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentsServicesRest.AttachmentData>)
            : ResponseEntity<*> {
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
            "User tries to encrypt attachment: ${createLogInfo(request, sessionData, category, id, listId, fileId)}."
        }
        if (!attachmentsAccessChecker.hasUpdateAccess(request, area, fileId)) {
            return RestUtils.badRequest("Encryption not allowed.")
        }
        val password = postData.data.attachment.password
        attachmentsServicesRest.passwordTooShort(password)?.let { return it }
        val (fileObject, inputStream) = getInputStream(area, fileId)
            ?: return RestUtils.badRequest("File not found.")
        val encrypted = inputStream.use { istream ->
            attachmentsServicesRest.encryptFile(
                dataTransferAreaEntityRest.jcrPath!!,
                dataTransferAreaDao,
                area,
                attachmentsAccessChecker,
                fileId = fileId,
                listId = listId,
                fileObject = fileObject,
                inputStream = istream,
                password = password!!,
                mode = postData.data.attachment.newZipMode,
                description = postData.data.attachment.description,
                userString = getExternalUserString(request, sessionData.userInfo),
            )
        }
        dataTransferPublicSession.registerFileAsOwner(request, area.id, encrypted.fileId, encrypted.name)
        val list = getClientAttachments(request, area)
        return ResponseEntity.ok()
            .body(
                ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
                    .addVariable("data", AttachmentsServicesRest.ResponseData(list))
            )
    }

    /** Checks a password against an own encrypted file ([AttachmentsServicesRest.testDecryption]). Changes nothing. */
    @PostMapping("testDecryption")
    @AccessChecked("PUBLIC: valid login of the area in the session (checkLogin) and own file with upload access (hasUpdateAccess)")
    fun testDecryption(
        request: HttpServletRequest,
        @RequestBody postData: PostData<AttachmentsServicesRest.AttachmentData>
    ): Any {
        val category = postData.data.category
        val id = postData.data.id
        val listId = postData.data.listId
        check(category == DataTransferPlugin.ID)
        check(listId == AttachmentsService.DEFAULT_NODE)
        val data = dataTransferPublicSession.checkLogin(request, id) ?: return RestUtils.badRequest("No valid login.")
        val area: DataTransferAreaDO = data.first
        val fileId = postData.data.fileId
        if (!attachmentsAccessChecker.hasUpdateAccess(request, area, fileId)) {
            return RestUtils.badRequest("Decryption test not allowed.")
        }
        val (_, inputStream) = getInputStream(area, fileId) ?: return RestUtils.badRequest("File not found.")
        val success = inputStream.use { attachmentsServicesRest.isDecryptable(postData.data.attachment.password, it) }
        return attachmentsServicesRest.testDecryptionResponse(success)
    }

    private fun getInputStream(area: DataTransferAreaDO, fileId: String): Pair<FileObject, InputStream>? {
        val pair = attachmentsService.getAttachmentInputStream(
            dataTransferAreaEntityRest.jcrPath!!,
            area.id!!,
            fileId,
            attachmentsAccessChecker,
        )
        val inputStream = pair?.second ?: return null
        return pair.first to inputStream
    }

    /**
     * Whether the visitor may see who uploaded and changed the files: not with the area's password alone, only if
     * logged in to ProjectForge as well (single sign-on, or the login of the app in the same session) and with
     * access to the area there ([DataTransferAreaDao.hasInternalAccess]: admin, access user or access group).
     */
    internal fun isUserInfoVisible(request: HttpServletRequest, area: DataTransferAreaDO): Boolean {
        val user = LoginService.getUser(request) ?: return false
        return dataTransferAreaDao.hasInternalAccess(user, area)
    }

    internal fun convert(
        request: HttpServletRequest,
        dbo: DataTransferAreaDO,
        userInfo: String?
    ): DataTransferPublicArea {
        val dto = DataTransferPublicArea()
        dto.copyFrom(dbo)
        dto.attachments = getClientAttachments(request, dbo)
        dto.userInfo = userInfo
        return dto
    }

    /**
     * The attachments of the area as the external user may see them: without download access only the files uploaded
     * in this session (see [DataTransferPublicAccessChecker.filterAttachments]). Every file carries its expiry info, and
     * files the user may neither rename nor delete are marked as [Attachment.readonly]. Who uploaded or changed a file
     * is removed — users of ProjectForge, or the ip address and name of other external users — unless the visitor is
     * logged in to ProjectForge with access to the area there ([isUserInfoVisible]).
     */
    internal fun getClientAttachments(request: HttpServletRequest, area: DataTransferAreaDO): List<Attachment>? {
        val attachments = attachmentsAccessChecker.filterAttachments(
            request,
            area.externalDownloadEnabled,
            area.id!!,
            attachmentsService.getAttachments(
                dataTransferAreaEntityRest.jcrPath!!,
                area.id!!,
                attachmentsAccessChecker
            )
        )
        val userInfoVisible = isUserInfoVisible(request, area)
        attachments?.forEach {
            it.addExpiryInfo(DataTransferUtils.expiryTimeLeft(it, area.expiryDays))
            it.readonly = it.fileId?.let { fileId -> !attachmentsAccessChecker.hasUpdateAccess(request, area, fileId) } ?: true
            if (!userInfoVisible) {
                it.createdByUser = null
                it.createdByUserId = null
                it.createdByUserDeactivated = null
                it.lastUpdateByUser = null
                it.lastUpdateByUserDeactivated = null
            }
        }
        return attachments
    }

    private fun createLogInfo(
        request: HttpServletRequest,
        sessionData: DataTransferPublicSession.TransferAreaData,
        category: String,
        id: Long,
        listId: String? = null,
        fileId: String? = null,
        file: Attachment? = null,
    ): String {
        val sb = StringBuilder()
        sb.append("category=$category, id=$id,")
        if (!listId.isNullOrBlank()) {
            sb.append("fileId=$listId,")
        }
        if (!fileId.isNullOrBlank()) {
            sb.append("fileId=$fileId,")
        }
        if (file != null) {
            sb.append("file=$file,")
        }
        sb.append(
            "user='${
                getExternalUserString(
                    request,
                    sessionData.userInfo
                )
            }'"
        )
        return sb.toString()
    }

    companion object {
        /**
         * The most a ZIP download (all files or a selection) may contain; more is left to the single files. Checked here
         * as well as in the clients, which only offer the buttons then.
         */
        internal const val MAX_ZIP_SIZE = NumberOfBytes.GIGA_BYTES

        /**
         * Download all as one ZIP: only with download access, and for 1 byte to [MAX_ZIP_SIZE] in total.
         */
        internal fun isDownloadAllAvailable(area: DataTransferAreaDO): Boolean {
            return area.externalDownloadEnabled == true && (area.attachmentsSize ?: 0) in 1..MAX_ZIP_SIZE
        }
    }
}
