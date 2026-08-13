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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.PaymentScheduleDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.rest.fibu.AuftragPagesRest
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The order DTO carries two collections, and the persistence layer removes **physically** whatever a
 * posted collection leaves out: `AuftragDO.positionen` has `autoUpdateCollectionEntries = true` but no
 * `@SoftDeleteCollection`, and the collection handler matches a posted row against its database row by
 * (`number`, `auftrag.id`) — see [AuftragsPositionDO.equals]. So a round trip that loses a row's `number`,
 * its `deleted` flag or its back reference to the order costs data and history, silently.
 *
 * That is what these tests pin down. The sums are covered too, because the recalculate endpoint and the
 * list both read them from [Auftrag.calculateOrderInfo] for an order that has no id yet — the cache
 * answers all zeros for such an order.
 */
class AuftragDtoTest : AbstractTestBase() {

    @Test
    fun `the round trip keeps id, number, deleted flag and the back reference of every position`() {
        val order = createOrder()
        val dto = Auftrag()
        dto.copyFromWithCollections(order)

        // Deleted rows are carried, or the collection handler would delete them physically.
        assertEquals(3, dto.positionen?.size)
        assertEquals(listOf(false, true, false), dto.positionen?.map { it.deleted })

        val dest = AuftragDO()
        dto.copyTo(dest)

        assertEquals(3, dest.positionen?.size)
        assertEquals(listOf<Short>(1, 2, 3), dest.positionen?.map { it.number })
        assertEquals(listOf(42L, 43L, 44L), dest.positionen?.map { it.id })
        assertEquals(listOf(false, true, false), dest.positionen?.map { it.deleted })
        dest.positionen?.forEach { position ->
            // Without it a position looks removed to the collection handler.
            assertSame(dest, position.auftrag, "Position ${position.number} lost its order.")
        }
        assertEquals(BigDecimal("1000.00"), dest.positionen?.first()?.nettoSumme)
        assertEquals(AuftragsStatus.BEAUFTRAGT, dest.positionen?.first()?.status)
    }

    @Test
    fun `the round trip keeps the payment schedules, including the position they point at`() {
        val dto = Auftrag()
        dto.copyFromWithCollections(createOrder())
        val dest = AuftragDO()
        dto.copyTo(dest)

        assertEquals(1, dest.paymentSchedules?.size)
        val schedule = dest.paymentSchedules!!.first()
        assertEquals(7L, schedule.id)
        assertEquals(1.toShort(), schedule.number)
        // A position number, not an id — see PaymentScheduleDO.
        assertEquals(3.toShort(), schedule.positionNumber)
        assertEquals(BigDecimal("500.00"), schedule.amount)
        assertSame(dest, schedule.auftrag, "The schedule lost its order (the column is not nullable).")
    }

    @Test
    fun `copyTo rebuilds the collections instead of appending to them`() {
        // Appending (as Rechnung.copyTo does) would duplicate every row of an order that already carries
        // positions, which is exactly what a second copyTo onto the same destination would show.
        val dto = Auftrag()
        dto.copyFromWithCollections(createOrder())
        val dest = AuftragDO()
        dto.copyTo(dest)
        dto.copyTo(dest)

        assertEquals(3, dest.positionen?.size)
        assertEquals(1, dest.paymentSchedules?.size)
    }

    @Test
    fun `the customer and the project are written back, although the DTO names them differently`() {
        val order = createOrder()
        val dto = Auftrag()
        dto.copyFromWithCollections(order)
        assertEquals(11L, dto.customer?.id)

        val dest = AuftragDO()
        dto.copyTo(dest)
        // BaseDTO.copy maps a relation by id, but only between fields of the same name — and these are
        // called customer/project here and kunde/projekt there.
        assertEquals(11L, dest.kunde?.id)
        assertNull(dest.projekt, "No project was set, so none must be invented.")
    }

    @Test
    fun `the sums of an unsaved order are computed from its own positions`() {
        val order = createOrder()
        order.id = null
        order.positionen?.forEach { it.id = null }

        val info = Auftrag.calculateOrderInfo(order)

        // Deleted positions don't count, so only the two live ones do.
        assertEquals(BigDecimal("3000.00"), info.netSum)
        // The invoiced sums come from RechnungCache per position id, and a position without an id can
        // have no invoice — this is the case AuftragsCache cannot answer, since it needs the order's id.
        assertEquals(BigDecimal.ZERO.setScale(2), info.invoicedSum.setScale(2))
        // Must be the posted positions, not the stored ones: the getter falls back to AuftragsCache
        // whenever the backing field is null.
        assertNotNull(info.infoPositions)
        assertEquals(3, info.infoPositions?.size)
    }

    @Test
    fun `copyFrom fills the numeric sums the list sorts by, not only the formatted ones`() {
        val dto = Auftrag()
        dto.copyFromWithCollections(createOrder().also { it.id = null })

        assertEquals(BigDecimal("3000.00"), dto.nettoSumme)
        assertNotNull(dto.formattedNettoSumme)
        // "#2" — the deleted position is not counted.
        assertEquals("#2", dto.pos)
    }

    @Test
    fun `the list variant carries no collections, but still counts the positions`() {
        // Both collections are lazy, so mapping them is a query per row - which is what made the list of
        // some 7000 orders answer 26 MB in 99 seconds. The count comes from the info instead.
        val dto = Auftrag()
        dto.copyFrom(createOrder().also { it.id = null })

        assertNull(dto.positionen, "The list shows no position, so none may be loaded.")
        assertNull(dto.paymentSchedules)
        // The deleted one of the three is not counted.
        assertEquals("#2", dto.pos)
        assertEquals(BigDecimal("3000.00"), dto.nettoSumme)
    }

    @Test
    fun `a deleted position is excluded from the period of performance rules`() {
        // Its dates are none of the user's business anymore; it travels only so it isn't removed.
        val dto = Auftrag()
        dto.copyFromWithCollections(createOrder())
        val live = dto.positionen?.filter { !it.deleted }
        assertEquals(2, live?.size)
        assertTrue(live?.none { it.number == 2.toShort() } == true)
    }

    @Test
    fun `a new instalment gets the number past the highest stored one, deleted rows included`() {
        val order = createOrder()
        // #1 is stored and marked deleted, #2 is stored, and the client adds one with the number it
        // previewed in the row's header (nextScheduleNumber of the next frontend: 3).
        order.paymentSchedules = mutableListOf(
            schedule(order, id = 7L, number = 1, amount = "100.00", deleted = true),
            schedule(order, id = 8L, number = 2, amount = "200.00"),
            schedule(order, id = null, number = 3, amount = "300.00"),
        )

        AuftragPagesRest.assignNumbersToNewRows(order)

        // The deleted row keeps its number — it is still in the database, and `payment#1` is its history
        // key — so the new row is #3, exactly what the client showed.
        assertEquals(listOf<Short>(1, 2, 3), order.paymentSchedules?.map { it.number })
        assertEquals(listOf(true, false, false), order.paymentSchedules?.map { it.deleted })
    }

    @Test
    fun `a gap in the stored instalment numbers is kept, not filled`() {
        val order = createOrder()
        order.paymentSchedules = mutableListOf(
            schedule(order, id = 7L, number = 1, amount = "100.00"),
            schedule(order, id = 9L, number = 5, amount = "500.00"),
            schedule(order, id = null, number = 6, amount = "600.00"),
        )

        AuftragPagesRest.assignNumbersToNewRows(order)

        // 2 to 4 belong to rows that were deleted, and reusing one would merge the new instalment with a
        // deleted one's history. Both sides compute 6, which is what makes the preview trustworthy.
        assertEquals(listOf<Short>(1, 5, 6), order.paymentSchedules?.map { it.number })
    }

    @Test
    fun `the instalments of an order without positions are numbered as well`() {
        // The positions branch used to return early, which left every new instalment of such an order at
        // whatever number the client had guessed.
        val order = createOrder()
        order.positionen = null
        order.paymentSchedules = mutableListOf(
            schedule(order, id = null, number = 17, amount = "100.00"),
        )

        AuftragPagesRest.assignNumbersToNewRows(order)

        assertEquals(listOf<Short>(1), order.paymentSchedules?.map { it.number })
    }

    @Test
    fun `a new position is renumbered and the instalment pointing at it follows`() {
        val order = createOrder()
        // A new position with the placeholder #4 (one past the highest stored one), and an instalment
        // pointing at it — the case a new order runs into: the schedule may only refer to a number.
        order.positionen?.add(position(order, id = null, number = 4, netSum = "500.00"))
        order.paymentSchedules = mutableListOf(
            schedule(order, id = null, number = 2, amount = "500.00").also { it.positionNumber = 4 },
        )

        AuftragPagesRest.assignNumbersToNewRows(order)

        // The stored positions keep 1..3, so the new one becomes #4 as well — and the instalment, being
        // new too, is renumbered to #1, since no instalment is stored yet.
        assertEquals(listOf<Short>(1, 2, 3, 4), order.positionen?.map { it.number })
        assertEquals(4.toShort(), order.paymentSchedules?.first()?.positionNumber)
        assertEquals(1.toShort(), order.paymentSchedules?.first()?.number)
    }

    private fun createOrder(): AuftragDO {
        val order = AuftragDO()
        order.id = 4711L
        order.nummer = 815
        order.titel = "Test order"
        order.status = AuftragsStatus.BEAUFTRAGT
        order.kunde = KundeDO().also { it.id = 11L }
        order.periodOfPerformanceBegin = LocalDate.of(2026, 3, 1)
        order.periodOfPerformanceEnd = LocalDate.of(2026, 6, 30)
        order.positionen = mutableListOf(
            position(order, id = 42L, number = 1, netSum = "1000.00"),
            position(order, id = 43L, number = 2, netSum = "9999.00", deleted = true),
            position(order, id = 44L, number = 3, netSum = "2000.00"),
        )
        order.paymentSchedules = mutableListOf(
            PaymentScheduleDO().also {
                it.id = 7L
                it.auftrag = order
                it.number = 1
                it.positionNumber = 3
                it.scheduleDate = LocalDate.of(2026, 4, 15)
                it.amount = BigDecimal("500.00")
                it.comment = "First milestone"
            },
        )
        return order
    }

    private fun schedule(
        order: AuftragDO,
        id: Long?,
        number: Short,
        amount: String,
        deleted: Boolean = false,
    ): PaymentScheduleDO {
        return PaymentScheduleDO().also {
            it.id = id
            it.auftrag = order
            it.number = number
            it.amount = BigDecimal(amount)
            it.deleted = deleted
        }
    }

    private fun position(
        order: AuftragDO,
        id: Long?,
        number: Short,
        netSum: String,
        deleted: Boolean = false,
    ): AuftragsPositionDO {
        return AuftragsPositionDO().also {
            it.id = id
            it.auftrag = order
            it.number = number
            it.titel = "Position $number"
            it.status = AuftragsStatus.BEAUFTRAGT
            it.nettoSumme = BigDecimal(netSum)
            it.deleted = deleted
        }
    }
}
