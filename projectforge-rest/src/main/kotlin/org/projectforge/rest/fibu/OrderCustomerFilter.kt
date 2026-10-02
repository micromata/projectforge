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
 * The customer filter of the order book: the orders of any of the chosen customers.
 *
 * A customer is either a customer entity (`AuftragDO.kunde`) or, for an order naming a customer that is
 * not in the customer list, the free text `AuftragDO.kundeText`, as the list's customer cell shows it.
 * Both travel as keys in `MagicFilterEntry.value.values`: [ENTITY_PREFIX] plus the customer's number, or
 * [TEXT_PREFIX] plus the free text.
 */
internal object OrderCustomerFilter {
  /** Id of the filter entry, a pseudo field (see [addCriterion]). */
  const val FIELD = "customers"

  /** Endpoint of the selectable customers, relative to `/rs/` (see `OrderEntityRest.customerFilterValues`). */
  const val VALUES_URL = "order/customerFilterValues"

  private const val ENTITY_PREFIX = "k:"
  private const val TEXT_PREFIX = "t:"

  /**
   * The customers of the given orders, each once, sorted by name. A free-text customer only counts for an
   * order without a customer entity: with one, the cell shows the entity and so does this list.
   */
  fun valuesOf(infos: Sequence<OrderInfo>): List<UIFilterListValue> {
    val customerIds = mutableSetOf<Long>()
    val texts = mutableSetOf<String>()
    infos.forEach { info ->
      val kundeId = info.kundeId
      if (kundeId != null) {
        customerIds.add(kundeId)
      } else {
        // Without a customer entity, kundeAsString is the free text itself (see KundeFormatter).
        info.kundeAsString?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
      }
    }
    val entities = customerIds.map { id ->
      UIFilterListValue(ENTITY_PREFIX + id, PfCaches.instance.getKunde(id)?.displayName ?: id.toString())
    }
    val freeTexts = texts.map { UIFilterListValue(TEXT_PREFIX + it, it.trim(), freeText = true) }
    return (entities + freeTexts).sortedWith { a, b -> StringComparator.compare(a.displayName, b.displayName) }
  }

  /**
   * Turns the [FIELD] entry into `kunde IN (…) OR (kunde IS NULL AND kundeText IN (…))`. Synthetic, because
   * the field is no property of `AuftragDO`, so `MagicFilterProcessor` cannot derive it. An entry without
   * a known key filters nothing.
   */
  fun addCriterion(target: QueryFilter, source: MagicFilter) {
    val entry = source.entries.find { it.field == FIELD } ?: return
    entry.synthetic = true
    buildPredicate(entry.value.values)?.let { target.add(it) }
  }

  internal fun buildPredicate(keys: Array<String>?): DBPredicate? {
    keys ?: return null
    val ids = keys.filter { it.startsWith(ENTITY_PREFIX) }.mapNotNull { it.removePrefix(ENTITY_PREFIX).toLongOrNull() }
    val texts = keys.filter { it.startsWith(TEXT_PREFIX) }.map { it.removePrefix(TEXT_PREFIX) }
    val byEntity = ids.takeIf { it.isNotEmpty() }?.let { DBPredicate.IsIn("kunde.id", it) }
    val byText = texts.takeIf { it.isNotEmpty() }?.let {
      DBPredicate.And(DBPredicate.IsNull("kunde"), DBPredicate.IsIn("kundeText", it))
    }
    return when {
      byEntity != null && byText != null -> DBPredicate.Or(byEntity, byText)
      else -> byEntity ?: byText
    }
  }
}
