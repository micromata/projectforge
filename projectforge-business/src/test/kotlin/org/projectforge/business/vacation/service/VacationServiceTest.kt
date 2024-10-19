/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.vacation.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.user.UserDao
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.RemainingLeaveDao
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class VacationServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var remainingLeaveDao: RemainingLeaveDao

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Autowired
    private lateinit var vacationService: VacationService

    /**
     * For access in caller.
     */
    private var lastStoredVacation: VacationDO? = null

    override fun beforeAll() {
        VacationValidator.rejectNewVacationEntriesBeforeNow = false
    }

    override fun afterAll() {
        VacationValidator.rejectNewVacationEntriesBeforeNow = true // Reset to normal value.
    }

    /**
     * Test is based on year 2020 (should also run in 2021...).
     */
    @Test
    fun employeeJoined2018WithoutVacationsYearTest() {
        val employee = createEmployee("2018-joiner-without-vacations", LocalDate.of(2018, Month.MAY, 1))
        val manager = createEmployee("2018-joiner-without-vacations-manager", LocalDate.of(2018, Month.MAY, 1))
        logon(TEST_HR_USER)
        // No vacation in 2019: joined in May, therefore 30/12*8=20 days should remain:
        assertStats(
            employee, 2019,
            vacationDaysInYearFromContract = 30.0
        ) // Full year
        assertStats(
            employee, 2020,
            vacationDaysLeftInYear = 60.0,
            remainingLeaveFromPreviousYear = 30.0
        ) // Employee joined in 2018, carry expected.
        addVacations(employee, manager, 2020, Month.JANUARY, 1, Month.JANUARY, 20, true)
        val stats = assertStats(
            employee, 2020,
            vacationDaysLeftInYear = 60.0,
            remainingLeaveFromPreviousYear = 30.0
        ) // Employee joined in 2018, carry expected.

        assertNumbers(stats, 13.0, stats.specialVacationDaysApproved, "specialVacationDaysInProgress")
    }

    /**
     * Test is based on year 2020 (should also run in 2021...).
     */
    @Test
    fun employeeJoined2018YearTest() {
        val employee = createEmployee(
            "2018-joiner", LocalDate.of(2018, Month.JULY, 1), annualLeaveDays = 20,
            annualLeaveDayEntries = arrayOf(AnnualLeaveDays(2019, 30))
        )
        val manager = createEmployee("2018-joiner-manager", LocalDate.of(2018, Month.MAY, 1))
        logon(TEST_HR_USER)
        Assertions.assertEquals(
            20.0,
            addVacations(employee, manager, 2019, Month.JULY, 1, Month.JULY, 26),
            "days off expected."
        )
        Assertions.assertEquals(
            10.0,
            addVacations(employee, manager, 2020, Month.JULY, 13, Month.JULY, 24),
            "days off expected."
        )
        assertStats(
            employee, 2018,
            vacationDaysAllocatedInYear = 0.0,
            vacationDaysInYearFromContract = 10.0
        ) // Half year (20 days/2)
        assertStats(
            employee, 2019,
            vacationDaysAllocatedInYear = 20.0,
            vacationDaysInYearFromContract = 30.0
        ) // Full year (30 days since 2019)
        assertStats(
            employee, 2020,
            vacationDaysLeftInYear = 30.0, // 30 from contract + 10 carry - 10 used
            vacationDaysAllocatedInYear = 10.0,
            remainingLeaveFromPreviousYear = 10.0
        ) // Employee joined in 2018, carry expected.
        assertStats(
            employee, 2020,
            baseMonth = Month.JUNE,
            vacationDaysLeftInYear = 20.0, // 30 from contract - 10 used
            vacationDaysAllocatedInYear = 10.0,
            remainingLeaveFromPreviousYear = 10.0
        ) // Employee joined in 2018, carry expected.

        suppressErrorLogs {
            try {
                addVacations(employee, manager, 2020, Month.JULY, 20, Month.JULY, 28)
                fail("UserException expected due to collision of vacation entries.")
            } catch (ex: Exception) {
                Assertions.assertTrue(ex is UserException)
                Assertions.assertEquals(VacationValidator.Error.COLLISION.messageKey, ex.message)
            }
            logon(createEmployee("Foreign-user", LocalDate.of(2017, Month.JANUARY, 1)).user!!)
            try {
                addVacations(employee, manager, 2020, Month.JULY, 20, Month.JULY, 28)
                fail("AccessException expected, user has not right.")
            } catch (ex: Exception) {
                Assertions.assertTrue(ex is AccessException)
            }
        }
    }

    /**
     * Test is based on year 2020 (should also run in 2021...).
     */
    @Test
    fun employeeJoined2019WithoutVacationsYearTest() {
        val employee = createEmployee("2019-joiner-without-vacations", LocalDate.of(2019, Month.MAY, 1))
        logon(TEST_HR_USER)
        // No vacation in 2019: joined in May, therefore 30/12*8=20 days should remain:
        assertStats(
            employee, 2019,
            vacationDaysInYearFromContract = 20.0
        ) // 7 months
        assertStats(
            employee, 2020,
            remainingLeaveFromPreviousYear = 20.0,
            vacationDaysLeftInYear = 50.0
        )
    }

    /**
     * Test is based on year 2020 (should also run in 2021...).
     */
    @Test
    fun employeeJoined2019YearTest() {
        val employee = createEmployee("2019-joiner", LocalDate.of(2019, Month.MAY, 1))
        val manager = createEmployee("2019-joiner-manager", LocalDate.of(2018, Month.MAY, 1))
        logon(TEST_HR_USER)
        // 10 days
        Assertions.assertEquals(
            10.0,
            addVacations(employee, manager, 2019, Month.JULY, 1, Month.JULY, 12),
            "days off expected."
        )
        // 24.12.2018 (0.5), 27./30. (2), 31.12.2018 (0.5) -> 3 days, 2.-4.1. -> 3 days, total -> 6 days
        // Not yet allowed: Assertions.assertEquals(6.0, addVacations(employee, manager, 2019, Month.DECEMBER, 24, Month.JANUARY, 6), "days off expected.")
        Assertions.assertEquals(
            3.0,
            addVacations(employee, manager, 2019, Month.DECEMBER, 24, Month.DECEMBER, 31),
            "days off expected."
        )
        Assertions.assertEquals(
            3.0,
            addVacations(employee, manager, 2020, Month.JANUARY, 1, Month.JANUARY, 6),
            "days off expected."
        )

        assertStats(
            employee, 2019,
            vacationDaysInYearFromContract = 20.0, // 7 months
            vacationDaysAllocatedInYear = 13.0
        )

        assertStats(
            employee, 2020,
            remainingLeaveFromPreviousYear = 7.0,
            remainingLeaveFromPreviousYearUnused = 4.0,
            vacationDaysInYearFromContract = 30.0, // Full year
            vacationDaysAllocatedInYear = 3.0,
            vacationDaysLeftInYear = 34.0
        ) // 7 + 30 - 3 (used days)
        assertStats(
            employee, 2020,
            baseMonth = Month.JUNE,
            remainingLeaveFromPreviousYear = 7.0,
            remainingLeaveFromPreviousYearUnused = 4.0,
            vacationDaysAllocatedInYear = 3.0,
            vacationDaysLeftInYear = 30.0
        ) // 4 days lost after overlap period: 7 - 4 + 30 - 3

        Assertions.assertEquals(
            25.0,
            addVacations(employee, manager, 2020, Month.JUNE, 1, Month.JULY, 7),
            "days off expected."
        )
        assertStats(
            employee, 2020,
            remainingLeaveFromPreviousYear = 7.0,
            remainingLeaveFromPreviousYearUnused = 4.0,
            vacationDaysAllocatedInYear = 28.0,
            vacationDaysLeftInYear = 9.0
        ) // 7 + 30 - 28 (used days)
        assertStats(
            employee, 2020,
            baseMonth = Month.JUNE,
            remainingLeaveFromPreviousYear = 7.0,
            remainingLeaveFromPreviousYearUnused = 4.0,
            vacationDaysAllocatedInYear = 28.0,
            vacationDaysLeftInYear = 5.0
        ) // 4 days lost after overlap period: 7 - 4 + 30 - 28

        suppressErrorLogs {
            try {
                // Only 5 days left after 31.03.
                addVacations(employee, manager, 2020, Month.APRIL, 1, Month.APRIL, 10)
                fail("UserException expected due to not enough left vacation days.")
            } catch (ex: Exception) {
                Assertions.assertTrue(ex is UserException)
                Assertions.assertEquals(VacationValidator.Error.NOT_ENOUGH_DAYS_LEFT.messageKey, ex.message)
            }
            try {
                // So, try to put vacation partly into the overlap period: get 2 days from previous year and 6 days from new year (but only 5 are available)
                addVacations(employee, manager, 2020, Month.MARCH, 30, Month.APRIL, 8)
                fail("UserException expected due to not enough left vacation days.")
            } catch (ex: Exception) {
                Assertions.assertTrue(ex is UserException)
                Assertions.assertEquals(VacationValidator.Error.NOT_ENOUGH_DAYS_LEFT.messageKey, ex.message)
            }
        }

        // So, try to put vacation partly into the overlap period: get 3 days from previous year and 5 days from new year, should work:
        Assertions.assertEquals(
            7.0,
            addVacations(employee, manager, 2020, Month.MARCH, 29, Month.APRIL, 7),
            "days off expected."
        )

        // So, try to move this vacation one day later, this should fail:
        lastStoredVacation!!.startDate = lastStoredVacation!!.startDate!!.plusDays(1)
        lastStoredVacation!!.endDate = lastStoredVacation!!.endDate!!.plusDays(1)
        suppressErrorLogs {
            try {
                vacationDao.update(lastStoredVacation!!)
                fail("UserException expected due to not enough left vacation days.")
            } catch (ex: Exception) {
                Assertions.assertTrue(ex is UserException)
                Assertions.assertEquals(VacationValidator.Error.NOT_ENOUGH_DAYS_LEFT.messageKey, ex.message)
            }
        }
    }

    /**
     * Test is based on year 2020 (should also run in 2021...).
     */
    @Test
    fun employeeJoinedThisYearTest() {
        val employee = createEmployee("2020-joiner", LocalDate.of(2020, Month.MAY, 1))
        val manager = createEmployee("2020-joiner-manager", LocalDate.of(2018, Month.MAY, 1))
        logon(TEST_HR_USER)
        assertStats(
            employee, 2020,
            vacationDaysInYearFromContract = 20.0
        )
        // 1.6. is an holiday (Pfingstmontag):
        Assertions.assertEquals(
            7.0,
            addVacations(employee, manager, 2020, Month.JUNE, 1, Month.JUNE, 10),
            "days off expected."
        )
        assertStats(
            employee, 2020,
            vacationDaysInYearFromContract = 20.0,
            vacationDaysAllocatedInYear = 7.0
        )
    }

    @Test
    fun employeeJoinedInFutureTest() {
        val employee = createEmployee("FutureJoiner", LocalDate.now().plusDays(1))
        logon(TEST_HR_USER)
        assertStats(
            employee,
            2020,
            vacationDaysInYearFromContract = -1.0
        ) // Don't check vacationDaysInYearFromContract: it depends on the date of year this test runs.
    }

    @Test
    fun yearlyVacationDaysForJoinersAndLeaversTest() {
        val leaver1 = createEmployee("Leaver1", LocalDate.of(2019, Month.JULY, 14), LocalDate.of(2020, Month.JUNE, 15))
        Assertions.assertEquals(15, vacationService.getAnnualLeaveDays(leaver1, 2019).toInt()) // Jul - Dec (6)
        Assertions.assertEquals(15, vacationService.getAnnualLeaveDays(leaver1, 2020).toInt()) // Jan - Jun (6)

        val leaver2 = createEmployee("Leaver2", LocalDate.of(2019, Month.JULY, 15), LocalDate.of(2020, Month.JUNE, 14))
        Assertions.assertEquals(
            13,
            vacationService.getAnnualLeaveDays(leaver2, 2019).toInt()
        ) // Aug - Dec (5):5/12*30=12.5
        Assertions.assertEquals(13, vacationService.getAnnualLeaveDays(leaver2, 2020).toInt()) // Jan-May: 5/12*30=12.5

        val leaver3 =
            createEmployee("Leaver3", LocalDate.of(2020, Month.FEBRUARY, 14), LocalDate.of(2020, Month.JUNE, 15))
        Assertions.assertEquals(13, vacationService.getAnnualLeaveDays(leaver3, 2020).toInt()) // 5/12*30=12.5

        val leaver4 =
            createEmployee("Leaver4", LocalDate.of(2020, Month.FEBRUARY, 15), LocalDate.of(2020, Month.JUNE, 15))
        Assertions.assertEquals(
            10,
            vacationService.getAnnualLeaveDays(leaver4, 2020).toInt()
        ) // 4/12*30=10 (March - June)

        val leaver5 =
            createEmployee("Leaver5", LocalDate.of(2020, Month.FEBRUARY, 14), LocalDate.of(2020, Month.JUNE, 14))
        Assertions.assertEquals(
            10,
            vacationService.getAnnualLeaveDays(leaver5, 2020).toInt()
        ) // 5/12*30=12.5 (February - May)

        val leaver6 =
            createEmployee("Leaver6", LocalDate.of(2020, Month.JANUARY, 1), LocalDate.of(2020, Month.DECEMBER, 31))
        Assertions.assertEquals(30, vacationService.getAnnualLeaveDays(leaver6, 2020).toInt()) // 12
    }

    @Test
    fun checkHalfDays() {
        val employee = createEmployee("half-day", LocalDate.of(2010, Month.MAY, 1))
        val manager = createEmployee("half-day-manager", LocalDate.of(2018, Month.MAY, 1))
        logon(TEST_HR_USER)
        assertBigDecimal(
            0.5,
            VacationService.getVacationDays(
                LocalDate.of(2019, Month.DECEMBER, 24),
                LocalDate.of(2019, Month.DECEMBER, 24)
            ),
            "days off expected."
        )
        assertBigDecimal(
            1.5,
            VacationService.getVacationDays(
                LocalDate.of(2019, Month.DECEMBER, 24),
                LocalDate.of(2019, Month.DECEMBER, 27)
            ),
            "days off expected."
        )
        assertBigDecimal(
            3.0,
            VacationService.getVacationDays(
                LocalDate.of(2019, Month.DECEMBER, 24),
                LocalDate.of(2019, Month.DECEMBER, 31)
            ),
            "days off expected."
        )
        Assertions.assertEquals(
            1.5,
            addVacations(employee, manager, 2019, Month.DECEMBER, 24, Month.DECEMBER, 27),
            "days off expected."
        )
        Assertions.assertEquals(
            1.0,
            addVacations(
                employee,
                manager,
                2018,
                Month.DECEMBER,
                24,
                Month.DECEMBER,
                27,
                halfDayBegin = true,
                halfDayEnd = true
            ),
            "24.12. is already an half day, so 0.5+0.5 expected."
        )
        Assertions.assertEquals(
            1.0,
            addVacations(
                employee,
                manager,
                2015,
                Month.DECEMBER,
                23,
                Month.DECEMBER,
                24,
                halfDayBegin = true,
                halfDayEnd = true
            ),
            "24.12. is already an half day, so 0.5+0.5 expected."
        )

        Assertions.assertEquals(
            .5,
            addVacations(
                employee,
                manager,
                2020,
                Month.JANUARY,
                20,
                Month.JANUARY,
                20,
                halfDayBegin = true,
                halfDayEnd = true
            ),
            "days off expected."
        )

        Assertions.assertEquals(
            .5,
            addVacations(employee, manager, 2020, Month.JANUARY, 21, Month.JANUARY, 21, halfDayBegin = true),
            "days off expected."
        )
        Assertions.assertEquals(
            .5,
            addVacations(employee, manager, 2020, Month.JANUARY, 22, Month.JANUARY, 22, halfDayEnd = true),
            "days off expected."
        )

        Assertions.assertEquals(
            2.5,
            addVacations(employee, manager, 2020, Month.JANUARY, 27, Month.JANUARY, 29, halfDayEnd = true),
            "days off expected."
        )

        Assertions.assertEquals(
            2.5,
            addVacations(employee, manager, 2020, Month.FEBRUARY, 3, Month.FEBRUARY, 5, halfDayBegin = true),
            "days off expected."
        )
        Assertions.assertEquals(
            3.0,
            addVacations(
                employee,
                manager,
                2020,
                Month.FEBRUARY,
                10,
                Month.FEBRUARY,
                13,
                halfDayBegin = true,
                halfDayEnd = true
            ),
            "days off expected."
        )
    }

    @Test
    fun checkOverYearsLeave() {
        val employee = createEmployee("over-years", LocalDate.of(2010, Month.MAY, 1))
        val manager = createEmployee("over-years-manager", LocalDate.of(2018, Month.MAY, 1))
        logon(TEST_HR_USER)
        Assertions.assertEquals(
            5.0,
            addVacations(employee, manager, 2019, Month.DECEMBER, 24, Month.JANUARY, 5),
            "days off expected."
        )
        assertStats(
            employee, 2020,
            vacationDaysInYearFromContract = 30.0,
            vacationDaysAllocatedInYear = 2.0,
            remainingLeaveFromPreviousYear = 27.0,
            remainingLeaveFromPreviousYearUnused = 25.0,
            vacationDaysLeftInYear = 55.0
        )
        assertStats(
            employee, 2019,
            vacationDaysInYearFromContract = 30.0,
            vacationDaysAllocatedInYear = 3.0
        )

        Assertions.assertEquals(
            23.0,
            addVacations(employee, manager, 2017, Month.JUNE, 1, Month.JULY, 5),
            "days off expected."
        )
        Assertions.assertEquals(
            13.0,
            addVacations(employee, manager, 2017, Month.DECEMBER, 24, Month.JANUARY, 15),
            "days off expected."
        )
        assertStats(
            employee, 2018,
            vacationDaysInYearFromContract = 30.0,
            vacationDaysAllocatedInYear = 10.0,
            remainingLeaveFromPreviousYear = 4.0,
            remainingLeaveFromPreviousYearUnused = 0.0,
            vacationDaysLeftInYear = 24.0,
            // Force calculation for older year through baseDate:
            baseDate = LocalDate.of(2018, Month.JANUARY, 10)
        )

        Assertions.assertEquals(
            25.0,
            addVacations(employee, manager, 2015, Month.JUNE, 1, Month.JULY, 6),
            "days off expected."
        )
        assertStats(
            employee, 2016,
            vacationDaysInYearFromContract = 30.0,
            vacationDaysAllocatedInYear = 0.0,
            remainingLeaveFromPreviousYear = 5.0,
            remainingLeaveFromPreviousYearUnused = 5.0,
            vacationDaysLeftInYear = 35.0,
            // Force calculation for older year through baseDate:
            baseDate = LocalDate.of(2016, Month.JANUARY, 10)
        )
        suppressErrorLogs {
            try {
                addVacations(employee, manager, 2015, Month.DECEMBER, 22, Month.JANUARY, 15)
                fail("Not enough days exception expected.")
            } catch (ex: Exception) {
                Assertions.assertEquals(VacationValidator.Error.NOT_ENOUGH_DAYS_LEFT.messageKey, ex.message)
            }
        }
        Assertions.assertEquals(
            15.0,
            addVacations(employee, manager, 2015, Month.DECEMBER, 23, Month.JANUARY, 15),
            "days off expected."
        )
    }

    /**
     * If endMonth is before startMonth, the next year will be used as endYear.
     * @return Number of vacation days (equals to working days between startDate and endDate)
     */
    private fun addVacations(
        employee: EmployeeDO, manager: EmployeeDO,
        startYear: Int, startMonth: Month, startDay: Int, endMonth: Month, endDay: Int,
        special: Boolean = false, replacement: EmployeeDO = employee,
        status: VacationStatus = VacationStatus.APPROVED,
        halfDayBegin: Boolean = false,
        halfDayEnd: Boolean = false
    ): Double {
        val endYear = if (startMonth > endMonth)
            startYear + 1 // Vacations over years.
        else
            startYear
        return addVacations(
            employee,
            manager,
            LocalDate.of(startYear, startMonth, startDay),
            LocalDate.of(endYear, endMonth, endDay),
            special,
            replacement,
            status,
            halfDayBegin,
            halfDayEnd
        )
    }

    /**
     * Ensures vacation days only after join date of this employee.
     * @return Number of vacation days (equals to working days between startDate and endDate)
     */
    private fun addVacations(
        employee: EmployeeDO, manager: EmployeeDO,
        startDate: LocalDate, endDate: LocalDate,
        special: Boolean = false, replacement: EmployeeDO = employee,
        status: VacationStatus = VacationStatus.APPROVED,
        halfDayBegin: Boolean = false,
        halfDayEnd: Boolean = false
    ): Double {
        if (endDate.isBefore(employee.eintrittsDatum))
            return 0.0
        val vacation = VacationDO()
        vacation.employee = employee
        vacation.startDate = if (startDate.isBefore(employee.eintrittsDatum)) employee.eintrittsDatum else startDate
        vacation.endDate = endDate
        vacation.halfDayBegin = halfDayBegin
        vacation.halfDayEnd = halfDayEnd
        vacation.special = false
        vacation.status = status
        vacation.manager = manager
        vacation.replacement = replacement
        vacation.special = special
        vacationDao.insert(vacation)
        lastStoredVacation = vacation
        return VacationService.getVacationDays(vacation).toDouble()
    }

    private fun createEmployee(
        name: String,
        joinDate: LocalDate?,
        leaveDate: LocalDate? = null,
        annualLeaveDays: Int = 30,
        annualLeaveDayEntries: Array<AnnualLeaveDays>? = null
    ): EmployeeDO {
        val user = PFUserDO()
        user.firstname = name
        user.lastname = name
        user.username = "$name.$name"
        user.email = "$name@devnull.com"
        userDao.insert(user, checkAccess = false)
        val employee = EmployeeDO()
        employee.user = user
        employee.eintrittsDatum = joinDate
        employee.austrittsDatum = leaveDate
        employeeDao.insert(employee, checkAccess = false)
        joinDate?.let { validFrom ->
            employeeService.insertAnnualLeaveDays(employee, validFrom, BigDecimal(annualLeaveDays), checkAccess = false)
        }
        annualLeaveDayEntries?.forEach {
            employeeService.insertAnnualLeaveDays(employee, LocalDate.of(it.year, Month.JUNE, 1), BigDecimal(it.value), checkAccess = false)
        }
        return employee
    }

    private fun assertStats(
        employee: EmployeeDO,
        year: Int,
        remainingLeaveFromPreviousYear: Double = 0.0,
        remainingLeaveFromPreviousYearUnused: Double = remainingLeaveFromPreviousYear,
        vacationDaysInYearFromContract: Double = 30.0,
        vacationDaysAllocatedInYear: Double = 0.0,
        vacationDaysLeftInYear: Double? = null,
        /**
         * Inside overlap time (before end of vacation year 31.03.2020, default) or after.
         */
        baseMonth: Month = Month.JANUARY,
        baseDate: LocalDate? = null
    ): VacationStats {
        val base = baseDate ?: LocalDate.of(2020, baseMonth, 15)
        val stats = vacationService.getVacationStats(employee, year, true, base)
        assertNumbers(
            stats,
            remainingLeaveFromPreviousYear,
            stats.remainingLeaveFromPreviousYear,
            "remainingLeaveFromPreviousYear"
        )
        assertNumbers(
            stats,
            remainingLeaveFromPreviousYearUnused,
            stats.remainingLeaveFromPreviousYearUnused,
            "remainingLeaveFromPreviousYearUnused"
        )
        assertNumbers(
            stats,
            vacationDaysAllocatedInYear,
            stats.vacationDaysInProgressAndApproved,
            "vacationDaysAllocatedInYear"
        )
        if (vacationDaysInYearFromContract >= 0) {
            assertNumbers(
                stats,
                vacationDaysInYearFromContract,
                stats.vacationDaysInYearFromContract,
                "vacationDaysInYearFromContract"
            )
            if (vacationDaysLeftInYear != null)
                assertNumbers(stats, vacationDaysLeftInYear, stats.vacationDaysLeftInYear, "vacationDaysLeftInYear")
            else
                assertNumbers(
                    stats,
                    vacationDaysInYearFromContract - vacationDaysAllocatedInYear,
                    stats.vacationDaysLeftInYear,
                    "vacationDaysLeftInYear"
                )
        }
        if (remainingLeaveFromPreviousYear > 0) {
            assertNumbers(
                stats,
                remainingLeaveFromPreviousYear,
                remainingLeaveDao.get(stats.employee.id, stats.year)?.remainingFromPreviousYear,
                "remainingFromPreviousYear in db: $stats"
            )
        }
        return stats
    }

    private fun assertNumbers(stats: VacationStats, expected: Double?, actual: BigDecimal?, msg: String) {
        if (expected == null) {
            Assertions.assertNull(actual, "$msg: $stats")
        } else {
            Assertions.assertEquals(expected, actual?.toDouble(), "$msg: $stats")
        }
    }

    private fun assertBigDecimal(expected: Double, actual: BigDecimal, msg: String = "") {
        Assertions.assertEquals(expected, actual.toDouble(), msg)
    }

    class AnnualLeaveDays(val year: Int, val value: Int)
}
