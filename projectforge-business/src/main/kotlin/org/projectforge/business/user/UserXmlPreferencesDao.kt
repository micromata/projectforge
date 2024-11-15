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
import org.bouncycastle.asn1.x500.style.RFC4519Style.title
import org.projectforge.business.scripting.xstream.RecentScriptCalls
import org.projectforge.business.scripting.xstream.ScriptCallData
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskFilter
import org.projectforge.common.extensions.abbreviate
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.loggedInUserId
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUser
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
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
        UserPrefCacheData::class.java,
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
    private fun selectUserPreferencesByUserId(
        userId: Long, key: String?,
        checkAccess: Boolean,
        attached: Boolean = false,
    ): UserXmlPreferencesDO? {
        if (checkAccess) {
            checkAccess(userId)
        }
        return persistenceService.selectNamedSingleResult(
            UserXmlPreferencesDO.FIND_BY_USER_ID_AND_KEY,
            UserXmlPreferencesDO::class.java,
            Pair("userId", userId),
            Pair("key", key),
            attached = attached,
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun <T> getDeserializedUserPreferencesByUserId(userId: Long, key: String?, returnClass: Class<T>): T? {
        val userPref = selectUserPreferencesByUserId(userId, key, true) ?: return null
        @Suppress("UNCHECKED_CAST")
        return deserialize(userPref) as T?
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
     * @param userPrefs
     */
    fun deserialize(userPrefs: UserXmlPreferencesDO): Any? {
        val userId = userPrefs.user?.id
        UserXmlPreferencesMigrationDao.migrate(userPrefs)
        var xml = userPrefs.serializedValue
        if (xml.isNullOrEmpty()) {
            return null
        }
        xml = UserPrefDao.getUncompressed(xml)
        log.debug { "UserId: $userId Object to deserialize: $xml" }
        val value = fromXml(xstream, xml)
        return value
    }

    /*private fun getSourceClassName(xml: String): String? {
        val elements = xml.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (elements.size > 0) {
            val result = elements[0].replace("<", "").replace(">", "")
            if (StringUtils.countMatches(result, ".") > 1) {
                return result
            }
        }
        return null
    }*/

    /**
     * Serializes the given object to xml.
     * @param compressBigContent If true, the xml will be compressed if it is larger than 1000 characters.
     * @return The xml string.
     */
    fun serialize(value: Any?, compressBigContent: Boolean): String {
        val xml = toXml(xstream, value)
        return if (compressBigContent) {
            UserPrefDao.compressIfRequired(xml)
        } else {
            xml
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

    fun saveOrUpdate(userId: Long, key: String?, value: Any?, checkAccess: Boolean) {
        key ?: return
        if (accessChecker.isDemoUser(userId)) {
            // Do nothing.
            return
        }
        if (checkAccess) {
            if (userId != loggedInUserId) {
                throw AccessException("$title: User '$loggedInUserId' has no access to write user preferences of other user '$userId'.")
            }
        }
        synchronized(this) {
            // Avoid parallel insert, update, delete operations.
            val date = Date()
            persistenceService.runInTransaction { context ->
                context.flush()
                val userPrefs = selectUserPreferencesByUserId(userId, key, checkAccess, attached = true)
                    ?: UserXmlPreferencesDO().also {
                        it.created = date
                        it.user = PFUserDO().also { it.id = userId }
                        it.key = key
                    }
                val serialized = serialize(value, compressBigContent = true)
                log.debug { "UserXmlPrefs serialized for db: $serialized" }
                userPrefs.lastUpdate = date
                userPrefs.serializedValue = serialized
                userPrefs.setVersion()
                if (userPrefs.id == null) {
                    log.debug { "Storing new user preference for user '$userId': ${serialized.abbreviate(40)}" }
                    context.insert(userPrefs)
                } else {
                    log.debug { "Updating user preference for user '$userId': ${serialized.abbreviate(40)}" }
                    userPrefs.setVersion()
                    context.flush()
                }
            }
        }
    }

    fun remove(userId: Long, key: String?) {
        if (accessChecker.isDemoUser(userId)) {
            // Do nothing.
            return
        }
        val userPreferencesDO = selectUserPreferencesByUserId(userId, key, true)
        userPreferencesDO?.id?.let { id ->
            persistenceService.runInTransaction { context ->
                context.delete(UserXmlPreferencesDO::class.java, id)
            }
        }
    }
}
