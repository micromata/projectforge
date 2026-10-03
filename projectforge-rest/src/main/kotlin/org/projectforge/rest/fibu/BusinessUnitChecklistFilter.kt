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

import org.projectforge.business.fibu.customergroup.BusinessUnit
import org.projectforge.business.fibu.customergroup.CustomerGroupIndex
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.utils.StringComparator
import org.projectforge.ui.filter.UIFilterListElement
import org.projectforge.ui.filter.UIFilterListValue

/**
 * The business-unit checklist of a list with a customer checklist ([CustomerChecklistFilter]): the rows of any
 * of the chosen business units, each standing for its customers and those of its groups, and for the projects
 * below its tasks where the row's customer has no business unit (see `CustomerGroupIndex`). Keys are [PREFIX] plus the business unit's key. Offered only if business units are
 * configured.
 *
 * The rows of no business unit are offered as one more, [NONE_KEY] ("Sonstige"): the remainder, so the business
 * units together with it cover every row.
 *
 * @param valuesUrl Endpoint of the selectable business units, relative to `/rs/`.
 */
internal class BusinessUnitChecklistFilter(
    private val valuesUrl: String,
    kundePath: String = "kunde",
    kundeTextPath: String? = "kundeText",
    projektIdPath: String? = "projekt.id",
    private val groupIndex: () -> CustomerGroupIndex = CustomerPaths.defaultIndex,
) {
    private val paths = CustomerPaths(kundePath, kundeTextPath, projektIdPath)

    /** The pinned element (as the customer checklist's), or null if no business unit is configured. */
    fun element(): UIFilterListElement? {
        if (groupIndex().businessUnits.isEmpty()) {
            return null
        }
        return UIFilterListElement(FIELD, label = translate("fibu.businessUnit"), multi = true).also {
            it.valuesUrl = valuesUrl
            it.defaultFilter = true
        }
    }

    /** Turns the [FIELD] entry into the criterion on the business units' customers, synthetic as [FIELD] is. */
    fun addCriterion(target: QueryFilter, source: MagicFilter) {
        val entry = source.entries.find { it.field == FIELD } ?: return
        entry.synthetic = true
        buildPredicate(entry.value.values)?.let { target.add(it) }
    }

    /**
     * Keys of business units no longer configured are ignored. If none is left of a non-empty pick, nothing is
     * filtered, as with unknown customer keys.
     */
    internal fun buildPredicate(keys: Array<String>?): DBPredicate? {
        keys ?: return null
        val index = groupIndex()
        val byBusinessUnit = keys.filter { it.startsWith(PREFIX) }
            .mapNotNull { index.resolveBusinessUnit(it.removePrefix(PREFIX)) }
            .takeIf { it.isNotEmpty() }
            ?.reduce { acc, it -> acc + it }
            ?.let { paths.predicate(it, index.businessUnitCustomers) }
        val withoutBusinessUnit = if (NONE_KEY in keys && !index.businessUnits.isEmpty()) {
            paths.withoutBusinessUnit(index.businessUnitCustomers, index.businessUnitProjects)
        } else {
            null
        }
        return when {
            byBusinessUnit != null && withoutBusinessUnit != null -> DBPredicate.Or(byBusinessUnit, withoutBusinessUnit)
            else -> byBusinessUnit ?: withoutBusinessUnit
        }
    }

    /**
     * The projects the picks stand for, for a list reaching its customer only through the project (see
     * [ViaProjectCriteria]): those whose customers or task lead to a picked business unit (one per project, see
     * `CustomerGroupIndex.businessUnitOf(customers, projektId)`), and with [NONE_KEY]
     * those leading to none, and the rows of no project. Null if no key is known, as [buildPredicate].
     */
    fun projectMatch(keys: Array<String>?): ProjectMatch? {
        keys ?: return null
        val index = groupIndex()
        val picked = keys.filter { it.startsWith(PREFIX) }.mapNotNull { index.getBusinessUnit(it.removePrefix(PREFIX)) }
        val none = NONE_KEY in keys && index.businessUnits.isNotEmpty()
        if (picked.isEmpty() && !none) {
            return null
        }
        return ProjectMatch(withoutProject = none) { customers, projektId ->
            val bu = index.businessUnitOf(customers, projektId)
            if (bu == null) none else picked.any { it === bu }
        }
    }

    /**
     * The business units of the given rows (by customer, else by project), each once, sorted by name, followed by
     * [NONE_KEY] if a row has none.
     */
    fun valuesOf(customers: Sequence<CustomerRow>): List<UIFilterListValue> {
        val index = groupIndex()
        if (index.businessUnits.isEmpty()) {
            return emptyList()
        }
        return valuesOf(customers.map { index.businessUnitOf(it.kundeId, it.kundeText, it.projektId) }.toSet())
    }

    /**
     * The business units of the given rows' projects (see [projectMatch]), each once, sorted by name, followed by
     * [NONE_KEY] if a row has none (also a row of no project).
     */
    fun valuesOfProjects(projektIds: Sequence<Long?>): List<UIFilterListValue> {
        val index = groupIndex()
        if (index.businessUnits.isEmpty()) {
            return emptyList()
        }
        return valuesOf(projektIds.distinct().map { it?.let { id -> index.businessUnitOfProject(id) } }.toSet())
    }

    private fun valuesOf(businessUnits: Set<BusinessUnit?>): List<UIFilterListValue> {
        val values = businessUnits.filterNotNull()
            .map { UIFilterListValue(PREFIX + it.key, it.name ?: it.key!!) }
            .sortedWith { a, b -> StringComparator.compare(a.displayName, b.displayName) }
        if (null !in businessUnits) {
            return values
        }
        return values + UIFilterListValue(NONE_KEY, translate("fibu.businessUnits.none"))
    }

    companion object {
        /** Id of the filter entry, a pseudo field (see [addCriterion]). */
        const val FIELD = "businessUnits"

        private const val PREFIX = "b:"

        /** The rows of no business unit: a key no business unit has, theirs being six characters long. */
        const val NONE_KEY = "${PREFIX}none"
    }
}
