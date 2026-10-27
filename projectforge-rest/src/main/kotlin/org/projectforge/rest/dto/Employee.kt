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

import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeStatus
import org.projectforge.business.user.service.UserService
import java.math.BigDecimal
import java.time.LocalDate

class Employee(
    id: Long? = null,
    displayName: String? = null,
    var user: User? = null,
    var kost1: Kost1? = null,
    var konto: Konto? = null,
    var position: String? = null,
    var eintrittsDatum: LocalDate? = null,
    var austrittsDatum: LocalDate? = null,
    var abteilung: String? = null,
    var staffNumber: String? = null,
    var urlaubstage: Int? = null,
    var comment: String? = null,
) : BaseDTODisplayObject<EmployeeDO>(id, displayName = displayName), EntityAccessSupport {
    override var writeAccess: Boolean? = null
    override var deleteAccess: Boolean? = null

    /**
     * Read-only field.
     */
    var annualLeave: BigDecimal? = null

    /**
     * Read-only field.
     */
    var status: EmployeeStatus? = null

    /**
     * Read-only field.
     */
    var weeklyWorkingHours: BigDecimal? = null

    var annualLeaveEntries: List<EmployeeValidSinceAttr>? = null
    var weeklyWorkingHoursEntries: List<EmployeeValidSinceAttr>? = null
    var statusEntries: List<EmployeeValidSinceAttr>? = null

    companion object {
        private val employeeCache by lazy { EmployeeCache.instance }

        /**
         * Set display names of any existing employee in the given list, and whether it is still active (deleted,
         * deactivated): a no longer active employee is struck through wherever shown as a reference. The cache holds
         * no deleted employee: one missing there keeps its name and flags (see [toEmployeeList]).
         */
        fun restoreDisplayNames(employees: List<Employee>?) {
            employees?.forEach { employee ->
                val employeeDO = employeeCache.getEmployee(employee.id) ?: return@forEach
                employee.displayName = employeeDO.displayName
                employee.deleted = employeeDO.deleted
                employee.deactivated = employeeDO.deactivated
            }
        }

        /**
         * The employees with id, display name and their inactive flags only. The name comes from the [EmployeeCache],
         * not from the given (usually lazy) employees: their user would be loaded one by one otherwise (N+1 selects of
         * `T_PF_USER`). The cache holds no deleted employee, so the name of a deleted one is taken from itself, and
         * the flags always are (columns of the employee, no user needed).
         */
        fun toEmployeeList(employees: Collection<EmployeeDO>?): List<Employee>? {
            employees ?: return null
            return employees.map { employeeDO ->
                val displayName = employeeCache.getEmployee(employeeDO.id)?.displayName ?: employeeDO.displayName
                Employee(employeeDO.id, displayName = displayName).also {
                    it.deleted = employeeDO.deleted
                    it.deactivated = employeeDO.deactivated
                }
            }.sortedBy { it.displayName }
        }

        fun toEmployeeDOList(employees: Collection<Employee>?): MutableSet<EmployeeDO>? {
            employees ?: return null
            val result = mutableSetOf<EmployeeDO>()
            employees.forEach { e ->
                val employeeDO = EmployeeDO()
                employeeDO.id = e.id
                result.add(employeeDO)
            }
            return result
        }
    }
}
