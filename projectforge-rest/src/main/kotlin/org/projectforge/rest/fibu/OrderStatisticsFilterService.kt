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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.favorites.Favorites
import org.projectforge.framework.persistence.api.MagicFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The filter of the order statistics page (`/next/orderStatistics`: forecast charts and contribution margin)
 * and its favorites, stored per user in an area of their own ([AREA]), so they are independent of the order
 * book's filter and favorites (`order` area, see `AbstractEntityRest.getFilterFavorites`).
 *
 * The filter only ever holds business units, customers and projects ([statisticsFilter]): every filter
 * stored or passed on is reduced to these first.
 */
@Service
class OrderStatisticsFilterService {
  @Autowired
  private lateinit var userPrefService: UserPrefService

  /** The filter the page showed last, empty if never used. */
  fun getCurrentFilter(): MagicFilter {
    val filter = userPrefService.getEntry(AREA, Favorites.PREF_NAME_CURRENT, MagicFilter::class.java)
      ?: return MagicFilter()
    filter.init()
    return statisticsFilter(filter)
  }

  /** Stores the reduced [filter] as current one and returns it. */
  fun saveCurrentFilter(filter: MagicFilter): MagicFilter {
    val current = statisticsFilter(filter)
    userPrefService.putEntry(AREA, Favorites.PREF_NAME_CURRENT, current, true)
    return current
  }

  /**
   * The user's favorites, created empty on first use. A cached object: changed in place, it is written by the
   * user pref cache, which detects the change by its serialized form.
   */
  fun getFavorites(): Favorites<MagicFilter> {
    try {
      @Suppress("UNCHECKED_CAST")
      (userPrefService.getEntry(AREA, Favorites.PREF_NAME_LIST, Favorites::class.java) as? Favorites<MagicFilter>)
        ?.let { return it }
    } catch (ex: Exception) {
      log.error("Exception while getting the order statistics favorites: ${ex.message}. Starting with none.")
    }
    return Favorites<MagicFilter>().also { userPrefService.putEntry(AREA, Favorites.PREF_NAME_LIST, it, true) }
  }

  companion object {
    /** User pref area of the filter and its favorites. */
    internal const val AREA = "orderStatistics"

    /** The filter fields of the statistics page. */
    internal val FIELDS = setOf(
      BusinessUnitChecklistFilter.FIELD,
      CustomerChecklistFilter.FIELD,
      ProjectChecklistFilter.FIELD,
    )

    /**
     * A copy of [magicFilter] with nothing but its business unit, customer and project criteria — whatever
     * else a client sends (or the order book's filter holds, see `OrderStatisticsRest.getMeta`) is dropped.
     * Name and id are kept: they tell which favorite the filter came from.
     */
    internal fun statisticsFilter(magicFilter: MagicFilter?): MagicFilter {
      val filter = MagicFilter(name = magicFilter?.name, id = magicFilter?.id)
      magicFilter?.entries?.filter { it.field in FIELDS && it.isCriterion }?.forEach { filter.entries.add(it) }
      return filter.clone() // Deep copy: the entries must not be shared with the source.
    }
  }
}
