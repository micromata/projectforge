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

package org.projectforge.business.user

import com.fasterxml.jackson.annotation.JsonIgnore
import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamOmitField
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * User preferences contains a Map used by [UserPrefCache] and [UserXmlPreferencesCache] for storing user data application wide.
 * Also, persistent user preferences in the database are supported.<br>
 * The values are stored by area and identifier.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XStreamAlias("userPreferences")
class UserPrefCacheData {
    @JvmField
    @XStreamOmitField
    @JsonIgnore
    var userId: Long? = null

    private val persistentData = mutableMapOf<UserPrefCacheDataKey, Any>()

    internal fun persistentDataForeach(action: (key: UserPrefCacheDataKey, value: Any) -> Unit) {
        synchronized(persistentData) {
            persistentData.forEach { (identifier, value) ->
                action(identifier, value)
            }
        }
    }

    @XStreamOmitField
    @Transient
    @JsonIgnore
    private var volatileData = mutableMapOf<UserPrefCacheDataKey, Any>()


    /**
     * For detecting modifications. Value is the hashCode of original xml/json stored in the database.
     */
    @XStreamOmitField
    @Transient
    @JsonIgnore
    private var originalPersistentDataHashCode = mutableMapOf<UserPrefCacheDataKey, Int>()


    fun containsPersistentEntry(key: UserPrefCacheDataKey): Boolean {
        return synchronized(persistentData) {
            persistentData.containsKey(key)
        }
    }

    fun containsVolatileEntry(key: UserPrefCacheDataKey): Boolean {
        return synchronized(volatileData) {
            volatileData.containsKey(key)
        }
    }

    /**
     * @param key The key of the cached data.
     * @param value
     * @param persistent If true, the object will be marked as modified and persisted in the database.
     * @param originalSerializedHashCode The original value as xml (uncompressed)/json for storing as original value.
     */
    fun putEntry(key: UserPrefCacheDataKey, value: Any?, persistent: Boolean, originalSerializedHashCode: Int? = null) {
        value ?: return
        log.debug { "Put entry: user=$userId, area=${key.area}, identifier=${key.identifier}, value=$value, persistent=$persistent, hashCodeOfOriginalSerialized=$originalSerializedHashCode" }
        if (persistent) {
            synchronized(persistentData) {
                persistentData[key] = value
            }
            originalSerializedHashCode?.let {
                synchronized(originalPersistentDataHashCode) {
                    originalPersistentDataHashCode[key] = it
                }
            }
        } else {
            synchronized(volatileData) {
                volatileData[key] = value
            }
        }
    }

    /**
     * Gets the stored user preference entry.
     *
     * @param key The key of the cached data.
     * @return Return a persistent object with this identifier, if existing, or if not a volatile object with this identifier, if
     * existing, otherwise null;
     */
    fun getEntry(key: UserPrefCacheDataKey): Any? {
        return synchronized(persistentData) {
            persistentData[key]
        } ?: synchronized(volatileData) {
            volatileData[key]
        }
    }

    fun getEntry(area: String?, identifier: String?): Any? {
        return getEntry(UserPrefCacheDataKey(area, identifier))
    }

    /**
     * Gets the hashCode of the original entry from persistent storage (database).
     * Used for detecting modifications.
     */
    internal fun getOriginalDataHashCode(key: UserPrefCacheDataKey): Int? {
        return synchronized(originalPersistentDataHashCode) {
            originalPersistentDataHashCode[key]
        }
    }

    /**
     * Removes the entry from persistent and volatile storage if exist. Does not remove the entry from the database!
     *
     * @param key The key of the cached data.
     * @return the removed value if found.
     */
    fun removeEntry(key: UserPrefCacheDataKey): Any? {
        val value = synchronized(persistentData) {
            persistentData.remove(key)
        }
        val volatileValue = synchronized(volatileData) {
            volatileData.remove(key)
        }
        return value ?: volatileValue
    }

    /**
     * Clear all volatile data (after logout). Forces refreshing of volatile data after re-login.
     */
    fun clear() {
        synchronized(volatileData) {
            volatileData.clear()
        }
    }
}
