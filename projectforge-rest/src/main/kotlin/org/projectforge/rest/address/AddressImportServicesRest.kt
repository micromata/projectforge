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

package org.projectforge.rest.address

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.address.importer.AddressImportPageRest
import org.projectforge.rest.address.importer.AddressImportStorage
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.importer.AbstractImportPageRest
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.UIColor
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/address")
class AddressImportServicesRest {

    /**
     * Uploads a VCF file and creates an import storage in session.
     * Returns redirect to import page.
     */
    @PostMapping("uploadVcf")
    fun uploadVcf(
        request: HttpServletRequest,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ResponseAction> {
        log.info { "Uploading VCF file: ${file.originalFilename}, size: ${file.size} bytes" }

        // Validate file
        if (file.isEmpty) {
            return ResponseEntity.ok(
                ResponseAction(
                    message = ResponseAction.Message(
                        message = translate("file.upload.error.noFile"),
                        color = UIColor.DANGER
                    ),
                    targetType = TargetType.TOAST
                )
            )
        }

        // Validate file extension
        val filename = file.originalFilename ?: ""
        if (!filename.endsWith(".vcf", ignoreCase = true)) {
            return ResponseEntity.ok(
                ResponseAction(
                    message = ResponseAction.Message(
                        message = translate("address.book.vCardImports.wrongFileType"),
                        color = UIColor.DANGER
                    ),
                    targetType = TargetType.TOAST
                )
            )
        }

        try {
            // Parse VCF file
            val bytes = file.bytes
            val importStorage = AddressImportStorage()
            importStorage.filename = filename
            importStorage.parseVcfData(bytes)

            // Check if any addresses were parsed
            if (importStorage.readAddresses.isEmpty()) {
                return ResponseEntity.ok(
                    ResponseAction(
                        message = ResponseAction.Message(
                            message = translate("address.import.error.noAddresses"),
                            color = UIColor.WARNING
                        ),
                        targetType = TargetType.TOAST
                    )
                )
            }

            // Store in session (expires after 30 minutes)
            val sessionAttrName = AbstractImportPageRest.getSessionAttributeName(AddressImportPageRest::class.java)
            ExpiringSessionAttributes.setAttribute(request, sessionAttrName, importStorage, 30)

            log.info { "VCF parsed successfully: ${importStorage.readAddresses.size} addresses found" }

            // Redirect to import page
            val importPageUrl = PagesResolver.getDynamicPageUrl(
                AddressImportPageRest::class.java,
                absolute = true
            )
            return ResponseEntity.ok(
                ResponseAction(
                    url = importPageUrl,
                    targetType = TargetType.REDIRECT
                )
            )
        } catch (e: Exception) {
            log.error("Error processing VCF file: ${e.message}", e)
            return ResponseEntity.ok(
                ResponseAction(
                    message = ResponseAction.Message(
                        message = translate("address.import.error.parsing") + ": ${e.message}",
                        color = UIColor.DANGER
                    ),
                    targetType = TargetType.TOAST
                )
            )
        }
    }
}
