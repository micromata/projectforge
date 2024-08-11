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

package org.projectforge.rest.fibu

import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.EmployeeSalaryDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.EmployeeSalary
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/employeeSalary")
class EmployeeSalaryPagesRest
    : AbstractDTOPagesRest<EmployeeSalaryDO, EmployeeSalary, EmployeeSalaryDao>(
        EmployeeSalaryDao::class.java,
        "fibu.employee.salary.title") {

    override fun transformFromDB(obj: EmployeeSalaryDO, editMode: Boolean): EmployeeSalary {
        val employeeSalary = EmployeeSalary()
        employeeSalary.copyFrom(obj)
        return employeeSalary
    }

    override fun transformForDB(dto: EmployeeSalary): EmployeeSalaryDO {
        val employeeSalaryDO = EmployeeSalaryDO()
        dto.copyTo(employeeSalaryDO)
        return employeeSalaryDO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(request: HttpServletRequest, layout: UILayout, magicFilter: MagicFilter, userAccess: UILayout.UserAccess) {
      layout.add(UITable.createUIResultSetTable()
                        .add(lc, "month")
                        .add(UITableColumn("fibu.employee.user.lastname", "name"))
                        .add(UITableColumn("fibu.employee.user.firstname", "firstName"))
                        .add(UITableColumn("fibu.employee.staffNumber", "fibu.employee.staffNumber"))
                        .add(lc, "type", "bruttoMitAgAnteil", "comment"))
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: EmployeeSalary, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "fibu.employee.user.lastname", "fibu.employee.user.firstname")
                                .add(UILabel("TODO: Implement selector for year/month"))
                                .add(lc, "type", "bruttoMitAgAnteil", "comment")))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
