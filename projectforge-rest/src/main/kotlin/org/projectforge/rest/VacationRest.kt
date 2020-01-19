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

import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.repository.VacationDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Vacation
import org.projectforge.ui.Formatter
import org.projectforge.ui.LayoutUtils
import org.projectforge.ui.UILayout
import org.projectforge.ui.UITable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/vacation")
class VacationRest : AbstractDTORest<VacationDO, Vacation, VacationDao>(VacationDao::class.java, "vacation.title") {

    override fun transformForDB(dto: Vacation): VacationDO {
        val vacationDO = VacationDO()
        dto.copyFrom(vacationDO)
        return vacationDO
    }

    override fun transformFromDB(obj: VacationDO, editMode: Boolean): Vacation {
        val vacation = Vacation()
        vacation.copyFrom(obj)
        return vacation
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "employee", "startDate", "endDate", "vacationmode", "status", "workingDays",
                                "specialLeave", "manager", "substitution"))
        layout.getTableColumnById("employee").formatter = Formatter.EMPLOYEE
        layout.getTableColumnById("startDate").formatter = Formatter.DATE
        layout.getTableColumnById("endDate").formatter = Formatter.DATE
        layout.getTableColumnById("manager").formatter = Formatter.USER
        layout.getTableColumnById("substitution").formatter = Formatter.USER
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Vacation, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(lc, "TODO")
        return LayoutUtils.processEditPage(layout, dto, this)
    }


}
