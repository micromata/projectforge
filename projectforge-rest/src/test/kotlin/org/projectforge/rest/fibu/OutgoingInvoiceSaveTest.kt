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

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Rechnung
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The invoice number a *new* invoice gets on the save — `RechnungEditPage.onSaveOrUpdate` in Wicket,
 * [OutgoingInvoiceEntityRest.onBeforeSave] here.
 *
 * Worth a test of its own because the number is the one field of an invoice that cannot be repaired: it is
 * read-only in the form, it comes from a database sequence, and [RechnungDao] refuses a new invoice whose
 * number is absent *or* not the next free one. So the two ways to be wrong are opposites — no number at all
 * ("Wert 'fibu.rechnung.nummer' nicht gegeben"), and a number handed out to something that must not carry
 * one.
 *
 * Not in `RechnungDtoTest`: that one is about the DTO round trip and its two nested collections, while
 * everything here is about the save path and needs the dao.
 */
class OutgoingInvoiceSaveTest : AbstractTestBase() {

    @Autowired
    private lateinit var outgoingInvoiceEntityRest: OutgoingInvoiceEntityRest

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun `a new invoice is given the next free number, which is what makes it storable at all`() {
        logon(TEST_FINANCE_USER)
        val expected = rechnungDao.getNextNumber(null)

        val invoice = newInvoice()
        onBeforeSave(invoice)

        assertEquals(expected, invoice.nummer, "The number of a new invoice comes from the sequence.")
        // The point of the whole hook: RechnungDao.onInsertOrModify rejects a new invoice without a number,
        // and rejects one whose number isn't the next free one - so the insert is the only real assertion
        // that the right number was assigned.
        val id = rechnungDao.insert(invoice)
        assertNotNull(id)
        assertEquals(expected, rechnungDao.find(id)?.nummer)
    }

    @Test
    fun `a planned invoice gets no number yet, because it has not been issued`() {
        logon(TEST_FINANCE_USER)

        val invoice = newInvoice().also { it.status = RechnungStatus.GEPLANT }
        onBeforeSave(invoice)

        // RechnungDao hands it one when it leaves GEPLANT; taking a number now would spend it on an invoice
        // that may never be issued.
        assertNull(invoice.nummer)
    }

    @Test
    fun `a credit note announced by the customer gets no number, as it is not our document`() {
        logon(TEST_FINANCE_USER)

        val invoice = newInvoice().also { it.typ = RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN }
        onBeforeSave(invoice)

        // A number here would be refused outright:
        // fibu.rechnung.error.gutschriftsanzeigeDarfKeineRechnungsnummerHaben.
        assertNull(invoice.nummer)
    }

    @Test
    fun `a number already on the invoice is left alone rather than replaced`() {
        logon(TEST_FINANCE_USER)

        val invoice = newInvoice().also { it.nummer = 4711 }
        onBeforeSave(invoice)

        // Overwriting it would hide a mismatch that RechnungDao reports
        // (fibu.rechnung.error.rechnungsNummerIstNichtFortlaufend) - and the field is read-only in the form,
        // so a number that is there came from the backend in the first place.
        assertEquals(4711, invoice.nummer)
    }

    /** The hook as `AbstractPagesRestUtils.saveOrUpdate` calls it: last step before the insert. */
    private fun onBeforeSave(invoice: RechnungDO) {
        outgoingInvoiceEntityRest.onBeforeSave(
            Mockito.mock(HttpServletRequest::class.java),
            invoice,
            // The hook reads the DO, not the posted DTO, so an empty one is all this needs.
            PostData(data = Rechnung(), watchFieldsTriggered = null, serverData = null),
        )
    }

    /**
     * A new invoice as the form posts one: no id, and issued rather than planned — the case that failed
     * before the hook existed.
     *
     * A free text customer and one position, because `RechnungDao.validate` insists on both
     * (`fibu.rechnung.error.kundeTextOderProjektRequired`,
     * `fibu.rechnung.error.rechnungHatKeinePositionen`) and the insert above has to get through it.
     */
    private fun newInvoice(): RechnungDO {
        val invoice = RechnungDO()
        invoice.status = RechnungStatus.GESTELLT
        invoice.typ = RechnungTyp.RECHNUNG
        invoice.datum = LocalDate.of(2026, 8, 18)
        invoice.betreff = "Invoice save test"
        invoice.kundeText = "Invoice save test customer"
        invoice.positionen = mutableListOf(
            RechnungsPositionDO().also { position ->
                position.rechnung = invoice
                position.number = 1
                position.menge = BigDecimal.ONE
                position.einzelNetto = BigDecimal("100.00")
                position.vat = BigDecimal("0.19")
            }
        )
        return invoice
    }
}
