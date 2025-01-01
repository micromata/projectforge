/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.favorites

import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.projectforge.business.user.UserPrefDao
import org.projectforge.framework.i18n.addTranslations
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.UserPrefDO
import org.projectforge.framework.persistence.user.entities.UserPrefEntryDO

/**
 * Persist the user's set of favorites sorted by unique names. The user may configure a set of favorites and my apply one
 * by choosing from a drop down set.
 *
 * Ensures the uniqueness of favorite's names.
 */
open class Favorites<T : AbstractFavorite>() {

  constructor(list: List<T>) : this() {
    list.forEach {
      set.add(it)
    }
    fixNamesAndIds()
  }

  /**
   * For exports etc.
   */
  class FavoriteIdTitle(val id: Long, val name: String)

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
  private val set: MutableSet<T> = mutableSetOf()

  fun get(id: Long?): T? {
    if (id == null) return null
    fixNamesAndIds()
    return set.find { it.id == id }
  }

  fun get(name: String?): T? {
    if (name == null) return null
    fixNamesAndIds()
    return set.find { it.name == name }
  }

  fun add(favorite: T) {
    set.add(favorite)
    fixNamesAndIds()
  }

  fun remove(name: String) {
    fixNamesAndIds()
    set.removeIf { it.name == name }
  }

  fun remove(id: Long) {
    fixNamesAndIds()
    set.removeIf { it.id == id }
  }

  fun rename(id: Long, newName: String) {
    val entry = get(id)
    if (entry != null) {
      entry.name = newName
      fixNamesAndIds()
    } else {
      log.warn("Could not rename the user's filter. Filter with id '$id' not found for calendar.")
    }

  }

  fun createUserPrefLegacyEntry(
    userPrefDao: UserPrefDao,
    area: String,
    newFavorite: T,
    parameter: String,
    value: String
  ) {
    add(newFavorite) // If name is already given, a new name is set.
    val userPref = UserPrefDO()
    userPref.area = area
    userPref.user = ThreadLocalUserContext.loggedInUser
    userPref.name = newFavorite.name
    val userPrefEntry = UserPrefEntryDO()
    userPrefEntry.parameter = parameter
    userPrefEntry.value = value
    @Suppress("DEPRECATION")
    userPref.addOrUpdateUserPrefEntry(userPrefEntry)
    userPrefDao.insert(userPref)
  }

  fun createUserPref(userPrefDao: UserPrefDao, area: String, newFavorite: T) {
    add(newFavorite) // If name is already given, a new name is set.
    val userPref = UserPrefDO()
    userPref.area = area
    userPref.user = ThreadLocalUserContext.loggedInUser
    userPref.name = newFavorite.name
    userPref.id = newFavorite.id
    userPref.valueObject = newFavorite
    userPrefDao.insert(userPref)
  }

  /**
   * Fixes empty names and doublets of names.
   */
  private fun fixNamesAndIds() {
    val namesSet = mutableSetOf<String>()
    val idSet = mutableSetOf<Long>()
    var maxId: Long = set.maxByOrNull { it.id ?: 0 }?.id ?: 0
    set.forEach {
      var id = it.id
      if (id == null || idSet.contains(id)) {
        // Id already used, must fix it:
        id = ++maxId
        it.id = id
      }
      idSet.add(id)
      var name = it.name
      if (name.isNullOrBlank()) {
        name = getAutoName() // Fix empty names
        it.name = name
      }
      if (namesSet.contains(name)) {
        // Doublet found
        name = getAutoName(it.name)
        it.name = name
      }
      namesSet.add(name)
    }
  }

  private fun getAutoName(prefix: String? = null): String {
    val _prefix = prefix ?: translate("favorite.untitled")
    if (set.isEmpty()) {
      return _prefix
    }
    val existingNames = set.map { it.name }
    if (!existingNames.contains(_prefix))
      return _prefix
    for (i in 1..30) {
      val name = "$_prefix $i"
      if (!existingNames.contains(name))
        return name
    }
    return _prefix // Giving up, 1..30 are already used.
  }

  private val sortedList: List<T>
    get() {
      return set.sorted()
    }

  /**
   * Maps the set of favorites to list of names.
   */
  val favoriteNames: List<String>
    get() {
      fixNamesAndIds()
      return set.map { it.name!! }
    }

  /**
   * Maps the set of favorites to list of [FavoriteIdTitle].
   */
  val idTitleList: List<FavoriteIdTitle>
    get() {
      fixNamesAndIds()
      return sortedList.map { FavoriteIdTitle(it.id!!, it.name!!) }
    }

  fun getElementAt(pos: Int): T? {
    if (pos < 0) return null // No favorite is marked as active.
    if (pos < set.size) {
      // Get the user's active favorite:
      return set.elementAt(pos)
    }
    log.error("Favorite index #$pos is out of array bounds [0..${set.size - 1}].")
    return null
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(Favorites::class.java)

    const val PREF_NAME_LIST = "favorites.list"
    const val PREF_NAME_CURRENT = "favorites.current"

    fun addTranslations(translations: MutableMap<String, String>) {
      addTranslations(
        "favorites",
        "delete",
        "rename",
        "favorites",
        "favorite.addNew",
        "favorite.untitled",
        "favorites.saveModification",
        "uptodate",
        translations = translations
      )
    }

    fun deleteUserPref(userPrefDao: UserPrefDao, area: String, id: Long) {
      val userPref = userPrefDao.selectUserPref(area, id)
      if (userPref != null) {
        userPrefDao.delete(userPref)
      } else {
        log.warn("User tried to delete user pref with id #$id for area '$area', but it can't be deleted (is from other user, different area or has an unknown id).")
      }
    }

    /**
     * Rename the user pref itself, not the name of the favorite. This is only used for backwardcompatibility (e. g. used by TaskFavorites).
     */
    fun renameUserPref(userPrefDao: UserPrefDao, area: String, id: Long, newName: String) {
      val userPref = userPrefDao.selectUserPref(area, id)
      if (userPref != null) {
        if (userPref.name == newName) {
          // Nothing to-do: name isn't really changed.
          log.info("User tried to rename user pref with id #$id with name '${userPref.name}', but the new name doesn't differ.")
          return
        }
        if (userPrefDao.doesParameterNameAlreadyExist(id, ThreadLocalUserContext.loggedInUserId, area, newName)) {
          log.warn("User tried to rename user pref with id #$id from '${userPref.name}' into '$newName', but another entry with this name already exist.")
        } else {
          userPref.name = newName
          userPrefDao.update(userPref)
        }
      } else {
        log.warn("User tried to rename user pref with id #$id for area '$area', but it can't be renamed (is from other user, different area or has an unknown id).")
      }
    }
  }
}
