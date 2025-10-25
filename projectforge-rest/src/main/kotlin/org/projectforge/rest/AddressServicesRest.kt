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

import de.micromata.merlin.utils.ReplaceUtils
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressExport
import org.projectforge.business.address.PersonalAddressDao
import org.projectforge.business.address.vcard.VCardUtils
import org.projectforge.common.BeanHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.time.DateHelper
import org.projectforge.rest.address.importer.AddressImportDTO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.LanguageService
import org.projectforge.rest.core.ResultSet
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.UIColor
import org.projectforge.rest.ResponseData
import org.projectforge.rest.MessageType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * For uploading address immages.
 */
@RestController
@RequestMapping("${Rest.URL}/address")
class AddressServicesRest {

    companion object {
        internal const val SESSION_IMAGE_ATTR = "uploadedAddressImage"
        private const val APPLE_SCRIPT_DIR = "misc/"
        private const val APPLE_SCRIPT_FOR_ADDRESS_BOOK = "AddressBookRemoveNotesOfClassWork.scpt"
    }

    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var addressRest: AddressPagesRest

    @Autowired
    private lateinit var addressExport: AddressExport

    @Autowired
    private lateinit var personalAddressDao: PersonalAddressDao

    @Autowired
    private lateinit var languageService: LanguageService

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
        log.info("Exporting personal address book as vcards.")
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
        addressDao.exportFavoriteVCards(writer, list)

        return RestUtils.downloadFile(filename, writer.toString())
    }

    /**
     * Exports favorites addresses.
     */
    @GetMapping("exportFavoritesExcel")
    fun exportFavoritesExcel(request: HttpServletRequest): ResponseEntity<*> {
        log.info("Exporting personal address book as Excel file.")
        val list = addressDao.favoriteVCards.map { it.address!! }
        val magicFilter = MagicFilter(maxRows = QueryFilter.QUERY_FILTER_MAX_ROWS)
        val resultSet = ResultSet(list, null, list.size, magicFilter = magicFilter)
        addressRest.postProcessResultSet(resultSet, request, magicFilter)

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
        log.info("Downloading AppleScript.")
        val content: ByteArray?
        val file = APPLE_SCRIPT_DIR + APPLE_SCRIPT_FOR_ADDRESS_BOOK
        try {
            val cLoader = this.javaClass.classLoader
            val inputStream = cLoader.getResourceAsStream(file)
            if (inputStream == null) {
                log.error("Could not find script in resource path: '$file'.")
            }
            content = IOUtils.toByteArray(inputStream!!)
        } catch (ex: IOException) {
            log.error("Could not load script '" + file + "'." + ex.message, ex)
            throw RuntimeException(ex)
        }
        val filename = (APPLE_SCRIPT_FOR_ADDRESS_BOOK)
        val resource = ByteArrayResource(content!!)
        return RestUtils.downloadFile(filename, resource)
    }

    @GetMapping("exportVCard/{id}")
    fun exportVCard(@PathVariable("id") id: Long?): ResponseEntity<*> {
        val address = addressDao.find(id) ?: return ResponseEntity<Any>(HttpStatus.NOT_FOUND)
        val filename = ("ProjectForge-" + ReplaceUtils.encodeFilename(address.fullName, true) + "_"
                + DateHelper.getDateAsFilenameSuffix(Date()) + ".vcf")
        val writer = StringWriter()
        addressDao.exportVCard(PrintWriter(writer), address)
        return RestUtils.downloadFile(filename, writer.toString())
    }

    /**
     * Parses a VCF file and finds the best matching address based on name/firstName.
     * Used by the text parser to drop VCF files for quick import.
     */
    @PostMapping("parseVcf")
    fun parseVcf(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("addressId") addressId: String?
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
                val imageData = addressDO.getTransientAttribute("image")
                if (imageData != null) {
                    dto.setTransientAttribute("image", imageData)
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

            return ResponseEntity.ok(
                ResponseAction(targetType = TargetType.NOTHING)
                    .addVariable("parsedData", parsedData)
            )

        } catch (e: Exception) {
            log.error("Error parsing VCF file: $filename", e)
            return ResponseEntity.ok(
                ResponseAction(targetType = TargetType.NOTHING)
                    .addVariable("error", translate("address.book.vCardsImport.error.parsing"))
            )
        }
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
}
