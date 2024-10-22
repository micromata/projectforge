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

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import org.projectforge.business.scripting.xstream.RecentScriptCalls
import org.projectforge.business.scripting.xstream.ScriptCallData
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskFilter
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUser
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.GZIPHelper
import org.projectforge.framework.xmlstream.XStreamHelper.createXStream
import org.projectforge.framework.xmlstream.XStreamHelper.fromXml
import org.projectforge.framework.xmlstream.XStreamHelper.toXml
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Stores all user persistent objects such as filter settings, personal settings and persists them to the database as
 * xml (compressed (gzip and base64) for larger xml content).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class UserXmlPreferencesDao {
    private val xstream = createXStream(
        UserXmlPreferencesMap::class.java,
        TaskFilter::class.java,
        ScriptCallData::class.java,
        RecentScriptCalls::class.java
    )

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @PostConstruct
    private fun init() {
        registerConverter(UserDao::class.java, PFUserDO::class.java, 20)
        registerConverter(GroupDao::class.java, GroupDO::class.java, 19)
        registerConverter(TaskDao::class.java, TaskDO::class.java, 18)
    }

    /**
     * Register converters before marshaling and unmarshaling by XStream. This method is usable by plugins.
     *
     * @param daoClass Class of the dao.
     * @param doClass  Class of the DO which will be converted.
     * @param priority The priority needed by xtream for using converters in the demanded order.
     * // @see UserXmlPreferencesBaseDOSingleValueConverter#UserXmlPreferencesBaseDOSingleValueConverter(Class, Class)
     */
    fun registerConverter(
        daoClass: Class<out BaseDao<*>?>?, doClass: Class<out BaseDO<*>?>?,
        priority: Int
    ) {
        xstream.registerConverter(
            UserXmlPreferencesBaseDOSingleValueConverter(applicationContext, daoClass, doClass),
            priority
        )
    }

    /**
     * Throws AccessException if the context user is not admin user and not owner of the UserXmlPreferences, meaning the
     * given userId must be the id of the context user.
     *
     * @param userId
     */
    fun getUserPreferencesByUserId(
        userId: Long, key: String?,
        checkAccess: Boolean
    ): UserXmlPreferencesDO? {
        if (checkAccess) {
            checkAccess(userId)
        }
        val list: List<UserXmlPreferencesDO> = persistenceService.executeQuery(
            "from UserXmlPreferencesDO where user.id = :userid and key = :key",
            UserXmlPreferencesDO::class.java,
            Pair("userid", userId), Pair("key", key)
        )
        Validate.isTrue(list.size <= 1)
        return if (list.size == 1) {
            list[0]
        } else null
    }

    fun <T> getDeserializedUserPreferencesByUserId(userId: Long, key: String?, returnClass: Class<T>?): T? {
        return deserialize(userId, getUserPreferencesByUserId(userId, key, true)!!, false) as T?
    }

    /**
     * Throws AccessException if the context user is not admin user and not owner of the UserXmlPreferences, meaning the
     * given userId must be the id of the context user.
     *
     * @param userId
     */
    fun getUserPreferencesByUserId(userId: Long): List<UserXmlPreferencesDO> {
        checkAccess(userId)
        return persistenceService.executeQuery(
            "select u from UserXmlPreferencesDO u where u.user.id = :userid",
            UserXmlPreferencesDO::class.java,
            Pair("userid", userId)
        )
    }

    /**
     * Checks if the given userIs is equals to the context user or the if the user is an admin user. If not a
     * AccessException will be thrown.
     *
     * @param userId
     */
    fun checkAccess(userId: Long) {
        requireNotNull(userId)
        val user = requiredLoggedInUser
        if (userId != user.id) {
            accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        }
    }

    /**
     * Here you can update user preferences formats by manipulation the stored xml string.
     *
     * @param userId
     * @param userPrefs
     * @param logError
     */
    fun deserialize(userId: Long, userPrefs: UserXmlPreferencesDO, logError: Boolean): Any? {
        var xml: String? = null
        try {
            UserXmlPreferencesMigrationDao.migrate(userPrefs)
            xml = userPrefs.serializedSettings
            if (xml.isNullOrEmpty()) {
                return null
            }
            if (xml.startsWith("!")) {
                // Uncompress value:
                val uncompressed = GZIPHelper.uncompress(xml.substring(1))!!
                xml = uncompressed
            }
            val sourceClassName = getSourceClassName(xml)
            if (log.isDebugEnabled) {
                log.debug("UserId: $userId Object to deserialize: $xml")
            }
            val value = fromXml(xstream, xml)
            return value
        } catch (ex: Throwable) {
            if (logError) {
                log.warn(
                    ("Can't deserialize user preferences: "
                            + ex.message
                            + " for user: "
                            + userPrefs.userId
                            + ":"
                            + userPrefs.key
                            + " (may-be ok after a new ProjectForge release). xml="
                            + xml)
                )
            }
            return null
        }
    }

    private fun getSourceClassName(xml: String): String? {
        val elements = xml.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (elements.size > 0) {
            val result = elements[0].replace("<", "").replace(">", "")
            if (StringUtils.countMatches(result, ".") > 1) {
                return result
            }
        }
        return null
    }

    fun serialize(userPrefs: UserXmlPreferencesDO, value: Any?): String {
        val xml = toXml(xstream, value)

        if (xml.length > 1000) {
            // Compress value:
            val compressed = GZIPHelper.compress(xml)
            userPrefs.serializedSettings = "!$compressed"
        } else {
            userPrefs.serializedSettings = xml
        }
        return xml
    }

    // REQUIRES_NEW needed for avoiding a lot of new data base connections from HibernateFilter.
    fun saveOrUpdateUserEntries(userId: Long, data: UserXmlPreferencesMap, checkAccess: Boolean) {
        for ((key, value) in data.persistentData) {
            if (data.isModified(key)) {
                try {
                    saveOrUpdate(userId, key, value, checkAccess)
                } catch (ex: Throwable) {
                    log.warn(ex.message, ex)
                }
                data.setModified(key, false)
            }
        }
    }

    /**
     * @param userId If null, then user will be set to null;
     * @see BaseDao.findOrLoad
     */
    fun setUser(userPrefs: UserXmlPreferencesDO, userId: Long?) {
        userId ?: return
        val user = userDao.findOrLoad(userId)
        userPrefs.user = user
    }

    fun saveOrUpdate(userId: Long, key: String?, entry: Any?, checkAccess: Boolean) {
        if (accessChecker.isDemoUser(userId)) {
            // Do nothing.
            return
        }
        var isNew = false
        var userPrefs = getUserPreferencesByUserId(userId, key, checkAccess)
        val date = Date()
        if (userPrefs == null) {
            isNew = true
            userPrefs = UserXmlPreferencesDO()
            userPrefs.created = date
            userPrefs.user = userDao.find(userId, checkAccess = false)
            userPrefs.key = key
        }
        val xml = serialize(userPrefs, entry)
        if (log.isDebugEnabled) {
            log.debug("UserXmlPrefs serialize to db: $xml")
        }
        userPrefs.lastUpdate = date
        userPrefs.setVersion()
        val userPrefsForDB: UserXmlPreferencesDO = userPrefs
        persistenceService.runInTransaction { context ->
            if (isNew) {
                if (log.isDebugEnabled) {
                    log.debug("Storing new user preference for user '$userId': $xml")
                }
                context.insert(userPrefsForDB)
            } else {
                if (log.isDebugEnabled) {
                    log.debug("Updating user preference for user '" + userPrefs.userId + "': " + xml)
                }
                context.find(
                    UserXmlPreferencesDO::class.java,
                    userPrefsForDB.id,
                    attached = true,
                )?.let { attachedEntity ->
                    attachedEntity.serializedSettings = userPrefsForDB.serializedSettings
                    attachedEntity.lastUpdate = userPrefsForDB.lastUpdate
                    attachedEntity.setVersion()
                    context.flush()
                }
                null
            }
        }
    }

    fun remove(userId: Long, key: String?) {
        if (accessChecker.isDemoUser(userId)) {
            // Do nothing.
            return
        }
        val userPreferencesDO = getUserPreferencesByUserId(userId, key, true)
        userPreferencesDO?.id?.let { id ->
            persistenceService.runInTransaction { context ->
                context.delete(UserXmlPreferencesDO::class.java, id)
            }
        }
    }
}
