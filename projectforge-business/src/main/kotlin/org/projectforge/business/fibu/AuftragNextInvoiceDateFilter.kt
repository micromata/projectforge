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
import org.projectforge.framework.time.PFDay
import java.time.LocalDate

/**
 * Matches orders whose next invoice is due within the given period: the date of the earliest reached payment
 * schedule not yet invoiced ([OrderInfo.nextInvoiceDate]), or today for orders to be invoiced immediately
 * ([OrderInfo.toBeInvoicedImmediately]). Orders with nothing to invoice never match.
 * @param from First day of the period (inclusive), or null for no lower bound.
 * @param until Last day of the period (inclusive), or null for no upper bound.
 */
class AuftragNextInvoiceDateFilter(
    val from: LocalDate?,
    val until: LocalDate?,
    private val today: LocalDate = PFDay.now().localDate,
) : CustomResultFilter<AuftragDO> {
    override fun match(list: MutableList<AuftragDO>, element: AuftragDO): Boolean {
        return match(AuftragsCache.instance.getOrderInfo(element))
    }

    fun match(orderInfo: OrderInfo): Boolean {
        val date = effectiveDate(orderInfo, today) ?: return false
        return (from == null || !date.isBefore(from)) && (until == null || !date.isAfter(until))
    }

    companion object {
        /**
         * @return The day the next invoice of the order is due: today if due immediately, otherwise
         * [OrderInfo.nextInvoiceDate]. Null if nothing is to be invoiced.
         */
        fun effectiveDate(orderInfo: OrderInfo, today: LocalDate): LocalDate? {
            if (!orderInfo.toBeInvoiced) {
                return null
            }
            return if (orderInfo.toBeInvoicedImmediately) today else orderInfo.nextInvoiceDate
        }
    }
}
