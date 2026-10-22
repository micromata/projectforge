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
 * tree shows a user booking by a grant only: the granted tasks and their ancestors, titles only.
 *
 * Holds [Grant] values, not the entities: read on every access check of a time sheet, they need no session. Revoked
 * (deleted) grants are kept, they still give read access to the own time sheets.
 */
@Component
open class TaskBookingGrantCache : AbstractCache(), BaseDOModifiedListener<TaskBookingGrantDO> {
    /**
     * The values of a grant the access checks need.
     */
    data class Grant(
        val id: Long,
        val taskId: Long,
        val userId: Long,
        val validFrom: LocalDate,
        val validUntil: LocalDate,
        val kost2Id: Long?,
        val accepted: Boolean,
        val deleted: Boolean,
    ) {
        /** Accepted and not revoked: the user may book on the task within the period. */
        val active: Boolean
            get() = accepted && !deleted

        /** Active and not expired: the task is shown to the user (period not yet begun included). */
        fun isCurrent(today: LocalDate): Boolean = active && !validUntil.isBefore(today)

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

    @PostConstruct
    private fun postConstruct() {
        taskBookingGrantDao.register(this)
    }

    /**
     * All grants of the user, also the revoked, expired and not yet accepted ones.
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
        return getGrants(userId).filter { it.isCurrent(today) }.mapTo(mutableSetOf()) { it.taskId }
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
     * - [OperationType.SELECT]: there is an accepted grant on the task, even a revoked or expired one: the own time
     *   sheets stay readable.
     * - Any other operation: there is an active grant on the task, today is within its period and so is the time
     *   sheet ([startDay], [stopDay]); a grant with a cost unit allows only this one ([kost2Id]).
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
        val grants = getGrants(userId).filter { it.taskId == taskId && it.accepted }
        if (operationType == OperationType.SELECT) {
            return grants.isNotEmpty()
        }
        val today = PFDay.now().localDate
        return grants.any { grant ->
            grant.active && grant.isValidAt(today) &&
                    startDay != null && grant.isValidAt(startDay) &&
                    (stopDay == null || grant.isValidAt(stopDay)) &&
                    (grant.kost2Id == null || grant.kost2Id == kost2Id)
        }
    }

    /**
     * The cost unit the user is bound to on the task by the grant valid today, or null if the grant allows every
     * cost unit of the task (or there is none).
     */
    open fun getFixedKost2Id(userId: Long?, taskId: Long?): Long? {
        val today = PFDay.now().localDate
        val grants = getGrants(userId).filter { it.taskId == taskId && it.isCurrent(today) }
        if (grants.isEmpty() || grants.any { it.kost2Id == null }) {
            return null
        }
        return grants.first().kost2Id
    }

    override fun afterInsertOrModify(obj: TaskBookingGrantDO, operationType: OperationType) {
        val grant = toGrant(obj) ?: return
        synchronized(lock) {
            modifiedDuringRefresh?.put(grant.id, grant)
            grantMap[grant.id] = grant
            grantsByUser = groupByUser(grantMap.values)
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
                taskBookingGrantDao.selectAll(checkAccess = false).forEach { obj ->
                    toGrant(obj)?.let { map[it.id] = it }
                }
                synchronized(lock) {
                    modifiedDuringRefresh?.values?.forEach { map[it.id] = it }
                    grantMap = map
                    grantsByUser = groupByUser(map.values)
                }
            }
        } finally {
            synchronized(lock) {
                modifiedDuringRefresh = null
            }
        }
        log.info { "Refreshing of TaskBookingGrantCache done." }
    }

    private fun groupByUser(grants: Collection<Grant>): Map<Long, List<Grant>> {
        return grants.groupBy { it.userId }
    }

    /**
     * The ids of the lazy associations are read without initializing them.
     */
    private fun toGrant(obj: TaskBookingGrantDO): Grant? {
        return Grant(
            id = obj.id ?: return null,
            taskId = obj.task?.id ?: return null,
            userId = obj.user?.id ?: return null,
            validFrom = obj.validFrom ?: return null,
            validUntil = obj.validUntil ?: return null,
            kost2Id = obj.kost2?.id,
            accepted = obj.accepted,
            deleted = obj.deleted,
        )
    }
}
