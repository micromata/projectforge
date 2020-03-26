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

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.api.EmployeeService
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.business.vacation.model.*
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.business.vacation.service.VacationStats
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Vacation
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterBooleanElement
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Year
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/vacation")
class VacationPagesRest : AbstractDTOPagesRest<VacationDO, Vacation, VacationDao>(VacationDao::class.java, "vacation.title") {

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var vacationDao: VacationDao

    @Autowired
    private lateinit var vacationService: VacationService

    override fun transformForDB(dto: Vacation): VacationDO {
        val vacationDO = VacationDO()
        dto.copyTo(vacationDO)
        return vacationDO
    }

    override fun transformFromDB(obj: VacationDO, editMode: Boolean): Vacation {
        val vacation = Vacation()
        vacation.copyFrom(obj)
        return vacation
    }

    override fun newBaseDTO(request: HttpServletRequest?): Vacation {
        val vacation = getUserPref()
        val result = Vacation()
        var employeeDO: EmployeeDO? = null
        if (vacationDao.hasHrRights(ThreadLocalUserContext.getUser())) {
            val employeeId = NumberHelper.parseInteger(request?.getParameter("employee"))
            if (employeeId != null) {
                employeeDO = employeeService.getById(employeeId)
            } else {
                employeeDO = vacation.employee
            }
        }
        if (employeeDO == null) {
            // For non HR staff members, choose always the logged in employee:
            employeeDO = employeeService.getEmployeeByUserId(ThreadLocalUserContext.getUserId())
        }
        result.employee = createEmployee(employeeDO!!)
        vacation.manager?.let { result.manager = createEmployee(it) }
        vacation.replacement?.let { result.replacement = createEmployee(it) }
        result.status = VacationStatus.IN_PROGRESS
        return result
    }

    private fun createEmployee(employeeDO: EmployeeDO?): Employee? {
        employeeDO?.id ?: return null
        val employee = Employee()
        employeeDO.id?.let {
            employee.copyFromMinimal(employeeDao.internalGetById(it))
        }
        return employee
    }

    override fun afterSave(obj: VacationDO, postData: PostData<Vacation>): ResponseAction {
        // Save current edited object as user preference for pre-filling the edit form for the next usage.
        val vacation = getUserPref()
        vacation.employee = obj.employee
        vacation.manager = obj.manager
        vacation.replacement = obj.replacement
        return super.afterSave(obj, postData)
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "employee", "startDate", "endDate", "vacationModeString", "statusString", "workingDaysFormatted",
                                "special", "replacement", "manager", "comment"))
        layout.getTableColumnById("employee").formatter = Formatter.EMPLOYEE
        layout.getTableColumnById("startDate").formatter = Formatter.DATE
        layout.getTableColumnById("endDate").formatter = Formatter.DATE
        layout.getTableColumnById("vacationModeString").title = "vacation.vacationmode"
        layout.getTableColumnById("statusString").title = "vacation.status"
        layout.getTableColumnById("replacement").formatter = Formatter.USER
        layout.getTableColumnById("manager").formatter = Formatter.USER
        layout.getTableColumnById("workingDaysFormatted").title = "vacation.Days"
        return LayoutUtils.processListPage(layout, this)
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(UIFilterListElement("status", label = translate("vacation.status"), defaultFilter = true)
                .buildValues(VacationStatus::class.java))
        elements.add(UIFilterListElement("assignment", label = translate("vacation.vacationmode"), defaultFilter = true)
                .buildValues(VacationMode.OWN, VacationMode.REPLACEMENT, VacationMode.MANAGER, VacationMode.OTHER))
        elements.add(UIFilterElement("year", label = translate("calendar.year"), defaultFilter = true))
        elements.add(UIFilterBooleanElement("special", label = translate("vacation.special")))
        elements.add(UIFilterElement("period", label = translate("timePeriod"), filterType = UIFilterElement.FilterType.DATE))
    }

    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<VacationDO>>? {
        val filters = mutableListOf<CustomResultFilter<VacationDO>>()
        val assignmentFilterEntry = source.entries.find { it.field == "assignment" }
        if (assignmentFilterEntry != null) {
            assignmentFilterEntry.synthetic = true
            val values = assignmentFilterEntry.value.values
            if (!values.isNullOrEmpty()) {
                val enums = values.map { VacationMode.valueOf(it) }
                filters.add(VacationModeFilter(enums))
            }
        }
        val yearFilterEntry = source.entries.find { it.field == "year" }
        if (yearFilterEntry != null) {
            yearFilterEntry.synthetic = true
            val value = yearFilterEntry.value.value
            val year = NumberHelper.parseInteger(value)
            if (year != null) {
                filters.add(VacationYearFilter(year))
            }
        }
        return filters
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Vacation, userAccess: UILayout.UserAccess): UILayout {
        val employeeCol = UICol(6)
        if (vacationDao.hasHrRights(ThreadLocalUserContext.getUser())) {
            employeeCol.add(lc, "employee")
        } else {
            employeeCol.add(UIReadOnlyField("employee.displayName", label = "vacation.employee"))
        }
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(employeeCol)
                        .add(UICol(3)
                                .add(UIReadOnlyField("workingDaysFormatted", lc, UIDataType.STRING, "vacation.workingdays")))
                        .add(UICol(3)
                                .add(UIReadOnlyField("vacationDaysLeftInYearString", lc, UIDataType.STRING, "vacation.availabledays"))))
                .add(UIRow()
                        .add(UICol(6)
                                .add(lc, "startDate"))
                        .add(UICol(6)
                                .add(lc, "halfDayBegin")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(lc, "endDate"))
                        .add(UICol(6)
                                .add(lc, "halfDayEnd")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(lc, "replacement"))
                        .add(UICol(6)
                                .add(lc, "special")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(lc, "manager"))
                        .add(UICol(6)
                                .add(lc, "status")))
                .add(lc, "comment")

        layout.watchFields.addAll(arrayOf("startDate", "endDate", "halfDayBegin", "halfDayEnd"))
        updateStats(dto)
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun onWatchFieldsUpdate(request: HttpServletRequest, dto: Vacation, watchFieldsTriggered: Array<String>?): ResponseAction {
        var startDate = dto.startDate
        var endDate = dto.endDate
        if (watchFieldsTriggered?.contains("startDate") == true && startDate != null) {
            if (endDate == null || endDate.isBefore(startDate)) {
                dto.endDate = startDate
            }
        } else if (watchFieldsTriggered?.contains("endDate") == true && endDate != null) {
            if (startDate == null || endDate.isBefore(startDate)) {
                dto.startDate = dto.endDate
            }
        }
        updateStats(dto)
        return ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto)
    }

    override fun createReturnToCallerResponseAction(returnToCaller: String): ResponseAction {
        if (returnToCaller == "account") {
            return ResponseAction(PagesResolver.getDynamicPageUrl(VacationAccountPageRest::class.java, absolute = true))
        }
        return super.createReturnToCallerResponseAction(returnToCaller)
    }

    private fun updateStats(dto: Vacation) {
        dto.employee?.let { employee ->
            val employeeDO = EmployeeDO()
            employeeDO.id = employee.id
            val vacationStats = vacationService.getVacationStats(employeeDO, dto.startDate?.year ?: Year.now().value)
            dto.vacationDaysLeftInYear = vacationStats.vacationDaysLeftInYear
            dto.vacationDaysLeftInYearString = VacationStats.format(vacationStats.vacationDaysLeftInYear)
        }
        val startDate = dto.startDate
        val endDate = dto.endDate
        if (startDate != null && endDate != null) {
            dto.workingDays = VacationService.getVacationDays(startDate, endDate, dto.halfDayBegin, dto.halfDayEnd)
            dto.workingDaysFormatted = VacationStats.format(dto.workingDays)
        }
    }

    private fun getUserPref(): VacationDO {
        val vacation = userPrefService.ensureEntry("vacation", "newEntry", VacationDO())
        return vacation
    }
}
