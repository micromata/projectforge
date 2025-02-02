/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
                calculateAndAssert(
                    orderInfo,
                    pos,
                    "0",
                    "0",
                    "10000",
                    "10000",
                    "10000",
                    "15000",
                    distributeUnused = true
                ).also {
                    assertSame("0", it.difference)
                }
                calculateAndAssert(
                    orderInfo,
                    pos,
                    "0",
                    "0",
                    "10000",
                    "10000",
                    "10000",
                    "10000",
                    distributeUnused = false
                ).also {
                    assertSame("-5000", it.difference)
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
        // Test order with big loss of budget:
        OrderInfo().also { orderInfo ->  // Order 5575
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            orderInfo.snapshotDate = baseDate.localDate
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2024, Month.JANUARY, 7)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2025, Month.JUNE, 30)
            createPos(
                AuftragsStatus.BEAUFTRAGT, AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.SEEABOVE, netSum = BigDecimal(1_800_000)
            ).also { pos ->
                // Nothing invoiced.
                ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                    fcPosInfo.calculate()
                    Assertions.assertEquals(19, fcPosInfo.months.size, "Jan 24 -> Jul 25")
                    for (i in 0..11) {
                        // December payment is before baseDate.
                        Assertions.assertEquals(
                            BigDecimal.ZERO,
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Jan - Dec no payments, ${fcPosInfo.months[i].date} should be 0.00 but is ${fcPosInfo.months[i].toBeInvoicedSum}"
                        )
                    }
                    for (i in 12..17) {
                        // December payment is before baseDate.
                        Assertions.assertEquals(
                            BigDecimal(100_000),
                            fcPosInfo.months[i].toBeInvoicedSum,
                            "Jan - Jun 2025 , ${fcPosInfo.months[i].date} should be 10,000.00 but is ${fcPosInfo.months[i].toBeInvoicedSum}"
                        )
                    }
                    val remaining = if (ForecastOrderPosInfo.DISTRIBUTE_UNUSED_BUDGET) {
                        BigDecimal(1_200_000)
                    } else {
                        BigDecimal(100_000)
                    }
                    Assertions.assertEquals(remaining, fcPosInfo.months[18].toBeInvoicedSum, "remaining in Jul 2025")
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
            distributeUnused: Boolean = ForecastOrderPosInfo.DISTRIBUTE_UNUSED_BUDGET,
        ): ForecastOrderPosInfo {
            return calculateAndAssert(orderInfo, pos, months.toList(), distributeUnused)
        }

        private fun calculateAndAssert(
            orderInfo: OrderInfo,
            pos: OrderPositionInfo,
            months: List<String>,
            distributeUnused: Boolean = ForecastOrderPosInfo.DISTRIBUTE_UNUSED_BUDGET,
        ): ForecastOrderPosInfo {
            ForecastOrderPosInfo(orderInfo, pos).also { fcPosInfo ->
                val saveDefault = ForecastOrderPosInfo.DISTRIBUTE_UNUSED_BUDGET
                ForecastOrderPosInfo.DISTRIBUTE_UNUSED_BUDGET = distributeUnused
                fcPosInfo.calculate()
                assertMonths(fcPosInfo, months)
                ForecastOrderPosInfo.DISTRIBUTE_UNUSED_BUDGET = saveDefault
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
