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

package org.projectforge.business.fibu

import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.projectforge.business.fibu.ForecastExportContext.*
import org.projectforge.business.fibu.orderbooksnapshots.OrderbookSnapshotsService
import org.projectforge.business.scripting.ScriptLogger
import org.projectforge.business.scripting.ThreadLocalScriptingContext
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.common.FilenameUtils
import org.projectforge.common.extensions.format2Digits
import org.projectforge.common.extensions.formatCurrency
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import org.projectforge.business.fibu.ForecastExportContext as Context

private val log = KotlinLogging.logger {}

/**
 * Forcast excel export based on order book with probabilities as well as on already invoiced orders.
 *
 * @author Kai Reinhard
 * @author Florian Blumenstein
 */
@Service
open class ForecastExport { // open needed by Wicket.

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var forecastExportInvoices: ForecastExportInvoices

    @Autowired
    private lateinit var orderbookSnapshotsService: OrderbookSnapshotsService

    @Autowired
    private lateinit var orderDao: AuftragDao

    @Autowired
    private lateinit var ordersCache: AuftragsCache

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    /**
     * Export the forecast sheet.
     * @param origFilter The filter for the orders to export.
     * @param planningDate If given, the monthly forecast will be calculated with the specified date and inserted as plan data.
     * @param snapshotDate Today (null) or, the day of the snapshot, if the orderList is loaded from order book snapshots.
     * @param fillUnitCol The function to get the unit of the order to show in the unit column.
     */
    @JvmOverloads
    @Throws(IOException::class)
    open fun xlsExport(
        origFilter: AuftragFilter,
        planningDate: LocalDate? = null,
        snapshotDate: LocalDate? = null,
        fillUnitCol: ((orderInfo: OrderInfo) -> String)? = null,
    ): ByteArray? {
        val startDateParam = origFilter.periodOfPerformanceStartDate
        val startDate = if (startDateParam != null) PFDay.from(startDateParam).beginOfMonth else PFDay.now().beginOfYear
        val filter = AuftragFilter()
        filter.searchString = origFilter.searchString
        filter.projectList = origFilter.projectList
        //filter.auftragFakturiertFilterStatus = origFilter.auftragFakturiertFilterStatus
        //filter.auftragsPositionsPaymentType = origFilter.auftragsPositionsPaymentType
        filter.periodOfPerformanceStartDate =
            startDate.plusYears(-2).localDate // Go 2 years back for getting all orders referred by invoices of prior year.
        filter.user = origFilter.user
        val scriptLogger = ThreadLocalScriptingContext.getLogger()
        val closestPlanningDate = getClosestSnapshotDate(planningDate, scriptLogger, "planning")
        val closestSnapshotDate = getClosestSnapshotDate(snapshotDate, scriptLogger, "snapshot")
        val msgSB = StringBuilder("Exporting forecast script with start date ${startDate.isoString}")
        if (closestPlanningDate != null) {
            msgSB.append(" with planningDate ${closestPlanningDate}")
        }
        if (closestSnapshotDate != null) {
            msgSB.append(" with snapshotDate ${closestSnapshotDate}")
        }
        if (!filter.searchString.isNullOrBlank()) {
            msgSB.append(" with filter: str='${filter.searchString}'")
        }
        // scriptLogger?.info { msgSB } ?: log.info { msgSB }
        val orderList = if (closestSnapshotDate != null) {
            readSnapshot(closestSnapshotDate, filter)
        } else {
            orderDao.select(filter)
        }
        if (!filter.projectList.isNullOrEmpty()) {
            log.info {
                "$msgSB, projects=${
                    filter.projectList?.sortedBy { it.name }?.joinToString() { it.name ?: "???" }
                }"
            }
        }
        val showAll = accessChecker.isLoggedInUserMemberOfGroup(
            ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.CONTROLLING_GROUP
        ) &&
                filter.searchString.isNullOrBlank() &&
                filter.projectList.isNullOrEmpty()
        try {
            return xlsExport(
                orderList,
                startDate = startDate,
                planningDate = closestPlanningDate,
                snapshotDate = closestSnapshotDate,
                showAll = showAll,
                auftragFilter = filter,
                scriptLogger = scriptLogger,
                fillUnitCol = fillUnitCol,
            )
        } catch (ex: Exception) {
            log.error(ex) { "Error exporting forecast: $ex" }
            throw ex
        }
    }

    private fun getStartDate(origFilter: AuftragFilter): PFDay {
        val startDateParam = origFilter.periodOfPerformanceStartDate
        return if (startDateParam != null) PFDay.from(startDateParam).beginOfMonth else PFDay.now().beginOfYear
    }

    open fun getExcelFilenmame(origFilter: AuftragFilter): String {
        return getFilename(getStartDate(origFilter), extension = ".xlsx")
    }

    private fun getClosestSnapshotDate(date: LocalDate?, scriptLogger: ScriptLogger?, name: String): LocalDate? {
        date ?: return null
        val closestDate = orderbookSnapshotsService.findClosestSnapshotDate(date)
        if (closestDate != date) {
            val msg = "No $name found for date $date. Using closest $name date $closestDate."
            scriptLogger?.warn { msg } ?: log.warn { msg }
        }
        return closestDate
    }

    /**
     * Get the export filename. Example: 'Forecast-start-2021-01_2025-01-02_22-48.xlsx'
     * or 'Forecast-ACME-snapshot-2023-08-01-start-2023-01-2025-01-02_22-48.zip'.
     * @param startDate The start date for the forecast.
     * @param extension The optional file extension ('.xlsx' or '.zip').
     * @param part The optional part of the export file (e.g. 'ACME', 'Customer', ...).
     */
    open fun getFilename(
        startDate: PFDay,
        extension: String? = null,
        part: String? = null,
        planningDate: LocalDate? = null,
        snapshot: LocalDate? = null
    ): String {
        val startDateString = "-start_${startDate.year}-${startDate.monthValue.format2Digits()}"
        val partString = if (part.isNullOrBlank()) "" else "-${FilenameUtils.escapeFilename(part)}"
        val usePlanningDate = orderbookSnapshotsService.findClosestSnapshotDate(planningDate)
        val planningDateString = if (planningDate != null) "-plan_${usePlanningDate}" else ""
        val useSnapshot = orderbookSnapshotsService.findClosestSnapshotDate(snapshot)
        val snapshotString = if (useSnapshot != null) "-snapshot_${useSnapshot}" else ""
        val created = DateHelper.getDateAsFilenameSuffix()
        return "${created}_Forecast$partString$planningDateString$snapshotString${startDateString}${extension ?: ""}"
    }

    /**
     * Export the forecast sheet.
     * @param orderList The list of orders to export.
     * @param startDate The start date for the forecast.
     * @param showAll True, if no filter is given, for financial and controlling staff only.
     * @param planningDate If given, the monthly forecast will be calculated with the specified date and inserted as plan data.
     * @param snapshotDate Today (null) or, the day of the snapshot, if the orderList is loaded from order book snapshots.
     *              If the date is in the past, the forecast will be simulated with the specified date.
     *              If date is given, no caches will be used.
     * @return The byte array of the Excel file.
     */
    @Throws(IOException::class)
    private fun xlsExport(
        orderList: Collection<AuftragDO>,
        startDate: PFDay,
        showAll: Boolean,
        planningDate: LocalDate?,
        snapshotDate: LocalDate?,
        auftragFilter: AuftragFilter,
        scriptLogger: ScriptLogger?,
        fillUnitCol: ((orderInfo: OrderInfo) -> String)?,
    ): ByteArray? {
        if (orderList.isEmpty()) {
            val msg = "No orders found for export."
            scriptLogger?.info { msg } ?: log.info { msg } // scriptLogger does also log.info
            // No orders found, so we don't need the forecast sheet.
            return null
        }
        val useAuftragsCache = snapshotDate == null
        val prevYearBaseDate = startDate.plusYears(-1) // One year back for getting all invoices.
        val invoiceFilter = RechnungFilter()
        invoiceFilter.fromDate =
            prevYearBaseDate.plusDays(-1).localDate // Go 1 day back, paranoia setting for getting all invoices for given time period.
        if (snapshotDate != null) {
            // Don't load invoices later than snapshotDate:
            invoiceFilter.toDate = snapshotDate.minusDays(1)
        }
        val queryFilter = AuftragAndRechnungDaoHelper.createQueryFilterWithDateRestriction(invoiceFilter)
        queryFilter.addOrder(desc("datum"))
        queryFilter.addOrder(desc("nummer"))
        var invoices = rechnungDao.select(queryFilter, checkAccess = false)
        if (snapshotDate != null) {
            // For time travel: Filter invoices before the given date.
            invoices = invoices.filter { it.datum?.let { datum -> PFDay(datum).isBefore(snapshotDate) } ?: false }
        }
        val forecastTemplate = applicationContext.getResource("classpath:officeTemplates/ForecastTemplate.xlsx")

        ExcelWorkbook(forecastTemplate.inputStream, "ForecastTemplate.xlsx").use { workbook ->
            val forecastSheet = workbook.getSheet(Sheet.FORECAST.title)!!
            log.debug { "Forecast sheet: $forecastSheet" }
            ForecastCol.entries.forEach { forecastSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { forecastSheet.registerColumn(it.header) }

            val planningSheet = workbook.getSheet(Sheet.PLANNING.title)!!
            log.debug { "Planning forecast sheet: $planningSheet" }
            ForecastCol.entries.forEach { planningSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { planningSheet.registerColumn(it.header) }

            val invoicesSheet = workbook.getSheet(Sheet.INVOICES.title)!!
            log.debug { "Invoices sheet: $invoicesSheet" }
            InvoicesCol.entries.forEach { invoicesSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { invoicesSheet.registerColumn(it.header) }

            val invoicesPrevYearSheet = workbook.getSheet(Sheet.INVOICES_PREV_YEAR.title)!!
            log.debug { "InvoicesPriorYearSheet sheet: $invoicesPrevYearSheet" }
            InvoicesCol.entries.forEach { invoicesPrevYearSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { invoicesPrevYearSheet.registerColumn(it.header) }

            val planningInvoicesSheet = workbook.getSheet(Sheet.PLANNING_INVOICES.title)!!
            log.debug { "PlanningInvoicesSheet sheet: $planningInvoicesSheet" }
            InvoicesCol.entries.forEach { planningInvoicesSheet.registerColumn(it.header) }
            MonthCol.entries.forEach { planningInvoicesSheet.registerColumn(it.header) }

            val ctx = Context(
                workbook,
                forecastSheet = forecastSheet,
                invoicesSheet = invoicesSheet,
                invoicesPrevYearSheet = invoicesPrevYearSheet,
                planningSheet = planningSheet,
                planningInvoicesSheet = planningInvoicesSheet,
                startDate = startDate,
                invoices = invoices,
                baseDate = PFDay.fromOrNow(snapshotDate),
                planningDate = planningDate,
                snapshot = snapshotDate != null,
                fillUnitCol = fillUnitCol,
            )
            ctx.showAll = showAll

            val infoSheet = workbook.getSheet(Sheet.INFO.title)!!
            infoSheet.setDateValue(0, 1, Date(), ctx.excelDateFormat)
            planningDate?.let { infoSheet.setDateValue(1, 1, it, ctx.excelDateFormat) }
            snapshotDate?.let { infoSheet.setDateValue(2, 1, it, ctx.excelDateFormat) }
            log.debug { "info sheet: $infoSheet" }

            val orderPositionsFound =
                fillOrderPositions(
                    orderList,
                    ctx,
                    ctx.forecastSheet,
                    baseDate = snapshotDate,
                    useAuftragsCache,
                )
            if (!orderPositionsFound) {
                val msg = "No orders positions found for export."
                scriptLogger?.info { msg } ?: log.info { msg } // scriptLogger does also log.info
                // No order positions found, so we don't need the forecast sheet.
                return null
            }
            forecastExportInvoices.fillInvoices(ctx)
            replaceMonthDatesInHeaderRow(forecastSheet, startDate, true)
            replaceMonthDatesInHeaderRow(planningSheet, startDate, true)
            replaceMonthDatesInHeaderRow(invoicesSheet, startDate)
            replaceMonthDatesInHeaderRow(invoicesPrevYearSheet, prevYearBaseDate)
            replaceMonthDatesInHeaderRow(planningInvoicesSheet, startDate)
            if (!ctx.hasUnitColEntries) {
                ExcelUtils.setColumnHidden(forecastSheet, ForecastCol.UNIT.header, true)
                ExcelUtils.setColumnHidden(planningSheet, ForecastCol.UNIT.header, true)
            }
            ExcelUtils.setAutoFilter(forecastSheet, FORECAST_HEAD_ROW, 0, FORECAST_NUMBER_OF_COLS_AUTOFILTER)
            invoicesSheet.setAutoFilter()
            invoicesPrevYearSheet.setAutoFilter()
            ExcelUtils.setAutoFilter(planningSheet, FORECAST_HEAD_ROW, 0, FORECAST_NUMBER_OF_COLS_AUTOFILTER)
            planningInvoicesSheet.setAutoFilter()

            fillPlanningForecast(planningDate, auftragFilter, ctx)
            workbook.pOIWorkbook.creationHelper.createFormulaEvaluator().evaluateAll()
            return workbook.asByteArrayOutputStream.toByteArray()
        }
    }

    /**
     * Fill the forecast data of order positions.
     * @param orderList The list of orders to export.
     * @param ctx The context for the export.
     * @param sheet The Excelsheet to use (forecast or planning)
     * @param useAuftragsCache True, if the orders cache should be used for updated order info, otherwise false (for snapshots and plannings the
     *                         cache shouldn't be used.).
     * @return true, if order positions found and filled, otherwise false.
     */
    private fun fillOrderPositions(
        orderList: Collection<AuftragDO>,
        ctx: Context,
        sheet: ExcelSheet,
        baseDate: LocalDate?,
        useAuftragsCache: Boolean,
    ): Boolean {
        // Set the date in the upper left corner (red and bold) for showing date of snapshot/orderbook.
        sheet.getCell(1, 0)?.setCellValue(baseDate ?: ctx.baseDate.localDate)
        sheet.getRow(FORECAST_HEAD_ROW)
        var currentRow = FORECAST_FISRT_ORDER_ROW
        var orderPositionFound = false
        for (auftragDO in orderList) {
            auftragDO.projekt?.id?.let { projektId ->
                ctx.projectIds.add(projektId)
            }
            val orderInfo = if (useAuftragsCache) ordersCache.getOrderInfo(auftragDO) else auftragDO.info
            auftragDO.id?.let { ctx.orderMap[it] = orderInfo }
            orderInfo.infoPositions?.forEach { pos ->
                pos.id?.let {
                    ctx.orderPositionMap[it] = pos // Register all order positions for invoice handling.
                    ctx.orderMapByPositionId[it] = orderInfo
                }
            }
            if (auftragDO.deleted || orderInfo.infoPositions.isNullOrEmpty()) {
                continue
            }

            if (ForecastUtils.auftragsStatusToShow.contains(auftragDO.status)) {
                orderInfo.infoPositions?.forEach { pos ->
                    if (pos.status in ForecastUtils.auftragsPositionsStatusToShow) {
                        addOrderPosition(
                            ctx,
                            sheet,
                            currentRow++,
                            orderInfo,
                            pos,
                            baseDate = baseDate,
                            useAuftragsCache = useAuftragsCache,
                        )
                        orderPositionFound = true
                    }
                }
            }
        }
        return orderPositionFound
    }

    private fun fillPlanningForecast(planningDate: LocalDate?, auftragFilter: AuftragFilter, ctx: Context, ) {
        planningDate ?: return
        val orderList = readSnapshot(planningDate, auftragFilter)
        fillOrderPositions(
            orderList,
            ctx,
            ctx.planningSheet,
            baseDate = planningDate,
            useAuftragsCache = false,
        )
    }

    private fun replaceMonthDatesInHeaderRow(
        sheet: ExcelSheet,
        baseDate: PFDay,
        planningSheet: Boolean = false
    ) { // Adding month columns
        var currentMonth = baseDate
        MonthCol.entries.forEach {
            sheet.headRow!!.getCell(sheet.getColumnDef(it.header)!!).also { cell ->
                cell.setCellValue(formatMonthHeader(currentMonth))
            }
            if (planningSheet) {
                // Second head row for planning sheet:
                sheet.getRow(FORECAST_HEAD_ROW).getCell(sheet.getColumnDef(it.header)!!)
                    .setCellValue(formatMonthHeader(currentMonth))
            }
            currentMonth = currentMonth.plusMonths(1)
        }
        if (planningSheet) {
            // Clear first heading row for planning sheet:
            sheet.headRow?.let { row ->
                ExcelUtils.clearCells(row, 0, 26)
                ExcelUtils.clearCells(row, 39, FORECAST_NUMBER_OF_COLS)
            }
        }
    }

    private fun addOrderPosition(
        ctx: Context,
        sheet: ExcelSheet,
        row: Int,
        order: OrderInfo,
        pos: OrderPositionInfo,
        baseDate: LocalDate?,
        useAuftragsCache: Boolean,
    ) {
        val isPlanningSheet = ctx.planningSheet == sheet
        sheet.setIntValue(row, ForecastCol.ORDER_NR.header, order.nummer)
        sheet.setStringValue(row, ForecastCol.POS_NR.header, "#${pos.number}")
        ExcelUtils.setLongValue(sheet, row, ForecastCol.PROJECT_ID.header, order.projektId)
        val excelRow = row + 1 // Excel row number for formulas, 1-based.
        if (isPlanningSheet) {
            // Planning sheet: visible column is true, if the project is visible in the forecast sheet.
            val visibleProjectIdCol =
                ctx.forecastSheet.getColumnDef(ForecastCol.VISIBLE_PROJECT_ID.header)?.columnNumberAsLetters
            val projectIdCol = ctx.forecastSheet.getColumnDef(ForecastCol.PROJECT_ID.header)?.columnNumberAsLetters
            ExcelUtils.setCellFormula(
                sheet,
                row,
                ForecastCol.VISIBLE.header,
                "COUNTIF(Forecast_Data!$visibleProjectIdCol$11:$visibleProjectIdCol$100000, $projectIdCol$excelRow) > 0"
            )
        } else {
            // Visible cell is 1, if row is visible (by filter), otherwise, 0.
            ExcelUtils.setCellFormula(sheet, row, ForecastCol.VISIBLE.header, "SUBTOTAL(3, A$excelRow)")
            val visibleCol = ctx.forecastSheet.getColumnDef(ForecastCol.VISIBLE.header)?.columnNumberAsLetters
            val projectIdCol = ctx.forecastSheet.getColumnDef(ForecastCol.PROJECT_ID.header)?.columnNumberAsLetters
            ExcelUtils.setCellFormula(
                sheet,
                row,
                ForecastCol.VISIBLE_PROJECT_ID.header,
                "IF($visibleCol$excelRow=1, $projectIdCol$excelRow, \"\")"
            )
        }
        order.angebotsDatum?.let {
            sheet.setDateValue(row, ForecastCol.DATE_OF_OFFER.header, PFDay(it).localDate, ctx.excelDateFormat)
        }
        ForecastUtils.ensureErfassungsDatum(order).let {
            sheet.setDateValue(row, ForecastCol.DATE.header, PFDay(it).localDate, ctx.excelDateFormat)
        }
        order.entscheidungsDatum?.let {
            sheet.setDateValue(row, ForecastCol.DATE_OF_DECISION.header, PFDay(it).localDate, ctx.excelDateFormat)
        }
        ctx.fillUnitCol?.invoke(order)?.let {
            if (it.isNotBlank()) {
                ctx.hasUnitColEntries = true
            }
            sheet.setStringValue(row, ForecastCol.UNIT.header, it)
        }
        sheet.setStringValue(row, ForecastCol.CUSTOMER.header, order.kundeAsString)
        sheet.setStringValue(row, ForecastCol.PROJECT.header, order.projektAsString)
        sheet.setStringValue(row, ForecastCol.TITEL.header, order.titel)
        if (pos.titel != order.titel)
            sheet.setStringValue(row, ForecastCol.POS_TITLE.header, pos.titel)
        pos.art.let { art ->
            sheet.setStringValue(row, ForecastCol.ART.header, if (art != null) translate(art.i18nKey) else "")
        }
        pos.paymentType.let { paymentType ->
            sheet.setStringValue(
                row,
                ForecastCol.ABRECHNUNGSART.header,
                if (paymentType != null) translate(paymentType.i18nKey) else ""
            )
        }
        sheet.setStringValue(row, ForecastCol.AUFTRAG_STATUS.header, translate(order.status.i18nKey))
        sheet.setStringValue(row, ForecastCol.POSITION_STATUS.header, translate(pos.status.i18nKey))
        sheet.setIntValue(row, ForecastCol.PT.header, pos.personDays?.toInt() ?: 0)
        sheet.setBigDecimalValue(
            row, ForecastCol.NETTOSUMME.header, pos.netSum
                ?: BigDecimal.ZERO
        ).cellStyle = ctx.currencyCellStyle

        val orderInfo = if (useAuftragsCache) order else {
            ordersCache.getOrderInfo(order.id)
        }
        val posInfo = orderInfo?.getInfoPosition(pos.id)
        val netSum = pos.netSum ?: BigDecimal.ZERO
        val invoicedSum = posInfo?.invoicedSum ?: BigDecimal.ZERO
        val forecastInfo = ForecastOrderPosInfo(order, pos)
        forecastInfo.calculate()
        sheet.setBigDecimalValue(row, ForecastCol.NETTOSUMME.header, netSum).cellStyle = ctx.currencyCellStyle
        if (invoicedSum.compareTo(BigDecimal.ZERO) != 0) {
            sheet.setBigDecimalValue(row, ForecastCol.FAKTURIERT.header, invoicedSum).cellStyle = ctx.currencyCellStyle
        }
        val toBeInvoicedSum = forecastInfo.toBeInvoicedSum
        if (toBeInvoicedSum.compareTo(BigDecimal.ZERO) != 0) {
            sheet.setBigDecimalValue(row, ForecastCol.TO_BE_INVOICED.header, toBeInvoicedSum).cellStyle =
                ctx.currencyCellStyle
        }
        sheet.setStringValue(
            row,
            ForecastCol.VOLLSTAENDIG_FAKTURIERT.header,
            if (pos.vollstaendigFakturiert) "x" else ""
        )

        val invoicePositions = rechnungCache.getRechnungsPosInfosByAuftragsPositionId(pos.id)?.filter {
            // Don't load invoices later than snapshotDate or planningDate (baseDate):
            baseDate == null || (it.rechnungInfo?.date ?: LocalDate.MAX) <= baseDate
        }
        sheet.setStringValue(row, ForecastCol.DEBITOREN_RECHNUNGEN.header, ForecastUtils.getInvoices(invoicePositions))
        val leistungsZeitraumColDef = sheet.getColumnDef(ForecastCol.LEISTUNGSZEITRAUM.header)!!
        if (PeriodOfPerformanceType.OWN == pos.periodOfPerformanceType) { // use "own" period -> from pos
            sheet.setDateValue(
                row,
                leistungsZeitraumColDef,
                PFDay(pos.periodOfPerformanceBegin!!).localDate,
                ctx.excelDateFormat
            )
            sheet.setDateValue(
                row,
                leistungsZeitraumColDef.columnNumber + 1,
                PFDay(pos.periodOfPerformanceEnd!!).localDate,
                ctx.excelDateFormat
            )
        } else { // use "see above" period -> from order
            sheet.setDateValue(
                row,
                leistungsZeitraumColDef,
                PFDay.fromOrNull(order.periodOfPerformanceBegin)?.localDate,
                ctx.excelDateFormat
            )
            sheet.setDateValue(
                row,
                leistungsZeitraumColDef.columnNumber + 1,
                PFDay.fromOrNull(order.periodOfPerformanceEnd)?.localDate,
                ctx.excelDateFormat
            )
        }

        sheet.setBigDecimalValue(
            row,
            ForecastCol.EINTRITTSWAHRSCHEINLICHKEIT.header,
            forecastInfo.probability,
        ).cellStyle =
            ctx.percentageCellStyle

        sheet.setBigDecimalValue(row, ForecastCol.PROBABILITY_NETSUM.header, forecastInfo.weightedNetSum).cellStyle =
            ctx.currencyCellStyle

        sheet.setStringValue(row, ForecastCol.ANSPRECHPARTNER.header, order.contactPerson?.getFullname())
        val node = TaskTree.instance.getTaskNodeById(pos.taskId)
        sheet.setStringValue(row, ForecastCol.STRUKTUR_ELEMENT.header, node?.task?.title ?: "")
        sheet.setStringValue(row, ForecastCol.BEMERKUNG.header, pos.bemerkung)

        sheet.setBigDecimalValue(
            row,
            ForecastCol.ANZAHL_MONATE.header,
            ForecastUtils.getMonthCountForOrderPosition(order, pos)
        )
        sheet.setStringValue(
            row,
            ForecastCol.FORECAST_TYPE.header,
            translate(ForecastUtils.getForecastType(order, pos).i18nKey)
        )
        val remaining = forecastInfo.getRemainingForecastSumAfter(ctx.endDate)
        if (remaining.compareTo(BigDecimal.ZERO) != 0) {
            sheet.setBigDecimalValue(row, ForecastCol.REMAINING.header, remaining).cellStyle =
                ctx.currencyCellStyle
        }
        if (forecastInfo.difference.compareTo(BigDecimal.ZERO) != 0) {
            sheet.setBigDecimalValue(row, ForecastCol.DIFFERENCE.header, forecastInfo.difference).cellStyle =
                ctx.currencyCellStyle
        }

        if (forecastInfo.paymentEntries.isNotEmpty()) {
            val str = forecastInfo.paymentEntries.joinToString {
                "${it.scheduleDate.format(ctx.dateFormat)}: ${
                    ctx.currencyFormat.format(it.amount)
                }"
            }
            sheet.setStringValue(row, ForecastCol.PAYMENT_SCHEDULE.header, str)
        }
        forecastInfo.months.forEach { monthEntry ->
            val monthDate = monthEntry.date
            val offset = ctx.startDate.monthsBetween(monthDate).toInt()
            if (offset !in 0..11) {
                return@forEach // continue
            }
            if (monthEntry.toBeInvoicedSum.abs() < BigDecimal.ONE) {
                return@forEach // continue
            }
            val columnDef = sheet.getColumnDef(MonthCol.entries[offset].header)!!
            val cell =
                sheet.setBigDecimalValue(
                    row,
                    columnDef,
                    monthEntry.toBeInvoicedSum.setScale(2, RoundingMode.HALF_UP),
                )
            cell.cellStyle = ctx.currencyCellStyle
            if (monthEntry.lostBudgetWarning) {
                val errorStyle = when {
                    monthEntry.lostBudget > NumberHelper.HUNDRED_THOUSAND -> ctx.hugeErrorCellStyle
                    monthEntry.lostBudget > NumberHelper.TEN_THOUSAND -> ctx.largeErrorCellStyle
                    else -> ctx.errorCellStyle
                }
                sheet.setStringValue(
                    row,
                    ForecastCol.WARNING.header,
                    translateMsg(
                        "fibu.auftrag.forecast.lostBudgetWarning",
                        monthEntry.lostBudget.formatCurrency(true, scale = 0),
                        monthEntry.lostBudgetPercent,
                        ForecastOrderPosInfo.PERCENTAGE_OF_LOST_BUDGET_WARNING,
                    )
                ).cellStyle = errorStyle
                sheet.getCell(row, columnDef.columnNumber)?.cellStyle = ctx.errorCurrencyCellStyle
            }
        }
    }

    private fun readSnapshot(date: LocalDate, filter: AuftragFilter): List<AuftragDO> {
        return orderbookSnapshotsService.readSnapshot(date)?.filter { filter.match(it) }
            ?.sortedByDescending { it.nummer } ?: emptyList()
    }

    companion object {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

        private const val FORECAST_HEAD_ROW = 9
        private const val FORECAST_FISRT_ORDER_ROW = FORECAST_HEAD_ROW + 1
        private const val FORECAST_NUMBER_OF_COLS_AUTOFILTER = 47

        // Two more technical cols: ProjectID, visible and visibleID
        private const val FORECAST_NUMBER_OF_COLS = FORECAST_NUMBER_OF_COLS_AUTOFILTER + 3

        fun formatMonthHeader(date: PFDay): String {
            return date.format(formatter)
        }
    }
}
