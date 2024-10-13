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

package org.projectforge.business.fibu

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.apache.commons.collections4.CollectionUtils
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.business.timesheet.TimesheetFilter
import org.projectforge.business.user.UserDao
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime.Companion.from
import org.projectforge.framework.time.PFDateTime.Companion.now
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Standard implementation of the Employee service interface.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Service
class EmployeeService {
    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var kost1Dao: Kost1Dao

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var vacationService: VacationService

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @PostConstruct
    private fun postConstruct() {
        employeeDao.employeeService = this
    }

    fun getList(filter: BaseSearchFilter): List<EmployeeDO> {
        return employeeDao.getList(filter)
    }

    fun getEmployeeByUserId(userId: Long?): EmployeeDO? {
        return employeeDao.findByUserId(userId)
    }

    fun hasLoggedInUserInsertAccess(): Boolean {
        return employeeDao.hasLoggedInUserInsertAccess()
    }

    fun hasLoggedInUserInsertAccess(obj: EmployeeDO, throwException: Boolean): Boolean {
        return employeeDao.hasLoggedInUserInsertAccess(obj, throwException)
    }

    fun hasLoggedInUserUpdateAccess(obj: EmployeeDO, dbObj: EmployeeDO, throwException: Boolean): Boolean {
        return employeeDao.hasLoggedInUserUpdateAccess(obj, dbObj, throwException)
    }

    fun hasLoggedInUserDeleteAccess(obj: EmployeeDO, dbObj: EmployeeDO, throwException: Boolean): Boolean {
        return employeeDao.hasLoggedInUserDeleteAccess(obj, dbObj, throwException)
    }

    fun hasDeleteAccess(user: PFUserDO, obj: EmployeeDO, dbObj: EmployeeDO, throwException: Boolean): Boolean {
        return employeeDao.hasDeleteAccess(user, obj, dbObj, throwException)
    }

    fun getAutocompletion(property: String, searchString: String): List<String> {
        return employeeDao.getAutocompletion(property, searchString)
    }

    fun getDisplayHistoryEntries(obj: EmployeeDO): List<DisplayHistoryEntry> {
        return employeeDao.getDisplayHistoryEntries(obj)
    }

    fun isEmployeeActive(employee: EmployeeDO): Boolean {
        employee.austrittsDatum.let { austrittsdatum ->
            if (austrittsdatum == null) {
                return true
            }
            val now = now()
            val date = from(austrittsdatum)
            return now.isBefore(date)
        }
    }

    fun findAllActive(checkAccess: Boolean): List<EmployeeDO> {
        val employeeList: Collection<EmployeeDO> = if (checkAccess) {
            employeeDao.getList(EmployeeFilter())
        } else {
            employeeDao.internalLoadAll()
        }
        return employeeList.filter { employee -> isEmployeeActive(employee) }
    }

    fun getEmployeeByStaffnumber(staffnumber: String): EmployeeDO? {
        return employeeDao.getEmployeeByStaffnumber(staffnumber)
    }

    fun getAll(checkAccess: Boolean): List<EmployeeDO> {
        return if (checkAccess) employeeDao.getList(EmployeeFilter()) else employeeDao.internalLoadAll()
    }

    private fun getValidityPeriodAttrs(
        employee: EmployeeDO,
        type: EmployeeValidityPeriodAttrType,
    ): List<EmployeeValidityPeriodAttrDO> {
        requireNotNull(employee.id) { "Employee id must not be null." }
        val list = persistenceService.executeQuery(
            "from EmployeeValidityPeriodAttrDO a where a.employee.id = :employeeId and a.attribute = :attribute order by a.validFrom desc",
            EmployeeValidityPeriodAttrDO::class.java,
            Pair("employeeId", employee.id),
            Pair("attribute", type)
        )
        return list
    }

    fun getEmployeeStatus(employee: EmployeeDO): EmployeeStatus? {
        val list = getValidityPeriodAttrs(employee, EmployeeValidityPeriodAttrType.STATUS)
        val validEntry = getActiveEntry(list)
        val status = validEntry?.value
        if (status != null) {
            try {
                return EmployeeStatus.valueOf(status)
            } catch (e: IllegalArgumentException) {
                log.error { "Oups, unknown status value in validityPeriodAttr: $validEntry" }
            }
        }
        return null
    }

    fun getAnnualLeaveDays(employee: EmployeeDO?): BigDecimal? {
        return getAnnualLeaveDays(employee, LocalDate.now())
    }

    fun getAnnualLeaveDays(employee: EmployeeDO?, validAtDate: LocalDate?): BigDecimal? {
        if (employee == null || validAtDate == null) { // Should only occur in CallAllPagesTest (Wicket).
            return null
        }
        return getActiveEntry(getAnnualLeaveDayEntries(employee), validAtDate)?.value?.toBigDecimal()
    }

    private fun ensure(validAtDate: LocalDate?): LocalDate {
        return validAtDate ?: LocalDate.of(1970, Month.JANUARY, 1)
    }

    internal fun getActiveEntry(
        entries: List<EmployeeValidityPeriodAttrDO>,
        validAtDate: LocalDate? = null,
    ): EmployeeValidityPeriodAttrDO? {
        var found: EmployeeValidityPeriodAttrDO? = null
        // example
        // null (active before 2021-01-01), 2021-01-01, 2023-05-08 (active)
        val useDate = validAtDate ?: LocalDate.now()
        entries.forEach { entry ->
            if (useDate >= ensure(entry.validFrom)) {
                found.let { f ->
                    if (f == null) {
                        found = entry
                    } else if (ensure(f.validFrom) < ensure(entry.validFrom)) {
                        found = entry // entry is newer!
                    }
                }
            }
        }
        return found
    }

    fun getAnnualLeaveDayEntries(employee: EmployeeDO): List<EmployeeValidityPeriodAttrDO> {
        val list = getValidityPeriodAttrs(employee, EmployeeValidityPeriodAttrType.ANNUAL_LEAVE)
        return list
    }

    fun addNewAnnualLeaveDays(
        employee: EmployeeDO,
        validfrom: LocalDate?,
        annualLeaveDays: BigDecimal
    ): EmployeeValidityPeriodAttrDO {
        val attr = EmployeeValidityPeriodAttrDO()
        attr.employee = employee
        attr.validFrom = validfrom
        attr.value = annualLeaveDays.toString()
        attr.attribute = EmployeeValidityPeriodAttrType.ANNUAL_LEAVE
        attr.created = Date()
        attr.lastUpdate = attr.created
        persistenceService.runInTransaction { context ->
            val em = context.em
            log.error { "******** Not yet migrated: History entries." }
            em.persist(attr)
        }
        return attr
    }

    fun getReportOfMonth(year: Int, month: Int?, user: PFUserDO): MonthlyEmployeeReport {
        val monthlyEmployeeReport = MonthlyEmployeeReport(this, vacationService, user, year, month)
        monthlyEmployeeReport.init()
        val filter = TimesheetFilter()
        filter.setDeleted(false)
        filter.startTime = monthlyEmployeeReport.fromDate
        filter.stopTime = monthlyEmployeeReport.toDate
        filter.userId = user.id
        val list = timesheetDao.getList(filter)
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
}
