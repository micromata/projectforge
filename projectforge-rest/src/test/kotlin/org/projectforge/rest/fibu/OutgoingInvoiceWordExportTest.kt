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
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The Word export of one invoice — [OutgoingInvoiceEntityRest.exportInvoiceWord], where Wicket has a menu
 * entry per template variant (`RechnungEditPage.addExportMenu`).
 *
 * What is worth testing is not the document — that is `InvoiceService`'s and Merlin's business — but the three
 * decisions of the endpoint: it exports the *stored* invoice by id and therefore has to answer 404 for an id
 * that isn't one (the reason `RechnungDao.find` had to be made null safe), it checks the read access of that
 * invoice rather than only the category right, and it fills `info` before handing the invoice over, since
 * `InvoiceService` reads the sums from there and nothing on the load path computes them.
 *
 * A test installation configures no custom template, so what runs here is the packaged `InvoiceTemplate.docx`
 * and the single unnamed variant — which is also the case that used to throw instead of exporting.
 */
class OutgoingInvoiceWordExportTest : AbstractTestBase() {

    @Autowired
    private lateinit var outgoingInvoiceEntityRest: OutgoingInvoiceEntityRest

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun `the stored invoice is answered as a docx of the configured template`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("Word export")

        val response = outgoingInvoiceEntityRest.exportInvoiceWord(id, variant = null)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body as Resource
        // A real document rather than an empty stream: a .docx is a zip, so the first bytes are its signature -
        // which is what distinguishes "the template was processed" from "a file was returned".
        val bytes = body.inputStream.readBytes()
        assertTrue(bytes.size > 1000, "A processed template, not an empty document: ${bytes.size} bytes.")
        assertEquals(0x50, bytes[0].toInt(), "PK - the zip signature every .docx starts with.")
        assertEquals(0x4B, bytes[1].toInt())
        // Named by `InvoiceService.getInvoiceFilename`, which is what the browser saves it as; the header is
        // URL encoded, so the assertion is on the suffix alone.
        val disposition = response.headers[HttpHeaders.CONTENT_DISPOSITION]?.first()
        assertNotNull(disposition)
        assertTrue(disposition!!.endsWith(".docx"), "The download is named as a Word document: $disposition")
    }

    @Test
    fun `an unknown invoice is answered with 404 rather than an empty document`() {
        logon(TEST_FINANCE_USER)

        // Before step 2 of this migration this was an NPE inside `RechnungDao.find`, i.e. a 500 with a stack
        // trace where the honest answer is "no such invoice".
        assertEquals(HttpStatus.NOT_FOUND, outgoingInvoiceEntityRest.exportInvoiceWord(-1L, null).statusCode)
    }

    @Test
    fun `a user without access to the invoice gets none of its document`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("Word export access")

        logon(TEST_USER)
        // The point of going through `baseDao.find` instead of the cache: an invoice is a document of the
        // finance department, and the export is a way out of the application for its whole content.
        org.junit.jupiter.api.assertThrows<AccessException> {
            outgoingInvoiceEntityRest.exportInvoiceWord(id, null)
        }
    }

    /**
     * A planned invoice with two positions, as the seed of the e2e specs writes one: `GEPLANT` spends no
     * invoice number, and two positions with different amounts make the sums of the document distinguishable.
     */
    private fun insertInvoice(subject: String): Long {
        val invoice = RechnungDO()
        invoice.status = RechnungStatus.GEPLANT
        invoice.typ = RechnungTyp.RECHNUNG
        invoice.datum = LocalDate.of(2026, 3, 2)
        invoice.betreff = subject
        invoice.kundeText = "$subject customer"
        invoice.positionen = mutableListOf(
            position(invoice, 1, BigDecimal("2"), BigDecimal("1000.00")),
            position(invoice, 2, BigDecimal("1"), BigDecimal("500.00")),
        )
        return rechnungDao.insert(invoice)!!
    }

    private fun position(
        invoice: RechnungDO,
        number: Short,
        amount: BigDecimal,
        singleNet: BigDecimal,
    ): RechnungsPositionDO {
        return RechnungsPositionDO().also { pos ->
            pos.rechnung = invoice
            pos.number = number
            pos.menge = amount
            pos.einzelNetto = singleNet
            // The same rate on both, so `extractSharedVat` finds one and the document states a VAT rate
            // instead of its "??????????" placeholder.
            pos.vat = BigDecimal("0.19")
        }
    }
}
