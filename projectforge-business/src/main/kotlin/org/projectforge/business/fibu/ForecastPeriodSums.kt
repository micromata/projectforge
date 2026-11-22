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

import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Computes the forecast of order positions (amounts still to be invoiced per month). Shared by the forecast export and
 * the order book, so that both show the same numbers.
 */
@Service
open class ForecastPeriodSums {
    @Autowired
    private lateinit var rechnungCache: RechnungCache

    /**
     * Calculates the forecast of a position.
     * @param baseDate Only invoices up to this date are considered (null: all, i.e. the current state).
     */
    open fun calculate(
        order: OrderInfo,
        pos: OrderPositionInfo,
        baseDate: LocalDate? = null,
        distributeUnusedBudget: Boolean = ForecastOrderPosInfo.defaultDistributeUnusedBudget,
    ): ForecastOrderPosInfo {
        val forecastInfo = ForecastOrderPosInfo(order, pos)
        forecastInfo.distributeUnusedBudget = distributeUnusedBudget
        // Distribution must not re-forecast months already covered by actual invoices (respecting baseDate):
        forecastInfo.lastInvoiceMonth = rechnungCache.getRechnungsPosInfosByAuftragsPositionId(pos.id)
            ?.filter { it.isInvoiced && (baseDate == null || (it.rechnungInfo?.date ?: LocalDate.MAX) <= baseDate) }
            ?.mapNotNull { it.rechnungInfo?.date }
            ?.maxOrNull()
            ?.let { PFDay.from(it).beginOfMonth }
        forecastInfo.calculate()
        return forecastInfo
    }

    /**
     * The amount the forecast expects to be invoiced for the given order in the months from [from] to [until]
     * (both inclusive, month granularity). The same positions as in the forecast export are considered.
     */
    open fun sumInPeriod(order: OrderInfo, from: LocalDate, until: LocalDate): BigDecimal {
        if (!ForecastUtils.auftragsStatusToShow.contains(order.status)) {
            return BigDecimal.ZERO
        }
        val fromMonth = PFDay.from(from).beginOfMonth
        val untilMonth = PFDay.from(until).beginOfMonth
        val baseMonth = PFDay.fromOrNow(order.snapshotDate).beginOfMonth
        var sum = BigDecimal.ZERO
        order.infoPositions?.forEach { pos ->
            if (ForecastUtils.isCompletedAndOpen(order, pos, baseMonth) ||
                pos.status in ForecastUtils.auftragsPositionsStatusToShow
            ) {
                calculate(order, pos).months.forEach { month ->
                    if (month.date >= fromMonth && month.date <= untilMonth) {
                        sum += month.toBeInvoicedSum
                    }
                }
            }
        }
        return sum
    }
}
