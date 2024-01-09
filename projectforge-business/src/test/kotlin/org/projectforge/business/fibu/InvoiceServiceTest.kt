/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.time.PFDay.Companion.today
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class InvoiceServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var invoiceService: InvoiceService

    @Test
    fun invoiceFilenameEmptyTest() {
        val data = RechnungDO()
        val filename = invoiceService.getInvoiceFilename(data)
        Assertions.assertNotNull(filename)
        Assertions.assertTrue(filename.length < 256)
        Assertions.assertEquals("_" + today().isoString + ".docx", filename)
    }

    @Test
    fun invoiceFilenameStandardTest() {
        val data = RechnungDO()
        data.nummer = 12345
        val kunde = KundeDO()
        kunde.name = "Kunde"
        data.kunde = kunde
        val projekt = ProjektDO()
        projekt.name = "Projekt"
        data.projekt = projekt
        data.betreff = "Betreff"
        val date = LocalDate.of(2017, Month.AUGUST, 4)
        data.datum = date
        val filename = invoiceService.getInvoiceFilename(data)
        Assertions.assertNotNull(filename)
        Assertions.assertTrue(filename.length < 256)
        Assertions.assertEquals("12345_Kunde_Projekt_Betreff_2017-08-04.docx", filename)
    }

    @Test
    fun invoiceFilenameSpecialCharacterTest() {
        val data = RechnungDO()
        data.nummer = 12345
        val kunde = KundeDO()
        kunde.name = "Kunde & Kunde"
        data.kunde = kunde
        val projekt = ProjektDO()
        projekt.name = "Projekt-Titel"
        data.projekt = projekt
        data.betreff = "Betreff/Änderung?"
        val date = LocalDate.of(2017, Month.AUGUST, 4)
        data.datum = date
        logon(TEST_USER)
        val filename = invoiceService.getInvoiceFilename(data)
        Assertions.assertNotNull(filename)
        Assertions.assertTrue(filename.length < 256)
        Assertions.assertEquals("12345_Kunde_Kunde_Projekt-Titel_Betreff_Aenderung_2017-08-04.docx", filename)
    }

    @Test
    fun invoiceFilenameTooLongTest() {
        val data = RechnungDO()
        data.nummer = 12345
        val kunde = KundeDO()
        kunde.name = "Kunde König"
        data.kunde = kunde
        val projekt = ProjektDO()
        projekt.name = "Projekt: $§webapp"
        data.projekt = projekt
        val character = "abc"
        for (i in 1..84) {
            data.betreff = (if (data.betreff != null) data.betreff else "") + character
        }
        val filename = invoiceService.getInvoiceFilename(data)
        Assertions.assertNotNull(filename)
        Assertions.assertTrue(filename.length < 256)
        Assertions.assertEquals("12345_Kunde_Koenig_Projekt_webapp_abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc....docx",
                filename, "Assertions.equals is dependent from property projectforge.domain!")
    }

    @Test
    fun extractSharedVatTest() {
        Assertions.assertNull(invoiceService.extractSharedVat(createInvoice(null)))
        Assertions.assertNull(invoiceService.extractSharedVat(createInvoice(null, null)))
        Assertions.assertNull(invoiceService.extractSharedVat(createInvoice(BigDecimal.TEN, null, BigDecimal.TEN)))
        Assertions.assertNull(invoiceService.extractSharedVat(createInvoice(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN)))
        Assertions.assertEquals(BigDecimal.TEN, invoiceService.extractSharedVat(createInvoice(BigDecimal.TEN)))
        Assertions.assertEquals(BigDecimal.TEN, invoiceService.extractSharedVat(createInvoice(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN)))
    }

    private fun createInvoice(vararg vats: BigDecimal?): RechnungDO {
        val invoice = RechnungDO()
        vats.forEach { vat ->
            val pos = RechnungsPositionDO()
            pos.vat = vat
            invoice.addPosition(pos)
        }
        return invoice
    }
}
