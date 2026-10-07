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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.teamcal.CalendarHelper
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetDayBookingService
import org.projectforge.business.timesheet.TimesheetVacationBookingConfig
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

private val log = KotlinLogging.logger {}

/**
 * Books time sheets for several days at once from the calendar, e.g. a vacation (see [TimesheetDayBookingService]).
 */
@RestController
@RequestMapping("${Rest.URL}/timesheet/bookDays")
class TimesheetBookDaysRest {
    class Initial(val startDate: LocalDate, val hoursPerDay: BigDecimal)

    class Vacation(
        val id: Long,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val halfDayBegin: Boolean,
        val halfDayEnd: Boolean,
        val special: Boolean,
        val status: VacationStatus,
        val workingDays: BigDecimal,
    )

    /**
     * The task and cost unit vacations are booked on (projectforge.timesheet.vacation.*).
     *
     * @param title The title the calendar shows for a time sheet booked on them.
     */
    class VacationBooking(val taskId: Long, val kost2Id: Long, val title: String)

    class Vacations(val vacations: List<Vacation>, val vacationBooking: VacationBooking?)

    class Request(
        var userId: Long? = null,
        var taskId: Long? = null,
        var kost2Id: Long? = null,
        var location: String? = null,
        var description: String? = null,
        var startDate: LocalDate? = null,
        var endDate: LocalDate? = null,
        /** HH:mm */
        var startTime: String? = null,
        var hoursPerDay: BigDecimal? = null,
        var halfDayBegin: Boolean = false,
        var halfDayEnd: Boolean = false,
        /** Only compute the days, book nothing. */
        var dryRun: Boolean = true,
    )

    class Day(
        val date: LocalDate,
        val status: TimesheetDayBookingService.Status,
        val hours: BigDecimal,
        val note: String?,
        /** The time of an existing time sheet the booked one overlaps, as a shared cost element allows. */
        val sharedOverlap: String?,
    )

    class Result(val days: List<Day>, val bookedCount: Int)

    @Autowired
    private lateinit var dayBookingService: TimesheetDayBookingService

    @Autowired
    private lateinit var vacationBookingConfig: TimesheetVacationBookingConfig

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var employeeCache: EmployeeCache

    @Autowired
    private lateinit var vacationService: VacationService

    /**
     * The defaults of the dialog: the first working day from [from] on without a time sheet of the user, and the
     * user's hours per day.
     */
    @GetMapping("initial")
    @AccessChecked("Own user, or another one's only for who may see the time sheets of others (TimesheetDao.showTimesheetsOfOtherUsers)")
    fun initial(
        @RequestParam("userId", required = false) userId: Long?,
        @RequestParam("from", required = false) from: LocalDate?,
    ): ResponseEntity<*> {
        return handle("initial") {
            Initial(
                dayBookingService.firstBookableDay(userId, from ?: LocalDate.now()),
                dayBookingService.defaultHoursPerDay(userId),
            )
        }
    }

    /**
     * The current and past vacations of the user (a year back and ahead, not rejected), newest first, and the task
     * and cost unit a vacation is booked on, if configured. Another user's vacations only for who may book for them,
     * an empty list otherwise.
     */
    @GetMapping("vacations")
    @AccessChecked("Own user, or another one's only for who may see the time sheets of others (TimesheetDao.showTimesheetsOfOtherUsers)")
    fun vacations(@RequestParam("userId", required = false) userId: Long?): Vacations {
        val loggedInUserId = ThreadLocalUserContext.requiredLoggedInUserId
        val ownUser = userId == null || userId == loggedInUserId
        val employee = if (ownUser || timesheetDao.showTimesheetsOfOtherUsers()) {
            employeeCache.getEmployeeByUserId(userId ?: loggedInUserId)
        } else null
        val today = LocalDate.now()
        val vacations = employee?.let {
            vacationService.getVacationsListForPeriod(it.id, today.minusYears(1), today.plusYears(1), withSpecial = true)
        }.orEmpty().mapNotNull { vacation ->
            Vacation(
                id = vacation.id ?: return@mapNotNull null,
                startDate = vacation.startDate ?: return@mapNotNull null,
                endDate = vacation.endDate ?: return@mapNotNull null,
                halfDayBegin = vacation.halfDayBegin == true,
                halfDayEnd = vacation.halfDayEnd == true,
                special = vacation.special == true,
                status = vacation.status ?: return@mapNotNull null,
                workingDays = VacationService.getVacationDays(vacation),
            )
        }.sortedByDescending { it.startDate }
        val booking = vacationBookingConfig.resolve()?.let {
            val sheet = TimesheetDO().also { sheet -> sheet.task = it.task; sheet.kost2 = it.kost2 }
            VacationBooking(it.task.id!!, it.kost2.id!!, CalendarHelper.getTitle(sheet))
        }
        return Vacations(vacations, booking)
    }

    /**
     * The days of the period and what happens on each; books them unless [Request.dryRun]. A rejected booking books
     * nothing at all and answers 406 with the reason.
     */
    @PostMapping
    @AccessChecked("TimesheetDao.insert (access, protection, bookable task); planning another user's days only for who may see the time sheets of others")
    fun bookDays(@RequestBody request: Request): ResponseEntity<*> {
        return handle("bookDays") {
            val serviceRequest = TimesheetDayBookingService.Request(
                userId = request.userId,
                taskId = request.taskId,
                kost2Id = request.kost2Id,
                location = request.location,
                description = request.description,
                startDate = request.startDate,
                endDate = request.endDate,
                startTime = parseTime(request.startTime),
                hoursPerDay = request.hoursPerDay,
                halfDayBegin = request.halfDayBegin,
                halfDayEnd = request.halfDayEnd,
            )
            val days = if (request.dryRun) {
                dayBookingService.planDays(serviceRequest)
            } else {
                dayBookingService.book(serviceRequest)
            }
            Result(
                days.map { Day(it.date, it.status, hoursOf(it.minutes), it.note, it.sharedOverlap) },
                days.count { it.booking },
            )
        }
    }

    private fun hoursOf(minutes: Int): BigDecimal {
        return BigDecimal(minutes).divide(SIXTY, 2, RoundingMode.HALF_UP).stripTrailingZeros()
    }

    private fun parseTime(time: String?): LocalTime? {
        if (time.isNullOrBlank()) return null
        return try {
            LocalTime.parse(time)
        } catch (_: DateTimeParseException) {
            throw UserException("timesheet.bookDays.error.startTime")
        }
    }

    private fun handle(action: String, block: () -> Any): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(block())
        } catch (ex: UserException) {
            // Also an AccessException: a refusal the user is told about, not an error of the system.
            log.info { "$action: message='${ex.i18nKey}'" }
            val error = ValidationError(translateMsg(ex), messageId = ex.i18nKey)
            ResponseEntity(ResponseAction(validationErrors = listOf(error)), HttpStatus.NOT_ACCEPTABLE)
        }
    }

    companion object {
        private val SIXTY = BigDecimal(60)
    }
}
