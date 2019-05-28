package org.projectforge.rest.fibu

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.EmployeeFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.Employee
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/employee")
class EmployeeRest() : AbstractDTORest<EmployeeDO, Employee, EmployeeDao, EmployeeFilter>(EmployeeDao::class.java, EmployeeFilter::class.java, "fibu.employee.title") {
    override fun transformDO(obj: EmployeeDO, editMode: Boolean): Employee {
        val employee = Employee()
        employee.copyFrom(obj)
        return employee
    }

    override fun transformDTO(dto: Employee): EmployeeDO {
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
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: EmployeeDO): UILayout {
        val layout = super.createEditLayout(dataObject)
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
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
