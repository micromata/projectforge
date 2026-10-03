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

/**
 * A project as far as the business-unit rules ask: its customer, its task, and the customers it is worked for
 * (see [CustomerUsage.projectCustomers]): its own customer entity, else the customers of its orders.
 */
data class ProjectRef(
    val kundeId: Long?,
    val taskId: Long?,
    val customers: List<CustomerKey> = listOfNotNull(kundeId?.let { CustomerKey(it) }),
)

/**
 * What the rules of the customer groups are applied to, taken from the caches (`CustomerGroupService`): the
 * customer entities by name, the projects by task, and for the validation the free-text customers in use.
 *
 * @param customers The customer entities, number -> name.
 * @param freeTexts The `kundeText` of orders and invoices without a customer entity, distinct. Only the
 * validation needs them, to find a free text two groups claim.
 * @param projects The projects, id -> customer and task.
 * @param taskPath The ids of a task's ancestors and of the task itself, root first; null for an unknown task.
 * @param taskTitle A task's title, for the validation's messages.
 */
class CustomerDirectory(
    val customers: Map<Long, String?> = emptyMap(),
    val freeTexts: Collection<String> = emptyList(),
    val projects: Map<Long, ProjectRef> = emptyMap(),
    val taskPath: (Long) -> List<Long>? = { null },
    val taskTitle: (Long) -> String? = { null },
) {
    companion object {
        val EMPTY = CustomerDirectory()
    }
}
