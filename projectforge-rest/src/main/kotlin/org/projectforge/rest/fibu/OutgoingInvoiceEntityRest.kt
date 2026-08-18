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

package org.projectforge.rest.fibu

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.AbstractRechnungDO
import org.projectforge.business.fibu.AuftragAndRechnungDaoHelper
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungInfo
import org.projectforge.business.fibu.RechnungsStatistik
import org.projectforge.business.fibu.SearchFilterWithPeriodOfPerformance
import org.projectforge.business.fibu.kost.KostZuweisungExport
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.api.SortPropertyComparator
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.dto.Rechnung
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.UISelectValue
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The list of outgoing invoices (Debitorenrechnungen), layout free.
 *
 * `open` for the same reason [OrderEntityRest] is: Wicket asks for this bean through `WicketSupport`,
 * which proxies it (`RechnungEditForm` reads the attachment settings and the access checker from here).
 *
 * The edit page is still Wicket's - this class answers the list, its statistics, its two exports and the
 * multi selection the mass update page runs on ([RechnungMultiSelectedPageRest]). It carries no
 * `createEditLayout` any more, and with it the legacy React page of this entity is gone: it only ever
 * showed a read only header plus the attachment list.
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/outgoingInvoice")
open class OutgoingInvoiceEntityRest : // open: proxied by Wicket's WicketSupport.
    AbstractDTOEntityRest<RechnungDO, Rechnung, RechnungDao>(RechnungDao::class.java, "fibu.rechnung.title") {

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var kostZuweisungExport: KostZuweisungExport

    @PostConstruct
    private fun postConstruct() {
        enableJcr()
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
     * Opts the invoice list into the lean row of [Rechnung.copyFrom4ListRow]: only the columns
     * `invoice.page.tsx` renders, instead of the positions and the dozen fields only the edit form reads.
     */
    override fun newDTO(): Rechnung {
        return Rechnung()
    }

    /**
     * Adds the statistics of the whole result set, the ones the Wicket list shows above its table
     * (`AbstractRechnungListForm.addStatistics`).
     *
     * Computed here rather than in the browser: none of the sums is a property of the [Rechnung] DTO, and
     * how a discount, a partial payment and a foreign currency enter them is [RechnungsStatistik]'s
     * business - a second implementation on the client would be a second answer.
     *
     * Sent as data, not as the markdown [ResultSet.resultInfo] carries for the legacy React app: the hand
     * built next page formats currency in the user's locale and takes its colours from css tokens (see
     * InvoiceStatisticsLine there).
     */
    override fun postProcessResultSet(
        resultSet: ResultSet<RechnungDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        val invoices = resultSet.resultSet
        return super.postProcessResultSet(resultSet, request, magicFilter).also {
            it.statistics = InvoiceStatistics(baseDao.buildStatistik(invoices))
        }
    }

    /**
     * The sums the list shows above its table, plus the two averages of the payment target.
     *
     * `bruttoWithDiscount` travels although the Wicket page shows it only where it differs from the gross
     * sum: whether a line is worth a row is the client's decision there as here, and the two are compared
     * as numbers either way.
     */
    class InvoiceStatistics(statistics: RechnungsStatistik) {
        val counter: Int = statistics.counter
        val counterPaid: Int = statistics.counterBezahlt
        val brutto: BigDecimal = statistics.brutto
        val bruttoWithDiscount: BigDecimal = statistics.bruttoMitSkonto
        val netto: BigDecimal = statistics.netto
        val paid: BigDecimal = statistics.gezahlt
        val open: BigDecimal = statistics.offen
        val overdue: BigDecimal = statistics.ueberfaellig
        val discount: BigDecimal = statistics.skonto

        /** Agreed payment target in days, averaged over the invoices - what was asked for. */
        val paymentTargetAverage: Int = statistics.zahlungszielAverage

        /** Actual payment target in days, weighted by the gross sum - what the customers did. */
        val actualPaymentTargetAverage: Int = statistics.tatsaechlichesZahlungzielAverage

        /**
         * Invoices whose amount could not be converted into the system currency, so their own amount
         * entered the sums above (see `AbstractRechnungsStatistik.convertToSystemCurrency`). Empty for
         * every installation with a single currency, and the reason the client shows a warning.
         */
        val currencyConversionWarnings: List<String> = statistics.currencyConversionWarningsList
    }

    /**
     * Adds the two filters the Wicket list has and no property of [RechnungDO] yields.
     *
     * The payment state ([LIST_TYPE_FILTER]) is Wicket's radio group over `RechnungFilter.listType`, and
     * the period of performance is its second time period panel: the first is a predicate over
     * [RechnungInfo], the second over two date columns at once, so neither is a search field.
     *
     * The status is opened by default, as in the order list: it is what an invoice list is narrowed by
     * first.
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        val statusFilter = elements.find { it is UIFilterElement && it.id == RechnungDO::status.name }
        (statusFilter as? UIFilterElement)?.defaultFilter = true
        elements.add(
            UIFilterListElement(
                LIST_TYPE_FILTER,
                // Its own label, not the status of RechnungDO: the two sit next to each other in the filter
                // bar, and this one asks whether the money arrived (unpaid, delinquent, paid), not which of
                // the seven states the invoice is in.
                label = translate("fibu.rechnung.filter.paymentStatus"),
                multi = false,
                defaultFilter = true,
            ).also { element ->
                // Hand built rather than from an enum: `listType` is a set of string constants of
                // `RechnungFilter`, and its fourth value (`all`) is "no filter", which is the pill's absence.
                element.values = listOf(
                    UISelectValue(LIST_TYPE_UNPAID, translate("fibu.rechnung.filter.unbezahlt")),
                    UISelectValue(LIST_TYPE_OVERDUE, translate("fibu.rechnung.filter.ueberfaellig")),
                    UISelectValue(LIST_TYPE_PAID, translate("fibu.rechnung.status.bezahlt")),
                )
            }
        )
        // The two ends of the period of performance are one question, as the Wicket panel asks it: one
        // label, two dates. As two independent range filters they are four dates for a single time window,
        // and each of them alone matches invoices whose *other* end lies outside it.
        elements.removeIf { it is UIFilterElement && it.id in PERIOD_OF_PERFORMANCE_FIELDS }
        elements.add(
            UIFilterElement(
                PERIOD_OF_PERFORMANCE_FILTER,
                UIFilterElement.FilterType.DATE,
                label = translate("fibu.periodOfPerformance"),
            )
        )
    }

    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter,
    ): List<CustomResultFilter<RechnungDO>>? {
        val filters = mutableListOf<CustomResultFilter<RechnungDO>>()
        val listTypeEntry = source.entries.find { it.field == LIST_TYPE_FILTER }
        listTypeEntry?.synthetic = true // No property of RechnungDO, so the database cannot answer it.
        listTypeEntry?.value?.values?.firstOrNull { it.isNotBlank() }?.let { listType ->
            filters.add(PaymentStateFilter(listType))
        }
        addPeriodOfPerformanceCriterion(target, source)
        return filters
    }

    /**
     * Turns the single [PERIOD_OF_PERFORMANCE_FILTER] entry into the overlap criterion Wicket's
     * "Leistungszeitraum" panel uses (see [AuftragAndRechnungDaoHelper]): an invoice matches if its own
     * period reaches into the window asked for. Filtering `periodOfPerformanceBegin` by the window
     * instead would hide a two-year invoice from a one-month window it runs right through.
     */
    private fun addPeriodOfPerformanceCriterion(target: QueryFilter, source: MagicFilter) {
        val entry = source.entries.find { it.field == PERIOD_OF_PERFORMANCE_FILTER } ?: return
        entry.synthetic = true
        val filter = object : SearchFilterWithPeriodOfPerformance {
            override val periodOfPerformanceStartDate = PFDayUtils.parseDate(entry.value.fromValue)
            override val periodOfPerformanceEndDate = PFDayUtils.parseDate(entry.value.toValue)
        }
        AuftragAndRechnungDaoHelper.createCriterionForPeriodOfPerformance(filter).ifPresent { target.add(it) }
    }

    /**
     * Takes the columns no `ORDER BY` can express out of the query - [filterList] sorts by those.
     *
     * Without it the whole `ORDER BY` is lost: `addOrder` swallows the exception an unknown property
     * causes, so the list comes back in whatever order the database produced and the sort the user asked
     * for silently does nothing.
     */
    override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
        target.sortProperties.removeIf { COMPUTED_SORT_PROPERTIES.containsKey(it.property) }
    }

    /**
     * Sorts the loaded invoices by the columns that are no database column, the way the Wicket list page
     * has always done it (`MyListPageSortableDataProvider`).
     *
     * The two sums and the translated status live in [RechnungInfo], which `RechnungCache` answered for
     * every row that was loaded anyway - a map lookup per comparison, not a query. The customer and the
     * project sort by the very string their cell shows, `displayName`, which `KostFormatter` composes of
     * number and name ("473 - Air Liquide"); the numbers are left padded, so ordering by the string is
     * ordering by the number. Sorting by `kunde.name` instead produces a list that looks unsorted to
     * whoever reads it, since the numbers lead every cell.
     *
     * The whole result set is loaded (there is no server side paging yet, see
     * `MIGRATION-list-paging.md`), so sorting it here is sorting all of it.
     */
    override fun filterList(resultSet: MutableList<RechnungDO>, filter: MagicFilter): List<RechnungDO> {
        val computed = filter.sortProperties.filter { COMPUTED_SORT_PROPERTIES.containsKey(it.property) }
        if (computed.isEmpty()) {
            return resultSet
        }
        // The invoice number as the last criterion, so equal values keep a deterministic order instead of
        // shifting between two requests over the same data.
        val sortProperties = computed + SortProperty.desc(RechnungDO::nummer.name)
        return SortPropertyComparator.sort(resultSet, sortProperties) { invoice, property ->
            COMPUTED_SORT_PROPERTIES[property]?.invoke(invoice)
        }
    }

    /**
     * The filtered list as the Excel file Wicket's "Excel export" produces (`RechnungListPage`).
     *
     * The rows come from [getResultList], i.e. through the same pipeline the list itself uses (the
     * synthetic payment state filter, then [filterList]) - `getObjectList` directly would export invoices
     * the list doesn't show.
     *
     * An empty result answers 404 rather than a file, as the order export does: Wicket reports it as a
     * form error, and a file saying "nothing to export" looks like a successful export in the download
     * folder.
     */
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting outgoing invoices as Excel file.")
        val invoices = getResultList(filter)
        if (invoices.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        ExcelUtils.prepareWorkbook().use { workbook ->
            val sheet = workbook.createOrGetSheet(translate("fibu.rechnungen"))
            val currencyStyle = workbook.createOrGetCellStyle("currency")
            currencyStyle.dataFormat = workbook.createDataFormat().getFormat(CURRENCY_FORMAT)
            ExcelUtils.registerColumn(sheet, RechnungDO::nummer, 10)
            sheet.registerColumn(translate("fibu.kunde"), COL_CUSTOMER).withSize(30)
            sheet.registerColumn(translate("fibu.projekt"), COL_PROJECT).withSize(30)
            ExcelUtils.registerColumn(sheet, RechnungDO::betreff, 40)
            ExcelUtils.registerColumn(sheet, RechnungDO::datum)
            ExcelUtils.registerColumn(sheet, RechnungDO::faelligkeit)
            ExcelUtils.registerColumn(sheet, RechnungDO::bezahlDatum)
            sheet.registerColumn(translate("fibu.rechnung.status"), COL_STATUS).withSize(16)
            sheet.registerColumn(translate("fibu.common.netto"), COL_NET_SUM).withSize(14)
            sheet.registerColumn(translate("fibu.common.brutto"), COL_GROSS_SUM).withSize(14)
            ExcelUtils.registerColumn(sheet, RechnungDO::zahlBetrag, 14)
            sheet.registerColumn(translate("fibu.konto.nummer"), COL_ACCOUNT).withSize(10)
            sheet.registerColumn(translate("fibu.konto.bezeichnung"), COL_ACCOUNT_TEXT).withSize(30)
            ExcelUtils.registerColumn(sheet, RechnungDO::bemerkung, 40)
            ExcelUtils.addHeadRow(sheet)
            invoices.forEach { invoice ->
                val row = sheet.createRow()
                row.autoFillFromObject(invoice)
                // The related customer or, for an invoice naming none, the free text - the same fallback
                // the list's cell and `KundeFormatter` make.
                row.getCell(COL_CUSTOMER)?.setCellValue(
                    PfCaches.instance.getKundeIfNotInitialized(invoice.kunde)?.displayName ?: invoice.kundeText
                )
                row.getCell(COL_PROJECT)
                    ?.setCellValue(PfCaches.instance.getProjektIfNotInitialized(invoice.projekt)?.displayName)
                row.getCell(COL_STATUS)?.setCellValue(invoice.status?.let { translate(it.i18nKey) })
                val info = invoice.ensuredInfo
                row.getCell(COL_NET_SUM)?.setCellValue(info.netSum)?.setCellStyle(currencyStyle)
                row.getCell(COL_GROSS_SUM)?.setCellValue(info.grossSum)?.setCellStyle(currencyStyle)
                ExcelUtils.getCell(row, RechnungDO::zahlBetrag)?.setCellStyle(currencyStyle)
                val konto = kontoCache.getKonto(invoice)
                konto?.nummer?.let { row.getCell(COL_ACCOUNT)?.setCellValue(it) }
                row.getCell(COL_ACCOUNT_TEXT)?.setCellValue(konto?.bezeichnung)
            }
            sheet.setAutoFilter()
            val filename = "ProjectForge-${translate("fibu.common.debitor")}" +
                    "_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx"
            return RestUtils.downloadFile(filename, workbook.asByteArrayOutputStream.toByteArray())
        }
    }

    /**
     * The same invoices with one row per cost assignment, i.e. `RechnungListPage.exportExcelWithCostAssignments`.
     *
     * Answers 404 where no cost ids are configured, as the Wicket menu entry is simply absent there:
     * without them every row of the sheet would be the invoice itself, which the export above already is.
     */
    @PostMapping(EXPORT_COST_ASSIGNMENTS_PATH)
    fun exportCostAssignmentsAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting cost assignments of outgoing invoices as Excel file.")
        if (!Configuration.instance.isCostConfigured) {
            return ResponseEntity.notFound().build<Any>()
        }
        val invoices: List<AbstractRechnungDO> = getResultList(filter)
        if (invoices.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        val debitor = translate("fibu.common.debitor")
        val xls = kostZuweisungExport.exportRechnungen(invoices, debitor)
        if (xls == null || xls.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        val filename = "ProjectForge-$debitor-${translate("menu.fibu.kost")}" +
                "_${DateHelper.getDateAsFilenameSuffix(Date())}.xls"
        return RestUtils.downloadFile(filename, xls)
    }

    /**
     * Wicket's radio group over `RechnungFilter.listType`, as a filter of the result list.
     *
     * A [CustomResultFilter] and not a query criterion: whether an invoice is paid or overdue follows from
     * [RechnungInfo] - the sum of its positions against what was paid, and the due date against today -
     * which is why `RechnungDao.select` filters it in memory as well.
     */
    private class PaymentStateFilter(private val listType: String) : CustomResultFilter<RechnungDO> {
        override fun match(list: MutableList<RechnungDO>, element: RechnungDO): Boolean {
            val info = element.ensuredInfo
            return when (listType) {
                LIST_TYPE_UNPAID -> !info.isBezahlt
                LIST_TYPE_PAID -> info.isBezahlt
                LIST_TYPE_OVERDUE -> info.isUeberfaellig
                else -> true
            }
        }
    }

    companion object {
        /**
         * Id of the payment state filter - a pseudo field standing for `RechnungFilter.listType`, whose
         * values are the three constants below (`RechnungFilter.FILTER_*`).
         */
        internal const val LIST_TYPE_FILTER = "listType"
        private const val LIST_TYPE_UNPAID = "unbezahlt"
        private const val LIST_TYPE_PAID = "bezahlt"
        private const val LIST_TYPE_OVERDUE = "ueberfaellig"

        /**
         * Id of the combined period-of-performance filter, standing for a criterion over
         * [PERIOD_OF_PERFORMANCE_FIELDS] - see [addPeriodOfPerformanceCriterion].
         */
        internal const val PERIOD_OF_PERFORMANCE_FILTER = "periodOfPerformance"

        /** The two date properties the combined filter replaces in the filter field list. */
        private val PERIOD_OF_PERFORMANCE_FIELDS = setOf(
            RechnungDO::periodOfPerformanceBegin.name,
            RechnungDO::periodOfPerformanceEnd.name,
        )

        /** Sub path of the cost assignment export, as `invoice-list-actions.tsx` calls it. */
        internal const val EXPORT_COST_ASSIGNMENTS_PATH = "exportCostAssignmentsAsExcel"

        private const val CURRENCY_FORMAT = "#,##0.00;[Red]-#,##0.00"

        /** Aliases of the Excel columns that are no property of [RechnungDO]. */
        private const val COL_CUSTOMER = "customer"
        private const val COL_PROJECT = "project"
        private const val COL_STATUS = "statusAsString"
        private const val COL_NET_SUM = "netSum"
        private const val COL_GROSS_SUM = "grossSum"
        private const val COL_ACCOUNT = "kontoNummer"
        private const val COL_ACCOUNT_TEXT = "kontoBezeichnung"

        /** Sort ids of the customer and the project column, as `invoice.page.tsx` declares them. */
        private const val CUSTOMER_SORT_PROPERTY = "kunde.displayName"
        private const val PROJECT_SORT_PROPERTY = "projekt.displayName"

        /**
         * The sort ids no database column can answer, and the value each one sorts by (see [filterList]).
         *
         * Keyed by what `invoice.page.tsx` declares its columns as, which for the three DTO fields is the
         * DTO's property name.
         */
        private val COMPUTED_SORT_PROPERTIES = mapOf<String, (RechnungDO) -> Comparable<*>?>(
            Rechnung::netSum.name to { it.ensuredInfo.netSum },
            Rechnung::grossSumWithDiscount.name to { it.ensuredInfo.grossSumWithDiscount },
            Rechnung::statusAsString.name to { invoice -> invoice.status?.let { translate(it.i18nKey) } },
            CUSTOMER_SORT_PROPERTY to { invoice ->
                PfCaches.instance.getKundeIfNotInitialized(invoice.kunde)?.displayName ?: invoice.kundeText
            },
            PROJECT_SORT_PROPERTY to { invoice ->
                PfCaches.instance.getProjektIfNotInitialized(invoice.projekt)?.displayName
            },
        )
    }
}
