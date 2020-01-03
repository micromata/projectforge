/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.vacation

import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import javax.persistence.LockModeType

/**
 * The kost2 entries will be cached.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
class VacationCache : AbstractCache() {
    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var vacationDao: VacationDao

    private lateinit var vacationSet: Set<VacationDO>

    /**
     * Checks also the select access of the logged in user.
     */
    fun getVacationForPeriodAndUsers(startVacationDate: PFDateTime, endVacationDate: PFDateTime,
                                     groupIds: Set<Int>?, userIds: Set<Int>?): List<VacationDO> {
        checkRefresh()
        val result = mutableListOf<VacationDO>()
        if (groupIds.isNullOrEmpty() && userIds.isNullOrEmpty()) {
            log.info("No groups given, therefore no vacation will be returned.")
            return result
        }
        val userGroupCache = UserGroupCache.getTenantInstance()
        val loggedInUser = ThreadLocalUserContext.getUser()
        for (vacation in vacationSet) {
            if (!vacationDao.hasSelectAccess(vacation, loggedInUser, false)) {
                continue
            }
            val employeeUser = vacation.employee?.user ?: continue
            var match = groupIds?.any { gid ->
                userGroupCache.getGroup(gid)?.assignedUsers?.any { user ->
                    user.id == employeeUser.id // The employee matches with one assigned user of the group.
                } ?: false // Null doesn't match.
            } ?: false // Null doesn't match
            if (!match) { // Search for users
                match = userIds?.any {uid ->
                    uid == employeeUser.id // The employee matches with one given user.
                } ?: false // Null doesn't match
            }
            if (match) {
                result.add(vacation) // Employee is part of group, so return the vacation entry for this user.
            }
        }
        return result
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Initializing VacationCache ...")
        // This method must not be synchronized because it works with a new copy of maps.
        val set = mutableSetOf<VacationDO>()
        val list = em.createQuery("from VacationDO t", VacationDO::class.java)
                .setLockMode(LockModeType.NONE)
                .resultList
        list.forEach {
            if (!it.isDeleted) {
                set.add(it)
            }
        }
        vacationSet = set
        log.info("Initializing of VacationCache done.")
    }

    companion object {
        private val log = LoggerFactory.getLogger(VacationCache::class.java)
    }
}
