/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.availability

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.user.UserGroupCache
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
 * The availability entries will be cached for calendar display.
 */
@Component
open class AvailabilityCache : AbstractCache(), BaseDOModifiedListener<AvailabilityDO> {
    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    private var availabilityMap = mutableMapOf<Long?, AvailabilityDO>()

    private var availabilities = listOf<AvailabilityDO>() // Thread safe

    @PostConstruct
    private fun postConstruct() {
        availabilityDao.register(this)
    }

    /**
     * Checks also the select access of the logged in user.
     * @param groupIds Null items should only occur on (de)serialization issues.
     * @param userIds Null items should only occur on (de)serialization issues.
     */
    open fun getAvailabilityForPeriodAndUsers(
        startDate: LocalDate, endDate: LocalDate,
        groupIds: Set<Long?>?, userIds: Set<Long?>?
    ): List<AvailabilityDO> {
        checkRefresh()
        val result = mutableListOf<AvailabilityDO>()
        if (groupIds.isNullOrEmpty() && userIds.isNullOrEmpty()) {
            return result
        }
        for (availability in availabilities) {
            if (availability.endDate?.isBefore(startDate) == true ||
                availability.startDate?.isAfter(endDate) == true
            ) {
                continue
            }
            if (!availabilityDao.hasSelectAccess(availability, ThreadLocalUserContext.requiredLoggedInUser)) {
                continue
            }
            val employeeUser = employeeCache.getUser(availability.employee) ?: continue
            var match = groupIds?.any { gid ->
                userGroupCache.getGroup(gid)?.assignedUsers?.any { user ->
                    user.id == employeeUser.id
                } ?: false
            } ?: false
            if (!match) {
                match = userIds?.any { uid ->
                    uid == employeeUser.id
                } ?: false
            }
            if (match) {
                result.add(availability)
            }
        }
        return result
    }

    override fun afterInsertOrModify(obj: AvailabilityDO, operationType: OperationType) {
        synchronized(availabilityMap) {
            availabilityMap[obj.id] = obj
            availabilities = availabilityMap.values.toList()
        }
    }

    override fun refresh() {
        log.info("Refreshing AvailabilityCache ...")
        persistenceService.runIsolatedReadOnly {
            val map = mutableMapOf<Long?, AvailabilityDO>()
            availabilityDao.selectAll(checkAccess = false).forEach {
                if (!it.deleted) {
                    map[it.id] = it
                }
            }
            availabilityMap = map
            availabilities = availabilityMap.values.toList()
        }
        log.info("Refreshing of AvailabilityCache done.")
    }
}
