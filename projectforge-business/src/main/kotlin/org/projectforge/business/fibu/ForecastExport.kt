/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
import org.apache.commons.collections4.CollectionUtils
import org.projectforge.Const
import org.projectforge.business.excel.ExcelDateFormats
import org.projectforge.business.excel.XlsContentProvider
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Forcast excel export.
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
    private lateinit var applicationContext: ApplicationContext

    enum class ForecastCol(val header: String) {
        ORDER_NR("Nr."), POS_NR("Position"), DATE_OF_OFFER("Angebotsdatum"), DATE("Erfassungsdatum"),
        DATE_OF_DECISION("Entscheidungsdatum"), HEAD("HOB"), CUSTOMER("Kunde"), PROJECT("Projekt"),
        TITEL("Titel"), POS_TITLE("Pos.-Titel"), ART("Art"), ABRECHNUNGSART("Abrechnungsart"),
        AUFTRAG_STATUS("Auftrag Status"), POSITION_STATUS("Position Status"),
        PT("PT"), NETTOSUMME("Nettosumme"), FAKTURIERT("fakturiert"),
        NOCH_ZU_FAKTURIEREN("Noch zu fakturieren"), VOLLSTAENDIG_FAKTURIERT("vollständig fakturiert"),
        DEBITOREN_RECHNUNGEN("Debitorenrechnungen"), LEISTUNGSZEITRAUM("Leistungszeitraum"),
        EINTRITTSWAHRSCHEINLICHKEIT("Eintrittswahrsch. in %"), ANSPRECHPARTNER("Ansprechpartner"),
        STRUKTUR_ELEMENT("Strukturelement"), BEMERKUNG("Bemerkung"), WAHRSCHEINLICHKEITSWERT("Wahrscheinlichkeitswert"),
        MONATSENDE_START_DATUM("Monatsende Startdatum +1"), MONATSENDE_ENDE_DATUM("Monatsende Enddatum +1"),
        ANZAHL_MONATE("Anzahl Monate")
    }

    enum class MonthCol(val header: String) {
        MONTH1("Month 1"), MONTH2("Month 2"), MONTH3("Month 3"), MONTH4("Month 4"), MONTH5("Month 5"), MONTH6("Month 6"),
        MONTH7("Month 7"), MONTH8("Month 8"), MONTH9("Month 9"), MONTH10("Month 10"), MONTH11("Month 11"), MONTH12("Month 12")
    }

    private class Context(workbook: ExcelWorkbook, val forecastSheet: ExcelSheet) {
        val excelDateFormat = ThreadLocalUserContext.getUser()?.excelDateFormat ?: ExcelDateFormats.EXCEL_DEFAULT_DATE
        val currencyCellStyle = workbook.createOrGetCellStyle("DataFormat.currency")!!
        val percentageCellStyle = workbook.createOrGetCellStyle("DataFormat.percentage")!!
        val monthMap = mutableMapOf<MonthCol, BigDecimal>()
        val writerContext = ExcelWriterContext(I18n(Const.RESOURCE_BUNDLE_NAME, ThreadLocalUserContext.getLocale()), workbook)

        init {
            currencyCellStyle.dataFormat = workbook.getDataFormat(XlsContentProvider.FORMAT_CURRENCY)
            percentageCellStyle.dataFormat = workbook.getDataFormat("0%")
        }
    }

    // Vergangene Auftragspositionen anzeigen, die nicht vollständig fakturiert bzw. abgelehnt sind.

    @Throws(IOException::class)
    open fun export(auftragList: List<AuftragDO?>, startDateParam: Date?): ByteArray? {
        if (CollectionUtils.isEmpty(auftragList)) {
            return null
        }
        val startDate = (if (startDateParam != null) PFDate.from(startDateParam)!! else PFDate.now()).beginOfMonth
        log.info("Exporting forecast script for date ${startDate.isoString}")
        val forecastTemplate = applicationContext.getResource("classpath:officeTemplates/ForecastTemplate.xlsx")

        val workbook = ExcelWorkbook(forecastTemplate.inputStream, "ForecastTemplate.xlsx")
        val forecastSheet = workbook.getSheet("Forecast_Data")
        ForecastCol.values().forEach {
            forecastSheet.registerColumn(it.header)
        }
        MonthCol.values().forEach {
            forecastSheet.registerColumn(it.header)
        }
        val context = Context(workbook, forecastSheet)

        val istSumMap = createIstSumMap()
        var currentRow = 9
        for (order in auftragList) {
            if (order == null || order.isDeleted || order.positionenExcludingDeleted.isEmpty()) {
                continue
            }
            orderBookDao.calculateInvoicedSum(order)
            if (ForecastUtils.auftragsStatusToShow.contains(order.auftragsStatus)) {
                for (pos in order.positionenExcludingDeleted) {
                    calculateIstSum(istSumMap, startDate, pos)
                    if (pos.status != null && ForecastUtils.auftragsPositionsStatusToShow.contains(pos.status!!)) {
                        addOrderPosition(context, currentRow++, order, pos, startDate)
                    }
                }
            }
        }
        fillIstSum(context, istSumMap)
        replaceMonthDatesInHeaderRow(context, startDate, forecastSheet)
        forecastSheet.setAutoFilter()

        // Now: evaluate the formulars:
        for (row in 1..6) {
            val excelRow = forecastSheet.getRow(row)
            MonthCol.values().forEach {
                val cell = excelRow.getCell(forecastSheet.getColumnDef(it.header))
                cell.evaluateFormularCell()
            }
        }
        val revenueSheet = workbook.getSheet("Umsatz kumuliert")
        for (row in 0..5) {
            val excelRow = revenueSheet.getRow(row)
            for (col in 1..12) {
                val cell = excelRow.getCell(col)
                cell.evaluateFormularCell()
            }
        }

        val result = workbook.asByteArrayOutputStream.toByteArray()
        workbook.close()
        return result
    }

    private fun createIstSumMap(): MutableMap<Int, BigDecimal> {
        val istSumMap = mutableMapOf<Int, BigDecimal>()
        for (month in 0..11) {
            istSumMap[month] = BigDecimal.ZERO
        }
        return istSumMap
    }

    private fun fillIstSum(ctx: Context, istSumMap: Map<Int, BigDecimal>) {
        val istRow = ctx.forecastSheet.getRow(FORECAST_IST_SUM_ROW)
        var col = ctx.forecastSheet.getColumnDef(MonthCol.MONTH1.header).columnNumber
        for (monthCol in istSumMap.keys) {
            ctx.forecastSheet.setBigDecimalValue(istRow.rowNum, col, istSumMap[monthCol]).cellStyle = ctx.currencyCellStyle
            col++
        }
    }

    private fun calculateIstSum(istSumMap: MutableMap<Int, BigDecimal>, startDate: PFDate, pos: AuftragsPositionDO) {
        val invoicePositions = rechnungCache.getRechnungsPositionVOSetByAuftragsPositionId(pos.id) ?: return
        val beginCurrentMonth = PFDate.now().beginOfMonth
        for (rpo in invoicePositions) {
            val rDate = PFDate.from(rpo.date)
            if (rDate?.isBefore(beginCurrentMonth) == true) {
                val monthCol = getMonthIndex(rDate, startDate)
                if (monthCol in 0..11) {
                    istSumMap.replace(monthCol, istSumMap[monthCol]!!.add(rpo.nettoSumme))
                }
            }
        }
    }

    private fun replaceMonthDatesInHeaderRow(ctx: Context, startDate: PFDate, sheet: ExcelSheet) { // Adding month columns
        val formatter = DateTimeFormatter.ofPattern("MMM yyyy")
        var currentMonth = startDate
        MonthCol.values().forEach {
            val cell = sheet.headRow.getCell(ctx.forecastSheet.getColumnDef(it.header))
            cell.setCellValue(currentMonth.format(formatter))
            currentMonth = currentMonth.plusMonths(1)
        }
    }

    private fun addOrderPosition(ctx: Context, row: Int, order: AuftragDO, pos: AuftragsPositionDO, startDate: PFDate) {
        ctx.monthMap.clear()
        val sheet = ctx.forecastSheet
        sheet.setIntValue(row, ForecastCol.ORDER_NR.header, order.nummer)
        sheet.setStringValue(row, ForecastCol.POS_NR.header, "#${pos.number}")
        sheet.setDateValue(row, ForecastCol.DATE_OF_OFFER.header, order.angebotsDatum, ctx.excelDateFormat)
        sheet.setDateValue(row, ForecastCol.DATE.header, order.erfassungsDatum, ctx.excelDateFormat)
        sheet.setDateValue(row, ForecastCol.DATE_OF_DECISION.header, ForecastUtils.ensureErfassungsDatum(order), ctx.excelDateFormat)
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
        val toBeInvoicedSum = if (netSum > invoicedSum) netSum.subtract(invoicedSum) else BigDecimal.ZERO

        sheet.setBigDecimalValue(row, ForecastCol.NETTOSUMME.header, netSum).cellStyle = ctx.currencyCellStyle
        sheet.setBigDecimalValue(row, ForecastCol.FAKTURIERT.header, invoicedSum).cellStyle = ctx.currencyCellStyle
        sheet.setBigDecimalValue(row, ForecastCol.NOCH_ZU_FAKTURIEREN.header, toBeInvoicedSum).cellStyle = ctx.currencyCellStyle
        sheet.setStringValue(row, ForecastCol.VOLLSTAENDIG_FAKTURIERT.header, if (pos.vollstaendigFakturiert == true) "x" else "")

        val invoicePositions = rechnungCache.getRechnungsPositionVOSetByAuftragsPositionId(pos.id)
        sheet.setStringValue(row, ForecastCol.DEBITOREN_RECHNUNGEN.header, ForecastUtils.getInvoices(invoicePositions))
        val leistungsZeitraumColDef = sheet.getColumnDef(ForecastCol.LEISTUNGSZEITRAUM.header)
        if (PeriodOfPerformanceType.OWN == pos.periodOfPerformanceType) { // use "own" period -> from pos
            sheet.setDateValue(row, leistungsZeitraumColDef, pos.periodOfPerformanceBegin, ctx.excelDateFormat)
            sheet.setDateValue(row, leistungsZeitraumColDef.columnNumber + 1, pos.periodOfPerformanceEnd, ctx.excelDateFormat)
        } else { // use "see above" period -> from order
            sheet.setDateValue(row, leistungsZeitraumColDef, order.periodOfPerformanceBegin, ctx.excelDateFormat)
            sheet.setDateValue(row, leistungsZeitraumColDef.columnNumber + 1, order.periodOfPerformanceEnd, ctx.excelDateFormat)
        }
        val probability = ForecastUtils.getProbabilityOfAccurence(order, pos)
        sheet.setBigDecimalValue(row, ForecastCol.EINTRITTSWAHRSCHEINLICHKEIT.header, probability).cellStyle = ctx.percentageCellStyle

        val accurenceValue = ForecastUtils.computeAccurenceValue(order, pos)
        sheet.setBigDecimalValue(row, ForecastCol.WAHRSCHEINLICHKEITSWERT.header, accurenceValue).cellStyle = ctx.currencyCellStyle

        sheet.setStringValue(row, ForecastCol.ANSPRECHPARTNER.header, order.contactPerson?.getFullname())
        val node = TenantRegistryMap.getInstance().tenantRegistry.taskTree.getTaskNodeById(pos.taskId)
        sheet.setStringValue(row, ForecastCol.STRUKTUR_ELEMENT.header, node?.task?.title ?: "")
        sheet.setStringValue(row, ForecastCol.BEMERKUNG.header, pos.bemerkung)

        sheet.setDateValue(row, ForecastCol.MONATSENDE_START_DATUM.header, ForecastUtils.getStartLeistungszeitraumNextMonthEnd(order, pos).sqlDate, ctx.excelDateFormat)
        sheet.setDateValue(row, ForecastCol.MONATSENDE_ENDE_DATUM.header, ForecastUtils.getEndLeistungszeitraumNextMonthEnd(order, pos).sqlDate, ctx.excelDateFormat)

        sheet.setBigDecimalValue(row, ForecastCol.ANZAHL_MONATE.header, ForecastUtils.getMonthCountForOrderPosition(order, pos))

        // get payment schedule for order position
        val paymentSchedules = ForecastUtils.getPaymentSchedule(order, pos)
        val sumPaymentSchedule: BigDecimal
        var beginDistribute: PFDate
        // handle payment schedule
        if (paymentSchedules.isNotEmpty()) {
            var sum = BigDecimal.ZERO
            beginDistribute = PFDate.from(paymentSchedules[0].scheduleDate)!!
            for (schedule in paymentSchedules) {
                if (schedule.vollstaendigFakturiert) // Ignore payments already invoiced.
                    continue
                sum = sum.add(schedule.amount!!.multiply(probability))
                if (beginDistribute.isBefore(schedule.scheduleDate!!)) {
                    beginDistribute = PFDate.from(schedule.scheduleDate)!!
                }
            }
            fillByPaymentSchedule(paymentSchedules, ctx, row, order, pos, startDate)
            sumPaymentSchedule = sum
            beginDistribute = beginDistribute.plusMonths(2) // values are added to the next month (+1), start the month after the last one (+1)
        } else {
            sumPaymentSchedule = BigDecimal.ZERO
            beginDistribute = ForecastUtils.getStartLeistungszeitraumNextMonthEnd(order, pos)
        }
        // compute diff, return if diff is empty
        val diff = accurenceValue.subtract(sumPaymentSchedule)
        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return
        }
        // handle diff
        if (pos.paymentType != null) {
            when (pos.paymentType) {
                AuftragsPositionsPaymentType.TIME_AND_MATERIALS -> fillMonthColumnsDistributed(diff, ctx, row, order, pos, startDate, beginDistribute)
                AuftragsPositionsPaymentType.PAUSCHALE -> if (order.probabilityOfOccurrence != null) {
                    fillMonthColumnsDistributed(diff, ctx, row, order, pos, startDate, beginDistribute)
                }
                AuftragsPositionsPaymentType.FESTPREISPAKET ->  // fill reset at end of project time
                    addEndAtPeriodOfPerformance(diff, ctx, row, order, pos, startDate)
            }
        }
    }

    private fun fillByPaymentSchedule(paymentSchedules: List<PaymentScheduleDO>, ctx: Context, row: Int,
                                      order: AuftragDO, pos: AuftragsPositionDO, startDate: PFDate) { // payment values
        val probability = ForecastUtils.getProbabilityOfAccurence(order, pos)
        var currentMonth = startDate.plusMonths(-1).beginOfMonth
        MonthCol.values().forEach {
            currentMonth = currentMonth.plusMonths(1)
            if (checkAfterMonthBefore(currentMonth)) {
                var sum = BigDecimal.ZERO
                for (schedule in paymentSchedules) {
                    if (schedule.vollstaendigFakturiert) {
                        continue
                    }
                    val date = PFDate.from(schedule.scheduleDate)!!.plusMonths(1).endOfMonth
                    if (date.year == currentMonth.year && date.month == currentMonth.month) {
                        sum = sum.add(schedule.amount!!.multiply(probability))
                    }
                }
                if (sum != BigDecimal.ZERO) {
                    val cell = ctx.forecastSheet.setBigDecimalValue(row, it.header, sum.setScale(2, RoundingMode.HALF_UP))
                    cell.cellStyle = ctx.currencyCellStyle
                    if (sum < BigDecimal.ZERO) {
                        highlightErrorCell(ctx, row)
                    }
                }
            }
        }
    }

    private fun highlightErrorCell(ctx: Context, rowNumber: Int) {
        val excelRow = ctx.forecastSheet.getRow(rowNumber)
        val excelCell = excelRow.getCell(0)
        ctx.writerContext.cellHighlighter.highlightErrorCell(excelCell, ctx.writerContext, ctx.forecastSheet, ctx.forecastSheet.getColumnDef(0), excelRow)
        //ctx.writerContext.cellHighlighter.setCellComment(excelCell, comment)
    }

    private fun addEndAtPeriodOfPerformance(sum: BigDecimal, ctx: Context, row: Int,
                                            order: AuftragDO, pos: AuftragsPositionDO, startDate: PFDate) {
        val posEndDate = ForecastUtils.getEndLeistungszeitraumNextMonthEnd(order, pos)
        val index = getMonthIndex(posEndDate, startDate)
        if (index < 0 || index > 11) {
            return
        }
        val month = MonthCol.values()[index]
        // handle payment difference
        val previousValue = ctx.monthMap[month]
        val value = if (previousValue == null && checkAfterMonthBefore(posEndDate)) {
            sum
        } else if (checkAfterMonthBefore(posEndDate)) {
            sum.add(previousValue)
        } else {
            BigDecimal.ZERO
        }
        ctx.monthMap[month] = value
        val cell = ctx.forecastSheet.setBigDecimalValue(row, month.header, sum)
        cell.cellStyle = ctx.currencyCellStyle
        if (sum < BigDecimal.ZERO) {
            if (sum < BigDecimal.ZERO) {
                highlightErrorCell(ctx, row)
            }
        }
    }

    private fun getMonthIndex(date: PFDate, startDate: PFDate): Int {
        val monthDate = date.year * 12 + date.monthValue
        val monthStartDate = startDate.year * 12 + startDate.monthValue
        return monthDate - monthStartDate + 1 // index from 0 to 11, +1 because table starts one month before
    }

    /**
     * Checks, if given date is behind the month before now.
     *
     * @param toCheck
     * @return
     */
    private fun checkAfterMonthBefore(toCheck: PFDate): Boolean {
        val oneMonthBeforeNow = PFDate.now().plusMonths(-1)
        return toCheck.isAfter(oneMonthBeforeNow)
    }

    private fun fillMonthColumnsDistributed(value: BigDecimal, ctx: Context, row: Int, order: AuftragDO, pos: AuftragsPositionDO,
                                            startDate: PFDate, beginDistribute: PFDate) {
        val indexBegin = getMonthIndex(beginDistribute, startDate)
        val indexEnd = getMonthIndex(ForecastUtils.getEndLeistungszeitraumNextMonthEnd(order, pos), startDate)
        if (indexEnd < indexBegin) { //should not happen
            return
        }
        val partlyNettoSum = value.divide(BigDecimal.valueOf(indexEnd - indexBegin + 1.toLong()), RoundingMode.HALF_UP)
        MonthCol.values().forEach {
            ctx.forecastSheet.setBigDecimalValue(row, ctx.forecastSheet.getColumnDef(it.header), partlyNettoSum).cellStyle = ctx.currencyCellStyle
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ForecastExport::class.java)
        private const val FORECAST_IST_SUM_ROW = 7
    }
}
