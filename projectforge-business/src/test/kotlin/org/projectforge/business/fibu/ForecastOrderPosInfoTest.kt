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
                calculateAndAssert(
                    orderInfo,
                    pos,
                    "0",
                    "0",
                    "0",
                    "15000",
                    "15000",
                    "15000",
                    distributeUnused = false,
                    lastInvoiceMonth = janInvoice,
                ).also {
                    assertSame("0", it.difference)
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
     * Documents the current effect of the [ForecastOrderPosInfo.distributeUnusedBudget] switch (configurable via
     * application.properties / the Forecast.kts script parameter).
     *
     * Since the #6424 fix, the remaining budget is divided by exactly the remaining month count and distributed
     * evenly, so the last month only ever holds its own fair share — there is no "unused budget" pile-up. As a
     * result both switch settings currently produce the same distribution (apart from sub-euro rounding) and the
     * forecast never over-estimates future revenue: the total distributed always equals toBeInvoicedSum. The switch
     * is kept wired through for the (currently rare) cases where a last-month rest can still arise and for forward
     * compatibility, but it must not change the conserved total.
     */
    @Test
    fun `distributeUnusedBudget switch never over-estimates and conserves the total`() {
        fun buildOrder() = OrderInfo().also { orderInfo ->
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            orderInfo.snapshotDate = baseDate.localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2024, Month.JANUARY, 1)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2025, Month.MARCH, 31)
        }
        // Call-off (Abruf) budget, heavily under-invoiced: big net sum, little invoiced, few remaining months.
        for (distributeUnused in listOf(true, false)) {
            buildOrder().also { orderInfo ->
                createPos(
                    AuftragsStatus.BEAUFTRAGT, AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                    PeriodOfPerformanceType.SEEABOVE, netSum = BigDecimal("120000"), invoicedSum = BigDecimal("10000")
                ).also { pos ->
                    ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                        fcPosInfo.distributeUnusedBudget = distributeUnused
                        fcPosInfo.calculate()
                        val distributed = fcPosInfo.months.sumOf { it.toBeInvoicedSum }
                        // No over-estimation: never forecast more than remains to be invoiced.
                        Assertions.assertTrue(
                            distributed <= fcPosInfo.toBeInvoicedSum.add(BigDecimal.ONE),
                            "Over-estimation (distributeUnused=$distributeUnused): $distributed > ${fcPosInfo.toBeInvoicedSum}"
                        )
                        // Conservation: distributed forecast plus the reported shortfall equals toBeInvoicedSum.
                        assertSame(
                            fcPosInfo.toBeInvoicedSum.toPlainString(),
                            distributed.subtract(fcPosInfo.difference),
                            "Conservation (distributeUnused=$distributeUnused)"
                        )
                    }
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
            distributeUnused: Boolean = ForecastOrderPosInfo.defaultDistributeUnusedBudget,
            lastInvoiceMonth: PFDay? = null,
        ): ForecastOrderPosInfo {
            return calculateAndAssert(orderInfo, pos, months.toList(), distributeUnused, lastInvoiceMonth)
        }

        private fun calculateAndAssert(
            orderInfo: OrderInfo,
            pos: OrderPositionInfo,
            months: List<String>,
            distributeUnused: Boolean = ForecastOrderPosInfo.defaultDistributeUnusedBudget,
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
