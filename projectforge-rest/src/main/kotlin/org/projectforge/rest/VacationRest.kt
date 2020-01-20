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
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Vacation
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/vacation")
class VacationRest : AbstractDTORest<VacationDO, Vacation, VacationDao>(VacationDao::class.java, "vacation.title") {

    @Autowired
    private lateinit var employeeService: EmployeeService

    @Autowired
    private lateinit var userPrefService: UserPrefService

    override fun transformForDB(dto: Vacation): VacationDO {
        val vacationDO = VacationDO()
        dto.copyFrom(vacationDO)
        return vacationDO
    }

    override fun transformFromDB(obj: VacationDO, editMode: Boolean): Vacation {
        val vacation = Vacation()
        vacation.copyFrom(obj)
        vacation.status = VacationStatus.IN_PROGRESS
        return vacation
    }

    override fun newBaseDO(request: HttpServletRequest?): VacationDO {
        val vacation = getUserPref()
        val result = VacationDO()
        result.employee = vacation.employee
        result.manager = vacation.manager
        result.replacement = vacation.replacement
        return vacation
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

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Vacation, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol(6)
                                .add(lc, "employee")))
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
                                .add(lc, "status")))
                .add(UIRow()
                        .add(UICol(6)
                                .add(lc, "manager"))
                        .add(UICol(6)
                                .add(lc, "special")))
                .add(lc, "comment")

        layout.watchFields.add("startDate")
        layout.watchFields.add("endDate")
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
        return ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto)
    }

    private fun getUserPref(): VacationDO {
        val vacation = userPrefService.ensureEntry("vacation", "newEntry", VacationDO())
        if (vacation.employee == null) {
            vacation.employee = employeeService.getEmployeeByUserId(ThreadLocalUserContext.getUserId())
        }
        return vacation
    }
}
