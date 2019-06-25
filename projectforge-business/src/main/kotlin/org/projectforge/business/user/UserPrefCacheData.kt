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

package org.projectforge.business.user

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.UserPrefDO

/**
 * User preferences contains a Map used by UserPrefCache for storing user data application wide.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
internal class UserPrefCacheData {
    class CacheEntry(
            var userPrefDO: UserPrefDO,
            var persistant: Boolean = true,
            var modified: Boolean = false)

    var userId: Int? = null

    private var entries = mutableListOf<CacheEntry>()

    internal fun putEntry(userPref: UserPrefDO) {
        entries.add(CacheEntry(userPref))
    }

    /**
     * @param persistent If true, the object will be persisted in the database.
     */
    internal fun putEntry(area: String, name: String, value: Any, persistent: Boolean = true) {
        synchronized(entries) {
            var cacheEntry = findEntry(area, name)
            if (cacheEntry != null) {
                cacheEntry.modified = true
                cacheEntry.persistant = persistent
            } else {
                val userPref = UserPrefDO()
                userPref.area = area
                userPref.name = name
                userPref.user = ThreadLocalUserContext.getUser()
                cacheEntry = CacheEntry(userPref, persistent)
                entries.add(cacheEntry)
            }
            cacheEntry.userPrefDO.valueObject = value
        }
    }

    /**
     * Gets the stored user preference entry.
     *
     * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
     * existing, otherwise null;
     */
    internal fun getEntry(area: String, name: String): CacheEntry? {
        val cacheEntry = findEntry(area, name) ?: return null
        // Assuming modification after use-age:
        cacheEntry.modified = true
        return cacheEntry
    }

    /**
     * Removes the entry from persistent and volatile storage if exist. Does not remove the entry from the data base!
     *
     * @return the removed value if found.
     */
    internal fun removeEntry(area: String, name: String) {
        synchronized(entries) {
            entries.removeIf { it.userPrefDO.name == name && it.userPrefDO.area == area }
        }
    }

    internal fun getModifiedPersistentEntries(): List<CacheEntry> {
        return entries.filter { it.persistant && it.modified }
    }

    /**
     * Clear all volatile data (after logout). Forces refreshing of volatile data after re-login.
     */
    fun clear() {
        synchronized(entries) {
            entries.clear()
        }
    }

    private fun findEntry(area: String, name: String): CacheEntry? {
        return entries.find { it.userPrefDO.name == name && it.userPrefDO.area == area }
    }

}
