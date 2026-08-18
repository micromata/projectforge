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
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.rest.fibu.OutgoingInvoiceEntityRest
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The invoice DTO carries **two** nested collections — positions, and the cost assignments of each position
 * — and the persistence layer removes physically whatever a posted collection leaves out: neither
 * `RechnungDO.positionen` nor `RechnungsPositionDO.kostZuweisungen` has `@SoftDeleteCollection` (only
 * `EingangsrechnungDO.positionen` does). So a round trip that loses a row's number, its `deleted` flag or its
 * back reference costs data and history, silently — and frees a number the unique constraint
 * `UNIQUE(rechnung_fk, number)` and the two order columns would then collide with.
 *
 * That is what these tests pin down, together with the numbering of new rows and the sums an unsaved invoice
 * has to answer (the recalculate endpoint computes them off the posted state, which no cache knows).
 */
class RechnungDtoTest : AbstractTestBase() {

    @Test
    fun `the round trip keeps id, number, deleted flag and the back reference of every position`() {
        val invoice = createInvoice()
        val dto = Rechnung()
        dto.copyFromWithCollections(invoice)

        // Deleted rows are carried, or the collection handler would delete them physically.
        assertEquals(3, dto.positionen?.size)
        assertEquals(listOf(false, true, false), dto.positionen?.map { it.deleted })

        val dest = RechnungDO()
        dto.copyTo(dest)

        assertEquals(3, dest.positionen?.size)
        assertEquals(listOf<Short>(1, 2, 3), dest.positionen?.map { it.number })
        assertEquals(listOf(42L, 43L, 44L), dest.positionen?.map { it.id })
        assertEquals(listOf(false, true, false), dest.positionen?.map { it.deleted })
        dest.positionen?.forEach { position ->
            // Without it a position looks removed to the collection handler — and `rechnung_fk` is not
            // nullable, so it could not be written at all.
            assertSame(dest, position.rechnung, "Position ${position.number} lost its invoice.")
        }
        assertEquals(BigDecimal("100.00"), dest.positionen?.first()?.einzelNetto)
        assertEquals("Position 1", dest.positionen?.first()?.text)
    }

    @Test
    fun `the round trip keeps the cost assignments of a position, index and owner included`() {
        val dto = Rechnung()
        dto.copyFromWithCollections(createInvoice())
        val dest = RechnungDO()
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
            assertSame(position, assignment.rechnungsPosition, "Assignment ${assignment.index} lost its position.")
        }
    }

    @Test
    fun `copyTo rebuilds the collections instead of appending to them`() {
        // Appending would duplicate every row of an invoice that already carries positions, which is
        // exactly what a second copyTo onto the same destination shows.
        val dto = Rechnung()
        dto.copyFromWithCollections(createInvoice())
        val dest = RechnungDO()
        dto.copyTo(dest)
        dto.copyTo(dest)

        assertEquals(3, dest.positionen?.size)
        assertEquals(2, dest.positionen?.first()?.kostZuweisungen?.size)
    }

    @Test
    fun `the customer is written back, although the DTO names it differently`() {
        val dto = Rechnung()
        dto.copyFromWithCollections(createInvoice())
        assertEquals(11L, dto.customer?.id)

        val dest = RechnungDO()
        dto.copyTo(dest)
        // BaseDTO.copy maps a relation by id, but only between fields of the same name — and these are
        // called customer/project here and kunde/projekt there.
        assertEquals(11L, dest.kunde?.id)
        assertNull(dest.projekt, "No project was set, so none must be invented.")
    }

    @Test
    fun `the free text customer stays the raw column on a round trip`() {
        // The former copyPositionenFrom overwrote it with `kundeAsString`, the merged display string, which
        // the next save then wrote into the column.
        val invoice = createInvoice()
        invoice.kundeText = "Not in the list"
        val dto = Rechnung()
        dto.copyFromWithCollections(invoice)

        assertEquals("Not in the list", dto.kundeText)
    }

    @Test
    fun `the sums of an unsaved invoice are computed from its own positions`() {
        val invoice = createInvoice()
        invoice.id = null
        invoice.positionen?.forEach { position ->
            position.id = null
            position.kostZuweisungen?.forEach { it.id = null }
        }

        val info = Rechnung.calculateInvoiceInfo(invoice)

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
        val dto = Rechnung()
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
        val dto = Rechnung()
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

        OutgoingInvoiceEntityRest.assignNumbersAndIndicesToNewRows(invoice)

        // The deleted row keeps its number — it stays in the database, `UNIQUE(rechnung_fk, number)` — so
        // the new row is #4, exactly what the client showed.
        assertEquals(listOf<Short>(1, 2, 3, 4), invoice.positionen?.map { it.number })
    }

    @Test
    fun `a gap in the stored position numbers is kept, not filled`() {
        val invoice = RechnungDO()
        invoice.id = 4711L
        invoice.positionen = mutableListOf(
            position(invoice, id = 42L, number = 1, einzelNetto = "10.00", menge = "1"),
            position(invoice, id = 44L, number = 5, einzelNetto = "10.00", menge = "1"),
            position(invoice, id = null, number = 6, einzelNetto = "10.00", menge = "1"),
        )

        OutgoingInvoiceEntityRest.assignNumbersAndIndicesToNewRows(invoice)

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

        OutgoingInvoiceEntityRest.assignNumbersAndIndicesToNewRows(invoice)

        assertEquals(listOf<Short>(0, 1, 2), invoice.positionen?.first()?.kostZuweisungen?.map { it.index })
        assertEquals(listOf<Short>(0), invoice.positionen?.last()?.kostZuweisungen?.map { it.index })
    }

    @Test
    fun `a clone keeps the content of an invoice and none of what the original earned`() {
        val dto = clonedInvoice()

        assertNull(dto.id, "A clone is a new invoice.")
        assertNull(dto.nummer, "A number is spent once handed out; RechnungDao assigns the next free one.")
        // Not the original's GEPLANT/BEZAHLT state and nothing paid on an invoice that doesn't exist yet.
        assertEquals(RechnungStatus.GESTELLT, dto.status)
        assertNull(dto.zahlBetrag)
        assertNull(dto.bezahlDatum)
        // The content is what a clone is for.
        assertEquals("Test invoice", dto.betreff)
        assertEquals(11L, dto.customer?.id)
    }

    @Test
    fun `a clone is dated today, with both payment targets derived again`() {
        val dto = clonedInvoice()

        assertEquals(TODAY, dto.datum)
        // Derived, not copied: the original's due date lies in the past, so a copy would be overdue on the
        // day it is written (see AuftragAndRechnungDaoHelper.onSaveOrModify, which derives it the same way).
        assertEquals(TODAY.plusDays(30), dto.faelligkeit)
        assertEquals(TODAY.plusDays(14), dto.discountMaturity)
    }

    @Test
    fun `a clone carries every live position and cost assignment, none of them with an id`() {
        val dto = clonedInvoice()

        // The deleted position is left out: it is on its way out of the *old* invoice.
        assertEquals(2, dto.positionen?.size)
        assertEquals(listOf("Position 1", "Position 3"), dto.positionen?.map { it.text })
        dto.positionen?.forEach { position ->
            // Every row has to be written as a new one, which is also what makes
            // assignNumbersAndIndicesToNewRows renumber them from 1 on the save.
            assertNull(position.id, "Position ${position.text} would be merged into the original's row.")
            position.kostZuweisungen?.forEach { assignment ->
                assertNull(assignment.id, "A cost assignment of ${position.text} kept its id.")
            }
        }
        // The split itself travels — that is content, unlike the ids above.
        assertEquals(2, dto.positionen?.first()?.kostZuweisungen?.size)
        assertEquals(BigDecimal("600.00"), dto.positionen?.first()?.kostZuweisungen?.first()?.netto)
    }

    @Test
    fun `a clone has no attachments, the files staying with the original`() {
        // They live in JCR under the old invoice's id, so a counter naming them would promise files that
        // aren't there.
        val dto = clonedInvoice()

        assertNull(dto.attachments)
        assertNull(dto.attachmentsCounter)
        assertNull(dto.attachmentsSize)
    }

    /**
     * The invoice of [createInvoice] as the clone endpoint answers it: paid, overdue, with an attachment —
     * i.e. carrying everything a clone has to drop.
     */
    private fun clonedInvoice(): Rechnung {
        val invoice = createInvoice()
        invoice.faelligkeit = LocalDate.of(2026, 3, 15)
        invoice.zahlungsZielInTagen = 30
        invoice.discountZahlungsZielInTagen = 14
        invoice.discountMaturity = LocalDate.of(2026, 3, 8)
        invoice.bezahlDatum = LocalDate.of(2026, 3, 10)
        invoice.zahlBetrag = BigDecimal("2380.00")
        invoice.attachmentsCounter = 2
        invoice.attachmentsSize = 4096L
        val dto = Rechnung()
        dto.copyFromWithCollections(invoice)
        // Both steps the endpoint runs: the generic stripping of AbstractEntityRest.prepareClone, then the
        // invoice's own rules.
        dto.id = null
        dto.deleted = false
        return OutgoingInvoiceEntityRest.prepareInvoiceClone(dto, TODAY)
    }

    private fun createInvoice(): RechnungDO {
        val invoice = RechnungDO()
        invoice.id = 4711L
        invoice.nummer = 815
        invoice.betreff = "Test invoice"
        invoice.status = RechnungStatus.GESTELLT
        invoice.datum = LocalDate.of(2026, 3, 1)
        invoice.kunde = KundeDO().also { it.id = 11L }
        invoice.positionen = mutableListOf(
            position(invoice, id = 42L, number = 1, einzelNetto = "100.00", menge = "10").also { position ->
                position.kostZuweisungen = mutableListOf(
                    assignment(id = 101L, index = 0, netto = "600.00", comment = "Development")
                        .also { it.rechnungsPosition = position },
                    assignment(id = 102L, index = 1, netto = "0.00", comment = "Consulting")
                        .also { it.rechnungsPosition = position },
                )
            },
            position(invoice, id = 43L, number = 2, einzelNetto = "9999.00", menge = "1", deleted = true),
            position(invoice, id = 44L, number = 3, einzelNetto = "200.00", menge = "5"),
        )
        return invoice
    }

    private fun position(
        invoice: RechnungDO,
        id: Long?,
        number: Short,
        einzelNetto: String,
        menge: String,
        deleted: Boolean = false,
    ): RechnungsPositionDO {
        return RechnungsPositionDO().also {
            it.id = id
            it.rechnung = invoice
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

    companion object {
        /** Passed in rather than read from the clock, so the derived dates are assertable at all. */
        private val TODAY = LocalDate.of(2026, 6, 15)
    }
}
