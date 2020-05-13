/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.liquidityplanning

import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

class LiquidityForecastSettings : Serializable {
    var baseDate: LocalDate? = null
        get() {
            field?.let {
                if (it.isBefore(LocalDate.now())) {
                    return it
                }
            }
            return null
        }

    var startAmount: BigDecimal? = BigDecimal.ZERO
    var nextDays = 30

    /**
     * For calculating the expected date of payment all paid invoices of an debitor of the last n month are analyzed.
     *
     * @return the expectencyForRecentMonths
     */
    var expectencyForRecentMonths = 12

    companion object {
        private const val serialVersionUID = -6429410479048275707L
        const val MAX_FORECAST_DAYS = 600
        const val DEFAULT_FORECAST_DAYS = 600
    }
}
