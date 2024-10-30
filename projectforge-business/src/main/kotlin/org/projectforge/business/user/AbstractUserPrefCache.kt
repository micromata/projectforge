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
import mu.KotlinLogging
import org.projectforge.business.user.UserPrefCache.Companion.dontCallPreDestroyInTestMode
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.loggedInUserId
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired

private val log = KotlinLogging.logger {}

/**
 * Stores all user persistent objects such as filter settings, personal settings and persists them to the database.
 *
 * Extended by [UserPrefCache] and [UserXmlPreferencesCache].
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractUserPrefCache<DBObj : IUserPref>(
    val title: String,
    val identifierName: String
) :
    AbstractCache() {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    private val allPreferences = mutableMapOf<Long, UserPrefCacheData>()

    protected abstract fun selectUserPreferencesByUserId(userId: Long): Collection<DBObj>?
    protected abstract fun saveOrUpdate(userId: Long, key: UserPrefCacheDataKey, value: Any, checkAccess: Boolean)
    protected abstract fun deserialize(userPref: DBObj): Any?

    /**
     * Should be serialized and compressed, if required.
     */
    protected abstract fun serialize(value: Any): String
    protected abstract fun remove(userId: Long, key: UserPrefCacheDataKey)
    protected abstract fun newEntry(): DBObj

    @JvmOverloads
    fun getEntry(
        area: String?, identifier: String?, userId: Long? = null
    ): Any? {
        val uid = userId ?: ThreadLocalUserContext.requiredLoggedInUserId
        return ensureAndGetUserPreferencesData(uid).getEntry(area, identifier)
    }

    @Suppress("UNCHECKED_CAST", "UNUSED_PARAMETER")
    @JvmOverloads
    fun <T> getEntry(
        area: String?, identifier: String, expectedType: Class<T>, userId: Long? = null
    ): T? {
        val uid = userId ?: ThreadLocalUserContext.requiredLoggedInUserId
        return ensureAndGetUserPreferencesData(uid).getEntry(area, identifier) as? T?
    }

    @JvmOverloads
    fun putEntry(
        area: String?, identifier: String, value: Any?, persistent: Boolean,
        userId: Long? = null
    ) {
        val uid = userId ?: ThreadLocalUserContext.requiredLoggedInUserId
        val cacheData = ensureAndGetUserPreferencesData(uid)
        val key = UserPrefCacheDataKey(area, identifier)
        synchronized(cacheData) {
            cacheData.putEntry(key, value, persistent)
        }
    }

    /**
     * Puts an entry into the user preferences cache. If the entry is persistent, it will be stored in the database.
     */
    fun putEntry(
        key: UserPrefCacheDataKey,
        value: Any?,
        persistent: Boolean,
        userId: Long? = null
    ) {
        val uid = userId ?: ThreadLocalUserContext.requiredLoggedInUserId
        if (accessChecker.isDemoUser(uid)) {
            // Store user pref for demo user only in user's session.
            return
        }
        val data = ensureAndGetUserPreferencesData(uid)
        log.debug {
            "$title: Put value for ${createLogMessagePart(userId, key)} (persistent=$persistent): ${
                ToStringUtil.toJsonString(value ?: "null")
            }"
        }
        data.putEntry(key, value, persistent)
    }

    /**
     * Gets an entry from the user preferences cache.
     */
    fun getEntry(userId: Long, key: UserPrefCacheDataKey): Any? {
        val data = ensureAndGetUserPreferencesData(userId)
        return data.getEntry(key)
    }

    @JvmOverloads
    fun removeEntry(
        area: String?,
        identifier: String,
        userId: Long? = null,
    ) {
        val uid = userId ?: ThreadLocalUserContext.requiredLoggedInUserId
        ensureAndGetUserPreferencesData(uid)
        val key = UserPrefCacheDataKey(area, identifier)
        remove(uid, key)
    }

    internal fun insertOrUpdateUserEntriesIfModified(data: UserPrefCacheData, checkAccess: Boolean) {
        val userId = data.userId ?: return
        var counter = 0
        data.persistentDataForeach { key, value ->
            if (isModified(data, key, value)) {
                log.debug { "${title}: User preference modified: ${createLogMessagePart(userId, key)}" }
                // Only save if changed to avoid unnecessary database updates.
                ++counter
                try {
                    saveOrUpdate(userId, key, value, checkAccess)
                } catch (ex: Throwable) {
                    log.warn(ex.message, ex)
                }
            } else {
                log.debug { "User preference not modified: ${createLogMessagePart(userId, key)}" }
            }
        }
        if (counter > 0) {
            log.info { "Saved $counter modified entries of user=${data.userId}" }
        }
    }

    internal fun isModified(data: UserPrefCacheData, key: UserPrefCacheDataKey, value: Any?): Boolean {
        val userId = data.userId
        val originalHashCode = data.getOriginalDataHashCode(key)
        val currenHashCode = if (value != null) serialize(value).hashCode() else 0
        log.debug {
            "User preference modification status=${originalHashCode != currenHashCode}, ${
                createLogMessagePart(userId, key)
            }, value=$value, originalHashCode=$originalHashCode, currenHashCode=$currenHashCode"
        }
        return originalHashCode != currenHashCode
    }

    /**
     * Please use UserPreferenceHelper instead for correct handling of demo user's preferences!
     *
     * @param userId
     * @return
     */
    internal fun ensureAndGetUserPreferencesData(userId: Long, logError: Boolean = true): UserPrefCacheData {
        checkRefresh()
        var data = getUserPreferencesData(userId)
        if (data == null) {
            data = UserPrefCacheData()
            data.userId = userId
            val userPrefsCol = selectUserPreferencesByUserId(userId)
            userPrefsCol?.forEach { userPref ->
                val key = UserPrefCacheDataKey(userPref.area, userPref.identifier!!)
                try {
                    val originalSerializedHashCode = userPref.serializedValue.hashCode()
                    deserialize(userPref)?.let { value ->
                        data.putEntry(
                            key,
                            value,
                            persistent = true,
                            originalSerializedHashCode = originalSerializedHashCode,
                        )
                    }
                } catch (ex: Throwable) {
                    if (logError) {
                        log.warn {
                            "Can't deserialize user preferences: ${ex.message}, ${
                                createLogMessagePart(
                                    userId,
                                    key
                                )
                            } (may-be ok after a new ProjectForge release). string=${userPref.serializedValue}"
                        }
                    }
                }
            }
            synchronized(allPreferences) {
                allPreferences[userId] = data
            }
        }
        return data
    }

    internal fun getUserPreferencesData(userId: Long): UserPrefCacheData? {
        synchronized(allPreferences) {
            return allPreferences[userId]
        }
    }

    /**
     * Flushes the user settings to the database (independent of the expired mechanism). Should be used after the user's
     * logout. If the user data isn't modified, then nothing will be done.
     */
    fun flushToDB(userId: Long) {
        flushToDB(userId, true)
    }

    /**
     * Flushes the user settings to the database (independent from the expire mechanism). Should be used after the user's
     * logout. If the user data isn't modified, then nothing will be done.
     */
    private fun flushAllToDB() {
        log.info("$title: Flushing all user preferences to database....")
        persistenceService.runInNewTransaction {
            synchronized(allPreferences) {
                allPreferences.forEach { (_, data) ->
                    insertOrUpdateUserEntriesIfModified(data, checkAccess = false)
                }
                allPreferences.clear()
            }
        }
    }

    private fun flushToDB(userId: Long, checkAccess: Boolean) {
        if (checkAccess) {
            if (userId != loggedInUserId) {
                log.error { "$title: User '$loggedInUserId' has no access to write user preferences of other user '$userId'." }
                // No access.
                return
            }
            val user = persistenceService.find(PFUserDO::class.java, userId)
            if (AccessChecker.isDemoUser(user)) {
                // Do nothing for demo user.
                return
            }
        }
        persistenceService.runInNewTransaction {
            synchronized(allPreferences) {
                allPreferences[userId]?.let { data ->
                    insertOrUpdateUserEntriesIfModified(data, checkAccess)
                }
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
        log.info("$title: Flushing all user preferences to database....")
        flushAllToDB()
        log.info("$title: Flushing of user preferences to database done.")
    }

    /**
     * Clear all volatile data (after logout). Forces refreshing of volatile data after re-login.
     *
     * @param userId
     */
    fun clear(userId: Long) {
        synchronized(allPreferences) {
            allPreferences.remove(userId)
        }
    }

    private fun createLogMessagePart(userId: Long?, key: UserPrefCacheDataKey): String {
        return "userId=$userId, area=${key.area}, $identifierName='${key.identifier}'"
    }

    override fun setExpireTimeInMinutes(expireTime: Long) {
        this.expireTime = 10 * TICKS_PER_MINUTE
    }

    @PreDestroy
    fun preDestroy() {
        if (dontCallPreDestroyInTestMode) {
            log.info("$title: It seems to be running in test mode. No sync to database in UserXmlPreferencesCache.")
            return
        }
        flushAllToDB()
    }
}
