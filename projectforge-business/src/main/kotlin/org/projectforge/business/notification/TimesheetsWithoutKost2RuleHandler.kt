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
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.user.UserLocale
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

/**
 * Time sheets without cost 2 unit within the last months ([NotificationParams.kost2MissingMonths], the current month
 * included), only if these are to be reported at all ([Configuration.isWarnMissingKost2]). Booked on a task without
 * cost 2 units (e.g. not yet assigned to its project), their hours are missing in the salary export and end up as
 * correction there.
 *
 * Meant to run daily: the period key is the day of the run, so a notification marked as done comes back with the next
 * run as long as such time sheets are left. An older pending notification of the same recipient expires once a newer
 * one exists; all of them are resolved, as soon as no such time sheet is left in the period.
 */
@Service
class TimesheetsWithoutKost2RuleHandler : NotificationRuleHandler {
    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var notificationLinks: NotificationLinks

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    override val ruleType = NotificationRuleType.TIMESHEETS_WITHOUT_KOST2

    override val variables = listOf(
        "period",
        "timesheetsWithoutKost2Count",
        "timesheetsWithoutKost2Hours",
        "timesheetsWithoutKost2Tasks",
        "timesheetLink",
    )

    override fun evaluate(rule: NotificationRuleDO, today: LocalDate): NotificationRuleHandler.Evaluation? {
        if (!Configuration.instance.isWarnMissingKost2) {
            return null
        }
        val months = rule.readParams().kost2MissingMonths
        val affected = mutableMapOf<Long, Map<String, String>>()
        timesheetsWithoutKost2(months, today).forEach { (userId, sheets) ->
            val user = caches.getUser(userId) ?: return@forEach
            affected[userId] = variables(user, months, today, sheets)
        }
        return NotificationRuleHandler.Evaluation(today.toString(), affected, TimesheetsMissingRuleHandler.TIMESHEET_PATH)
    }

    override fun variables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        val months = rule.readParams().kost2MissingMonths
        return variables(user, months, today, timesheetsWithoutKost2(months, today)[user.id].orEmpty())
    }

    /**
     * Resolved if the recipient has no time sheet without cost 2 unit in the period any more (or they aren't reported
     * any more); expired if a newer notification of the recipient is pending (the one of a later run).
     */
    override fun resolve(
        rule: NotificationRuleDO,
        notifications: List<NotificationDO>,
        today: LocalDate,
    ): Map<Long, NotificationStatus> {
        val result = mutableMapOf<Long, NotificationStatus>()
        val sheets = if (Configuration.instance.isWarnMissingKost2) {
            timesheetsWithoutKost2(rule.readParams().kost2MissingMonths, today)
        } else emptyMap()
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

    private fun timesheetsWithoutKost2(months: Int, today: LocalDate): Map<Long, List<TimesheetDao.TimesheetWithoutKost2>> {
        val zoneId = Configuration.instance.defaultTimeZone.toZoneId()
        val from = Date.from(firstDay(months, today).atStartOfDay(zoneId).toInstant())
        val to = Date.from(today.plusDays(1).atStartOfDay(zoneId).toInstant())
        return timesheetDao.getTimesheetsWithoutKost2(from, to)
    }

    override fun exampleVariables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        val tasks = listOf("ACME -> Website -> Development", "ACME -> Support")
        return variables(user, rule.readParams().kost2MissingMonths, today, 3, 16_200_000L, tasks)
    }

    private fun variables(
        user: PFUserDO,
        months: Int,
        today: LocalDate,
        sheets: List<TimesheetDao.TimesheetWithoutKost2>,
    ): Map<String, String> {
        val taskIds = sheets.mapNotNull { it.taskId }.distinct()
        val tasks = taskIds.take(MAX_TASKS).mapNotNull { TaskFormatter.getTaskPath(it) }.toMutableList()
        if (taskIds.size > MAX_TASKS) {
            tasks += "…"
        }
        return variables(user, months, today, sheets.size, sheets.sumOf { it.durationMillis }, tasks)
    }

    /** The variables of [count] time sheets of [durationMillis] in total, booked on the [tasks] (paths). */
    private fun variables(
        user: PFUserDO,
        months: Int,
        today: LocalDate,
        count: Int,
        durationMillis: Long,
        tasks: List<String>,
    ): Map<String, String> {
        val locale = UserLocale.determineUserLocale(user)
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        val hours = BigDecimal(durationMillis).divide(BigDecimal(3_600_000), 2, RoundingMode.HALF_UP)
        val numberFormat = NumberFormat.getNumberInstance(locale).also {
            it.minimumFractionDigits = 0
            it.maximumFractionDigits = 2
        }
        return mapOf(
            "period" to "${firstDay(months, today).format(dateFormatter)} – ${today.format(dateFormatter)}",
            "timesheetsWithoutKost2Count" to count.toString(),
            "timesheetsWithoutKost2Hours" to numberFormat.format(hours),
            "timesheetsWithoutKost2Tasks" to tasks.joinToString("; ").ifEmpty { "–" },
            "timesheetLink" to notificationLinks.absolute(TimesheetsMissingRuleHandler.TIMESHEET_PATH),
        )
    }

    companion object {
        /** The tasks listed at most in [variables]. */
        private const val MAX_TASKS = 10

        /** The first day of the period: the 1st of the earliest of the [months], the current month included. */
        internal fun firstDay(months: Int, today: LocalDate): LocalDate =
            YearMonth.from(today).minusMonths(months.coerceAtLeast(1) - 1L).atDay(1)
    }
}
