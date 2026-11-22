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

import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import java.time.LocalDate

/**
 * Matches orders for which the forecast expects something to be invoiced in the given period, see
 * [ForecastPeriodSums.sumInPeriod].
 * @param from First day of the period; the whole month counts.
 * @param until Last day of the period; the whole month counts.
 */
class AuftragForecastPeriodFilter(
    val from: LocalDate,
    val until: LocalDate,
    private val forecastPeriodSums: ForecastPeriodSums,
) : CustomResultFilter<AuftragDO> {
    override fun match(list: MutableList<AuftragDO>, element: AuftragDO): Boolean {
        val orderInfo = AuftragsCache.instance.getOrderInfo(element) ?: return false
        return forecastPeriodSums.sumInPeriod(orderInfo, from, until).signum() != 0
    }
}
