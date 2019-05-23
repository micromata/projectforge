package org.projectforge.rest.dto

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.EmployeeSalaryType
import java.math.BigDecimal

class EmployeeSalary(
        var employee: EmployeeDO? = null,
        var firstName: String? = null,
        var year: Int? = null,
        var month: Int? = null,
        var bruttoMitAgAnteil: BigDecimal? = null,
        var comment: String? = null,
        var type: EmployeeSalaryType? = null
) : BaseObject<EmployeeSalaryDO>() {}