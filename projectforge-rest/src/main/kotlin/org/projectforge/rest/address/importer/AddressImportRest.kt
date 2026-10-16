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

package org.projectforge.rest.address.importer

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.FileCheck
import org.projectforge.rest.AddressImageServicesRest
import org.projectforge.rest.AddressServicesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

private val log = KotlinLogging.logger {}

/**
 * The layout-free VCF import of addresses (`next/address/import`), replacing the React page
 * `AddressImportUploadPageRest`.
 *
 * After the upload every vCard is matched against the database at once and the result kept in the session
 * ([AddressImportSupport]). There is no bulk commit: the user opens an entry in the address form, which compares the
 * fields of the vCard with the stored ones ([getEntry]), and saves it there; the save marks the entry as imported
 * (`AddressSaveSupport.reconcileImport`).
 */
@RestController
@RequestMapping("${Rest.URL}/addressImport")
class AddressImportRest {
    /**
     * The fields of the vCard of one entry next to the stored ones, for the comparison of the address form.
     */
    class AddressImportEntryComparison(
        val importIndex: Int,
        /** The id of the matching address, null if the entry is a new one. */
        val addressId: Long?,
        val status: String,
        /** Field name -> map with the values "vcf" and "db". */
        val vcfComparisonData: Map<String, Map<String, String?>>,
        /** Whether the vCard carries an image; staged for `address/vcfImagePreview` and `address/applyVcfImage`. */
        val hasImage: Boolean,
        /** The image is too large to be taken over. */
        val imageTooLarge: Boolean,
    )

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressImageServicesRest: AddressImageServicesRest

    /**
     * The import in the session, or an empty view if there is none.
     */
    @AccessChecked("insert access to addresses (checkRight); own session only")
    @GetMapping("state")
    fun getState(request: HttpServletRequest): AddressImportView {
        checkRight()
        val storage = AddressImportSupport.getStorage(request) ?: return AddressImportView()
        return AddressImportView(storage)
    }

    /**
     * Parses the uploaded VCF file, matches its vCards against the database and keeps the result in the session.
     * A refusal (empty file, wrong type, nothing parsed) is a 400 carrying `{ error }`.
     */
    @AccessChecked("insert access to addresses (checkRight); matching reads the addresses the user may see")
    @PostMapping("upload")
    fun upload(request: HttpServletRequest, @RequestParam("file") file: MultipartFile): ResponseEntity<*> {
        checkRight()
        val filename = file.originalFilename ?: "unknown"
        log.info { "User tries to upload VCF import file: '$filename', size=${file.size} bytes." }
        if (file.isEmpty) {
            return rejected(translate("file.upload.error.empty"))
        }
        FileCheck.checkFile(filename, file.size, *FILE_EXTENSIONS, megaBytes = MAX_FILE_SIZE_MB)?.let { error ->
            return rejected(error)
        }
        try {
            val storage = AddressImportStorage()
            storage.filename = filename
            file.inputStream.use { storage.parseVcfData(it.readBytes()) }
            if (storage.readAddresses.isEmpty()) {
                return rejected(translate("address.book.vCardsImport.error.noAddresses"))
            }
            storage.reconcileImportStorage()
            AddressImportSupport.storeStorage(request, storage)
            log.info { "VCF parsed and reconciled successfully: ${storage.pairEntries.size} entries." }
            return ResponseEntity.ok(AddressImportView(storage))
        } catch (ex: Exception) {
            log.error(ex) { "Error processing uploaded file: $filename" }
            return rejected(translate("address.book.vCardsImport.error.parsing"))
        }
    }

    /**
     * The comparison of one entry for the address form. Stages the image of its vCard in the session, so the form
     * may preview and take it over.
     */
    @AccessChecked("insert access to addresses (checkRight); own session only")
    @GetMapping("entry/{index}")
    fun getEntry(request: HttpServletRequest, @PathVariable("index") index: Int): ResponseEntity<AddressImportEntryComparison> {
        checkRight()
        val storage = AddressImportSupport.getStorage(request) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val entry = storage.getImportEntryByIndex(index) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val vcfData = entry.read
        val session = request.getSession(false)
        var imageTooLarge = false
        val image = vcfData?.addressImage
        if (image != null) {
            imageTooLarge = image.bytes.size.toLong() > addressImageServicesRest.maxImageSize.toBytes()
            ExpiringSessionAttributes.setAttribute(
                session,
                AddressServicesRest.SESSION_VCF_IMAGE_ATTR,
                AddressServicesRest.SessionVcfImage(
                    filename = storage.filename ?: "import.vcf",
                    imageType = image.imageType,
                    bytes = image.bytes,
                    imageTooLarge = imageTooLarge,
                ),
                AddressImageServicesRest.SESSION_IMAGE_TTL_MINUTES,
            )
        } else {
            ExpiringSessionAttributes.removeAttribute(session, AddressServicesRest.SESSION_VCF_IMAGE_ATTR)
        }
        return ResponseEntity.ok(
            AddressImportEntryComparison(
                importIndex = index,
                addressId = entry.stored?.id,
                status = entry.status.name,
                vcfComparisonData = AddressImportSupport.buildVcfComparisonData(vcfData, entry.stored),
                hasImage = image != null,
                imageTooLarge = imageTooLarge,
            )
        )
    }

    /**
     * The preview image of the vCard of an entry (or the image itself, if there is no preview).
     */
    @AccessChecked("own session only")
    @GetMapping("imagePreview/{index}")
    fun getImagePreview(request: HttpServletRequest, @PathVariable("index") index: Int): ResponseEntity<Resource> {
        val storage = AddressImportSupport.getStorage(request) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val image = storage.getImportEntryByIndex(index)?.read?.addressImage ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        val bytes = image.previewBytes ?: image.bytes
        return RestUtils.downloadFile("imagePreview.${image.imageType.extension}", ByteArrayResource(bytes))
    }

    /**
     * Drops the import from the session.
     */
    @AccessChecked("own session only")
    @PostMapping("cancel")
    fun cancel(request: HttpServletRequest): ResponseEntity<Void> {
        AddressImportSupport.clearStorage(request)
        return ResponseEntity.ok().build()
    }

    private fun checkRight() {
        addressDao.checkLoggedInUserInsertAccess(AddressDO())
    }

    private fun rejected(error: String): ResponseEntity<Map<String, String>> {
        return ResponseEntity.badRequest().body(mapOf("error" to error))
    }

    companion object {
        private val FILE_EXTENSIONS = arrayOf("vcf")
        private const val MAX_FILE_SIZE_MB = 10L
    }
}
