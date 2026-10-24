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

package org.projectforge.business.notification

import org.projectforge.business.PfCaches
import org.projectforge.business.task.TaskFormatter
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserLocale
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate

/**
 * Time sheets booked by an ad hoc booking grant an employee proposed, to be accepted by the project management
 * ([org.projectforge.business.timesheet.TimesheetDO.acceptancePending]). Concerned are the managers of the project of
 * each such time sheet (members of its project manager group, its contact persons), who may accept it: who may edit
 * the time sheets of others on its task ([TimesheetDao.hasForeignTimesheetWriteAccess]). A time sheet without such a
 * manager is found in the time sheet list by the filter "to accept" only.
 *
 * Meant to run daily: the period key is the day of the run, so a notification marked as done comes back with the next
 * run as long as such time sheets are left. An older pending notification of the same recipient expires once a newer
 * one exists; all of them are resolved, as soon as no such time sheet is left for the recipient.
 */
@Service
class TimesheetsToAcceptRuleHandler : NotificationRuleHandler {
    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var notificationLinks: NotificationLinks

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    override val ruleType = NotificationRuleType.TIMESHEETS_TO_ACCEPT

    override val variables = listOf(
        "timesheetsToAcceptCount",
        "timesheetsToAcceptHours",
        "timesheetsToAcceptTasks",
        "timesheetsToAcceptLink",
    )

    override fun evaluate(rule: NotificationRuleDO, today: LocalDate): NotificationRuleHandler.Evaluation? {
        val affected = mutableMapOf<Long, Map<String, String>>()
        timesheetsToAccept().forEach { (userId, sheets) ->
            val user = caches.getUser(userId) ?: return@forEach
            affected[userId] = variables(user, sheets)
        }
        return NotificationRuleHandler.Evaluation(today.toString(), affected, LIST_PATH)
    }

    override fun variables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        return variables(user, timesheetsToAccept()[user.id].orEmpty())
    }

    /**
     * Resolved if the recipient has no time sheet to accept any more; expired if a newer notification of the
     * recipient is pending (the one of a later run).
     */
    override fun resolve(
        rule: NotificationRuleDO,
        notifications: List<NotificationDO>,
        today: LocalDate,
    ): Map<Long, NotificationStatus> {
        val result = mutableMapOf<Long, NotificationStatus>()
        val sheets = timesheetsToAccept()
        notifications.groupBy { it.recipientId }.forEach { (recipientId, list) ->
            val newest = list.maxWith(compareBy<NotificationDO> { it.periodKey ?: "" }.thenBy { it.id ?: 0 })
            list.forEach { notification ->
                if (sheets[recipientId].isNullOrEmpty()) {
                    result[notification.id!!] = NotificationStatus.RESOLVED
                } else if (notification !== newest) {
                    result[notification.id!!] = NotificationStatus.EXPIRED
                }
            }
        }
        return result
    }

    /**
     * The time sheets to accept by the id of the managers, who may accept them.
     */
    internal fun timesheetsToAccept(): Map<Long, List<TimesheetDao.TimesheetToAccept>> {
        val sheets = timesheetDao.getTimesheetsToAccept()
        if (sheets.isEmpty()) {
            return emptyMap()
        }
        val result = mutableMapOf<Long, MutableList<TimesheetDao.TimesheetToAccept>>()
        val groupMembers by lazy { membersByGroup() }
        sheets.groupBy { it.taskId }.forEach { (taskId, taskSheets) ->
            val projekt = taskTree.getProjekt(taskId) ?: return@forEach
            val candidates = mutableSetOf<Long>()
            projekt.projektManagerGroupId?.let { candidates += groupMembers[it].orEmpty() }
            projekt.contactPerson?.id?.let { candidates += it }
            candidates += projekt.additionalContactUserIdList
            candidates.forEach { userId ->
                val user = caches.getUser(userId) ?: return@forEach
                if (user.hasSystemAccess() && timesheetDao.hasForeignTimesheetWriteAccess(user, taskId)) {
                    result.getOrPut(userId) { mutableListOf() }.addAll(taskSheets)
                }
            }
        }
        return result
    }

    private fun membersByGroup(): Map<Long, Set<Long>> {
        val result = mutableMapOf<Long, MutableSet<Long>>()
        userGroupCache.getUserGroupIdMap().forEach { (userId, groupIds) ->
            groupIds.forEach { result.getOrPut(it) { mutableSetOf() }.add(userId) }
        }
        return result
    }

    private fun variables(user: PFUserDO, sheets: List<TimesheetDao.TimesheetToAccept>): Map<String, String> {
        val locale = UserLocale.determineUserLocale(user)
        val hours = BigDecimal(sheets.sumOf { it.durationMillis }).divide(BigDecimal(3_600_000), 2, RoundingMode.HALF_UP)
        val numberFormat = NumberFormat.getNumberInstance(locale).also {
            it.minimumFractionDigits = 0
            it.maximumFractionDigits = 2
        }
        val taskIds = sheets.map { it.taskId }.distinct()
        val tasks = taskIds.take(MAX_TASKS).mapNotNull { TaskFormatter.getTaskPath(it) }.toMutableList()
        if (taskIds.size > MAX_TASKS) {
            tasks += "…"
        }
        return mapOf(
            "timesheetsToAcceptCount" to sheets.size.toString(),
            "timesheetsToAcceptHours" to numberFormat.format(hours),
            "timesheetsToAcceptTasks" to tasks.joinToString("; ").ifEmpty { "–" },
            "timesheetsToAcceptLink" to notificationLinks.absolute(LIST_PATH),
        )
    }

    companion object {
        /** The time sheet list with the filter "to accept" (see the time sheet list of projectforge-next). */
        const val LIST_PATH = "/timesheet?acceptancePending=true"

        /** The tasks listed at most in [variables]. */
        private const val MAX_TASKS = 10
    }
}
