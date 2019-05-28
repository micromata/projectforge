package org.projectforge.rest.fibu

import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.EmployeeSalaryDao
import org.projectforge.business.fibu.EmployeeSalaryFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.EmployeeSalary
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/employeeSalary")
class EmployeeSalaryRest() : AbstractDTORest<EmployeeSalaryDO, EmployeeSalary, EmployeeSalaryDao, EmployeeSalaryFilter>(EmployeeSalaryDao::class.java, EmployeeSalaryFilter::class.java, "fibu.employee.salary.title") {
    override fun transformDO(obj: EmployeeSalaryDO, editMode : Boolean): EmployeeSalary {
        val employeeSalary = EmployeeSalary()
        employeeSalary.copyFrom(obj)
        return employeeSalary
    }

    override fun transformDTO(dto: EmployeeSalary): EmployeeSalaryDO {
        val employeeSalaryDO = EmployeeSalaryDO()
        dto.copyTo(employeeSalaryDO)
        return employeeSalaryDO
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "month")
                        .add(UITableColumn("fibu.employee.user.name", "name"))
                        .add(UITableColumn("fibu.employee.user.firstname", "firstName"))
                        .add(UITableColumn("fibu.employee.staffNumber", "fibu.employee.staffNumber"))
                        .add(lc, "type", "bruttoMitAgAnteil", "comment"))
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: EmployeeSalaryDO): UILayout {
        val layout = super.createEditLayout(dataObject)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "employee", "month", "type", "bruttoMitAgAnteil", "comment")))
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}