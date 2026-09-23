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

package org.projectforge.rest

import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.timesheet.OrderDirection
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The personal statistics page ("My statistics", `/next/personalStatistics`), successor of Wicket's
 * `PersonalStatisticsPage` (`wa/personalStatistics`).
 *
 * Like [org.projectforge.rest.fibu.MonthlyEmployeeReportRest] and [SendTextMessageRest] it is a non-entity,
 * standalone read-only page: it exposes plain JSON so the next frontend can render — with recharts — the two
 * "timesheet discipline" charts of the logged-in user over the last [LAST_N_DAYS] days. The computation is
 * ported from the former `TimesheetDisciplineChartBuilder`:
 *
 *  1. cumulative planned ("Soll") vs. actually booked ("Ist") working hours, and
 *  2. the average number of days between a timesheet's date and its booking vs. the 2-day goal.
 *
 * CSRF is inherited via `RestAuthenticationUtils`/`RestCsrfProtection` for all `/rs` endpoints; the legacy
 * page was not 2FA-gated, so no 2FA registration is needed.
 */
@RestController
@RequestMapping("${Rest.URL}/personalStatistics")
class PersonalStatisticsRest {
    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    /** One day of the discipline chart: planned ("soll") vs. booked ("ist") cumulative working hours. */
    class WorkingHoursPoint(
        val date: String,
        val soll: Double,
        val ist: Double,
    )

    /** One day of the booking-latency chart: the 2-day goal ("plan") vs. the actual average ("actual"). */
    class BookingLatencyPoint(
        val date: String,
        val plan: Double,
        val actual: Double,
    )

    /** Key figures shown in the two chart legends. */
    class Summary(
        val planWorkingHours: Double,
        val actualWorkingHours: Double,
        val averageBookingLatency: BigDecimal,
        val plannedBookingLatency: Double,
    )

    /** Full response: both series plus the legend figures and the covered period. */
    class Statistics(
        val lastNDays: Int,
        val workingHours: List<WorkingHoursPoint>,
        val bookingLatency: List<BookingLatencyPoint>,
        val summary: Summary,
    )

    @GetMapping
    fun getStatistics(): Statistics {
        val userId = ThreadLocalUserContext.loggedInUserId
        val employee = employeeCache.getEmployeeByUserId(userId)
        var workingHoursPerDay = 8.0
        if (employee != null && NumberHelper.isGreaterZero(employee.weeklyWorkingHours)) {
            workingHoursPerDay = employee.weeklyWorkingHours!!.toDouble() / 5
        }
        val timesheets = loadTimesheets(userId)
        val workingHours = buildWorkingHours(timesheets, workingHoursPerDay)
        val bookingLatency = buildBookingLatency(timesheets)
        return Statistics(
            lastNDays = LAST_N_DAYS,
            workingHours = workingHours.points,
            bookingLatency = bookingLatency.points,
            summary = Summary(
                planWorkingHours = workingHours.planWorkingHours,
                actualWorkingHours = workingHours.actualWorkingHours,
                averageBookingLatency = bookingLatency.average,
                plannedBookingLatency = PLANNED_AVERAGE_DIFFERENCE_BETWEEN_TIMESHEET_AND_BOOKING,
            ),
        )
    }

    private fun loadTimesheets(userId: Long?): List<TimesheetDO> {
        val now = PFDateTime.now()
        val filter = TimesheetFilter()
        filter.stopTime = now.utilDate
        filter.startTime = now.minusDays(LAST_N_DAYS.toLong()).utilDate
        filter.userId = userId
        filter.orderType = OrderDirection.ASC
        return timesheetDao.select(filter)
    }

    private class WorkingHoursResult(
        val points: List<WorkingHoursPoint>,
        val planWorkingHours: Double,
        val actualWorkingHours: Double,
    )

    /**
     * Cumulative planned vs. actually booked working hours per day (chart #1). Planned hours grow by
     * [workingHoursPerDay] on every working day, weighted by the day's work fraction (holidays / half
     * holidays); booked hours grow by each timesheet's work-fraction duration.
     */
    private fun buildWorkingHours(timesheets: List<TimesheetDO>, workingHoursPerDay: Double): WorkingHoursResult {
        var dt = PFDateTime.now().minusDays(LAST_N_DAYS.toLong())
        val iterator = timesheets.iterator()
        var current: TimesheetDO? = if (iterator.hasNext()) iterator.next() else null
        val points = ArrayList<WorkingHoursPoint>(LAST_N_DAYS + 1)
        var planWorkingHours = 0.0
        var actualWorkingHours = 0.0
        val holidays = Holidays.instance
        for (i in 0..LAST_N_DAYS) {
            var timesheetDateTime = current?.let { PFDateTime.from(it.startTime!!) }
            while (current != null && (dt.isSameDay(timesheetDateTime!!) || timesheetDateTime.isBefore(dt))) {
                actualWorkingHours += current.workFractionDuration.toDouble() / 3600000
                current = if (iterator.hasNext()) iterator.next() else null
                timesheetDateTime = current?.let { PFDateTime.from(it.startTime!!) }
            }
            if (holidays.isWorkingDay(dt.dateTime)) {
                val workFraction = holidays.getWorkFraction(dt)
                planWorkingHours += (workFraction?.toDouble() ?: 1.0) * workingHoursPerDay
            }
            points.add(WorkingHoursPoint(dt.dateTime.toLocalDate().toString(), planWorkingHours, actualWorkingHours))
            dt = dt.plusDays(1)
        }
        return WorkingHoursResult(points, planWorkingHours, actualWorkingHours)
    }

    private class BookingLatencyResult(
        val points: List<BookingLatencyPoint>,
        val average: BigDecimal,
    )

    /**
     * Average number of days between a timesheet's date and the time it was actually booked, per day
     * (chart #2), against the constant [PLANNED_AVERAGE_DIFFERENCE_BETWEEN_TIMESHEET_AND_BOOKING]-day goal.
     * Only days that actually have bookings contribute a point (as in the former chart builder).
     */
    private fun buildBookingLatency(timesheets: List<TimesheetDO>): BookingLatencyResult {
        var dt = PFDateTime.now().minusDays(LAST_N_DAYS.toLong())
        val iterator = timesheets.iterator()
        var current: TimesheetDO? = if (iterator.hasNext()) iterator.next() else null
        val points = ArrayList<BookingLatencyPoint>()
        var numberOfBookedDays = 0L
        var totalDifference = 0L
        for (i in 0..LAST_N_DAYS) {
            var difference = 0L
            var totalDuration = 0L // Weight for the average.
            var timesheetDateTime = current?.let { PFDateTime.from(it.startTime!!) }
            while (current != null && (dt.isSameDay(timesheetDateTime!!) || timesheetDateTime.isBefore(dt))) {
                val duration = current.workFractionDuration
                difference += (current.created!!.time - current.startTime!!.time) * duration
                totalDuration += duration
                current = if (iterator.hasNext()) iterator.next() else null
                timesheetDateTime = current?.let { PFDateTime.from(it.startTime!!) }
            }
            val averageDifference = if (difference > 0) difference.toDouble() / totalDuration / 86400000 else 0.0 // In days.
            if (averageDifference > 0) {
                points.add(
                    BookingLatencyPoint(
                        dt.dateTime.toLocalDate().toString(),
                        PLANNED_AVERAGE_DIFFERENCE_BETWEEN_TIMESHEET_AND_BOOKING,
                        averageDifference,
                    )
                )
                totalDifference += averageDifference.toLong()
                numberOfBookedDays++
            }
            dt = dt.plusDays(1)
        }
        val average = if (numberOfBookedDays > 0) {
            BigDecimal(totalDifference).divide(BigDecimal(numberOfBookedDays), 1, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        return BookingLatencyResult(points, average)
    }

    companion object {
        /** Number of days back the statistics cover (same window as the former Wicket page). */
        private const val LAST_N_DAYS = 45

        /** The targeted average number of days between a timesheet's date and its booking. */
        private const val PLANNED_AVERAGE_DIFFERENCE_BETWEEN_TIMESHEET_AND_BOOKING = 2.0
    }
}
