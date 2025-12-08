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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.InvoiceExcelExporter
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungFilter
import org.projectforge.business.fibu.RechnungInfo
import org.projectforge.business.fibu.RechnungsStatistik
import mu.KotlinLogging
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.time.DateHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.RestUtils
import org.projectforge.ui.filter.UIFilterBooleanElement
import org.projectforge.ui.filter.UIFilterListElement
import java.util.Date
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.getObjectList
import org.projectforge.rest.dto.Rechnung
import org.projectforge.ui.*
import org.projectforge.ui.UISelectValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.ResponseEntity

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/outgoingInvoice")
class RechnungPagesRest :
    AbstractDTOPagesRest<RechnungDO, Rechnung, RechnungDao>(RechnungDao::class.java, "fibu.rechnung.title") {

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var invoiceExcelExporter: InvoiceExcelExporter

    override val addNewEntryUrl = "wa/outgoingInvoiceEdit"

    override fun getStandardEditPage(): String {
        return "wa/outgoingInvoiceEdit?id=:id"
    }

    override val classicsLinkListUrl: String?
        get() = "wa/outgoingInvoiceList"

    /**
     * LAYOUT List page
     */
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        // Add statistics display above the grid
        val list = getObjectList(this, baseDao, magicFilter)
        val stats = rechnungDao.buildStatistik(list)
        layout.add(UIAlert("'${stats.asMarkdown}", color = UIColor.LIGHT, markdown = true))

        val infoLC = LayoutContext(RechnungInfo::class.java)
        val grid = agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            RechnungMultiSelectedPageRest::class.java,
            userAccess = userAccess,
            rowClickUrl = "/wa/outgoingInvoiceEdit?id={id}",
        )
            .add(lc, "nummer", width = 120, pinnedAndLocked = UIAgGridColumnDef.Orientation.LEFT)
            .add(lc, "customer", lcField = "kunde", pinnedAndLocked = UIAgGridColumnDef.Orientation.LEFT)
        if (Configuration.instance.isCostConfigured) {
            grid.add(lc, "project", lcField = "projekt", pinnedAndLocked = UIAgGridColumnDef.Orientation.LEFT)
        }
        grid.add(lc, "betreff", "datum", "faelligkeit", "bezahlDatum")
            .add(lc, "statusAsString", headerName = "fibu.rechnung.status", width = 100)
            .add(infoLC, "netSum")
            .add(infoLC, "grossSumWithDiscount")
            .add(lc, "konto", "periodOfPerformanceBegin", "periodOfPerformanceEnd", "bemerkung")
            .add(field = "kost1List", headerName = translate("fibu.kost1"), tooltipField = "kost1Info")
            .add(field = "kost2List", headerName = translate("fibu.kost2"), tooltipField = "kost2Info")
            .withMultiRowSelection(request, magicFilter)
            .withGetRowClass(
                """if (params.node.data.ueberfaellig) {
            return 'ag-row-red';
        } else if (params.node.data.status !== 'BEZAHLT') {
            return 'ag-row-blue';
        }"""
            )
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        // Add filter for payment status (ALL, BEZAHLT, UNBEZAHLT, UEBERFÄLLIG)
        val listTypeFilter = UIFilterListElement(
            "listType",
            label = translate("fibu.rechnung.filter"),
            defaultFilter = true
        )
        listTypeFilter.values = listOf(
            UISelectValue(RechnungFilter.FILTER_ALL, translate("fibu.rechnung.filter.all")),
            UISelectValue(RechnungFilter.FILTER_BEZAHLT, translate("fibu.rechnung.filter.bezahlt")),
            UISelectValue(RechnungFilter.FILTER_UNBEZAHLT, translate("fibu.rechnung.filter.unbezahlt")),
            UISelectValue(RechnungFilter.FILTER_UEBERFAELLIG, translate("fibu.rechnung.filter.ueberfaellig"))
        )
        listTypeFilter.multi = false  // Only one filter can be selected at a time
        elements.add(listTypeFilter)

        // Add cost assignment status filter if cost is configured
        if (Configuration.instance.isCostConfigured) {
            elements.add(
                UIFilterBooleanElement(
                    "showKostZuweisungStatus",
                    label = translate("fibu.rechnung.showKostZuweisungStatus"),
                    defaultFilter = false
                )
            )
        }
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Rechnung, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
            .add(lc, "betreff")
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(lc, "nummer", "typ")
                    )
                    .add(
                        UICol()
                            .add(lc, "status", "konto")
                    )
                    .add(
                        UICol()
                            .add(lc, "datum", "vatAmountSum", "bezahlDatum", "faelligkeit")
                    )
                    .add(
                        UICol()
                            .add(lc, "netSum", "grossSum", "zahlBetrag", "discountMaturity", "discountPercent")
                    )
            )
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(UISelect.createProjectSelect(lc, "projekt", false))
                            .add(UISelect.createCustomerSelect(lc, "kunde", false))
                            .add(
                                lc,
                                "kundeText",
                                "customerAddress",
                                "customerref1",
                                "attachment",
                                "periodOfPerformanceBegin",
                                "periodOfPerformanceEnd"
                            )
                    )
            )
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(lc, "bemerkung")
                    )
                    .add(
                        UICol()
                            .add(lc, "besonderheiten")
                    )
            )
            // Positionen
            .add(UICustomized("invoice.outgoingPosition"))
        return LayoutUtils.processEditPage(layout, dto, this)
    }


    override fun transformForDB(dto: Rechnung): RechnungDO {
        val rechnungDO = RechnungDO()
        dto.copyTo(rechnungDO)
        return rechnungDO
    }

    override fun transformFromDB(obj: RechnungDO, editMode: Boolean): Rechnung {
        val rechnung = Rechnung()
        rechnung.copyFrom(obj)
        if (editMode) {
            rechnung.copyPositionenFrom(obj)
        } else {
            rechnung.project?.displayName = obj.projekt?.name
        }
        val kost1Sorted = obj.info.sortedKost1
        rechnung.kost1List = RechnungInfo.numbersAsString(kost1Sorted)
        rechnung.kost1Info = RechnungInfo.detailsAsString(kost1Sorted)
        val kost2Sorted = obj.info.sortedKost2
        rechnung.kost2List = RechnungInfo.numbersAsString(kost2Sorted)
        rechnung.kost2Info = RechnungInfo.detailsAsString(kost2Sorted)
        return rechnung
    }

    /**
     * Standard Excel export for outgoing invoices
     */
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting outgoing invoices as Excel file.")
        val list = getObjectList(this, baseDao, filter)

        if (list.isEmpty()) {
            return RestUtils.downloadFile("empty.txt", "nothing to export.")
        }

        val xls = invoiceExcelExporter.exportInvoices(list)
        if (xls == null || xls.isEmpty()) {
            log.error("Excel export returned empty result")
            return RestUtils.downloadFile("empty.txt", "export not yet implemented.")
        }

        val filename = "ProjectForge-${translate("fibu.common.debitor")}_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx"
        return RestUtils.downloadFile(filename, xls)
    }

    /**
     * Excel export with cost assignments for outgoing invoices
     */
    @PostMapping("exportExcelWithCostAssignments")
    fun exportExcelWithCostAssignments(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting outgoing invoices with cost assignments as Excel file.")

        val list = getObjectList(this, baseDao, filter)
        if (list.isEmpty()) {
            return RestUtils.downloadFile("empty.txt", "nothing to export.")
        }

        val xls = invoiceExcelExporter.exportInvoicesWithCostAssignments(list, translate("fibu.common.debitor"))
        if (xls == null || xls.isEmpty()) {
            log.error("Excel export with cost assignments returned empty result")
            return RestUtils.downloadFile("empty.txt", "export not yet implemented.")
        }

        val filename = "ProjectForge-${translate("fibu.common.debitor")}-${translate("menu.fibu.kost")}_${DateHelper.getDateAsFilenameSuffix(Date())}.xls"
        return RestUtils.downloadFile(filename, xls)
    }
}
