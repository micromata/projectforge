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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

// private val log = KotlinLogging.logger {}

/**
 * Stores all user persistent objects such as filter settings, personal settings and persists them to the database.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
@DependsOn("entityManagerFactory")
class UserXmlPreferencesCache
    : AbstractUserPrefCache<UserXmlPreferencesDO>("UserXmlPreferencesCache", "key") {
    @Autowired
    private lateinit var userXmlPreferencesDao: UserXmlPreferencesDao

    override fun newEntry(): UserXmlPreferencesDO {
        return UserXmlPreferencesDO()
    }

    override fun getUserPreferencesByUserId(userId: Long): Collection<UserXmlPreferencesDO>? {
        return userXmlPreferencesDao.getUserPreferencesByUserId(userId)
    }

    override fun saveOrUpdate(userPref: UserXmlPreferencesDO, value: Any, checkAccess: Boolean) {
        userXmlPreferencesDao.saveOrUpdate(userPref, value, checkAccess)
    }

    override fun remove(userPref: UserXmlPreferencesDO) {
        val userId = userPref.user?.id ?: return
        val key = userPref.identifier ?: return
        userXmlPreferencesDao.remove(userId, key)
    }

    /**
     * Please note: uncompressed value is needed for comparison.
     */
    override fun serialize(value: Any): String {
        return userXmlPreferencesDao.serialize(value)
    }

    override fun deserialize(userPref: UserXmlPreferencesDO): Any? {
        return userXmlPreferencesDao.deserialize(userPref)
    }
}
