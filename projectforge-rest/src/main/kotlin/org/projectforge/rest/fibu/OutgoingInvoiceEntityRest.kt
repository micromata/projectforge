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
import org.projectforge.business.fibu.RechnungCalculator
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungInfo
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
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
import org.projectforge.rest.dto.PostData
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
import java.time.LocalDate
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The list of outgoing invoices (Debitorenrechnungen), layout free.
 *
 * `open` for the same reason [OrderEntityRest] is: Wicket asks for this bean through `WicketSupport`,
 * which proxies it (`RechnungEditForm` reads the attachment settings and the access checker from here).
 *
 * Answers the list, its statistics, its two exports, the multi selection the mass update page runs on
 * ([RechnungMultiSelectedPageRest]) and the read/write path of the hand built edit page of
 * `/next/invoice/[id]` — including its [recalculate]. It carries no `createEditLayout` any more, and with it
 * the legacy React page of this entity is gone: it only ever showed a read only header plus the attachment
 * list. Wicket's edit page is still reachable and writes through the same [RechnungDao].
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

    /**
     * Builds a fresh [RechnungDO] instead of mutating the persisted one, for the reason
     * [OrderEntityRest.transformForDB] spells out: the persistence layer merges the posted object over the
     * database row, and `RechnungRight.hasAccess(obj, oldObj)` compares the two.
     *
     * Because of that merge, every field the DTO doesn't carry ends up as null in the database — the five
     * attachment columns are such fields (written by the attachment endpoints, not by this form), so they
     * are copied back from the database row, as `RechnungEditPage.update` does.
     *
     * [RechnungDO.nummer] is deliberately *not* assigned here, unlike the order's: `RechnungDao` assigns it
     * itself on the transition from [org.projectforge.business.fibu.RechnungStatus.GEPLANT] to any other
     * status, and a `GUTSCHRIFTSANZEIGE_DURCH_KUNDEN` must have no number at all.
     */
    override fun transformForDB(dto: Rechnung): RechnungDO {
        val rechnungDO = RechnungDO()
        dto.copyTo(rechnungDO)
        if (rechnungDO.kunde != null) {
            // A customer chosen from the list wins over the free text one, see RechnungEditPage.onSaveOrUpdate.
            rechnungDO.kundeText = null
        }
        assignNumbersAndIndicesToNewRows(rechnungDO)
        dto.id?.let { id ->
            baseDao.find(id, checkAccess = false)?.let { dbObj ->
                rechnungDO.attachmentsCounter = dbObj.attachmentsCounter
                rechnungDO.attachmentsNames = dbObj.attachmentsNames
                rechnungDO.attachmentsIds = dbObj.attachmentsIds
                rechnungDO.attachmentsSize = dbObj.attachmentsSize
                rechnungDO.attachmentsLastUserAction = dbObj.attachmentsLastUserAction
            }
        }
        return rechnungDO
    }

    override fun transformFromDB(obj: RechnungDO, editMode: Boolean): Rechnung {
        val rechnung = Rechnung()
        // Only the edit page needs the positions with their cost assignments, and only it can afford them:
        // both collections are lazy, so mapping them is a query per invoice.
        if (editMode) {
            rechnung.copyFromWithCollections(obj)
        } else {
            rechnung.copyFrom(obj)
            rechnung.project?.displayName = obj.projekt?.name
        }
        rechnung.deleteAccess = baseDao.hasLoggedInUserDeleteAccess(obj, obj, false)
        rechnung.writeAccess = if (obj.id == null) {
            baseDao.hasLoggedInUserInsertAccess(obj, false)
        } else {
            baseDao.hasLoggedInUserUpdateAccess(obj, obj, false)
        }
        // What hides the cost assignments of a position, as `AbstractRechnungEditForm` hides its whole
        // cost table where no cost ids are configured.
        rechnung.costConfigured = Configuration.instance.isCostConfigured
        // ensuredInfo, not info: for a new invoice (see [newBaseDTO]) that lateinit would throw.
        val info = obj.ensuredInfo
        val kost1Sorted = info.sortedKost1
        rechnung.kost1List = RechnungInfo.numbersAsString(kost1Sorted)
        rechnung.kost1Info = RechnungInfo.detailsAsString(kost1Sorted)
        val kost2Sorted = info.sortedKost2
        rechnung.kost2List = RechnungInfo.numbersAsString(kost2Sorted)
        rechnung.kost2Info = RechnungInfo.detailsAsString(kost2Sorted)
        return rechnung
    }

    /**
     * Presets the date, the status and the type of a new invoice, as `RechnungEditPage.onPreEdit` does.
     *
     * The date is required: `RechnungDao.onInsertOrModify` refuses a null one, and every other date of the
     * form (due date, discount maturity) is derived from it. The status and the type have no default in
     * [RechnungDO] either, and Wicket gets away without a preset only because its two drop downs cannot be
     * empty (`setNullValid(false)`) — which silently makes the first constant the answer. The first
     * constant of the status is `GEPLANT` though, whose invoice gets no number, so `GESTELLT` is named here.
     */
    override fun newBaseDTO(request: HttpServletRequest?): Rechnung {
        val rechnung = super.newBaseDTO(request)
        rechnung.datum = LocalDate.now()
        rechnung.status = RechnungStatus.GESTELLT
        rechnung.typ = RechnungTyp.RECHNUNG
        return rechnung
    }

    /**
     * The sums of an invoice as they are right now in the form, i.e. computed on the posted state, not on
     * the stored one.
     *
     * Needed because a hand built form has to show the same sums the list and the Wicket page show, and
     * those are calculated server side by [RechnungCalculator] from the whole invoice — how a position is
     * rounded before it enters a sum (`roundPositionsBeforeSum`, German law) is its rule, and a client
     * re-implementing that would drift. The cache is no help either: it answers the stored positions of an
     * invoice whose form the user has changed, and nothing at all for one without an id.
     *
     * Deleted positions may be posted untouched — [RechnungCalculator] skips them itself.
     *
     * Not a `saveOrUpdate` in disguise: nothing is written, so the read access has to be checked here.
     */
    @PostMapping("recalculate")
    fun recalculate(@RequestBody postData: PostData<Rechnung>): InvoiceSums {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        val invoice = RechnungDO()
        postData.data.copyTo(invoice)
        val info = Rechnung.calculateInvoiceInfo(invoice)
        return InvoiceSums(
            netSum = info.netSum,
            vatAmount = info.vatAmount,
            grossSum = info.grossSum,
            grossSumWithDiscount = info.grossSumWithDiscount,
            kostZuweisungenNetSum = info.kostZuweisungenNetSum,
            kostZuweisungenFehlbetrag = info.kostZuweisungenFehlbetrag,
            bezahlt = info.isBezahlt,
            ueberfaellig = info.isUeberfaellig,
            positions = info.positions?.map { position ->
                PositionSums(
                    number = position.number,
                    netSum = position.netSum,
                    vatAmount = position.vatAmount,
                    grossSum = position.grossSum,
                    kostZuweisungNetSum = position.kostZuweisungNetSum,
                    kostZuweisungNetFehlbetrag = position.kostZuweisungNetFehlbetrag,
                )
            },
        )
    }

    /**
     * The sums [recalculate] answers, one flat object — the invoice's own plus one entry per position.
     */
    class InvoiceSums(
        val netSum: BigDecimal,
        val vatAmount: BigDecimal,
        val grossSum: BigDecimal,
        val grossSumWithDiscount: BigDecimal,
        val kostZuweisungenNetSum: BigDecimal,
        /**
         * How much of [netSum] is not assigned to a cost unit yet. A hint for the user only: `RechnungDao`
         * performs no validation of the cost assignment sums, so an invoice with a difference saves fine.
         */
        val kostZuweisungenFehlbetrag: BigDecimal,
        val bezahlt: Boolean,
        val ueberfaellig: Boolean,
        val positions: List<PositionSums>?,
    )

    class PositionSums(
        val number: Short,
        val netSum: BigDecimal,
        val vatAmount: BigDecimal,
        val grossSum: BigDecimal,
        val kostZuweisungNetSum: BigDecimal,
        /** The per position counterpart of [InvoiceSums.kostZuweisungenFehlbetrag], which Wicket paints red. */
        val kostZuweisungNetFehlbetrag: BigDecimal,
    )

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
                label = translate("fibu.rechnung.status"),
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
         * Gives every posted row that has no id yet its number: the positions of the invoice, and the cost
         * assignments of each position.
         *
         * Both are `@OrderColumn`s the client cannot assign, and both identify a row inside its collection:
         * `RechnungsPositionDO` has `UNIQUE(rechnung_fk, number)` with `@ListIndexBase(1)`, and
         * `KostZuweisungDO.index` is the order column of a position's assignments (0-based, as
         * `AbstractRechnungsPositionDO.addKostZuweisung` assigns it).
         *
         * The next free number is taken from the **stored** rows only: whatever number the client gave a new
         * row is its guess and is about to be replaced, so counting those in would leave a gap for every new
         * row. A stored row the client marked deleted still counts — it stays in the database, so its number
         * is never reused, and a gap is the record of what was deleted.
         *
         * No renumbering map is needed, unlike the order's: nothing refers to an invoice position by number
         * the way a payment schedule refers to an order position.
         *
         * `internal` and in the companion object rather than a private method: it needs nothing of the
         * instance, and this is what the numbering of a whole posted invoice can be tested through without a
         * Spring context (`RechnungDtoTest`).
         */
        internal fun assignNumbersAndIndicesToNewRows(invoice: RechnungDO) {
            val positions = invoice.positionen ?: return
            var nextNumber = (positions.filter { it.id != null }.maxOfOrNull { it.number } ?: 0).toInt()
            positions.filter { it.id == null }.forEach { position ->
                position.number = (++nextNumber).toShort()
            }
            positions.forEach { position ->
                val assignments = position.kostZuweisungen ?: return@forEach
                // -1, not 0: the index is 0-based, so the first assignment of a position has to become 0.
                var nextIndex = (assignments.filter { it.id != null }.maxOfOrNull { it.index } ?: -1).toInt()
                assignments.filter { it.id == null }.forEach { assignment ->
                    assignment.index = (++nextIndex).toShort()
                }
            }
        }

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
