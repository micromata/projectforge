/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.address.AddressImageDao
import org.projectforge.business.address.ImageType
import org.projectforge.common.DataSizeConfig
import org.projectforge.jcr.FileInfo
import org.projectforge.jcr.FileSizeStandardChecker
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


private val log = KotlinLogging.logger {}

/**
 * For uploading address immages.
 */
@RestController
@RequestMapping("${Rest.URL}/address")
class AddressImageServicesRest {
    internal class SessionImage(val filename: String, val imageType: ImageType, val bytes: ByteArray)

    @Value("\${${MAX_IMAGE_SIZE_SPRING_PROPERTY}:500KB}")
    internal lateinit var maxImageSizeConfig: String

    lateinit var maxImageSize: DataSize
        internal set

    private lateinit var fileSizeStandardChecker: FileSizeStandardChecker

    @PostConstruct
    private fun postConstruct() {
        maxImageSize = DataSizeConfig.init(maxImageSizeConfig, DataUnit.MEGABYTES)
        log.info { "Maximum configured size of images: ${MAX_IMAGE_SIZE_SPRING_PROPERTY}=$maxImageSizeConfig." }
        fileSizeStandardChecker = FileSizeStandardChecker(maxImageSize.toBytes(), MAX_IMAGE_SIZE_SPRING_PROPERTY)
    }

    @Autowired
    private lateinit var addressImageDao: AddressImageDao


    /**
     * If given and greater 0, the image will be added to the address with the given id (pk), otherwise the image is
     * stored in the user's session and will be used for the next update or save event.
     */
    @PostMapping("uploadImage/{id}")
    fun uploadFile(
        @PathVariable("id") id: Long?,
        @RequestParam("file") file: MultipartFile,
        request: HttpServletRequest
    ):
            ResponseEntity<*> {
        val filename = file.originalFilename!!
        val imageType = addressImageDao.getSupportedImageType(filename) ?: run {
            return ResponseEntity("Unsupported file: $filename. Only files of types {${ImageType.entries.joinToString ()}} supported", HttpStatus.BAD_REQUEST)
        }
        fileSizeStandardChecker.checkSize(FileInfo(filename, fileSize = file.size), displayUserMessage = false)
        val bytes = file.bytes
        if (id == null || id < 0) {
            val session = request.getSession(false)
            val image = SessionImage(filename, imageType, bytes)
            ExpiringSessionAttributes.setAttribute(session, SESSION_IMAGE_ATTR, image, 1)
        } else {
            addressImageDao.saveOrUpdate(id, bytes, imageType)
        }
        return ResponseEntity("OK", HttpStatus.OK)
    }

    /**
     * @param id The id of the address the image is assigned to.
     */
    @GetMapping("image/{id}")
    fun getImage(@PathVariable("id") id: Long): ResponseEntity<Resource> {
        val addressImage = addressImageDao.findImage(id, fetchImage = true) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val resource = ByteArrayResource(addressImage.image)
        val extension = addressImage.imageType?.extension ?: "jpg"
        return RestUtils.downloadFile("ProjectForge-addressImage_$id.$extension", resource)
    }

    /**
     * @param id The id of the address the image is assigned to.
     */
    @GetMapping("imagePreview/{id}")
    fun getImagePreview(@PathVariable("id") id: Long): ResponseEntity<Resource> {
        val addressImage = addressImageDao.findImage(id, fetchPreviewImage = true) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val resource = ByteArrayResource(addressImage.imagePreview)
        val extension = addressImage.imageType?.extension ?: "jpg"
        return RestUtils.downloadFile("ProjectForge-addressImagePreview_$id.$extension", resource)
    }

    /**
     * If given and greater 0, the image will be deleted from the address with the given id (pk), otherwise the image is
     * removed from the user's session and will not be used for the next update or save event anymore.
     * @param id The id of the address the image is assigned to.
     */
    @DeleteMapping("deleteImage/{id}")
    fun deleteImage(request: HttpServletRequest, @PathVariable("id") id: Long?): ResponseEntity<String> {
        val session = request.getSession(false)
        ExpiringSessionAttributes.removeAttribute(session, SESSION_IMAGE_ATTR)
        if (id != null && id > 0) {
            addressImageDao.delete(id)
        }
        return ResponseEntity("OK", HttpStatus.OK)
    }

    companion object {
        internal const val SESSION_IMAGE_ATTR = "uploadedAddressImage"
        private const val MAX_IMAGE_SIZE_SPRING_PROPERTY = "projectforge.address.maxImageSize"

        /**
         * Maximum file size for images stored in database.
         * Images larger than this will be automatically compressed to fit.
         */
        const val MAX_STORED_IMAGE_FILE_SIZE_KB = 100L
    }
}
