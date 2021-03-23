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
import org.projectforge.business.address.AddressImageDao
import org.projectforge.framework.configuration.ConfigurationChecker
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.i18n.I18nUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.unit.DataSize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletRequest


private val log = KotlinLogging.logger {}

/**
 * For uploading address immages.
 */
@RestController
@RequestMapping("${Rest.URL}/address")
class AddressImageServicesRest {

  companion object {
    internal const val SESSION_IMAGE_ATTR = "uploadedAddressImage"
    private const val MAX_IMAGE_SIZE_SPRING_PROPERTY = "projectforge.address.maxImageSize"
  }

  //@Value("\${projectforge.address.maxImageSize:500KB}")
  internal val maxImageSize: DataSize = DataSize.ofKilobytes(500)

  @Autowired
  private lateinit var addressImageDao: AddressImageDao

  @Autowired
  private lateinit var configurationChecker: ConfigurationChecker

  /**
   * If given and greater 0, the image will be added to the address with the given id (pk), otherwise the image is
   * stored in the user's session and will be used for the next update or save event.
   */
  @PostMapping("uploadImage/{id}")
  fun uploadFile(@PathVariable("id") id: Int?, @RequestParam("file") file: MultipartFile, request: HttpServletRequest):
      ResponseEntity<*> {
    val filename = file.originalFilename
    if (filename == null || !filename.endsWith(".png", true)) {
      return ResponseEntity("Unsupported file: $filename. Only png files supported", HttpStatus.BAD_REQUEST)
    }
    val bytes = file.bytes
    configurationChecker.checkConfiguredSpringUploadFileSize(
      bytes.size,
      maxImageSize.toBytes(),
      MAX_IMAGE_SIZE_SPRING_PROPERTY,
      filename,
      false
    )?.let {
      log.error(it)
      return ResponseEntity(
        I18nUtils.translateMaxSizeExceeded(
          filename,
          bytes.size.toLong(),
          maxImageSize.toBytes()
        ), HttpStatus.BAD_REQUEST
      )
    }

    if (id == null || id < 0) {
      val session = request.session
      ExpiringSessionAttributes.setAttribute(session, SESSION_IMAGE_ATTR, bytes, 1)
    } else {
      addressImageDao.saveOrUpdate(id, bytes)
    }
    return ResponseEntity("OK", HttpStatus.OK)
  }

  /**
   * @param id The id of the address the image is assigned to.
   */
  @GetMapping("image/{id}")
  fun getImage(@PathVariable("id") id: Int): ResponseEntity<Resource> {
    val image = addressImageDao.getImage(id) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
    val resource = ByteArrayResource(image)
    return RestUtils.downloadFile("ProjectForge-addressImage_$id.png", resource)
  }

  /**
   * @param id The id of the address the image is assigned to.
   */
  @GetMapping("imagePreview/{id}")
  fun getImagePreview(@PathVariable("id") id: Int): ResponseEntity<Resource> {
    val image = addressImageDao.getPreviewImage(id) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
    val resource = ByteArrayResource(image)
    return RestUtils.downloadFile("ProjectForge-addressImagePreview_$id.png", resource)
  }

  /**
   * If given and greater 0, the image will be deleted from the address with the given id (pk), otherwise the image is
   * removed from the user's session and will not be used for the next update or save event anymore.
   * @param id The id of the address the image is assigned to.
   */
  @DeleteMapping("deleteImage/{id}")
  fun deleteImage(request: HttpServletRequest, @PathVariable("id") id: Int?): ResponseEntity<String> {
    val session = request.session
    ExpiringSessionAttributes.removeAttribute(session, SESSION_IMAGE_ATTR)
    if (id != null && id > 0) {
      addressImageDao.delete(id)
    }
    return ResponseEntity("OK", HttpStatus.OK)
  }
}
