/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

    private val persistentData = mutableMapOf<IUserPref, Any>()

    internal fun persistentDataForeach(action: (userPref: IUserPref, value: Any) -> Unit) {
        synchronized(persistentData) {
            persistentData.forEach { (identifier, value) ->
                action(identifier, value)
            }
        }
    }

    /**
     * For detecting modifications. Value is the hashCode of original xml/json stored in the database.
     */
    @XStreamOmitField
    @Transient
    @JsonIgnore
    private var originalPersistentDataHashCode = mutableMapOf<IUserPref, Int>()


    @XStreamOmitField
    @Transient
    @JsonIgnore
    private var volatileData = mutableMapOf<IUserPref, Any>()

    fun containsPersistentEntry(userPref: IUserPref): Boolean {
        return synchronized(persistentData) {
            persistentData.containsKey(userPref)
        }
    }

    fun containsVolatileEntry(userPref: IUserPref): Boolean {
        return synchronized(volatileData) {
            volatileData.containsKey(userPref)
        }
    }

    /**
     * @param area Optional (isn't used by [UserXmlPreferencesCache].
     * @param identifier key/area of the entry.
     * @param value
     * @param persistent If true, the object will be marked as modified and persisted in the database.
     * @param originalSerializedHashCode The original value as xml (uncompressed)/json for storing as original value.
     */
    fun putEntry(prefEntry: IUserPref, value: Any?, persistent: Boolean, originalSerializedHashCode: Int? = null) {
        value ?: return
        log.debug { "Put entry: user=$userId, area=${prefEntry.area}, identifier=${prefEntry.identifier}, value=$value, persistent=$persistent, hashCodeOfOriginalSerialized=$originalSerializedHashCode" }
        if (persistent) {
            synchronized(persistentData) {
                persistentData[prefEntry] = value
            }
            originalSerializedHashCode?.let {
                synchronized(originalPersistentDataHashCode) {
                    originalPersistentDataHashCode[prefEntry] = it
                }
            }
        } else {
            synchronized(volatileData) {
                volatileData[prefEntry] = value
            }
        }
    }

    /**
     * Gets the stored user preference entry.
     *
     * @param identifier
     * @return Return a persistent object with this identifier, if existing, or if not a volatile object with this identifier, if
     * existing, otherwise null;
     */
    fun getEntry(userPref: IUserPref): Any? {
        return synchronized(persistentData) {
            persistentData[userPref]
        } ?: synchronized(volatileData) {
            volatileData[userPref]
        }
    }

    fun getEntry(area: String?, identifier: String): Any? {
        getUserPref(area, identifier)?.let {
            return getEntry(it)
        }
        return null
    }

    internal fun getUserPref(area: String?, identifier: String): IUserPref? {
        return synchronized(persistentData) {
            persistentData.keys.firstOrNull { it.area == area && it.identifier == identifier }
        } ?: synchronized(volatileData) {
            volatileData.keys.firstOrNull { it.area == area && it.identifier == identifier }
        }
    }

    /**
     * Gets the hashCode of the original entry from persistent storage (database).
     * Used for detecting modifications.
     */
    internal fun getOriginalDataHashCode(userPref: IUserPref): Int? {
        return synchronized(originalPersistentDataHashCode) {
            originalPersistentDataHashCode[userPref]
        }
    }

    /**
     * Removes the entry from persistent and volatile storage if exist. Does not remove the entry from the database!
     *
     * @param identifier
     * @return the removed value if found.
     */
    fun removeEntry(userPref: IUserPref): Any? {
        val value = synchronized(persistentData) {
            persistentData.remove(userPref)
        }
        val volatileValue = synchronized(volatileData) {
            volatileData.remove(userPref)
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
