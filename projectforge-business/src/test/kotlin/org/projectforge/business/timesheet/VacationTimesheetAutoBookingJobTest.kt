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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.task.TaskDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.business.vacation.service.VacationValidator
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationLegacyProperties
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.ConfigurationType
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/**
 * The nightly job books approved vacation days without time sheets afterwards, each day only once (a deleted
 * vacation time sheet is not booked again), never today or in advance, and at most the configured days back.
 *
 * November 2033: Tue 08. until Thu 10. are the days before the run on Fri 11.
 */
class VacationTimesheetAutoBookingJobTest : AbstractTestBase() {
    @Autowired
    private lateinit var job: VacationTimesheetAutoBookingJob

    @Autowired
    private lateinit var vacationBookingConfig: TimesheetVacationBookingConfig

    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var configurationLegacyProperties: ConfigurationLegacyProperties

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    override fun beforeAll() {
        VacationValidator.rejectNewVacationEntriesBeforeNow = false
        logon(TEST_FINANCE_USER)
        configurationDao.checkAndUpdateDatabaseEntries()
        val costConfigured = configurationDao.getEntry(ConfigurationParam.COST_CONFIGURED)!!
        costConfigured.booleanValue = true
        configurationDao.update(costConfigured, checkAccess = false)
        val kunde = KundeDO().also {
            it.name = "Vacation-Auto-Kunde"
            it.id = 78
            kundeDao.insert(it)
        }
        val projekt = initTestDB.addProjekt(kunde, 78, "Vacation-Auto-Projekt", 0)
        initTestDB.addProjekt(kunde, 79, "Vacation-Auto-Other-Projekt", 0) // Not linked to the vacation task.
        vacationTask = initTestDB.addTask("vacation-auto-task", "root")
        projektDao.setTask(projekt, vacationTask.id)
        projektDao.update(projekt)
        setString(ConfigurationParam.TIMESHEET_VACATION_KOST2, "5.078.78.00")
        setLong(ConfigurationParam.TIMESHEET_VACATION_TASK_ID, vacationTask.id)
    }

    override fun afterAll() {
        VacationValidator.rejectNewVacationEntriesBeforeNow = true // Reset to normal value.
        setLong(ConfigurationParam.TIMESHEET_VACATION_AUTO_BOOKING_DAYS, null)
    }

    @BeforeEach
    fun setUp() {
        logon(TEST_FINANCE_USER)
        setLong(ConfigurationParam.TIMESHEET_VACATION_AUTO_BOOKING_DAYS, 3)
    }

    @Test
    fun booksPastVacationDaysOnceOnly() {
        val (user, employee) = createVacationer("vacation-auto-user")
        val (_, other) = createVacationer("vacation-auto-other")
        val manager = createVacationer("vacation-auto-manager").second
        insertSheet(user, vacationTask, day(9), 8, 10) // Some time sheet on Wed: the day isn't booked.
        insertVacation(employee, manager, day(1), day(9))
        insertVacation(employee, manager, day(10), day(11), halfDayBegin = true)
        insertVacation(other, manager, day(8), day(10), status = VacationStatus.IN_PROGRESS)

        job.bookVacations(today = day(11), lastSuccessDay = null)
        // Not before Tue 08. (3 days back), not Wed 09. (booked already), half of Thu 10., not today (Fri 11.).
        Assertions.assertEquals(
            mapOf(day(8) to 480L, day(9) to 120L, day(10) to 240L),
            minutesByDay(user),
        )
        sheetsOf(user).filter { it.taskId == vacationTask.id }.forEach {
            Assertions.assertEquals("5.078.78.00", it.kost2?.formattedNumber)
        }
        Assertions.assertTrue(sheetsOf(other.user!!).isEmpty(), "A vacation not approved isn't booked.")

        // The user deletes a booked vacation day: a further run doesn't book it again.
        val deleted = sheetsOf(user).first { localDateOf(it) == day(8) }
        timesheetDao.markAsDeleted(deleted, checkAccess = false)
        // A second run on the same day (by hand) after the successful one:
        Assertions.assertEquals(0, job.bookVacations(today = day(11), lastSuccessDay = day(11)).bookedDays)
        Assertions.assertEquals(2, minutesByDay(user).size)
        // Mon 14., the last successful run on Fri 11.: only Fri 11. (the weekend has no working days).
        job.bookVacations(today = day(14), lastSuccessDay = day(11))
        Assertions.assertEquals(
            mapOf(day(9) to 120L, day(10) to 240L, day(11) to 480L),
            minutesByDay(user),
        )
    }

    @Test
    fun booksAtMostTheConfiguredDaysBack() {
        val (user, employee) = createVacationer("vacation-auto-stale")
        val manager = createVacationer("vacation-auto-stale-manager").second
        insertVacation(employee, manager, LocalDate.of(2033, 10, 3), day(11))
        // The last successful run long ago (the job didn't run for weeks): still only 3 days back.
        job.bookVacations(today = day(11), lastSuccessDay = LocalDate.of(2033, 10, 1))
        Assertions.assertEquals(setOf(day(8), day(9), day(10)), minutesByDay(user).keys)
    }

    @Test
    fun booksTheWeeklyHoursValidOnTheVacationDay() {
        val (user, employee) = createVacationer("vacation-auto-hours")
        val manager = createVacationer("vacation-auto-hours-manager").second
        employeeService.insertWeeklyHours(employee, LocalDate.of(2020, 1, 1), BigDecimal(40), checkAccess = false)
        // Part time from Thu 10. on, before the job runs on Fri 11.:
        employeeService.insertWeeklyHours(employee, day(10), BigDecimal(20), checkAccess = false)
        insertVacation(employee, manager, day(8), day(10))
        job.bookVacations(today = day(11), lastSuccessDay = null)
        Assertions.assertEquals(
            mapOf(day(8) to 480L, day(9) to 480L, day(10) to 240L),
            minutesByDay(user),
        )
    }

    @Test
    fun disabledBooksNothing() {
        val (user, employee) = createVacationer("vacation-auto-disabled")
        val manager = createVacationer("vacation-auto-disabled-manager").second
        insertVacation(employee, manager, day(8), day(10))
        setLong(ConfigurationParam.TIMESHEET_VACATION_AUTO_BOOKING_DAYS, 0)
        job.bookVacations(today = day(11), lastSuccessDay = null)
        Assertions.assertTrue(sheetsOf(user).isEmpty())
    }

    @Test
    fun propertiesAreTakenOverOnCreatingTheParameterOnly() {
        // On creating the row: taken over (an invalid task id not).
        ConfigurationDO().also {
            it.configurationType = ConfigurationType.LONG
            Assertions.assertTrue(configurationDao.takeOverLegacyProperty(it, ConfigurationParam.TIMESHEET_VACATION_TASK_ID, " 4711 "))
            Assertions.assertEquals(4711L, it.longValue)
            Assertions.assertFalse(configurationDao.takeOverLegacyProperty(it, ConfigurationParam.TIMESHEET_VACATION_TASK_ID, "no-id"))
        }
        ConfigurationDO().also {
            it.configurationType = ConfigurationType.STRING
            Assertions.assertTrue(configurationDao.takeOverLegacyProperty(it, ConfigurationParam.TIMESHEET_VACATION_KOST2, "1.111.11.11"))
            Assertions.assertEquals("1.111.11.11", it.stringValue)
            Assertions.assertFalse(configurationDao.takeOverLegacyProperty(it, ConfigurationParam.TIMESHEET_VACATION_KOST2, " "))
        }
        // The row exists: the properties are ignored, also if the parameter is cleared.
        ReflectionTestUtils.setField(vacationBookingConfig, "taskIdProperty", vacationTask.id.toString())
        ReflectionTestUtils.setField(vacationBookingConfig, "kost2Property", "5.078.78.00")
        try {
            Assertions.assertEquals(vacationTask.id, vacationBookingConfig.resolve()?.task?.id)
            setLong(ConfigurationParam.TIMESHEET_VACATION_TASK_ID, null)
            setString(ConfigurationParam.TIMESHEET_VACATION_KOST2, null)
            Assertions.assertNull(vacationBookingConfig.resolve())
            configurationLegacyProperties.takeOverAndCheck()
            Assertions.assertNull(configurationDao.getEntry(ConfigurationParam.TIMESHEET_VACATION_TASK_ID)!!.longValue)
            Assertions.assertNull(configurationDao.getEntry(ConfigurationParam.TIMESHEET_VACATION_KOST2)!!.stringValue)
        } finally {
            ReflectionTestUtils.setField(vacationBookingConfig, "taskIdProperty", "")
            ReflectionTestUtils.setField(vacationBookingConfig, "kost2Property", "")
            setLong(ConfigurationParam.TIMESHEET_VACATION_TASK_ID, vacationTask.id)
            setString(ConfigurationParam.TIMESHEET_VACATION_KOST2, "5.078.78.00")
        }
    }

    @Test
    fun checksTaskAndCostUnit() {
        val taskId = vacationTask.id.toString()
        Assertions.assertNotNull(vacationBookingConfig.check(taskId, "5.078.78.00").booking)
        vacationBookingConfig.check("999999999", "5.078.78.00").let {
            Assertions.assertEquals(TimesheetVacationBookingConfig.Problem.TASK_NOT_FOUND, it.problem)
            Assertions.assertNotNull(it.kost2, "The cost unit is looked up on its own.")
        }
        Assertions.assertEquals(
            TimesheetVacationBookingConfig.Problem.KOST2_NOT_FOUND,
            vacationBookingConfig.check(taskId, "9.999.99.99").problem,
        )
        // A cost unit of another project, not bookable on the vacation task:
        Assertions.assertEquals(
            TimesheetVacationBookingConfig.Problem.KOST2_NOT_BOOKABLE,
            vacationBookingConfig.check(taskId, "5.078.79.00").problem,
        )
    }

    /** A user with an employee, who may book own time sheets on the vacation task. */
    private fun createVacationer(username: String): Pair<PFUserDO, EmployeeDO> {
        val user = initTestDB.addUser(username)
        val group = initTestDB.addGroup("$username-group", username)
        initTestDB.createGroupTaskAccess(group, vacationTask, AccessType.OWN_TIMESHEETS, true, true, true, true)
        val employee = EmployeeDO()
        employee.user = user
        employee.eintrittsDatum = LocalDate.of(2020, 1, 1)
        employeeDao.insert(employee, checkAccess = false)
        employeeService.insertAnnualLeaveDays(employee, employee.eintrittsDatum!!, BigDecimal(30), checkAccess = false)
        return Pair(user, employee)
    }

    private fun insertVacation(
        employee: EmployeeDO, manager: EmployeeDO, from: LocalDate, until: LocalDate,
        status: VacationStatus = VacationStatus.APPROVED, halfDayBegin: Boolean = false,
    ) {
        val vacation = VacationDO()
        vacation.employee = employee
        vacation.manager = manager
        vacation.replacement = manager
        vacation.startDate = from
        vacation.endDate = until
        vacation.halfDayBegin = halfDayBegin
        vacation.special = false
        vacation.status = status
        vacationDao.insert(vacation, checkAccess = false)
    }

    private fun insertSheet(user: PFUserDO, task: TaskDO, day: LocalDate, fromHour: Int, toHour: Int) {
        timesheetDao.insert(TimesheetDO().also {
            it.user = user
            it.task = task
            it.kost2 = kostCache.getKost2("5.078.78.00")
            it.description = "existing"
            it.startTime = utc(day, fromHour)
            it.stopTime = utc(day, toHour)
        }, checkAccess = false)
    }

    /** The minutes of the time sheets of the user per day (one time sheet a day here). */
    private fun minutesByDay(user: PFUserDO): Map<LocalDate, Long> {
        return sheetsOf(user).associate { localDateOf(it) to it.duration / 60_000 }
    }

    private fun sheetsOf(user: PFUserDO): List<TimesheetDO> {
        val filter = TimesheetFilter()
        filter.userId = user.id
        return timesheetDao.internalGetList(filter, checkAccess = false).filter { !it.deleted }
    }

    private fun localDateOf(sheet: TimesheetDO): LocalDate =
        sheet.startTime!!.toInstant().atZone(ZoneId.of("UTC")).toLocalDate()

    private fun setLong(param: ConfigurationParam, value: Long?) {
        val entry = configurationDao.getEntry(param)!!
        entry.longValue = value
        configurationDao.update(entry, checkAccess = false)
    }

    private fun setString(param: ConfigurationParam, value: String?) {
        val entry = configurationDao.getEntry(param)!!
        entry.stringValue = value
        configurationDao.update(entry, checkAccess = false)
    }

    companion object {
        private lateinit var vacationTask: TaskDO

        private fun day(dayOfMonth: Int): LocalDate = LocalDate.of(2033, 11, dayOfMonth)

        private fun utc(day: LocalDate, hour: Int): Date =
            Date.from(LocalDateTime.of(day, LocalTime.of(hour, 0)).atZone(ZoneId.of("UTC")).toInstant())
    }
}
