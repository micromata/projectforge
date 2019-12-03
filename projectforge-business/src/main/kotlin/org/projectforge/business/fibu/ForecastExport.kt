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

import org.apache.commons.collections4.CollectionUtils
import org.projectforge.business.excel.*
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.common.DateFormatType
import org.projectforge.export.MyXlsContentProvider
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateFormats
import org.projectforge.framework.time.PFDate
import org.projectforge.framework.utils.NumberHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.Collectors

/**
 * Forcast excel export.
 *
 * @author Florian Blumenstein
 */
@Service
open class ForecastExport { // open needed by Wicket.
    @Autowired
    private lateinit var orderBookDao: AuftragDao

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    private val monthCols = arrayOf(PosCol.MONTH1, PosCol.MONTH2, PosCol.MONTH3, PosCol.MONTH4, PosCol.MONTH5, PosCol.MONTH6,
            PosCol.MONTH7, PosCol.MONTH8, PosCol.MONTH9, PosCol.MONTH10, PosCol.MONTH11, PosCol.MONTH12)

    private val auftragsPositionsStatusToShow = listOf(
            AuftragsPositionsStatus.IN_ERSTELLUNG,
            AuftragsPositionsStatus.POTENZIAL,
            AuftragsPositionsStatus.GELEGT,
            AuftragsPositionsStatus.BEAUFTRAGT,
            AuftragsPositionsStatus.LOI)

    @Throws(IOException::class)
    open fun export(auftragList: List<AuftragDO?>, startDateParam: Date?): ByteArray? {
        if (CollectionUtils.isEmpty(auftragList)) {
            return null
        }
        val startDate = (if (startDateParam != null) PFDate.from(startDateParam)!! else PFDate.now()).beginOfMonth
        log.info("Exporting forecast script for date ${startDate.isoString}")
        val forecastTemplate = applicationContext.getResource("classpath:officeTemplates/ForecastTemplate.xls")
        val xls = ExportWorkbook(forecastTemplate.inputStream)
        val contentProvider: ContentProvider = MyXlsContentProvider(xls)
        // create a default Date format and currency column
        xls.contentProvider = contentProvider
        val sheet = xls.getSheet("Forecast_Data")

        val colArr = PosCol.values().map { it.name }.toTypedArray()
        sheet.propertyNames = colArr
        replaceMonthDatesInHeaderRow(startDate, sheet.getRow(0))
        val sheetProvider = sheet.contentProvider
        sheetProvider.putFormat(MyXlsContentProvider.FORMAT_CURRENCY, PosCol.NETSUM, PosCol.INVOICED, PosCol.TO_BE_INVOICED)
        sheetProvider.putFormat(DateFormats.getExcelFormatString(DateFormatType.DATE), PosCol.DATE_OF_OFFER, PosCol.DATE_OF_ENTRY, PosCol.PERIOD_OF_PERFORMANCE_BEGIN,
                        PosCol.PERIOD_OF_PERFORMANCE_END)
        val istSumMap = createIstSumMap()
        for (order in auftragList) {
            if (order?.positionenExcludingDeleted == null) {
                continue
            }
            orderBookDao.calculateInvoicedSum(order)
            for (pos in order.positionenExcludingDeleted) {
                calculateIstSum(istSumMap, startDate, pos)
                if (pos.status != null && auftragsPositionsStatusToShow.contains(pos.status!!)) {
                    val mapping = PropertyMapping()
                    addPosMapping(mapping, order, pos, startDate)
                    sheet.addRow(mapping.mapping, 0)
                }
            }
        }
        fillIstSum(sheet, istSumMap)
        sheet.setAutoFilter()
        return xls.asByteArray
    }

    //Has to be in same order like excel template file headers
    enum class PosCol {
        NUMBER, POS_NUMBER, DATE_OF_OFFER, DATE_OF_ENTRY, DATE_OF_DECISION, HOB_MANAGER, PROJECT, ORDER_TITLE, TITLE, TYPE, PAYMENTTYPE, STATUS_ORDER, STATUS_POS, PERSON_DAYS, NETSUM, INVOICED, TO_BE_INVOICED, COMPLETELY_INVOICED, INVOICES, PERIOD_OF_PERFORMANCE_BEGIN, PERIOD_OF_PERFORMANCE_END, PROBABILITY_OF_OCCURRENCE, CONTACT_PERSON, TASK, COMMENT, PROBABILITY_OF_OCCURRENCE_VALUE, MONTHEND_STARTDATE_ADD1, MONTHEND_ENDDATE_ADD1, MONTHCOUNT, EMPTY, MONTH1, MONTH2, MONTH3, MONTH4, MONTH5, MONTH6, MONTH7, MONTH8, MONTH9, MONTH10, MONTH11, MONTH12
    }

    private fun createIstSumMap(): MutableMap<PosCol, BigDecimal> {
        val istSumMap: MutableMap<PosCol, BigDecimal> = TreeMap()
        for (posCol in monthCols) {
            istSumMap[posCol] = BigDecimal.ZERO
        }
        return istSumMap
    }

    private fun fillIstSum(sheet: ExportSheet, istSumMap: Map<PosCol, BigDecimal>) {
        val istRow = sheet.getRow(6)
        var i = 30
        for (monthCol in istSumMap.keys) {
            istRow.getCell(i).setValue(istSumMap[monthCol])
            i++
        }
    }

    private fun calculateIstSum(istSumMap: MutableMap<PosCol, BigDecimal>, startDate: PFDate, pos: AuftragsPositionDO) {
        val invoicePositions = rechnungCache.getRechnungsPositionVOSetByAuftragsPositionId(pos.id) ?: return
        val beginCurrentMonth = PFDate.now().beginOfMonth
        for (rpo in invoicePositions) {
            val rDate = PFDate.from(rpo.date)
            if (rDate?.isBefore(beginCurrentMonth) == true) {
                val monthCol = getMonthIndex(rDate, startDate)
                if (monthCol in 0..11) {
                    istSumMap.replace(monthCols[monthCol], istSumMap[monthCols[monthCol]]!!.add(rpo.nettoSumme))
                }
            }
        }
    }

    private fun replaceMonthDatesInHeaderRow(startDate: PFDate, row: ExportRow) { // Adding month columns
        val formatter = DateTimeFormatter.ofPattern("MMM yyyy")
        var currentMonth = startDate
        var i = 30
        for (month in monthCols) {
            val cell = row.getCell(i++)
            cell.setValue(currentMonth.format(formatter))
            currentMonth = currentMonth.plusMonths(1)
        }
    }

    private fun addPosMapping(mapping: PropertyMapping, order: AuftragDO, pos: AuftragsPositionDO, startDate: PFDate) {
        mapping.add(PosCol.NUMBER, order.nummer)
        mapping.add(PosCol.POS_NUMBER, "#" + pos.number)
        mapping.add(PosCol.DATE_OF_OFFER, order.angebotsDatum)
        mapping.add(PosCol.DATE_OF_ENTRY, order.erfassungsDatum)
        mapping.add(PosCol.DATE_OF_DECISION, ensureErfassungsDatum(order))
        mapping.add(PosCol.HOB_MANAGER, if (order.headOfBusinessManager != null) order.headOfBusinessManager!!.getFullname() else "")
        mapping.add(PosCol.PROJECT, order.projektAsString)
        mapping.add(PosCol.ORDER_TITLE, order.titel)
        mapping.add(PosCol.TITLE, pos.titel)
        mapping.add(PosCol.TYPE, if (pos.art != null) ThreadLocalUserContext.getLocalizedString(pos.art!!.i18nKey) else "")
        mapping.add(PosCol.PAYMENTTYPE, if (pos.paymentType != null) ThreadLocalUserContext.getLocalizedString(pos.paymentType!!.i18nKey) else "")
        mapping.add(PosCol.STATUS_ORDER, if (order.auftragsStatus != null) ThreadLocalUserContext.getLocalizedString(order.auftragsStatus!!.i18nKey) else "")
        mapping.add(PosCol.STATUS_POS, if (pos.status != null) ThreadLocalUserContext.getLocalizedString(pos.status!!.i18nKey) else "")
        mapping.add(PosCol.PERSON_DAYS, pos.personDays)
        val netSum = if (pos.nettoSumme != null) pos.nettoSumme else BigDecimal.ZERO
        val invoicedSum = if (pos.fakturiertSum != null) pos.fakturiertSum else BigDecimal.ZERO
        val toBeInvoicedSum = netSum!!.subtract(invoicedSum)
        mapping.add(PosCol.NETSUM, netSum)
        addCurrency(mapping, PosCol.INVOICED, invoicedSum)
        addCurrency(mapping, PosCol.TO_BE_INVOICED, toBeInvoicedSum)
        mapping.add(PosCol.COMPLETELY_INVOICED, if (pos.vollstaendigFakturiert!!) "x" else "")
        val invoicePositions = rechnungCache.getRechnungsPositionVOSetByAuftragsPositionId(pos.id)
        mapping.add(PosCol.INVOICES, getInvoices(invoicePositions))
        if (PeriodOfPerformanceType.OWN == pos.periodOfPerformanceType) { // use "own" period -> from pos
            mapping.add(PosCol.PERIOD_OF_PERFORMANCE_BEGIN, pos.periodOfPerformanceBegin)
            mapping.add(PosCol.PERIOD_OF_PERFORMANCE_END, pos.periodOfPerformanceEnd)
        } else { // use "see above" period -> from order
            mapping.add(PosCol.PERIOD_OF_PERFORMANCE_BEGIN, order.periodOfPerformanceBegin)
            mapping.add(PosCol.PERIOD_OF_PERFORMANCE_END, order.periodOfPerformanceEnd)
        }
        val probability = getProbabilityOfAccurence(order, pos)
        mapping.add(PosCol.PROBABILITY_OF_OCCURRENCE, probability.multiply(BigDecimal(100)))
        //    mapping.add(PosCol.PROBABILITY_OF_OCCURRENCE, order.getProbabilityOfOccurrence())
        mapping.add(PosCol.CONTACT_PERSON, if (order.contactPerson != null) order.contactPerson!!.getFullname() else "")
        val node = TenantRegistryMap.getInstance().tenantRegistry.taskTree.getTaskNodeById(pos.taskId)
        mapping.add(PosCol.TASK, if (node != null && node.task != null) node.task.title else "")
        mapping.add(PosCol.COMMENT, pos.bemerkung)
        val accurenceValue = computeAccurenceValue(order, pos)
        mapping.add(PosCol.PROBABILITY_OF_OCCURRENCE_VALUE, accurenceValue)

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        mapping.add(PosCol.MONTHEND_STARTDATE_ADD1, getStartLeistungszeitraumNextMonthEnd(order, pos).format(formatter))
        mapping.add(PosCol.MONTHEND_ENDDATE_ADD1, getEndLeistungszeitraumNextMonthEnd(order, pos).format(formatter))
        mapping.add(PosCol.MONTHCOUNT, getMonthCountForOrderPosition(order, pos))
        // get payment schedule for order position
        val paymentSchedules = getPaymentSchedule(order, pos)
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
            fillByPaymentSchedule(paymentSchedules, mapping, order, pos, startDate)
            sumPaymentSchedule = sum
            beginDistribute = beginDistribute.plusMonths(2) // values are added to the next month (+1), start the month after the last one (+1)
        } else {
            sumPaymentSchedule = BigDecimal.ZERO
            beginDistribute = getStartLeistungszeitraumNextMonthEnd(order, pos)
        }
        // compute diff, return if diff is empty
        val diff = accurenceValue.subtract(sumPaymentSchedule)
        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            return
        }
        // handle diff
        if (pos.paymentType != null) {
            when (pos.paymentType) {
                AuftragsPositionsPaymentType.TIME_AND_MATERIALS -> fillMonthColumnsDistributed(diff, mapping, order, pos, startDate, beginDistribute)
                AuftragsPositionsPaymentType.PAUSCHALE -> if (order.probabilityOfOccurrence != null) {
                    fillMonthColumnsDistributed(diff, mapping, order, pos, startDate, beginDistribute)
                }
                AuftragsPositionsPaymentType.FESTPREISPAKET ->  // fill reset at end of project time
                    addEndAtPeriodOfPerformance(diff, mapping, order, pos, startDate)
            }
        }
    }

    private fun fillByPaymentSchedule(paymentSchedules: List<PaymentScheduleDO>, mapping: PropertyMapping,
                                      order: AuftragDO?, pos: AuftragsPositionDO, startDate: PFDate) { // payment values
        val probability = getProbabilityOfAccurence(order, pos)
        var currentMonth = startDate.plusMonths(-1).beginOfMonth
        for (monthCol in monthCols) {
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
        }
    }

    private fun addEndAtPeriodOfPerformance(sum: BigDecimal, mapping: PropertyMapping,
                                            order: AuftragDO?, pos: AuftragsPositionDO, startDate: PFDate) {
        val posEndDate = getEndLeistungszeitraumNextMonthEnd(order, pos)
        val index = getMonthIndex(posEndDate, startDate)
        if (index < 0 || index > 11) {
            return
        }
        // handle payment difference
        val previousValue = mapping.mapping[monthCols[index].name]
        if (previousValue == null && checkAfterMonthBefore(posEndDate)) {
            mapping.add(monthCols[index], sum)
        } else {
            if (checkAfterMonthBefore(posEndDate)) {
                mapping.add(monthCols[index], sum.add(previousValue as BigDecimal?))
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

    private fun getPaymentSchedule(order: AuftragDO?, pos: AuftragsPositionDO): List<PaymentScheduleDO> {
        val schedules = order!!.paymentSchedules ?: return emptyList()
        return schedules.stream()
                .filter { schedule: PaymentScheduleDO -> schedule.positionNumber != null && schedule.scheduleDate != null && schedule.amount != null }
                .filter { schedule: PaymentScheduleDO -> schedule.positionNumber!!.toInt() == pos.number.toInt() }
                .collect(Collectors.toList())
    }

    private fun fillMonthColumnsDistributed(value: BigDecimal, mapping: PropertyMapping, order: AuftragDO?, pos: AuftragsPositionDO,
                                            startDate: PFDate, beginDistribute: PFDate) {
        var indexBegin = getMonthIndex(beginDistribute, startDate)
        var indexEnd = getMonthIndex(getEndLeistungszeitraumNextMonthEnd(order, pos), startDate)
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
            mapping.add(monthCols[i], partlyNettoSum)
        }
    }

    private fun computeAccurenceValue(order: AuftragDO?, pos: AuftragsPositionDO): BigDecimal {
        val netSum = if (pos.nettoSumme != null) pos.nettoSumme else BigDecimal.ZERO
        val invoicedSum = if (pos.fakturiertSum != null) pos.fakturiertSum else BigDecimal.ZERO
        val toBeInvoicedSum = netSum!!.subtract(invoicedSum)
        val probability = getProbabilityOfAccurence(order, pos)
        return toBeInvoicedSum.multiply(probability)
    }

    private fun getProbabilityOfAccurence(order: AuftragDO?, pos: AuftragsPositionDO): BigDecimal {
        if (pos.status == AuftragsPositionsStatus.BEAUFTRAGT) {
            return BigDecimal.ONE
        }
        return if (order!!.probabilityOfOccurrence != null) {
            BigDecimal.valueOf(order.probabilityOfOccurrence!! / 100.toLong())
        } else when (pos.status) {
            AuftragsPositionsStatus.GELEGT -> BigDecimal.valueOf(0.5)
            AuftragsPositionsStatus.LOI -> BigDecimal.valueOf(0.9)
            AuftragsPositionsStatus.BEAUFTRAGT, AuftragsPositionsStatus.ABGESCHLOSSEN -> BigDecimal.ONE
            else -> BigDecimal.ZERO
        }
    }

    private fun getStartLeistungszeitraumNextMonthEnd(order: AuftragDO?, pos: AuftragsPositionDO): PFDate {
        var result = PFDate.now()
        if (PeriodOfPerformanceType.OWN == pos.periodOfPerformanceType) {
            if (pos.periodOfPerformanceBegin != null) {
                result = PFDate.from(pos.periodOfPerformanceBegin)!!.plusMonths(1).endOfMonth
            }
        } else if (order!!.periodOfPerformanceBegin != null) {
            result = PFDate.from(order.periodOfPerformanceBegin)!!.plusMonths(1).endOfMonth
        }
        return result
    }

    private fun getEndLeistungszeitraumNextMonthEnd(order: AuftragDO?, pos: AuftragsPositionDO): PFDate {
        var result = PFDate.now()
        if (PeriodOfPerformanceType.OWN == pos.periodOfPerformanceType) {
            if (pos.periodOfPerformanceEnd != null) {
                result = PFDate.from(pos.periodOfPerformanceEnd)!!.plusMonths(1).endOfMonth
            }
        } else {
            if (order!!.periodOfPerformanceEnd != null) {
                result = PFDate.from(order.periodOfPerformanceEnd)!!.plusMonths(1).endOfMonth
            }
        }
        return result
    }

    private fun getMonthCountForOrderPosition(order: AuftragDO?, pos: AuftragsPositionDO): BigDecimal? {
        if (PeriodOfPerformanceType.OWN == pos.periodOfPerformanceType) {
            if (pos.periodOfPerformanceEnd != null && pos.periodOfPerformanceBegin != null) {
                return getMonthCount(pos.periodOfPerformanceBegin!!, pos.periodOfPerformanceEnd!!)
            }
        } else {
            if (order!!.periodOfPerformanceEnd != null && order.periodOfPerformanceBegin != null) {
                return getMonthCount(order.periodOfPerformanceBegin!!, order.periodOfPerformanceEnd!!)
            }
        }
        return null
    }

    private fun getMonthCount(start: Date, end: Date): BigDecimal {
        val startDate = PFDate.from(start)!!
        val endDate = PFDate.from(end)!!
        val diffYear = endDate.year - startDate.year
        val diffMonth = diffYear * 12 + endDate.monthValue - startDate.monthValue + 1
        return BigDecimal.valueOf(diffMonth.toLong())
    }

    private fun addCurrency(mapping: PropertyMapping, col: Enum<*>, value: BigDecimal?) {
        if (NumberHelper.isNotZero(value)) {
            mapping.add(col, value)
        } else {
            mapping.add(col, "")
        }
    }

    private fun getInvoices(invoicePositions: Set<RechnungsPositionVO>?): String {
        return invoicePositions?.joinToString(", ") { it.rechnungNummer?.toString() ?: "" } ?: ""
    }

    private fun ensureErfassungsDatum(order: AuftragDO): Date? {
        if (order.erfassungsDatum != null)
            return order.erfassungsDatum
        if (order.created != null)
            return order.created
        if (order.angebotsDatum != null)
            return order.angebotsDatum
        return PFDate.now().sqlDate
    }

    companion object {
        private val log = LoggerFactory.getLogger(ForecastExport::class.java)
    }
}
