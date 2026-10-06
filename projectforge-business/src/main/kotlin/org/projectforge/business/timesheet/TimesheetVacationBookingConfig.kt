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

package org.projectforge.business.timesheet

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskTree
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The task (Strukturelement) and cost unit (Kost2) a vacation is booked on, when the days of a vacation are taken
 * over as time sheets ([TimesheetDayBookingService]). Both are configured together in projectforge.properties:
 *
 * ```
 * projectforge.timesheet.vacation.taskId=4711
 * projectforge.timesheet.vacation.kost2=5.000.00.03
 * ```
 */
@Service
open class TimesheetVacationBookingConfig {
    class VacationBooking(val task: TaskDO, val kost2: Kost2DO)

    @Value("\${projectforge.timesheet.vacation.taskId:}")
    private var taskIdProperty: String? = null

    @Value("\${projectforge.timesheet.vacation.kost2:}")
    private var kost2Property: String? = null

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var kostCache: KostCache

    /** The reason of the last failed resolution, so it is logged once rather than on every call. */
    private var lastWarning: String? = null

    /**
     * Resolved on every call rather than at startup: the task tree and the cost cache aren't ready when this bean is
     * created, and a task or cost unit may be changed while the system runs.
     *
     * @return null if not configured, or if the configuration names no task, no cost unit, or a cost unit not
     * bookable on the task (a warning is logged once in the last two cases).
     */
    open fun resolve(): VacationBooking? {
        val taskIdString = taskIdProperty?.trim()
        val kost2String = kost2Property?.trim()
        if (taskIdString.isNullOrEmpty() && kost2String.isNullOrEmpty()) {
            return null
        }
        val task = taskIdString?.toLongOrNull()?.let { taskTree.getTaskById(it) }
        if (task == null) {
            return warn("projectforge.timesheet.vacation.taskId='$taskIdString' names no task")
        }
        val kost2 = kostCache.getKost2(kost2String)
        if (kost2 == null) {
            return warn("projectforge.timesheet.vacation.kost2='$kost2String' names no cost unit (format #.###.##.##)")
        }
        if (taskTree.getKost2List(task.id)?.any { it.id == kost2.id } != true) {
            return warn(
                "projectforge.timesheet.vacation.kost2='$kost2String' is not bookable on task " +
                        "projectforge.timesheet.vacation.taskId=${task.id} ('${task.title}')"
            )
        }
        lastWarning = null
        return VacationBooking(task, kost2)
    }

    private fun warn(message: String): VacationBooking? {
        if (message != lastWarning) {
            log.warn { "Vacation booking of time sheets not available: $message." }
            lastWarning = message
        }
        return null
    }
}
