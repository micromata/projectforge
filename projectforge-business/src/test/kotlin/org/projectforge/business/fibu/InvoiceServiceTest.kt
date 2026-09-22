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

package org.projectforge.business.fibu

import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.PFDay.Companion.today
import org.projectforge.business.test.AbstractTestBase
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
    Assertions.assertEquals(
      "12345_Kunde_Koenig_Projekt_webapp_abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc....docx",
      filename, "Assertions.equals is dependent from property projectforge.domain!"
    )
  }

  @Test
  fun extractSharedVatTest() {
    Assertions.assertNull(invoiceService.extractSharedVat(createInvoice(null)))
    Assertions.assertNull(invoiceService.extractSharedVat(createInvoice(null, null)))
    Assertions.assertNull(invoiceService.extractSharedVat(createInvoice(BigDecimal.TEN, null, BigDecimal.TEN)))
    Assertions.assertNull(
      invoiceService.extractSharedVat(
        createInvoice(
          BigDecimal.TEN,
          BigDecimal.ONE,
          BigDecimal.TEN
        )
      )
    )
    Assertions.assertEquals(BigDecimal.TEN, invoiceService.extractSharedVat(createInvoice(BigDecimal.TEN)))
    Assertions.assertEquals(
      BigDecimal.TEN,
      invoiceService.extractSharedVat(createInvoice(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN))
    )
  }

  /**
   * A deleted position doesn't appear in the document, so its VAT must not decide the rate the document prints.
   */
  @Test
  fun extractSharedVatIgnoresDeletedPositionsTest() {
    val invoice = createInvoice(BigDecimal.TEN, null)
    invoice.positionen!![1].deleted = true
    Assertions.assertEquals(BigDecimal.TEN, invoiceService.extractSharedVat(invoice))
  }

  /**
   * The document of an invoice with a deleted position: it used to throw instead, because
   * [RechnungCalculator] skips a deleted position and so never fills the `info` whose net sum the position
   * row reads.
   */
  @Test
  fun invoiceWordDocumentWithDeletedPositionTest() {
    val invoice = RechnungDO()
    invoice.nummer = 12345
    invoice.datum = LocalDate.of(2024, Month.JUNE, 15)
    invoice.typ = RechnungTyp.RECHNUNG
    invoice.addPosition(RechnungsPositionDO().also { pos ->
      pos.text = "Softwareentwicklung"
      pos.menge = BigDecimal.TEN
      pos.einzelNetto = BigDecimal("150.00")
      pos.vat = BigDecimal("0.19")
    })
    invoice.addPosition(RechnungsPositionDO().also { pos ->
      pos.text = "Gelöschte Position"
      pos.menge = BigDecimal.ONE
      pos.einzelNetto = BigDecimal("100.00")
      pos.vat = BigDecimal("0.19")
      pos.deleted = true
    })
    RechnungCalculator.calculate(invoice, useCaches = false)
    val document = invoiceService.getInvoiceWordDocument(invoice, null)
    Assertions.assertNotNull(document, "The document is created, deleted position or not.")
    Assertions.assertTrue(document!!.size() > 0)
  }

  /**
   * The discount text of the document: the template states it inside `{if isSkonto=true}`, and whether that
   * condition holds must follow from the invoice alone — not from whether the caller happened to call
   * [AbstractRechnungDO.recalculate] first, which fills the transient payment terms in days. Wicket's edit page
   * does (so its export always showed the text), the REST export and the ZUGFeRD conversion don't.
   */
  @Test
  fun invoiceWordDocumentStatesTheDiscountTest() {
    val invoice = discountInvoice()
    Assertions.assertNull(
      invoice.discountZahlungsZielInTagen,
      "No recalculate() here - exactly the state an invoice loaded from the database is in.",
    )
    RechnungCalculator.calculate(invoice, useCaches = false)
    val text = documentText(invoiceService.getInvoiceWordDocument(invoice, null))
    // The dates as [DateTimeFormatter] renders them in the locale of the test user, not as a literal: what is
    // asserted here is that percentage and maturity reach the document at all.
    val maturity = DateTimeFormatter.instance().getFormattedDate(invoice.discountMaturity)
    Assertions.assertTrue(
      text.contains("abzüglich 3% Skonto bis zum $maturity"),
      "The discount sentence of the template, with percentage and maturity: $text",
    )
    Assertions.assertFalse(
      text.contains("isSkonto"),
      "The condition itself is resolved and gone from the document: $text",
    )
  }

  /**
   * Without a discount the template states its other sentence, i.e. `isSkonto=false` reaches Merlin as well.
   */
  @Test
  fun invoiceWordDocumentWithoutDiscountTest() {
    val invoice = discountInvoice().also {
      it.discountPercent = null
      it.discountMaturity = null
    }
    RechnungCalculator.calculate(invoice, useCaches = false)
    val text = documentText(invoiceService.getInvoiceWordDocument(invoice, null))
    val dueDate = DateTimeFormatter.instance().getFormattedDate(invoice.faelligkeit)
    Assertions.assertFalse(text.contains("Skonto"), "No discount, no discount text: $text")
    Assertions.assertTrue(text.contains("Überweisung des Gesamtbetrages bis zum $dueDate"), text)
  }

  private fun discountInvoice(): RechnungDO {
    return RechnungDO().also { invoice ->
      invoice.nummer = 12345
      invoice.typ = RechnungTyp.RECHNUNG
      invoice.datum = LocalDate.of(2024, Month.JUNE, 15)
      invoice.faelligkeit = LocalDate.of(2024, Month.JULY, 15)
      invoice.discountPercent = BigDecimal("3.00")
      invoice.discountMaturity = LocalDate.of(2024, Month.JUNE, 22)
      invoice.addPosition(RechnungsPositionDO().also { pos ->
        pos.text = "Softwareentwicklung"
        pos.menge = BigDecimal.TEN
        pos.einzelNetto = BigDecimal("150.00")
        pos.vat = BigDecimal("0.19")
      })
    }
  }

  private fun documentText(document: org.apache.commons.io.output.ByteArrayOutputStream?): String {
    Assertions.assertNotNull(document, "The document is created.")
    java.io.ByteArrayInputStream(document!!.toByteArray()).use { istream ->
      XWPFDocument(istream).use { word ->
        return XWPFWordExtractor(word).use { it.text }
      }
    }
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

  @Test
  fun templateVariantsTest() {
    invoiceService.getTemplateVariants(arrayOf("test.docx", "test_Englisch.docx", "test_Deutsch.docx", "test_Commerzbank_Deutsch.docx"), "test").let {
      Assertions.assertEquals(4, it.size)
      Assertions.assertEquals("", it[0])
      Assertions.assertEquals("Commerzbank_Deutsch", it[1])
      Assertions.assertEquals("Deutsch", it[2])
      Assertions.assertEquals("Englisch", it[3])
    }
    invoiceService.getTemplateVariants(arrayOf("test.docx"), "test").let {
      Assertions.assertEquals(1, it.size)
      Assertions.assertEquals("", it[0])
    }
  }
}
