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
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.utils.StringComparator
import org.projectforge.ui.filter.UIFilterListElement
import org.projectforge.ui.filter.UIFilterListValue

/**
 * The project checklist of a list (order book, invoices, cost 2, timesheets): the rows of any of the chosen
 * projects, offered as the rows the list's other criteria match (see `AbstractEntityRest.checklistFilter`).
 * The projects travel as their ids in `MagicFilterEntry.value.values`.
 *
 * @param valuesUrl Endpoint of the selectable projects, relative to `/rs/`.
 * @param path The id property the picked projects are matched against: `projekt.id`, or `kost2.id` for
 *   timesheets, which reach their project via the cost 2.
 * @param idsOf Maps the picked project ids to the ids [path] holds: the project ids themselves, or the
 *   cost 2 of the projects.
 */
internal class ProjectChecklistFilter(
    private val valuesUrl: String,
    private val path: String = "projekt.id",
    private val idsOf: (List<Long>) -> List<Long> = { it },
) {
    /** Pinned (`defaultFilter`): always on the filter row, not only once picked from the field list. */
    fun element(): UIFilterListElement =
        UIFilterListElement(FIELD, label = translate("fibu.projekt"), multi = true).also {
            it.valuesUrl = valuesUrl
            it.defaultFilter = true
        }

    /**
     * Turns the [FIELD] entry into `projekt.id IN (…)`. Synthetic, because the field is no property of the
     * entity, so `MagicFilterProcessor` cannot derive it. An entry without a known id filters nothing.
     */
    fun addCriterion(target: QueryFilter, source: MagicFilter) {
        val entry = source.entries.find { it.field == FIELD } ?: return
        entry.synthetic = true
        buildPredicate(entry.value.values)?.let { target.add(it) }
    }

    internal fun buildPredicate(keys: Array<String>?): DBPredicate? {
        val projectIds = keys?.mapNotNull { it.toLongOrNull() }?.takeIf { it.isNotEmpty() } ?: return null
        val ids = idsOf(projectIds).takeIf { it.isNotEmpty() } ?: return null
        return DBPredicate.IsIn(path, ids)
    }

    companion object {
        /** Id of the filter entry, a pseudo field (see [addCriterion]). */
        const val FIELD = "projects"

        /** The given projects, each once, sorted by name as the lists' project cells show it. */
        fun valuesOf(projectIds: Sequence<Long?>): List<UIFilterListValue> {
            return projectIds.filterNotNullTo(mutableSetOf())
                .map { id -> UIFilterListValue(id.toString(), PfCaches.instance.getProjekt(id)?.displayName ?: id.toString()) }
                .sortedWith { a, b -> StringComparator.compare(a.displayName, b.displayName) }
        }
    }
}
