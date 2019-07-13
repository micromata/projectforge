/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Employee
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/employee")
class EmployeeRest : AbstractDTORest<EmployeeDO, Employee, EmployeeDao>(EmployeeDao::class.java, "fibu.employee.title") {
    override fun transformFromDB(obj: EmployeeDO, editMode: Boolean): Employee {
        val employee = Employee()
        employee.copyFrom(obj)
        return employee
    }

    override fun transformForDB(dto: Employee): EmployeeDO {
        val employeeDO = EmployeeDO()
        dto.copyTo(employeeDO)
        return employeeDO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "user", "status", "staffNumber")
                        .add(UITableColumn("kost1", "fibu.kost1", formatter = Formatter.COST1))
                        .add(lc, "position", "abteilung", "eintrittsDatum", "austrittsDatum", "comment"))
        layout.getTableColumnById("user").formatter = Formatter.USER
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Employee): UILayout {
        val layout = super.createEditLayout(dto)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "user", "kost1", "abteilung", "position"))
                        .add(UICol()
                                .add(lc, "staffNumber", "weeklyWorkingHours", "urlaubstage", "", "",
                                        "eintrittsDatum", "austrittsDatum")))
                .add(UIRow()
                        .add(UICol().add(lc, "street", "zipCode", "city"))
                        .add(UICol().add(lc, "country", "state"))
                        .add(UICol().add(lc, "birthday", "gender"))
                        .add(UICol().add(lc, "accountHolder", "iban", "bic")))
                .add(UIRow()
                        .add(UICol().add(lc, "status")))
                .add(UIRow()
                        .add(UICol().add(lc, "comment")))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
