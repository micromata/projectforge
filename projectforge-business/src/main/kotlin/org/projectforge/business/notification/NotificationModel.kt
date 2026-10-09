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

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.business.fibu.EmployeeStatus
import org.projectforge.framework.calendar.Holidays
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date

/** The rule types, each evaluated by its [NotificationRuleHandler]. */
enum class NotificationRuleType {
    /** Working days without time sheets in a month (e.g. reminders at the month end). */
    TIMESHEETS_MISSING,

    /** Vacation days left which will expire soon (carry-over from the previous year or the end of the year). */
    VACATION_LEFT,

    /** No condition: all recipients get the notification when it is triggered (by hand or by the schedule). */
    MANUAL,
}

enum class NotificationSeverity {
    INFO,
    IMPORTANT,
    URGENT,
}

/** How a notification is shown in the app. */
enum class NotificationDisplay {
    /** Shown once as a toast, acknowledged by being shown. */
    TOAST,

    /** A toast staying until confirmed. */
    TOAST_CONFIRM,

    /** A banner staying until done: resolved by the rule or marked as done by the user (if allowed). */
    BANNER,
}

enum class NotificationStatus {
    OPEN,

    /** Confirmed (read) by the recipient: no escalation steps any more. A banner stays until done. */
    ACKNOWLEDGED,

    /** Marked as done by the recipient. */
    DONE,

    /** The condition of the rule isn't fulfilled any more (e.g. the time sheets are booked). */
    RESOLVED,

    /** Out of date, e.g. the vacation days are lost or the rule was deactivated. */
    EXPIRED;

    /** Open or acknowledged: not finished. */
    val isPending: Boolean
        get() = this == OPEN || this == ACKNOWLEDGED

    companion object {
        val PENDING = listOf(OPEN, ACKNOWLEDGED)
    }
}

enum class NotificationChannel {
    /** Visible in the app (toast, banner, menu badge) from the delivery of this step on. */
    IN_APP,
    MAIL,

    /** Not yet supported (phase 2): such steps are skipped. */
    SMS,
}

/** When the rule is evaluated: once per day at most, on the days given by the [mode]. */
class NotificationSchedule(
    var mode: Mode = Mode.NONE,
    /**
     * [Mode.WORKING_DAYS_BEFORE_MONTH_END]: the number of working days before the end of the month (0 is the last
     * working day); [Mode.DAY_OF_MONTH]: the day of the month.
     */
    var day: Int = 0,
    /** [Mode.WEEKLY] only. */
    var dayOfWeek: DayOfWeek? = null,
) {
    enum class Mode {
        /** [day] working days before the end of the month: 0 is the last working day. */
        WORKING_DAYS_BEFORE_MONTH_END,

        /**
         * The [day]-th day of the month (the last one, if the month is shorter). A day off is moved to the next
         * working day, the last days off of a month to its last working day.
         */
        DAY_OF_MONTH,

        /** Every working day. */
        DAILY,

        /** Every week on [dayOfWeek] (moved to the next working day of the same week, if it's a day off). */
        WEEKLY,

        /** Never by schedule, only by hand. */
        NONE,
    }

    /** Is the rule due on the given day? */
    fun isDue(date: LocalDate): Boolean {
        return when (mode) {
            Mode.WORKING_DAYS_BEFORE_MONTH_END, Mode.DAY_OF_MONTH -> sendDay(YearMonth.from(date)) == date
            Mode.DAILY -> isWorkingDay(date)
            Mode.WEEKLY -> {
                // The given day of the week of the date, or the next working day up to the end of the week (none, if
                // there is no working day left in the week):
                val target = date.with(dayOfWeek ?: DayOfWeek.MONDAY)
                val candidates = (0..(7 - target.dayOfWeek.value)).map { target.plusDays(it.toLong()) }
                candidates.firstOrNull { isWorkingDay(it) } == date
            }

            Mode.NONE -> false
        }
    }

    /**
     * The send day in the given month ([Mode.WORKING_DAYS_BEFORE_MONTH_END] and [Mode.DAY_OF_MONTH] only, null
     * otherwise or if the month has no working day at all).
     */
    fun sendDay(month: YearMonth): LocalDate? {
        val workingDays = (1..month.lengthOfMonth()).map { month.atDay(it) }.filter { isWorkingDay(it) }
        if (workingDays.isEmpty()) {
            return null
        }
        return when (mode) {
            Mode.WORKING_DAYS_BEFORE_MONTH_END -> workingDays.getOrNull(workingDays.size - 1 - day.coerceAtLeast(0))
                ?: workingDays.first()

            Mode.DAY_OF_MONTH -> {
                val date = month.atDay(day.coerceIn(1, month.lengthOfMonth()))
                workingDays.firstOrNull { !it.isBefore(date) } ?: workingDays.last()
            }

            else -> null
        }
    }

    /** The i18n keys (field to key) of the errors. */
    fun validate(): List<Pair<String, String>> {
        val errors = mutableListOf<Pair<String, String>>()
        when (mode) {
            Mode.WORKING_DAYS_BEFORE_MONTH_END -> if (day !in 0..MAX_WORKING_DAYS_BEFORE_MONTH_END) {
                errors += "schedule.day" to "notification.rule.error.day"
            }

            Mode.DAY_OF_MONTH -> if (day !in 1..31) {
                errors += "schedule.day" to "notification.rule.error.day"
            }

            Mode.WEEKLY -> if (dayOfWeek == null) {
                errors += "schedule.dayOfWeek" to "notification.rule.error.dayOfWeek"
            }

            else -> {}
        }
        return errors
    }

    private fun isWorkingDay(date: LocalDate) = Holidays.instance.isWorkingDay(date)

    companion object {
        const val MAX_WORKING_DAYS_BEFORE_MONTH_END = 15
    }
}

/**
 * The type specific settings of a rule, all optional (stored as JSON, so new settings need no new column).
 */
class NotificationParams(
    /** [NotificationRuleType.TIMESHEETS_MISSING]: the month of the rule run or the one before. */
    var referredMonth: ReferredMonth = ReferredMonth.CURRENT,
    /** [NotificationRuleType.VACATION_LEFT]: which vacation days are about to expire. */
    var vacationExpiry: VacationExpiry = VacationExpiry.CARRY_OVER,
    /** [NotificationRuleType.VACATION_LEFT]: notify within these days before the expiry. */
    var daysBeforeExpiry: Int = 30,
) {
    enum class ReferredMonth(val offset: Long) {
        CURRENT(0),
        PREVIOUS(-1);

        fun of(date: LocalDate): YearMonth = YearMonth.from(date).plusMonths(offset)
    }

    enum class VacationExpiry {
        /** The days carried over from the previous year, lost after the end of the vacation year. */
        CARRY_OVER,

        /** All days left at the end of the year. */
        YEAR_END,
    }
}

/**
 * The recipients of a rule: the users of the groups, the single users and the employees of the given status (or all
 * employees), minus the employees of the excluded status, users with system access only.
 */
class NotificationRecipients(
    var groupIds: List<Long> = emptyList(),
    var userIds: List<Long> = emptyList(),
    /** All active employees. */
    var allEmployees: Boolean = false,
    /** The active employees of these status. */
    var employeeStatus: List<EmployeeStatus> = emptyList(),
    var excludedEmployeeStatus: List<EmployeeStatus> = emptyList(),
    /**
     * Only the recipients concerned by the rule (e.g. with missing time sheets). If no recipient is given at all,
     * all users concerned.
     */
    var onlyAffected: Boolean = true,
) {
    @get:JsonIgnore
    val isEmpty: Boolean
        get() = groupIds.isEmpty() && userIds.isEmpty() && !allEmployees && employeeStatus.isEmpty()
}

/** One step of the delivery cascade: [channel] after [delayMinutes] since the notification was created. */
class NotificationDeliveryStep(
    var channel: NotificationChannel = NotificationChannel.IN_APP,
    var delayMinutes: Int = 0,
    /** Skipped, if the notification was acknowledged, done or resolved in the meantime. */
    var onlyIfUnacknowledged: Boolean = false,
)

/** The delivery cascade of a rule, e.g. in the app at once and a mail after two days, if not yet confirmed. */
class NotificationDelivery(
    var steps: List<NotificationDeliveryStep> = listOf(NotificationDeliveryStep()),
)

/** The delivery state of the steps of a notification (one entry per step of its [NotificationDelivery]). */
class NotificationDeliveryState(
    var steps: MutableList<StepState> = mutableListOf(),
) {
    class StepState(
        var step: Int = 0,
        var channel: NotificationChannel = NotificationChannel.IN_APP,
        var dueAt: Date? = null,
        var sentAt: Date? = null,
        var skipped: Boolean = false,
        var error: String? = null,
    ) {
        @get:JsonIgnore
        val isProcessed: Boolean
            get() = sentAt != null || skipped || error != null
    }

    /** The due time of the next unprocessed step, null if all steps are processed. */
    @JsonIgnore
    fun nextDueAt(): Date? = steps.filter { !it.isProcessed }.mapNotNull { it.dueAt }.minOrNull()

    companion object {
        fun of(delivery: NotificationDelivery, created: Date): NotificationDeliveryState {
            return NotificationDeliveryState(delivery.steps.mapIndexed { index, step ->
                StepState(
                    step = index,
                    channel = step.channel,
                    dueAt = Date(created.time + step.delayMinutes.coerceAtLeast(0) * 60_000L),
                )
            }.toMutableList())
        }
    }
}
