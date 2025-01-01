/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.ShutdownService
import org.projectforge.framework.persistence.user.entities.UserPrefDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * A cache for UserPrefDO, if preferences are modified and accessed very often by the user's normal work
 * (such as current filters in Calendar and list pages etc.)
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
@DependsOn("entityManagerFactory")
class UserPrefCache : AbstractUserPrefCache<UserPrefDO>("UserPrefCache", "area") {
    @Autowired
    private lateinit var shutdownService: ShutdownService

    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    @PostConstruct
    private fun postConstruct() {
        shutdownService.registerListener(this)
    }

    override fun newEntry(): UserPrefDO {
        return UserPrefDO()
    }

    override fun selectUserPreferencesByUserId(userId: Long): Collection<UserPrefDO> {
        return userPrefDao.selectUserPrefs(userId)
    }

    override fun saveOrUpdate(userId: Long, key: UserPrefCacheDataKey, value: Any, checkAccess: Boolean) {
        userPrefDao.insertOrUpdate(userId, key, value, checkAccess)
    }

    override fun remove(userId: Long, key: UserPrefCacheDataKey) {
        throw UnsupportedOperationException("Not implemented yet.")
    }

    override fun deserialize(userPref: UserPrefDO): Any? {
        return userPrefDao.deserizalizeValueObject(userPref)
    }

    override fun serialize(value: Any): String {
        return UserPrefDao.serialize(value, compressBigContent = true)
    }

    override fun setExpireTimeInMinutes(expireTime: Long) {
        this.expireTime = 10 * TICKS_PER_MINUTE
    }

    companion object {
        /**
         * If true, the preDestroy will not call sync to database. This is useful for tests, but should never be called
         * in production mode. Otherwise, user data will be lost.
         * This is a static variable, because the preDestroy method is called by the Spring container and the test
         * In test cases the database connections may be closed before [preDestroy] is called.
         * This is a workaround for this problem.
         * This variable is also used by [UserXmlPreferencesCache].
         */
        @JvmStatic
        var dontCallShutdownInTestMode = false
    }
}
