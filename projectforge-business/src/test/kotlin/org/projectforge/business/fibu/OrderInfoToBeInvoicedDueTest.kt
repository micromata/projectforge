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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Reached payment schedules dated in a following month aren't due yet (see [OrderInfo.isToBeInvoicedBy]).
 */
class OrderInfoToBeInvoicedDueTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    private val cutoff = LocalDate.of(2026, 9, 30)
    private val septemberDate = LocalDate.of(2026, 9, 15)
    private val octoberDate = LocalDate.of(2026, 10, 15)

    @Test
    fun `reached payment schedule dated in a following month is not due`() {
        val info = insert(AuftragsStatus.BEAUFTRAGT, AuftragsStatus.BEAUFTRAGT) { order ->
            order.addPaymentSchedule(schedule(40, octoberDate, order.positionen!![0].number))
        }
        assertTrue(info.toBeInvoiced, "Including future payment schedules.")
        assertFalse(info.toBeInvoicedImmediately)
        assertEquals(octoberDate, info.nextInvoiceDate)
        assertFalse(info.isToBeInvoicedBy(cutoff))
        assertTrue(info.isToBeInvoicedAfter(cutoff), "To be invoiced in following months.")
        assertTrue(info.isToBeInvoicedBy(LocalDate.of(2026, 10, 31)))
        assertFalse(info.isToBeInvoicedAfter(LocalDate.of(2026, 10, 31)))
        assertSum(40, info.toBeInvoicedSum)
        assertSum(0, info.toBeInvoicedSumBy(cutoff))
        assertSum(40, info.toBeInvoicedSumBy(LocalDate.of(2026, 10, 31)))
        val pos = info.infoPositions!!.first { it.number == 1.toShort() }
        assertFalse(info.isPositionToBeInvoicedBy(pos, cutoff))
        assertTrue(info.isPositionToBeInvoicedBy(pos, LocalDate.of(2026, 10, 31)))
        assertFalse(AuftragNextInvoiceDateFilter(null, cutoff, today = septemberDate).match(info))
        assertTrue(AuftragNextInvoiceDateFilter(LocalDate.of(2026, 10, 1), null, today = septemberDate).match(info))
    }

    @Test
    fun `reached payment schedule dated in the current month is due`() {
        val info = insert(AuftragsStatus.BEAUFTRAGT, AuftragsStatus.BEAUFTRAGT) { order ->
            order.addPaymentSchedule(schedule(40, septemberDate, order.positionen!![0].number))
            order.addPaymentSchedule(schedule(30, octoberDate, order.positionen!![0].number))
        }
        assertEquals(septemberDate, info.nextInvoiceDate)
        assertTrue(info.isToBeInvoicedBy(cutoff))
        // Due this month and in following months as well.
        assertTrue(info.isToBeInvoicedAfter(cutoff))
        assertSum(40, info.toBeInvoicedSumBy(cutoff))
        assertSum(70, info.toBeInvoicedSum)
    }

    @Test
    fun `reached payment schedule without date is due immediately`() {
        val info = insert(AuftragsStatus.BEAUFTRAGT, AuftragsStatus.BEAUFTRAGT) { order ->
            order.addPaymentSchedule(schedule(40, null, order.positionen!![0].number))
        }
        assertTrue(info.toBeInvoicedImmediately)
        assertNull(info.nextInvoiceDate)
        assertTrue(info.isToBeInvoicedBy(cutoff))
        assertFalse(info.isToBeInvoicedAfter(cutoff))
        assertSum(40, info.toBeInvoicedSumBy(cutoff))
        assertEquals(septemberDate, AuftragNextInvoiceDateFilter.effectiveDate(info, septemberDate))
    }

    @Test
    fun `finished position is due immediately, even with a future payment schedule`() {
        val info = insert(AuftragsStatus.BEAUFTRAGT, AuftragsStatus.ABGESCHLOSSEN, AuftragsStatus.BEAUFTRAGT) { order ->
            // Of the finished position: due now, whatever its date.
            order.addPaymentSchedule(schedule(80, octoberDate, order.positionen!![0].number))
            // Of the running position: not due yet.
            order.addPaymentSchedule(schedule(30, octoberDate, order.positionen!![1].number))
        }
        assertTrue(info.toBeInvoicedImmediately)
        assertTrue(info.isToBeInvoicedBy(cutoff))
        assertSum(110, info.toBeInvoicedSum)
        assertSum(80, info.toBeInvoicedSumBy(cutoff))
        // Only the schedule of the running position is left for following months.
        assertTrue(info.isToBeInvoicedAfter(cutoff))
        val (pos1, pos2) = info.infoPositions!!.sortedBy { it.number }
        assertTrue(info.isPositionToBeInvoicedBy(pos1, cutoff))
        assertFalse(info.isPositionToBeInvoicedBy(pos2, cutoff))
    }

    @Test
    fun `finished position without payment schedules is due immediately`() {
        val info = insert(AuftragsStatus.BEAUFTRAGT, AuftragsStatus.ABGESCHLOSSEN, AuftragsStatus.BEAUFTRAGT)
        assertTrue(info.toBeInvoicedImmediately)
        assertTrue(info.isToBeInvoicedBy(cutoff))
        assertSum(100, info.toBeInvoicedSumBy(cutoff))
        assertNull(info.nextInvoiceDate)
    }

    @Test
    fun `to be invoiced counter ignores payment schedules of following months`() {
        val nextMonth = OrderInfo.invoiceCutoff().plusDays(1)
        val counter = auftragsCache.getToBeInvoicedCounter()
        insert(AuftragsStatus.BEAUFTRAGT, AuftragsStatus.BEAUFTRAGT) { order ->
            order.addPaymentSchedule(schedule(40, nextMonth, order.positionen!![0].number))
        }
        assertEquals(counter, auftragsCache.getToBeInvoicedCounter())
        insert(AuftragsStatus.BEAUFTRAGT, AuftragsStatus.BEAUFTRAGT) { order ->
            order.addPaymentSchedule(schedule(40, OrderInfo.invoiceCutoff(), order.positionen!![0].number))
        }
        assertEquals(counter + 1, auftragsCache.getToBeInvoicedCounter())
    }

    private fun insert(
        orderStatus: AuftragsStatus,
        vararg positionStatus: AuftragsStatus,
        modify: ((AuftragDO) -> Unit)? = null,
    ): OrderInfo {
        val order = AuftragDO().also {
            positionStatus.forEachIndexed { index, status ->
                it.addPosition(AuftragsPositionDO().also { pos ->
                    pos.titel = "Pos ${index + 1}"
                    pos.nettoSumme = 100.toBigDecimal()
                    pos.status = status
                })
            }
            it.status = orderStatus
            it.nummer = auftragDao.nextNumber
        }
        modify?.invoke(order)
        auftragDao.insert(order, checkAccess = false)
        return auftragsCache.getOrderInfo(order.id)!!
    }

    private fun schedule(amount: Int, date: LocalDate?, positionNumber: Short?): PaymentScheduleDO {
        return PaymentScheduleDO().also {
            it.amount = amount.toBigDecimal()
            it.reached = true
            it.scheduleDate = date
            it.positionNumber = positionNumber
        }
    }

    private fun assertSum(expected: Int, actual: BigDecimal) {
        assertEquals(0, expected.toBigDecimal().compareTo(actual), "Expected $expected, got $actual.")
    }
}
