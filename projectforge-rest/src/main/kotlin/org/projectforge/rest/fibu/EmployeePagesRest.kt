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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.EmployeeActiveFilter
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.Kost1
import org.projectforge.rest.dto.User
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterBooleanElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/employee")
class EmployeePagesRest :
    AbstractDTOPagesRest<EmployeeDO, Employee, EmployeeDao>(EmployeeDao::class.java, "fibu.employee.title") {
    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    override fun transformFromDB(obj: EmployeeDO, editMode: Boolean): Employee {
        val employee = Employee()
        employee.copyFrom(obj)
        userGroupCache.getUser(obj.userId)?.let { userDO ->
            User(userDO).let { user ->
                user.firstname = userDO.firstname
                user.lastname = userDO.lastname
                employee.user = user
            }
        }
        kostCache.getKost1(obj.kost1Id)?.let { kost ->
            employee.kost1 = Kost1(kost)
        }
        employee.status = obj.status
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
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        val agGrid = agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            userAccess = userAccess,
        )
            // Name	Vorname	Status	Personalnummer	Kost1	Position	Team	Eintrittsdatum	Austrittsdatum	Bemerkung
            .add(
                lc,
                "user.firstname",
                "user.lastname",
                "status",
                "staffNumber",
                "kost1",
                "position",
                "abteilung",
                "eintrittsDatum",
                "austrittsDatum",
                "comment"
            )

        /*        layout.add(UITable.createUIResultSetTable()
                                .add(lc, "user.lastname", "user.firstname", "status", "staffNumber", "kost1")
                                .add(lc, "position", "abteilung", "eintrittsDatum", "austrittsDatum", "comment"))
                layout.getTableColumnById("eintrittsDatum").formatter = UITableColumn.Formatter.DATE
                layout.getTableColumnById("austrittsDatum").formatter = UITableColumn.Formatter.DATE
                // layout.getTableColumnById("user").formatter = UITableColumn.Formatter.USER
                layout.getTableColumnById("kost1").formatter = UITableColumn.Formatter.COST1*/
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        /*elements.add(
            UIFilterListElement("status", label = translate("fibu.employee.status"), defaultFilter = true)
                .buildValues(EmployeeStatus::class.java)
        )*/
        elements.add(
            UIFilterBooleanElement(
                "onlyActives",
                label = translate("label.onlyActiveEntries"),
                defaultFilter = true
            )
        )
    }

    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<EmployeeDO>> {
        val filters = mutableListOf<CustomResultFilter<EmployeeDO>>()
        /*val assignmentFilterEntry = source.entries.find { it.field == "status" }
        if (assignmentFilterEntry != null) {
            assignmentFilterEntry.synthetic = true
            val values = assignmentFilterEntry.value.values
            if (!values.isNullOrEmpty()) {
                val enums = values.map { EmployeeStatus.safeValueOf(it) }
                filters.add(EmployeeStatusFilter(enums))
            }
        }*/
        source.entries.find { it.field == "onlyActives" }?.let { entry ->
            entry.synthetic = true
            val conflictsOnly = entry.value.value
            if (conflictsOnly == "true") {
                filters.add(EmployeeActiveFilter())
            }
        }
        return filters
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Employee, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(lc, "user", "kost1", "abteilung", "position")
                    )
                    .add(
                        UICol()
                            .add(
                                lc, "staffNumber", "weeklyWorkingHours", "eintrittsDatum", "austrittsDatum"
                            )
                    )
            )
            .add(
                UIRow()
                    .add(UICol().add(lc, "statusAttr", "annualLeaveAttr"))
            )
            .add(
                UIRow()
                    .add(UICol().add(lc, "comment"))
            )
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override val autoCompleteSearchFields = arrayOf("user.username", "user.firstname", "user.lastname", "user.email")

    override fun queryAutocompleteObjects(
        request: HttpServletRequest,
        filter: BaseSearchFilter
    ): MutableList<EmployeeDO> {
        return baseDao.selectWithActiveStatus(filter, checkAccess = true, showOnlyActiveEntries = true, showRecentlyLeavers = true).toMutableList()
    }
}
