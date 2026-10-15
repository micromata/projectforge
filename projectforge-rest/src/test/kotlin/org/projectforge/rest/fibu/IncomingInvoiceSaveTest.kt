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
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The save path of the hand built incoming-invoice form: [IncomingInvoiceEntityRest.transformFromDB] read as a
 * DTO and [IncomingInvoiceEntityRest.transformForDB] posted back.
 *
 * The incoming invoice has no invoice-number sequence (that is the outgoing invoice's `onBeforeSave`) and no
 * period of performance to validate, so what is left to pin down is the field the DTO does not carry:
 * `uiStatusAsXml`, the collapsed-position memory of the Wicket form. `transformForDB` builds a fresh
 * `EingangsrechnungDO` and the persistence layer merges it over the database row, so a field the DTO omits ends
 * up null unless it is copied back — which is exactly what would make the Wicket form forget what the user
 * collapsed. Unlike the outgoing invoice there are no attachment columns to preserve.
 *
 * Each test creates its own throwaway invoice; nothing here touches an existing row.
 */
class IncomingInvoiceSaveTest : AbstractTestBase() {

    @Autowired
    private lateinit var incomingInvoiceEntityRest: IncomingInvoiceEntityRest

    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Test
    fun `a save from the next form keeps the collapsed positions the Wicket form remembered`() {
        logon(TEST_FINANCE_USER)
        val invoice = newInvoice()
        val id = eingangsrechnungDao.insert(invoice)
        // As `AbstractRechnungEditForm` writes it: the numbers of the position rows shown collapsed. Set on
        // the database row directly, because that form is the only thing that ever produces it.
        val uiStatus = "<rechnungUIStatus><closedPositions><short>1</short></closedPositions></rechnungUIStatus>"
        eingangsrechnungDao.find(id)!!.let { dbObj ->
            dbObj.uiStatusAsXml = uiStatus
            eingangsrechnungDao.update(dbObj)
        }

        // The round trip of the next form: read it as a DTO, post it back unchanged.
        val dto = incomingInvoiceEntityRest.transformFromDB(eingangsrechnungDao.find(id)!!, editMode = true)
        val posted = incomingInvoiceEntityRest.transformForDB(dto)

        // The DTO doesn't carry the field, so transformForDB has to copy it back from the database row -
        // otherwise the merge nulls the column and the Wicket form forgets what the user collapsed.
        assertEquals(uiStatus, posted.uiStatusAsXml)
    }

    @Test
    fun `the round trip keeps the position with its cost-free content`() {
        logon(TEST_FINANCE_USER)
        val invoice = newInvoice()
        val id = eingangsrechnungDao.insert(invoice)

        val dto = incomingInvoiceEntityRest.transformFromDB(eingangsrechnungDao.find(id)!!, editMode = true)
        val posted = incomingInvoiceEntityRest.transformForDB(dto)

        assertEquals("Test creditor", posted.kreditor)
        assertNotNull(posted.positionen)
        assertEquals(1, posted.positionen?.size)
        assertEquals(BigDecimal("100.00"), posted.positionen?.first()?.einzelNetto)
        // The back reference survives, or the collection handler would treat the row as removed.
        assertEquals(posted, posted.positionen?.first()?.eingangsrechnung)
    }

    /**
     * A new invoice as the form posts one: `EingangsrechnungDao` refuses one without a date and one without a
     * position (`fibu.rechnung.error.rechnungHatKeinePositionen`), so both are given.
     */
    private fun newInvoice(): EingangsrechnungDO {
        val invoice = EingangsrechnungDO()
        invoice.kreditor = "Test creditor"
        invoice.referenz = "save-test"
        invoice.datum = LocalDate.of(2026, 8, 18)
        invoice.faelligkeit = LocalDate.of(2026, 9, 18)
        invoice.betreff = "Incoming invoice save test"
        invoice.positionen = mutableListOf(
            EingangsrechnungsPositionDO().also { position ->
                position.eingangsrechnung = invoice
                position.number = 1
                position.menge = BigDecimal.ONE
                position.einzelNetto = BigDecimal("100.00")
                position.vat = BigDecimal("0.19")
            }
        )
        return invoice
    }
}
