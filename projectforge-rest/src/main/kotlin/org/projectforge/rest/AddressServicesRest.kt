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

package org.projectforge.rest

import de.micromata.merlin.utils.ReplaceUtils
import jakarta.servlet.http.HttpServletRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.io.IOUtils
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressTextImportService
import org.projectforge.business.address.FormOfAddress
import org.projectforge.business.address.AddressExport
import org.projectforge.business.address.AddressImageDO
import org.projectforge.business.address.ImageType
import org.projectforge.business.address.PersonalAddressDao
import org.projectforge.business.address.vcard.VCardUtils
import org.projectforge.common.BeanHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.LocaleUtils
import org.projectforge.rest.dto.Address
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.UIToast
import org.projectforge.rest.address.importer.AddressImportDTO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.LanguageService
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.UIColor
import org.projectforge.rest.ResponseData
import org.projectforge.rest.MessageType
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * For uploading address immages.
 */
@RestController
@RequestMapping("${Rest.URL}/address")
class AddressServicesRest {

    internal class SessionVcfImage(
        val filename: String,
        val imageType: ImageType,
        val bytes: ByteArray,
        val imageTooLarge: Boolean
    )

    companion object {
        private const val APPLE_SCRIPT_DIR = "misc/"
        private const val APPLE_SCRIPT_FOR_ADDRESS_BOOK = "AddressBookRemoveNotesOfClassWork.scpt"
        internal const val SESSION_VCF_IMAGE_ATTR = "vcfImage"
    }

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressExport: AddressExport

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    @Autowired
    private lateinit var languageService: LanguageService

    @Autowired
    private lateinit var addressImageServicesRest: AddressImageServicesRest

    @Autowired
    private lateinit var imageService: org.projectforge.business.image.ImageService

    @Autowired
    private lateinit var addressTextImportService: AddressTextImportService

    @GetMapping("acLang")
    fun getLanguages(@RequestParam("search") search: String?): List<DisplayLanguage> {
        return languageService.getLanguages(search).map { DisplayLanguage(it.value, it.label) }
    }

    @GetMapping("usedLanguages")
    fun getUsedLanguages(): List<LanguageService.Language> {
        return languageService.getLanguages(addressDao.usedCommunicationLanguages.asIterable())
    }

    @GetMapping("exportFavoritesVCards")
    fun exportFavoritesVCards(): ResponseEntity<*> {
        log.info { "Exporting personal address book as vcards." }
        val list = addressDao.favoriteVCards
        if (list.isNullOrEmpty()) {
            return ResponseEntity(
                ResponseData(
                    "address.book.hasNoVCards",
                    messageType = MessageType.TOAST,
                    color = UIColor.WARNING
                ), HttpStatus.NOT_FOUND
            )
        }
        val filename = ("ProjectForge-PersonalAddressBook_" + DateHelper.getDateAsFilenameSuffix(Date())
                + ".vcf")
        val writer = StringWriter()
        addressDao.exportFavoriteVCards(writer, list, embedImage = true)

        return RestUtils.downloadFile(filename, writer.toString())
    }

    /**
     * Exports favorites addresses.
     */
    @GetMapping("exportFavoritesExcel")
    fun exportFavoritesExcel(request: HttpServletRequest): ResponseEntity<*> {
        log.info { "Exporting personal address book as Excel file." }
        val list = addressDao.favoriteVCards.map { it.address!! }
        val personalAddressMap = personalAddressDao.personalAddressByAddressId

        val xls = addressExport.export(list, personalAddressMap)
        if (xls == null || xls.isEmpty()) {
            return ResponseEntity(
                ResponseData(
                    "address.book.hasNoVCards",
                    messageType = MessageType.TOAST,
                    color = UIColor.WARNING
                ), HttpStatus.NOT_FOUND
            )
        }
        val filename = ("ProjectForge-AddressExport_" + DateHelper.getDateAsFilenameSuffix(Date())
                + ".xlsx")

        val resource = ByteArrayResource(xls)
        return RestUtils.downloadFile(filename, resource)
    }

    @GetMapping("downloadAppleScript")
    fun downloadAppleScript(): ResponseEntity<*> {
        log.info { "Downloading AppleScript." }
        val content: ByteArray?
        val file = APPLE_SCRIPT_DIR + APPLE_SCRIPT_FOR_ADDRESS_BOOK
        try {
            val cLoader = this.javaClass.classLoader
            val inputStream = cLoader.getResourceAsStream(file)
            if (inputStream == null) {
                log.error { "Could not find script in resource path: '$file'." }
            }
            content = IOUtils.toByteArray(inputStream!!)
        } catch (ex: IOException) {
            log.error(ex) { "Could not load script '" + file + "'." + ex.message }
            throw RuntimeException(ex)
        }
        val filename = (APPLE_SCRIPT_FOR_ADDRESS_BOOK)
        val resource = ByteArrayResource(content!!)
        return RestUtils.downloadFile(filename, resource)
    }

    @GetMapping("exportVCard/{id}")
    fun exportVCard(@PathVariable("id") id: Long?): ResponseEntity<*> {
        val addressDO = addressDao.find(id) ?: return ResponseEntity<Any>(HttpStatus.NOT_FOUND)

        // Use AddressDao for VCard export (handles image fetching internally)
        val vcardString = addressDao.exportVCard(addressDO, embedImage = true)

        val filename = "ProjectForge-" + ReplaceUtils.encodeFilename(addressDO.fullName, true) + "_" +
                DateHelper.getDateAsFilenameSuffix(Date()) + ".vcf"
        return RestUtils.downloadFile(filename, vcardString)
    }

    /**
     * Parses a VCF file and finds the best matching address based on name/firstName.
     * Used by the text parser to drop VCF files for quick import.
     */
    @PostMapping("parseVcf")
    fun parseVcf(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("addressId") addressId: String?,
        request: HttpServletRequest
    ): ResponseEntity<*> {
        // 1. Validierung
        if (file.isEmpty) {
            return ResponseEntity.ok(
                ResponseAction(targetType = TargetType.NOTHING)
                    .addVariable("error", translate("file.upload.error.empty"))
            )
        }

        val filename = file.originalFilename ?: "unknown"
        if (!filename.lowercase().endsWith(".vcf")) {
            return ResponseEntity.ok(
                ResponseAction(targetType = TargetType.NOTHING)
                    .addVariable("error", translate("address.book.vCardsImport.wrongFileType"))
            )
        }

        // 2. Lese aktuelle Adresse aus DB (für Matching)
        val currentAddress = if (!addressId.isNullOrBlank() && addressId != "0") {
            try {
                addressDao.find(addressId.toLong())
            } catch (e: NumberFormatException) {
                null
            }
        } else {
            null
        }

        try {
            // 3. Parse VCF-Datei
            val vcfBytes = file.inputStream.readBytes()
            log.info { "Parsing VCF file: $filename (${vcfBytes.size} bytes)" }
            val parsedAddresses = VCardUtils.parseFromByteArray(vcfBytes)

            if (parsedAddresses.isEmpty()) {
                return ResponseEntity.ok(
                    ResponseAction(targetType = TargetType.NOTHING)
                        .addVariable("error", translate("address.book.vCardsImport.error.noAddresses"))
                )
            }

            log.info { "Parsed ${parsedAddresses.size} addresses from VCF file" }

            // 4. Konvertiere zu AddressImportDTO und berechne Match-Score
            val matches = parsedAddresses.map { addressDO ->
                val dto = AddressImportDTO()
                dto.copyFrom(addressDO)

                // Handle image from VCard (stored as transient attribute)
                val imageData = addressDO.transientImage
                if (imageData != null) {
                    dto.setTransientImage(imageData, imageService)
                }

                val score = if (currentAddress != null) {
                    dto.matchScore(currentAddress, logErrors = true)
                } else {
                    0
                }

                Pair(dto, score)
            }

            // 5. Sortiere nach Score und wähle beste Match
            val bestMatch = matches.maxByOrNull { it.second }

            // 6. Prüfe ob Match gut genug ist (Score >= 50 oder neue Adresse)
            // Wenn nur eine einzige vCard vorhanden ist, verwende diese immer (auch bei niedrigem Score)
            val singleVCardMatch = parsedAddresses.size == 1
            if (currentAddress != null && !singleVCardMatch && (bestMatch == null || bestMatch.second < 50)) {
                log.warn { "No matching address found in VCF file for: ${currentAddress.name}, ${currentAddress.firstName}" }
                return ResponseEntity.ok(
                    ResponseAction(targetType = TargetType.NOTHING)
                        .addVariable("error", translate("address.book.vCardsImport.error.noMatch"))
                )
            }

            // 7. Konvertiere zu parsedData-Format
            val matchedDto = bestMatch?.first ?: matches.first().first
            log.info { "Best match: ${matchedDto.name}, ${matchedDto.firstName} (score=${bestMatch?.second ?: 0}, singleVCard=$singleVCardMatch)" }
            val parsedData = convertVcfToParsedDataFormat(matchedDto, currentAddress)

            // 8. Handle image from VCF (if present)
            var hasImage = false
            var imageTooLarge = false
            val session = request.getSession(false)

            matchedDto.transientImage?.let { imageData ->
                imageData.image?.let { imageBytes ->
                    hasImage = true
                    val imageSize = imageBytes.size.toLong()
                    val maxSize = addressImageServicesRest.maxImageSize.toBytes()
                    imageTooLarge = imageSize > maxSize

                    log.info { "VCF contains image: ${imageBytes.size} bytes (max: $maxSize bytes, tooLarge: $imageTooLarge)" }

                    // Store in session for later use
                    val sessionImage = SessionVcfImage(
                        filename = filename,
                        imageType = imageData.imageType ?: ImageType.PNG,
                        bytes = imageBytes,
                        imageTooLarge = imageTooLarge
                    )
                    ExpiringSessionAttributes.setAttribute(
                        session,
                        SESSION_VCF_IMAGE_ATTR,
                        sessionImage,
                        AddressImageServicesRest.SESSION_IMAGE_TTL_MINUTES,
                    )
                }
            }

            // Clear VCF image from session if no image in new VCF
            if (!hasImage) {
                ExpiringSessionAttributes.removeAttribute(session, SESSION_VCF_IMAGE_ATTR)
                log.info { "No image in VCF - cleared session image" }
            }

            return ResponseEntity.ok(
                ResponseAction(targetType = TargetType.NOTHING)
                    .addVariable("parsedData", parsedData)
                    .addVariable("hasImage", hasImage)
                    .addVariable("imageTooLarge", imageTooLarge)
            )

        } catch (e: Exception) {
            log.error(e) { "Error parsing VCF file: $filename" }
            return ResponseEntity.ok(
                ResponseAction(targetType = TargetType.NOTHING)
                    .addVariable("error", translate("address.book.vCardsImport.error.parsing"))
            )
        }
    }

    /**
     * Returns the image preview from a VCF import stored in the session.
     * This is only available after parseVcf has been called and only if imageTooLarge is false.
     */
    @GetMapping("vcfImagePreview")
    fun getVcfImagePreview(request: HttpServletRequest): ResponseEntity<Resource> {
        val session = request.getSession(false)
        val sessionImage = ExpiringSessionAttributes.getAttribute(session, SESSION_VCF_IMAGE_ATTR) as? SessionVcfImage
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)

        if (sessionImage.imageTooLarge) {
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }

        val resource = ByteArrayResource(sessionImage.bytes)
        return RestUtils.downloadFile("vcf-image-preview.${sessionImage.imageType.extension}", resource)
    }

    /**
     * Parses address text (email signature) and returns structured field mapping.
     */
    @AccessChecked("no data access: parses the posted text only")
    @PostMapping("parseText")
    fun parseText(@RequestBody data: PostData<Map<String, String>>): ResponseEntity<ResponseAction> {
        val inputText = data.data["inputText"] ?: ""
        log.info { "Parsing address text (${inputText.length} characters)" }

        val parsed = addressTextImportService.parseText(inputText)

        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("parsedData", parsed)
        )
    }

    /**
     * DTO for applying parsed address data.
     */
    data class ApplyParsedDataRequest(
        val address: Address,
        val selectedFields: Map<String, String>,
        val applyImage: Boolean = false
    )

    /**
     * Applies parsed data to address form fields.
     */
    @AccessChecked("no data access: applies the posted fields to the posted address only")
    @PostMapping("applyParsedData")
    fun applyParsedData(
        @RequestBody request: ApplyParsedDataRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ResponseAction> {
        val address = request.address
        val selectedFields = request.selectedFields
        val applyImage = request.applyImage

        // Handle image from VCF if requested
        var vcfImageBase64: String? = null
        var vcfImageFilename: String? = null
        var vcfImageMimeType: String? = null
        if (applyImage) {
            val session = httpRequest.getSession(false)
            val vcfImage = ExpiringSessionAttributes.getAttribute(session, SESSION_VCF_IMAGE_ATTR)
                    as? SessionVcfImage

            if (vcfImage != null && !vcfImage.imageTooLarge) {
                // Send image bytes to frontend (Base64-encoded)
                vcfImageBase64 = Base64.getEncoder().encodeToString(vcfImage.bytes)
                vcfImageFilename = vcfImage.filename
                vcfImageMimeType = vcfImage.imageType.mimeType

                // Clear VCF image from session (frontend will handle upload)
                ExpiringSessionAttributes.removeAttribute(session, SESSION_VCF_IMAGE_ATTR)
                log.debug { "Sending VCF image to frontend: ${vcfImage.filename} (${vcfImage.bytes.size} bytes, ${vcfImage.imageType})" }
            }
        }

        // Apply only selected fields
        selectedFields.forEach { (fieldName, value) ->
            when (fieldName) {
                "title" -> address.title = value
                "firstName" -> address.firstName = value
                "name" -> address.name = value
                "organization" -> address.organization = value
                "division" -> address.division = value
                "positionText" -> address.positionText = value
                "businessPhone" -> address.businessPhone = value
                "mobilePhone" -> address.mobilePhone = value
                "fax" -> address.fax = value
                "privatePhone" -> address.privatePhone = value
                "privateMobilePhone" -> address.privateMobilePhone = value
                "email" -> address.email = value
                "privateEmail" -> address.privateEmail = value
                // Business address fields
                "addressText" -> address.addressText = value
                "addressText2" -> address.addressText2 = value
                "zipCode" -> address.zipCode = value
                "city" -> address.city = value
                "state" -> address.state = value
                "country" -> address.country = value
                // Postal address fields
                "postalAddressText" -> address.postalAddressText = value
                "postalAddressText2" -> address.postalAddressText2 = value
                "postalZipCode" -> address.postalZipCode = value
                "postalCity" -> address.postalCity = value
                "postalState" -> address.postalState = value
                "postalCountry" -> address.postalCountry = value
                // Private address fields
                "privateAddressText" -> address.privateAddressText = value
                "privateAddressText2" -> address.privateAddressText2 = value
                "privateZipCode" -> address.privateZipCode = value
                "privateCity" -> address.privateCity = value
                "privateState" -> address.privateState = value
                "privateCountry" -> address.privateCountry = value
                "website" -> address.website = value
                "comment" -> address.comment = value
                // Additional VCard fields
                "birthName" -> address.birthName = value
                "birthday" -> address.birthday = tryParseLocalDate(value)
                "fingerprint" -> address.fingerprint = value
                "publicKey" -> address.publicKey = value
                "form" -> address.form = tryParseFormOfAddress(value)
                "communicationLanguage" -> address.communicationLanguage = tryParseLocale(value)
            }
        }

        log.debug { "Applied ${selectedFields.size} parsed fields to address" }

        val response = ResponseAction(targetType = TargetType.UPDATE)
            .addVariable("data", address)
            .addVariable("ui", UIToast.createToast(translate("address.parseText.applied")))

        // Include VCF image data if present (frontend will handle upload)
        if (vcfImageBase64 != null) {
            response.addVariable("vcfImageData", vcfImageBase64)
            response.addVariable("vcfImageFilename", vcfImageFilename)
            response.addVariable("vcfImageMimeType", vcfImageMimeType)
        }

        return ResponseEntity.ok(response)
    }

    /**
     * Takes the image of the last parsed vCard (see [parseVcf], or an entry of the VCF import) over as the uploaded
     * image of the address form, which the next save stores (see `AddressSaveSupport`). Unlike the image data
     * [applyParsedData] returns for the React form, nothing travels to the client and back.
     */
    @AccessChecked("own session only")
    @PostMapping("applyVcfImage")
    fun applyVcfImage(request: HttpServletRequest): ResponseEntity<String> {
        val session = request.getSession(false)
        val vcfImage = ExpiringSessionAttributes.getAttribute(session, SESSION_VCF_IMAGE_ATTR) as? SessionVcfImage
        if (vcfImage == null || vcfImage.imageTooLarge) {
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
        val image = AddressImageServicesRest.SessionImage(vcfImage.filename, vcfImage.imageType, vcfImage.bytes)
        ExpiringSessionAttributes.setAttribute(
            session,
            AddressImageServicesRest.SESSION_IMAGE_ATTR,
            image,
            AddressImageServicesRest.SESSION_IMAGE_TTL_MINUTES,
        )
        ExpiringSessionAttributes.removeAttribute(session, SESSION_VCF_IMAGE_ATTR)
        return ResponseEntity("OK", HttpStatus.OK)
    }

    /**
     * Converts VCF data to the parsedData format expected by the text parser.
     */
    private fun convertVcfToParsedDataFormat(
        vcfData: AddressImportDTO,
        currentAddress: org.projectforge.business.address.AddressDO?
    ): Map<String, Any> {
        val fields = mutableMapOf<String, Map<String, Any>>()

        // Für jedes Feld in vcfData: Wenn es einen Wert hat und vom DB-Wert abweicht
        vcfData.properties.forEach { property ->
            val vcfValue = BeanHelper.getProperty(vcfData, property.name)
            val dbValue = if (currentAddress != null) {
                BeanHelper.getProperty(currentAddress, property.name)
            } else {
                null
            }

            if (vcfValue != null && vcfValue.toString().isNotBlank() && vcfValue != dbValue) {
                fields[property.name] = mapOf(
                    "value" to vcfValue,
                    "confidence" to "high",
                    "currentValue" to (dbValue?.toString() ?: "")
                )
            }
        }

        log.info { "Converted VCF data to parsedData format: ${fields.size} fields" }

        return mapOf(
            "fields" to fields,
            "warnings" to emptyList<String>()
        )
    }

    class DisplayLanguage(val id: String, val displayName: String)

    /**
     * Helper function to parse LocalDate from string using PFDayUtils.parseDate().
     * Accepts ISO format (YYYY-MM-DD), user's date format, and other formats.
     * Returns null if parsing fails.
     */
    private fun tryParseLocalDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return PFDayUtils.parseDate(value)
    }

    /**
     * Helper function to parse FormOfAddress from string representation.
     * Uses VCardUtils.parseFormOfAddress() which checks all supported locales.
     * Accepts both enum names (PERSON_MALE) and localized display strings (Herr, Mr.).
     * Returns null if parsing fails.
     */
    private fun tryParseFormOfAddress(value: String?): FormOfAddress? {
        return VCardUtils.parseFormOfAddress(value)
    }

    /**
     * Helper function to parse Locale from language tag string.
     * Uses LocaleUtils.parse() which handles language tags (e.g., "de", "en-US").
     * Returns null if parsing fails.
     */
    private fun tryParseLocale(value: String?): Locale? {
        return LocaleUtils.parse(value)
    }
}

