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

import org.projectforge.business.fibu.customergroup.CustomerGroupIndex
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.utils.StringComparator
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.filter.UIFilterListElement
import org.projectforge.ui.filter.UIFilterListValue

/**
 * The business-unit checklist of a list with a customer checklist ([CustomerChecklistFilter]): the rows of any
 * of the chosen business units, each standing for its customers and those of its groups, and for the projects
 * below its tasks where the row's customer has no business unit (see `CustomerGroupIndex`). Keys are [PREFIX] plus the business unit's key. Offered only if business units are
 * configured.
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

    /** Adds the pinned element (as the customer checklist's), if any business unit is configured. */
    fun addElement(elements: MutableList<UILabelledElement>) {
        if (groupIndex().businessUnits.isEmpty()) {
            return
        }
        elements.add(UIFilterListElement(FIELD, label = translate("fibu.businessUnit"), multi = true).also {
            it.valuesUrl = valuesUrl
            it.defaultFilter = true
        })
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
        val resolved = keys.filter { it.startsWith(PREFIX) }
            .mapNotNull { index.resolveBusinessUnit(it.removePrefix(PREFIX)) }
            .takeIf { it.isNotEmpty() }
            ?.reduce { acc, it -> acc + it }
            ?: return null
        return paths.predicate(resolved, index.businessUnitCustomers)
    }

    /** The business units of the given rows (by customer, else by project), each once, sorted by name. */
    fun valuesOf(customers: Sequence<CustomerRow>): List<UIFilterListValue> {
        val index = groupIndex()
        if (index.businessUnits.isEmpty()) {
            return emptyList()
        }
        return customers.mapNotNull { index.businessUnitOf(it.kundeId, it.kundeText, it.projektId) }
            .distinct()
            .map { UIFilterListValue(PREFIX + it.key, it.name ?: it.key!!) }
            .sortedWith { a, b -> StringComparator.compare(a.displayName, b.displayName) }
            .toList()
    }

    companion object {
        /** Id of the filter entry, a pseudo field (see [addCriterion]). */
        const val FIELD = "businessUnits"

        private const val PREFIX = "b:"
    }
}
