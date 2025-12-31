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

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.apache.commons.collections4.CollectionUtils
import org.projectforge.business.PfCaches
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.history.HistoryFormatService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * Standard implementation of the Employee service interface.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Service
class EmployeeService {
    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var employeeServiceSupport: EmployeeServiceSupport

    @Autowired
    private lateinit var historyFormatService: HistoryFormatService

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @PostConstruct
    private fun postConstruct() {
        instance = this
        employeeDao.employeeService = this
        historyFormatService.register(EmployeeValidSinceAttrDO::class.java, EmployeeValidSinceAttrHistoryAdapter())
    }

    fun findByUserId(userId: Long?): EmployeeDO? {
        return employeeDao.findByUserId(userId)
    }

    /**
     * Returns true if the given employee is active.
     * An employee is active if the austrittsdatum is not set or if the austrittsdatum is in the future.
     * If showRecentLeft is true, the employee is also active if the austrittsdatum is within the last 3 months.
     * @param employee The employee to check.
     * @param showRecentlyLeavers If true, the employee is also active if the austrittsdatum is within the last 3 months.
     * @return True if the employee is active.
     */
    fun isEmployeeActive(employee: EmployeeDO, showRecentlyLeavers: Boolean = false): Boolean {
        employee.austrittsDatum.let { quitDate ->
            val user = caches.getUser(employee.user?.id)
            quitDate
                ?: // No quit date given. Employee is active if user is not deleted or deactivated.
                return user == null || (!user.deactivated && !user.deleted) // user is only null in tests.
            val date = if (showRecentlyLeavers) {
                LocalDate.now().minusMonths(3)
            } else {
                LocalDate.now()
            }
            return !date.isAfter(quitDate)
        }
    }

    /**
     * Returns all active employees.
     * An employee is active if the austrittsdatum is not set or if the austrittsdatum is in the future.
     * If showRecentLeft is true, the employee is also active if the austrittsdatum is within the last 3 months.
     * @param checkAccess If true, the logged-in user must have access to the employee.
     * @param showRecentLeft If true, the employee is also active if the austrittsdatum is within the last 3 months.
     * @return List of active employees.
     */
    fun selectAllActive(checkAccess: Boolean, showRecentLeft: Boolean = false): List<EmployeeDO> {
        val employeeList: Collection<EmployeeDO> = if (checkAccess) {
            employeeDao.select(EmployeeFilter())
        } else {
            employeeDao.selectAll(checkAccess = false)
        }
        return employeeList.filter { employee -> isEmployeeActive(employee, showRecentLeft) }
            .sortedBy { it.displayName }
    }

    fun findByStaffnumber(staffnumber: Int?): EmployeeDO? {
        return findByStaffnumber(staffnumber?.toString())
    }

    fun findByStaffnumber(staffnumber: String?): EmployeeDO? {
        staffnumber ?: return null
        return employeeDao.findByStaffnumber(staffnumber)
    }

    fun findValidSinceAttr(
        id: Long?,
        expectedType: EmployeeValidSinceAttrType? = null,
        checkAccess: Boolean = true
    ): EmployeeValidSinceAttrDO? {
        return employeeServiceSupport.findValidSinceAttr(id, expectedType, checkAccess)
    }

    /**
     * @param employee The employee to select the attribute for.
     * @param type The type of the attribute to select.
     * @param deleted If true, only deleted attributes will be returned, if false, only not deleted attributes will be returned. If null, deleted and not deleted attributes will be returned.
     */
    internal fun selectAllValidSinceAttrs(
        employee: EmployeeDO,
        type: EmployeeValidSinceAttrType? = null,
        deleted: Boolean? = false,
        checkAccess: Boolean = true,
    ): List<EmployeeValidSinceAttrDO> {
        return employeeServiceSupport.selectAllValidSinceAttrs(
            employee,
            type,
            deleted = deleted,
            checkAccess = checkAccess,
        )
    }

    /**
     * @param employee The employee to select the attribute for.
     * @param checkAccess If true, the logged in user must have access to the employee.
     * @return The attribute that is currently valid.
     */
    fun getEmployeeStatus(
        employee: EmployeeDO,
        checkAccess: Boolean = true
    ): EmployeeStatus? {
        return employeeServiceSupport.getEmployeeStatus(employee, checkAccess = checkAccess)
    }

    fun getAnnualLeaveDays(employee: EmployeeDO?): BigDecimal? {
        return getAnnualLeaveDays(employee, LocalDate.now())
    }

    fun getAnnualLeaveDays(employee: EmployeeDO?, validAtDate: LocalDate?, checkAccess: Boolean = true): BigDecimal? {
        return employeeServiceSupport.getAnnualLeaveDays(employee, validAtDate, checkAccess = checkAccess)
    }

    fun getWeeklyWorkingHours(employee: EmployeeDO?): BigDecimal? {
        return getWeeklyWorkingHours(employee, LocalDate.now())
    }

    fun getWeeklyWorkingHours(
        employee: EmployeeDO?,
        validAtDate: LocalDate?,
        checkAccess: Boolean = true
    ): BigDecimal? {
        return employeeServiceSupport.getWeeklyWorkingHours(employee, validAtDate, checkAccess = checkAccess)
    }

    internal fun findActiveEntry(
        entries: List<EmployeeValidSinceAttrDO>,
        validAtDate: LocalDate? = null,
    ): EmployeeValidSinceAttrDO? {
        return employeeServiceSupport.getActiveEntry(entries, validAtDate)
    }

    /**
     * @param employeeId The employee (as id) to select the attribute for.
     * @param deleted If true, only deleted attributes will be returned, if false, only not deleted attributes will be returned. If null, deleted and not deleted attributes will be returned.
     * @param checkAccess If true, the logged-in user must have access to the employee.
     */
    fun selectAnnualLeaveDayEntries(
        employeeId: Long,
        deleted: Boolean? = false,
        checkAccess: Boolean = true,
    ): List<EmployeeValidSinceAttrDO> {
        return employeeServiceSupport.selectAnnualLeaveDayEntries(
            employeeId,
            deleted = deleted,
            checkAccess = checkAccess
        )
    }

    /**
     * @param employee The employee to select the attribute for.
     * @param deleted If true, only deleted attributes will be returned, if false, only not deleted attributes will be returned. If null, deleted and not deleted attributes will be returned.
     * @param checkAccess If true, the logged-in user must have access to the employee.
     */
    fun selectAnnualLeaveDayEntries(
        employee: EmployeeDO,
        deleted: Boolean? = false,
        checkAccess: Boolean = true
    ): List<EmployeeValidSinceAttrDO> {
        return selectAllValidSinceAttrs(
            employee,
            EmployeeValidSinceAttrType.ANNUAL_LEAVE,
            deleted = deleted,
            checkAccess = checkAccess
        )
    }

    /**
     * @param employeeId The employee (as id) to select the attribute for.
     * @param deleted If true, only deleted attributes will be returned, if false, only not deleted attributes will be returned. If null, deleted and not deleted attributes will be returned.
     * @param checkAccess If true, the logged-in user must have access to the employee.
     */
    fun selectWeeklyWorkingHoursEntries(
        employeeId: Long,
        deleted: Boolean? = false,
        checkAccess: Boolean = true,
    ): List<EmployeeValidSinceAttrDO> {
        return employeeServiceSupport.selectWeeklyWorkingHoursEntries(
            employeeId,
            deleted = deleted,
            checkAccess = checkAccess
        )
    }

    /**
     * @param employee The employee to select the attribute for.
     * @param deleted If true, only deleted attributes will be returned, if false, only not deleted attributes will be returned. If null, deleted and not deleted attributes will be returned.
     * @param checkAccess If true, the logged-in user must have access to the employee.
     */
    fun selectWeeklyWorkingHoursEntries(
        employee: EmployeeDO,
        deleted: Boolean? = false,
        checkAccess: Boolean = true
    ): List<EmployeeValidSinceAttrDO> {
        return selectAllValidSinceAttrs(
            employee,
            EmployeeValidSinceAttrType.WEEKLY_HOURS,
            deleted = deleted,
            checkAccess = checkAccess
        )
    }

    /**
     * @return Error message, if any. Null if given object can be modified or inserted.
     */
    fun validate(attr: EmployeeValidSinceAttrDO): String? {
        try {
            employeeServiceSupport.validate(attr)
            return null
        } catch (ex: Exception) {
            return translateMsg("attr.validation.error.entryWithDateDoesAlreadyExist", attr.validSince)
        }
    }

    fun insertAnnualLeaveDays(
        employee: EmployeeDO,
        validSince: LocalDate,
        annualLeaveDays: BigDecimal,
        checkAccess: Boolean = true,
    ): EmployeeValidSinceAttrDO {
        return employeeServiceSupport.insertValidSinceAttr(
            employee,
            validSince,
            annualLeaveDays.toString(),
            EmployeeValidSinceAttrType.ANNUAL_LEAVE,
            checkAccess = checkAccess,
        )
    }

    fun insertWeeklyHours(
        employee: EmployeeDO,
        validSince: LocalDate,
        weeklyHours: BigDecimal,
        checkAccess: Boolean = true,
    ): EmployeeValidSinceAttrDO {
        return employeeServiceSupport.insertValidSinceAttr(
            employee,
            validSince,
            weeklyHours.stripTrailingZeros().toString(),
            EmployeeValidSinceAttrType.WEEKLY_HOURS,
            checkAccess = checkAccess,
        )
    }

    fun insert(
        employeeId: Long,
        attrDO: EmployeeValidSinceAttrDO,
        checkAccess: Boolean = true,
    ): Long? {
        return employeeServiceSupport.insert(employeeId, attrDO, checkAccess = checkAccess)
    }

    fun insert(
        employee: EmployeeDO,
        attrDO: EmployeeValidSinceAttrDO,
        checkAccess: Boolean = true,
    ): Long? {
        return employeeServiceSupport.insert(employee, attrDO, checkAccess = checkAccess)
    }

    /**
     * @param employeeId The employee (as id) to select the attribute for.
     * @param deleted If true, only deleted attributes will be returned, if false, only not deleted attributes will be returned. If null, deleted and not deleted attributes will be returned.
     * @param checkAccess If true, the logged in user must have access to the employee.
     */
    fun selectStatusEntries(
        employeeId: Long,
        deleted: Boolean? = false,
        checkAccess: Boolean = true
    ): List<EmployeeValidSinceAttrDO> {
        val employee = employeeDao.find(employeeId)!!
        return selectStatusEntries(employee, deleted, checkAccess)
    }

    /**
     * @param employee The employee to select the attribute for.
     * @param deleted If true, only deleted attributes will be returned, if false, only not deleted attributes will be returned. If null, deleted and not deleted attributes will be returned.
     * @param checkAccess If true, the logged in user must have access to the employee.
     */
    fun selectStatusEntries(
        employee: EmployeeDO,
        deleted: Boolean? = false,
        checkAccess: Boolean = true
    ): List<EmployeeValidSinceAttrDO> {
        return selectAllValidSinceAttrs(
            employee,
            EmployeeValidSinceAttrType.STATUS,
            deleted = deleted,
            checkAccess = checkAccess,
        )
    }

    fun insertStatus(
        employee: EmployeeDO,
        validSince: LocalDate,
        status: EmployeeStatus,
        checkAccess: Boolean = true,
    ): EmployeeValidSinceAttrDO {
        return employeeServiceSupport.insertValidSinceAttr(
            employee,
            validSince,
            status.toString(),
            EmployeeValidSinceAttrType.STATUS,
            checkAccess = checkAccess,
        )
    }

    /**
     * @param employeeId The employee (by id) to update the attribute for. Needed for checkAccess.
     * @param attrDO: The attribute to update.
     * @param checkAccess: If true, the logged-in user must have update access to the employee.
     */
    fun updateValidSinceAttr(
        employeeId: Long?,
        attrDO: EmployeeValidSinceAttrDO,
        checkAccess: Boolean = true,
    ): EntityCopyStatus {
        val employee = employeeDao.find(employeeId)!!
        return updateValidSinceAttr(employee, attrDO, checkAccess)
    }

    /**
     * @param employee The employee to update the attribute for. Needed for checkAccess.
     * @param attrDO: The attribute to update.
     * @param checkAccess: If true, the logged-in user must have update access to the employee.
     */
    fun updateValidSinceAttr(
        employee: EmployeeDO,
        attrDO: EmployeeValidSinceAttrDO,
        checkAccess: Boolean = true,
    ): EntityCopyStatus {
        return employeeServiceSupport.updateValidSinceAttr(employee, attrDO, checkAccess = checkAccess)
    }

    fun markValidSinceAttrAsDeleted(
        employeeId: Long?,
        attrId: Long?,
        checkAccess: Boolean = true,
    ) {
        return employeeServiceSupport.markValidSinceAttrAsDeleted(employeeId, attrId, checkAccess = checkAccess)
    }

    fun markValidSinceAttrAsDeleted(
        employee: EmployeeDO,
        attr: EmployeeValidSinceAttrDO,
        checkAccess: Boolean = true,
    ) {
        return employeeServiceSupport.markValidSinceAttrAsDeleted(employee, attr, checkAccess = checkAccess)
    }

    fun undeleteValidSinceAttr(
        employee: EmployeeDO,
        attr: EmployeeValidSinceAttrDO,
        checkAccess: Boolean = true,
    ) {
        return employeeServiceSupport.undeleteValidSinceAttr(employee, attr, checkAccess = checkAccess)
    }

    fun getReportOfMonth(year: Int, month: Int, user: PFUserDO): MonthlyEmployeeReport {
        val monthlyEmployeeReport = MonthlyEmployeeReport(user, year, month)
        monthlyEmployeeReport.init()
        val filter = TimesheetFilter()
        filter.deleted = false
        filter.startTime = monthlyEmployeeReport.fromDate
        filter.stopTime = monthlyEmployeeReport.toDate
        filter.userId = user.id
        val list = timesheetDao.select(filter)
        val loggedInUser = ThreadLocalUserContext.requiredLoggedInUser
        if (CollectionUtils.isNotEmpty(list)) {
            for (sheet in list) {
                monthlyEmployeeReport.addTimesheet(
                    sheet,
                    timesheetDao.hasUserSelectAccess(loggedInUser, sheet, false)
                )
            }
        }
        monthlyEmployeeReport.calculate()
        return monthlyEmployeeReport
    }

    companion object {
        lateinit var instance: EmployeeService
            private set
    }
}
