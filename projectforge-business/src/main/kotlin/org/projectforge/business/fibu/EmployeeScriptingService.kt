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

package org.projectforge.business.fibu

import org.projectforge.business.PfCaches
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Scripting proxy for EmployeeService.
 * Provides read-only access to employee service methods for Groovy/Kotlin scripts.
 * Available in scripts as: employeeService
 *
 * This proxy follows the same pattern as ScriptingDao, but wraps EmployeeService
 * instead of a DAO, since it provides business logic methods rather than entity access.
 */
class EmployeeScriptingService(private val __employeeService: EmployeeService) {
    protected val caches by lazy { PfCaches.instance }

    /**
     * Gets weekly working hours for an employee valid at today's date.
     * @param employee The employee to get weekly hours for.
     * @return Weekly working hours as BigDecimal, or null if not defined.
     */
    fun getWeeklyWorkingHours(employee: EmployeeDO?): BigDecimal? {
        return __employeeService.getWeeklyWorkingHours(employee)
    }

    /**
     * Gets weekly working hours for an employee valid at a specific date.
     * This is critical for scripts that need historical data!
     *
     * @param employee The employee to get weekly hours for.
     * @param validAtDate The date at which the weekly hours should be valid (historical lookup).
     * @param checkAccess If true, check access rights for the logged-in user.
     * @return Weekly working hours as BigDecimal, or null if not defined.
     */
    fun getWeeklyWorkingHours(
        employee: EmployeeDO?,
        validAtDate: LocalDate?,
        checkAccess: Boolean = true
    ): BigDecimal? {
        return __employeeService.getWeeklyWorkingHours(employee, validAtDate, checkAccess)
    }

    /**
     * Gets annual leave days for an employee valid at today's date.
     * @param employee The employee to get annual leave for.
     * @return Annual leave days as BigDecimal, or null if not defined.
     */
    fun getAnnualLeaveDays(employee: EmployeeDO?): BigDecimal? {
        return __employeeService.getAnnualLeaveDays(employee)
    }

    /**
     * Gets annual leave days for an employee valid at a specific date.
     *
     * @param employee The employee to get annual leave for.
     * @param validAtDate The date at which the annual leave should be valid (historical lookup).
     * @param checkAccess If true, check access rights for the logged-in user.
     * @return Annual leave days as BigDecimal, or null if not defined.
     */
    fun getAnnualLeaveDays(
        employee: EmployeeDO?,
        validAtDate: LocalDate?,
        checkAccess: Boolean = true
    ): BigDecimal? {
        return __employeeService.getAnnualLeaveDays(employee, validAtDate, checkAccess)
    }

    /**
     * Checks if an employee is currently active.
     * An employee is active if austrittsdatum (quit date) is not set or in the future.
     *
     * @param employee The employee to check.
     * @param showRecentlyLeavers If true, also consider employees as active if they left within the last 3 months.
     * @return True if the employee is active.
     */
    fun isEmployeeActive(
        employee: EmployeeDO,
        showRecentlyLeavers: Boolean = false
    ): Boolean {
        return __employeeService.isEmployeeActive(employee, showRecentlyLeavers)
    }

    /**
     * Gets the current employment status of an employee.
     *
     * @param employee The employee to get status for.
     * @param checkAccess If true, check access rights for the logged-in user.
     * @return The EmployeeStatus enum value, or null if not defined.
     */
    fun getEmployeeStatus(
        employee: EmployeeDO,
        checkAccess: Boolean = true
    ): EmployeeStatus? {
        return __employeeService.getEmployeeStatus(employee, checkAccess)
    }

    /**
     * Returns all active employees.
     * An employee is active if austrittsdatum is not set or in the future.
     *
     * @param checkAccess If true, only return employees the logged-in user has access to.
     * @param showRecentLeft If true, also include employees who left within the last 3 months.
     * @return List of active employees.
     */
    fun selectAllActive(
        checkAccess: Boolean,
        showRecentLeft: Boolean = false
    ): List<EmployeeDO> {
        return __employeeService.selectAllActive(checkAccess, showRecentLeft)
    }

    /**
     * Finds an employee by user ID.
     *
     * @param userId The user ID to search for.
     * @return The employee, or null if not found.
     */
    fun findByUserId(userId: Long?): EmployeeDO? {
        return __employeeService.findByUserId(userId)
    }

    /**
     * Finds an employee by staff number (Int).
     *
     * @param staffnumber The staff number to search for.
     * @return The employee, or null if not found.
     */
    fun findByStaffnumber(staffnumber: Int?): EmployeeDO? {
        return __employeeService.findByStaffnumber(staffnumber)
    }

    /**
     * Finds an employee by staff number (String).
     *
     * @param staffnumber The staff number to search for.
     * @return The employee, or null if not found.
     */
    fun findByStaffnumber(staffnumber: String?): EmployeeDO? {
        return __employeeService.findByStaffnumber(staffnumber)
    }

    /**
     * Gets all time-dependent attributes (ValidSinceAttr) for an employee.
     * This includes annual leave days, weekly working hours, and status entries with their validity dates.
     *
     * @param employee The employee to get attributes for.
     * @param type Optional type filter (ANNUAL_LEAVE, WEEKLY_HOURS, or STATUS). If null, all types are returned.
     * @param deleted If true, only deleted attributes are returned. If false, only non-deleted. If null, all are returned.
     * @param checkAccess If true, check access rights for the logged-in user.
     * @return List of all ValidSinceAttr entries for the employee.
     */
    fun selectAllValidSinceAttrs(
        employee: EmployeeDO,
        type: EmployeeValidSinceAttrType? = null,
        deleted: Boolean? = false,
        checkAccess: Boolean = true
    ): List<EmployeeValidSinceAttrDO> {
        return __employeeService.selectAllValidSinceAttrs(employee, type, deleted, checkAccess)
    }
}
