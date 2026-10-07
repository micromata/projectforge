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
import org.projectforge.common.DateFormatType
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
import java.util.Date

/**
 * Books one time sheet per working day of a period, e.g. a vacation, or the absence of a colleague booked by the
 * office (parental leave, illness).
 *
 * Weekends and holidays are skipped; a half working day (24.12., 31.12.) and a half vacation day at either end are
 * booked pro rata. A day that has time sheets of the user already is booked as well, unless the new time sheet would
 * collide with one of them the way [TimesheetDao.hasTimeOverlap] refuses it (an overlap released by a shared cost
 * element is fine). All checks (access, protected periods, cost unit, bookable task) stay with [TimesheetDao]: this
 * service only computes the days, so that the preview shows what the booking does.
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

        /** The time sheet of the day would collide with an existing one of the user: the day is skipped. */
        OVERLAP,

        /**
         * The time sheet of the day would overlap an existing one of the user, and no task is chosen yet: whether the
         * overlap is allowed (shared cost element) depends on it. Nothing is booked without a task anyway.
         */
        OVERLAP_UNKNOWN,
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
     * @param note The title of the holiday, if the day is one; the time of the existing time sheet for an overlap
     * ([Status.OVERLAP], [Status.OVERLAP_UNKNOWN]), e.g. "10:00–11:00".
     * @param sharedOverlap The time of an existing time sheet the booked one overlaps, as a shared cost element allows.
     */
    class Day(
        val date: LocalDate,
        val status: Status,
        val minutes: Int,
        val note: String? = null,
        val sharedOverlap: String? = null,
    ) {
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
     * fails the way the booking would; overlaps are checked for every day (see [Status.OVERLAP]).
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
        val booked = existingSheets(checkUser(userId), from, until).flatMap { sheet ->
            listOfNotNull(sheet.startTime, sheet.stopTime).map { localDateOf(it) }
        }.toSet()
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
        val holidays = Holidays.instance
        val days = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(until) }.map { date ->
            val day = PFDay.from(date)
            when {
                day.isWeekend() -> Day(date, Status.WEEKEND, 0)
                !holidays.isWorkingDay(date) -> Day(date, Status.HOLIDAY, 0, holidayTitle(day))
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
        return checkOverlaps(request, userId, days)
    }

    /**
     * The booking days of [days] whose time sheet would collide with an existing one of the user are skipped
     * ([Status.OVERLAP]), as [TimesheetDao.hasTimeOverlap] would refuse them; an allowed overlap (shared cost element)
     * is noted. Without a task, every overlap is [Status.OVERLAP_UNKNOWN].
     */
    private fun checkOverlaps(request: Request, userId: Long, days: List<Day>): List<Day> {
        val booking = days.filter { it.booking }
        if (booking.isEmpty()) {
            return days
        }
        val existing = existingSheets(userId, booking.first().date, booking.last().date)
        if (existing.isEmpty()) {
            return days
        }
        val candidate = TimesheetDO().also {
            it.task = taskTree.getTaskById(request.taskId)
            it.kost2 = request.kost2Id?.let { id -> kost2Dao.findOrLoad(id) }
        }
        return days.map { day ->
            if (!day.booking) {
                return@map day
            }
            val (start, stop) = intervalOf(day, request.startTime!!)
            // Strictly overlapping, as hasTimeOverlap (one ending when the other starts is no collision).
            val overlapping = existing.filter { it.startTime!! < stop.utilDate && it.stopTime!! > start.utilDate }
            if (overlapping.isEmpty()) {
                return@map day
            }
            if (candidate.task == null) {
                return@map Day(day.date, Status.OVERLAP_UNKNOWN, 0, formatPeriod(overlapping.first(), day.date))
            }
            val collision = overlapping.firstOrNull { timesheetDao.isCollision(candidate, it) }
            if (collision != null) {
                Day(day.date, Status.OVERLAP, 0, formatPeriod(collision, day.date))
            } else {
                Day(day.date, day.status, day.minutes, day.note, formatPeriod(overlapping.first(), day.date))
            }
        }
    }

    /** Start and stop of the time sheet booked on the given day. */
    private fun intervalOf(day: Day, startTime: LocalTime): Pair<PFDateTime, PFDateTime> {
        val start = PFDateTime.from(LocalDateTime.of(day.date, startTime), ThreadLocalUserContext.zoneId)
            .withPrecision(DatePrecision.MINUTE_5)
        return Pair(start, start.plus(day.minutes.toLong(), ChronoUnit.MINUTES))
    }

    /** "10:00–11:00", with the date of a time that isn't on the given day (a time sheet over midnight). */
    private fun formatPeriod(sheet: TimesheetDO, date: LocalDate): String {
        fun format(time: Date): String {
            val dateTime = PFDateTime.from(time).withZoneSameInstant(ThreadLocalUserContext.zoneId)
            val timeOfDay = dateTime.format(DateFormatType.TIME_OF_DAY_MINUTES)
            return if (dateTime.localDate == date) timeOfDay else "${dateTime.format(DateFormatType.DATE_WITHOUT_YEAR)} $timeOfDay"
        }
        return "${format(sheet.startTime!!)}–${format(sheet.stopTime!!)}"
    }

    private fun createTimesheets(request: Request, days: List<Day>): List<TimesheetDO> {
        val user = userGroupCache.getUser(request.userId ?: ThreadLocalUserContext.loggedInUserId)
        val task = taskTree.getTaskById(request.taskId)
            ?: throw UserException("timesheet.bookDays.error.taskRequired")
        val kost2 = request.kost2Id?.let { kost2Dao.findOrLoad(it) }
        return days.filter { it.booking }.map { day ->
            val (start, stop) = intervalOf(day, request.startTime!!)
            TimesheetDO().also {
                it.user = user
                it.task = task
                it.kost2 = kost2
                it.location = request.location?.trim()?.ifEmpty { null }
                it.description = request.description?.trim()?.ifEmpty { null }
                it.startTime = start.utilDate
                it.stopTime = stop.utilDate
            }
        }
    }

    /**
     * The time sheets of the user touching the given period; the day before is included for a time sheet over
     * midnight. Time sheets without start or stop are ignored.
     */
    private fun existingSheets(userId: Long, from: LocalDate, until: LocalDate): List<TimesheetDO> {
        val zoneId = ThreadLocalUserContext.zoneId
        val filter = TimesheetFilter()
        filter.userId = userId
        filter.startTime = PFDateTime.from(from.minusDays(1), zoneId).utilDate
        filter.stopTime = PFDateTime.from(until, zoneId).utilDate // Extended to the end of day by internalGetList.
        return timesheetDao.internalGetList(filter, checkAccess = false)
            .filter { it.startTime != null && it.stopTime != null }
    }

    private fun localDateOf(time: Date): LocalDate {
        return PFDateTime.from(time).withZoneSameInstant(ThreadLocalUserContext.zoneId).localDate
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
