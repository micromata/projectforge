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

@file:Suppress("DEPRECATION")

package org.projectforge.business.timesheet

import mu.KotlinLogging
import org.projectforge.business.user.UserPrefDao
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.favorites.Favorites
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserPrefArea
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class TimesheetFavoritesService {
  @Autowired
  private lateinit var userPrefDao: UserPrefDao

  @Autowired
  private lateinit var userPrefService: UserPrefService

  private class MigrationCache(val service: TimesheetFavoritesService) : AbstractCache() {
    // Key is user id, value is flag, if the user has entries to migrate.
    private var map = mutableMapOf<Long, Boolean>()

    fun refresh(userId: Long?) {
      userId ?: return
      synchronized(map) {
        map.remove(userId)
      }
    }

    fun hasLegacyFavoritesToMigrate(userId: Long?): Boolean {
      userId ?: return false
      synchronized(map) {
        map[userId]?.let { return it }
        // Not in cache:
        service.hasLegacyFavoritesToMigrate(userId).let {
          map[userId] = it
          return it
        }
      }
    }

    override fun refresh() {
      map = mutableMapOf()
    }
  }

  private val migrationCache = MigrationCache(this)

  fun getList(): List<TimesheetFavorite> {
    val favorites = getFavorites()
    return favorites.idTitleList.map { TimesheetFavorite(it.name, it.id) }
  }

  fun selectTimesheet(id: Long): TimesheetFavorite? {
    return getFavorites().get(id)
  }

  fun createFavorite(newFavorite: TimesheetFavorite) {
    getFavorites().add(newFavorite)
  }

  fun deleteFavorite(id: Long) {
    getFavorites().remove(id)
  }

  fun renameFavorite(id: Long, newName: String) {
    getFavorites().rename(id, newName)
  }

  /**
   * After modifying the user's favorites, the migration cache should be invalidated for the given user, so
   * it will be checked, if there are any favorites of the old (classical) version to migrate.
   */
  fun refreshMigrationCache(userId: Long) {
    migrationCache.refresh(userId)
  }

  // Ensures filter list (stored one, adopted from the formerly shared slot, restored from legacy filter or a empty new one).
  fun getFavorites(): Favorites<TimesheetFavorite> {
    var favorites: Favorites<TimesheetFavorite>? = null
    try {
      @Suppress("UNCHECKED_CAST", "USELESS_ELVIS")
      favorites = userPrefService.getEntry(
        PREF_AREA,
        Favorites.PREF_NAME_LIST,
        Favorites::class.java
      ) as Favorites<TimesheetFavorite>?
        ?: adoptFromCollidingArea()
        ?: migrateFromLegacyFavorites(Favorites())
    } catch (ex: Exception) {
      log.error("Exception while getting user preferred favorites: ${ex.message}. This might be OK for new releases. Ignoring filter.")
    }
    if (favorites == null) {
      // Creating empty filter list (user has no filter list yet):
      favorites = Favorites()
      userPrefService.putEntry(PREF_AREA, Favorites.PREF_NAME_LIST, favorites)
    }
    return favorites
  }

  /**
   * Historically, timesheet template favorites were stored under the same `(area, name)` slot as the react list's
   * filter favorites ([org.projectforge.rest.core.AbstractEntityRest.getFilterFavorites] uses the entity category
   * `"timesheet"` as area and [Favorites.PREF_NAME_LIST] as name). Both wrote a [Favorites] object into that single
   * row, so whichever list was saved last overwrote the other, and reading the slot with the wrong element type threw
   * a `ClassCastException` (`TimesheetFavorite` vs. `MagicFilter`). Template favorites now live in their own area
   * ([PREF_AREA]); this best-effort adoption salvages any [TimesheetFavorite] entries still present in the old shared
   * slot. Filter favorites (`MagicFilter`) stored there are ignored and left untouched.
   */
  private fun adoptFromCollidingArea(): Favorites<TimesheetFavorite>? {
    val legacy = try {
      userPrefService.getEntry(
        LEGACY_SHARED_AREA,
        Favorites.PREF_NAME_LIST,
        Favorites::class.java
      ) as? Favorites<*>
    } catch (ex: Exception) {
      log.error("Exception while reading legacy shared timesheet favorites: ${ex.message}. Ignoring.")
      null
    } ?: return null
    val adopted = Favorites<TimesheetFavorite>()
    var count = 0
    // Only public accessors are used, so MagicFilter entries are simply filtered out by the type check.
    legacy.idTitleList.forEach { idTitle ->
      (legacy.get(idTitle.id) as? TimesheetFavorite)?.let {
        adopted.add(it)
        count++
      }
    }
    if (count == 0) {
      // The old slot held only filter favorites (MagicFilter) -> nothing to adopt.
      return null
    }
    userPrefService.putEntry(PREF_AREA, Favorites.PREF_NAME_LIST, adopted)
    // The old shared slot held template favorites only (no MagicFilter filter favorites, otherwise count would be 0),
    // so clear it. This stops the react list from repeatedly filtering these foreign entries out.
    userPrefService.putEntry(LEGACY_SHARED_AREA, Favorites.PREF_NAME_LIST, Favorites<TimesheetFavorite>())
    log.info("Adopted $count legacy timesheet template favorite(s) from the shared '$LEGACY_SHARED_AREA' pref slot into '$PREF_AREA'.")
    return adopted
  }

  /**
   * Will not overwrite any existing new favorite (with name same).
   */
  fun migrateFromLegacyFavorites(currentFavorites: Favorites<TimesheetFavorite>): Favorites<TimesheetFavorite>? {
    val list = userPrefDao.selectUserPrefs(UserPrefArea.TIMESHEET_TEMPLATE)
    if (list.isNullOrEmpty())
      return null
    var modified = false
    for (userPref in list) {
      if (currentFavorites.get(userPref.name) == null) {
        val timesheet = TimesheetDO()
        userPrefDao.fillFromUserPrefParameters(userPref, timesheet)
        val favorite = TimesheetFavorite(userPref.name)
        favorite.fillFromTimesheet(timesheet)
        currentFavorites.add(favorite)
        modified = true
      }
    }
    if (modified) {
      userPrefService.putEntry(PREF_AREA, Favorites.PREF_NAME_LIST, currentFavorites)
      migrationCache.refresh(ThreadLocalUserContext.loggedInUserId)
    }
    return currentFavorites
  }

  fun hasLegacyFavoritesToMigrate(): Boolean {
    return migrationCache.hasLegacyFavoritesToMigrate(ThreadLocalUserContext.loggedInUserId)
  }

  private fun hasLegacyFavoritesToMigrate(userId: Long): Boolean {
    val list = userPrefDao.selectUserPrefs(userId, UserPrefArea.TIMESHEET_TEMPLATE)
    if (list.isNullOrEmpty())
      return false
    val favorites = getFavorites()
    for (userPref in list) {
      if (favorites.get(userPref.name) == null) {
        // Found old favorite, which has no entry in new favorites.
        return true
      }
    }
    return false
  }

  companion object {
    /**
     * Dedicated pref area for the timesheet edit-form template favorites. Formerly `"timesheet"`, which collided with
     * the react list's filter favorites (see [adoptFromCollidingArea]).
     */
    private const val PREF_AREA = "timesheetTemplateFavorites"

    /**
     * The area the template favorites historically shared with the react list's filter favorites (the entity category
     * of [org.projectforge.rest.TimesheetPagesRest]). Read-only, for one-time adoption of orphaned template favorites.
     */
    private const val LEGACY_SHARED_AREA = "timesheet"
  }
}
