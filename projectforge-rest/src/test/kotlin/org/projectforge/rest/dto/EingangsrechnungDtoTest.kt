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

package org.projectforge.rest.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.rest.fibu.IncomingInvoiceEntityRest
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The incoming-invoice counterpart of `RechnungDtoTest`: the DTO carries the same **two** nested collections
 * — positions, and the cost assignments of each position — and a round trip that loses a row's number, its
 * `deleted` flag or its back reference costs data and history.
 *
 * The one structural difference is `@SoftDeleteCollection` on `EingangsrechnungDO.positionen` (the outgoing
 * `RechnungDO.positionen` has none): a position missing from the posted collection is soft-deleted rather than
 * removed physically. The cost assignments have no such marker, so a lost assignment is gone for good — which
 * is exactly why the round trip has to keep every one of them, index and owner included.
 *
 * Also pins the numbering of new rows and the sums an unsaved invoice answers (the recalculate endpoint
 * computes them off the posted state, which no cache knows).
 */
class EingangsrechnungDtoTest : AbstractTestBase() {

    @Test
    fun `the round trip keeps id, number, deleted flag and the back reference of every position`() {
        val invoice = createInvoice()
        val dto = Eingangsrechnung()
        dto.copyFromWithCollections(invoice)

        // Deleted rows are carried, or the collection handler would lose them.
        assertEquals(3, dto.positionen?.size)
        assertEquals(listOf(false, true, false), dto.positionen?.map { it.deleted })

        val dest = EingangsrechnungDO()
        dto.copyTo(dest)

        assertEquals(3, dest.positionen?.size)
        assertEquals(listOf<Short>(1, 2, 3), dest.positionen?.map { it.number })
        assertEquals(listOf(42L, 43L, 44L), dest.positionen?.map { it.id })
        assertEquals(listOf(false, true, false), dest.positionen?.map { it.deleted })
        dest.positionen?.forEach { position ->
            // Without it a position looks removed to the collection handler — and `eingangsrechnung_fk` is not
            // nullable, so it could not be written at all.
            assertSame(dest, position.eingangsrechnung, "Position ${position.number} lost its invoice.")
        }
        assertEquals(BigDecimal("100.00"), dest.positionen?.first()?.einzelNetto)
        assertEquals("Position 1", dest.positionen?.first()?.text)
    }

    @Test
    fun `the round trip keeps the cost assignments of a position, index and owner included`() {
        val dto = Eingangsrechnung()
        dto.copyFromWithCollections(createInvoice())
        val dest = EingangsrechnungDO()
        dto.copyTo(dest)

        val position = dest.positionen!!.first()
        assertEquals(2, position.kostZuweisungen?.size)
        assertEquals(listOf<Short>(0, 1), position.kostZuweisungen?.map { it.index })
        assertEquals(listOf(101L, 102L), position.kostZuweisungen?.map { it.id })
        assertEquals(BigDecimal("600.00"), position.kostZuweisungen?.first()?.netto)
        assertEquals("Development", position.kostZuweisungen?.first()?.comment)
        // The two cost units are named the same on both sides, so BaseDTO.copy maps them by id itself.
        assertEquals(21L, position.kostZuweisungen?.first()?.kost1?.id)
        assertEquals(22L, position.kostZuweisungen?.first()?.kost2?.id)
        position.kostZuweisungen?.forEach { assignment ->
            assertSame(
                position,
                assignment.eingangsrechnungsPosition,
                "Assignment ${assignment.index} lost its position.",
            )
        }
    }

    @Test
    fun `copyTo rebuilds the collections instead of appending to them`() {
        // Appending would duplicate every row of an invoice that already carries positions, which is
        // exactly what a second copyTo onto the same destination shows.
        val dto = Eingangsrechnung()
        dto.copyFromWithCollections(createInvoice())
        val dest = EingangsrechnungDO()
        dto.copyTo(dest)
        dto.copyTo(dest)

        assertEquals(3, dest.positionen?.size)
        assertEquals(2, dest.positionen?.first()?.kostZuweisungen?.size)
    }

    @Test
    fun `the sums of an unsaved invoice are computed from its own positions`() {
        val invoice = createInvoice()
        invoice.id = null
        invoice.positionen?.forEach { position ->
            position.id = null
            position.kostZuweisungen?.forEach { it.id = null }
        }

        val info = Eingangsrechnung.calculateInvoiceInfo(invoice)

        // Deleted positions don't count, so only 10 * 100.00 and 5 * 200.00 do.
        assertEquals(BigDecimal("2000.00"), info.netSum)
        assertEquals(BigDecimal("600.00"), info.kostZuweisungenNetSum)
        assertEquals(BigDecimal("1400.00"), info.kostZuweisungenFehlbetrag)
        assertNotNull(info.positions)
        // No info for the deleted position: RechnungCalculator skips it.
        assertEquals(2, info.positions?.size)
    }

    @Test
    fun `copyFromWithCollections fills the read-only sums of every position`() {
        val dto = Eingangsrechnung()
        dto.copyFromWithCollections(createInvoice().also { it.id = null })

        val first = dto.positionen?.first()
        assertEquals(BigDecimal("1000.00"), first?.netSum)
        assertEquals(BigDecimal("600.00"), first?.kostZuweisungNetSum)
        // Negated, as RechnungPosInfo computes it: what is still missing reads as -400.00.
        assertEquals(BigDecimal("-400.00"), first?.kostZuweisungNetFehlbetrag)
        // The deleted position has no info, so its sums stay null rather than becoming 0.00.
        assertNull(dto.positionen?.get(1)?.netSum)
    }

    @Test
    fun `the list variant carries no positions`() {
        val dto = Eingangsrechnung()
        dto.copyFrom(createInvoice().also { it.id = null })

        assertNull(dto.positionen, "The list shows no position, so none may be loaded.")
        assertEquals(BigDecimal("2000.00"), dto.netSum)
    }

    @Test
    fun `a new position gets the number past the highest stored one, deleted rows included`() {
        val invoice = createInvoice()
        // The client adds one with the number it previewed in the row's header (3 is taken by a stored
        // position, so its guess is 4).
        invoice.positionen?.add(position(invoice, id = null, number = 4, einzelNetto = "50.00", menge = "1"))

        IncomingInvoiceEntityRest.assignNumbersAndIndicesToNewRows(invoice)

        // The deleted row keeps its number — it stays in the database, `UNIQUE(eingangsrechnung_fk, number)`
        // — so the new row is #4, exactly what the client showed.
        assertEquals(listOf<Short>(1, 2, 3, 4), invoice.positionen?.map { it.number })
    }

    @Test
    fun `a gap in the stored position numbers is kept, not filled`() {
        val invoice = EingangsrechnungDO()
        invoice.id = 4711L
        invoice.positionen = mutableListOf(
            position(invoice, id = 42L, number = 1, einzelNetto = "10.00", menge = "1"),
            position(invoice, id = 44L, number = 5, einzelNetto = "10.00", menge = "1"),
            position(invoice, id = null, number = 6, einzelNetto = "10.00", menge = "1"),
        )

        IncomingInvoiceEntityRest.assignNumbersAndIndicesToNewRows(invoice)

        // 2 to 4 belong to rows that were deleted, and reusing one would merge the new position with a
        // deleted one's history.
        assertEquals(listOf<Short>(1, 5, 6), invoice.positionen?.map { it.number })
    }

    @Test
    fun `a new cost assignment gets the index past the highest stored one of its own position`() {
        val invoice = createInvoice()
        invoice.positionen!!.first().kostZuweisungen!!.add(assignment(id = null, index = 99, netto = "400.00"))
        // A position without any stored assignment starts at 0, not at 1: the index is 0-based.
        invoice.positionen!!.last().kostZuweisungen = mutableListOf(assignment(id = null, index = 7, netto = "1000.00"))

        IncomingInvoiceEntityRest.assignNumbersAndIndicesToNewRows(invoice)

        assertEquals(listOf<Short>(0, 1, 2), invoice.positionen?.first()?.kostZuweisungen?.map { it.index })
        assertEquals(listOf<Short>(0), invoice.positionen?.last()?.kostZuweisungen?.map { it.index })
    }

    private fun createInvoice(): EingangsrechnungDO {
        val invoice = EingangsrechnungDO()
        invoice.id = 4711L
        invoice.kreditor = "Test creditor"
        invoice.referenz = "REF-0815"
        invoice.betreff = "Test invoice"
        invoice.datum = LocalDate.of(2026, 3, 1)
        invoice.positionen = mutableListOf(
            position(invoice, id = 42L, number = 1, einzelNetto = "100.00", menge = "10").also { position ->
                position.kostZuweisungen = mutableListOf(
                    assignment(id = 101L, index = 0, netto = "600.00", comment = "Development")
                        .also { it.eingangsrechnungsPosition = position },
                    assignment(id = 102L, index = 1, netto = "0.00", comment = "Consulting")
                        .also { it.eingangsrechnungsPosition = position },
                )
            },
            position(invoice, id = 43L, number = 2, einzelNetto = "9999.00", menge = "1", deleted = true),
            position(invoice, id = 44L, number = 3, einzelNetto = "200.00", menge = "5"),
        )
        return invoice
    }

    private fun position(
        invoice: EingangsrechnungDO,
        id: Long?,
        number: Short,
        einzelNetto: String,
        menge: String,
        deleted: Boolean = false,
    ): EingangsrechnungsPositionDO {
        return EingangsrechnungsPositionDO().also {
            it.id = id
            it.eingangsrechnung = invoice
            it.number = number
            it.text = "Position $number"
            it.einzelNetto = BigDecimal(einzelNetto)
            it.menge = BigDecimal(menge)
            it.deleted = deleted
        }
    }

    private fun assignment(
        id: Long?,
        index: Short,
        netto: String,
        comment: String? = null,
    ): KostZuweisungDO {
        return KostZuweisungDO().also {
            it.id = id
            it.index = index
            it.netto = BigDecimal(netto)
            it.comment = comment
            it.kost1 = Kost1DO().also { kost -> kost.id = 21L }
            it.kost2 = Kost2DO().also { kost -> kost.id = 22L }
        }
    }
}
