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


package org.projectforge.business.testdata

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.fibu.EmployeeStatus
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * Employee records of the users and their vacations around today.
 */
@Service
open class EmployeeTestDataContributor : TestDataContributor {
    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var vacationDao: VacationDao

    override val order = 30

    private class Vacation(val user: String, val from: LocalDate, val to: LocalDate, val status: VacationStatus)

    override fun seed(ctx: TestDataContext) {
        val since = LocalDate.of(2019, 4, 1)
        UserTestDataContributor.EMPLOYEES.forEach { (username, position) ->
            val employee = EmployeeDO().also {
                it.user = ctx.user(username)
                it.position = position
                it.eintrittsDatum = since
                it.abteilung = "Delivery"
            }
            employeeDao.insert(employee, checkAccess = false)
            employeeService.insertStatus(employee, since, EmployeeStatus.FEST_ANGESTELLTER, checkAccess = false)
            employeeService.insertAnnualLeaveDays(employee, since, BigDecimal(30), checkAccess = false)
            employeeService.insertWeeklyHours(employee, since, BigDecimal(40), checkAccess = false)
            ctx.employees[username] = employee
        }
        // Vacations in the past need the HR right, Laura has it.
        val origUser = ThreadLocalUserContext.loggedInUser
        ThreadLocalUserContext.setUser(ctx.user("laura"))
        try {
            insertVacations(ctx)
        } finally {
            ThreadLocalUserContext.setUser(origUser)
        }
    }

    private fun insertVacations(ctx: TestDataContext) {
        listOf(
            Vacation("ann", ctx.week(2026, 9, 28), ctx.week(2026, 10, 2), VacationStatus.APPROVED),
            Vacation("alex", ctx.week(2026, 10, 19), ctx.week(2026, 10, 23), VacationStatus.APPROVED),
            Vacation("chris", ctx.week(2026, 10, 15), ctx.week(2026, 10, 16), VacationStatus.IN_PROGRESS),
            Vacation("julia", ctx.week(2026, 11, 2), ctx.week(2026, 11, 13), VacationStatus.APPROVED),
            Vacation("max", ctx.week(2026, 8, 10), ctx.week(2026, 8, 21), VacationStatus.APPROVED),
            Vacation("laura", ctx.week(2026, 12, 21), ctx.week(2026, 12, 30), VacationStatus.APPROVED),
            Vacation("mona", ctx.week(2026, 10, 26), ctx.week(2026, 10, 30), VacationStatus.APPROVED),
            Vacation("joe", ctx.week(2026, 7, 6), ctx.week(2026, 7, 24), VacationStatus.APPROVED),
            Vacation("michael", ctx.week(2026, 6, 1), ctx.week(2026, 6, 12), VacationStatus.APPROVED),
        ).forEach { v ->
            val vacation = VacationDO().also {
                it.employee = ctx.employee(v.user)
                it.startDate = v.from
                it.endDate = v.to
                it.status = v.status
                // Nobody approves the own vacation.
                it.manager = ctx.employee(if (v.user == "laura") "joe" else "laura")
                it.replacement = ctx.employee(if (v.user == "max") "mona" else "max")
                it.special = false
            }
            try {
                vacationDao.insert(vacation, checkAccess = false)
                if (v.status == VacationStatus.APPROVED) {
                    ctx.absences.add(Triple(v.user, v.from, v.to))
                }
            } catch (ex: Exception) {
                // The validation refuses a vacation before anything is written (e.g. not enough leave days left).
                log.warn { "Test data: vacation of '${v.user}' refused: ${ex.message}" }
            }
        }
    }
}
