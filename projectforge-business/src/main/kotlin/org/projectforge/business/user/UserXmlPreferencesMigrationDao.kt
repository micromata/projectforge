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

import org.apache.commons.lang3.Validate
import org.projectforge.business.user.UserXmlPreferencesDO.Companion.getCurrentVersion
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Stores all user persistent objects such as filter settings, personal settings and persists them to the database.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class UserXmlPreferencesMigrationDao {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var userXmlPreferencesDao: UserXmlPreferencesDao

    @Autowired
    private lateinit var userXmlPreferencesCache: UserXmlPreferencesCache

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    open fun migrateAllUserPrefs(): String {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        val buf = StringBuilder()
        val list = persistenceService.executeQuery(
            "from ${UserXmlPreferencesDO::class.java.simpleName} t order by user.id, key",
            UserXmlPreferencesDO::class.java,
        )
        var versionNumber = Int.MAX_VALUE
        for (userPrefs in list) {
            buf.append(migrateUserPrefs(userPrefs))
            if (userPrefs.version < versionNumber) {
                versionNumber = userPrefs.version
            }
        }
        migrate(versionNumber)
        userXmlPreferencesCache.refresh()
        return buf.toString()
    }

    /**
     * Here you can insert update or delete statements for all user xml pref entries (e. g. delete all entries with an
     * unused key).
     *
     * @param version Version number of oldest entry.
     */
    protected fun migrate(version: Int) {
        if (version < 4) {
            // deleteOldKeys("org.projectforge.web.humanresources.HRViewForm:Filter");
            // deleteOldKeys("org.projectforge.web.fibu.AuftragListAction:Filter");
            // deleteOldKeys("OLD-VERSION-1.1");
            // hibernateTemplate.flush();
        }
    }

    /**
     * Unsupported or unused keys should be deleted. This method deletes all entries with the given key.
     *
     * @param key Key of the entries to delete.
     */
    protected fun deleteOldKeys(key: String) {
        val numberOfUpdatedEntries = persistenceService.runInTransaction { context ->
            context.executeUpdate("delete from ${UserXmlPreferencesDO::class.java.simpleName} where key = '$key'")
        }
        log.info("$numberOfUpdatedEntries '$key' entries deleted.")
    }

    protected fun migrateUserPrefs(userPrefs: UserXmlPreferencesDO): String {
        val userId = userPrefs.user?.id ?: return ""
        val buf = StringBuilder()
        buf.append("Checking user preferences for user '")
        val user = userGroupCache.getUser(userId)
        if (user != null) {
            buf.append(user.username)
        } else {
            buf.append(userId)
        }
        buf.append("': ").append(userPrefs.identifier).append(" ... ")
        if (userPrefs.version >= getCurrentVersion()) {
            buf.append("version ").append(userPrefs.version).append(" (up to date)\n")
            return buf.toString()
        }
        migrate(userPrefs)
        val data = userXmlPreferencesDao.deserialize(userPrefs)
        buf.append("version ")
        buf.append(userPrefs.version)
        if (data != null || "<null/>" == userPrefs.serializedValue) {
            buf.append(" OK ")
        } else {
            buf.append(" ***not re-usable*** ")
        }
        buf.append("\n")
        if (data == null) {
            return buf.toString()
        }
        return buf.toString()
    }

    companion object {
        private val log: Logger = LoggerFactory
            .getLogger(UserXmlPreferencesMigrationDao::class.java)

        /**
         * Fixes incompatible versions of user preferences before de-serialization.
         *
         * @param userPrefs
         */
        fun migrate(userPrefs: UserXmlPreferencesDO) {
            if (userPrefs.version < 4) {
                userPrefs.version = 4
            }
        }
    }
}
