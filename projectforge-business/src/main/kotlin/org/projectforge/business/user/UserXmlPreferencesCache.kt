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

import jakarta.annotation.PreDestroy
import org.projectforge.business.user.UserPrefCache.Companion.dontCallPreDestroyInTestMode
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.loggedInUserId
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

/**
 * Stores all user persistent objects such as filter settings, personal settings and persists them to the database.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
@DependsOn("entityManagerFactory")
class UserXmlPreferencesCache : AbstractCache() {
    @Autowired
    private lateinit var userXmlPreferencesDao: UserXmlPreferencesDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    private val allPreferences = mutableMapOf<Long, UserXmlPreferencesMap>()

    /**
     * Please use UserPreferenceHelper instead for correct handling of demo user's preferences!
     *
     * @see org.projectforge.business.user.UserXmlPreferencesMap.putEntry
     */
    fun putEntry(userId: Long, key: String, value: Any, persistent: Boolean) {
        val data = ensureAndGetUserPreferencesData(userId)
        data.putEntry(key, value, persistent)
    }

    /**
     * Please use UserPreferenceHelper instead for correct handling of demo user's preferences!
     *
     * @see .ensureAndGetUserPreferencesData
     */
    fun getEntry(userId: Long, key: String?): Any? {
        key ?: return null
        val data = ensureAndGetUserPreferencesData(userId)
        return data.getEntry(key)
    }

    /**
     * Please use UserPreferenceHelper instead for correct handling of demo user's preferences!
     *
     * @see org.projectforge.business.user.UserXmlPreferencesMap.removeEntry
     */
    fun removeEntry(userId: Long, key: String): Any? {
        return removeEntry(userId, key, true)
    }

    private fun removeEntry(userId: Long, key: String, warnIfNotExists: Boolean): Any? {
        val data = getUserPreferencesData(userId)
            ?: // Should only occur for the pseudo-first-login-user setting up the system.
            return null
        if (data.containsPersistentKey(key)) {
            userXmlPreferencesDao.remove(userId, key)
        } else if (!data.containsVolatileKey(key)) {
            if (warnIfNotExists) {
                log.warn("Oups, user preferences object with key '$key' is wether persistent nor volatile!")
            }
            return null
        }
        return data.removeEntry(key)
    }

    /**
     * Please use UserPreferenceHelper instead for correct handling of demo user's preferences!
     *
     * @see org.projectforge.business.user.UserXmlPreferencesMap.removeEntry
     */
    fun removeEntryIfExists(userId: Long, key: String): Any? {
        return removeEntry(userId, key, false)
    }

    /**
     * Please use UserPreferenceHelper instead for correct handling of demo user's preferences!
     *
     * @param userId
     * @return
     */
    internal fun ensureAndGetUserPreferencesData(userId: Long): UserXmlPreferencesMap {
        var data = getUserPreferencesData(userId)
        if (data == null) {
            data = UserXmlPreferencesMap()
            data.userId = userId
            val userPrefs = userXmlPreferencesDao.getUserPreferencesByUserId(userId)
            userPrefs.forEach { userPref ->
                userXmlPreferencesDao.deserialize(userId, userPref, true)?.let { value ->
                    val originalXml = UserXmlPreferencesDao.getUncompressed(userPref.serializedSettings).hashCode()
                    data.putEntry(userPref.key!!, value, persistent = true, hashCodeOfOriginalXml = originalXml)
                }
            }
            synchronized(allPreferences) {
                allPreferences[userId] = data
            }
        }
        return data
    }

    internal fun getUserPreferencesData(userId: Long): UserXmlPreferencesMap? {
        synchronized(allPreferences) {
            return allPreferences[userId]
        }
    }

    internal fun setUserPreferencesData(userId: Long, data: UserXmlPreferencesMap) {
        synchronized(allPreferences) {
            allPreferences[userId] = data
        }
    }

    /**
     * Flushes the user settings to the database (independent from the expire mechanism). Should be used after the user's
     * logout. If the user data isn't modified, then nothing will be done.
     */
    fun flushToDB(userId: Long) {
        flushToDB(userId, true)
    }

    /**
     * Flushes the user settings to the database (independent from the expire mechanism). Should be used after the user's
     * logout. If the user data isn't modified, then nothing will be done.
     */
    fun flushAllToDB() {
        synchronized(allPreferences) {
            allPreferences.keys.forEach { userId ->
                flushToDB(userId, checkAccess = false)
            }
            allPreferences.clear()
        }
    }

    private fun flushToDB(userId: Long, checkAccess: Boolean) {
        if (checkAccess) {
            if (userId != loggedInUserId) {
                log.error(
                    ("User '" + loggedInUserId
                            + "' has no access to write user preferences of other user '" + userId + "'.")
                )
                // No access.
                return
            }
            val user = persistenceService.find(PFUserDO::class.java, userId)
            if (AccessChecker.isDemoUser(user)) {
                // Do nothing for demo user.
                return
            }
        }
        synchronized(allPreferences) {
            allPreferences[userId]?.let { data ->
                userXmlPreferencesDao.saveOrUpdateUserEntriesIfModified(userId, data, checkAccess)
                allPreferences.remove(userId)
            }
        }
    }

    /**
     * Stores the PersistentUserObjects in the database or on start up restores the persistent user objects from the
     * database.
     *
     * @see org.projectforge.framework.cache.AbstractCache.refresh
     */
    public override fun refresh() {
        log.info("Flushing all user preferences to database....")
        flushAllToDB()
        log.info("Flushing of user preferences to database done.")
    }

    /**
     * Clear all volatile data (after logout). Forces refreshing of volatile data after re-login.
     *
     * @param userId
     */
    fun clear(userId: Long) {
        val data = allPreferences[userId] ?: return
        data.clear()
    }

    override fun setExpireTimeInMinutes(expireTime: Long) {
        this.expireTime = 10 * TICKS_PER_MINUTE
    }

    @PreDestroy
    fun preDestroy() {
        if (dontCallPreDestroyInTestMode) {
            log.info("It seems to be running in test mode. No sync to database in UserPrefCache.")
            return
        }
        log.info("Syncing all user preferences to database.")
        this.forceReload()
    }

    companion object {
        private const val serialVersionUID = 248972660689793455L

        private val log: Logger = LoggerFactory.getLogger(UserXmlPreferencesCache::class.java)
    }
}
