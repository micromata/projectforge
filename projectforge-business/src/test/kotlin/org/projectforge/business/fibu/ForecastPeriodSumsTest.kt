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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

/**
 * [ForecastPeriodSums.sumInPeriod]: the amount the forecast expects to be invoiced of an order in a period.
 */
class ForecastPeriodSumsTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var forecastPeriodSums: ForecastPeriodSums

    private val nextMonth = PFDay.now().beginOfMonth.plusMonths(1)

    @BeforeEach
    fun setup() {
        logon(TEST_FINANCE_USER)
    }

    @Test
    fun `a month counts as a whole, a neighbour month doesn't`() {
        val id = insertOrder(1000, nextMonth.localDate, nextMonth.endOfMonth.localDate)
        // Any day of the month asks for the whole month.
        assertSum(1000, id, nextMonth.plusDays(14).localDate, nextMonth.plusDays(15).localDate)
        assertSum(1000, id, nextMonth.minusMonths(1).localDate, nextMonth.endOfMonth.localDate)
        assertSum(0, id, nextMonth.minusMonths(1).localDate, nextMonth.minusMonths(1).endOfMonth.localDate)
        assertSum(0, id, nextMonth.plusMonths(1).localDate, nextMonth.plusMonths(1).endOfMonth.localDate)
    }

    @Test
    fun `the whole period asked for is summed`() {
        val id = insertOrder(
            3000, nextMonth.localDate, nextMonth.plusMonths(2).endOfMonth.localDate,
            type = AuftragForecastType.CURRENT_MONTH,
        )
        assertSum(3000, id, nextMonth.localDate, nextMonth.plusMonths(2).endOfMonth.localDate)
        assertSum(1000, id, nextMonth.localDate, nextMonth.endOfMonth.localDate)
        assertSum(2000, id, nextMonth.plusMonths(1).localDate, nextMonth.plusMonths(2).endOfMonth.localDate)
    }

    @Test
    fun `the following month forecast type shifts the revenue by a month`() {
        val id = insertOrder(
            2000, nextMonth.localDate, nextMonth.plusMonths(1).endOfMonth.localDate,
            type = AuftragForecastType.FOLLOWING_MONTH,
        )
        // Nothing in the first month of the performance period; the invoicing follows one month after the performance.
        assertSum(0, id, nextMonth.localDate, nextMonth.endOfMonth.localDate)
        assertSum(2000, id, nextMonth.plusMonths(1).localDate, nextMonth.plusMonths(2).endOfMonth.localDate)
    }

    @Test
    fun `a payment schedule doesn't change what is forecast in total`() {
        val id = insertOrder(
            1000, nextMonth.localDate, nextMonth.plusMonths(3).endOfMonth.localDate,
            type = AuftragForecastType.CURRENT_MONTH,
            scheduleDate = nextMonth.plusMonths(1).plusDays(9).localDate, scheduleAmount = 400,
        )
        assertSum(1000, id, nextMonth.localDate, nextMonth.plusMonths(6).endOfMonth.localDate)
        assertTrue(
            forecastPeriodSums.sumInPeriod(info(id), nextMonth.plusMonths(1).localDate, nextMonth.plusMonths(1).endOfMonth.localDate)
                    .signum() > 0,
            "The month of the schedule is part of the forecast.",
        )
    }

    @Test
    fun `a rejected order has no forecast`() {
        val id = insertOrder(
            1000, nextMonth.localDate, nextMonth.endOfMonth.localDate,
            status = AuftragsStatus.ABGELEHNT,
        )
        assertSum(0, id, nextMonth.minusMonths(1).localDate, nextMonth.plusMonths(6).localDate)
    }

    private fun info(id: Long): OrderInfo = auftragsCache.getOrderInfo(id)!!

    private fun assertSum(expected: Int, id: Long, from: LocalDate, until: LocalDate) {
        val sum = forecastPeriodSums.sumInPeriod(info(id), from, until)
        // A third of 1000 is not exact: the cents of the monthly rate may be missing.
        assertTrue(
            (BigDecimal(expected) - sum).abs() < BigDecimal.ONE,
            "Expected $expected from $from to $until, but was $sum.",
        )
    }

    private fun insertOrder(
        netSum: Int,
        begin: LocalDate,
        end: LocalDate,
        type: AuftragForecastType = AuftragForecastType.CURRENT_MONTH,
        status: AuftragsStatus = AuftragsStatus.BEAUFTRAGT,
        scheduleDate: LocalDate? = null,
        scheduleAmount: Int = 0,
    ): Long {
        val order = AuftragDO().also {
            it.nummer = auftragDao.nextNumber
            it.status = status
            it.forecastType = type
            it.periodOfPerformanceBegin = begin
            it.periodOfPerformanceEnd = end
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.titel = "Pos $netSum"
                pos.nettoSumme = netSum.toBigDecimal()
                pos.status = status
            })
            if (scheduleDate != null) {
                it.addPaymentSchedule(PaymentScheduleDO().also { schedule ->
                    schedule.positionNumber = 1
                    schedule.scheduleDate = scheduleDate
                    schedule.amount = scheduleAmount.toBigDecimal()
                })
            }
        }
        auftragDao.insert(order, checkAccess = false)
        return order.id!!
    }
}
