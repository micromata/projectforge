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

package org.projectforge.business.fibu.customergroup

import java.time.LocalDate

/**
 * A customer as a row names it: a customer entity by number or, without one, a free text (trimmed).
 */
data class CustomerKey(val kundeId: Long?, val kundeText: String? = null) {
    companion object {
        /** The entity if given, else the trimmed free text; null for neither. */
        fun of(kundeId: Long?, kundeText: String?): CustomerKey? =
            if (kundeId != null) CustomerKey(kundeId) else kundeText?.trim()?.takeIf { it.isNotEmpty() }?.let { CustomerKey(null, it) }
    }
}

/**
 * Where customers are actually worked for: the customers of the orders, by project, and the projects booked on
 * by timesheets. Only these count as customers in the customer-group editor, so customers known only from
 * invoices (debtors) stay out of it. Built by [CustomerUsageCache].
 *
 * @param orders Per customer of an order and its project (null: an order of none), the latest order's date
 * (offer date, else entry date).
 * @param sheets Per project, the date of its latest timesheet.
 */
class CustomerUsage(
    val orders: Map<Pair<CustomerKey, Long?>, LocalDate> = emptyMap(),
    val sheets: Map<Long, LocalDate> = emptyMap(),
) {
    /**
     * The customers of each project: its own customer entity if it has one, else those of its orders, the most
     * recent first, a free text once regardless of its case. Projects with neither are left out.
     *
     * @param projectKunde The projects, id -> number of its customer entity.
     */
    fun projectCustomers(projectKunde: Map<Long, Long?>): Map<Long, List<CustomerKey>> {
        val byOrders = mutableMapOf<Long, MutableList<Pair<CustomerKey, LocalDate>>>()
        orders.forEach { (key, date) ->
            key.second?.let { projektId -> byOrders.getOrPut(projektId) { mutableListOf() }.add(key.first to date) }
        }
        return projectKunde.keys.mapNotNull { projektId ->
            val customers = projectKunde[projektId]?.let { listOf(CustomerKey(it)) }
                ?: byOrders[projektId]
                    ?.sortedByDescending { it.second }
                    ?.map { it.first }
                    ?.distinctBy { it.kundeId?.toString() ?: "t:${it.kundeText?.lowercase()}" }
            customers?.takeIf { it.isNotEmpty() }?.let { projektId to it }
        }.toMap()
    }

    /**
     * The year each customer was last worked for, per project (null: orders of none): by the orders, and by the
     * timesheets of the projects, which count for the project's customers (see [projectCustomers]).
     */
    fun years(projectCustomers: Map<Long, List<CustomerKey>>): Map<Pair<CustomerKey, Long?>, Int> {
        val years = mutableMapOf<Pair<CustomerKey, Long?>, Int>()
        orders.forEach { (key, date) -> years.merge(key, date.year, ::maxOf) }
        sheets.forEach { (projektId, date) ->
            projectCustomers[projektId]?.forEach { years.merge(it to projektId, date.year, ::maxOf) }
        }
        return years
    }

    companion object {
        val EMPTY = CustomerUsage()
    }
}
