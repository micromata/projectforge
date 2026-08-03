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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.commons.test.TestUtils
import org.projectforge.framework.time.PFDay
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class ForecastOrderPosInfoTest {
    @Test
    fun `distribute revenue`() {
        OrderInfo().also { order ->
            order.status = AuftragsStatus.BEAUFTRAGT
            order.snapshotDate = baseDate.localDate
            createPos(
                AuftragsStatus.BEAUFTRAGT,
                AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.OWN,
                periodOfPerformanceBegin = LocalDate.of(2025, Month.JANUARY, 1),
                periodOfPerformanceEnd = LocalDate.of(2025, Month.MAY, 31),
                netSum = BigDecimal("50000"), // 5 month
            ).also { pos ->
                ForecastOrderPosInfo(order, pos).also { fcPosInfo ->
                    fcPosInfo.calculate()
                    // January - June (6 month, forecast in following month)
                    assertMonths(fcPosInfo, "0", "10000", "10000", "10000", "10000", "10000")
                }
                pos.forecastType = AuftragForecastType.CURRENT_MONTH
                ForecastOrderPosInfo(order, pos).also { fcPosInfo ->
                    fcPosInfo.calculate()
                    // January - May (5 month, forecast in current month)
                    assertMonths(fcPosInfo, "10000", "10000", "10000", "10000", "10000")
                }
            }
            createPos(
                AuftragsStatus.BEAUFTRAGT,
                AuftragsPositionsPaymentType.FESTPREISPAKET,
                PeriodOfPerformanceType.OWN,
                periodOfPerformanceBegin = LocalDate.of(2025, Month.JANUARY, 1),
                periodOfPerformanceEnd = LocalDate.of(2025, Month.MAY, 31),
                netSum = BigDecimal("50000"), // 5 month
            ).also { pos ->
                ForecastOrderPosInfo(order, pos).also { fcPosInfo ->
                    fcPosInfo.calculate()
                    assertMonths(fcPosInfo, "0", "0", "0", "0", "0", "50000")
                }
                pos.forecastType = AuftragForecastType.CURRENT_MONTH
                ForecastOrderPosInfo(order, pos).also { fcPosInfo ->
                    fcPosInfo.calculate()
                    assertMonths(fcPosInfo, "0", "0", "0", "0", "50000")
                }
            }
        }
    }

    @Test
    fun `test time and materials pos`() {
        OrderInfo().also { orderInfo -> // Order 6308
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            orderInfo.snapshotDate = baseDate.localDate
            createPos(
                AuftragsStatus.BEAUFTRAGT,
                AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.OWN,
                periodOfPerformanceBegin = LocalDate.of(2024, Month.NOVEMBER, 13),
                periodOfPerformanceEnd = LocalDate.of(2025, Month.MARCH, 31),
                netSum = BigDecimal("50000"), // 5 month
                invoicedSum = BigDecimal("5000")
            ).also { pos ->
                // toBeInvoicedSum = 45000, distributed over 3 remaining months (Feb-Apr) = 15000/month.
                // Jan is skipped because the retroactive invoice for December's work was already written in
                // January (the current month): lastInvoiceMonth = Jan 2025, so distribution starts in February.
                val janInvoice = PFDay.of(2025, Month.JANUARY, 5)
                calculateAndAssert(
                    orderInfo,
                    pos,
                    "0",
                    "0",
                    "0",
                    "15000",
                    "15000",
                    "15000",
                    distributeUnused = true,
                    lastInvoiceMonth = janInvoice,
                ).also {
                    assertSame("0", it.difference)
                }
                // distributeUnused = false for T&M: extrapolate the historical call-off run rate instead of
                // spreading the remaining budget. invoicedSum = 5000 over 3 elapsed months (Nov, Dec, Jan) = 1666.67
                // per month. Feb-Apr are forecast at that rate; the not-called-off budget (~40000) is reported as a
                // negative difference with a budget warning rather than assumed as future revenue.
                calculateAndAssert(
                    orderInfo,
                    pos,
                    "0",
                    "0",
                    "0",
                    "1666.67",
                    "1666.67",
                    "1666.67",
                    distributeUnused = false,
                    lastInvoiceMonth = janInvoice,
                ).also {
                    Assertions.assertTrue(
                        it.difference < BigDecimal.ZERO,
                        "Under-run must be reported as negative difference, was ${it.difference}"
                    )
                    Assertions.assertTrue(it.lostBudgetWarning, "Budget under-run warning expected")
                }
            }
        }
        OrderInfo().also { orderInfo -> // Order 6395
            orderInfo.status = AuftragsStatus.GELEGT
            orderInfo.snapshotDate = baseDate.localDate
            orderInfo.probabilityOfOccurrence = 50
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2025, Month.JANUARY, 1)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2025, Month.DECEMBER, 31)
            createPos(
                AuftragsStatus.GELEGT, AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.SEEABOVE, netSum = BigDecimal("1000000.00")
            ).also { pos ->
                calculateAndAssert(
                    orderInfo,
                    pos,
                    months = buildList { add("0"); repeat(12) { add("41666.6667") } }).let { fcPosInfo ->
                    Assertions.assertEquals(BigDecimal.ZERO, fcPosInfo.months[0].toBeInvoicedSum)
                    assertSame("125000.00", fcPosInfo.getRemainingForecastSumAfter(PFDay.of(2025, Month.OCTOBER, 31)))
                }
            }
        }
        OrderInfo().also { orderInfo ->  // Order 5850
            orderInfo.status = AuftragsStatus.ABGESCHLOSSEN
            orderInfo.snapshotDate = baseDate.localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2024, Month.JANUARY, 2)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2024, Month.DECEMBER, 31)
            createPos(
                AuftragsStatus.BEAUFTRAGT, AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.SEEABOVE, netSum = BigDecimal("120000.00")
            ).also { pos ->
                pos.invoicedSum = BigDecimal("120000.00")
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.calculate()
                    Assertions.assertEquals(13, fcPosInfo.months.size, "September -> March")
                    for (i in 0..12) {
                        // December payment is before baseDate.
                        Assertions.assertEquals(
                            BigDecimal.ZERO,
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Jan - jan no payments (all is invoiced), ${fcPosInfo.months[i].date} should be 0.00 but is ${fcPosInfo.months[i].toBeInvoicedSum}"
                        )
                    }
                }
            }
        }
        // Test order with big loss of budget (now distributes evenly over remaining months):
        OrderInfo().also { orderInfo ->  // Order 5575
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            orderInfo.snapshotDate = baseDate.localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2024, Month.JANUARY, 7)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2025, Month.JUNE, 30)
            createPos(
                AuftragsStatus.BEAUFTRAGT, AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.SEEABOVE, netSum = BigDecimal(1_800_000)
            ).also { pos ->
                // Nothing invoiced. toBeInvoicedSum = 1,800,000.
                // Since nothing was invoiced yet, the current month (Jan 2025) is part of the forecast:
                // remainingMonthCount = 7 (Jan-Jul 2025), partlyNetSum = 1,800,000 / 7 = 257143.
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.calculate()
                    Assertions.assertEquals(19, fcPosInfo.months.size, "Jan 24 -> Jul 25")
                    // Jan - Dec 2024 no payments (all in the past):
                    for (i in 0..11) {
                        Assertions.assertEquals(
                            BigDecimal.ZERO,
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Jan 24 - Dec 24 no payments, ${fcPosInfo.months[i].date} should be 0.00 but is ${fcPosInfo.months[i].toBeInvoicedSum}"
                        )
                    }
                    val partlyNetSum = BigDecimal(1_800_000).divide(BigDecimal(7), java.math.RoundingMode.HALF_UP)
                    // Jan - Jun 2025 (current month is included because nothing was invoiced yet):
                    for (i in 12..17) {
                        assertSame(
                            partlyNetSum.toPlainString(),
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Jan - Jun 2025, ${fcPosInfo.months[i].date}"
                        )
                    }
                    // Last month (Jul 2025) gets the remaining amount
                    Assertions.assertTrue(
                        fcPosInfo.months[18].toBeInvoicedSum > BigDecimal.ZERO,
                        "Jul 2025 should have remaining forecast"
                    )
                    // Total forecast must equal toBeInvoicedSum (no over-estimation):
                    assertSame(
                        "1800000",
                        fcPosInfo.months.sumOf { it.toBeInvoicedSum },
                        "Total distributed must equal toBeInvoicedSum"
                    )
                }
            }
        }
    }

    /**
     * Reproduces the reported bug: For FOLLOWING_MONTH (retroactive invoicing) orders, the current month
     * was incorrectly included in the remaining month count, dividing by 7 instead of 6.
     * Example: Order 6863 with performance period Jan-Dec 2026, baseMonth=Jul 2026.
     *
     * The current month must be skipped only if it is *already invoiced* (its retroactive invoice has been
     * written). See the [`following month forecast includes current month until it is invoiced`] test for the
     * contrasting case reported as order 6809 / 5503.
     */
    @Test
    fun `following month forecast should not count current month`() {
        val julBaseDate = PFDay.of(2026, Month.JULY, 15)
        // Scenario similar to reported order 6863: T&M, FOLLOWING_MONTH, full year 2026, partially invoiced.
        OrderInfo().also { orderInfo ->
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            orderInfo.snapshotDate = julBaseDate.localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2026, Month.JANUARY, 1)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2026, Month.DECEMBER, 31)
            createPos(
                AuftragsStatus.BEAUFTRAGT,
                AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.SEEABOVE,
                netSum = BigDecimal("120000"),
                invoicedSum = BigDecimal("60000"), // Jan-Jun work invoiced
            ).also { pos ->
                // Retroactive invoicing: June's work was invoiced in July (the current month), so the latest
                // actual invoice is dated July. toBeInvoicedSum = 60000, remaining months = Aug-Jan2027 = 6 (not 7!)
                // partlyNetSum = 60000 / 6 = 10000
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.distributeUnusedBudget = true // This test verifies even distribution, not run-rate.
                    fcPosInfo.lastInvoiceMonth = PFDay.of(2026, Month.JULY, 3)
                    fcPosInfo.calculate()
                    // Months: Jan2026..Jan2027 = 13 months (FOLLOWING_MONTH adds 1)
                    Assertions.assertEquals(13, fcPosInfo.months.size)
                    // Jan-Jul should be 0 (past + current month already invoiced)
                    for (i in 0..6) {
                        Assertions.assertEquals(
                            BigDecimal.ZERO,
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Month ${fcPosInfo.months[i].date} should be 0"
                        )
                    }
                    // Aug-Jan2027 = 6 months, each 10000
                    for (i in 7..12) {
                        assertSame(
                            "10000",
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Month ${fcPosInfo.months[i].date} should be 10000"
                        )
                    }
                }
            }
        }
        // Same scenario but with CURRENT_MONTH: current month IS included in distribution.
        OrderInfo().also { orderInfo ->
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            orderInfo.snapshotDate = julBaseDate.localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2026, Month.JANUARY, 1)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2026, Month.DECEMBER, 31)
            createPos(
                AuftragsStatus.BEAUFTRAGT,
                AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.SEEABOVE,
                netSum = BigDecimal("120000"),
                invoicedSum = BigDecimal("60000"),
            ).also { pos ->
                pos.forecastType = AuftragForecastType.CURRENT_MONTH
                // CURRENT_MONTH: work-month = invoice-month, so Jan-Jun work invoiced -> latest invoice = June.
                // toBeInvoicedSum = 60000, remaining months = Jul-Dec = 6
                // partlyNetSum = 60000 / 6 = 10000
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.distributeUnusedBudget = true // This test verifies even distribution, not run-rate.
                    fcPosInfo.lastInvoiceMonth = PFDay.of(2026, Month.JUNE, 30)
                    fcPosInfo.calculate()
                    // Months: Jan2026..Dec2026 = 12 months (no extra month for CURRENT_MONTH)
                    Assertions.assertEquals(12, fcPosInfo.months.size)
                    // Jan-Jun should be 0 (past)
                    for (i in 0..5) {
                        Assertions.assertEquals(
                            BigDecimal.ZERO,
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Month ${fcPosInfo.months[i].date} should be 0"
                        )
                    }
                    // Jul-Dec = 6 months, each 10000
                    for (i in 6..11) {
                        assertSame(
                            "10000",
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Month ${fcPosInfo.months[i].date} should be 10000"
                        )
                    }
                }
            }
        }
        // Festpreispaket with FOLLOWING_MONTH (similar to reported order 6809):
        // Fixed price goes to the last month, so the month-count bug doesn't affect the amount,
        // but the distribution position matters.
        OrderInfo().also { orderInfo ->
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            orderInfo.snapshotDate = julBaseDate.localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2026, Month.JANUARY, 1)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2026, Month.DECEMBER, 31)
            createPos(
                AuftragsStatus.BEAUFTRAGT,
                AuftragsPositionsPaymentType.FESTPREISPAKET,
                PeriodOfPerformanceType.SEEABOVE,
                netSum = BigDecimal("80000"),
                invoicedSum = BigDecimal("40000"),
            ).also { pos ->
                // FESTPREISPAKET: remaining sum goes to last month (Jan 2027 for FOLLOWING_MONTH)
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.calculate()
                    Assertions.assertEquals(13, fcPosInfo.months.size)
                    // All months 0 except last (Jan 2027)
                    for (i in 0..11) {
                        Assertions.assertEquals(
                            BigDecimal.ZERO,
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Month ${fcPosInfo.months[i].date} should be 0"
                        )
                    }
                    assertSame("40000", fcPosInfo.months[12].toBeInvoicedSum, "Jan 2027 should have remaining")
                }
            }
        }
    }

    /**
     * Reproduces reported orders 6809 / 5503: For FOLLOWING_MONTH (retroactive invoicing) orders the current
     * month must remain part of the forecast until its invoice has actually been written. Otherwise the current
     * month's revenue is dropped and the remaining budget is spread over too few months (e.g. 5 instead of 6).
     *
     * Scenario: T&M, FOLLOWING_MONTH, performance period Jan-Dec 2026, baseMonth = Aug 2026, Jan-Jun work
     * invoiced (latest invoice dated Jul, for June's work). The invoice for August has NOT been written yet, so
     * August is still forecast and the remaining budget is spread over Aug-Jan2027 = 6 months.
     */
    @Test
    fun `following month forecast includes current month until it is invoiced`() {
        val augBaseDate = PFDay.of(2026, Month.AUGUST, 3)
        OrderInfo().also { orderInfo ->
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            orderInfo.snapshotDate = augBaseDate.localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2026, Month.JANUARY, 1)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2026, Month.DECEMBER, 31)
            createPos(
                AuftragsStatus.BEAUFTRAGT,
                AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.SEEABOVE,
                netSum = BigDecimal("120000"),
                invoicedSum = BigDecimal("60000"), // Jan-Jun work invoiced
            ).also { pos ->
                // Latest actual invoice is dated July (June's work); the August invoice is NOT written yet.
                // toBeInvoicedSum = 60000, remaining months = Aug-Jan2027 = 6, partlyNetSum = 10000.
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.distributeUnusedBudget = true // This test verifies even distribution, not run-rate.
                    fcPosInfo.lastInvoiceMonth = PFDay.of(2026, Month.JULY, 3)
                    fcPosInfo.calculate()
                    Assertions.assertEquals(13, fcPosInfo.months.size)
                    // Jan-Jul should be 0 (past + already invoiced)
                    for (i in 0..6) {
                        Assertions.assertEquals(
                            BigDecimal.ZERO,
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Month ${fcPosInfo.months[i].date} should be 0"
                        )
                    }
                    // Aug-Jan2027 = 6 months, each 10000 (August is included!)
                    for (i in 7..12) {
                        assertSame(
                            "10000",
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Month ${fcPosInfo.months[i].date} should be 10000"
                        )
                    }
                    assertSame("0", fcPosInfo.difference)
                }
            }
        }
        // Once the August invoice is written (latest invoice = August), the forecast moves to Sep-Jan = 5 months,
        // each 12000. This is exactly the behavior the user observed after writing the August invoice.
        OrderInfo().also { orderInfo ->
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            orderInfo.snapshotDate = augBaseDate.localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2026, Month.JANUARY, 1)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2026, Month.DECEMBER, 31)
            createPos(
                AuftragsStatus.BEAUFTRAGT,
                AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.SEEABOVE,
                netSum = BigDecimal("120000"),
                invoicedSum = BigDecimal("60000"),
            ).also { pos ->
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.distributeUnusedBudget = true // This test verifies even distribution, not run-rate.
                    fcPosInfo.lastInvoiceMonth = PFDay.of(2026, Month.AUGUST, 2)
                    fcPosInfo.calculate()
                    Assertions.assertEquals(13, fcPosInfo.months.size)
                    // Jan-Aug should be 0 (past + already invoiced through August)
                    for (i in 0..7) {
                        Assertions.assertEquals(
                            BigDecimal.ZERO,
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Month ${fcPosInfo.months[i].date} should be 0"
                        )
                    }
                    // Sep-Jan2027 = 5 months, each 12000
                    for (i in 8..12) {
                        assertSame(
                            "12000",
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Month ${fcPosInfo.months[i].date} should be 12000"
                        )
                    }
                }
            }
        }
    }

    /**
     * Guards against over-estimation of future revenue: the total distributed forecast (plus what was already
     * invoiced) must never exceed the weighted net sum, regardless of forecast type or how many months have
     * already been invoiced.
     */
    @Test
    fun `forecast must not over-estimate future revenue`() {
        val augBaseDate = PFDay.of(2026, Month.AUGUST, 3)
        // For a range of "latest invoiced month" the sum of forecast months must always equal toBeInvoicedSum.
        for (lastInvoiceMonthValue in listOf(null, Month.MARCH, Month.JULY, Month.AUGUST, Month.NOVEMBER)) {
            for (forecastType in AuftragForecastType.entries) {
                OrderInfo().also { orderInfo ->
                    orderInfo.status = AuftragsStatus.BEAUFTRAGT
                    orderInfo.snapshotDate = augBaseDate.localDate
                    orderInfo.periodOfPerformanceBegin = LocalDate.of(2026, Month.JANUARY, 1)
                    orderInfo.periodOfPerformanceEnd = LocalDate.of(2026, Month.DECEMBER, 31)
                    createPos(
                        AuftragsStatus.BEAUFTRAGT,
                        AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                        PeriodOfPerformanceType.SEEABOVE,
                        netSum = BigDecimal("120000"),
                        invoicedSum = BigDecimal("60000"),
                    ).also { pos ->
                        pos.forecastType = forecastType
                        ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                            fcPosInfo.lastInvoiceMonth =
                                lastInvoiceMonthValue?.let { PFDay.of(2026, it, 15) }
                            fcPosInfo.calculate()
                            val distributed = fcPosInfo.months.sumOf { it.toBeInvoicedSum }
                            // toBeInvoicedSum = weightedNetSum - invoicedSum = 60000. The distributed forecast plus
                            // the reported difference (lost/undistributed budget) must reconstruct toBeInvoicedSum,
                            // and must never exceed it.
                            Assertions.assertTrue(
                                distributed <= fcPosInfo.toBeInvoicedSum.add(BigDecimal.ONE),
                                "Over-estimation for type=$forecastType, lastInvoiceMonth=$lastInvoiceMonthValue: " +
                                        "distributed=$distributed > toBeInvoicedSum=${fcPosInfo.toBeInvoicedSum}"
                            )
                            // Total accounted for = distributed + difference-shortfall must equal toBeInvoicedSum.
                            assertSame(
                                fcPosInfo.toBeInvoicedSum.toPlainString(),
                                distributed.subtract(fcPosInfo.difference),
                                "Conservation for type=$forecastType, lastInvoiceMonth=$lastInvoiceMonthValue"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Reproduces reported order 5503: a fixed-price order billed monthly ("Festpreisauftrag" with PAUSCHALE payment
     * type and retroactive/FOLLOWING_MONTH invoicing), performance period Jan - Dec 2025, total 120000 (10000/month).
     * Base month = Aug 2025. Jan - Jun work is invoiced retroactively (the latest actual invoice is dated July, for
     * June's work); the August invoice (for July's work) has NOT been written yet. So the remaining 60000 must be
     * spread evenly over the 6 months Aug 2025 - Jan 2026 (10000 each), NOT over Sept - Jan (5 months, the bug).
     */
    @Test
    fun `pauschale order with retroactive invoicing distributes over current and remaining months`() {
        val order = OrderInfo().also { orderInfo -> // Order 5503
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            orderInfo.snapshotDate = PFDay.of(2025, Month.AUGUST, 3).localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2025, Month.JANUARY, 1)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2025, Month.DECEMBER, 31)
        }
        createPos(
            AuftragsStatus.BEAUFTRAGT,
            AuftragsPositionsPaymentType.PAUSCHALE,
            PeriodOfPerformanceType.SEEABOVE,
            netSum = BigDecimal("120000"),
            invoicedSum = BigDecimal("60000"), // Jan - Jun work invoiced
        ).also { pos ->
            // Latest actual invoice dated July (June's work); August invoice not written yet.
            // toBeInvoicedSum = 60000, distribution over Aug 2025 - Jan 2026 = 6 months, 10000 each.
            // Months: Jan 2025 .. Jan 2026 = 13 entries (FOLLOWING_MONTH adds one trailing month).
            ForecastOrderPosInfo(order, pos).also { fcPosInfo ->
                fcPosInfo.lastInvoiceMonth = PFDay.of(2025, Month.JULY, 3)
                fcPosInfo.calculate()
                Assertions.assertEquals(13, fcPosInfo.months.size)
                // Jan - Jul 2025 blank (past + already invoiced):
                for (i in 0..6) {
                    assertSame(
                        "0",
                        fcPosInfo.months[i].toBeInvoicedSum,
                        "Month ${fcPosInfo.months[i].date} should be 0"
                    )
                }
                // Aug 2025 - Jan 2026 = 6 months, each 10000 (August IS included):
                for (i in 7..12) {
                    assertSame(
                        "10000",
                        fcPosInfo.months[i].toBeInvoicedSum,
                        "Month ${fcPosInfo.months[i].date} should be 10000"
                    )
                }
                assertSame(
                    "60000",
                    fcPosInfo.months.sumOf { it.toBeInvoicedSum },
                    "Total distributed must equal toBeInvoicedSum"
                )
            }
        }
        // After writing the August invoice (latest invoice = Aug 2025), the remaining 50000 is spread over the
        // 5 months Sept 2025 - Jan 2026 = 10000 each. This is what the user observed as "now correct".
        createPos(
            AuftragsStatus.BEAUFTRAGT,
            AuftragsPositionsPaymentType.PAUSCHALE,
            PeriodOfPerformanceType.SEEABOVE,
            netSum = BigDecimal("120000"),
            invoicedSum = BigDecimal("70000"), // Jan - Jul work invoiced
        ).also { pos ->
            ForecastOrderPosInfo(order, pos).also { fcPosInfo ->
                fcPosInfo.lastInvoiceMonth = PFDay.of(2025, Month.AUGUST, 2)
                fcPosInfo.calculate()
                // Jan - Aug 2025 blank (past + already invoiced through August):
                for (i in 0..7) {
                    assertSame(
                        "0",
                        fcPosInfo.months[i].toBeInvoicedSum,
                        "Month ${fcPosInfo.months[i].date} should be 0"
                    )
                }
                // Sept 2025 - Jan 2026 = 5 months, each 10000:
                for (i in 8..12) {
                    assertSame(
                        "10000",
                        fcPosInfo.months[i].toBeInvoicedSum,
                        "Month ${fcPosInfo.months[i].date} should be 10000"
                    )
                }
            }
        }
    }

    /**
     * The reported call-off (Abruf) budget problem: a customer commissions 120000 T&M for the calendar year, but
     * only ~1000/month is actually called off. The remaining budget must NOT be assumed as future revenue.
     *
     * - distributeUnusedBudget = true (default): the remaining budget is spread evenly over the remaining months
     *   (previous behavior) — this overestimates, but is the configured default.
     * - distributeUnusedBudget = false: the historical call-off run rate (invoiced / elapsed months) is extrapolated
     *   into the future and the not-called-off budget is shown as a negative difference with a warning.
     */
    @Test
    fun `time and materials call-off budget extrapolates run rate when distributeUnusedBudget is false`() {
        fun buildOrder() = OrderInfo().also { orderInfo ->
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            // Base month = November 2025; Jan-Oct called off at 1000/month (10 months elapsed, 10000 invoiced).
            orderInfo.snapshotDate = PFDay.of(2025, Month.NOVEMBER, 5).localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2025, Month.JANUARY, 1)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2025, Month.DECEMBER, 31)
        }

        // distributeUnusedBudget = false: extrapolate ~1000/month, don't assume the unused ~109000 as revenue.
        buildOrder().also { orderInfo ->
            createPos(
                AuftragsStatus.BEAUFTRAGT, AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.SEEABOVE, netSum = BigDecimal("120000"), invoicedSum = BigDecimal("10000")
            ).also { pos ->
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.lastInvoiceMonth = PFDay.of(2025, Month.OCTOBER, 31)
                    fcPosInfo.distributeUnusedBudget = false
                    fcPosInfo.calculate()
                    // Run rate = 10000 / 10 elapsed months = 1000. Future months (Nov, Dec, Jan) forecast at 1000.
                    val futureMonths = fcPosInfo.months.filter { it.toBeInvoicedSum.signum() != 0 }
                    Assertions.assertTrue(
                        futureMonths.all { it.toBeInvoicedSum.compareTo(BigDecimal("1000")) == 0 },
                        "Each future month should be forecast at the ~1000 run rate, was: " +
                                futureMonths.joinToString { "${it.date}=${it.toBeInvoicedSum}" }
                    )
                    val distributed = fcPosInfo.months.sumOf { it.toBeInvoicedSum }
                    // Far below the remaining 110000: the unused budget is NOT assumed as revenue.
                    Assertions.assertTrue(
                        distributed < BigDecimal("10000"),
                        "Distributed ($distributed) must be close to the run rate, not the remaining budget"
                    )
                    // The under-run shows up as a large negative difference with a warning.
                    Assertions.assertTrue(
                        fcPosInfo.difference < BigDecimal("-100000"),
                        "Under-called budget must be a negative difference, was ${fcPosInfo.difference}"
                    )
                    Assertions.assertTrue(fcPosInfo.lostBudgetWarning, "Budget under-run warning expected")
                }
            }
        }

        // distributeUnusedBudget = true (default): even distribution of the remaining 110000 over remaining months.
        buildOrder().also { orderInfo ->
            createPos(
                AuftragsStatus.BEAUFTRAGT, AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.SEEABOVE, netSum = BigDecimal("120000"), invoicedSum = BigDecimal("10000")
            ).also { pos ->
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.lastInvoiceMonth = PFDay.of(2025, Month.OCTOBER, 31)
                    fcPosInfo.distributeUnusedBudget = true
                    fcPosInfo.calculate()
                    // The whole remaining 110000 is distributed (previous behavior, may overestimate).
                    assertSame(
                        "110000",
                        fcPosInfo.months.sumOf { it.toBeInvoicedSum },
                        "Default distributes the full remaining budget"
                    )
                }
            }
        }
    }

    /**
     * The pointed "all revenue lands in the last month" case for a T&M call-off order: 120000 for the calendar year,
     * only 1000/month called off (Jan-Nov invoiced = 11000). In December, the single remaining future month, the two
     * modes diverge sharply:
     *
     * - distributeUnusedBudget = true (default): December absorbs the whole remaining 109000 at once (the reported
     *   over-estimation — the forecast suddenly spikes in the last month).
     * - distributeUnusedBudget = false: December stays at the ~1000 run rate; the un-called ~108000 is reported as a
     *   negative difference with a warning instead of being booked as revenue.
     *
     * CURRENT_MONTH so the performance period is exactly Jan-Dec (no trailing FOLLOWING_MONTH month), leaving December
     * as the one and only future month.
     */
    @Test
    fun `time and materials call-off books whole rest in last month only when distributeUnusedBudget is true`() {
        fun buildOrder() = OrderInfo().also { orderInfo ->
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            // Base month = December 2025; Jan-Nov called off at 1000/month (11 months elapsed, 11000 invoiced).
            orderInfo.snapshotDate = PFDay.of(2025, Month.DECEMBER, 5).localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2025, Month.JANUARY, 1)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2025, Month.DECEMBER, 31)
        }

        fun buildPos(orderInfo: OrderInfo) = createPos(
            AuftragsStatus.BEAUFTRAGT, AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
            PeriodOfPerformanceType.SEEABOVE, netSum = BigDecimal("120000"), invoicedSum = BigDecimal("11000")
        ).also { pos ->
            pos.forecastType = AuftragForecastType.CURRENT_MONTH
        }

        // distributeUnusedBudget = true: December (the only future month) absorbs the whole remaining 109000.
        buildOrder().also { orderInfo ->
            buildPos(orderInfo).also { pos ->
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.lastInvoiceMonth = PFDay.of(2025, Month.NOVEMBER, 30)
                    fcPosInfo.distributeUnusedBudget = true
                    fcPosInfo.calculate()
                    val futureMonths = fcPosInfo.months.filter { it.toBeInvoicedSum.signum() != 0 }
                    Assertions.assertEquals(
                        1, futureMonths.size,
                        "Only December should carry forecast, was: " +
                                futureMonths.joinToString { "${it.date}=${it.toBeInvoicedSum}" }
                    )
                    Assertions.assertEquals(Month.DECEMBER, futureMonths[0].date.month)
                    assertSame("109000", futureMonths[0].toBeInvoicedSum, "December absorbs the whole remaining budget")
                }
            }
        }

        // distributeUnusedBudget = false: December stays at the ~1000 run rate; the rest is a negative difference.
        buildOrder().also { orderInfo ->
            buildPos(orderInfo).also { pos ->
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.lastInvoiceMonth = PFDay.of(2025, Month.NOVEMBER, 30)
                    fcPosInfo.distributeUnusedBudget = false
                    fcPosInfo.calculate()
                    val futureMonths = fcPosInfo.months.filter { it.toBeInvoicedSum.signum() != 0 }
                    Assertions.assertEquals(
                        1, futureMonths.size,
                        "Only December should carry forecast, was: " +
                                futureMonths.joinToString { "${it.date}=${it.toBeInvoicedSum}" }
                    )
                    Assertions.assertEquals(Month.DECEMBER, futureMonths[0].date.month)
                    // Run rate = 11000 / 11 elapsed months = 1000. December forecast at 1000, NOT the whole 109000.
                    assertSame("1000", futureMonths[0].toBeInvoicedSum, "December stays at the run rate")
                    Assertions.assertTrue(
                        fcPosInfo.difference < BigDecimal("-100000"),
                        "Un-called budget must be a negative difference, was ${fcPosInfo.difference}"
                    )
                    Assertions.assertTrue(fcPosInfo.lostBudgetWarning, "Budget under-run warning expected")
                }
            }
        }
    }

    /**
     * PAUSCHALE (flat monthly fee): the fee is spread evenly over the whole performance period. If less is invoiced,
     * the future monthly fee does NOT increase — the pauschale stays fixed and the shortfall is reported as a
     * negative difference with a warning.
     */
    @Test
    fun `pauschale keeps fixed monthly fee and reports under-run as difference`() {
        OrderInfo().also { orderInfo ->
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            // Base month = November 2025; Jan-Oct should have been 10000/month but only 5000/month was invoiced.
            orderInfo.snapshotDate = PFDay.of(2025, Month.NOVEMBER, 5).localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2025, Month.JANUARY, 1)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2025, Month.DECEMBER, 31)
            createPos(
                AuftragsStatus.BEAUFTRAGT, AuftragsPositionsPaymentType.PAUSCHALE,
                PeriodOfPerformanceType.SEEABOVE, netSum = BigDecimal("120000"), invoicedSum = BigDecimal("50000")
            ).also { pos ->
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.lastInvoiceMonth = PFDay.of(2025, Month.OCTOBER, 31)
                    fcPosInfo.calculate()
                    // Fixed pauschale = 120000 / 12 = 10000/month. The future rate stays 10000 (not raised to catch up).
                    val futureMonths = fcPosInfo.months.filter { it.toBeInvoicedSum.signum() != 0 }
                    Assertions.assertTrue(
                        futureMonths.all { it.toBeInvoicedSum.compareTo(BigDecimal("10000")) == 0 },
                        "Future pauschale months must stay at the fixed 10000 fee, was: " +
                                futureMonths.joinToString { "${it.date}=${it.toBeInvoicedSum}" }
                    )
                    // toBeInvoicedSum = 70000, but at 10000/month only 30000 (Nov, Dec, Jan) is forecast:
                    // the 40000 already-missed pauschale is a negative difference with a warning.
                    Assertions.assertTrue(
                        fcPosInfo.difference < BigDecimal.ZERO,
                        "Pauschale under-run must be a negative difference, was ${fcPosInfo.difference}"
                    )
                    Assertions.assertTrue(fcPosInfo.lostBudgetWarning, "Pauschale under-run warning expected")
                }
            }
        }
    }

    @Test
    fun `test fixed price orders`() {
        OrderInfo().also { orderInfo ->  // Order 6215
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            orderInfo.snapshotDate = baseDate.localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2024, Month.SEPTEMBER, 2)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2025, Month.JANUARY, 31)
            addPaymentSchedule(orderInfo, LocalDate.of(2025, Month.JANUARY, 31), BigDecimal("21457.33"))
            addPaymentSchedule(orderInfo, LocalDate.of(2025, Month.FEBRUARY, 28), BigDecimal("21457.33"))
            addPaymentSchedule(orderInfo, LocalDate.of(2025, Month.MARCH, 31), BigDecimal("21457.33"))
            createPos(
                AuftragsStatus.BEAUFTRAGT, AuftragsPositionsPaymentType.FESTPREISPAKET,
                PeriodOfPerformanceType.SEEABOVE, netSum = BigDecimal("64372.00")
            ).also { pos ->
                calculateAndAssert(
                    orderInfo,
                    pos,
                    // September - December no payments (all in the past)
                    // payments in January, February and March
                    "0", "0", "0", "0", "21457.33", "21457.33", "21457.33"
                ).also {
                    assertSame("0", it.difference)
                }
            }
        }
    }

    private fun createPos(
        status: AuftragsStatus,
        paymentType: AuftragsPositionsPaymentType,
        periodOfPerformanceType: PeriodOfPerformanceType,
        periodOfPerformanceBegin: LocalDate? = null,
        periodOfPerformanceEnd: LocalDate? = null,
        netSum: BigDecimal = BigDecimal.ZERO,
        invoicedSum: BigDecimal = BigDecimal.ZERO,
    ): OrderPositionInfo {
        return OrderPositionInfo().also {
            it.status = status
            it.number = 0
            it.paymentType = paymentType
            it.periodOfPerformanceType = periodOfPerformanceType
            it.periodOfPerformanceBegin = periodOfPerformanceBegin
            it.periodOfPerformanceEnd = periodOfPerformanceEnd
            it.netSum = netSum
            it.invoicedSum = invoicedSum
        }
    }

    private fun addPaymentSchedule(orderInfo: OrderInfo, date: LocalDate, amount: BigDecimal) {
        orderInfo.paymentScheduleEntries = orderInfo.paymentScheduleEntries ?: mutableListOf()
        val entries = orderInfo.paymentScheduleEntries as MutableList
        val schedule = PaymentScheduleDO().also {
            it.scheduleDate = date
            it.number = (entries.size + 1).toShort()
            it.amount = amount
            it.positionNumber = 0
        }
        entries.add(OrderInfo.PaymentScheduleInfo(schedule))
    }

    companion object {
        private fun calculateAndAssert(
            orderInfo: OrderInfo,
            pos: OrderPositionInfo,
            vararg months: String,
            // Fixed true (even distribution), NOT ForecastOrderPosInfo.defaultDistributeUnusedBudget: these
            // assertions verify even distribution and must stay deterministic regardless of the configured global
            // default (which a Spring-based test in the same JVM may flip to false).
            distributeUnused: Boolean = true,
            lastInvoiceMonth: PFDay? = null,
        ): ForecastOrderPosInfo {
            return calculateAndAssert(orderInfo, pos, months.toList(), distributeUnused, lastInvoiceMonth)
        }

        private fun calculateAndAssert(
            orderInfo: OrderInfo,
            pos: OrderPositionInfo,
            months: List<String>,
            distributeUnused: Boolean = true,
            lastInvoiceMonth: PFDay? = null,
        ): ForecastOrderPosInfo {
            ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                fcPosInfo.distributeUnusedBudget = distributeUnused
                fcPosInfo.lastInvoiceMonth = lastInvoiceMonth
                fcPosInfo.calculate()
                assertMonths(fcPosInfo, months)
                return fcPosInfo
            }
        }

        private fun assertMonths(fcPosInfo: ForecastOrderPosInfo, vararg months: String) {
            assertMonths(fcPosInfo, months.toList())
        }

        private fun assertMonths(fcPosInfo: ForecastOrderPosInfo, months: List<String>) {
            val debug = "months=[${fcPosInfo.months.joinToString { "${it.date}=${it.toBeInvoicedSum}" }}"
            Assertions.assertEquals(months.size, fcPosInfo.months.size, debug)
            for (i in months.indices) {
                assertSame(
                    months[i],
                    fcPosInfo.months[i].toBeInvoicedSum,
                    "month(i)=$i, $debug]"
                )
            }
        }

        private fun assertSame(expected: String, actual: Number?, msg: String? = null) {
            TestUtils.assertSame(expected, actual, BigDecimal("0.01"), msg)
        }

        private val baseDate = PFDay.of(2025, Month.JANUARY, 8)
    }
}
