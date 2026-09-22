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
import java.math.BigDecimal

/**
 * The probability of occurrence of a whole order, as the order's edit page shows it above the sums.
 *
 * [ForecastUtils.getProbabilityOfAccurence] is asserted by [ForecastOrderPosInfoTest] per position; what is
 * tested here is the weighting over the positions and the cases in which no single probability exists.
 */
class ForecastUtilsWeightedProbabilityTest {
    @Test
    fun `probability of a commissioned order is 100 percent`() {
        val order = order(AuftragsStatus.BEAUFTRAGT, pos(AuftragsStatus.BEAUFTRAGT, "100000"))
        assertProbability("1.0000", order)
    }

    @Test
    fun `given probability applies where the status leaves it open`() {
        // GELEGT order and position: Excel row "Order 11" takes the given probability, 50% by default.
        assertProbability("0.7000", order(AuftragsStatus.GELEGT, pos(AuftragsStatus.GELEGT, "100000"), probability = 70))
        assertProbability("0.5000", order(AuftragsStatus.GELEGT, pos(AuftragsStatus.GELEGT, "100000")))
    }

    @Test
    fun `positions weigh by their net sum`() {
        // 100% on 75000 and 50% on 25000 -> (75000 + 12500) / 100000.
        val order = order(
            AuftragsStatus.GELEGT,
            pos(AuftragsStatus.BEAUFTRAGT, "75000"),
            pos(AuftragsStatus.GELEGT, "25000"),
        )
        assertProbability("0.8750", order)
    }

    @Test
    fun `a lost order never occurs`() {
        // Zero rather than null, although the net sums of a lost order's positions are zeroed as well: that the
        // order is lost is a statement, not a missing amount.
        assertProbability("0", order(AuftragsStatus.ABGELEHNT, pos(AuftragsStatus.BEAUFTRAGT, "100000")))
        assertProbability("0", order(AuftragsStatus.ERSETZT, pos(AuftragsStatus.BEAUFTRAGT, "100000")))
    }

    @Test
    fun `a lost position drops out of the weighting`() {
        // The rejected position counts neither in the numerator nor in the denominator - its netSum is 0 - so the
        // remaining commissioned one decides the whole order.
        val order = order(
            AuftragsStatus.GELEGT,
            pos(AuftragsStatus.BEAUFTRAGT, "50000"),
            pos(AuftragsStatus.ABGELEHNT, "50000"),
        )
        assertProbability("1.0000", order)
    }

    @Test
    fun `an order without amounts has no probability`() {
        // Nothing to weigh: a quotient would divide by zero, and 0% would read as "lost".
        Assertions.assertNull(ForecastUtils.getWeightedProbabilityOfAccurence(order(AuftragsStatus.GELEGT)))
        Assertions.assertNull(
            ForecastUtils.getWeightedProbabilityOfAccurence(
                order(AuftragsStatus.GELEGT, pos(AuftragsStatus.GELEGT, "0"))
            )
        )
    }

    @Test
    fun `a deleted position is ignored`() {
        val order = order(
            AuftragsStatus.GELEGT,
            pos(AuftragsStatus.BEAUFTRAGT, "50000"),
            pos(AuftragsStatus.GELEGT, "50000").also { it.deleted = true },
        )
        assertProbability("1.0000", order)
    }

    private fun assertProbability(expected: String, order: OrderInfo) {
        val actual = ForecastUtils.getWeightedProbabilityOfAccurence(order)
        Assertions.assertEquals(
            BigDecimal(expected).stripTrailingZeros(),
            actual?.stripTrailingZeros(),
            "Weighted probability of ${order.status} order",
        )
    }

    private fun order(
        status: AuftragsStatus,
        vararg positions: OrderPositionInfo,
        probability: Int? = null,
    ): OrderInfo {
        return OrderInfo().also { order ->
            order.status = status
            order.probabilityOfOccurrence = probability
            order.infoPositions = positions.toList()
        }
    }

    /**
     * The fields [ForecastUtils.getWeightedProbabilityOfAccurence] reads, set directly rather than through
     * [OrderPositionInfo.recalculateAll]: that would go on to the invoiced sums and thereby to
     * [RechnungCache], which only exists in a Spring context. `netSum` is what recalculateAll would derive -
     * zero for a lost position, the amount otherwise.
     */
    private fun pos(status: AuftragsStatus, netSum: String): OrderPositionInfo {
        return OrderPositionInfo().also {
            it.status = status
            it.dbNetSum = BigDecimal(netSum)
            it.netSum =
                if (status.orderState == AuftragsOrderState.LOST) BigDecimal.ZERO else BigDecimal(netSum)
        }
    }
}
