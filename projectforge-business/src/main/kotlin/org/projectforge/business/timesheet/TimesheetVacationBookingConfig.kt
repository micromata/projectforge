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
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationLegacyProperties
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The task (Strukturelement) and cost unit (Kost2) a vacation is booked on, when the days of a vacation are taken
 * over as time sheets ([TimesheetDayBookingService], by the user in the calendar or by the nightly
 * [VacationTimesheetAutoBookingJob]). Both are configured together by the finance department on the configuration
 * page ([ConfigurationParam.TIMESHEET_VACATION_TASK_ID], [ConfigurationParam.TIMESHEET_VACATION_KOST2]).
 *
 * Former installations configured them in projectforge.properties:
 * ```
 * projectforge.timesheet.vacation.taskId=4711
 * projectforge.timesheet.vacation.kost2=5.000.00.03
 * ```
 * A property is taken over once, as initial value of its parameter when the parameter's row is created on startup
 * ([ConfigurationLegacyProperties], [ConfigurationParam.getLegacyProperty]). Afterwards only the parameter applies, also if
 * it is cleared on the configuration page.
 */
@Service
open class TimesheetVacationBookingConfig {
    class VacationBooking(val task: TaskDO, val kost2: Kost2DO)

    @Value("\${projectforge.timesheet.vacation.taskId:}")
    private var taskIdProperty: String? = null

    @Value("\${projectforge.timesheet.vacation.kost2:}")
    private var kost2Property: String? = null

    @Autowired
    private lateinit var configurationDao: ConfigurationDao

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
        val (taskIdString, taskIdName) = valueOf(ConfigurationParam.TIMESHEET_VACATION_TASK_ID, taskIdProperty) {
            it.longValue?.takeIf { id -> id > 0 }?.toString()
        }
        val (kost2String, kost2Name) = valueOf(ConfigurationParam.TIMESHEET_VACATION_KOST2, kost2Property) {
            it.stringValue
        }
        if (taskIdString == null && kost2String == null) {
            return null
        }
        val result = check(taskIdString, kost2String)
        return when (result.problem) {
            null -> {
                lastWarning = null
                result.booking
            }

            Problem.TASK_NOT_FOUND -> warn("$taskIdName='${taskIdString ?: ""}' names no task")
            Problem.KOST2_NOT_FOUND -> warn("$kost2Name='${kost2String ?: ""}' names no cost unit (format #.###.##.##)")
            Problem.KOST2_NOT_BOOKABLE -> warn(
                "$kost2Name='$kost2String' is not bookable on task $taskIdName=${result.task?.id} ('${result.task?.title}')"
            )
        }
    }

    /**
     * Checks the given task id and cost unit, e.g. on saving them on the configuration page. Task and cost unit are
     * looked up each on its own, so [CheckResult.task] and [CheckResult.kost2] are set if found, whatever the problem.
     */
    open fun check(taskId: String?, kost2: String?): CheckResult {
        val task = taskId?.trim()?.toLongOrNull()?.let { taskTree.getTaskById(it) }
        val kost2DO = kost2?.trim()?.ifEmpty { null }?.let { kostCache.getKost2(it) }
        val problem = when {
            task == null -> Problem.TASK_NOT_FOUND
            kost2DO == null -> Problem.KOST2_NOT_FOUND
            taskTree.getKost2List(task.id)?.any { it.id == kost2DO.id } != true -> Problem.KOST2_NOT_BOOKABLE
            else -> null
        }
        val booking = if (problem == null) VacationBooking(task!!, kost2DO!!) else null
        return CheckResult(booking = booking, problem = problem, task = task, kost2 = kost2DO)
    }

    /** @param problem The first problem found (task, cost unit, bookable), null if [booking] is given. */
    class CheckResult(
        val booking: VacationBooking?,
        val problem: Problem?,
        val task: TaskDO?,
        val kost2: Kost2DO?,
    )

    enum class Problem(val i18nKey: String) {
        TASK_NOT_FOUND("administration.configuration.error.vacationTaskNotFound"),
        KOST2_NOT_FOUND("administration.configuration.error.vacationKost2NotFound"),
        KOST2_NOT_BOOKABLE("administration.configuration.error.vacationKost2NotBookable"),
    }

    /**
     * The value of the parameter and its name for the warnings. The property only if the parameter's row doesn't
     * exist yet: it is taken over on creating the row (see [ConfigurationParam.getLegacyProperty]), afterwards the
     * parameter alone applies, also if cleared.
     */
    private fun valueOf(
        param: ConfigurationParam,
        propertyValue: String?,
        value: (ConfigurationDO) -> String?,
    ): Pair<String?, String> {
        val entry = configurationDao.getEntry(param)
            ?: return propertyValue?.trim()?.ifEmpty { null } to (param.legacyProperty ?: param.key)
        return value(entry)?.trim()?.ifEmpty { null } to param.key
    }

    private fun warn(message: String): VacationBooking? {
        if (message != lastWarning) {
            log.warn { "Vacation booking of time sheets not available: $message." }
            lastWarning = message
        }
        return null
    }
}
