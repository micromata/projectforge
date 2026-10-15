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

package org.projectforge.business.vacation

import jakarta.annotation.PostConstruct
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
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
 * The vacation entries will be cached.
 *
 * @author Kai Reinhard
 */
@Component
open class VacationCache : AbstractCache(), BaseDOModifiedListener<VacationDO> {
    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var vacationDao: VacationDao

    private var vacationMap = mutableMapOf<Long?, VacationDO>()

    private var vacations = listOf<VacationDO>() // Thread safe

    /**
     * Guards [vacationMap], [vacations] and [changedWhileRefreshing]. Not the map itself: [refresh] replaces it.
     */
    private val lock = Any()

    /**
     * The entries [afterInsertOrModify] took while a [refresh] was reading (null value: removed), or null while no
     * refresh runs. The refresh read them before or after their change, so they're applied again to its result:
     * otherwise a change saved meanwhile vanished from the cache until the next refresh.
     */
    private var changedWhileRefreshing: MutableMap<Long, VacationDO?>? = null

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
            log.info { "No groups given, therefore no vacation will be returned." }
            return result
        }
        for (vacation in vacations) {
            if (vacation.endDate?.isBefore(startVacationDate) == true ||
                vacation.startDate?.isAfter(endVacationDate) == true
            ) {
                continue
            }
            if (vacation.status == VacationStatus.REJECTED) {
                continue // Nobody is away: not shown in the calendar nor in its subscriptions.
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
        val id = obj.id ?: return
        // obj is detached here and its lazy associations mustn't be touched (see BaseDOModifiedListener). Only the
        // ids of the employee proxies are read (which needs no query); the graph is then hydrated from the caches,
        // so serving this entry never triggers a lazy load.
        //
        // runReadOnlyForCacheMaintenance, NOT runIsolatedReadOnly: this listener is called from within the insert's
        // write transaction (BaseDOPersistenceService.privateInsert -> BaseDOChangedRegistry.afterInsertOrModify).
        // A second, isolated connection would deadlock on the row locks that still-open transaction holds - it froze
        // VacationDaoTest. The helper reuses the transaction's connection when one is active (seeing the just
        // inserted/updated row) and stays isolated otherwise.
        runReadOnlyForCacheMaintenance { context ->
            hydrate(obj, loadOtherReplacementIds(context, id)[id])
        }
        // Called within the write transaction, so a change that is rolled back afterwards stays in the cache until
        // the next refresh: the persistence layer offers no after-commit hook for the listeners.
        synchronized(lock) {
            if (obj.deleted) {
                vacationMap.remove(id)
            } else {
                vacationMap[id] = obj
            }
            changedWhileRefreshing?.put(id, obj.takeUnless { it.deleted })
            vacations = vacationMap.values.toList()
        }
    }

    /**
     * This method will be called by CacheHelper and is synchronized via getData();
     */
    override fun refresh() {
        log.info { "Refreshing VacationCache ..." }
        // runReadOnlyForCacheMaintenance guards against the same self-deadlock as afterInsertOrModify: a refresh
        // triggered from within a write transaction (e.g. a cache getter called mid-insert) must not open a second
        // connection. Outside a transaction it stays isolated, as a refresh normally is.
        synchronized(lock) {
            changedWhileRefreshing = mutableMapOf()
        }
        try {
            refreshVacations()
        } finally {
            synchronized(lock) {
                changedWhileRefreshing = null
            }
        }
        log.info { "Refreshing of VacationCache done." }
    }

    private fun refreshVacations() {
        runReadOnlyForCacheMaintenance { context ->
            // This method must not be synchronized because it works with a new copy of maps.
            val map = mutableMapOf<Long?, VacationDO>()
            // Read without a logged-in user: the first fill runs on the thread of whoever asks first, and
            // VacationDao.afterLoad would mask the comments that user may not see - for everybody, as the cache is
            // shared. The consumers check the comment per user (VacationProvider).
            withoutLoggedInUser { vacationDao.selectAll(checkAccess = false) }.forEach {
                if (!it.deleted) {
                    map[it.id] = it
                }
            }
            // Hydrate the employee graph of every cached vacation from the caches, so serving the cache
            // (calendar events, access checks) never triggers a lazy load. selectAll returns detached
            // entities, so the other-replacements join table is read in a single query here instead of one
            // lazy collection init per vacation — the N+1 the calendar suffered from.
            val otherReplacementIds = loadOtherReplacementIds(context)
            map.values.forEach { hydrate(it, otherReplacementIds[it.id]) }
            synchronized(lock) {
                changedWhileRefreshing?.forEach { (id, changed) ->
                    if (changed == null) map.remove(id) else map[id] = changed
                }
                vacationMap = map
                vacations = vacationMap.values.toList() // Make a copy for avoiding ConcurrentModificationExceptions
            }
        }
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
     * Hydrates the employee graph of the given vacations read outside this cache (e.g. trimmed to a period), as
     * [hydrate] does for the cached ones: the other replacements of all of them are read in one query instead of
     * one lazy collection init per vacation (N+1), all employees come from [EmployeeCache].
     */
    fun hydrate(vacations: Collection<VacationDO>) {
        val ids = vacations.mapNotNull { it.id }
        if (ids.isEmpty()) {
            return
        }
        val otherReplacementIds = runReadOnlyForCacheMaintenance { context -> loadOtherReplacementIds(context, ids) }
        vacations.forEach { hydrate(it, otherReplacementIds[it.id]) }
    }

    /**
     * Replaces the lazy [EmployeeDO] proxies of the given vacation (employee, manager, replacement and the
     * other replacements) by the fully initialized instances held in [EmployeeCache]. The proxy ids are read
     * without initializing them (a proxy always knows its id), so this is safe on a detached entity and adds
     * no query. Afterwards nothing on the cached vacation is lazy, so serving it hits neither the database nor
     * a [org.hibernate.LazyInitializationException].
     *
     * @param otherReplacementIds The employee ids of [VacationDO.otherReplacements], pre-loaded in one query
     * (see [loadOtherReplacementIds]) to avoid initializing the many-to-many collection per vacation.
     */
    private fun hydrate(vacation: VacationDO, otherReplacementIds: Set<Long>?) {
        vacation.employee = employeeCache.getEmployee(vacation.employee?.id)
        vacation.manager = employeeCache.getEmployee(vacation.manager?.id)
        vacation.replacement = employeeCache.getEmployee(vacation.replacement?.id)
        vacation.otherReplacements = otherReplacementIds
            ?.mapNotNull { employeeCache.getEmployee(it) }
            ?.toMutableSet()
    }

    /**
     * Reads the vacation → other-replacement-employee mapping straight from the join table in a single query,
     * so [hydrate] doesn't have to initialize the many-to-many collection of every vacation one by one.
     *
     * @param vacationId When given, only that vacation's row is read (used when a single entry is updated).
     */
    private fun loadOtherReplacementIds(
        context: PfPersistenceContext,
        vacationId: Long? = null,
    ): Map<Long, MutableSet<Long>> {
        return loadOtherReplacementIds(context, vacationId?.let { listOf(it) })
    }

    /**
     * @param vacationIds When given, only the rows of these vacations are read (in chunks, as an IN clause has a
     * limited number of parameters).
     */
    private fun loadOtherReplacementIds(
        context: PfPersistenceContext,
        vacationIds: Collection<Long>?,
    ): Map<Long, MutableSet<Long>> {
        val sql = "SELECT vacation_id, employee_id FROM t_employee_vacation_other_replacements"
        val result = mutableMapOf<Long, MutableSet<Long>>()
        val queries = if (vacationIds == null) {
            listOf(context.em.createNativeQuery(sql))
        } else {
            vacationIds.distinct().chunked(IN_CLAUSE_CHUNK_SIZE).map { chunk ->
                context.em.createNativeQuery("$sql WHERE vacation_id IN (:vacationIds)")
                    .setParameter("vacationIds", chunk)
            }
        }
        queries.forEach { query ->
            @Suppress("UNCHECKED_CAST")
            (query.resultList as List<Array<Any?>>).forEach { row ->
                val vId = (row[0] as Number).toLong()
                val employeeId = (row[1] as Number).toLong()
                result.getOrPut(vId) { mutableSetOf() }.add(employeeId)
            }
        }
        return result
    }

    companion object {
        private const val IN_CLAUSE_CHUNK_SIZE = 1000
    }
}
