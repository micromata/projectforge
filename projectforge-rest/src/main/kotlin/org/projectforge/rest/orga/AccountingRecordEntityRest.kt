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

package org.projectforge.rest.orga

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.KontoCache
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.BuchungssatzDao
import org.projectforge.business.fibu.kost.reporting.ReportStorage
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.dto.Buchungssatz
import org.projectforge.rest.dto.BwaStatistics
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Date

/**
 * Hand-built REST backend for the DATEV accounting-record list ("Buchungssätze"), migrated from the Wicket
 * `AccountingRecordListPage`. The next frontend declares its own columns and edit form (definePage), so this is
 * a layout-free [AbstractDTOEntityRest] (no createListLayout/createEditLayout).
 *
 * The list carries an invoice-style booking-period filter (mapped to the fiscal year/month columns, see
 * [BuchungssatzDao.addTimePeriodPredicates]) and the BWA (business assessment) computed over the whole result
 * set. A report drill-down ([getReportRecords]) shows a fixed set of records from the per-user in-memory
 * [ReportStorage] the (still Wicket) reporting page creates.
 */
@RestController
@RequestMapping("${Rest.URL}/accountingRecord")
class AccountingRecordEntityRest :
    AbstractDTOEntityRest<BuchungssatzDO, Buchungssatz, BuchungssatzDao>(
        BuchungssatzDao::class.java,
        "fibu.buchungssatz.title",
    ) {

    @Autowired
    private lateinit var kontoCache: KontoCache

    /**
     * The DATEV-import right the Wicket `AccountingRecordListPage`/`AccountingRecordEditPage` gate on — kept
     * here so the migrated page is no more permissive than the pages it replaces. The DAO's group-only
     * select access (FINANCE/CONTROLLING) is intentionally *not* enough: the reporting page reads the same
     * records through that group access and must keep working without this right, so the gate lives at these
     * endpoints, not in [BuchungssatzDao]. Called from every read/write/meta choke point below. Restricted and
     * demo users are refused as well, as by the Wicket pages.
     */
    private fun checkDatevImportAccess() {
        accessChecker.checkLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE)
        accessChecker.checkRestrictedOrDemoUser()
    }

    override fun transformForDB(dto: Buchungssatz): BuchungssatzDO {
        checkDatevImportAccess() // choke point for every write (save/update/delete all go through transformForDB).
        val buchungssatzDO = BuchungssatzDO()
        dto.copyTo(buchungssatzDO)
        return buchungssatzDO
    }

    override fun transformFromDB(obj: BuchungssatzDO, editMode: Boolean): Buchungssatz {
        checkDatevImportAccess() // choke point for every read (list rows, edit read, report drill-down).
        val buchungssatz = Buchungssatz()
        buchungssatz.copyFrom(obj)
        return buchungssatz
    }

    override fun newDTO(): Buchungssatz {
        checkDatevImportAccess()
        return Buchungssatz()
    }

    /**
     * The single booking-period range filter, default-visible: a range of whole months (Wicket's from/to
     * year/month selects), as the booking period is a year and a month and nothing finer. Its from/to dates
     * are translated to fiscal year/month predicates in [preProcessMagicFilter].
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        checkDatevImportAccess() // gates listMeta, which returns before any row is transformed.
        elements.add(
            UIFilterElement(
                PERIOD_FILTER,
                UIFilterElement.FilterType.MONTH,
                label = translate("fibu.buchungssatz.bookingMonth"),
                defaultFilter = true,
            )
        )
    }

    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter,
    ): List<CustomResultFilter<BuchungssatzDO>>? {
        checkDatevImportAccess() // gates list/listPage even when the result set is empty (no row transformed).
        val entry = source.entries.find { it.field == PERIOD_FILTER }
        if (entry != null) {
            entry.synthetic = true
            val from = PFDayUtils.parseDate(entry.value.fromValue)
            val to = PFDayUtils.parseDate(entry.value.toValue)
            // Map the picked from/to dates to the fiscal year/month columns to keep the Wicket list's semantics.
            baseDao.addTimePeriodPredicates(target, from?.year, from?.monthValue, to?.year, to?.monthValue)
        }
        return null
    }

    /**
     * The record number column (`satznr`, shown as `yyyy-mm-#####`) sorts by year, month and number, as Wicket's
     * `formattedSatzNummer` did. The entity's `satznr` column alone is the number within a month only, so an
     * `ORDER BY satznr` interleaves the months (all `…-00001` first) — which, paged, looks as if only the
     * visible page were sorted.
     */
    override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
        val index = target.sortProperties.indexOfFirst { it.property == SATZNR }
        if (index < 0) {
            return
        }
        val sortOrder = target.sortProperties[index].sortOrder
        target.sortProperties.removeAt(index)
        target.sortProperties.addAll(
            index,
            SATZNR_SORT_COLUMNS.map { SortProperty(it, sortOrder) },
        )
    }

    /**
     * The four reference columns show the cost unit's/account's display name, which no `ORDER BY` can express.
     * Resolved via the caches (as the formatters do): the references are lazy, and sorting touches every row.
     */
    override val computedSortProperties: Map<String, (BuchungssatzDO) -> Comparable<*>?>
        get() = mapOf(
            "kost1.displayName" to { obj: BuchungssatzDO ->
                KostFormatter.instance.formatKost1(obj.kost1, KostFormatter.FormatType.TEXT)
            },
            "kost2.displayName" to { obj: BuchungssatzDO ->
                KostFormatter.instance.formatKost2(obj.kost2, KostFormatter.FormatType.TEXT)
            },
            "konto.displayName" to { obj: BuchungssatzDO -> kontoCache.getKontoIfNotInitialized(obj.konto)?.displayName },
            "gegenKonto.displayName" to { obj: BuchungssatzDO ->
                kontoCache.getKontoIfNotInitialized(obj.gegenKonto)?.displayName
            },
        )

    /**
     * BWA over the whole (non-paged) result set. For server paging the frontend asks for it separately via
     * [aggregate].
     */
    override fun postProcessResultSet(
        resultSet: ResultSet<BuchungssatzDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        val result = super.postProcessResultSet(resultSet, request, magicFilter)
        if (resultSet.offset == null) {
            result.statistics = BwaStatistics.from(resultSet.resultSet)
        }
        return result
    }

    /**
     * BWA over the full result set identified by [ids] (server-paging counterpart of [postProcessResultSet]).
     */
    override fun aggregate(ids: LongArray, filter: MagicFilter): Any? {
        return BwaStatistics.from(getListByIds(ids.toList()))
    }

    /**
     * Report drill-down: the fixed set of accounting records of a report (optionally of one business-assessment
     * row) plus that set's BWA, read from the per-user in-memory [ReportStorage] created by the (still Wicket)
     * reporting page. An absent storage / report (e.g. after a restart, or if the reporting UI was never opened)
     * yields an empty result — the same limitation the Wicket list has.
     */
    @AccessChecked("FIBU_DATEV_IMPORT (checkDatevImportAccess); own session storage")
    @GetMapping("reportRecords")
    fun getReportRecords(
        @RequestParam("reportId") reportId: String?,
        @RequestParam("businessAssessmentRowId", required = false) businessAssessmentRowId: String?,
    ): ReportRecordsResult {
        checkDatevImportAccess() // the Wicket drill-down opened the (import-right-gated) record list; keep parity.
        val result = ReportRecordsResult()
        if (reportId.isNullOrBlank()) {
            return result
        }
        val storage = userPrefService.getEntry(
            UserPrefService.LEGACY_XML_AREA,
            ReportStorage.USER_PREF_KEY,
            ReportStorage::class.java,
        ) ?: return result
        val report = storage.findById(reportId) ?: return result
        val records = if (!businessAssessmentRowId.isNullOrBlank()) {
            report.businessAssessment?.getRow(businessAssessmentRowId)?.accountRecords ?: emptyList()
        } else {
            report.buchungssaetze ?: emptyList()
        }
        result.records = records.map { transformFromDB(it, false) }
        result.statistics = BwaStatistics.from(records)
        return result
    }

    /**
     * The filtered list as the Excel file of Wicket's `AccountingRecordListPage` ("exportAsXls", sheet
     * "fibu.buchungssaetze"), with the record's own fields as Wicket's generic exporter wrote them. The rows
     * come from [getResultList], i.e. through the same pipeline (and access checks) the list itself uses. An
     * empty result answers 404 rather than a file.
     */
    @AccessChecked("FIBU_DATEV_IMPORT (checkDatevImportAccess); DAO select access")
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        checkDatevImportAccess()
        val list = getResultList(filter)
        if (list.isEmpty()) {
            return ResponseEntity.notFound().build<Any>()
        }
        ExcelUtils.prepareWorkbook().use { workbook ->
            val sheet = workbook.createOrGetSheet(translate("fibu.buchungssaetze"))
            // Excel's built-in "General" for the amount: merlin's default "#.#" shows whole numbers as "35," in
            // German Excel. Registered at column level before any cell exists, so setCellValue(BigDecimal) keeps it.
            val numberStyle = workbook.createOrGetCellStyle("number")
            numberStyle.dataFormat = workbook.createDataFormat().getFormat("General")
            sheet.registerColumn(translate("fibu.buchungssatz.satznr"), COL_SATZNR).withSize(16)
            sheet.registerColumn(translate("date"), COL_DATE).withSize(12)
            sheet.registerColumn(translate("fibu.common.betrag"), COL_AMOUNT).withSize(14)
            sheet.registerColumn(translate("finance.accountingRecord.dc"), COL_DC).withSize(10)
            sheet.registerColumn(translate("fibu.buchungssatz.konto"), COL_ACCOUNT).withSize(30)
            sheet.registerColumn(translate("fibu.buchungssatz.gegenKonto"), COL_COUNTER_ACCOUNT).withSize(30)
            sheet.registerColumn(translate("fibu.kost1"), COL_KOST1).withSize(14)
            sheet.registerColumn(translate("fibu.kost2"), COL_KOST2).withSize(14)
            sheet.registerColumn(translate("fibu.buchungssatz.beleg"), COL_VOUCHER).withSize(14)
            sheet.registerColumn(translate("fibu.buchungssatz.text"), COL_TEXT).withSize(40)
            sheet.registerColumn(translate("fibu.buchungssatz.menge"), COL_QUANTITY).withSize(10)
            sheet.registerColumn(translate("comment"), COL_COMMENT).withSize(40)
            sheet.setColumnStyle(COL_AMOUNT, numberStyle)
            ExcelUtils.addHeadRow(sheet)
            list.forEach { satz ->
                val row = sheet.createRow()
                if (satz.year != null && satz.month != null) {
                    row.getCell(COL_SATZNR)?.setCellValue(satz.formattedSatzNummer)
                }
                satz.datum?.let { row.getCell(COL_DATE)?.setCellValue(it) }
                satz.betrag?.let { row.getCell(COL_AMOUNT)?.setCellValue(it)?.setCellStyle(numberStyle) }
                satz.sh?.let { row.getCell(COL_DC)?.setCellValue(translate(it.i18nKey)) }
                kontoCache.getKontoIfNotInitialized(satz.konto)?.displayName
                    ?.let { row.getCell(COL_ACCOUNT)?.setCellValue(it) }
                kontoCache.getKontoIfNotInitialized(satz.gegenKonto)?.displayName
                    ?.let { row.getCell(COL_COUNTER_ACCOUNT)?.setCellValue(it) }
                satz.kost1?.let {
                    row.getCell(COL_KOST1)
                        ?.setCellValue(KostFormatter.instance.formatKost1(it, KostFormatter.FormatType.FORMATTED_NUMBER))
                }
                satz.kost2?.let {
                    row.getCell(COL_KOST2)
                        ?.setCellValue(KostFormatter.instance.formatKost2(it, KostFormatter.FormatType.FORMATTED_NUMBER))
                }
                satz.beleg?.let { row.getCell(COL_VOUCHER)?.setCellValue(it) }
                satz.text?.let { row.getCell(COL_TEXT)?.setCellValue(it) }
                satz.menge?.let { row.getCell(COL_QUANTITY)?.setCellValue(it) }
                satz.comment?.let { row.getCell(COL_COMMENT)?.setCellValue(it) }
            }
            sheet.setAutoFilter()
            val filename = "ProjectForge-AccountingRecords_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx"
            return RestUtils.downloadFile(filename, workbook.asByteArrayOutputStream.toByteArray())
        }
    }

    /** Response of [getReportRecords]: the records to list plus their BWA. */
    class ReportRecordsResult(
        var records: List<Buchungssatz> = emptyList(),
        var statistics: BwaStatistics? = null,
    )

    companion object {
        /** Filter field id of the booking-period range (invoice-style date picker). */
        internal const val PERIOD_FILTER = "timePeriod"

        /** Sort property of the record number column (the column id the next list sends). */
        private const val SATZNR = "satznr"

        /** The database columns the record number column sorts by, in this order. */
        private val SATZNR_SORT_COLUMNS = listOf("year", "month", SATZNR)

        private const val COL_SATZNR = "satznr"
        private const val COL_DATE = "datum"
        private const val COL_AMOUNT = "betrag"
        private const val COL_DC = "sh"
        private const val COL_ACCOUNT = "konto"
        private const val COL_COUNTER_ACCOUNT = "gegenKonto"
        private const val COL_KOST1 = "kost1"
        private const val COL_KOST2 = "kost2"
        private const val COL_VOUCHER = "beleg"
        private const val COL_TEXT = "text"
        private const val COL_QUANTITY = "menge"
        private const val COL_COMMENT = "comment"
    }
}
