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

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamOmitField
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * User preferences contains a Map used by UserXmlPreferencesCache for storing user data application wide. Also
 * persistent user preferences in the database are supported.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XStreamAlias("userPreferences")
internal class UserXmlPreferencesMap {
    @JvmField
    @XStreamOmitField
    var userId: Long? = null

    private val persistentData = mutableMapOf<String, Any>()

    internal fun persistentDataForeach(action: (key: String, value: Any) -> Unit) {
        synchronized(persistentData) {
            persistentData.forEach { (key, value) ->
                action(key, value)
            }
        }
    }

    /**
     * For detecting modifications. Value is the hashCode of original xml stored in the database.
     */
    @XStreamOmitField
    @Transient
    private var originalPersistentDataHashCode = mutableMapOf<String, Int>()


    @XStreamOmitField
    @Transient
    private var volatileData = mutableMapOf<String, Any>()

    fun containsPersistentKey(key: String): Boolean {
        return synchronized(persistentData) {
            persistentData.containsKey(key)
        }
    }

    fun containsVolatileKey(key: String): Boolean {
        return synchronized(volatileData) {
            volatileData.containsKey(key)
        }
    }

    /**
     * @param key
     * @param value
     * @param persistent If true, the object will be marked as modified and persisted in the database.
     * @param hashCodeOfOriginalXml The original value as xml (uncompressed) for storing as original value.
     */
    fun putEntry(key: String, value: Any, persistent: Boolean, hashCodeOfOriginalXml: Int? = null) {
        log.debug { "Put entry: key=$key, value=$value, persistent=$persistent, hashCodeOfOriginalXml=$hashCodeOfOriginalXml" }
        if (persistent) {
            synchronized(persistentData) {
                persistentData[key] = value
            }
            hashCodeOfOriginalXml?.let {
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
     * @param key
     * @return Return a persistent object with this key, if existing, or if not a volatile object with this key, if
     * existing, otherwise null;
     */
    fun getEntry(key: String): Any? {
        return synchronized(persistentData) {
            persistentData[key]
        } ?: synchronized(volatileData) {
            volatileData[key]
        }
    }

    /**
     * Gets the hashCode of the original entry from persistent storage (database).
     * Used for detecting modifications.
     */
    internal fun getOriginalDataHashCode(key: String): Int? {
        return synchronized(originalPersistentDataHashCode) {
            originalPersistentDataHashCode[key]
        }
    }

    /**
     * Removes the entry from persistent and volatile storage if exist. Does not remove the entry from the database!
     *
     * @param key
     * @return the removed value if found.
     */
    fun removeEntry(key: String): Any? {
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
