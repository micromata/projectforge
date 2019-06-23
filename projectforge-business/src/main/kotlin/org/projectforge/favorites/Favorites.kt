/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.i18n.addTranslations
import org.projectforge.framework.i18n.translate

/**
 * Persist the user's set of favorites sorted by unique names. The user may configure a set of favorites and my apply one
 * by choosing from a drop down set.
 *
 * Ensures the uniqueness of favorite's names.
 */
class Favorites<T : AbstractFavorite>() {

    /**
     * For exports etc.
     */
    class FavoriteIdTitle(val id: Int, val name: String)

    private val log = org.slf4j.LoggerFactory.getLogger(Favorites::class.java)

    private val set: MutableSet<T> = mutableSetOf()

    fun get(id: Int): T? {
        fixNamesAndIds()
        return set.find { it.id == id }
    }

    fun add(filter: T) {
        set.add(filter)
        fixNamesAndIds()
    }

    fun remove(name: String) {
        fixNamesAndIds()
        set.removeIf { it.name == name }
    }

    fun remove(id: Int) {
        fixNamesAndIds()
        set.removeIf { it.id == id }
    }

    /**
     * Fixes empty names and doublets of names.
     */
    private fun fixNamesAndIds() {
        val namesSet = mutableSetOf<String>()
        val idSet = mutableSetOf<Int>()
        var maxId: Int = set.maxBy { it.id }?.id ?: 0
        set.forEach {
            if (idSet.contains(it.id)) {
                // Id already used, must fix it:
                it.id = ++maxId
            }
            idSet.add(it.id)
            if (it.name.isNullOrBlank())
                it.name = getAutoName() // Fix empty names
            if (namesSet.contains(it.name)) {
                // Doublet found
                it.name = getAutoName(it.name)
            }
            namesSet.add(it.name)
        }
    }

    fun getAutoName(prefix: String? = null): String {
        var _prefix = prefix ?: translate("calendar.filter.untitled")
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

    val sortedList: List<T>
        get() {
            return set.sorted()
        }

    /**
     * Maps the set of filters to list of names.
     */
    val favoriteNames: List<String>
        get() {
            fixNamesAndIds()
            return set.map { it.name }
        }

    /**
     * Maps the set of filters to list of [FavoriteIdTitle].
     */
    val idTitleList: List<FavoriteIdTitle>
        get() {
            fixNamesAndIds()
            return sortedList.map { FavoriteIdTitle(it.id, it.name) }
        }

    fun getElementAt(pos: Int): T? {
        if (pos < 0) return null // No filter is marked as active.
        if (pos < set.size) {
            // Get the user's active filter:
            return set.elementAt(pos)
        }
        log.error("Favorite index #$pos is out of array bounds [0..${set.size - 1}].")
        return null
    }

    internal fun getFilter(name: String): T? {
        set.forEach {
            if (name == it.name)
                return it
        }
        log.error("Favorite named '$name' not found.")
        return null
    }

    companion object {
        fun addTranslations(translations: MutableMap<String, String>) {
            addTranslations(
                    "favorites",
                    "delete",
                    "rename",
                    "favorite.addNew",
                    "save",
                    "uptodate",
                    translations = translations)
        }
    }
}
