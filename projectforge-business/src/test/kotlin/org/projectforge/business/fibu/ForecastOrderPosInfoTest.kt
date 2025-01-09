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
    fun `test time and materials pos`() {
        OrderInfo().also { orderInfo -> // Order 6308
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            createPos(
                AuftragsStatus.BEAUFTRAGT,
                AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.OWN,
                periodOfPerformanceBegin = LocalDate.of(2024, Month.NOVEMBER, 13),
                periodOfPerformanceEnd = LocalDate.of(2025, Month.MARCH, 31),
                netSum = BigDecimal("69456.24"),
                invoicedSum = BigDecimal("6924.95")
            ).also { pos ->
                ForecastOrderPosInfo(orderInfo, pos, baseDate = baseDate).also { forecastInfo ->
                    forecastInfo.calculate()
                    Assertions.assertEquals(6, forecastInfo.months.size)
                    for (i in 0..1) {
                        Assertions.assertEquals(BigDecimal.ZERO, forecastInfo.months[i].toBeInvoicedSum)
                    }
                    if (ForecastOrderPosInfo.DISTRIBUTE_UNUSED_BUDGET) {
                        for (i in 2..4) {
                            assertSame("13891.25", forecastInfo.months[i].toBeInvoicedSum)
                        }
                        assertSame("20857.54", forecastInfo.months[5].toBeInvoicedSum)
                        Assertions.assertEquals(BigDecimal.ZERO, forecastInfo.difference)
                    } else {
                        for (i in 2..5) {
                            assertSame("13891.25", forecastInfo.months[i].toBeInvoicedSum)
                        }
                        assertSame("-6966.29", forecastInfo.difference)
                    }
                }
            }
        }
        OrderInfo().also { orderInfo -> // Order 6395
            orderInfo.status = AuftragsStatus.GELEGT
            orderInfo.probabilityOfOccurrence = 50
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2025, Month.JANUARY, 1)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2025, Month.DECEMBER, 31)
            createPos(
                AuftragsStatus.GELEGT, AuftragsPositionsPaymentType.TIME_AND_MATERIALS,
                PeriodOfPerformanceType.SEEABOVE, netSum = BigDecimal("1000000.00")
            ).also { pos ->
                ForecastOrderPosInfo(orderInfo, pos, baseDate = baseDate).also { forecastInfo ->
                    forecastInfo.calculate()
                    Assertions.assertEquals(13, forecastInfo.months.size)
                    Assertions.assertEquals(BigDecimal.ZERO, forecastInfo.difference)
                    Assertions.assertEquals(BigDecimal.ZERO, forecastInfo.months[0].toBeInvoicedSum)
                    for (i in 1..12) {
                        assertSame("41666.6667", forecastInfo.months[i].toBeInvoicedSum)
                    }
                    assertSame("125000.00", forecastInfo.getRemainingForecastSumAfter(PFDay.of(2025, Month.OCTOBER, 31)))
                }
            }
        }
        OrderInfo().also { orderInfo ->  // Order 6215
            orderInfo.status = AuftragsStatus.BEAUFTRAGT
            orderInfo.periodOfPerformanceBegin = LocalDate.of(2024, Month.SEPTEMBER, 2)
            orderInfo.periodOfPerformanceEnd = LocalDate.of(2025, Month.JANUARY, 31)
            addPaymentSchedule(orderInfo, LocalDate.of(2025, Month.JANUARY, 31), BigDecimal("21457.33"))
            addPaymentSchedule(orderInfo, LocalDate.of(2025, Month.FEBRUARY, 28), BigDecimal("21457.33"))
            addPaymentSchedule(orderInfo, LocalDate.of(2025, Month.MARCH, 31), BigDecimal("21457.33"))
            createPos(
                AuftragsStatus.BEAUFTRAGT, AuftragsPositionsPaymentType.FESTPREISPAKET,
                PeriodOfPerformanceType.SEEABOVE, netSum = BigDecimal("64372.00")
            ).also { pos ->
                ForecastOrderPosInfo(orderInfo, pos, baseDate = baseDate).also { forecastInfo ->
                    forecastInfo.calculate()
                    Assertions.assertEquals(7, forecastInfo.months.size, "September -> March")
                    for (i in 0..3) {
                        // December payment is before baseDate.
                        Assertions.assertEquals(
                            BigDecimal.ZERO,
                            forecastInfo.months[i].toBeInvoicedSum,
                            "September - December no payments (all in the past)"
                        )
                    }
                    for (i in 4..6) {
                        Assertions.assertEquals(
                            "21457.33",
                            forecastInfo.months[i].toBeInvoicedSum.toString(),
                            "payments in January, February and March"
                        )
                    }
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
        private fun assertSame(expected: String, actual: Number?) {
            TestUtils.assertSame(expected, actual, BigDecimal("0.01"))
        }
        private val baseDate = PFDay.of(2025, Month.JANUARY, 8)
    }
}
