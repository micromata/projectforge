/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.address.AddressDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletRequest


/**
 * For uploading address immages.
 */
@RestController
@RequestMapping("${Rest.URL}/address")
class AddressImageServicesRest() {

    companion object {
        internal val SESSION_IMAGE_ATTR = "uploadedAddressImage"
    }

    private val log = org.slf4j.LoggerFactory.getLogger(AddressImageServicesRest::class.java)

    @Autowired
    private lateinit var addressDao: AddressDao

    /**
     * If given and greater 0, the image will be added to the address with the given id (pk), otherwise the image is
     * stored in the user's session and will be used for the next update or save event.
     */
    @PostMapping("uploadImage/{id}")
    fun uploadFile(@PathVariable("id") id: Int?, @RequestParam("file") file: MultipartFile, request: HttpServletRequest):
            ResponseEntity<String> {
        val filename = file.originalFilename
        if (filename == null || !filename.endsWith(".png", true)) {
            return ResponseEntity("Unsupported file: ${filename}. Only png files supported", HttpStatus.BAD_REQUEST)
        }
        val bytes = file.bytes
        if (id == null || id < 0) {
            val session = request.session
            ExpiringSessionAttributes.setAttribute(session, SESSION_IMAGE_ATTR, bytes, 1)
        } else {
            val address = addressDao.getById(id)
            if (address == null)
                return ResponseEntity("Not found.", HttpStatus.NOT_FOUND)
            address.imageData = bytes
            addressDao.update(address)
            log.info("New image for address $id (${address.fullName}) saved.")
        }
        return ResponseEntity("OK", HttpStatus.OK)
    }

    @GetMapping("image/{id}")
    fun getImage(@PathVariable("id") id: Int): ResponseEntity<Resource> {
        val address = addressDao.getById(id)
        if (address?.imageData == null)
            return ResponseEntity(HttpStatus.NOT_FOUND)
        val resource = ByteArrayResource(address.imageData!!)
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ProjectForge-addressImage_$id.png")
                .body(resource);
    }

    @GetMapping("imagePreview/{id}")
    fun getImagePreview(@PathVariable("id") id: Int?): ResponseEntity<Resource> {
        val address = addressDao.getById(id)
        if (address?.imageDataPreview == null)
            return ResponseEntity(HttpStatus.NOT_FOUND)
        val resource = ByteArrayResource(address.imageDataPreview!!)
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ProjectForge-addressImagePreview_$id.png")
                .body(resource);
    }

    /**
     * If given and greater 0, the image will be deleted from the address with the given id (pk), otherwise the image is
     * removed from the user's session and will not be used for the next update or save event anymore.
     */

    @DeleteMapping("deleteImage/{id}")
    fun deleteImage(request: HttpServletRequest, @PathVariable("id") id: Int?): ResponseEntity<String> {
        if (id == null || id < 0) {
            val session = request.session
            ExpiringSessionAttributes.removeAttribute(session, SESSION_IMAGE_ATTR)
        } else {
            val address = addressDao.getById(id)
            if (address == null)
                return ResponseEntity(HttpStatus.NOT_FOUND)
            address.imageData = null
            address.imageDataPreview = null
            addressDao.update(address)
            log.info("Image for address $id (${address.fullName}) deleted.")
        }
        return ResponseEntity("OK", HttpStatus.OK)
    }
}
