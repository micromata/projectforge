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

import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.time.PFDay
import org.projectforge.test.AbstractTestBase
import org.projectforge.test.WorkFileHelper
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.math.RoundingMode

class ForecastExportTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var forecastExport: ForecastExport

    @Test
    fun exportTest() {
        logon(TEST_FINANCE_USER)
        val today = PFDay.now()
        val baseDate = today.plusMonths(-4)
        val order1 = createTimeAndMaterials(AuftragsStatus.BEAUFTRAGT, AuftragsPositionsStatus.BEAUFTRAGT,
                1000.0, baseDate,
                baseDate.plusMonths(1), baseDate.plusMonths(4),
                baseDate.plusMonths(2), baseDate.plusMonths(3), baseDate.plusMonths(4))
        //order1.addPaymentSchedule()

        createTimeAndMaterials(AuftragsStatus.BEAUFTRAGT, AuftragsPositionsStatus.BEAUFTRAGT,
                1000.0, baseDate,
                baseDate.plusMonths(1), baseDate.plusMonths(4),
                baseDate.plusMonths(2), baseDate.plusMonths(3))

        val order3 = createOrder(today, AuftragsStatus.IN_ERSTELLUNG,
                today.plusMonths(1), today.plusMonths(5))
        //order1.addPaymentSchedule()
        addPosition(order3, 1, AuftragsPositionsStatus.IN_ERSTELLUNG, 4000.00, AuftragsPositionsPaymentType.FESTPREISPAKET)
        auftragDao.save(order3)

        val filter = AuftragFilter()
        filter.periodOfPerformanceStartDate = baseDate.localDate
        val ba = forecastExport.export(filter)
        val excelFile = WorkFileHelper.getWorkFile("forecast.xlsx")
        baseLog.info("Writing forecast Excel file to work directory: " + excelFile.absolutePath)
        FileUtils.writeByteArrayToFile(excelFile, ba)

        ExcelWorkbook(ByteArrayInputStream(ba), excelFile.name).use { workbook ->
            val forecastSheet = workbook.getSheet(ForecastExport.Sheet.FORECAST.title)!!
            val monthCols = Array(12) {
                forecastSheet.registerColumn(ForecastExport.formatMonthHeader(baseDate.plusMonths(it.toLong())))
            }
            val firstRow = 9
            forecastSheet.headRow // Enforce analyzing the column definitions.

            // order 1
            Assertions.assertTrue(forecastSheet.getCell(firstRow + 1, monthCols[3])!!.stringCellValue.isNullOrBlank())
            val amount = forecastSheet.getCell(firstRow + 1, monthCols[4])!!.numericCellValue
            assertAmount(order1.getPosition(1)!!.nettoSumme!!.divide(BigDecimal(4)), amount)
        }
    }

    private fun createTimeAndMaterials(orderStatus: AuftragsStatus, posStatus: AuftragsPositionsStatus,
                                       monthlyAmount: Double, date: PFDay, periodStart: PFDay, periodEnd: PFDay, vararg invoiceMonth: PFDay)
            : AuftragDO {
        var order = createOrder(date, orderStatus, periodStart, periodEnd)
        //order1.addPaymentSchedule()
        addPosition(order, 1, posStatus, monthlyAmount * (1 + periodStart.monthsBetween(periodEnd)), AuftragsPositionsPaymentType.TIME_AND_MATERIALS)
        val id = auftragDao.save(order)
        order = auftragDao.getById(id)
        val pos1_1 = order.getPosition(1)!!
        invoiceMonth.forEach {
            val invoice1 = createInvoice(it)
            addPosition(invoice1, monthlyAmount, pos1_1)
            rechnungDao.save(invoice1)
        }
        return order
    }

    private fun createOrder(date: PFDay,
                            status: AuftragsStatus,
                            periodOfPerformanceBegin: PFDay? = null,
                            periodOfPerformanceEnd: PFDay? = null,
                            probability: Int? = null): AuftragDO {
        val order = AuftragDO()
        order.nummer = auftragDao.nextNumber
        order.auftragsStatus = status
        order.angebotsDatum = date.localDate
        order.periodOfPerformanceBegin = periodOfPerformanceBegin?.localDate
        order.periodOfPerformanceEnd = periodOfPerformanceEnd?.localDate
        order.probabilityOfOccurrence = probability
        return order
    }

    private fun addPosition(order: AuftragDO,
                            number: Short,
                            status: AuftragsPositionsStatus,
                            netSum: Double,
                            paymentType: AuftragsPositionsPaymentType,
                            periodOfPerformanceBegin: PFDay? = null,
                            periodOfPerformanceEnd: PFDay? = null,
                            periodOfPerformanceType: PeriodOfPerformanceType? = null): AuftragsPositionDO {
        val pos = AuftragsPositionDO()
        pos.number = number
        pos.status = status
        pos.paymentType = paymentType
        pos.nettoSumme = BigDecimal(netSum)
        pos.periodOfPerformanceBegin = periodOfPerformanceBegin?.localDate
        pos.periodOfPerformanceEnd = periodOfPerformanceEnd?.localDate
        pos.periodOfPerformanceType = periodOfPerformanceType
        order.addPosition(pos)
        return pos
    }

    private fun createInvoice(date: PFDay): RechnungDO {
        val invoice = RechnungDO()
        invoice.nummer = rechnungDao.nextNumber
        invoice.datum = date.localDate
        invoice.faelligkeit = date.plusDays(30).localDate
        invoice.status = RechnungStatus.GESTELLT
        invoice.typ = RechnungTyp.RECHNUNG
        invoice.kundeText = "ACME Inc."
        return invoice
    }

    private fun addPosition(invoice: RechnungDO, netSum: Double, orderPos: AuftragsPositionDO?): RechnungsPositionDO {
        val pos = RechnungsPositionDO()
        pos.auftragsPosition = orderPos
        pos.einzelNetto = BigDecimal(netSum)
        invoice.addPosition(pos)
        return pos
    }

    private fun assertAmount(v1: BigDecimal, v2: Double) {
        Assertions.assertEquals(v1.setScale(2, RoundingMode.HALF_UP), BigDecimal(v2).setScale(2, RoundingMode.HALF_UP))
    }
}
