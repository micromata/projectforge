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

import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component
import java.util.*
import javax.annotation.PreDestroy

/**
 * A cache for UserPrefDO, if preferences are modified and accessed very often by the user's normal work
 * (such as current filters in Calendar and list pages etc.)
 * Under construction (not yet in use).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
@DependsOn("entityManagerFactory")
class UserPrefCache : AbstractCache() {
    private val log = org.slf4j.LoggerFactory.getLogger(UserPrefCache::class.java)

    private val allPreferences = HashMap<Int, UserPrefCacheData>()

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    @Autowired
    private lateinit var emgrFactory: PfEmgrFactory

    /**
     * Does nothing for demo user.
     * @param persistent If true (default) this user preference will be stored to the data base, otherwise it will
     * be volatile stored in memory and will expire.
     */
    fun putEntry(area: String, name: String, value: Any, persistent: Boolean = true) {
        val userId = ThreadLocalUserContext.getUserId()
        if (accessChecker.isDemoUser(userId)) {
            // Store user pref for demo user only in user's session.
            return
        }
        val data = ensureAndGetUserPreferencesData(userId)
        data.putEntry(area, name, value, persistent)
        checkRefresh() // Should be called at the end of this method for considering changes inside this method.
    }

    /**
     * Gets the user's entry.
     */
    fun getEntry(area: String, name: String): Any? {
        val userId = ThreadLocalUserContext.getUserId()
        return getEntry(userId, area, name)
    }

    /**
     * Gets the user's entry.
     */
    fun <T> getEntry(area: String, name: String, clazz: Class<T>): T? {
        val userId = ThreadLocalUserContext.getUserId()
        return getEntry(userId, area, name, clazz)
    }

    fun removeEntry(area: String, name: String) {
        val userId = ThreadLocalUserContext.getUserId()
        if (accessChecker.isDemoUser(userId)) {
            // Store user pref for demo user only in user's session.
            return
        }
        return removeEntry(userId, area, name)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun <T> getEntry(userId: Int, area: String, name: String, clazz: Class<T>): T? {
        val value = getEntry(userId, area, name)
        try {
            @Suppress("UNCHECKED_CAST")
            return value as T
        } catch (ex: Exception) {
            log.error("Can't deserialize user pref (new version of ProjectForge and old prefs? ${ex.message}", ex)
            return null
        }
    }

    private fun getEntry(userId: Int, area: String, name: String): Any? {
        val data = ensureAndGetUserPreferencesData(userId)
        checkRefresh()
        val userPref = data.getEntry(area, name)?.userPrefDO ?: return null
        return userPref.valueObject ?: userPrefDao.deserizalizeValueObject(userPref)
    }

    private fun removeEntry(userId: Int, area: String, name: String) {
        val data = getUserPreferencesData(userId)
                ?: // Should only occur for the pseudo-first-login-user setting up the system.
                return
        val cacheEntry = data.getEntry(area, name)
        if (cacheEntry == null) {
            log.info("Oups, user preferences object with area '$area' and name '$name' not cached, can't remove it!")
            return
        }
        data.removeEntry(area, name)
        if (cacheEntry.persistant)
            userPrefDao.delete(cacheEntry.userPrefDO)
        checkRefresh()
    }

    /**
     * Please use UserPreferenceHelper instead for correct handling of demo user's preferences!
     *
     * @param userId
     * @return
     */
    @Synchronized
    private fun ensureAndGetUserPreferencesData(userId: Int): UserPrefCacheData {
        var data = getUserPreferencesData(userId)
        if (data == null) {
            data = UserPrefCacheData()
            data.userId = userId
            val userPrefs = userPrefDao.getUserPrefs()
            userPrefs?.forEach {
                data.putEntry(it)
            }
            this.allPreferences[userId] = data
        }
        return data
    }

    internal fun getUserPreferencesData(userId: Int): UserPrefCacheData? {
        return this.allPreferences[userId]
    }

    internal fun setUserPreferencesData(userId: Int, data: UserPrefCacheData) {
        this.allPreferences[userId] = data
    }

    /**
     * Flushes the user settings to the database (independent from the expire mechanism). Should be used after the user's
     * logout. If the user data isn't modified, then nothing will be done.
     */
    fun flushToDB(userId: Int?) {
        flushToDB(userId, true)
    }

    @Synchronized
    private fun flushToDB(userId: Int?, checkAccess: Boolean) {
        if (checkAccess) {
            if (userId != ThreadLocalUserContext.getUserId()) {
                log.error("User '" + ThreadLocalUserContext.getUserId()
                        + "' has no access to write user preferences of other user '" + userId + "'.")
                // No access.
                return
            }
            val user = emgrFactory.runInTrans { emgr -> emgr.selectByPk(PFUserDO::class.java, userId) }
            if (AccessChecker.isDemoUser(user)) {
                // Do nothing for demo user.
                return
            }
        }
        val data = allPreferences[userId]
        data?.getModifiedPersistentEntries()?.forEach {
            userPrefDao.internalSaveOrUpdate(it.userPrefDO)
        }
    }

    /**
     * Stores the PersistentUserObjects in the database or on start up restores the persistent user objects from the
     * database.
     *
     * @see AbstractCache.refresh
     */
    override fun refresh() {
        log.info("Flushing all user preferences to data-base....")
        for (userId in allPreferences.keys) {
            flushToDB(userId, false)
        }
        log.info("Flushing of user preferences to data-base done.")
    }

    /**
     * Clear all volatile data (after logout). Forces refreshing of volatile data after re-login.
     *
     * @param userId
     */
    fun clear(userId: Int?) {
        val data = allPreferences[userId] ?: return
        data.clear()
    }

    override fun setExpireTimeInMinutes(expireTime: Long) {
        this.expireTime = 10 * TICKS_PER_MINUTE
    }

    @PreDestroy
    fun preDestroy() {
        log.info("Syncing all user preferences to database.")
        this.forceReload()
    }
}
