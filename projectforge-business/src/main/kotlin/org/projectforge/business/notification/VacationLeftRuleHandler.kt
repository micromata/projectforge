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
import org.projectforge.business.user.UserLocale
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Vacation days left which expire soon ([NotificationParams.vacationExpiry]): the days carried over from the
 * previous year at the end of the vacation year, or all days left at the end of the year. Notifies within
 * [NotificationParams.daysBeforeExpiry] days before the expiry. Resolved once no days are left, expired afterwards.
 */
@Service
class VacationLeftRuleHandler : NotificationRuleHandler {
    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var notificationLinks: NotificationLinks

    @Autowired
    private lateinit var vacationService: VacationService

    override val ruleType = NotificationRuleType.VACATION_LEFT

    override val variables = listOf("daysLeft", "expiryDate", "vacationLink")

    override fun evaluate(rule: NotificationRuleDO, today: LocalDate): NotificationRuleHandler.Evaluation? {
        val params = rule.readParams()
        val expiry = expiryDate(params, today)
        if (today.isAfter(expiry) || today.isBefore(expiry.minusDays(params.daysBeforeExpiry.coerceAtLeast(0).toLong()))) {
            return null
        }
        val affected = mutableMapOf<Long, Map<String, String>>()
        employeeService.selectAllActive(checkAccess = false).forEach { employee ->
            val user = caches.getUser(employee.user?.id) ?: return@forEach
            val days = daysLeft(employee, params, today)
            if (days > BigDecimal.ZERO) {
                affected[user.id!!] = variables(user, days, expiry)
            }
        }
        return NotificationRuleHandler.Evaluation(periodKey(params, expiry), affected, VACATION_PATH)
    }

    override fun variables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        val params = rule.readParams()
        val days = caches.getEmployeeByUserId(user.id)?.let { daysLeft(it, params, today) } ?: BigDecimal.ZERO
        return variables(user, days, expiryDate(params, today))
    }

    override fun resolve(
        rule: NotificationRuleDO,
        notifications: List<NotificationDO>,
        today: LocalDate,
    ): Map<Long, NotificationStatus> {
        val params = rule.readParams()
        val expiry = expiryDate(params, today)
        val result = mutableMapOf<Long, NotificationStatus>()
        notifications.forEach { notification ->
            if (notification.periodKey != periodKey(params, expiry) || today.isAfter(expiry)) {
                // Another period (or settings of the rule changed): the expiry date is passed or not relevant anymore.
                result[notification.id!!] = NotificationStatus.EXPIRED
                return@forEach
            }
            val employee = caches.getEmployeeByUserId(notification.recipientId) ?: return@forEach
            if (daysLeft(employee, params, today) <= BigDecimal.ZERO) {
                result[notification.id!!] = NotificationStatus.RESOLVED
            }
        }
        return result
    }

    private fun variables(user: PFUserDO, days: BigDecimal, expiry: LocalDate): Map<String, String> {
        val locale = UserLocale.determineUserLocale(user)
        return mapOf(
            "daysLeft" to days.stripTrailingZeros().toPlainString(),
            "expiryDate" to expiry.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
            "vacationLink" to notificationLinks.absolute(VACATION_PATH),
        )
    }

    /** The next expiry date from [today] on. */
    private fun expiryDate(params: NotificationParams, today: LocalDate): LocalDate {
        return when (params.vacationExpiry) {
            NotificationParams.VacationExpiry.CARRY_OVER -> vacationService.getEndOfCarryVacationOfPreviousYear(today.year)
            NotificationParams.VacationExpiry.YEAR_END -> LocalDate.of(today.year, 12, 31)
        }
    }

    private fun daysLeft(employee: EmployeeDO, params: NotificationParams, today: LocalDate): BigDecimal {
        val stats = vacationService.getVacationStats(employee, today.year, baseDate = today)
        val days = when (params.vacationExpiry) {
            NotificationParams.VacationExpiry.CARRY_OVER -> stats.remainingLeaveFromPreviousYearUnused
            NotificationParams.VacationExpiry.YEAR_END -> stats.vacationDaysLeftInYear
        }
        return days ?: BigDecimal.ZERO
    }

    private fun periodKey(params: NotificationParams, expiry: LocalDate) = "${params.vacationExpiry}-$expiry"

    companion object {
        const val VACATION_PATH = "/vacation"
    }
}
