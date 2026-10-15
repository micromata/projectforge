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

package org.projectforge.plugins.liquidityplanning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.plugins.liquidityplanning.LiquiditySeriesProjector.Companion.decodeVirtualId
import org.projectforge.plugins.liquidityplanning.LiquiditySeriesProjector.Companion.encodeVirtualId
import org.projectforge.plugins.liquidityplanning.LiquiditySeriesProjector.Companion.isVirtual
import org.projectforge.plugins.liquidityplanning.LiquiditySeriesProjector.SeriesAnchor
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Pure unit tests of the recurring-series projection — the engine that turns a stored [LiquiditySeriesDO]
 * into the virtual occurrences the list and the forecast show. No database: the `project(...)` overload that
 * takes the series and the set of already-materialized anchors is exercised directly.
 *
 * @author Kai Reinhard
 */
class LiquiditySeriesProjectorTest {
    private val projector = LiquiditySeriesProjector()

    @Test
    fun `finite series projects exactly count occurrences, monthly, anchored on the start`() {
        val start = LocalDate.of(2025, 1, 15)
        val series = series(id = 1L, startDate = start, intervalMonths = 1, count = 3, amount = "100.00")
        // A wide horizon so the count, not the window, is what bounds the projection.
        val result = projector.project(start, start.plusYears(5), listOf(series), emptySet())
        assertEquals(3, result.size, "count = 3 must yield exactly three occurrences")
        assertEquals(
            listOf(LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 15), LocalDate.of(2025, 3, 15)),
            result.map { it.dateOfPayment },
        )
        // Every virtual row carries the series' template values, a null paid state and its anchor identity.
        result.forEachIndexed { ordinal, entry ->
            assertEquals(BigDecimal("100.00"), entry.amount)
            assertEquals("rent", entry.subject)
            assertEquals(1L, entry.seriesId)
            assertEquals(entry.dateOfPayment, entry.seriesDate, "seriesDate is the anchor, equal to the occurrence date")
            assertNull(entry.paid, "a virtual occurrence is always automatic (paid = null)")
            assertTrue(isVirtual(entry.id), "a virtual row has a synthetic negative id")
            assertEquals(encodeVirtualId(1L, ordinal), entry.id)
        }
    }

    @Test
    fun `endless series is bounded by the horizon end, not by a count`() {
        val start = LocalDate.of(2025, 1, 1)
        val series = series(id = 1L, startDate = start, intervalMonths = 1, count = null)
        // Horizon of exactly six months, end inclusive: Jan..Jul 1 = 7 occurrences.
        val result = projector.project(start, start.plusMonths(6), listOf(series), emptySet())
        assertEquals(7, result.size)
        assertEquals(LocalDate.of(2025, 1, 1), result.first().dateOfPayment)
        assertEquals(LocalDate.of(2025, 7, 1), result.last().dateOfPayment)
    }

    @Test
    fun `occurrences before the horizon start are skipped, the ones after the end stop the loop`() {
        val start = LocalDate.of(2025, 1, 10)
        val series = series(id = 1L, startDate = start, intervalMonths = 1, count = null)
        // Window Mar 10 .. May 10: the Jan and Feb occurrences are before the start, Jun onwards after the end.
        val result = projector.project(LocalDate.of(2025, 3, 10), LocalDate.of(2025, 5, 10), listOf(series), emptySet())
        assertEquals(
            listOf(LocalDate.of(2025, 3, 10), LocalDate.of(2025, 4, 10), LocalDate.of(2025, 5, 10)),
            result.map { it.dateOfPayment },
        )
    }

    @Test
    fun `an interval greater than one month steps by that many months`() {
        val start = LocalDate.of(2025, 1, 1)
        val series = series(id = 1L, startDate = start, intervalMonths = 3, count = 4)
        val result = projector.project(start, start.plusYears(2), listOf(series), emptySet())
        assertEquals(
            listOf(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2025, 10, 1),
            ),
            result.map { it.dateOfPayment },
        )
    }

    @Test
    fun `a month-end start does not drift, every occurrence is measured from the start`() {
        val start = LocalDate.of(2025, 1, 31)
        val series = series(id = 1L, startDate = start, intervalMonths = 1, count = 4)
        val result = projector.project(start, start.plusYears(1), listOf(series), emptySet())
        assertEquals(
            // Feb has no 31st (28th in 2025), yet March is the 31st again — the anchor is always the start,
            // never the previous (already shortened) occurrence.
            listOf(
                LocalDate.of(2025, 1, 31),
                LocalDate.of(2025, 2, 28),
                LocalDate.of(2025, 3, 31),
                LocalDate.of(2025, 4, 30),
            ),
            result.map { it.dateOfPayment },
        )
    }

    @Test
    fun `a leap-year February keeps the 29th`() {
        assertEquals(
            LocalDate.of(2024, 2, 29),
            projector.occurrenceDate(LocalDate.of(2024, 1, 31), 1, 1),
        )
    }

    @Test
    fun `a materialized occurrence is suppressed, including one whose real row was soft-deleted`() {
        val start = LocalDate.of(2025, 1, 1)
        val series = series(id = 5L, startDate = start, intervalMonths = 1, count = 4)
        // The occupied set is the caller's `(seriesId, seriesDate)` of every materialized row — deleted ones
        // included, so a deliberately skipped (soft-deleted) installment does not reappear as virtual.
        val occupied = setOf(
            SeriesAnchor(5L, LocalDate.of(2025, 2, 1)), // a normal materialized occurrence
            SeriesAnchor(5L, LocalDate.of(2025, 3, 1)), // stands for a soft-deleted materialized row
        )
        val result = projector.project(start, start.plusYears(1), listOf(series), occupied)
        assertEquals(
            listOf(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 1)),
            result.map { it.dateOfPayment },
            "only the two untouched occurrences remain virtual",
        )
    }

    @Test
    fun `several series are all projected and keep their own ids`() {
        val a = series(id = 1L, startDate = LocalDate.of(2025, 1, 1), intervalMonths = 1, count = 2)
        val b = series(id = 2L, startDate = LocalDate.of(2025, 1, 1), intervalMonths = 1, count = 1)
        val result = projector.project(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), listOf(a, b), emptySet())
        assertEquals(3, result.size)
        assertEquals(setOf(1L, 2L), result.mapNotNull { it.seriesId }.toSet())
    }

    @Test
    fun `a series without id or start date is skipped rather than throwing`() {
        val noId = series(id = null, startDate = LocalDate.of(2025, 1, 1), intervalMonths = 1, count = 2)
        val noStart = series(id = 1L, startDate = null, intervalMonths = 1, count = 2)
        val result = projector.project(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), listOf(noId, noStart), emptySet())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `no series yields no occurrences`() {
        assertTrue(projector.project(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), emptyList(), emptySet()).isEmpty())
    }

    @Test
    fun `virtual id encodes and decodes back to its series and ordinal`() {
        val id = encodeVirtualId(seriesId = 7L, ordinal = 3)
        assertTrue(id < 0, "the synthetic id is negative so it can never collide with a real primary key")
        val ref = decodeVirtualId(id)!!
        assertEquals(7L, ref.seriesId)
        assertEquals(3, ref.ordinal)
    }

    @Test
    fun `isVirtual and decode only accept negative ids`() {
        assertTrue(isVirtual(-1L))
        assertFalse(isVirtual(0L))
        assertFalse(isVirtual(42L))
        assertFalse(isVirtual(null))
        assertNull(decodeVirtualId(42L), "a real (positive) id is not a virtual reference")
        assertNull(decodeVirtualId(null))
    }

    private fun series(
        id: Long?,
        startDate: LocalDate?,
        intervalMonths: Int,
        count: Int?,
        amount: String = "100.00",
        subject: String = "rent",
    ): LiquiditySeriesDO {
        val series = LiquiditySeriesDO()
        series.id = id
        series.startDate = startDate
        series.intervalMonths = intervalMonths
        series.count = count
        series.amount = BigDecimal(amount)
        series.subject = subject
        return series
    }
}
