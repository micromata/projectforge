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


package org.projectforge.rest.notification

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.business.fibu.EmployeeStatus
import org.projectforge.business.notification.NotificationDelivery
import org.projectforge.business.notification.NotificationDeliveryStep
import org.projectforge.business.notification.NotificationDisplay
import org.projectforge.business.notification.NotificationParams
import org.projectforge.business.notification.NotificationRecipients
import org.projectforge.business.notification.NotificationRuleDO
import org.projectforge.business.notification.NotificationRuleType
import org.projectforge.business.notification.NotificationSchedule
import org.projectforge.business.notification.NotificationSeverity
import org.projectforge.framework.json.JsonUtils
import org.projectforge.rest.dto.BaseDTODisplayObject
import org.projectforge.rest.dto.EntityAccessSupport
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.User
import java.time.DayOfWeek
import java.util.Date

/**
 * A rule of the notification system for the hand-built next page: the JSON columns of [NotificationRuleDO]
 * (schedule, params and recipients flat, the delivery steps as a list), the group and user ids as references.
 */
class NotificationRule(
    id: Long? = null,
    displayName: String? = null,
    var name: String? = null,
    var description: String? = null,
    var active: Boolean = false,
    var ruleType: NotificationRuleType? = null,
    var scheduleMode: NotificationSchedule.Mode = NotificationSchedule.Mode.NONE,
    var scheduleDay: Int = 0,
    var scheduleDayOfWeek: DayOfWeek? = null,
    var referredMonth: NotificationParams.ReferredMonth = NotificationParams.ReferredMonth.CURRENT,
    var vacationExpiry: NotificationParams.VacationExpiry = NotificationParams.VacationExpiry.CARRY_OVER,
    var daysBeforeExpiry: Int = 30,
    var recipientGroups: List<Group> = emptyList(),
    var recipientUsers: List<User> = emptyList(),
    var allEmployees: Boolean = false,
    var employeeStatus: List<EmployeeStatus> = emptyList(),
    var excludedEmployeeStatus: List<EmployeeStatus> = emptyList(),
    var onlyAffected: Boolean = true,
    var deliverySteps: List<NotificationDeliveryStep> = listOf(NotificationDeliveryStep()),
    var severity: NotificationSeverity = NotificationSeverity.INFO,
    var display: NotificationDisplay = NotificationDisplay.BANNER,
    var manualDone: Boolean = false,
    var menuBadge: String? = null,
    var subject: String? = null,
    var text: String? = null,
    var editableByGroups: List<Group> = emptyList(),
    /** Read-only: the last run by schedule. */
    var lastRun: Date? = null,
) : BaseDTODisplayObject<NotificationRuleDO>(id = id, displayName = displayName), EntityAccessSupport {
    /** The schedule as the rule stores it (the form edits its fields flat). */
    @get:JsonIgnore
    val schedule: NotificationSchedule
        get() = NotificationSchedule(scheduleMode, scheduleDay, scheduleDayOfWeek)

    override var writeAccess: Boolean? = null
    override var deleteAccess: Boolean? = null

    override fun copyFrom(src: NotificationRuleDO) {
        super.copyFrom(src)
        val schedule = src.readSchedule()
        scheduleMode = schedule.mode
        scheduleDay = schedule.day
        scheduleDayOfWeek = schedule.dayOfWeek
        val params = src.readParams()
        referredMonth = params.referredMonth
        vacationExpiry = params.vacationExpiry
        daysBeforeExpiry = params.daysBeforeExpiry
        val recipients = src.readRecipients()
        recipientGroups = recipients.groupIds.map { Group(it) }.also { Group.restoreDisplayNames(it) }
        recipientUsers = recipients.userIds.map { User(it) }.also { User.restoreDisplayNames(it) }
        allEmployees = recipients.allEmployees
        employeeStatus = recipients.employeeStatus
        excludedEmployeeStatus = recipients.excludedEmployeeStatus
        onlyAffected = recipients.onlyAffected
        deliverySteps = src.readDelivery().steps
        editableByGroups = src.readEditableByGroupIds().map { Group(it) }.also { Group.restoreDisplayNames(it) }
    }

    override fun copyTo(dest: NotificationRuleDO) {
        super.copyTo(dest)
        dest.schedule = JsonUtils.toJson(schedule)
        dest.params = JsonUtils.toJson(NotificationParams(referredMonth, vacationExpiry, daysBeforeExpiry))
        dest.recipients = JsonUtils.toJson(
            NotificationRecipients(
                groupIds = recipientGroups.mapNotNull { it.id }.distinct(),
                userIds = recipientUsers.mapNotNull { it.id }.distinct(),
                allEmployees = allEmployees,
                employeeStatus = employeeStatus,
                excludedEmployeeStatus = excludedEmployeeStatus,
                onlyAffected = onlyAffected,
            )
        )
        dest.delivery = JsonUtils.toJson(NotificationDelivery(deliverySteps))
        dest.editableByGroupIds = editableByGroups.mapNotNull { it.id }.distinct().takeIf { it.isNotEmpty() }
            ?.joinToString(",")
        dest.menuBadge = menuBadge?.takeIf { it.isNotBlank() }
    }
}
