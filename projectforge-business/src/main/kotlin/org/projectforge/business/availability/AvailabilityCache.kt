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
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
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
     * The entries of the members of the given groups and of the given users overlapping the period, only those the
     * logged-in user may see ([AvailabilityDao.hasUserSelectAccess]: group mates up to VISIBLE_PAST_DAYS, HR staff all).
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

    /**
     * The cached (not deleted) entry of the given id with its substitutes resolved, or null. Its lazy associations are
     * replaced by the instances of [UserGroupCache] (see [hydrate]), so reading them needs no session.
     */
    open fun getEntry(id: Long?): AvailabilityDO? {
        id ?: return null
        checkRefresh()
        return synchronized(lock) { availabilityMap[id] }
    }

    /**
     * The cached (not deleted) entries with at least one substitute ending on or after the given day: whose
     * substitutes may be away (see ConflictingAvailabilitiesCache).
     */
    open fun getEntriesWithSubstitutes(endingFrom: LocalDate): List<AvailabilityDO> {
        checkRefresh()
        return availabilities.filter {
            !it.substitutes.isNullOrEmpty() && it.endDate?.isBefore(endingFrom) == false
        }
    }

    override fun afterInsertOrModify(obj: AvailabilityDO, operationType: OperationType) {
        val id = obj.id
        if (id != null && !obj.deleted) {
            // obj may be detached: its lazy substitutes mustn't be touched. They are read from the join table within
            // the write transaction (runReadOnlyForCacheMaintenance reuses its connection, see VacationCache).
            runReadOnlyForCacheMaintenance { context -> hydrate(obj, loadSubstituteIds(context, id)[id]) }
        }
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
            runReadOnlyForCacheMaintenance { context ->
                val map = mutableMapOf<Long?, AvailabilityDO>()
                // Read without a logged-in user: the first fill runs on the thread of whoever asks first, and
                // AvailabilityDao.afterLoad would hide the type and internal comment that user may not see - for
                // everybody, as the cache is shared. The consumers decide per user (AvailabilityDao.labelFor).
                withoutLoggedInUser { availabilityDao.selectAll(checkAccess = false) }.forEach {
                    if (!it.deleted) {
                        map[it.id] = it
                    }
                }
                // The substitutes of all entries in one query, instead of one lazy collection init per entry.
                val substituteIds = loadSubstituteIds(context)
                map.values.forEach { hydrate(it, substituteIds[it.id]) }
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

    /**
     * Replaces the lazy user proxy and the lazy substitutes of the given entry by the instances of [UserGroupCache]:
     * serving it hits neither the database nor a LazyInitializationException. The proxy's id is read without
     * initializing it.
     */
    private fun hydrate(entry: AvailabilityDO, substituteIds: Set<Long>?) {
        entry.user = userGroupCache.getUser(entry.user?.id) ?: entry.user
        entry.substitutes = substituteIds?.mapNotNull { userGroupCache.getUser(it) }?.toMutableSet()
    }

    /**
     * The substitutes' user ids per entry, straight from the join table.
     * @param entryId When given, only the rows of this entry are read.
     */
    private fun loadSubstituteIds(context: PfPersistenceContext, entryId: Long? = null): Map<Long, Set<Long>> {
        val sql = "SELECT availability_id, user_id FROM t_user_availability_substitute"
        val query = if (entryId == null) {
            context.em.createNativeQuery(sql)
        } else {
            context.em.createNativeQuery("$sql WHERE availability_id = :entryId").setParameter("entryId", entryId)
        }
        val result = mutableMapOf<Long, MutableSet<Long>>()
        @Suppress("UNCHECKED_CAST")
        (query.resultList as List<Array<Any?>>).forEach { row ->
            result.getOrPut((row[0] as Number).toLong()) { mutableSetOf() }.add((row[1] as Number).toLong())
        }
        return result
    }
}
