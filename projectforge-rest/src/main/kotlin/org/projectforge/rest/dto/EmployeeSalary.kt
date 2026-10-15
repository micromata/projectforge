/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto

import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.EmployeeSalaryType
import java.math.BigDecimal

/**
 * DTO of a monthly employee salary (`EmployeeSalaryDO`).
 *
 * The `employee` reference shares its name with the DO field, so the reflective, name-based
 * [BaseDTO.copyFrom]/[copyTo] resolves it on its own: a `BaseDO` field fed a `BaseDTO` of the same name
 * is rebuilt from the DTO's id (see `BaseDTO.copy`), and the other way round the DTO gets the employee
 * filled minimally ({id, displayName}). The read-only [lastName]/[firstName]/[staffNumber] the list
 * shows are no fields of the DO and are filled by hand in [copyFrom] from the resolved employee.
 */
class EmployeeSalary(
    id: Long? = null,
    displayName: String? = null,
    var employee: Employee? = null,
    var year: Int? = null,
    var month: Int? = null,
    var bruttoMitAgAnteil: BigDecimal? = null,
    var comment: String? = null,
    var type: EmployeeSalaryType? = null,
    /** `year-MM`, the entity's own transient format the list column shows (read-only). */
    var formattedYearAndMonth: String? = null,
    /** Read-only, from the employee's user — the list columns Wicket sorts by. */
    var lastName: String? = null,
    var firstName: String? = null,
    /** Read-only, from the employee. */
    var staffNumber: String? = null,
) : BaseDTODisplayObject<EmployeeSalaryDO>(id, displayName = displayName) {

    override fun copyFrom(src: EmployeeSalaryDO) {
        super.copyFrom(src)
        // The transient getter dereferences month; only a fully dated row has a formatted value.
        if (src.year != null && src.month != null) {
            formattedYearAndMonth = src.formattedYearAndMonth
        }
        src.employee?.let { employee ->
            staffNumber = employee.staffNumber
            lastName = employee.user?.lastname
            firstName = employee.user?.firstname
            displayName = listOfNotNull(employee.displayName, formattedYearAndMonth).joinToString(" ")
        }
    }
}
