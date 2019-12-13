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

import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.commons.collections4.CollectionUtils
import org.projectforge.business.excel.ExcelDateFormats
import org.projectforge.business.excel.XlsContentProvider
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.export.MyXlsContentProvider
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
open class ForecastExportNew { // open needed by Wicket.
    @Autowired
    private lateinit var orderBookDao: AuftragDao

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    enum class ForecastCol(val header: String) {
        ORDER_NR("Nr."), POS_NR("Position"), DATE_OF_OFFER("Angebotsdatum"), DATE("Erfassungsdatum"),
        DATE_OF_DECISION("Entscheidungsdatum"), HEAD("HOB"), PROJECT("Projekt"),
        TITEL("Titel"), POS_TITLE("Pos.-Titel"), ART("Art"), ABRECHNUNGSART("Abrechnungsart"),
        AUFTRAG_STATUS("Auftrag Status"), POSITION_STATUS("Position Status"),
        PT("PT"), NETTOSUMME("Nettosumme"), FAKTURIERT("fakturiert"),
        NOCH_ZU_FAKTURIEREN("Noch zu fakturieren"), VOLLSTAENDIG_FAKTURIERT("vollständig fakturiert"),
        DEBITOREN_RECHNUNGEN("Debitorenrechnungen"), LEISTUNGSZEITRAUM("Leistungszeitraum"),
        EINTRITTSWAHRSCHEINLICHKEIT("Eintrittswahrsch. in %"), ANSPRECHPARTNER("Ansprechpartner"),
        STRUKTUR_ELEMENT("Strukturelement"), BEMERKUNG("Bemerkung"), WAHRSCHEINLICHKEITSWERT("Wahrscheinlichkeitswert"),
        MONATSENDE_START_DATUM("Monatsende"), START_DATUM("Startdatum +1"), MONATSENDE_ENDE_DATUM("Monatsende Enddatum +1"),
        ANZAHL_MONATE("Anzahl Monate")
    }

    enum class MonthCol(val header: String) {
        MONTH1("Month 1"), MONTH2("Month 2"), MONTH3("Month 3"), MONTH4("Month 4"), MONTH5("Month 5"), MONTH6("Month 6"),
        MONTH7("Month 7"), MONTH8("Month 8"), MONTH9("Month 9"), MONTH10("Month 10"), MONTH11("Month 11"), MONTH12("Month 12")
    }

    enum class CellStyles(val style: String) {
        CURRENCY(MyXlsContentProvider.FORMAT_CURRENCY)
    }

    private class Context(val workbook: ExcelWorkbook, val forecastSheet: ExcelSheet) {
        var excelDateFormat = ThreadLocalUserContext.getUser()?.excelDateFormat ?: ExcelDateFormats.EXCEL_DEFAULT_DATE
        var currencyCellStyle = workbook.createOrGetCellStyle("DataFormat.currency")
        var percentageCellStyle = workbook.createOrGetCellStyle("DataFormat.percentage")

        init {
            currencyCellStyle.dataFormat = workbook.getDataFormat(XlsContentProvider.FORMAT_CURRENCY)
            percentageCellStyle.dataFormat = workbook.getDataFormat("#")
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

        //sheetProvider.putFormat(MyXlsContentProvider.FORMAT_CURRENCY, PosCol.NETSUM, PosCol.INVOICED, PosCol.TO_BE_INVOICED)
        //sheetProvider.putFormat(DateFormats.getExcelFormatString(DateFormatType.DATE), PosCol.DATE_OF_OFFER, PosCol.DATE_OF_ENTRY, PosCol.PERIOD_OF_PERFORMANCE_BEGIN,
        //        PosCol.PERIOD_OF_PERFORMANCE_END)

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
                        // addPosMapping(mapping, order, pos, startDate)
                        // sheet.addRow(mapping.mapping, 0)
                    }
                }
            }
        }
        //fillIstSum(sheet, istSumMap)
        //sheet.setAutoFilter()
        replaceMonthDatesInHeaderRow(context, startDate, forecastSheet)
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

    /* private fun fillIstSum(sheet: ExportSheet, istSumMap: Map<PosCol, BigDecimal>) {
         val istRow = sheet.getRow(6)
         var i = 30
         for (monthCol in istSumMap.keys) {
             istRow.getCell(i).setValue(istSumMap[monthCol])
             i++
         }
     }*/

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
        val sheet = ctx.forecastSheet
        sheet.setDoubleValue(row, ForecastCol.ORDER_NR.header, order.nummer?.toDouble())
        sheet.setStringValue(row, ForecastCol.POS_NR.header, "#${pos.number}")
        sheet.setDateValue(row, ForecastCol.DATE_OF_OFFER.header, order.angebotsDatum, ctx.excelDateFormat)
        sheet.setDateValue(row, ForecastCol.DATE.header, order.erfassungsDatum, ctx.excelDateFormat)
        sheet.setDateValue(row, ForecastCol.DATE_OF_DECISION.header, ForecastUtils.ensureErfassungsDatum(order), ctx.excelDateFormat)
        sheet.setStringValue(row, ForecastCol.HEAD.header, order.headOfBusinessManager?.getFullname())
        sheet.setStringValue(row, ForecastCol.PROJECT.header, order.projektAsString)
        sheet.setStringValue(row, ForecastCol.TITEL.header, order.titel)
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
        val toBeInvoicedSum = netSum.subtract(invoicedSum)

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
        sheet.setBigDecimalValue(row, ForecastCol.EINTRITTSWAHRSCHEINLICHKEIT.header, probability.multiply(BigDecimal(100))).cellStyle = ctx.percentageCellStyle

        sheet.setStringValue(row, ForecastCol.ANSPRECHPARTNER.header, order.contactPerson?.getFullname())
        val node = TenantRegistryMap.getInstance().tenantRegistry.taskTree.getTaskNodeById(pos.taskId)
        sheet.setStringValue(row, ForecastCol.STRUKTUR_ELEMENT.header, node?.task?.title ?: "")
        sheet.setStringValue(row, ForecastCol.BEMERKUNG.header, pos.bemerkung)

        val accurenceValue = ForecastUtils.computeAccurenceValue(order, pos)
        sheet.setBigDecimalValue(row, ForecastCol.WAHRSCHEINLICHKEITSWERT.header, accurenceValue)

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
                sum = sum.add(schedule.amount!!.multiply(probability))
                if (beginDistribute.isBefore(schedule.scheduleDate!!)) {
                    beginDistribute = PFDate.from(schedule.scheduleDate)!!
                }
            }
            fillByPaymentSchedule(paymentSchedules, ctx, order, pos, startDate)
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
                AuftragsPositionsPaymentType.TIME_AND_MATERIALS -> fillMonthColumnsDistributed(diff, ctx, order, pos, startDate, beginDistribute)
                AuftragsPositionsPaymentType.PAUSCHALE -> if (order.probabilityOfOccurrence != null) {
                    fillMonthColumnsDistributed(diff, ctx, order, pos, startDate, beginDistribute)
                }
                AuftragsPositionsPaymentType.FESTPREISPAKET ->  // fill reset at end of project time
                    addEndAtPeriodOfPerformance(diff, ctx, order, pos, startDate)
            }
        }
    }

    private fun fillByPaymentSchedule(paymentSchedules: List<PaymentScheduleDO>, ctx: Context,
                                      order: AuftragDO, pos: AuftragsPositionDO, startDate: PFDate) { // payment values
        val probability = ForecastUtils.getProbabilityOfAccurence(order, pos)
        var currentMonth = startDate.plusMonths(-1).beginOfMonth
        /*for (monthCol in monthCols) {
            currentMonth = currentMonth.plusMonths(1)
            var sum = BigDecimal(0.0)
            for (schedule in paymentSchedules) {
                if (schedule.vollstaendigFakturiert) {
                    continue
                }
                val date = PFDate.from(schedule.scheduleDate)!!.plusMonths(1).endOfMonth
                if (date.year == currentMonth.year && date.month == currentMonth.month) {
                    sum = sum.add(schedule.amount!!.multiply(probability))
                }
            }
            if (sum.compareTo(BigDecimal.ZERO) > 0 && checkAfterMonthBefore(currentMonth)) {
                mapping.add(monthCol, sum)
            }
        }*/
    }

    private fun addEndAtPeriodOfPerformance(sum: BigDecimal, ctx: Context,
                                            order: AuftragDO?, pos: AuftragsPositionDO, startDate: PFDate) {
        val posEndDate = ForecastUtils.getEndLeistungszeitraumNextMonthEnd(order, pos)
        val index = getMonthIndex(posEndDate, startDate)
        if (index < 0 || index > 11) {
            return
        }
        // handle payment difference
        /*val previousValue = mapping.mapping[monthCols[index].name]
        if (previousValue == null && checkAfterMonthBefore(posEndDate)) {
            mapping.add(monthCols[index], sum)
        } else {
            if (checkAfterMonthBefore(posEndDate)) {
                mapping.add(monthCols[index], sum.add(previousValue as BigDecimal?))
            }
        }*/
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

    private fun fillMonthColumnsDistributed(value: BigDecimal, ctx: Context, order: AuftragDO?, pos: AuftragsPositionDO,
                                            startDate: PFDate, beginDistribute: PFDate) {
        var indexBegin = getMonthIndex(beginDistribute, startDate)
        var indexEnd = getMonthIndex(ForecastUtils.getEndLeistungszeitraumNextMonthEnd(order, pos), startDate)
        if (indexEnd < indexBegin) { //should not happen
            return
        }
        val partlyNettoSum = value.divide(BigDecimal.valueOf(indexEnd - indexBegin + 1.toLong()), RoundingMode.HALF_UP)
        // create bounds
        if (indexBegin < 0) {
            indexBegin = 0
        }
        if (indexEnd > 11) {
            indexEnd = 11
        }
        for (i in indexBegin..indexEnd) {
            //ctx.forecastSheet.setBigDecimalValue(row, monthCols[i], partlyNettoSum)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ForecastExport::class.java)
    }
}
