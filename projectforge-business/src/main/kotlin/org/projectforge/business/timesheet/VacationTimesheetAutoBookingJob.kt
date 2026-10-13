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
import jakarta.annotation.PostConstruct
import org.projectforge.business.PfCaches
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.jobs.JobLogEvents
import org.projectforge.business.teamcal.CalendarHelper
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.common.i18n.UserException
import org.projectforge.common.logging.error
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerSchedule
import org.projectforge.framework.calendar.Holidays
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.scheduling.SchedulerJobStatsService
import org.projectforge.security.RegisterUser4Thread
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * Books the days of approved vacations (special ones included) as time sheets afterwards, every night: each working
 * day up to yesterday, on which the user has no time sheet at all, gets one on the configured vacation task and cost
 * unit ([TimesheetVacationBookingConfig]). Vacation is never booked in advance, so a vacation changed or cancelled
 * later leaves no time sheets behind.
 *
 * Each day is processed once only: a run books the days from the day of the last successful run on (that run booked
 * the days before it, see [SchedulerJobStatsService.lastSuccessStart]), but at most
 * [ConfigurationService.vacationAutoBookingDays] days back (catching up nights the job didn't run). So a vacation
 * time sheet the user deleted is not booked again, and a vacation approved only after its days were processed is not
 * booked automatically either (the monthly report lists such days, they are booked in the calendar). A day failing
 * unexpectedly is logged as error and not retried either (it is booked in the calendar). Only a run failing as a whole
 * doesn't count: the next one processes its days again (days with a time sheet are skipped anyway).
 *
 * The days are booked as the user would in the calendar ([TimesheetDayBookingService]): with the user's hours per day
 * (a fifth of the weekly hours valid on that day),
 * half of them on a half vacation day or a half working day (24.12., 31.12.), and with the user's access rights, so a
 * day in a closed period or on a task the user may not book on is skipped.
 */
@Component
class VacationTimesheetAutoBookingJob {
    class Result(var bookedDays: Int = 0, var users: Int = 0, var skippedDays: Int = 0, var failedDays: Int = 0)

    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var vacationBookingConfig: TimesheetVacationBookingConfig

    @Autowired
    private lateinit var dayBookingService: TimesheetDayBookingService

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var schedulerJobRunner: SchedulerJobRunner

    @Autowired
    private lateinit var schedulerJobStatsService: SchedulerJobStatsService

    @PostConstruct
    private fun postConstruct() {
        schedulerJobRunner.register(SCHEDULER_JOB, ::execute, ::inactiveReason)
    }

    private fun inactiveReason(): String? {
        if (configurationService.vacationAutoBookingDays <= 0) {
            return "${ConfigurationParam.TIMESHEET_VACATION_AUTO_BOOKING_DAYS.key} is 0 (configuration page)."
        }
        if (vacationBookingConfig.resolve() == null) {
            return "No valid vacation task and cost unit configured (${ConfigurationParam.TIMESHEET_VACATION_TASK_ID.key}, " +
                    "${ConfigurationParam.TIMESHEET_VACATION_KOST2.key} on the configuration page)."
        }
        return null
    }

    @Scheduled(cron = CRON)
    fun execute() {
        schedulerJobRunner.runAsync(SCHEDULER_JOB) { run ->
            val result = bookVacations()
            run.note(
                "Booked ${result.bookedDays} vacation day(s) of ${result.users} user(s), skipped ${result.skippedDays}" +
                        if (result.failedDays > 0) ", failed ${result.failedDays}." else "."
            )
            // Failed days don't fail the run (they are logged as errors): a failed run would let the next ones process
            // its days again and book vacation time sheets deleted meanwhile again, every night if the failure persists.
        }
    }

    /**
     * Books the vacation days of all users, see the class description.
     *
     * @param today The day of the run (the days before it are booked), today in the default time zone if not given.
     * @param lastSuccessDay The day of the last successful run (the days before it are booked already), null if none.
     */
    fun bookVacations(today: LocalDate? = null, lastSuccessDay: LocalDate? = lastSuccessDay()): Result {
        val result = Result()
        val days = configurationService.vacationAutoBookingDays
        val booking = vacationBookingConfig.resolve()
        if (days <= 0 || booking == null) {
            log.info { "Booking of vacation as time sheets not configured, nothing to do." }
            return result
        }
        val refDay = today ?: LocalDate.now(zoneId)
        val until = refDay.minusDays(1)
        val from = listOfNotNull(refDay.minusDays(days.toLong()), lastSuccessDay).max()
        if (from.isAfter(until)) {
            log.info { "Vacation already booked as time sheets by the run on $lastSuccessDay, nothing to do." }
            return result
        }
        val vacationsByEmployee = vacationDao.getVacationsOfAllEmployeesForPeriod(from, until)
            .filter { it.status == VacationStatus.APPROVED && !it.deleted }
            .groupBy { it.employee?.id }
        // Widened by a day on both sides: the time zones of the users differ, the days are cut per user below.
        val startTimesByUser = if (vacationsByEmployee.isEmpty()) {
            emptyMap()
        } else {
            timesheetDao.getStartTimesByUser(utcDate(from.minusDays(1)), utcDate(until.plusDays(2)))
        }
        vacationsByEmployee.forEach { (employeeId, vacations) ->
            val employee = caches.getEmployee(employeeId) ?: return@forEach
            val user = caches.getUser(employee.user?.id)
            if (user?.id == null || !user.hasSystemAccess()) {
                return@forEach
            }
            val zoneId = user.timeZone.toZoneId()
            val bookedDays = startTimesByUser[user.id].orEmpty().map { it.toInstant().atZone(zoneId).toLocalDate() }
                .toMutableSet()
            val booked = bookVacationsOfUser(user, vacations, from, until, bookedDays, booking, result)
            if (booked > 0) {
                result.users++
                result.bookedDays += booked
            }
        }
        log.info {
            "Vacation booked as time sheets from $from until $until: ${result.bookedDays} day(s) of ${result.users} " +
                    "user(s), skipped ${result.skippedDays}, failed ${result.failedDays}."
        }
        return result
    }

    /**
     * Books each working day of the given vacations from [from] until [until] on which the user has no time sheet
     * ([bookedDays], extended by the days booked here), one day a time: a day refused (closed period, no access) is
     * skipped without affecting the others.
     *
     * @return The number of days booked.
     */
    private fun bookVacationsOfUser(
        user: PFUserDO,
        vacations: List<VacationDO>,
        from: LocalDate,
        until: LocalDate,
        bookedDays: MutableSet<LocalDate>,
        booking: TimesheetVacationBookingConfig.VacationBooking,
        result: Result,
    ): Int {
        var count = 0
        // Restored afterwards: a run started by hand may run in the thread of the logged-in user.
        val previousUserContext = ThreadLocalUserContext.userContext
        try {
            // The days, the hours and the description in the user's time zone, locale and working hours:
            RegisterUser4Thread.registerUser(user)
            val description = vacationDescription(booking)
            vacations.sortedBy { it.startDate }.forEach { vacation ->
                val start = vacation.startDate ?: return@forEach
                val end = vacation.endDate ?: return@forEach
                var day = maxOf(start, from)
                val last = minOf(end, until)
                while (!day.isAfter(last)) {
                    if (Holidays.instance.isWorkingDay(day) && day !in bookedDays) {
                        val request = TimesheetDayBookingService.Request(
                            userId = user.id,
                            taskId = booking.task.id,
                            kost2Id = booking.kost2.id,
                            description = description,
                            startDate = day,
                            endDate = day,
                            startTime = START_TIME,
                            // The weekly hours valid on that day (they may have changed since).
                            hoursPerDay = dayBookingService.defaultHoursPerDay(user.id, day),
                            halfDayBegin = day == start && vacation.halfDayBegin == true,
                            halfDayEnd = day == end && vacation.halfDayEnd == true,
                        )
                        if (bookDay(user, request, result)) {
                            count++
                            bookedDays.add(day)
                        }
                    }
                    day = day.plusDays(1)
                }
            }
        } finally {
            RegisterUser4Thread.unregister()
            previousUserContext?.let { RegisterUser4Thread.registerUser(it) }
        }
        return count
    }

    /** @return true if the day was booked. */
    private fun bookDay(user: PFUserDO, request: TimesheetDayBookingService.Request, result: Result): Boolean {
        try {
            val days = dayBookingService.book(request, addToRecent = false)
            if (days.any { it.booking }) {
                return true
            }
            result.skippedDays++ // E.g. an overlapping time sheet beginning the day before.
        } catch (ex: UserException) {
            // Expected, e.g. a closed period (time sheet protection) or a task the user may not book on.
            log.info { "Vacation of user '${user.username}' on ${request.startDate} not booked: ${ex.message} ${ex.params?.joinToString() ?: ""}" }
            result.skippedDays++
        } catch (ex: Exception) {
            log.error(JobLogEvents.VACATION_AUTO_BOOKING_FAILED, ex) {
                "Vacation of user '${user.username}' on ${request.startDate} not booked: ${ex.message}"
            }
            result.failedDays++
        }
        return false
    }

    /** The day of the last successful run of this job (in the default time zone), null if none. */
    private fun lastSuccessDay(): LocalDate? {
        return schedulerJobStatsService.lastSuccessStart(SCHEDULER_JOB.id)?.let {
            Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
        }
    }

    private val zoneId: ZoneId
        get() = configurationService.timezone?.toZoneId() ?: ZoneId.systemDefault()

    /** As the calendar dialog: no text if the calendar title of the vacation task says so already. */
    private fun vacationDescription(booking: TimesheetVacationBookingConfig.VacationBooking): String? {
        val text = translate("timesheet.bookDays.vacationDescription")
        val sheet = TimesheetDO().also { it.task = booking.task; it.kost2 = booking.kost2 }
        return if (CalendarHelper.getTitle(sheet).contains(text, ignoreCase = true)) null else text
    }

    private fun utcDate(day: LocalDate): Date = Date.from(day.atStartOfDay(ZoneOffset.UTC).toInstant())

    companion object {
        private const val CRON = "\${projectforge.cron.vacationTimesheets}"

        /** The start time of a booked vacation day, as in the calendar dialog. */
        private val START_TIME = LocalTime.of(9, 0)

        val SCHEDULER_JOB = SchedulerJobDefinition(
            "timesheet.vacationAutoBooking", SchedulerJobArea.BUSINESS, VacationTimesheetAutoBookingJob::class.java,
            "execute", SchedulerSchedule.Cron(CRON),
        )
    }
}
