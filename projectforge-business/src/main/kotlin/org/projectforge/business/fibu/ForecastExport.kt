/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.merlin.I18n
import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import de.micromata.merlin.excel.ExcelWriterContext
import org.projectforge.Const
import org.projectforge.business.excel.ExcelDateFormats
import org.projectforge.business.excel.XlsContentProvider
import org.projectforge.business.task.TaskTree
import org.projectforge.common.DateFormatType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter

/**
 * Forcast excel export based on order book with probabilities as well as on already invoiced orders.
 *
 * @author Florian Blumenstein
 * @author Kai Reinhard
 */
@Service
open class ForecastExport { // open needed by Wicket.
    @Autowired
    private lateinit var orderBookDao: AuftragDao

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    enum class Sheet(val title: String) {
        FORECAST("Forecast_Data"),
        INVOICES("Rechnungen"),
        INVOICES_PREV_YEAR("Rechnungen Vorjahr")
    }

    enum class ForecastCol(val header: String) {
        ORDER_NR("Nr."), POS_NR("Pos."), DATE_OF_OFFER("Angebotsdatum"), DATE("Erfassungsdatum"),
        DATE_OF_DECISION("Entscheidungsdatum"), HEAD("HOB"), CUSTOMER("Kunde"), PROJECT("Projekt"),
        TITEL("Titel"), POS_TITLE("Pos.-Titel"), ART("Art"), ABRECHNUNGSART("Abrechnungsart"),
        AUFTRAG_STATUS("Auftrag Status"), POSITION_STATUS("Position Status"),
        PT("PT"), NETTOSUMME("Nettosumme"), FAKTURIERT("fakturiert"),
        TO_BE_INVOICED("gewichtet offen"), VOLLSTAENDIG_FAKTURIERT("vollständig fakturiert"),
        DEBITOREN_RECHNUNGEN("Debitorenrechnungen"), LEISTUNGSZEITRAUM("Leistungszeitraum"),
        EINTRITTSWAHRSCHEINLICHKEIT("Eintrittswahrsch. in %"), ANSPRECHPARTNER("Ansprechpartner"),
        STRUKTUR_ELEMENT("Strukturelement"), BEMERKUNG("Bemerkung"), PROBABILITY_NETSUM("gewichtete Nettosumme"),
        ANZAHL_MONATE("Anzahl Monate"), PAYMENT_SCHEDULE("Zahlplan"),
        DIFFERENCE("Differenz")
    }

    enum class InvoicesCol(val header: String) {
        INVOICE_NR("Nr."), POS_NR("Pos."), DATE("Datum"), CUSTOMER("Kunde"), PROJECT("Projekt"),
        SUBJECT("Betreff"), POS_TEXT("Positionstext"), DATE_OF_PAYMENT("Bezahldatum"),
        LEISTUNGSZEITRAUM("Leistungszeitraum"), ORDER("Auftrag"), NETSUM("Netto")
    }

    enum class MonthCol(val header: String) {
        MONTH1("Month 1"), MONTH2("Month 2"), MONTH3("Month 3"), MONTH4("Month 4"), MONTH5("Month 5"), MONTH6("Month 6"),
        MONTH7("Month 7"), MONTH8("Month 8"), MONTH9("Month 9"), MONTH10("Month 10"), MONTH11("Month 11"), MONTH12("Month 12")
    }

    private class Context(workbook: ExcelWorkbook, val forecastSheet: ExcelSheet, val invoicesSheet: ExcelSheet, val invoicesPriorYearSheet: ExcelSheet,
                          val baseDate: PFDay, val invoices: List<RechnungDO>) {
        val excelDateFormat = ThreadLocalUserContext.getUser()?.excelDateFormat ?: ExcelDateFormats.EXCEL_DEFAULT_DATE
        val dateFormat = DateTimeFormatter.ofPattern(DateFormats.getFormatString(DateFormatType.DATE_SHORT))!!
        val currencyFormat = NumberHelper.getCurrencyFormat(ThreadLocalUserContext.getLocale())
        val currencyCellStyle = workbook.createOrGetCellStyle("DataFormat.currency")
        val percentageCellStyle = workbook.createOrGetCellStyle("DataFormat.percentage")
        val writerContext = ExcelWriterContext(I18n(Const.RESOURCE_BUNDLE_NAME, ThreadLocalUserContext.getLocale()), workbook)
        val orderMap = mutableMapOf<Int, AuftragDO>()
        val orderPositionMap = mutableMapOf<Int, AuftragsPositionDO>()
        val today = PFDay.now()
        val thisMonth = today.beginOfMonth

        init {
            currencyCellStyle.dataFormat = workbook.getDataFormat(XlsContentProvider.FORMAT_CURRENCY)
            percentageCellStyle.dataFormat = workbook.getDataFormat("0%")
        }
    }

    // Vergangene Auftragspositionen anzeigen, die nicht vollständig fakturiert bzw. abgelehnt sind.

    @Throws(IOException::class)
    open fun export(origFilter: AuftragFilter): ByteArray? {
        val baseDateParam = origFilter.periodOfPerformanceStartDate
        val baseDate = if (baseDateParam != null) PFDay.from(baseDateParam).beginOfMonth else PFDay.now().beginOfYear
        val prioYearBaseDate = baseDate.plusYears(-1) // One day back for getting all invoices.

        val filter = AuftragFilter()
        filter.searchString = origFilter.searchString
        //filter.auftragFakturiertFilterStatus = origFilter.auftragFakturiertFilterStatus
        //filter.auftragsPositionsPaymentType = origFilter.auftragsPositionsPaymentType
        filter.periodOfPerformanceStartDate = baseDate.plusYears(-2).localDate // Go 2 years back for getting all orders referred by invoices of prior year.
        filter.user = origFilter.user
        val orderList = orderBookDao.getList(filter)
        if (orderList.isNullOrEmpty()) {
            return null
        }
        val invoiceFilter = RechnungFilter()
        invoiceFilter.fromDate = prioYearBaseDate.plusDays(-1).localDate // Go 1 day back, paranoia setting for getting all invoices of time period.
        val queryFilter = AuftragAndRechnungDaoHelper.createQueryFilterWithDateRestriction(invoiceFilter)
        queryFilter.addOrder(desc("datum"))
        queryFilter.addOrder(desc("nummer"))
        val invoices = rechnungDao.internalGetList(queryFilter)
        log.info("Exporting forecast script for date ${baseDate.isoString}")
        val forecastTemplate = applicationContext.getResource("classpath:officeTemplates/ForecastTemplate.xlsx")

        val workbook = ExcelWorkbook(forecastTemplate.inputStream, "ForecastTemplate.xlsx")
        val forecastSheet = workbook.getSheet(Sheet.FORECAST.title)!!
        ForecastCol.values().forEach { forecastSheet.registerColumn(it.header) }
        MonthCol.values().forEach { forecastSheet.registerColumn(it.header) }

        val invoicesSheet = workbook.getSheet(Sheet.INVOICES.title)!!
        InvoicesCol.values().forEach { invoicesSheet.registerColumn(it.header) }
        MonthCol.values().forEach { invoicesSheet.registerColumn(it.header) }

        val invoicesPriorYearSheet = workbook.getSheet(Sheet.INVOICES_PREV_YEAR.title)!!
        InvoicesCol.values().forEach { invoicesPriorYearSheet.registerColumn(it.header) }
        MonthCol.values().forEach { invoicesPriorYearSheet.registerColumn(it.header) }

        val ctx = Context(workbook, forecastSheet, invoicesSheet, invoicesPriorYearSheet, baseDate, invoices)

        var currentRow = 9
        for (order in orderList) {
            ctx.orderMap[order.id] = order
            for (pos in order.positionen ?: continue) {
                ctx.orderPositionMap[pos.id] = pos // Register all order positions for invoice handling.
            }
            if (order.isDeleted || order.positionenExcludingDeleted.isEmpty()) {
                continue
            }
            orderBookDao.calculateInvoicedSum(order)
            if (ForecastUtils.auftragsStatusToShow.contains(order.auftragsStatus)) {
                for (pos in order.positionenExcludingDeleted) {
                    if (pos.status != null && ForecastUtils.auftragsPositionsStatusToShow.contains(pos.status!!)) {
                        addOrderPosition(ctx, currentRow++, order, pos)
                    }
                }
            }
        }
        fillInvoices(ctx)
        replaceMonthDatesInHeaderRow(forecastSheet, baseDate)
        replaceMonthDatesInHeaderRow(invoicesSheet, baseDate)
        replaceMonthDatesInHeaderRow(invoicesPriorYearSheet, prioYearBaseDate)
        forecastSheet.setAutoFilter()
        invoicesSheet.setAutoFilter()
        invoicesPriorYearSheet.setAutoFilter()

        // Now: evaluate the formulars:
        for (row in 1..7) {
            val excelRow = forecastSheet.getRow(row)!!
            MonthCol.values().forEach {
                val cell = excelRow.getCell(forecastSheet.getColumnDef(it.header)!!)
                cell?.evaluateFormularCell()
            }
        }
        val revenueSheet = workbook.getSheet("Umsatz kumuliert")!!
        for (row in 0..8) {
            val excelRow = revenueSheet.getRow(row)!!
            for (col in 1..12) {
                val cell = excelRow.getCell(col)
                cell?.evaluateFormularCell()
            }
        }

        val result = workbook.asByteArrayOutputStream.toByteArray()
        workbook.close()
        return result
    }

    private fun fillInvoices(ctx: Context) {
        val firstMonthCol = ctx.invoicesSheet.getColumnDef(MonthCol.MONTH1.header)!!.columnNumber
        for (invoice in ctx.invoices) {
            for (pos in invoice.positionen ?: continue) {
                val orderPos = pos.auftragsPosition ?: continue
                if (ctx.orderPositionMap[orderPos.id] == null) {
                    continue // Ignore invoices referring an order position which isn't part of the order list filtered by the user.
                }
                val order = ctx.orderMap[orderPos.auftragId]
                if (order == null) {
                    log.error("Shouldn't occur: order position is registered but referred order itself not.")
                    continue
                }
                var monthIndex = getMonthIndex(ctx, PFDay.fromOrNow(invoice.datum))
                if (monthIndex !in -12..11) {
                    continue
                }
                val sheet = if (monthIndex < 0) ctx.invoicesPriorYearSheet else ctx.invoicesSheet
                if (monthIndex < 0) {
                    monthIndex += 12
                }
                val rowNumber = sheet.createRow().rowNum
                sheet.setIntValue(rowNumber, InvoicesCol.INVOICE_NR.header, invoice.nummer)
                sheet.setStringValue(rowNumber, InvoicesCol.POS_NR.header, "#${pos.number}")
                sheet.setDateValue(rowNumber, InvoicesCol.DATE.header, PFDay(invoice.datum!!).utilDate, ctx.excelDateFormat)
                sheet.setStringValue(rowNumber, InvoicesCol.CUSTOMER.header, invoice.kundeAsString)
                sheet.setStringValue(rowNumber, InvoicesCol.PROJECT.header, invoice.projekt?.name)
                sheet.setStringValue(rowNumber, InvoicesCol.SUBJECT.header, invoice.betreff)
                sheet.setStringValue(rowNumber, InvoicesCol.POS_TEXT.header, pos.text)
                invoice.bezahlDatum?.let {
                    sheet.setDateValue(rowNumber, InvoicesCol.DATE_OF_PAYMENT.header, PFDay(it).utilDate, ctx.excelDateFormat)
                }
                val leistungsZeitraumColDef = sheet.getColumnDef(InvoicesCol.LEISTUNGSZEITRAUM.header)
                invoice.periodOfPerformanceBegin?.let {
                    sheet.setDateValue(rowNumber, leistungsZeitraumColDef, PFDay(it).utilDate, ctx.excelDateFormat)
                }
                invoice.periodOfPerformanceEnd?.let {
                    sheet.setDateValue(rowNumber, leistungsZeitraumColDef!!.columnNumber + 1, PFDay(it).utilDate, ctx.excelDateFormat)
                }
                sheet.setStringValue(rowNumber, InvoicesCol.ORDER.header, "${order.nummer}.${orderPos.number}")
                sheet.setBigDecimalValue(rowNumber, InvoicesCol.NETSUM.header, pos.netSum).cellStyle = ctx.currencyCellStyle
                sheet.setBigDecimalValue(rowNumber, firstMonthCol + monthIndex, pos.netSum).cellStyle = ctx.currencyCellStyle
            }
        }
    }

    private fun replaceMonthDatesInHeaderRow(sheet: ExcelSheet, baseDate: PFDay) { // Adding month columns
        var currentMonth = baseDate
        MonthCol.values().forEach {
            val cell = sheet.headRow!!.getCell(sheet.getColumnDef(it.header)!!)
            cell?.setCellValue(formatMonthHeader(currentMonth))
            currentMonth = currentMonth.plusMonths(1)
        }
    }

    private fun addOrderPosition(ctx: Context, row: Int, order: AuftragDO, pos: AuftragsPositionDO) {
        val sheet = ctx.forecastSheet
        sheet.setIntValue(row, ForecastCol.ORDER_NR.header, order.nummer)
        sheet.setStringValue(row, ForecastCol.POS_NR.header, "#${pos.number}")
        order.angebotsDatum?.let {
            sheet.setDateValue(row, ForecastCol.DATE_OF_OFFER.header, PFDay(it).utilDate, ctx.excelDateFormat)
        }
        ForecastUtils.ensureErfassungsDatum(order)?.let {
            sheet.setDateValue(row, ForecastCol.DATE.header, PFDay(it).utilDate, ctx.excelDateFormat)
        }
        order.entscheidungsDatum?.let {
            sheet.setDateValue(row, ForecastCol.DATE_OF_DECISION.header, PFDay(it).utilDate, ctx.excelDateFormat)
        }
        sheet.setStringValue(row, ForecastCol.HEAD.header, order.headOfBusinessManager?.getFullname())
        sheet.setStringValue(row, ForecastCol.CUSTOMER.header, order.kundeAsString)
        sheet.setStringValue(row, ForecastCol.PROJECT.header, order.projektAsString)
        sheet.setStringValue(row, ForecastCol.TITEL.header, order.titel)
        if (pos.titel != order.titel)
            sheet.setStringValue(row, ForecastCol.POS_TITLE.header, pos.titel)
        sheet.setStringValue(row, ForecastCol.ART.header, if (pos.art != null) translate(pos.art?.i18nKey) else "")
        sheet.setStringValue(row, ForecastCol.ABRECHNUNGSART.header, if (pos.paymentType != null) translate(pos.paymentType?.i18nKey) else "")
        sheet.setStringValue(row, ForecastCol.AUFTRAG_STATUS.header, if (order.auftragsStatus != null) translate(order.auftragsStatus?.i18nKey) else "")
        sheet.setStringValue(row, ForecastCol.POSITION_STATUS.header, if (pos.status != null) translate(pos.status?.i18nKey) else "")
        sheet.setIntValue(row, ForecastCol.PT.header, pos.personDays?.toInt() ?: 0)
        sheet.setBigDecimalValue(row, ForecastCol.NETTOSUMME.header, pos.nettoSumme
                ?: BigDecimal.ZERO).cellStyle = ctx.currencyCellStyle

        val netSum = pos.nettoSumme ?: BigDecimal.ZERO
        val invoicedSum = pos.fakturiertSum ?: BigDecimal.ZERO
        val probabilityNetSum = ForecastUtils.computeProbabilityNetSum(order, pos)
        val toBeInvoicedSum = if (probabilityNetSum > invoicedSum) probabilityNetSum - invoicedSum else BigDecimal.ZERO

        sheet.setBigDecimalValue(row, ForecastCol.NETTOSUMME.header, netSum).cellStyle = ctx.currencyCellStyle
        if (invoicedSum.compareTo(BigDecimal.ZERO) != 0) {
            sheet.setBigDecimalValue(row, ForecastCol.FAKTURIERT.header, invoicedSum).cellStyle = ctx.currencyCellStyle
        }
        if (toBeInvoicedSum.compareTo(BigDecimal.ZERO) != 0) {
            sheet.setBigDecimalValue(row, ForecastCol.TO_BE_INVOICED.header, toBeInvoicedSum).cellStyle = ctx.currencyCellStyle
        }
        sheet.setStringValue(row, ForecastCol.VOLLSTAENDIG_FAKTURIERT.header, if (pos.vollstaendigFakturiert == true) "x" else "")

        val invoicePositions = rechnungCache.getRechnungsPositionVOSetByAuftragsPositionId(pos.id)
        sheet.setStringValue(row, ForecastCol.DEBITOREN_RECHNUNGEN.header, ForecastUtils.getInvoices(invoicePositions))
        val leistungsZeitraumColDef = sheet.getColumnDef(ForecastCol.LEISTUNGSZEITRAUM.header)!!
        if (PeriodOfPerformanceType.OWN == pos.periodOfPerformanceType) { // use "own" period -> from pos

            sheet.setDateValue(row, leistungsZeitraumColDef, PFDay(pos.periodOfPerformanceBegin!!).utilDate, ctx.excelDateFormat)
            sheet.setDateValue(row, leistungsZeitraumColDef.columnNumber + 1, PFDay(pos.periodOfPerformanceEnd!!).utilDate, ctx.excelDateFormat)
        } else { // use "see above" period -> from order
            sheet.setDateValue(row, leistungsZeitraumColDef, PFDay(order.periodOfPerformanceBegin!!).utilDate, ctx.excelDateFormat)
            sheet.setDateValue(row, leistungsZeitraumColDef.columnNumber + 1, PFDay(order.periodOfPerformanceEnd!!).utilDate, ctx.excelDateFormat)
        }
        val probability = ForecastUtils.getProbabilityOfAccurence(order, pos)
        sheet.setBigDecimalValue(row, ForecastCol.EINTRITTSWAHRSCHEINLICHKEIT.header, probability).cellStyle = ctx.percentageCellStyle

        sheet.setBigDecimalValue(row, ForecastCol.PROBABILITY_NETSUM.header, probabilityNetSum).cellStyle = ctx.currencyCellStyle

        sheet.setStringValue(row, ForecastCol.ANSPRECHPARTNER.header, order.contactPerson?.getFullname())
        val node = TaskTree.getInstance().getTaskNodeById(pos.taskId)
        sheet.setStringValue(row, ForecastCol.STRUKTUR_ELEMENT.header, node?.task?.title ?: "")
        sheet.setStringValue(row, ForecastCol.BEMERKUNG.header, pos.bemerkung)

        sheet.setBigDecimalValue(row, ForecastCol.ANZAHL_MONATE.header, ForecastUtils.getMonthCountForOrderPosition(order, pos))

        // get payment schedule for order position
        val paymentSchedules = ForecastUtils.getPaymentSchedule(order, pos)
        val sumPaymentSchedule: BigDecimal
        var beginDistribute: PFDay
        // handle payment schedule
        if (paymentSchedules.isNotEmpty()) {
            var sum = BigDecimal.ZERO
            beginDistribute = PFDay.fromOrNow(paymentSchedules[0].scheduleDate)
            val sb = StringBuilder()
            var first = true
            for (schedule in paymentSchedules) {
                if (schedule.vollstaendigFakturiert) // Ignore payments already invoiced.
                    continue
                val amount = schedule.amount!!.multiply(probability)
                sum += amount
                schedule.scheduleDate?.let { scheduleDate ->
                    if (beginDistribute.isBefore(scheduleDate)) {
                        beginDistribute = PFDay.from(scheduleDate)
                    }
                }
                if (first) first = false else sb.append(", ")
                sb.append("${beginDistribute.format(ctx.dateFormat)}: ${ctx.currencyFormat.format(amount)}")
            }
            sheet.setStringValue(row, ForecastCol.PAYMENT_SCHEDULE.header, sb.toString())
            fillByPaymentSchedule(paymentSchedules, ctx, row, order, pos)
            sumPaymentSchedule = sum
        } else {
            sumPaymentSchedule = BigDecimal.ZERO
            beginDistribute = ForecastUtils.getStartLeistungszeitraum(order, pos)
        }
        // compute diff, return if diff is empty
        val diff = probabilityNetSum - sumPaymentSchedule
        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return
        }
        // handle diff
        when (pos.paymentType) {
            AuftragsPositionsPaymentType.FESTPREISPAKET -> { // fill reset at end of project time
                val indexEnd = getMonthIndex(ctx, ForecastUtils.getEndLeistungszeitraum(order, pos)) + 1 // Will be invoiced 1 month later (+1)
                if (indexEnd in 0..11) {
                    val firstMonthCol = ctx.forecastSheet.getColumnDef(MonthCol.MONTH1.header)!!.columnNumber
                    ctx.forecastSheet.setBigDecimalValue(row, firstMonthCol + indexEnd, diff).cellStyle = ctx.currencyCellStyle
                }
            }
            else -> {
                fillMonthColumnsDistributed(diff, ctx, row, order, pos, beginDistribute, toBeInvoicedSum)
            }
        }
    }

    private fun fillByPaymentSchedule(paymentSchedules: List<PaymentScheduleDO>, ctx: Context, row: Int,
                                      order: AuftragDO, pos: AuftragsPositionDO) { // payment values
        val probability = ForecastUtils.getProbabilityOfAccurence(order, pos)
        var currentMonth = ctx.baseDate.plusMonths(-1).beginOfMonth
        MonthCol.values().forEach {
            currentMonth = currentMonth.plusMonths(1)
            if (checkAfterMonthBefore(currentMonth)) {
                var sum = BigDecimal.ZERO
                for (schedule in paymentSchedules) {
                    if (schedule.vollstaendigFakturiert) {
                        continue
                    }
                    val date = PFDay.fromOrNow(schedule.scheduleDate).endOfMonth
                    if (date.year == currentMonth.year && date.month == currentMonth.month) {
                        sum += schedule.amount!!.multiply(probability).setScale(2, RoundingMode.HALF_UP)
                    }
                }
                if (sum != BigDecimal.ZERO) {
                    val columnDef = ctx.forecastSheet.getColumnDef(it.header)!!
                    val cell = ctx.forecastSheet.setBigDecimalValue(row, columnDef, sum.setScale(2, RoundingMode.HALF_UP))
                    cell.cellStyle = ctx.currencyCellStyle
                    if (sum < BigDecimal.ZERO) {
                        highlightErrorCell(ctx, row, columnDef.columnNumber)
                    }
                }
            }
        }
    }

    private fun highlightErrorCell(ctx: Context, rowNumber: Int, colNumber: Int, comment: String? = null) {
        val excelRow = ctx.forecastSheet.getRow(rowNumber)!!
        val excelCell = excelRow.getCell(colNumber)
        ctx.writerContext.cellHighlighter.highlightErrorCell(excelCell, ctx.writerContext, ctx.forecastSheet, ctx.forecastSheet.getColumnDef(0), excelRow)
        if (comment != null)
            ctx.writerContext.cellHighlighter.setCellComment(excelCell, comment)
    }

    private fun getMonthIndex(ctx: Context, date: PFDay): Int {
        val monthDate = date.year * 12 + date.monthValue
        val monthBaseDate = ctx.baseDate.year * 12 + ctx.baseDate.monthValue
        return monthDate - monthBaseDate // index from 0 to 11
    }

    /**
     * Checks, if given date is behind the month before now.
     *
     * @param toCheck
     * @return
     */
    private fun checkAfterMonthBefore(toCheck: PFDay): Boolean {
        val oneMonthBeforeNow = PFDay.now().plusMonths(-1)
        return toCheck.isAfter(oneMonthBeforeNow)
    }

    private fun fillMonthColumnsDistributed(value: BigDecimal, ctx: Context, row: Int, order: AuftragDO, pos: AuftragsPositionDO,
                                            beginDistribute: PFDay, toBeInvoicedSum: BigDecimal) {
        val currentMonth = getMonthIndex(ctx, ctx.thisMonth)
        val indexBegin = getMonthIndex(ctx, beginDistribute) + 1 // Will be invoiced one month later (+1).
        val indexEnd = getMonthIndex(ctx, ForecastUtils.getEndLeistungszeitraum(order, pos)) + 1 // Will be invoiced one month later (+1).
        if (indexEnd < indexBegin) { // should not happen
            return
        }
        val partlyNettoSum = value.divide(BigDecimal.valueOf(indexEnd - indexBegin + 1.toLong()), RoundingMode.HALF_UP)
        MonthCol.values().forEach {
            val month = it.ordinal
            if (month in indexBegin..indexEnd) {
                val columnDef = ctx.forecastSheet.getColumnDef(it.header)
                if (month >= currentMonth) {
                    // Distribute payments only in future
                    ctx.forecastSheet.setBigDecimalValue(row, columnDef, partlyNettoSum).cellStyle = ctx.currencyCellStyle
                }
            }
        }
        // Calculate the difference between to be invoiced sum and forecasted sums:
        var futureInvoicesAmount = toBeInvoicedSum.negate()
        for (m in indexBegin..indexEnd) {
            if (m >= currentMonth) {
                futureInvoicesAmount += partlyNettoSum
            }
        }
        if (futureInvoicesAmount.abs() > BigDecimal.ONE) { // Only differences greater than 1 Euro
            ctx.forecastSheet.setBigDecimalValue(row, ctx.forecastSheet.getColumnDef(ForecastCol.DIFFERENCE.header), futureInvoicesAmount).cellStyle = ctx.currencyCellStyle
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ForecastExport::class.java)
        private const val FORECAST_IST_SUM_ROW = 7
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

        fun formatMonthHeader(date: PFDay): String {
            return date.format(formatter)
        }
    }
}
