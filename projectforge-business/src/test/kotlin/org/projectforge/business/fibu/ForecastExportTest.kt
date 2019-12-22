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

import de.micromata.merlin.excel.ExcelWorkbook
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import org.projectforge.framework.time.PFDate
import org.projectforge.test.AbstractTestBase
import org.projectforge.test.WorkFileHelper
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayInputStream
import java.math.BigDecimal

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
        val today = PFDate.now()
        var order1 = createOrder(today.plusMonths(-3), AuftragsStatus.BEAUFTRAGT,
                today.plusMonths(-2), today.plusMonths(1))
        //order1.addPaymentSchedule()
        addPosition(order1, 1, AuftragsPositionsStatus.BEAUFTRAGT, 4000.00)
        val id = auftragDao.save(order1)
        order1 = auftragDao.getById(id)
        val pos1_1 = order1.getPosition(1)!!

        val invoice1 = createInvoice(today.plusMonths(-1))
        addPosition(invoice1, 1000.00, pos1_1)
        rechnungDao.save(invoice1)

        val invoice2 = createInvoice(today)
        addPosition(invoice2, 1000.00, pos1_1)
        rechnungDao.save(invoice2)

        val order2 = createOrder(today, AuftragsStatus.IN_ERSTELLUNG,
                today.plusMonths(1), today.plusMonths(5))
        //order1.addPaymentSchedule()
        val pos2_1 = addPosition(order2, 1, AuftragsPositionsStatus.IN_ERSTELLUNG, 4000.00)
        auftragDao.save(order2)

        val filter = AuftragFilter()
        filter.periodOfPerformanceStartDate = today.plusMonths(-4).sqlDate
        val ba = forecastExport.export(filter)
        val excelFile = WorkFileHelper.getWorkFile("forecast.xlsx")
        log.info("Writing forecast Excel file to work directory: " + excelFile.absolutePath)
        FileUtils.writeByteArrayToFile(excelFile, ba)

        val workbook = ExcelWorkbook(ByteArrayInputStream(ba), excelFile.name)
        workbook.close()
    }

    private fun createOrder(date: PFDate,
                            status: AuftragsStatus,
                            periodOfPerformanceBegin: PFDate? = null,
                            periodOfPerformanceEnd: PFDate? = null,
                            probability: Int? = null): AuftragDO {
        val order = AuftragDO()
        order.nummer = auftragDao.nextNumber
        order.auftragsStatus = status
        order.angebotsDatum = date.sqlDate
        order.periodOfPerformanceBegin = periodOfPerformanceBegin?.sqlDate
        order.periodOfPerformanceEnd = periodOfPerformanceEnd?.sqlDate
        order.probabilityOfOccurrence = probability
        return order
    }

    private fun addPosition(order: AuftragDO,
                            number: Short,
                            status: AuftragsPositionsStatus,
                            netSum: Double,
                            periodOfPerformanceBegin: PFDate? = null,
                            periodOfPerformanceEnd: PFDate? = null,
                            periodOfPerformanceType: PeriodOfPerformanceType? = null): AuftragsPositionDO {
        val pos = AuftragsPositionDO()
        pos.number = number
        pos.status = status
        pos.nettoSumme = BigDecimal(netSum)
        pos.periodOfPerformanceBegin = periodOfPerformanceBegin?.sqlDate
        pos.periodOfPerformanceEnd = periodOfPerformanceEnd?.sqlDate
        pos.periodOfPerformanceType = periodOfPerformanceType
        order.addPosition(pos)
        return pos
    }

    private fun createInvoice(date: PFDate): RechnungDO {
        val invoice = RechnungDO()
        invoice.nummer = rechnungDao.nextNumber
        invoice.datum = date.sqlDate
        invoice.faelligkeit = date.plusDays(30).sqlDate
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
}
