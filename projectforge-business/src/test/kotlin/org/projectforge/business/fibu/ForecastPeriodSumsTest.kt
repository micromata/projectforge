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

import org.junit.jupiter.api.Assertions.assertEquals
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

    /**
     * Characterization: with FOLLOWING_MONTH a performance period of two months (Dec, Jan) is forecast in the two
     * months after its first one (Jan, Feb), half each; the first month stays empty. The forecast export shows the same.
     */
    @Test
    fun `the following month forecast type spreads the revenue over the months after the first one`() {
        val id = insertOrder(
            2000, nextMonth.localDate, nextMonth.plusMonths(1).endOfMonth.localDate,
            type = AuftragForecastType.FOLLOWING_MONTH,
        )
        assertSum(0, id, nextMonth.localDate, nextMonth.endOfMonth.localDate)
        assertSum(1000, id, nextMonth.plusMonths(1).localDate, nextMonth.plusMonths(1).endOfMonth.localDate)
        assertSum(1000, id, nextMonth.plusMonths(2).localDate, nextMonth.plusMonths(2).endOfMonth.localDate)
        assertSum(0, id, nextMonth.plusMonths(3).localDate, nextMonth.plusMonths(3).endOfMonth.localDate)
    }

    /**
     * The amount of a payment schedule stays in the month of its date; the rest of the position (here 1000 net minus
     * 400 scheduled) is distributed evenly from the month of the schedule on, on top of it.
     */
    @Test
    fun `a payment schedule keeps its amount in its month`() {
        val scheduleMonth = nextMonth.plusMonths(1)
        val id = insertOrder(
            1000, nextMonth.localDate, nextMonth.plusMonths(3).endOfMonth.localDate,
            type = AuftragForecastType.CURRENT_MONTH,
            scheduleDate = scheduleMonth.plusDays(9).localDate, scheduleAmount = 400,
        )
        assertSum(0, id, nextMonth.localDate, nextMonth.endOfMonth.localDate)
        assertSum(600, id, scheduleMonth.localDate, scheduleMonth.endOfMonth.localDate) // 400 + 200 of the rest
        assertSum(200, id, scheduleMonth.plusMonths(1).localDate, scheduleMonth.plusMonths(1).endOfMonth.localDate)
        assertSum(200, id, scheduleMonth.plusMonths(2).localDate, scheduleMonth.plusMonths(2).endOfMonth.localDate)
        assertSum(1000, id, nextMonth.localDate, nextMonth.plusMonths(6).endOfMonth.localDate)
    }

    @Test
    fun `a payment schedule covering the whole net sum leaves nothing to distribute`() {
        val scheduleMonth = nextMonth.plusMonths(1)
        val id = insertOrder(
            1000, nextMonth.localDate, nextMonth.plusMonths(3).endOfMonth.localDate,
            type = AuftragForecastType.CURRENT_MONTH,
            scheduleDate = scheduleMonth.plusDays(9).localDate, scheduleAmount = 1000,
        )
        assertSum(1000, id, scheduleMonth.localDate, scheduleMonth.endOfMonth.localDate)
        assertSum(0, id, scheduleMonth.plusMonths(1).localDate, nextMonth.plusMonths(6).endOfMonth.localDate)
        assertSum(0, id, nextMonth.localDate, nextMonth.endOfMonth.localDate)
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
