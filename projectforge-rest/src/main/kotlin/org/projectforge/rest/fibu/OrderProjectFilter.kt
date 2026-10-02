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
import org.projectforge.business.fibu.OrderInfo
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.framework.utils.StringComparator
import org.projectforge.ui.filter.UIFilterListValue

/**
 * The project filter of the order book: the orders of any of the chosen projects (`AuftragDO.projekt`).
 * The projects travel as their ids in `MagicFilterEntry.value.values`.
 */
internal object OrderProjectFilter {
  /** Id of the filter entry, a pseudo field (see [addCriterion]). */
  const val FIELD = "projects"

  /** Endpoint of the selectable projects, relative to `/rs/` (see `OrderEntityRest.projectFilterValues`). */
  const val VALUES_URL = "order/projectFilterValues"

  /**
   * The projects of the given orders, each once, sorted by name as the list's project cell shows it.
   */
  fun valuesOf(infos: Sequence<OrderInfo>): List<UIFilterListValue> {
    return infos.mapNotNullTo(mutableSetOf()) { it.projektId }
      .map { id -> UIFilterListValue(id.toString(), PfCaches.instance.getProjekt(id)?.displayName ?: id.toString()) }
      .sortedWith { a, b -> StringComparator.compare(a.displayName, b.displayName) }
  }

  /**
   * Turns the [FIELD] entry into `projekt.id IN (…)`. Synthetic, because the field is no property of
   * `AuftragDO`, so `MagicFilterProcessor` cannot derive it. An entry without a known id filters nothing.
   */
  fun addCriterion(target: QueryFilter, source: MagicFilter) {
    val entry = source.entries.find { it.field == FIELD } ?: return
    entry.synthetic = true
    buildPredicate(entry.value.values)?.let { target.add(it) }
  }

  internal fun buildPredicate(keys: Array<String>?): DBPredicate? {
    val ids = keys?.mapNotNull { it.toLongOrNull() }?.takeIf { it.isNotEmpty() } ?: return null
    return DBPredicate.IsIn("projekt.id", ids)
  }
}
