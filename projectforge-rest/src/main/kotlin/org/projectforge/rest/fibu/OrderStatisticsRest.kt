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

import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.contributionmargin.ContributionMarginService
import org.projectforge.favorites.Favorites
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The filter and the favorites of the order statistics page (`/next/finance/statistics`), see
 * [OrderStatisticsFilterService]. The charts themselves are computed by [OrderEntityRest.forecastChart] and
 * [OrderEntityRest.contributionMargin], which store the filter they are asked for as the current one.
 *
 * Every endpoint requires select access to the order book, as the page shows nothing but orders the user
 * may see; the contribution margin needs [ContributionMarginService.checkAccess] on top (see
 * [OrderStatisticsMeta.contributionMargin]). Filter and favorites are the logged-in user's own prefs.
 */
@RestController
@RequestMapping("${Rest.URL}/orderStatistics")
class OrderStatisticsRest {
  @Autowired
  private lateinit var auftragDao: AuftragDao

  @Autowired
  private lateinit var contributionMarginService: ContributionMarginService

  @Autowired
  private lateinit var filterService: OrderStatisticsFilterService

  @Autowired
  private lateinit var orderEntityRest: OrderEntityRest

  /**
   * What the page needs before the charts: the filter fields, the current filter and the favorites.
   *
   * @param fromOrderBook True, if the page was opened from the order book: the business units, customers
   *   and projects of the order book's current filter replace the current statistics filter (once; the
   *   order book's filter itself stays untouched).
   */
  @AccessChecked("DAO: select access (hasLoggedInUserSelectAccess); own user prefs")
  @GetMapping("meta")
  fun getMeta(@RequestParam("fromOrderBook", required = false) fromOrderBook: Boolean?): OrderStatisticsMeta {
    checkAccess()
    val filter = if (fromOrderBook == true) {
      // No favorite: the adopted criteria are the order book's.
      filterService.saveCurrentFilter(orderEntityRest.getCurrentFilter().also { it.id = null; it.name = null })
    } else {
      filterService.getCurrentFilter()
    }
    val favorites = filterService.getFavorites()
    return OrderStatisticsMeta(
      filterElements = OrderEntityRest.statisticsFilterElements(),
      filter = filter,
      favorite = filter.id?.let { favorites.get(it) }?.let { OrderStatisticsFilterService.statisticsFilter(it) },
      filterFavorites = favorites.idTitleList,
      contributionMargin = runCatching { contributionMarginService.hasAccess() }.getOrDefault(false),
    )
  }

  /** Makes the favorite the current filter. */
  @AccessChecked("DAO: select access (hasLoggedInUserSelectAccess); own user prefs")
  @PostMapping("filter/select")
  fun selectFavorite(@RequestParam("id") id: Long): FavoritesResponse {
    checkAccess()
    val favorites = filterService.getFavorites()
    val filter = favorites.get(id)?.let { filterService.saveCurrentFilter(it) } ?: filterService.getCurrentFilter()
    return FavoritesResponse(filter, favorites.idTitleList)
  }

  /** Saves [filter] under its name (a free one, if taken) as a new favorite and makes it the current filter. */
  @AccessChecked("DAO: select access (hasLoggedInUserSelectAccess); own user prefs")
  @PostMapping("filter/create")
  fun createFavorite(@RequestBody filter: MagicFilter): FavoritesResponse {
    checkAccess()
    val favorites = filterService.getFavorites()
    val favorite = OrderStatisticsFilterService.statisticsFilter(filter).also { it.id = null }
    favorites.add(favorite) // Assigns the id.
    return FavoritesResponse(filterService.saveCurrentFilter(favorite), favorites.idTitleList)
  }

  /** Overwrites the favorite of `filter.id` with the criteria of [filter], keeping its name. */
  @AccessChecked("DAO: select access (hasLoggedInUserSelectAccess); own user prefs")
  @PostMapping("filter/update")
  fun updateFavorite(@RequestBody filter: MagicFilter): FavoritesResponse {
    checkAccess()
    val favorites = filterService.getFavorites()
    val existing = favorites.get(filter.id) ?: return FavoritesResponse(filterService.getCurrentFilter(), favorites.idTitleList)
    val favorite = OrderStatisticsFilterService.statisticsFilter(filter).also { it.name = existing.name }
    favorites.remove(existing.id!!) // Not null: found by its id.
    favorites.add(favorite)
    return FavoritesResponse(filterService.saveCurrentFilter(favorite), favorites.idTitleList)
  }

  @AccessChecked("DAO: select access (hasLoggedInUserSelectAccess); own user prefs")
  @PostMapping("filter/rename")
  fun renameFavorite(@RequestParam("id") id: Long, @RequestParam("newName") newName: String): FavoritesResponse {
    checkAccess()
    val favorites = filterService.getFavorites()
    favorites.rename(id, newName)
    var current = filterService.getCurrentFilter()
    if (current.id == id) {
      current.name = favorites.get(id)?.name
      current = filterService.saveCurrentFilter(current)
    }
    return FavoritesResponse(current, favorites.idTitleList)
  }

  /** Deletes the favorite; the current filter keeps its criteria, but no longer refers to it. */
  @AccessChecked("DAO: select access (hasLoggedInUserSelectAccess); own user prefs")
  @PostMapping("filter/delete")
  fun deleteFavorite(@RequestParam("id") id: Long): FavoritesResponse {
    checkAccess()
    val favorites = filterService.getFavorites()
    favorites.remove(id)
    var current = filterService.getCurrentFilter()
    if (current.id == id) {
      current = filterService.saveCurrentFilter(current.also { it.id = null; it.name = null })
    }
    return FavoritesResponse(current, favorites.idTitleList)
  }

  private fun checkAccess() {
    auftragDao.hasLoggedInUserSelectAccess(throwException = true)
  }

  class OrderStatisticsMeta(
    /** The business unit (if any configured), customer and project filter. */
    val filterElements: List<UIFilterListElement>,
    val filter: MagicFilter,
    /** The favorite the current filter refers to (`filter.id`), as saved: tells whether the filter was modified since. */
    val favorite: MagicFilter?,
    val filterFavorites: List<Favorites.FavoriteIdTitle>,
    /** Whether the user may see the contribution margin ([ContributionMarginService.hasAccess]). */
    val contributionMargin: Boolean,
  )

  class FavoritesResponse(
    /** The current filter after the change. */
    val filter: MagicFilter,
    val filterFavorites: List<Favorites.FavoriteIdTitle>,
  )
}
