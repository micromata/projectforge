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


package org.projectforge.rest.fibu

import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.customergroup.CustomerGroupIndex
import org.projectforge.business.fibu.customergroup.CustomerKey
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate

/**
 * The projects the picks of a checklist stand for, by the project's customers (its own, else those of its
 * orders, see `CustomerGroupIndex.projectCustomers`) and its id.
 *
 * @param withoutProject The rows of no project as well (the business units' remainder, "Sonstige").
 */
internal class ProjectMatch(
    val withoutProject: Boolean = false,
    val matches: (customers: List<CustomerKey>, projektId: Long) -> Boolean,
)

/**
 * The customer and business-unit checklists of a list reaching its customer only through the project
 * (timesheets: `kost2.projekt`, cost 2: `projekt`, the projects themselves). The picks are resolved to the
 * projects they stand for, and these to the ids [path] holds, as the project checklist does it
 * ([ProjectChecklistFilter]): a search matched in memory then resolves no nested path per row.
 *
 * A project without a customer entity is matched by the customers of its orders, free texts included: its
 * timesheets count for the customer the orders were placed by.
 *
 * @param path The id property matched: `kost2.id` for timesheets.
 * @param nullPath The reference of [path], missing for a row of no project: `kost2`; null if every row has one.
 * @param projects All projects, id to their customers (empty for none).
 * @param idsOf Maps project ids to the ids [path] holds (their cost 2).
 * @param idsWithoutProject The ids [path] holds that belong to no project (a cost 2 without one).
 */
internal class ViaProjectCriteria(
    private val path: String,
    private val nullPath: String?,
    private val projects: () -> Map<Long, List<CustomerKey>>,
    private val idsOf: (List<Long>) -> List<Long>,
    private val idsWithoutProject: () -> List<Long>,
) {
    /** Turns the [field] entry into the criterion of [predicate], synthetic as the checklists' fields are. */
    fun addCriterion(
        target: QueryFilter,
        source: MagicFilter,
        field: String,
        matchOf: (Array<String>?) -> ProjectMatch?,
    ) {
        val entry = source.entries.find { it.field == field } ?: return
        entry.synthetic = true
        matchOf(entry.value.values)?.let { target.add(predicate(it)) }
    }

    /** `path IN (ids of the matching projects)`, or `nullPath IS NULL` too for [ProjectMatch.withoutProject]. */
    fun predicate(match: ProjectMatch): DBPredicate {
        val projektIds = projects().filter { (projektId, customers) -> match.matches(customers, projektId) }.keys
        val ids = idsOf(projektIds.toList()) + if (match.withoutProject) idsWithoutProject() else emptyList()
        val byIds = ids.takeIf { it.isNotEmpty() }?.let { DBPredicate.IsIn(path, it.distinct()) }
        val withoutProject = if (match.withoutProject) nullPath?.let { DBPredicate.IsNull(it) } else null
        return when {
            byIds != null && withoutProject != null -> DBPredicate.Or(byIds, withoutProject)
            // A pick of known keys whose projects have no rows matches none, it doesn't filter nothing.
            else -> byIds ?: withoutProject ?: DBPredicate.IsIn(path, listOf(NO_ID))
        }
    }

    private companion object {
        /** An id no row holds (ids are positive). */
        const val NO_ID = -1L
    }
}

/**
 * The customers of each of the [projects], for [ViaProjectCriteria]: those the [index] knows (the project's own,
 * else its orders', see `CustomerGroupIndex.projectCustomers`), else its own customer entity (outside a Spring
 * context, where there is no index).
 */
internal fun projectCustomersOf(
    projects: Map<Long, ProjektDO>,
    caches: PfCaches,
    index: CustomerGroupIndex = CustomerPaths.defaultIndex(),
): Map<Long, List<CustomerKey>> = projects.mapValues { (id, projekt) ->
    index.projectCustomers[id]
        ?: listOfNotNull(caches.getKundeIfNotInitialized(projekt.kunde)?.nummer?.let { CustomerKey(it) })
}

/**
 * The rows offered by the customer checklist for a row of the given project: one per customer of the project
 * (see [projectCustomersOf]), or one without a customer.
 */
internal fun customerRowsOf(projektId: Long?, projectCustomers: Map<Long, List<CustomerKey>>): List<CustomerRow> =
    projektId?.let { projectCustomers[it] }?.takeIf { it.isNotEmpty() }
        ?.map { CustomerRow(it.kundeId, it.kundeText, projektId) }
        ?: listOf(CustomerRow(null, null, projektId))
