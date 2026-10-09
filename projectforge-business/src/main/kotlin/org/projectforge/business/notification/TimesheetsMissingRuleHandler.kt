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
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.timesheet.UnbookedDaysCalculator
import org.projectforge.business.user.UserLocale
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import org.projectforge.framework.configuration.Configuration
import java.time.format.TextStyle

/**
 * Working days without time sheets in the referred month ([NotificationParams.referredMonth]), the same days as
 * "without time sheets" of the monthly report: only days up to the day of the run count (all days of a past month).
 * Resolved once these days are booked (or covered by an approved vacation).
 */
@Service
class TimesheetsMissingRuleHandler : NotificationRuleHandler {
    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var notificationLinks: NotificationLinks

    @Autowired
    private lateinit var unbookedDaysCalculator: UnbookedDaysCalculator

    override val ruleType = NotificationRuleType.TIMESHEETS_MISSING

    override val variables = listOf("month", "unbookedDays", "unbookedDaysCount", "timesheetLink", "monthlyReportLink")

    override fun evaluate(rule: NotificationRuleDO, today: LocalDate): NotificationRuleHandler.Evaluation {
        val month = rule.readParams().referredMonth.of(today)
        val employees = employeeService.selectAllActive(checkAccess = false, showRecentLeft = true)
            .filter { isEmployedIn(it, month) }
        val unbookedDays = unbookedDaysCalculator.unbookedDays(employees, month)
        val affected = mutableMapOf<Long, Map<String, String>>()
        employees.forEach { employee ->
            val user = caches.getUser(employee.user?.id) ?: return@forEach
            val days = cut(unbookedDays[employee.id].orEmpty(), month, today)
            if (days.isNotEmpty()) {
                affected[user.id!!] = variables(user, month, days)
            }
        }
        return NotificationRuleHandler.Evaluation(month.toString(), affected, MONTHLY_REPORT_PATH)
    }

    override fun variables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        val month = rule.readParams().referredMonth.of(today)
        val employee = caches.getEmployeeByUserId(user.id)
        val days = employee?.let { unbookedDaysCalculator.unbookedDays(listOf(it), month)[it.id] }
            ?: UnbookedDaysCalculator.unbookedDays(month, emptySet(), emptySet())
        return variables(user, month, cut(days, month, today))
    }

    override fun isRelevant(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Boolean {
        val employee = caches.getEmployeeByUserId(user.id) ?: return true // Not an employee, e.g. a single user.
        return isEmployedIn(employee, rule.readParams().referredMonth.of(today))
    }

    /** Resolved if no day up to the creation of the notification (within its month) is unbooked any more. */
    override fun resolve(
        rule: NotificationRuleDO,
        notifications: List<NotificationDO>,
        today: LocalDate,
    ): Map<Long, NotificationStatus> {
        val result = mutableMapOf<Long, NotificationStatus>()
        notifications.groupBy { it.periodKey }.forEach { (periodKey, list) ->
            val month = runCatching { YearMonth.parse(periodKey) }.getOrNull() ?: return@forEach
            val employees = list.mapNotNull { caches.getEmployeeByUserId(it.recipientId) }
            val unbookedDays = unbookedDaysCalculator.unbookedDays(employees, month)
            list.forEach { notification ->
                val employee = caches.getEmployeeByUserId(notification.recipientId) ?: return@forEach
                val created = notification.created?.toInstant()?.atZone(Configuration.instance.defaultTimeZone.toZoneId())?.toLocalDate() ?: today
                if (cut(unbookedDays[employee.id].orEmpty(), month, created).isEmpty()) {
                    result[notification.id!!] = NotificationStatus.RESOLVED
                }
            }
        }
        return result
    }

    private fun variables(user: PFUserDO, month: YearMonth, days: List<Int>): Map<String, String> {
        val locale = UserLocale.determineUserLocale(user)
        return mapOf(
            "month" to "${month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)} ${month.year}",
            "unbookedDays" to (UnbookedDaysCalculator.format(month, days) ?: "–"),
            "unbookedDaysCount" to days.size.toString(),
            "timesheetLink" to notificationLinks.absolute(TIMESHEET_PATH),
            "monthlyReportLink" to notificationLinks.absolute(MONTHLY_REPORT_PATH),
        )
    }

    companion object {
        const val TIMESHEET_PATH = "/timesheet"
        const val MONTHLY_REPORT_PATH = "/monthlyEmployeeReport"

        /** The days of the month up to [until] (all of them, if the month is over). */
        internal fun cut(days: List<Int>, month: YearMonth, until: LocalDate): List<Int> {
            return days.filter { !month.atDay(it).isAfter(until) }
        }

        internal fun isEmployedIn(employee: EmployeeDO, month: YearMonth): Boolean {
            return employee.eintrittsDatum?.isAfter(month.atEndOfMonth()) != true
                    && employee.austrittsDatum?.isBefore(month.atDay(1)) != true
        }
    }
}
