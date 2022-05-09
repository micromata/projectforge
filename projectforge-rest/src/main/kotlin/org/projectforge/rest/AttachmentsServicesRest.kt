/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.common.FormatterUtils
import org.projectforge.framework.api.TechnicalException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsDaoAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.jcr.FileInfo
import org.projectforge.jcr.FileObject
import org.projectforge.jcr.ZipMode
import org.projectforge.jcr.ZipUtils
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * Rest services for up- and downloading, updating and deletion of attachments. It's used by ContractDao as well as
 * by Data transfer for registered users.
 */
@RestController
@RequestMapping(AttachmentsServicesRest.REST_PATH)
class AttachmentsServicesRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  private var actionListeners = mutableMapOf<String, AttachmentsActionListener>()

  private lateinit var defaultActionListener: AttachmentsActionListener

  @PostConstruct
  private fun postConstruct() {
    defaultActionListener = AttachmentsActionListener(attachmentsService)
  }

  class AttachmentData(
    var category: String,
    var id: Int,
    var fileId: String,
    var listId: String? = null,
    /**
     * True, if the user selects the checkbox "encryption" for displaying/hiding functionality for zip encryption.
     */
    var showEncryptionOption: Boolean? = false,
  ) {
    lateinit var attachment: Attachment
  }

  class ResponseData(var attachments: List<Attachment>?)

  /**
   * Registered listener will be called on actions, if given category is affected.
   */
  fun register(category: String, listener: AttachmentsActionListener) {
    synchronized(actionListeners) {
      if (actionListeners[category] != null) {
        log.warn { "Can't register action listener twice for category '$category'. Already registered: $listener" }
      } else {
        actionListeners[category] = listener
      }
    }
  }

  /**
   * @param category
   * @return AttachmentsActionListener registered by category or default implementation, if no listener registered by given category.
   */
  fun getListener(category: String): AttachmentsActionListener {
    synchronized(actionListeners) {
      return actionListeners[category] ?: defaultActionListener
    }
  }

  @PostMapping("modify")
  fun modify(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentData>)
      : ResponseEntity<*>? {
    validateCsrfToken(request, postData)?.let { return it }
    val data = postData.data
    val category = data.category
    val listId = data.listId
    val attachment = data.attachment
    val pagesRest = getPagesRest(data.category, data.listId)
    getAttachment(pagesRest, data) // Check attachment availability
    val obj = getDataObject(pagesRest, data.id) // Check data object availability.

    attachmentsService.changeFileInfo(
      pagesRest.jcrPath!!, data.fileId, pagesRest.baseDao, obj, attachment.name, attachment.description,
      pagesRest.attachmentsAccessChecker, data.listId
    )
    val list =
      attachmentsService.getAttachments(pagesRest.jcrPath!!, data.id, pagesRest.attachmentsAccessChecker, data.listId)
    val actionListener = getListener(category)
    return actionListener.afterModification(attachment, obj, pagesRest.jcrPath!!, pagesRest.attachmentsAccessChecker, listId)
  }

  @PostMapping("encrypt")
  fun encrypt(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentData>)
      : Any? {
    validateCsrfToken(request, postData)?.let { return it }
    val password = postData.data.attachment.password
    if (password.isNullOrBlank() || password.length < 6) {
      return ResponseEntity(
        ResponseAction(
          validationErrors = createValidationErrors(
            ValidationError(
              translateMsg("user.changePassword.error.notMinLength", "6"),
              fieldId = "attachment.password"
            )
          )
        ), HttpStatus.NOT_ACCEPTABLE
      )
    }
    val result = prepareEncryption(postData)
    result.responseAction?.let { return it }
    val pagesRest = result.pagesRest!!
    var newFilename: String
    val tmpFile = File.createTempFile("projectforge-encrypted-zip", null)
    val encryptionMode = result.attachment!!.newZipMode ?: ZipMode.ENCRYPTED_STANDARD
    result.inputStream!!.use { istream ->
      val file = File(result.fileObject!!.fileName ?: "untitled.zip")
      val filenameWithoutExtension = file.nameWithoutExtension
      val oldExtension = file.extension
      val preserveExtension = if (oldExtension.equals("zip", ignoreCase = true)) "-encrypted" else ".$oldExtension"
      newFilename = "$filenameWithoutExtension$preserveExtension.zip"
      FileOutputStream(tmpFile).use { out ->
        ZipUtils.encryptZipFile(
          file.name,
          password,
          istream,
          out,
          encryptionMode
        )
      }
    }
    FileInputStream(tmpFile).use { istream ->
      attachmentsService.addAttachment(
        pagesRest.jcrPath!!,
        fileInfo = FileInfo(
          newFilename,
          fileSize = tmpFile.length(),
          description = result.attachment.description,
          zipMode = encryptionMode,
          encryptionInProgress = true,
        ),
        inputStream = istream,
        baseDao = pagesRest.baseDao,
        obj = result.obj!!,
        accessChecker = pagesRest.attachmentsAccessChecker
      )
    }
    tmpFile.delete()
    val data = postData.data
    attachmentsService.deleteAttachment(
      pagesRest.jcrPath!!,
      data.fileId,
      pagesRest.baseDao,
      result.obj!!,
      pagesRest.attachmentsAccessChecker,
      data.listId,
      encryptionInProgress = true,
    )
    val list =
      attachmentsService.getAttachments(pagesRest.jcrPath!!, data.id, pagesRest.attachmentsAccessChecker, data.listId)
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
          .addVariable("data", ResponseData(list))
      )
  }

  @PostMapping("testDecryption")
  fun testDecryption(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentData>)
      : Any {
    validateCsrfToken(request, postData)?.let { return it }
    val result = prepareEncryption(postData)
    result.responseAction?.let { return it }
    val password = postData.data.attachment.password
    val testResult = !password.isNullOrBlank() && result.inputStream!!.use { istream ->
      ZipUtils.testDecryptZipFile(postData.data.attachment.password ?: "empty password is wrong password", istream)
    }
    if (testResult) {
      return UIToast.createToast(translate("attachment.testDecryption.successful"), color = UIColor.SUCCESS)
    }
    return ResponseEntity(
      ResponseAction(
        validationErrors = createValidationErrors(
          ValidationError(
            translate("attachment.testDecryption.failed"),
            fieldId = "attachment.password"
          )
        )
      ), HttpStatus.NOT_ACCEPTABLE
    )
  }

  private fun prepareEncryption(postData: PostData<AttachmentData>): MyResult {
    val data = postData.data
    val attachment = data.attachment
    val pagesRest = getPagesRest(data.category, data.listId)
    getAttachment(pagesRest, data) // Check attachment availability
    val obj = getDataObject(pagesRest, data.id) // Check data object availability.

    val pair = attachmentsService.getAttachmentInputStream(
      pagesRest.jcrPath!!, data.id, data.fileId, pagesRest.attachmentsAccessChecker, data.listId
    )
    if (pair?.second == null) {
      log.error { "Can't encrypt zip file. Not found as inputstream: $attachment" }
      return MyResult(UIToast.createToast(translate("exception.internalError")))
    }
    return MyResult(
      fileObject = pair.first,
      inputStream = pair.second,
      attachment = attachment,
      obj = obj,
      pagesRest = pagesRest,
    )
  }

  /**
   * Upload service e. g. for [UIAttachmentList].
   * @param id Object id where the uploaded file should belong to.
   * @param listId Usable for handling different upload areas for one page. If only one attachment list is needed, you may
   * ignore this value.
   */
  @PostMapping("upload/{category}/{id}/{listId}")
  fun uploadAttachment(
    @PathVariable("category", required = true) category: String,
    @PathVariable("id", required = true) id: Int,
    @PathVariable("listId") listId: String?,
    @RequestParam("file") file: MultipartFile
  )
      : ResponseEntity<*> {
    //@RequestParam("files") files: Array<MultipartFile>) // Multiple file handling is done by client.
    val pagesRest = getPagesRest(category, listId)
    //files.forEach { file ->
    val filename = file.originalFilename
    log.info {
      "User tries to upload attachment: id='$id', listId='$listId', filename='$filename', size=${
        FormatterUtils.formatBytes(
          file.size
        )
      }, page='${this::class.java.name}'."
    }

    val obj = getDataObject(pagesRest, id) // Check data object availability.
    val fileInfo = FileInfo(file.originalFilename, fileSize = file.size)
    val actionListener = getListener(category)
    actionListener.onUpload(fileInfo, obj)?.let {
      return it
    }
    val attachment = attachmentsService.addAttachment(
      pagesRest.jcrPath!!,
      fileInfo = fileInfo,
      inputStream = file.inputStream,
      baseDao = pagesRest.baseDao,
      obj = obj,
      accessChecker = pagesRest.attachmentsAccessChecker,
      allowDuplicateFiles = actionListener.allowDuplicateFiles,
    )
    //}
    return actionListener.afterUpload(attachment, obj, pagesRest.jcrPath!!, pagesRest.attachmentsAccessChecker, listId)
  }

  @PostMapping("delete")
  fun delete(request: HttpServletRequest, @RequestBody postData: PostData<AttachmentData>)
      : ResponseEntity<ResponseAction>? {
    validateCsrfToken(request, postData)?.let { return it }
    val data = postData.data
    val pagesRest = getPagesRest(data.category, data.listId)
    val obj = getDataObject(pagesRest, data.id) // Check data object availability.
    attachmentsService.deleteAttachment(
      pagesRest.jcrPath!!,
      data.fileId,
      pagesRest.baseDao,
      obj,
      pagesRest.attachmentsAccessChecker,
      data.listId
    )
    val list =
      attachmentsService.getAttachments(pagesRest.jcrPath!!, data.id, pagesRest.attachmentsAccessChecker, data.listId)
        ?: emptyList() // Client needs empty list to update data of attachments.
    return ResponseEntity.ok()
      .body(
        ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
          .addVariable("data", ResponseData(list))
      )
  }

  @GetMapping("download/{category}/{id}")
  fun download(
    @PathVariable("category", required = true) category: String,
    @PathVariable("id", required = true) id: Int,
    @RequestParam("fileId", required = true) fileId: String,
    @RequestParam("listId") listId: String?
  )
      : ResponseEntity<InputStreamResource> {
    log.info { "User tries to download attachment: ${paramsToString(category, id, fileId, listId)}." }
    val pagesRest = getPagesRest(category, listId)

    val result =
      attachmentsService.getAttachmentInputStream(pagesRest.jcrPath!!, id, fileId, pagesRest.attachmentsAccessChecker)
        ?: throw TechnicalException(
          "File to download not accessible for user or not found: ${
            paramsToString(
              category,
              id,
              fileId,
              listId
            )
          }."
        )

    val filename = result.first.fileName ?: "file"
    val inputStream = result.second
    return RestUtils.downloadFile(filename, inputStream)
  }

  internal fun getPagesRest(
    category: String,
    listId: String?
  ): AbstractPagesRest<out ExtendedBaseDO<Int>, *, out BaseDao<*>> {
    val pagesRest = PagesResolver.getPagesRest(category)
      ?: throw UnsupportedOperationException("PagesRest class for category '$category' not known (registered).")
    pagesRest.attachmentsAccessChecker.let {
      if (it is AttachmentsDaoAccessChecker<*>) {
        it.checkJcrActivity(listId)
      }
      return pagesRest
    }
  }

  fun getAttachment(
    jcrPath: String,
    attachmentsAccessChecker: AttachmentsAccessChecker,
    data: AttachmentData
  ): Attachment {
    return attachmentsService.getAttachmentInfo(
      jcrPath,
      data.id,
      data.fileId,
      attachmentsAccessChecker,
      data.listId
    )
      ?: throw TechnicalException(
        "Attachment '$data.fileId' for object with id $data.id not found for category '$data.category' and list '$data.listId'.",
        "Attachment not found."
      )
  }

  internal fun getAttachment(pagesRest: AbstractPagesRest<*, *, *>, data: AttachmentData): Attachment {
    return getAttachment(pagesRest.jcrPath!!, pagesRest.attachmentsAccessChecker, data)
  }

  internal fun getDataObject(pagesRest: AbstractPagesRest<*, *, *>, id: Int): ExtendedBaseDO<Int> {
    return pagesRest.baseDao.getById(id)
      ?: throw TechnicalException(
        "Entity with id $id not accessible for category '$pagesRest.category' or doesn't exist.",
        "User without access or id unknown."
      )

  }

  private fun paramsToString(category: String, id: Any, fileId: String, listId: String?): String {
    return "category='$category', id='$id', fileId='$fileId', listId='$listId'"
  }

  private class MyResult(
    val responseAction: ResponseAction? = null,
    val inputStream: InputStream? = null,
    val fileObject: FileObject? = null,
    val attachment: Attachment? = null,
    val obj: ExtendedBaseDO<Int>? = null,
    val pagesRest: AbstractPagesRest<out ExtendedBaseDO<Int>, *, out BaseDao<*>>? = null,
  )

  companion object {
    internal const val REST_PATH = "${Rest.URL}/attachments"
    @JvmStatic
    fun getDownloadUrl(attachment: Attachment, category: String, id: Any, listId: String? = null): String {
      val listIdParam = if (listId.isNullOrBlank()) "" else "&listId=$listId"
      return "download/$category/$id?fileId=${attachment.fileId}$listIdParam"
    }
  }
}
