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

package org.projectforge.rest.fibu.importer

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.importer.AbstractImportPageRest
import org.projectforge.rest.importer.ImportPairEntry
import org.projectforge.rest.importer.ImportStorage
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UIAgGrid
import org.projectforge.ui.UIAgGridColumnDef.Formatter
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/importIncomingInvoicePos")
class IncomingInvoicePosImportPageRest : AbstractImportPageRest<EingangsrechnungPosImportDTO>() {

    @Autowired
    private lateinit var jobHandler: JobHandler

    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    override val title: String = "fibu.eingangsrechnung.import.title"

    override fun callerPage(request: HttpServletRequest): String {
        return "/wa/incomingInvoiceList" //PagesResolver.getListPageUrl(EingangsrechnungPagesRest::class.java, absolute = true)
    }

    override fun clearImportStorage(request: HttpServletRequest) {
        ExpiringSessionAttributes.removeAttribute(
            request,
            getSessionAttributeName(IncomingInvoicePosImportPageRest::class.java),
        )
    }

    override fun getImportStorage(request: HttpServletRequest): EingangsrechnungImportStorage? {
        return ExpiringSessionAttributes.getAttribute(
            request,
            getSessionAttributeName(IncomingInvoicePosImportPageRest::class.java),
            EingangsrechnungImportStorage::class.java,
        )
    }

    override fun import(
        importStorage: ImportStorage<*>,
        selectedEntries: List<ImportPairEntry<EingangsrechnungPosImportDTO>>
    ): Int {
        return jobHandler.addJob(
            EingangsrechnungImportJob(
                eingangsrechnungDao,
                selectedEntries,
                importStorage = importStorage as EingangsrechnungImportStorage,
            )
        ).id
    }

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val importStorage = getImportStorage(request)
        return createFormLayoutData(request, importStorage)
    }

    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        agGrid: UIAgGrid,
    ) {
        val lc = LayoutContext(EingangsrechnungDO::class.java)
        val importStorage = getImportStorage(request)
        val isPositionBasedImport = importStorage?.isPositionBasedImport ?: true

        // Position Number - first column (only for position-based imports)
        if (isPositionBasedImport) {
            addDiffColumn(agGrid, lc, "read.positionNummer", headerName = "label.position.short", width = 60)
        }

        // RENR (Invoice Number) - most important column
        addReadColumn(agGrid, lc, EingangsrechnungDO::referenz)

        // Creditor
        addReadColumn(agGrid, lc, EingangsrechnungDO::kreditor, wrapText = true, filter = true)

        // Datum
        addReadColumn(agGrid, lc, EingangsrechnungDO::datum)

        // Betrag (erstelle custom column da grossSum nicht in EingangsrechnungDO ist)
        addDiffColumn(
            agGrid, lc, "read.grossSum",
            headerName = "fibu.common.betrag",
            formatter = Formatter.CURRENCY_PLAIN
        )

        // Währung (custom column)
        addDiffColumn(agGrid, lc, "read.currency", headerName = "fibu.rechnung.currency", width = 80)

        // Betreff (Text/Ware/Leistung)
        addReadColumn(agGrid, lc, EingangsrechnungDO::betreff, wrapText = true)

        // Konto (custom column)
        addDiffColumn(agGrid, lc, "read.konto.nummer", headerName = "fibu.konto", width = 100)

        // KOST1 (custom column) - only for position-based imports
        if (isPositionBasedImport) {
            addDiffColumn(agGrid, lc, "read.kost1.description", headerName = "fibu.kost1", width = 150)
        }

        // KOST2 (custom column) - only for position-based imports
        if (isPositionBasedImport) {
            addDiffColumn(agGrid, lc, "read.kost2.description", headerName = "fibu.kost2", width = 150)
        }

        // Fälligkeit
        addReadColumn(agGrid, lc, EingangsrechnungDO::faelligkeit)

        if (!isPositionBasedImport) {
            // Bezahlt am
            addReadColumn(agGrid, lc, EingangsrechnungDO::bezahlDatum)

            // Zahlbetrag
            addDiffColumn(
                agGrid, lc, "read.zahlBetrag",
                headerName = "fibu.rechnung.zahlBetrag",
                formatter = Formatter.CURRENCY_PLAIN
            )
        }

        // TAX rate
        addDiffColumn(
            agGrid, lc, "read.taxRate", headerName = "fibu.common.vat", width = 80,
            formatter = Formatter.PERCENTAGE_DECIMAL
        )

        // Customer
        addDiffColumn(agGrid, lc, "read.customernr", headerName = "fibu.kunde.nummer", width = 150)

        // Skonto %
        addReadColumn(
            agGrid, lc, EingangsrechnungDO::discountPercent,
            formatter = Formatter.PERCENTAGE
        )

        // Skontodatum
        addReadColumn(agGrid, lc, EingangsrechnungDO::discountMaturity)

        if (!isPositionBasedImport) {
            // Comment
            addDiffColumn(
                agGrid, lc, "read.bemerkung",
                headerName = "comment",
            )
            addDiffColumn(
                agGrid, lc, "read.iban",
                headerName = "fibu.rechnung.iban",
            )
            addDiffColumn(
                agGrid, lc, "read.bic",
                headerName = "fibu.rechnung.bic",
            )
        }
    }
}
