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

import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DatePrecision
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Books one time sheet per working day of a period, e.g. a vacation, or the absence of a colleague booked by the
 * office (parental leave, illness).
 *
 * Weekends, holidays and days that already have a time sheet of the user are skipped; a half working day (24.12.,
 * 31.12.) and a half vacation day at either end are booked pro rata. All checks (access, protected periods, cost
 * unit, bookable task) stay with [TimesheetDao]: this service only computes the days.
 *
 * Days are days in the time zone of the logged-in user, as the calendar shows them.
 */
@Service
open class TimesheetDayBookingService {
    enum class Status {
        /** Booked with the full hours. */
        BOOK,

        /** Booked pro rata: a half working day or a half vacation day. */
        PARTIAL,
        WEEKEND,
        HOLIDAY,

        /** The user has a time sheet on this day already. */
        BOOKED,
    }

    class Request(
        var userId: Long? = null,
        var taskId: Long? = null,
        var kost2Id: Long? = null,
        var location: String? = null,
        var description: String? = null,
        var startDate: LocalDate? = null,
        var endDate: LocalDate? = null,
        var startTime: LocalTime? = null,
        /** The hours of a full day; rounded to 5 minutes. */
        var hoursPerDay: BigDecimal? = null,
        /** Book the first day with half the hours (a vacation beginning with a half day). */
        var halfDayBegin: Boolean = false,
        /** Book the last day with half the hours (a vacation ending with a half day). */
        var halfDayEnd: Boolean = false,
    )

    /**
     * @param minutes The minutes booked on this day, 0 for a skipped one.
     * @param note The title of the holiday, if the day is one.
     */
    class Day(val date: LocalDate, val status: Status, val minutes: Int, val note: String? = null) {
        val booking: Boolean
            get() = status == Status.BOOK || status == Status.PARTIAL
    }

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var timesheetRecentService: TimesheetRecentService

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var employeeService: EmployeeService

    /**
     * The days of the request's period and what would happen on each, without booking anything. If a task is given,
     * the booking of the first day is checked as well (access, protected period, bookable task), so the preview
     * fails the way the booking would.
     *
     * @throws UserException if the request is invalid.
     */
    open fun planDays(request: Request): List<Day> {
        val days = plan(request)
        if (request.taskId != null) {
            createTimesheets(request, days).firstOrNull()?.let {
                timesheetDao.hasInsertAccess(ThreadLocalUserContext.requiredLoggedInUser, it, throwException = true)
            }
        }
        return days
    }

    /**
     * Books the days of the request in one transaction: if any day fails, none is booked.
     *
     * @return The days as booked.
     * @throws UserException if the request is invalid or a time sheet can't be booked.
     */
    open fun book(request: Request): List<Day> {
        if (request.taskId == null) {
            throw UserException("timesheet.bookDays.error.taskRequired")
        }
        val days = plan(request)
        val timesheets = createTimesheets(request, days)
        if (timesheets.isEmpty()) {
            throw UserException("timesheet.bookDays.error.nothingToBook")
        }
        timesheetDao.insert(timesheets, checkAccess = true)
        timesheetRecentService.addRecentTimesheet(timesheets.first())
        return days
    }

    /**
     * The first working day from [from] on without a time sheet of the user, looking a month ahead; [from] itself if
     * there is none.
     */
    open fun firstBookableDay(userId: Long?, from: LocalDate): LocalDate {
        val until = from.plusDays(31)
        val booked = bookedDays(checkUser(userId), from, until)
        return generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(until) }
            .firstOrNull { Holidays.instance.isWorkingDay(it) && it !in booked } ?: from
    }

    /** A fifth of the user's weekly working hours, 8 if the user has no employee or no hours. */
    open fun defaultHoursPerDay(userId: Long?): BigDecimal {
        val employee = employeeCache.getEmployeeByUserId(checkUser(userId))
        val weekly = employeeService.getWeeklyWorkingHours(employee, LocalDate.now(), checkAccess = false)
        if (weekly == null || weekly.signum() <= 0) {
            return DEFAULT_HOURS_PER_DAY
        }
        return weekly.divide(BigDecimal(5), 2, RoundingMode.HALF_UP).stripTrailingZeros()
    }

    private fun plan(request: Request): List<Day> {
        val userId = checkUser(request.userId)
        val from = request.startDate ?: throw UserException("timesheet.bookDays.error.period")
        val until = request.endDate ?: throw UserException("timesheet.bookDays.error.period")
        if (until.isBefore(from)) {
            throw UserException("timesheet.bookDays.error.period")
        }
        if (until.isAfter(lastDayOfPeriod(from))) {
            throw UserException("timesheet.bookDays.error.tooManyDays")
        }
        val minutes = minutesOf(request.hoursPerDay)
        if (minutes < 5 || minutes * 60_000L > TimesheetDao.MAXIMUM_DURATION) {
            throw UserException("timesheet.bookDays.error.hours", TimesheetDao.MAXIMUM_DURATION / 3_600_000)
        }
        val startTime = request.startTime ?: throw UserException("timesheet.bookDays.error.startTime")
        if (startTime.toSecondOfDay() / 60 + minutes >= 24 * 60) {
            throw UserException("timesheet.bookDays.error.nextDay")
        }
        val booked = bookedDays(userId, from, until)
        val holidays = Holidays.instance
        return generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(until) }.map { date ->
            val day = PFDay.from(date)
            when {
                day.isWeekend() -> Day(date, Status.WEEKEND, 0)
                !holidays.isWorkingDay(date) -> Day(date, Status.HOLIDAY, 0, holidayTitle(day))
                date in booked -> Day(date, Status.BOOKED, 0)
                else -> {
                    var fraction = holidays.getWorkFraction(day) ?: BigDecimal.ONE
                    if ((request.halfDayBegin && date == from) || (request.halfDayEnd && date == until)) {
                        fraction = fraction.min(HALF)
                    }
                    if (fraction >= BigDecimal.ONE) {
                        Day(date, Status.BOOK, minutes)
                    } else {
                        val partial = roundTo5Minutes(BigDecimal(minutes).multiply(fraction))
                        val title = holidayTitle(day).ifBlank { null }
                        if (partial <= 0) Day(date, Status.HOLIDAY, 0, title) else Day(date, Status.PARTIAL, partial, title)
                    }
                }
            }
        }.toList()
    }

    private fun createTimesheets(request: Request, days: List<Day>): List<TimesheetDO> {
        val user = userGroupCache.getUser(request.userId ?: ThreadLocalUserContext.loggedInUserId)
        val task = taskTree.getTaskById(request.taskId)
            ?: throw UserException("timesheet.bookDays.error.taskRequired")
        val kost2 = request.kost2Id?.let { kost2Dao.findOrLoad(it) }
        val zoneId = ThreadLocalUserContext.zoneId
        return days.filter { it.booking }.map { day ->
            val start = PFDateTime.from(LocalDateTime.of(day.date, request.startTime), zoneId)
                .withPrecision(DatePrecision.MINUTE_5)
            TimesheetDO().also {
                it.user = user
                it.task = task
                it.kost2 = kost2
                it.location = request.location?.trim()?.ifEmpty { null }
                it.description = request.description?.trim()?.ifEmpty { null }
                it.startTime = start.utilDate
                it.stopTime = start.plus(day.minutes.toLong(), ChronoUnit.MINUTES).utilDate
            }
        }
    }

    /** The days in the given period with a time sheet of the user: a time sheet over midnight counts for both. */
    private fun bookedDays(userId: Long, from: LocalDate, until: LocalDate): Set<LocalDate> {
        val zoneId = ThreadLocalUserContext.zoneId
        val filter = TimesheetFilter()
        filter.userId = userId
        filter.startTime = PFDateTime.from(from, zoneId).utilDate
        filter.stopTime = PFDateTime.from(until, zoneId).utilDate // Extended to the end of day by internalGetList.
        return timesheetDao.internalGetList(filter, checkAccess = false).flatMap { sheet ->
            listOfNotNull(sheet.startTime, sheet.stopTime).map { PFDateTime.from(it).withZoneSameInstant(zoneId).localDate }
        }.toSet()
    }

    /**
     * The user to book for, the logged-in user if none is given. Another user's days may only be planned by someone
     * who may see the time sheets of others (as the calendar does); the booking itself is checked by [TimesheetDao].
     */
    private fun checkUser(userId: Long?): Long {
        val loggedInUserId = ThreadLocalUserContext.requiredLoggedInUserId
        if (userId == null || userId == loggedInUserId) {
            return loggedInUserId
        }
        if (!timesheetDao.showTimesheetsOfOtherUsers() || userGroupCache.getUser(userId) == null) {
            throw UserException("timesheet.bookDays.error.otherUser")
        }
        return userId
    }

    private fun holidayTitle(day: PFDay): String {
        val info = Holidays.instance.getHolidayInfo(day)
        return if (info.startsWith("calendar.holiday.")) translate(info) else info
    }

    companion object {
        /**
         * The last day a period beginning on [from] may end on: a period booked at once spans one month at most
         * (15.03. until 14.04.), so a mistyped end date can't book a whole year.
         */
        fun lastDayOfPeriod(from: LocalDate): LocalDate = from.plusMonths(1).minusDays(1)

        private val DEFAULT_HOURS_PER_DAY = BigDecimal(8)

        private val HALF = BigDecimal("0.5")

        private fun minutesOf(hours: BigDecimal?): Int {
            hours ?: return 0
            return roundTo5Minutes(hours.multiply(BigDecimal(60)))
        }

        private fun roundTo5Minutes(minutes: BigDecimal): Int {
            return minutes.divide(BigDecimal(5), 0, RoundingMode.HALF_UP).toInt() * 5
        }
    }
}
