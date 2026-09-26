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
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Pure unit test of the list-view statistics ([LiquidityEntriesStatistics]) over a set that mixes all three
 * kinds of liquidity entry — plain one-offs, a materialized series occurrence and virtual (projected) ones.
 * The statistics only ever read `effectivePaid` / `amount` / `dateOfPayment`, so the point is to prove those
 * three fields bucket every kind the same way: total over all, paid vs. open by `effectivePaid`, overdue for
 * the unpaid ones already past due, and the two counters.
 *
 * @author Kai Reinhard
 */
class LiquidityEntriesStatisticsTest {
    private val projector = LiquiditySeriesProjector()

    @Test
    fun `effectivePaid buckets total, paid, open and overdue across normal, materialized and virtual entries`() {
        val today = LocalDate.now()
        val stats = LiquidityEntriesStatistics()

        // 1) A plain paid entry in the past: paid regardless of its (past) due date, never overdue.
        stats.add(entry(amount = "100.00", dateOfPayment = today.minusDays(10), paid = true))
        // 2) A plain open entry in the future: open, not yet due, so not overdue.
        stats.add(entry(amount = "200.00", dateOfPayment = today.plusDays(10), paid = false))
        // 3) A plain open entry past its due date: open and overdue.
        stats.add(entry(amount = "50.00", dateOfPayment = today.minusDays(5), paid = false))
        // 4) A materialized series occurrence, marked paid: paid like any real entry.
        stats.add(entry(amount = "300.00", dateOfPayment = today.plusDays(3), paid = true, seriesId = 5L, seriesDate = today.plusDays(3)))
        // 5) A virtual future occurrence, automatic (paid = null) and not auto-paid: open, future, not overdue.
        stats.add(entry(amount = "400.00", dateOfPayment = today.plusDays(30), paid = null, seriesId = 7L, seriesDate = today.plusDays(30)))
        // 6) A virtual past occurrence of an autoSetPaid series: effectivePaid resolves to paid, so it counts
        //    as paid (and therefore is not overdue although its date is in the past).
        stats.add(entry(amount = "25.00", dateOfPayment = today.minusDays(2), paid = null, autoSetPaid = true, seriesId = 7L, seriesDate = today.minusDays(2)))

        assertAmount("1075.00", stats.total, "every entry counts towards the total")
        assertAmount("425.00", stats.paid, "the two paid entries plus the auto-paid virtual one (100 + 300 + 25)")
        assertAmount("650.00", stats.open, "the three unpaid entries (200 + 50 + 400)")
        assertAmount("50.00", stats.overdue, "only the single unpaid entry already past its due date")
        assertEquals(6, stats.counter, "all six entries are counted")
        assertEquals(3, stats.counterPaid, "two real paid entries plus the auto-paid virtual one")
    }

    @Test
    fun `a projected virtual occurrence reports effectivePaid from the series autoSetPaid rule`() {
        val today = LocalDate.now()
        // An endless, auto-set-paid series anchored a month back, so the window holds both past occurrences
        // (which auto-set to paid) and occurrences on or after today (which stay open).
        val series = LiquiditySeriesDO()
        series.id = 9L
        series.startDate = today.minusMonths(1)
        series.intervalMonths = 1
        series.count = null
        series.amount = BigDecimal("40.00")
        series.subject = "insurance"
        series.autoSetPaid = true

        val virtual = projector.project(today.minusMonths(2), today.plusMonths(2), listOf(series), emptySet())
        assertTrue(virtual.isNotEmpty(), "the window must contain occurrences on both sides of today")

        // effectivePaid on a virtual row is purely the autoSetPaid rule: paid once the due date has passed.
        virtual.forEach { occ ->
            assertNull(occ.paid, "a virtual occurrence is always automatic")
            val due = occ.dateOfPayment!!
            assertEquals(due.isBefore(today), occ.effectivePaid, "auto-set-paid resolves by the due date")
        }

        val stats = LiquidityEntriesStatistics()
        virtual.forEach { stats.add(it) }
        val pastCount = virtual.count { it.dateOfPayment!!.isBefore(today) }
        assertEquals(pastCount, stats.counterPaid, "the past occurrences count as paid")
        assertEquals(virtual.size - pastCount, virtual.size - stats.counterPaid, "the rest stay open")
        assertAmount((40 * pastCount).toString(), stats.paid, "each past occurrence contributes its template amount")
        // No unpaid occurrence lies before today (auto-set-paid covers those), so nothing is overdue.
        assertNull(stats.overdue, "auto-set-paid past occurrences are paid, never overdue")
        assertFalse(stats.total.signum() == 0, "the mix is not empty")
    }

    private fun entry(
        amount: String,
        dateOfPayment: LocalDate,
        paid: Boolean? = null,
        autoSetPaid: Boolean = false,
        seriesId: Long? = null,
        seriesDate: LocalDate? = null,
    ): LiquidityEntryDO {
        val entry = LiquidityEntryDO()
        entry.amount = BigDecimal(amount)
        entry.dateOfPayment = dateOfPayment
        entry.paid = paid
        entry.autoSetPaid = autoSetPaid
        entry.subject = "entry"
        entry.seriesId = seriesId
        entry.seriesDate = seriesDate
        return entry
    }

    private fun assertAmount(expected: String, actual: BigDecimal?, message: String) {
        assertEquals(0, BigDecimal(expected).compareTo(actual), "$message: expected $expected but was $actual")
    }
}
