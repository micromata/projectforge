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
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
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
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    /** Guards [availabilityMap] and [modifiedDuringRefresh]. */
    private val lock = Any()

    private var availabilityMap = mutableMapOf<Long?, AvailabilityDO>()

    /**
     * The entries inserted or modified while a [refresh] reads the database: its result may lack them (read before
     * their commit), so they are applied to it afterwards. Null while no refresh is running.
     */
    private var modifiedDuringRefresh: MutableMap<Long?, AvailabilityDO>? = null

    @Volatile
    private var availabilities = listOf<AvailabilityDO>() // Thread safe

    @PostConstruct
    private fun postConstruct() {
        availabilityDao.register(this)
    }

    /**
     * The entries of the members of the given groups and of the given users overlapping the period. Only of the users
     * sharing a group with the logged-in user ([UserGroupCache.getGroupMateIds]) unless HR or finance staff
     * ([VacationDao.hasLoggedInUserUnrestrictedVacationView]), and only those the logged-in user may see.
     * @param groupIds Null items should only occur on (de)serialization issues.
     * @param userIds Null items should only occur on (de)serialization issues.
     */
    open fun getAvailabilityForPeriodAndUsers(
        startDate: LocalDate, endDate: LocalDate,
        groupIds: Set<Long?>?, userIds: Set<Long?>?
    ): List<AvailabilityDO> {
        if (groupIds.isNullOrEmpty() && userIds.isNullOrEmpty()) {
            return emptyList()
        }
        checkRefresh()
        val viewer = ThreadLocalUserContext.requiredLoggedInUser
        val shownUserIds = userGroupCache.getMemberIds(groupIds, userIds)
        if (!vacationDao.hasLoggedInUserUnrestrictedVacationView()) {
            shownUserIds.retainAll(userGroupCache.getGroupMateIds(viewer))
        }
        if (shownUserIds.isEmpty()) {
            return emptyList()
        }
        return availabilities.filter { availability ->
            availability.user?.id in shownUserIds &&
                    availability.endDate?.isBefore(startDate) == false &&
                    availability.startDate?.isAfter(endDate) == false &&
                    availabilityDao.hasSelectAccess(availability, viewer)
        }
    }

    /**
     * The entries of the given users overlapping the period, only those the logged-in user may see. The internal
     * comment is kept raw (see [AvailabilityDao.isInternalCommentVisibleFor]).
     */
    open fun getAvailabilityForPeriodAndUserIds(
        startDate: LocalDate, endDate: LocalDate,
        userIds: Collection<Long>,
    ): List<AvailabilityDO> {
        if (userIds.isEmpty()) {
            return emptyList()
        }
        checkRefresh()
        val ids = userIds.toSet()
        val user = ThreadLocalUserContext.requiredLoggedInUser
        return availabilities.filter { availability ->
            availability.user?.id in ids &&
                    availability.endDate?.isBefore(startDate) == false &&
                    availability.startDate?.isAfter(endDate) == false &&
                    availabilityDao.hasSelectAccess(availability, user)
        }
    }

    /**
     * The entries of the given users overlapping the period, whoever is logged in: for the conflicts of a vacation,
     * which need its substitutes on duty (see VacationService.getVacationOverlaps), counting the absent ones
     * ([AvailabilityStatus.absent]). Not to be shown as they are: the caller checks the access.
     */
    open fun getEntriesOfUsers(
        userIds: Collection<Long>,
        startDate: LocalDate, endDate: LocalDate,
    ): List<AvailabilityDO> {
        if (userIds.isEmpty()) {
            return emptyList()
        }
        checkRefresh()
        val ids = userIds.toSet()
        return availabilities.filter { availability ->
            availability.user?.id in ids &&
                    availability.endDate?.isBefore(startDate) == false &&
                    availability.startDate?.isAfter(endDate) == false
        }
    }

    override fun afterInsertOrModify(obj: AvailabilityDO, operationType: OperationType) {
        synchronized(lock) {
            modifiedDuringRefresh?.put(obj.id, obj)
            apply(availabilityMap, obj)
            availabilities = availabilityMap.values.toList()
        }
    }

    private fun apply(map: MutableMap<Long?, AvailabilityDO>, obj: AvailabilityDO) {
        if (obj.deleted) {
            map.remove(obj.id)
        } else {
            map[obj.id] = obj
        }
    }

    override fun refresh() {
        log.info("Refreshing AvailabilityCache ...")
        // runReadOnlyForCacheMaintenance, not runIsolatedReadOnly: a refresh triggered from within a write transaction
        // (e.g. a cache getter called mid-insert) mustn't open a second connection.
        synchronized(lock) {
            modifiedDuringRefresh = mutableMapOf()
        }
        try {
            runReadOnlyForCacheMaintenance {
                val map = mutableMapOf<Long?, AvailabilityDO>()
                // Read without a logged-in user: the first fill runs on the thread of whoever asks first, and
                // AvailabilityDao.afterLoad would hide the type and internal comment that user may not see - for
                // everybody, as the cache is shared. The consumers decide per user (AvailabilityDao.labelFor).
                withoutLoggedInUser { availabilityDao.selectAll(checkAccess = false) }.forEach {
                    if (!it.deleted) {
                        map[it.id] = it
                    }
                }
                synchronized(lock) {
                    modifiedDuringRefresh?.values?.forEach { apply(map, it) }
                    availabilityMap = map
                    availabilities = availabilityMap.values.toList()
                }
            }
        } finally {
            synchronized(lock) {
                modifiedDuringRefresh = null
            }
        }
        log.info("Refreshing of AvailabilityCache done.")
    }

    private fun <T> withoutLoggedInUser(block: () -> T): T {
        val userContext = ThreadLocalUserContext.userContext ?: return block()
        ThreadLocalUserContext.clear()
        try {
            return block()
        } finally {
            ThreadLocalUserContext.userContext = userContext
        }
    }
}
