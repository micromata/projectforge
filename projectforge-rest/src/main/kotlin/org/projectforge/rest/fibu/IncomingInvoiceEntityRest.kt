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

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.fibu.AbstractRechnungDO
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.EingangsrechnungsStatistik
import org.projectforge.business.fibu.RechnungCache
import org.projectforge.business.fibu.InvoiceConfiguration
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.RechnungInfo
import org.projectforge.business.fibu.kost.KostZuweisungExport
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.time.DateHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.dto.Eingangsrechnung
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.UISelectValue
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The list of incoming invoices (Kreditorenrechnungen), layout free — the incoming sibling of
 * [OutgoingInvoiceEntityRest] and modelled on it.
 *
 * `open` for the same reason the outgoing one is: it is autowired by [EingangsrechnungMultiSelectedPageRest]
 * and named by Wicket's `EingangsrechnungListPage.addNewMassSelect`.
 *
 * Answers the list, its statistics, its two exports, and the read/write path of the hand built edit page of
 * `/next/creditor-invoice/[id]` — including its [recalculate]. It carries no `createEditLayout`/
 * `createListLayout` any more; the list columns come from `creditor-invoice.page.tsx`. Wicket's edit page is
 * still reachable as the escape hatch and writes through the same [EingangsrechnungDao].
 *
 * The incoming invoice is the simpler sibling: no e-invoice, invoice PDF, Word export, order positions,
 * customer/project fields, seller bank accounts, attachments or period of performance.
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/incomingInvoice")
open class IncomingInvoiceEntityRest : // open: autowired by the mass-select page and named by Wicket.
    AbstractDTOEntityRest<EingangsrechnungDO, Eingangsrechnung, EingangsrechnungDao>(
        EingangsrechnungDao::class.java,
        "fibu.eingangsrechnung.title",
    ) {

    @Autowired
    private lateinit var kontoCache: KontoCache

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var kostZuweisungExport: KostZuweisungExport

    @Autowired
    private lateinit var invoiceConfig: InvoiceConfiguration

    /**
     * Builds a fresh [EingangsrechnungDO] instead of mutating the persisted one, for the reason
     * [OutgoingInvoiceEntityRest.transformForDB] spells out: the persistence layer merges the posted object
     * over the database row, and every field the DTO doesn't carry ends up as null.
     *
     * [org.projectforge.business.fibu.AbstractRechnungDO.uiStatusAsXml] is such a field — it belongs to the
     * Wicket form alone (`RechnungDao.writeUiStatusToXml`) — so it is copied back from the database row.
     * Unlike the outgoing invoice there are no attachment columns to preserve: the incoming invoice has no
     * attachments.
     */
    override fun transformForDB(dto: Eingangsrechnung): EingangsrechnungDO {
        val eingangsrechnungDO = EingangsrechnungDO()
        dto.copyTo(eingangsrechnungDO)
        assignNumbersAndIndicesToNewRows(eingangsrechnungDO)
        dto.id?.let { id ->
            baseDao.find(id, checkAccess = false)?.let { dbObj ->
                eingangsrechnungDO.uiStatusAsXml = dbObj.uiStatusAsXml
            }
        }
        return eingangsrechnungDO
    }

    override fun transformFromDB(obj: EingangsrechnungDO, editMode: Boolean): Eingangsrechnung {
        val eingangsrechnung = Eingangsrechnung()
        // Only the edit page needs the positions with their cost assignments, and only it can afford them:
        // both collections are lazy, so mapping them is a query per invoice.
        if (editMode) {
            eingangsrechnung.copyFromWithCollections(obj)
        } else {
            eingangsrechnung.copyFrom(obj)
        }
        eingangsrechnung.deleteAccess = baseDao.hasLoggedInUserDeleteAccess(obj, obj, false)
        eingangsrechnung.writeAccess = if (obj.id == null) {
            baseDao.hasLoggedInUserInsertAccess(obj, false)
        } else {
            baseDao.hasLoggedInUserUpdateAccess(obj, obj, false)
        }
        eingangsrechnung.costConfigured = Configuration.instance.isCostConfigured
        val info = obj.ensuredInfo
        val kost1Sorted = info.sortedKost1
        eingangsrechnung.kost1List = RechnungInfo.numbersAsString(kost1Sorted)
        eingangsrechnung.kost1Info = RechnungInfo.detailsAsString(kost1Sorted)
        val kost2Sorted = info.sortedKost2
        eingangsrechnung.kost2List = RechnungInfo.numbersAsString(kost2Sorted)
        eingangsrechnung.kost2Info = RechnungInfo.detailsAsString(kost2Sorted)
        return eingangsrechnung
    }

    /**
     * Presets the date of a new invoice, as the Wicket edit page does: [EingangsrechnungDao] refuses a null
     * one, and the due date and the discount maturity are derived from it.
     */
    override fun newBaseDTO(request: HttpServletRequest?): Eingangsrechnung {
        val eingangsrechnung = super.newBaseDTO(request)
        eingangsrechnung.datum = LocalDate.now()
        return eingangsrechnung
    }

    /**
     * Everything the hand built form has to know before the user touches it. The incoming invoice needs
     * only the default VAT rate — it has no seller bank accounts, template variants or e-invoice flags.
     *
     * Read only, so the select access of the category is what has to be checked here.
     */
    @GetMapping("formDefaults")
    fun getFormDefaults(): FormDefaults {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        return FormDefaults(
            defaultVat = Configuration.instance.getPercentValue(ConfigurationParam.FIBU_DEFAULT_VAT),
        )
    }

    /**
     * The values [getFormDefaults] answers.
     */
    class FormDefaults(
        /**
         * The VAT rate a new position starts with, `fibu.defaultVAT` (as a fraction, i.e. 0.19 for 19 %).
         * Null where the installation configured none, in which case the field simply starts empty.
         */
        val defaultVat: BigDecimal?,
    )

    /**
     * The sums of an invoice as they are right now in the form, computed on the posted state, not on the
     * stored one — see [OutgoingInvoiceEntityRest.recalculate] for why the client cannot do this itself.
     */
    @PostMapping("recalculate")
    fun recalculate(@RequestBody postData: PostData<Eingangsrechnung>): InvoiceSums {
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        val invoice = EingangsrechnungDO()
        postData.data.copyTo(invoice)
        val info = Eingangsrechnung.calculateInvoiceInfo(invoice)
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
     * Mirrors [OutgoingInvoiceEntityRest.InvoiceSums].
     */
    class InvoiceSums(
        val netSum: BigDecimal,
        val vatAmount: BigDecimal,
        val grossSum: BigDecimal,
        val grossSumWithDiscount: BigDecimal,
        val kostZuweisungenNetSum: BigDecimal,
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
        val kostZuweisungNetFehlbetrag: BigDecimal,
    )

    /**
     * Opts the invoice list into the lean row of [Eingangsrechnung.copyFrom4ListRow]: only the columns
     * `creditor-invoice.page.tsx` renders.
     */
    override fun newDTO(): Eingangsrechnung {
        return Eingangsrechnung()
    }

    /**
     * Adds the statistics of the whole result set, the ones the Wicket list shows above its table
     * (`AbstractRechnungListForm.addStatistics`). See [OutgoingInvoiceEntityRest.postProcessResultSet].
     */
    override fun postProcessResultSet(
        resultSet: ResultSet<EingangsrechnungDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        val result = super.postProcessResultSet(resultSet, request, magicFilter)
        if (resultSet.offset == null) {
            // Non-paged POST list: the result set is the whole result, so its statistics are the whole result's.
            result.statistics = InvoiceStatistics(baseDao.buildStatistik(resultSet.resultSet))
        }
        // Server-side paged: the whole-result statistics were computed over the full id list in aggregate().
        return result
    }

    /**
     * The sums the list shows above its table, plus the two averages of the payment target. Mirrors
     * [OutgoingInvoiceEntityRest.InvoiceStatistics] — both read the same [EingangsrechnungsStatistik]
     * fields.
     */
    class InvoiceStatistics(statistics: EingangsrechnungsStatistik) {
        val counter: Int = statistics.counter
        val counterPaid: Int = statistics.counterBezahlt
        val brutto: BigDecimal = statistics.brutto
        val bruttoWithDiscount: BigDecimal = statistics.bruttoMitSkonto
        val netto: BigDecimal = statistics.netto
        val paid: BigDecimal = statistics.gezahlt
        val open: BigDecimal = statistics.offen
        val overdue: BigDecimal = statistics.ueberfaellig
        val discount: BigDecimal = statistics.skonto
        val paymentTargetAverage: Int = statistics.zahlungszielAverage
        val actualPaymentTargetAverage: Int = statistics.tatsaechlichesZahlungzielAverage
        val currencyConversionWarnings: List<String> = statistics.currencyConversionWarningsList
    }

    /**
     * Adds the two filters the Wicket list has and no property of [EingangsrechnungDO] yields: the payment
     * state ([LIST_TYPE_FILTER], `EingangsrechnungListFilter.listType`) and the completeness filter
     * ([INCOMPLETE_FILTER]). The incoming invoice has no period of performance, so that filter is absent.
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        // The invoice date opens by default: an incoming invoice list is read by period as much as by
        // its payment state. (There is no status property here, unlike the outgoing invoice.)
        val datumFilter = elements.find { it is UIFilterElement && it.id == EingangsrechnungDO::datum.name }
        (datumFilter as? UIFilterElement)?.defaultFilter = true
        elements.add(
            UIFilterListElement(
                LIST_TYPE_FILTER,
                label = translate("fibu.rechnung.filter.paymentStatus"),
                multi = false,
                defaultFilter = true,
            ).also { element ->
                element.values = listOf(
                    UISelectValue(LIST_TYPE_UNPAID, translate("fibu.rechnung.filter.unbezahlt")),
                    UISelectValue(LIST_TYPE_OVERDUE, translate("fibu.rechnung.filter.ueberfaellig")),
                    UISelectValue(LIST_TYPE_PAID, translate("fibu.rechnung.status.bezahlt")),
                )
            }
        )
        if (IncompleteInvoiceFilter.isOffered(Configuration.instance.isCostConfigured, invoiceConfig.accountRequired)) {
            elements.add(
                UIFilterElement(
                    INCOMPLETE_FILTER,
                    UIFilterElement.FilterType.BOOLEAN,
                    label = translate("fibu.rechnung.filter.incomplete"),
                    defaultFilter = true,
                )
            )
        }
    }

    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter,
    ): List<CustomResultFilter<EingangsrechnungDO>>? {
        val filters = mutableListOf<CustomResultFilter<EingangsrechnungDO>>()
        val listTypeEntry = source.entries.find { it.field == LIST_TYPE_FILTER }
        listTypeEntry?.synthetic = true // No property of EingangsrechnungDO, so the database cannot answer it.
        listTypeEntry?.value?.values?.firstOrNull { it.isNotBlank() }?.let { listType ->
            filters.add(PaymentStateFilter(listType, rechnungCache))
        }
        val incompleteEntry = source.entries.find { it.field == INCOMPLETE_FILTER }
        incompleteEntry?.synthetic = true
        if (incompleteEntry?.isTrueValue == true) {
            filters.add(
                IncompleteInvoiceFilter(
                    costConfigured = Configuration.instance.isCostConfigured,
                    accountRequired = invoiceConfig.accountRequired,
                    // The incoming invoice's own account, there being no project/customer to inherit one from.
                    accountOf = { kontoCache.getKontoIfNotInitialized(it.konto) },
                    // From the cache, not ensuredInfo: this filter runs before afterLoad puts the cached info
                    // on the row (see IncompleteInvoiceFilter and PaymentStateFilter).
                    infoOf = { rechnungCache.getEingangsrechnungInfo(it.id) },
                )
            )
        }
        return filters
    }

    /**
     * The columns no `ORDER BY` can express — the two sums, which live in [RechnungInfo]. See
     * [OutgoingInvoiceEntityRest.computedSortProperties]. `sortIds` keeps the base's load path.
     */
    override val computedSortProperties get() = COMPUTED_SORT_PROPERTIES

    override val computedSortTieBreak get() = SortProperty.desc(EingangsrechnungDO::datum.name)

    /**
     * The whole-result statistics of a server-side paged invoice list: computed over the full id list, not
     * over the single page [postProcessResultSet] sees (see `MIGRATION-list-paging.md`).
     * [EingangsrechnungsStatistik] converts foreign currencies from the loaded [EingangsrechnungDO], which no
     * cache holds, so the matching invoices are loaded here. See [OutgoingInvoiceEntityRest.aggregate].
     */
    override fun aggregate(ids: LongArray, filter: MagicFilter): Any? {
        return InvoiceStatistics(baseDao.buildStatistik(getListByIds(ids.toList())))
    }

    /**
     * The filtered list as the Excel file Wicket's "Excel export" produces. The rows come from
     * [getResultList], i.e. through the same pipeline the list itself uses. An empty result answers 404
     * rather than a file. See [OutgoingInvoiceEntityRest.exportAsExcel].
     */
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting incoming invoices as Excel file.")
        val invoices = getResultList(filter)
        if (invoices.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        ExcelUtils.prepareWorkbook().use { workbook ->
            val sheet = workbook.createOrGetSheet(translate("fibu.eingangsrechnungen"))
            val currencyStyle = workbook.createOrGetCellStyle("currency")
            currencyStyle.dataFormat = workbook.createDataFormat().getFormat(CURRENCY_FORMAT)
            ExcelUtils.registerColumn(sheet, EingangsrechnungDO::kreditor, 30)
            ExcelUtils.registerColumn(sheet, EingangsrechnungDO::referenz, 20)
            ExcelUtils.registerColumn(sheet, EingangsrechnungDO::betreff, 40)
            ExcelUtils.registerColumn(sheet, EingangsrechnungDO::datum)
            ExcelUtils.registerColumn(sheet, EingangsrechnungDO::faelligkeit)
            ExcelUtils.registerColumn(sheet, EingangsrechnungDO::bezahlDatum)
            sheet.registerColumn(translate("fibu.common.netto"), COL_NET_SUM).withSize(14)
            sheet.registerColumn(translate("fibu.common.brutto"), COL_GROSS_SUM).withSize(14)
            ExcelUtils.registerColumn(sheet, EingangsrechnungDO::zahlBetrag, 14)
            sheet.registerColumn(translate("fibu.konto.nummer"), COL_ACCOUNT).withSize(10)
            sheet.registerColumn(translate("fibu.konto.bezeichnung"), COL_ACCOUNT_TEXT).withSize(30)
            ExcelUtils.registerColumn(sheet, EingangsrechnungDO::bemerkung, 40)
            ExcelUtils.addHeadRow(sheet)
            invoices.forEach { invoice ->
                val row = sheet.createRow()
                row.autoFillFromObject(invoice)
                val info = invoice.ensuredInfo
                row.getCell(COL_NET_SUM)?.setCellValue(info.netSum)?.setCellStyle(currencyStyle)
                row.getCell(COL_GROSS_SUM)?.setCellValue(info.grossSum)?.setCellStyle(currencyStyle)
                ExcelUtils.getCell(row, EingangsrechnungDO::zahlBetrag)?.setCellStyle(currencyStyle)
                val konto = kontoCache.getKontoIfNotInitialized(invoice.konto)
                konto?.nummer?.let { row.getCell(COL_ACCOUNT)?.setCellValue(it) }
                row.getCell(COL_ACCOUNT_TEXT)?.setCellValue(konto?.bezeichnung)
            }
            sheet.setAutoFilter()
            val filename = "ProjectForge-${translate("fibu.common.creditor")}" +
                    "_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx"
            return RestUtils.downloadFile(filename, workbook.asByteArrayOutputStream.toByteArray())
        }
    }

    /**
     * The same invoices with one row per cost assignment. Answers 404 where no cost ids are configured. See
     * [OutgoingInvoiceEntityRest.exportCostAssignmentsAsExcel].
     */
    @PostMapping(EXPORT_COST_ASSIGNMENTS_PATH)
    fun exportCostAssignmentsAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting cost assignments of incoming invoices as Excel file.")
        if (!Configuration.instance.isCostConfigured) {
            return ResponseEntity.notFound().build<Any>()
        }
        val invoices: List<AbstractRechnungDO> = getResultList(filter)
        if (invoices.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        val creditor = translate("fibu.common.creditor")
        val xls = kostZuweisungExport.exportRechnungen(invoices, creditor)
        if (xls == null || xls.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        val filename = "ProjectForge-$creditor-${translate("menu.fibu.kost")}" +
                "_${DateHelper.getDateAsFilenameSuffix(Date())}.xls"
        return RestUtils.downloadFile(filename, xls)
    }

    /**
     * Wicket's radio group over `EingangsrechnungListFilter.listType`, as a filter of the result list. See
     * [OutgoingInvoiceEntityRest]'s `PaymentStateFilter`, including why the info comes from the cache and not
     * from [org.projectforge.business.fibu.AbstractRechnungDO.ensuredInfo].
     */
    private class PaymentStateFilter(
        private val listType: String,
        private val rechnungCache: RechnungCache,
    ) : CustomResultFilter<EingangsrechnungDO> {
        override fun match(list: MutableList<EingangsrechnungDO>, element: EingangsrechnungDO): Boolean {
            val info = rechnungCache.getEingangsrechnungInfo(element.id) ?: element.ensuredInfo
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
         * assignments of each position. See [OutgoingInvoiceEntityRest.assignNumbersAndIndicesToNewRows].
         */
        internal fun assignNumbersAndIndicesToNewRows(invoice: EingangsrechnungDO) {
            val positions = invoice.positionen ?: return
            var nextNumber = (positions.filter { it.id != null }.maxOfOrNull { it.number } ?: 0).toInt()
            positions.filter { it.id == null }.forEach { position ->
                position.number = (++nextNumber).toShort()
            }
            positions.forEach { position ->
                val assignments = position.kostZuweisungen ?: return@forEach
                var nextIndex = (assignments.filter { it.id != null }.maxOfOrNull { it.index } ?: -1).toInt()
                assignments.filter { it.id == null }.forEach { assignment ->
                    assignment.index = (++nextIndex).toShort()
                }
            }
        }

        /** Id of the payment state filter, standing for `EingangsrechnungListFilter.listType`. */
        internal const val LIST_TYPE_FILTER = "listType"
        private const val LIST_TYPE_UNPAID = "unbezahlt"
        private const val LIST_TYPE_PAID = "bezahlt"
        private const val LIST_TYPE_OVERDUE = "ueberfaellig"

        /** Sub path of the cost assignment export, as `creditor-invoice-list-actions.tsx` calls it. */
        internal const val EXPORT_COST_ASSIGNMENTS_PATH = "exportCostAssignmentsAsExcel"

        private const val CURRENCY_FORMAT = "#,##0.00;[Red]-#,##0.00"

        /** Aliases of the Excel columns that are no property of [EingangsrechnungDO]. */
        private const val COL_NET_SUM = "netSum"
        private const val COL_GROSS_SUM = "grossSum"
        private const val COL_ACCOUNT = "kontoNummer"
        private const val COL_ACCOUNT_TEXT = "kontoBezeichnung"

        /**
         * The sort ids no database column can answer, and the value each one sorts by (see [filterList]).
         * Keyed by what `creditor-invoice.page.tsx` declares its columns as.
         */
        private val COMPUTED_SORT_PROPERTIES = mapOf<String, (EingangsrechnungDO) -> Comparable<*>?>(
            Eingangsrechnung::netSum.name to { it.ensuredInfo.netSum },
            Eingangsrechnung::grossSumWithDiscount.name to { it.ensuredInfo.grossSumWithDiscount },
        )
    }
}
