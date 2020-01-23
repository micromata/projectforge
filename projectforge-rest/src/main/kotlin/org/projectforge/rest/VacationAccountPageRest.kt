/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.api.EmployeeService
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.repository.LeaveAccountEntryDao
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.business.vacation.service.VacationStatsFormatted
import org.projectforge.common.DateFormatType
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.LeaveAccountEntry
import org.projectforge.rest.dto.Vacation
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Month
import java.time.Year

@RestController
@RequestMapping("${Rest.URL}/vacationAccount")
class VacationAccountPageRest {
    class Data(var employee: Employee? = null)

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var leaveAccountEntryDao: LeaveAccountEntryDao

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var vacationService: VacationService

    @GetMapping("layout")
    fun getLayout(): UILayout {
        val layout = UILayout("vacation.leaveaccount.title")
        val lc = LayoutContext(VacationDO::class.java)
        layout.addTranslations("employee",
                "vacation.leaveaccount.title",
                "vacation.annualleave",
                "vacation.previousyearleave",
                "vacation.subtotal",
                "menu.vacation.leaveAccountEntry",
                "vacation.vacationApproved",
                "vacation.vacationInProgress",
                "vacation.availablevacation",
                "vacation.specialApproved",
                "vacation.specialInProgress",
                "vacation.title.list",
                "vacation.startdate",
                "vacation.enddate",
                "vacation.Days",
                "vacation.replacement",
                "vacation.manager",
                "vacation.status",
                "vacation.vacationmode",
                "vacation.special",
                "comment",
                "vacation.leaveAccountEntry.title.heading",
                "date",
                "vacation.leaveAccountEntry.amount",
                "description"
        )
        val endOfYear = vacationService.getEndOfCarryVacationOfPreviousYear(Year.now().value)
        val endOfYearString = PFDateTimeUtils.ensureUsersDateTimeFormat(DateFormatType.DATE_WITHOUT_YEAR).format(endOfYear)
        layout.addTranslation("vacation.previousyearleaveunused", translateMsg("vacation.previousyearleaveunused", endOfYearString))

        val userPref = getUserPref()
        var employeeId = userPref.employeeId ?: ThreadLocalUserContext.getUserContext().employeeId
        val employee = employeeService.getById(employeeId)
        val statistics = mutableMapOf<String, Any>()
        val currentStats = vacationService.getVacationStats(employee, Year.now().value)
        val prevStats = vacationService.getVacationStats(employee, Year.now().value - 1)
        val vacations = mutableMapOf<String, Any>()
        if (employeeId != null) {
            statistics["statisticsCurrentYear"] = VacationStatsFormatted(currentStats)
            statistics["statisticsPreviousYear"] = VacationStatsFormatted(prevStats)
            readVacations(vacations, "Current", employeeId, currentStats.year)
            readVacations(vacations, "Previous", employeeId, prevStats.year)
            val periodBegin = PFDay.of(prevStats.year, Month.JANUARY, 1).localDate
            val periodEnd = PFDay.of(currentStats.year, Month.JANUARY, 1).endOfYear.localDate
            leaveAccountEntryDao.getList(employeeId, periodBegin, periodEnd)?.let { list ->
                vacations["leaveAccountEntries"] = list.map { LeaveAccountEntry(it) }.sortedByDescending { it.date }
            }
        }
        val buttonCol = UICol(length = 6)
        buttonCol.add(UIButton("add", "add", UIColor.SUCCESS, responseAction = ResponseAction("http://localhost:3000/react/vacation/edit")))
        if (currentStats.remainingLeaveFromPreviousYear != prevStats.vacationDaysLeftInYear) {
            buttonCol.add(UIButton("recalculate", "vacation.recalculateRemainingLeave", UIColor.DANGER,
                    responseAction = ResponseAction(PagesResolver.getDynamicPageUrl(this.javaClass, mapOf<String, Any>("recalculate" to true)))))
        }
        layout.add(UIFieldset(length = 12)
                .add(UIRow()
                        .add(UICol(length = 6)
                                .add(lc, "employee"))
                        .add(UICol(length = 6)
                                .add(UICustomized("vacation.statistics",
                                        values = statistics))))
                .add(UIRow().add(buttonCol))
                .add(UIFieldset(length = 12)
                        .add(UIRow()
                                .add(UICol(length = 12)
                                        .add(UICustomized("vacation.entries",
                                                values = vacations))))))
        return layout
    }

    private fun readVacations(variables: MutableMap<String, Any>, id: String, employeeId: Int, year: Int) {
        val yearDate = PFDay.of(year, Month.JANUARY, 1)
        val dbList = vacationService.getVacationsListForPeriod(employeeId, yearDate.localDate, yearDate.endOfYear.localDate, true, true)
        val list = dbList.map { Vacation(it) }
        variables["vacations${id}Year"] = list
        variables["year$id"] = year
    }

    class VacationAccountUserPref(var employeeId: Int? = null)

    private fun getUserPref(): VacationAccountUserPref {
        return userPrefService.ensureEntry("vacation", "account", VacationAccountUserPref())
    }
}
