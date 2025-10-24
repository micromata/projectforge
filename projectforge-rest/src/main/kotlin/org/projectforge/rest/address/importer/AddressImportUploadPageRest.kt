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

package org.projectforge.rest.address.importer

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.address.AddressDao
import org.projectforge.framework.access.AccessChecker
import org.projectforge.rest.AddressPagesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.importer.AbstractImportPageRest
import org.projectforge.rest.importer.AbstractImportUploadPageRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.InputStream

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/addressImportUpload")
class AddressImportUploadPageRest : AbstractImportUploadPageRest() {

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var addressDao: AddressDao

    override val title: String = "address.book.vCardImport"

    override val description: String = "address.import.upload.description"

    override val fileExtensions = arrayOf("vcf")

    override val maxFileUploadSizeMB = 10L

    override fun callerPage(request: HttpServletRequest): String {
        return PagesResolver.getListPageUrl(AddressPagesRest::class.java, absolute = true)
    }

    override fun successPage(request: HttpServletRequest): String {
        return PagesResolver.getDynamicPageUrl(AddressImportPageRest::class.java, absolute = true)
    }

    override fun checkRight() {
        addressDao.checkLoggedInUserInsertAccess(org.projectforge.business.address.AddressDO())
    }

    override fun proceedUpload(inputstream: InputStream, filename: String): String? {
        return try {
            // Read VCF file
            val bytes = inputstream.readBytes()

            // Parse VCF data
            val importStorage = AddressImportStorage()
            importStorage.filename = filename
            importStorage.parseVcfData(bytes)

            // Check if any addresses were parsed
            if (importStorage.readAddresses.isEmpty()) {
                return "No addresses found in VCF file"
            }

            // Store in session (expires after 30 minutes)
            val request = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() as?
                    org.springframework.web.context.request.ServletRequestAttributes
            if (request != null) {
                val sessionAttrName = AbstractImportPageRest.getSessionAttributeName(AddressImportPageRest::class.java)
                ExpiringSessionAttributes.setAttribute(request.request, sessionAttrName, importStorage, 30)
                log.info { "VCF parsed successfully: ${importStorage.readAddresses.size} addresses found" }
                null // Success
            } else {
                "Unable to store import data in session"
            }
        } catch (e: Exception) {
            log.error("Error processing VCF file: ${e.message}", e)
            "Error processing VCF file: ${e.message}"
        }
    }
}
