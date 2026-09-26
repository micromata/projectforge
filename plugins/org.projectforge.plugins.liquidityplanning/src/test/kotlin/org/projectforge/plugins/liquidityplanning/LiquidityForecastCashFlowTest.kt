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
import org.junit.jupiter.api.Test
import org.projectforge.plugins.liquidityplanning.LiquiditySeriesProjector.SeriesAnchor
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure unit test of the per-day cash flow over a forecast that mixes all three kinds of liquidity entry: a
 * plain one-off, a materialized series occurrence and the virtual (projected) ones. It proves the projection
 * and the cash-flow bucketing line up — a materialized occurrence counts once and is not also projected, and
 * every entry lands on the day of its date of payment.
 *
 * @author Kai Reinhard
 */
class LiquidityForecastCashFlowTest {
    private val projector = LiquiditySeriesProjector()

    @Test
    fun `normal, materialized and virtual entries are each bucketed once on their day of payment`() {
        val today = LocalDate.now()
        val nextDays = 90

        // A monthly series of three installments of +200 (a debit: money going out), starting today.
        val series = LiquiditySeriesDO()
        series.id = 5L
        series.startDate = today
        series.intervalMonths = 1
        series.count = 3
        series.amount = BigDecimal("200.00")
        series.subject = "rent"

        // Occurrence 0 has been touched → a real, frozen entry. Its anchor is therefore "occupied".
        val materialized = entry(amount = "200.00", dateOfPayment = today, seriesId = 5L, seriesDate = today)
        val occupied = setOf(SeriesAnchor(5L, today))

        // A plain one-off credit (money coming in) three days out.
        val normal = entry(amount = "-500.00", dateOfPayment = today.plusDays(3))

        // The still-virtual occurrences (1 and 2); occurrence 0 is suppressed because it is materialized.
        val virtual = projector.project(today, today.plusDays(nextDays.toLong()), listOf(series), occupied)
        assertEquals(2, virtual.size, "occurrence 0 is materialized, only 1 and 2 stay virtual")
        val day1 = ChronoUnit.DAYS.between(today, virtual[0].dateOfPayment).toInt()
        val day2 = ChronoUnit.DAYS.between(today, virtual[1].dateOfPayment).toInt()

        val forecast = LiquidityForecast()
        forecast.set(listOf(materialized, normal) + virtual)
        forecast.build()
        assertEquals(4, forecast.getEntries().size, "materialized + normal + two virtual, no double count")

        val cashFlow = LiquidityForecastCashFlow(forecast, nextDays)

        // Debits (amount > 0): the materialized occurrence on day 0, the two virtual ones on their days.
        assertAmount("200.00", cashFlow.debits[0])
        assertAmount("200.00", cashFlow.debits[day1])
        assertAmount("200.00", cashFlow.debits[day2])
        // Credits (amount < 0): the one-off, three days out.
        assertAmount("-500.00", cashFlow.credits[3])

        // The credit day carries no debit and vice versa.
        assertAmount("0", cashFlow.credits[0])
        assertAmount("0", cashFlow.debits[3])

        // The totals over the whole window: three debits of 200, one credit of -500.
        assertAmount("600.00", cashFlow.debits.fold(BigDecimal.ZERO, BigDecimal::add))
        assertAmount("-500.00", cashFlow.credits.fold(BigDecimal.ZERO, BigDecimal::add))

        // With no invoice-derived expected date, the expected cash flow mirrors the due-date one.
        assertAmount("600.00", cashFlow.debitsExpected.fold(BigDecimal.ZERO, BigDecimal::add))
        assertAmount("-500.00", cashFlow.creditsExpected.fold(BigDecimal.ZERO, BigDecimal::add))
    }

    @Test
    fun `a payment beyond the window is not counted`() {
        val today = LocalDate.now()
        val forecast = LiquidityForecast()
        forecast.set(listOf(entry(amount = "100.00", dateOfPayment = today.plusDays(120))))
        forecast.build()
        val cashFlow = LiquidityForecastCashFlow(forecast, 90)
        assertAmount("0", cashFlow.debits.fold(BigDecimal.ZERO, BigDecimal::add))
    }

    private fun entry(
        amount: String,
        dateOfPayment: LocalDate,
        seriesId: Long? = null,
        seriesDate: LocalDate? = null,
    ): LiquidityEntryDO {
        val entry = LiquidityEntryDO()
        entry.amount = BigDecimal(amount)
        entry.dateOfPayment = dateOfPayment
        entry.subject = "entry"
        entry.seriesId = seriesId
        entry.seriesDate = seriesDate
        return entry
    }

    private fun assertAmount(expected: String, actual: BigDecimal) {
        assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected but was $actual")
    }
}
