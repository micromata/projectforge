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

package org.projectforge.rest.fibu

import de.micromata.merlin.excel.ExcelWorkbook
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.locale
import org.projectforge.rest.config.Rest
import org.projectforge.rest.fibu.importer.EingangsrechnungImportStorage
import org.projectforge.rest.fibu.importer.IncomingInvoicePosExcelParser
import org.projectforge.rest.importer.AbstractImportUploadPageRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.InputStream

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/uploadIncomingInvoices")
class EingangsrechnungUploadPageRest : AbstractImportUploadPageRest() {
    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var kontoCache: KontoCache

    // Temporary storage for the parsed import data
    private var lastParsedStorage: EingangsrechnungImportStorage? = null

    override val title: String
        get() = translate("fibu.eingangsrechnung.import.title")

    override val description: String?
        get() = translate("fibu.eingangsrechnung.import.description")

    override val templateInfo: String?
        get() = translate("fibu.eingangsrechnung.import.templateInfo")

    override fun callerPage(request: HttpServletRequest): String {
        return "/wa/incomingInvoiceList" //PagesResolver.getListPageUrl(EingangsrechnungPagesRest::class.java, absolute = true)
    }

    override fun successPage(request: HttpServletRequest): String {
        val storage = lastParsedStorage
        return if (storage != null) {
            val navigationUrl = IncomingInvoicePosExcelParser.storeInSessionAndGetNavigationUrl(request, storage)
            log.info("Navigation URL for import page: $navigationUrl")
            log.info("Storage contains ${storage.readInvoices.size} invoices, ${storage.consolidatedInvoices.size} consolidated")
            navigationUrl
        } else {
            log.warn("No storage available in successPage, falling back to caller page")
            callerPage(request)
        }
    }

    override fun proceedUpload(inputstream: InputStream, filename: String): String? {
        val storage = EingangsrechnungImportStorage()
        ExcelWorkbook(inputstream, filename, locale).use { workbook ->
            IncomingInvoicePosExcelParser(
                storage = storage,
                eingangsrechnungDao = eingangsrechnungDao,
                kostCache = kostCache,
                kontoCache = kontoCache,
            ).parse(workbook)
        }

        // Store for later use in successPage()
        lastParsedStorage = storage
        return null // Success - continue to successPage()
    }
}
