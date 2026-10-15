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


package org.projectforge.plugins.todo

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.common.i18n.Priority
import org.projectforge.favorites.Favorites
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The user's to-do templates (favorites of the edit form) and the type and priority of the last saved to-do,
 * which a new to-do starts with.
 *
 * The templates of the removed Wicket form (user prefs of the area `TODO_FAVORITE`) are not taken over.
 */
@Service
class ToDoFavoritesService {
    @Autowired
    private lateinit var userPrefService: UserPrefService

    /** Type and priority of the last saved to-do. */
    class LastValues(var type: ToDoType? = null, var priority: Priority? = null)

    fun getList(): List<ToDoFavorite> {
        return getFavorites().idTitleList.map { ToDoFavorite(it.name, it.id) }
    }

    fun get(id: Long): ToDoFavorite? {
        return getFavorites().get(id)
    }

    fun create(favorite: ToDoFavorite) {
        getFavorites().add(favorite)
    }

    fun delete(id: Long) {
        getFavorites().remove(id)
    }

    fun rename(id: Long, newName: String) {
        getFavorites().rename(id, newName)
    }

    fun getLastValues(): LastValues? {
        return userPrefService.getEntry(PREF_AREA, PREF_NAME_LAST_VALUES, LastValues::class.java)
    }

    fun storeLastValues(todo: ToDoDO) {
        userPrefService.putEntry(PREF_AREA, PREF_NAME_LAST_VALUES, LastValues(todo.type, todo.priority))
    }

    private fun getFavorites(): Favorites<ToDoFavorite> {
        var favorites: Favorites<ToDoFavorite>? = null
        try {
            @Suppress("UNCHECKED_CAST")
            favorites = userPrefService.getEntry(
                PREF_AREA,
                Favorites.PREF_NAME_LIST,
                Favorites::class.java
            ) as Favorites<ToDoFavorite>?
        } catch (ex: Exception) {
            log.error { "Exception while getting the user's to-do templates: ${ex.message}. Ignoring them." }
        }
        if (favorites == null) {
            favorites = Favorites()
            userPrefService.putEntry(PREF_AREA, Favorites.PREF_NAME_LIST, favorites)
        }
        return favorites
    }

    companion object {
        /**
         * Not the category of the list ("todo"), whose area holds the filter favorites of the list under the
         * same name [Favorites.PREF_NAME_LIST] (see `TimesheetFavoritesService` for the collision this avoids).
         */
        private const val PREF_AREA = "todoTemplateFavorites"

        private const val PREF_NAME_LAST_VALUES = "lastValues"
    }
}
