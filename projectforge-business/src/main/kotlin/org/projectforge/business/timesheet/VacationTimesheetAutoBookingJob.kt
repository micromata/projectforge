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
import jakarta.persistence.Tuple
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
import org.projectforge.framework.persistence.jpa.PfPersistenceService
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
import java.time.temporal.ChronoUnit
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * Books the days of approved vacations (special ones included) as time sheets afterwards, every night: each working
 * day up to yesterday, on which the user has no time sheet at all, gets one on the configured vacation task and cost
 * unit ([TimesheetVacationBookingConfig]). Vacation is never booked in advance. "Yesterday" is the user's: the days
 * are cut in each user's time zone, so a user west of the server never gets the current day booked.
 *
 * The booked time sheets carry the vacation as reference ([referenceOf]). A vacation shortened, rejected or deleted
 * after its days were booked doesn't leave them behind: each run marks the time sheets of the vacations changed since
 * the last run as deleted, whose day the vacation no longer covers ([removeUncoveredSheets]). Only these time sheets
 * of the job are touched, never one entered by the user (also not one booked in the calendar), and only with the
 * user's rights (a closed period keeps its time sheets).
 *
 * Each day is processed once only: a run books the days from the day of the last successful run on (that run booked
 * the days before it, see [SchedulerJobStatsService.lastSuccessStart]). So a vacation time sheet the user deleted is
 * not booked again, and a vacation approved only after its days were processed is not booked automatically either
 * (the monthly report lists such days, they are booked in the calendar). A day refused for the user (closed period,
 * no access) or failing unexpectedly (logged as error) is not retried either (it is booked in the calendar).
 *
 * The job heals itself: a run failing as a whole doesn't count, nor does a night the job didn't run. The next
 * successful run books all days since the last successful one (days with a time sheet are skipped anyway), up to
 * [MAX_CATCH_UP_DAYS] back, the days the job's statistics remember a run (a cut is logged as warning). A run fails
 * only for a cause not of the users: no time sheet is bookable on the vacation task at all (closed task, ended
 * project, see [TimesheetVacationBookingConfig.CheckResult.unbookable]). Once the finance department solved it, the
 * next run books the days waiting meanwhile. Only the very first run (no run known) books
 * [ConfigurationService.vacationAutoBookingDays] days back.
 *
 * The days are booked as the user would in the calendar ([TimesheetDayBookingService]): with the user's hours per day
 * (a fifth of the weekly hours valid on that day),
 * half of them on a half vacation day or a half working day (24.12., 31.12.), and with the user's access rights, so a
 * day in a closed period or on a task the user may not book on is skipped.
 */
@Component
class VacationTimesheetAutoBookingJob {
    /**
     * @param removedDays The booked days marked as deleted, as their vacation no longer covers them.
     * @param refusedDays The days refused for the user by the booking (part of [skippedDays]).
     * @param from The first day processed, null if nothing to do.
     * @param error Set, if the run is to be failed (see the class description).
     */
    class Result(
        var bookedDays: Int = 0,
        var users: Int = 0,
        var skippedDays: Int = 0,
        var failedDays: Int = 0,
        var removedDays: Int = 0,
        var refusedDays: Int = 0,
        var from: LocalDate? = null,
        var error: String? = null,
    )

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
    private lateinit var persistenceService: PfPersistenceService

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
        val checked = vacationBookingConfig.resolveChecked()
        // A task no time sheet is bookable on keeps the job active: its runs fail, until the cause is solved.
        if (checked == null || checked.problem != null && !checked.unbookable) {
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
                        (if (result.failedDays > 0) ", failed ${result.failedDays}" else "") +
                        (if (result.removedDays > 0) ", removed ${result.removedDays}" else "") + "."
            )
            // Failed or refused days don't fail the run (they are logged): a failed run would let the next ones process
            // its days again and book vacation time sheets deleted meanwhile again, every night if the failure persists.
            // An unbookable vacation task does: the days are to be processed again, once it's solved.
            result.error?.let { error ->
                log.error(JobLogEvents.VACATION_AUTO_BOOKING_FAILED) { error }
                run.fail(error)
            }
        }
    }

    /**
     * Books the vacation days of all users, see the class description.
     *
     * @param today The day of the run (the days before it are booked), today in the default time zone if not given.
     * @param lastSuccessDay The day of the last successful run (the days before it are booked already), null if none.
     * @param failedWithoutSuccess True if the job failed without any success the last [MAX_CATCH_UP_DAYS] days (so
     * [lastSuccessDay] is null although the job ran): the days are caught up as far as possible.
     * @param now The time of the run, for the days of the users in their time zones. If only [today] is given, the
     * days are cut in the default time zone for all users.
     * @param changedSince The vacations changed since are checked for booked days they no longer cover, all if null.
     */
    fun bookVacations(
        today: LocalDate? = null,
        lastSuccessDay: LocalDate? = lastSuccessDay(),
        failedWithoutSuccess: Boolean = lastSuccessDay == null && failedWithoutSuccess(),
        now: Instant? = null,
        changedSince: Instant? = lastSuccessStart()?.minus(1, ChronoUnit.DAYS),
    ): Result {
        val result = Result()
        val days = configurationService.vacationAutoBookingDays
        val checked = vacationBookingConfig.resolveChecked()
        if (days > 0 && checked?.unbookable == true) {
            result.error = "Vacation not booked as time sheets: ${checked.message}."
            return result
        }
        val booking = checked?.booking
        if (days <= 0 || booking == null) {
            log.info { "Booking of vacation as time sheets not configured, nothing to do." }
            return result
        }
        removeUncoveredSheets(changedSince, result)
        val instant = now ?: Instant.now()
        val refDay = today ?: LocalDate.ofInstant(instant, zoneId)
        val until = refDay.minusDays(1)
        val from = catchUpFrom(refDay, lastSuccessDay, failedWithoutSuccess, days)
        result.from = from
        if (from.isAfter(until)) {
            log.info { "Vacation already booked as time sheets by the run on $lastSuccessDay, nothing to do." }
            return result
        }
        // Widened by a day on both sides: the time zones of the users differ, the days are cut per user below.
        val vacationsByEmployee = vacationDao.getVacationsOfAllEmployeesForPeriod(from.minusDays(1), until.plusDays(1))
            .filter { it.status == VacationStatus.APPROVED && !it.deleted }
            .groupBy { it.employee?.id }
        val startTimesByUser = if (vacationsByEmployee.isEmpty()) {
            emptyMap()
        } else {
            timesheetDao.getStartTimesByUser(utcDate(from.minusDays(2)), utcDate(until.plusDays(3)))
        }
        vacationsByEmployee.forEach { (employeeId, vacations) ->
            val employee = caches.getEmployee(employeeId) ?: return@forEach
            val user = caches.getUser(employee.user?.id)
            if (user?.id == null || !user.hasSystemAccess()) {
                return@forEach
            }
            val userZoneId = user.timeZone.toZoneId()
            // The days of the run in the user's time zone: the user's today may be the day before (west of the
            // server) or after the server's. Shifted alike, every day of the user is processed once by the runs.
            val offset = if (now == null && today != null) {
                0L
            } else {
                ChronoUnit.DAYS.between(LocalDate.ofInstant(instant, userZoneId), refDay).coerceIn(-1L, 1L)
            }
            val bookedDays = startTimesByUser[user.id].orEmpty().map { it.toInstant().atZone(userZoneId).toLocalDate() }
                .toMutableSet()
            val booked = bookVacationsOfUser(
                user, vacations, from.minusDays(offset), until.minusDays(offset), bookedDays, booking, result,
            )
            if (booked > 0) {
                result.users++
                result.bookedDays += booked
            }
        }
        log.info {
            "Vacation booked as time sheets from $from until $until: ${result.bookedDays} day(s) of ${result.users} " +
                    "user(s), skipped ${result.skippedDays} (refused ${result.refusedDays}), failed ${result.failedDays}, " +
                    "removed ${result.removedDays}."
        }
        return result
    }

    /**
     * The first day to book: the day of the last successful run, else (the job failed for all the days known)
     * [MAX_CATCH_UP_DAYS] back, else (the first run) the configured [days] back. Never more than [MAX_CATCH_UP_DAYS]
     * back, a cut is logged as warning (the days before are booked in the calendar).
     */
    private fun catchUpFrom(
        refDay: LocalDate,
        lastSuccessDay: LocalDate?,
        failedWithoutSuccess: Boolean,
        days: Int,
    ): LocalDate {
        val earliest = refDay.minusDays(MAX_CATCH_UP_DAYS)
        val wanted = when {
            lastSuccessDay != null -> lastSuccessDay
            failedWithoutSuccess -> {
                log.warn {
                    "Vacation booking of time sheets failed for the last $MAX_CATCH_UP_DAYS days without any success: " +
                            "catching up from $earliest, days before not booked (book them in the calendar)."
                }
                return earliest
            }

            else -> refDay.minusDays(days.toLong())
        }
        if (wanted.isBefore(earliest)) {
            log.warn {
                "Vacation booking of time sheets catches up from $earliest only, the last successful run on " +
                        "$wanted is too long ago: days before not booked (book them in the calendar)."
            }
            return earliest
        }
        return wanted
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
        // The days, the hours and the description in the user's time zone, locale and working hours:
        asUser(user) {
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
                            reference = referenceOf(vacation.id),
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
            val message = "${ex.message} ${ex.params?.joinToString() ?: ""}".trim()
            log.info { "Vacation of user '${user.username}' on ${request.startDate} not booked: $message" }
            result.skippedDays++
            result.refusedDays++
        } catch (ex: Exception) {
            log.error(JobLogEvents.VACATION_AUTO_BOOKING_FAILED, ex) {
                "Vacation of user '${user.username}' on ${request.startDate} not booked: ${ex.message}"
            }
            result.failedDays++
        }
        return false
    }

    /**
     * Marks the time sheets booked by this job ([referenceOf]) as deleted, whose day their vacation no longer covers:
     * shortened, no longer approved (rejected, in progress again) or deleted. Only the vacations changed since
     * [changedSince] (all, if null) are checked. The days are deleted with the rights of the user, so a day in a
     * closed period is kept (and logged).
     */
    private fun removeUncoveredSheets(changedSince: Instant?, result: Result) {
        val select = "SELECT v.id AS id, v.startDate AS startDate, v.endDate AS endDate, v.status AS status," +
                " v.deleted AS deleted FROM VacationDO v"
        val vacations = if (changedSince == null) {
            persistenceService.executeQuery(select, Tuple::class.java)
        } else {
            persistenceService.executeQuery(
                "$select WHERE v.lastUpdate >= :since", Tuple::class.java, Pair("since", Date.from(changedSince)),
            )
        }.associate { tuple ->
            val covered = tuple.get("status") == VacationStatus.APPROVED && tuple.get("deleted") != true
            referenceOf(tuple.get("id") as Long) to
                    if (covered) tuple.get("startDate") as LocalDate? to tuple.get("endDate") as LocalDate? else null
        }
        if (vacations.isEmpty()) {
            return
        }
        val sheets = persistenceService.executeQueryBatched(
            "SELECT t FROM TimesheetDO t WHERE t.deleted = false AND t.reference IN :references",
            TimesheetDO::class.java,
            "references",
            vacations.keys,
        )
        sheets.groupBy { it.user?.id }.forEach { (userId, userSheets) ->
            val user = caches.getUser(userId) ?: return@forEach
            val userZoneId = user.timeZone.toZoneId()
            asUser(user) {
                userSheets.forEach sheet@{ sheet ->
                    val day = sheet.startTime?.toInstant()?.atZone(userZoneId)?.toLocalDate() ?: return@sheet
                    val (start, end) = vacations[sheet.reference] ?: (null to null)
                    if (start != null && end != null && !day.isBefore(start) && !day.isAfter(end)) {
                        return@sheet // Still covered.
                    }
                    try {
                        timesheetDao.markAsDeleted(sheet)
                        result.removedDays++
                    } catch (ex: UserException) {
                        // Expected, e.g. a closed period (time sheet protection).
                        log.info { "Vacation time sheet of user '${user.username}' on $day not removed: ${ex.message}" }
                    } catch (ex: Exception) {
                        log.error(JobLogEvents.VACATION_AUTO_BOOKING_FAILED, ex) {
                            "Vacation time sheet of user '${user.username}' on $day not removed: ${ex.message}"
                        }
                        result.failedDays++
                    }
                }
            }
        }
    }

    /** Runs [block] with the given user registered for the thread, restoring the previous one afterwards. */
    private fun asUser(user: PFUserDO, block: () -> Unit) {
        // Restored afterwards: a run started by hand may run in the thread of the logged-in user.
        val previousUserContext = ThreadLocalUserContext.userContext
        try {
            RegisterUser4Thread.registerUser(user)
            block()
        } finally {
            RegisterUser4Thread.unregister()
            previousUserContext?.let { RegisterUser4Thread.registerUser(it) }
        }
    }

    /** The start of the last successful run of this job, null if none. */
    private fun lastSuccessStart(): Instant? {
        return schedulerJobStatsService.lastSuccessStart(SCHEDULER_JOB.id)?.let { Instant.ofEpochMilli(it) }
    }

    private fun failedWithoutSuccess(): Boolean = schedulerJobStatsService.failedWithoutSuccess(SCHEDULER_JOB.id)

    /** The day of the last successful run of this job (in the default time zone), null if none. */
    private fun lastSuccessDay(): LocalDate? {
        return lastSuccessStart()?.atZone(zoneId)?.toLocalDate()
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

        /**
         * The reference of the time sheets booked by this job for the given vacation: it tells them from time sheets
         * entered by the user, which are never touched.
         */
        fun referenceOf(vacationId: Long?): String = "$REFERENCE_PREFIX$vacationId"

        private const val REFERENCE_PREFIX = "vacation#"

        /**
         * The days a run catches up at most: as far as the statistics of the job know its last successful run back
         * ([SchedulerJobStatsService.lastSuccessStart]).
         */
        const val MAX_CATCH_UP_DAYS = SchedulerJobStatsService.LAST_RUN_SEARCH_DAYS

        val SCHEDULER_JOB = SchedulerJobDefinition(
            "timesheet.vacationAutoBooking", SchedulerJobArea.BUSINESS, VacationTimesheetAutoBookingJob::class.java,
            "execute", SchedulerSchedule.Cron(CRON),
        )
    }
}
