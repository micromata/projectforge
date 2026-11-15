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

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The Excel export of the outgoing invoices ([OutgoingInvoiceEntityRest.exportAsExcel]): besides number, customer,
 * dates and sums the fields of the invoice the Wicket export had, above all the period of performance.
 */
class OutgoingInvoiceExcelExportTest : AbstractTestBase() {
    @Autowired
    private lateinit var outgoingInvoiceEntityRest: OutgoingInvoiceEntityRest

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun `the export has the period of performance and the other fields of the invoice`() {
        logon(TEST_FINANCE_USER)
        RechnungDO().also { invoice ->
            invoice.addPosition(RechnungsPositionDO().also {
                it.einzelNetto = BigDecimal("100")
                it.text = "Position"
            })
            invoice.nummer = rechnungDao.getNextNumber(invoice)
            invoice.kundeText = "Invoice Excel export customer"
            invoice.betreff = SUBJECT
            invoice.datum = LocalDate.of(2026, 10, 1)
            invoice.faelligkeit = LocalDate.of(2026, 10, 31)
            invoice.status = RechnungStatus.GESTELLT
            invoice.typ = RechnungTyp.RECHNUNG
            invoice.periodOfPerformanceBegin = LocalDate.of(2026, 9, 1)
            invoice.periodOfPerformanceEnd = LocalDate.of(2026, 9, 30)
            invoice.customerref1 = "PO-4711"
            invoice.besonderheiten = "Special"
            invoice.discountPercent = BigDecimal("2")
            invoice.discountMaturity = LocalDate.of(2026, 10, 10)
            invoice.customerAddress = "Street 1"
            invoice.customerZipCode = "34117"
            invoice.customerCity = "Kassel"
            invoice.customerLeitwegId = "991-12345-67"
            rechnungDao.insert(invoice, checkAccess = false)
        }

        val response = outgoingInvoiceEntityRest.exportAsExcel(MagicFilter())

        assertEquals(HttpStatus.OK, response.statusCode)
        val bytes = (response.body as Resource).inputStream.readBytes()
        val sheet = WorkbookFactory.create(bytes.inputStream()).getSheetAt(0)
        val headers = sheet.getRow(0).map { it.stringCellValue }
        val row = rowOf(sheet, headers)
        fun cell(i18nKey: String) =
            row.getCell(headers.indexOf(translate(i18nKey)).also { assertTrue(it >= 0, "No column $i18nKey.") })
        assertEquals(LocalDate.of(2026, 9, 1), cell("fibu.periodOfPerformance.from").localDateTimeCellValue.toLocalDate())
        assertEquals(LocalDate.of(2026, 9, 30), cell("fibu.periodOfPerformance.to").localDateTimeCellValue.toLocalDate())
        assertEquals(translate(RechnungTyp.RECHNUNG.i18nKey), cell("fibu.rechnung.typ").stringCellValue)
        assertEquals("PO-4711", cell("fibu.customerref1").stringCellValue)
        assertEquals("Special", cell("fibu.rechnung.besonderheiten").stringCellValue)
        // Transient, calculated from date and due date.
        assertEquals(30.0, cell("fibu.rechnung.zahlungsZiel").numericCellValue)
        // Transient, calculated from the dates: 2026-10-01 to 2026-10-10.
        assertEquals(9.0, cell("fibu.rechnung.discountZahlungsZiel").numericCellValue)
        assertEquals(2.0, cell("fibu.rechnung.discountPercent").numericCellValue)
        assertEquals(LocalDate.of(2026, 10, 10), cell("fibu.rechnung.discountMaturity").localDateTimeCellValue.toLocalDate())
        assertEquals("Street 1", cell("fibu.konto.street").stringCellValue)
        assertEquals("Kassel", cell("fibu.konto.city").stringCellValue)
        assertEquals("991-12345-67", cell("fibu.konto.leitwegId").stringCellValue)
        assertEquals(100.0, cell("fibu.common.netto").numericCellValue)
    }

    private fun rowOf(sheet: Sheet, headers: List<String>): Row {
        val subjectColumn = headers.indexOf(translate("fibu.rechnung.betreff"))
        return (1..sheet.lastRowNum).mapNotNull { sheet.getRow(it) }
            .firstOrNull { it.getCell(subjectColumn)?.stringCellValue == SUBJECT }
            .also { assertNotNull(it, "No row for the invoice in the export.") }!!
    }

    companion object {
        private const val SUBJECT = "Invoice Excel export"
    }
}
