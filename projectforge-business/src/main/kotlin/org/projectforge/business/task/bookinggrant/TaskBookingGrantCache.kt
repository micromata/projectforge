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

package org.projectforge.business.task.bookinggrant

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.hibernate.Hibernate
import org.projectforge.business.task.TaskTree
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * The booking grants ([TaskBookingGrantDO]) of all users, for the access checks of time sheets and for what the task
 * tree shows: a user booking by a grant only sees the granted tasks and their ancestors, titles only; who may grant
 * sees the grants on a task.
 *
 * Holds [Grant] values, not the entities: read on every access check of a time sheet, they need no session. Revoked
 * (deleted) grants are kept, they still give read access to the own time sheets.
 */
@Component
open class TaskBookingGrantCache : AbstractCache(), BaseDOModifiedListener<TaskBookingGrantDO> {
    /**
     * The values of a grant the access checks need.
     * @param positions The tasks of the grant, each with the cost units it may be booked on (null for all).
     */
    data class Grant(
        val id: Long,
        val userId: Long,
        val validFrom: LocalDate,
        val validUntil: LocalDate,
        val positions: Map<Long, Set<Long>?>,
        val deleted: Boolean,
    ) {
        /** Not revoked and not expired: the tasks are shown to the user (period not yet begun included). */
        fun isCurrent(today: LocalDate): Boolean = !deleted && !validUntil.isBefore(today)

        fun isValidAt(day: LocalDate): Boolean = !day.isBefore(validFrom) && !day.isAfter(validUntil)
    }

    @Autowired
    private lateinit var taskBookingGrantDao: TaskBookingGrantDao

    @Autowired
    private lateinit var taskTree: TaskTree

    /** Guards [grantMap] and [modifiedDuringRefresh]. */
    private val lock = Any()

    private var grantMap = mutableMapOf<Long, Grant>()

    /**
     * The grants inserted or modified while a [refresh] reads the database: its result may lack them (read before
     * their commit), so they are applied to it afterwards. Null while no refresh is running.
     */
    private var modifiedDuringRefresh: MutableMap<Long, Grant>? = null

    @Volatile
    private var grantsByUser = mapOf<Long, List<Grant>>() // Thread safe

    @Volatile
    private var grantsByTask = mapOf<Long, List<Grant>>() // Thread safe

    @PostConstruct
    private fun postConstruct() {
        taskBookingGrantDao.register(this)
    }

    /**
     * All grants of the user, also the revoked and expired ones.
     */
    open fun getGrants(userId: Long?): List<Grant> {
        userId ?: return emptyList()
        checkRefresh()
        return grantsByUser[userId] ?: emptyList()
    }

    /**
     * The tasks the user has a current grant for ([Grant.isCurrent]).
     */
    open fun getCurrentTaskIds(userId: Long?): Set<Long> {
        val today = PFDay.now().localDate
        return getGrants(userId).filter { it.isCurrent(today) }.flatMapTo(mutableSetOf()) { it.positions.keys }
    }

    /**
     * The current grants ([Grant.isCurrent]) with a position on the given task, of all users: what the task tree shows
     * who may grant on the task.
     */
    open fun getCurrentGrantsOnTask(taskId: Long?): List<Grant> {
        taskId ?: return emptyList()
        checkRefresh()
        val today = PFDay.now().localDate
        return grantsByTask[taskId]?.filter { it.isCurrent(today) } ?: emptyList()
    }

    /**
     * The tasks the user has a current grant for and all their ancestors (root excluded): the nodes the tree shows
     * the user although they may not see them otherwise, titles only.
     */
    open fun getVisiblePathTaskIds(userId: Long?): Set<Long> {
        val result = mutableSetOf<Long>()
        getCurrentTaskIds(userId).forEach { taskId ->
            taskTree.getPathToRoot(taskId).forEach { node -> result.add(node.taskId) }
        }
        return result
    }

    /**
     * Whether the user may do the given operation on an own time sheet of the given task by a grant.
     *
     * - [OperationType.SELECT]: there is a grant with the task, even a revoked or expired one: the own time sheets
     *   stay readable.
     * - Any other operation: there is a grant with the task, not revoked, today is within its period and so is the
     *   time sheet ([startDay], [stopDay]); a position with cost units allows only these ([kost2Id]).
     */
    open fun allows(
        userId: Long?,
        taskId: Long?,
        operationType: OperationType,
        startDay: LocalDate?,
        stopDay: LocalDate?,
        kost2Id: Long?,
    ): Boolean {
        taskId ?: return false
        val grants = getGrants(userId).filter { it.positions.containsKey(taskId) }
        if (operationType == OperationType.SELECT) {
            return grants.isNotEmpty()
        }
        val today = PFDay.now().localDate
        return grants.any { grant ->
            val kost2Ids = grant.positions[taskId]
            !grant.deleted && grant.isValidAt(today) &&
                    startDay != null && grant.isValidAt(startDay) &&
                    (stopDay == null || grant.isValidAt(stopDay)) &&
                    (kost2Ids == null || kost2Id in kost2Ids)
        }
    }

    /**
     * The cost units the user may book on the task by the current grants, or null if one of them allows every cost
     * unit of the task (or there is none).
     */
    open fun getAllowedKost2Ids(userId: Long?, taskId: Long?): Set<Long>? {
        taskId ?: return null
        val today = PFDay.now().localDate
        val grants = getGrants(userId).filter { it.positions.containsKey(taskId) && it.isCurrent(today) }
        if (grants.isEmpty() || grants.any { it.positions[taskId] == null }) {
            return null
        }
        return grants.flatMapTo(mutableSetOf()) { it.positions[taskId]!! }
    }

    override fun afterInsertOrModify(obj: TaskBookingGrantDO, operationType: OperationType) {
        synchronized(lock) {
            // The positions of a grant only revoked (loaded without them) are the cached ones.
            val grant = toGrant(obj, grantMap[obj.id]?.positions) ?: return
            modifiedDuringRefresh?.put(grant.id, grant)
            grantMap[grant.id] = grant
            index(grantMap.values)
        }
    }

    override fun refresh() {
        log.info { "Refreshing TaskBookingGrantCache ..." }
        synchronized(lock) {
            modifiedDuringRefresh = mutableMapOf()
        }
        try {
            runReadOnlyForCacheMaintenance {
                val map = mutableMapOf<Long, Grant>()
                taskBookingGrantDao.selectAllWithPositions().forEach { obj ->
                    toGrant(obj, null)?.let { map[it.id] = it }
                }
                synchronized(lock) {
                    modifiedDuringRefresh?.values?.forEach { map[it.id] = it }
                    grantMap = map
                    index(map.values)
                }
            }
        } finally {
            synchronized(lock) {
                modifiedDuringRefresh = null
            }
        }
        log.info { "Refreshing of TaskBookingGrantCache done." }
    }

    /** Called with [lock] held. */
    private fun index(grants: Collection<Grant>) {
        grantsByUser = grants.groupBy { it.userId }
        val byTask = mutableMapOf<Long, MutableList<Grant>>()
        grants.forEach { grant -> grant.positions.keys.forEach { byTask.getOrPut(it) { mutableListOf() }.add(grant) } }
        grantsByTask = byTask
    }

    /**
     * The ids of the lazy associations are read without initializing them.
     * @param knownPositions The positions to take if those of the object aren't loaded.
     */
    private fun toGrant(obj: TaskBookingGrantDO, knownPositions: Map<Long, Set<Long>?>?): Grant? {
        val positions = if (Hibernate.isInitialized(obj.positions) && obj.positions != null) {
            obj.positions!!.filter { !it.deleted }.mapNotNull { pos -> pos.task?.id?.let { it to pos.kost2IdSet } }.toMap()
        } else {
            knownPositions ?: return null
        }
        return Grant(
            id = obj.id ?: return null,
            userId = obj.user?.id ?: return null,
            validFrom = obj.validFrom ?: return null,
            validUntil = obj.validUntil ?: return null,
            positions = positions,
            deleted = obj.deleted,
        )
    }
}
