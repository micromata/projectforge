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

package org.projectforge.business.vacation

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * The vacation entries will be cached.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
open class VacationCache : AbstractCache(), BaseDOModifiedListener<VacationDO> {
    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var vacationDao: VacationDao

    private var vacationMap = mutableMapOf<Long?, VacationDO>()

    private var vacations = listOf<VacationDO>() // Thread safe

    @PostConstruct
    private fun postConstruct() {
        vacationDao.register(this)
    }

    /**
     * Checks also the select access of the logged in user.
     * @param groupIds Null items should only occur on (de)serialization issues.
     * @param userIds Null items should only occur on (de)serialization issues.
     */
    open fun getVacationForPeriodAndUsers(
        startVacationDate: LocalDate, endVacationDate: LocalDate,
        groupIds: Set<Long?>?, userIds: Set<Long?>?
    ): List<VacationDO> {
        checkRefresh()
        val result = mutableListOf<VacationDO>()
        if (groupIds.isNullOrEmpty() && userIds.isNullOrEmpty()) {
            log.info("No groups given, therefore no vacation will be returned.")
            return result
        }
        for (vacation in vacations) {
            if (vacation.endDate?.isBefore(startVacationDate) == true ||
                vacation.startDate?.isAfter(endVacationDate) == true
            ) {
                continue
            }
            if (!vacationDao.hasSelectAccess(vacation, ThreadLocalUserContext.requiredLoggedInUser)) {
                continue
            }
            val employeeUser = employeeCache.getUser(vacation.employee) ?: continue
            var match = groupIds?.any { gid ->
                userGroupCache.getGroup(gid)?.assignedUsers?.any { user ->
                    user.id == employeeUser.id // The employee matches with one assigned user of the group.
                } ?: false // Null doesn't match.
            } ?: false // Null doesn't match
            if (!match) { // Search for users
                match = userIds?.any { uid ->
                    uid == employeeUser.id // The employee matches with one given user.
                } ?: false // Null doesn't match
            }
            if (match) {
                result.add(vacation) // Employee is part of group, so return the vacation entry for this user.
            }
        }
        return result
    }

    override fun afterInsertOrModify(obj: VacationDO, operationType: OperationType) {
        synchronized(vacationMap) {
            vacationMap[obj.id] = obj
            vacations = vacationMap.values.toList()
        }
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info("Refreshing VacationCache ...")
        persistenceService.runIsolatedReadOnly {
            // This method must not be synchronized because it works with a new copy of maps.
            val map = mutableMapOf<Long?, VacationDO>()
            vacationDao.selectAll(checkAccess = false).forEach {
                if (!it.deleted) {
                    map[it.id] = it
                }
            }
            vacationMap = map
            vacations = vacationMap.values.toList() // Make a copy for avoiding ConcurrentModificationExceptions
        }
        log.info("Refreshing of VacationCache done.")
    }
}
