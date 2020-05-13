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

import org.projectforge.business.fibu.AmountType
import org.projectforge.business.fibu.PaymentStatus
import org.projectforge.framework.persistence.api.BaseSearchFilter
import java.io.Serializable
import java.time.LocalDate

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class LiquidityFilter(filter: BaseSearchFilter? = null) : BaseSearchFilter(filter), Serializable {
    /**
     * Base date is normally today. May be set to dates in the past for comparisons.
     */
    var baseDate: LocalDate? = null
        set(value) {
            if (value == null || value.isBefore(LocalDate.now())) {
                field = value
            } else {
                field = null
            }
        }

    /**
     * @return the nextDays
     */
    var nextDays = 0

    /**
     * @return the paymentStatus
     */
    var paymentStatus: PaymentStatus? = PaymentStatus.ALL
        set(value) {
            field = value ?: PaymentStatus.ALL
        }

    /**
     * @return the amountType
     */
    var amountType: AmountType? = AmountType.ALL
        set(value) {
            field = value ?: AmountType.ALL

        }
}
