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

package org.projectforge.business.vacation.repository

import org.apache.commons.lang3.StringUtils
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.business.vacation.VacationFilter
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.business.vacation.service.VacationSendMailService
import org.projectforge.business.vacation.service.VacationService
import org.projectforge.business.vacation.service.VacationValidator
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.Month
import java.util.stream.Collectors

/**
 * DAO für Urlaubsanträge.
 *
 * @author Florian Blumenstein, Kai Reinhard
 */
@Repository
open class VacationDao : BaseDao<VacationDO>(VacationDO::class.java) {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var vacationSendMailService: VacationSendMailService

    init {
        supportAfterUpdate = true
    }

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    override val defaultSortProperties: Array<SortProperty>
        get() = DEFAULT_SORT_PROPERTIES

    override fun newInstance(): VacationDO {
        return VacationDO()
    }

    override fun hasHistoryAccess(user: PFUserDO, obj: VacationDO, throwException: Boolean): Boolean {
        return if (hasHrRights(user) || isOwnEntry(user, obj) || isManager(user, obj)) {
            true
        } else throwOrReturnFalse(throwException)
    }

    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    override fun hasUserSelectAccess(user: PFUserDO, obj: VacationDO, throwException: Boolean): Boolean {
        if (hasHrRights(user) || isOwnEntry(user, obj) || isManager(user, obj) || isReplacement(user, obj)) {
            return true
        }
        return if (obj.employee != null && accessChecker.areUsersInSameGroup(user, obj.employee!!.user) &&
            // Don't show vacation entries of others older than 31 days (end date):
            obj.endDate?.isAfter(LocalDate.now().minusDays(31L)) == true
        ) {
            true
        } else throwOrReturnFalse(throwException)
    }

    override fun hasInsertAccess(user: PFUserDO, obj: VacationDO, throwException: Boolean): Boolean {
        return hasVacationAccess(user, obj, null, throwException)
    }

    override fun hasUpdateAccess(
        user: PFUserDO,
        obj: VacationDO,
        dbObj: VacationDO,
        throwException: Boolean
    ): Boolean {
        return hasVacationAccess(user, obj, dbObj, throwException)
    }

    private fun hasVacationAccess(
        user: PFUserDO,
        obj: VacationDO,
        dbObj: VacationDO?,
        throwException: Boolean,
    ): Boolean {
        if (hasHrRights(user)) {
            return true // HR staff member are allowed to do everything.
        }
        if (!isOwnEntry(user, obj) && !isManager(user, obj)) {
            return throwOrReturnFalse(throwException)
        }
        if (obj.startDate!!.isBefore(LocalDate.now())) {
            if (dbObj != null &&
                isTimePeriodAndEmployeeUnchanged(obj, dbObj) &&
                getAllowedStatus(user, dbObj).contains(obj.status)
            ) {
                // Past entries may be modified on non time period values, but on allowed status changes as well as on description.
                return true
            }
            // User aren't allowed to insert/update old entries.
            return throwOrReturnFalse(throwException)
        }
        obj.status?.let {
            if (!getAllowedStatus(user, obj).contains(it)) {
                return throwOrReturnFalse(throwException)
            }
        }
        if (!isOwnEntry(user, obj, dbObj)) {
            return if (dbObj != null && isManager(user, obj, dbObj)) {
                if (isTimePeriodAndEmployeeUnchanged(obj, dbObj)) {
                    if (obj.special != true) {
                        // Manager is only allowed to change status and replacement, but not allowed to approve special vacations.
                        true
                    } else {
                        if (obj.status == dbObj.status || obj.status != VacationStatus.APPROVED) {
                            true
                        } else {
                            throwOrReturnFalse(throwException)
                        }
                    }
                } else throwOrReturnFalse(throwException)
                // Normal user isn't allowed to insert foreign entries.
            } else throwOrReturnFalse(throwException)
            // Normal user isn't allowed to insert foreign entries.
        }
        return if (obj.status == VacationStatus.APPROVED) {
            // Normal user isn't allowed to insert/update approved entries:
            throwOrReturnFalse(VacationValidator.Error.NOT_ALLOWED_TO_APPROVE.messageKey, throwException)
        } else true
    }

    private fun isTimePeriodAndEmployeeUnchanged(obj: VacationDO, dbObj: VacationDO): Boolean {
        return obj.startDate == dbObj.startDate &&
                obj.endDate == dbObj.endDate &&
                obj.special === dbObj.special &&
                obj.employeeId == dbObj.employeeId &&
                obj.halfDayBegin === dbObj.halfDayBegin &&
                obj.halfDayEnd === dbObj.halfDayEnd
    }

    /**
     * Gets all available status values for given user.
     */
    open fun getAllowedStatus(user: PFUserDO, vacation: VacationDO): List<VacationStatus> {
        if (hasHrRights(user)) {
            return VacationStatus.values().toList() // All status values for HR staff.
        }
        val status = vacation.status
        vacation.startDate?.let {
            if (it.isBefore(LocalDate.now())) {
                if (isManager(user, vacation) && vacation.special != true) {
                    // Manager is only allowed to approve past entries (except specials).
                    return createDistinctList(status, VacationStatus.APPROVED)
                }
                // Users aren't allowed to change old entries.
                return createDistinctList(status) // Don't change status
            }
        }
        if (isManager(user, vacation)) {
            if (vacation.special == true) {
                return if (status != VacationStatus.APPROVED) {
                    // Manager can't approve special vacation entries:
                    createDistinctList(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED)
                } else {
                    // Manager can't dis-approve special vacation entries:
                    createDistinctList(status, VacationStatus.IN_PROGRESS, VacationStatus.REJECTED)
                }
            }
            return VacationStatus.values().toList() // All status values for manager.
        }
        if (status == VacationStatus.APPROVED) {
            return listOf(VacationStatus.APPROVED) // Approved entries are only allowed to delete.
        }
        // If not approved, these status values are allowed for employees:
        return listOf(VacationStatus.IN_PROGRESS, VacationStatus.REJECTED)
    }

    private fun createDistinctList(vararg statusValues: VacationStatus?): List<VacationStatus> {
        return statusValues.filterNotNull().distinct().sortedBy { it.ordinal }
    }

    override fun afterLoad(obj: VacationDO) {
        if (StringUtils.isNotBlank(obj.comment)) {
            val user = ThreadLocalUserContext.user!!
            if (!isOwnEntry(user, obj) && !hasUpdateAccess(user, obj, obj, false)) {
                // Entry is not own entry and user has no update access to it, so hide comment due to data privacy.
                obj.comment = "..."
            }
        }
    }

    override fun hasDeleteAccess(
        user: PFUserDO,
        obj: VacationDO,
        dbObj: VacationDO,
        throwException: Boolean
    ): Boolean {
        if (hasHrRights(user)) {
            return true
        }
        if (!isOwnEntry(user, obj, dbObj)) {
            return throwOrReturnFalse(throwException) // Normal user isn't allowed to insert foreign entries.
        }
        return if (obj.status == VacationStatus.APPROVED && obj.startDate!!.isBefore(LocalDate.now())) {
            // The user isn't allowed to delete approved entries of the past.
            throwOrReturnFalse(throwException)
        } else true
    }

    open fun hasLoggedInUserHRVacationAccess(): Boolean {
        return accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READWRITE)
    }

    open fun hasHrRights(loggedInUser: PFUserDO?): Boolean {
        return accessChecker.hasRight(loggedInUser, UserRightId.HR_VACATION, false, UserRightValue.READWRITE)
    }

    open fun getCurrentAndFutureVacations(): List<VacationDO> {
        return persistenceService.query(
            VacationDO.FIND_CURRENT_AND_FUTURE,
            VacationDO::class.java,
            Pair("endDate", LocalDate.now()),
            Pair("statusList", listOf(VacationStatus.APPROVED, VacationStatus.IN_PROGRESS)),
        )
    }

    private fun isOwnEntry(loggedInUser: PFUserDO, obj: VacationDO, oldObj: VacationDO?): Boolean {
        return if (!isOwnEntry(loggedInUser, obj)) {
            false
        } else oldObj?.let { isOwnEntry(loggedInUser, it) } ?: true
    }

    private fun isOwnEntry(loggedInUser: PFUserDO, obj: VacationDO): Boolean {
        return isUserEqualsEmployee(loggedInUser, obj.employee)
    }

    private fun isManager(loggedInUser: PFUserDO, obj: VacationDO, oldObj: VacationDO?): Boolean {
        return if (!isManager(loggedInUser, obj)) {
            false
        } else oldObj?.let { isManager(loggedInUser, it) } ?: true
    }

    private fun isManager(loggedInUser: PFUserDO, obj: VacationDO): Boolean {
        return isUserEqualsEmployee(loggedInUser, obj.manager)
    }

    private fun isReplacement(loggedInUser: PFUserDO, obj: VacationDO): Boolean {
        if (isUserEqualsEmployee(loggedInUser, obj.replacement)) {
            return true
        }
        obj.otherReplacements?.forEach { replacement ->
            if (isUserEqualsEmployee(loggedInUser, replacement)) {
                return true
            }
        }
        return false
    }

    private fun isUserEqualsEmployee(loggedInUser: PFUserDO, employee: EmployeeDO?): Boolean {
        employee ?: return false
        var empl: EmployeeDO? = employee
        if (employee.userId == null) {
            // Object wasn't loaded from data base:
            empl = employeeDao.internalGetById(employee.id)
        }
        return empl != null && empl.userId == loggedInUser.id

    }

    private fun throwOrReturnFalse(throwException: Boolean): Boolean {
        if (throwException) {
            throw AccessException("access.exception.userHasNotRight", UserRightId.HR_VACATION, UserRightValue.READWRITE)
        }
        return false
    }

    private fun throwOrReturnFalse(messageKey: String, throwException: Boolean): Boolean {
        if (throwException) {
            throw AccessException(messageKey)
        }
        return false
    }

    override fun onSaveOrModify(obj: VacationDO) {
        super.onSaveOrModify(obj)
        if (obj.special == null) {
            obj.special = false // Avoid null value of special.
        }
    }

    override fun onSave(obj: VacationDO) {
        super.onSave(obj)
        val service = applicationContext.getBean(VacationService::class.java)
        service.validate(obj, null, true)
    }

    override fun onChange(obj: VacationDO, dbObj: VacationDO) {
        super.onChange(obj, dbObj)
        val service = applicationContext.getBean(VacationService::class.java)
        service.validate(obj, dbObj, true)
    }

    override fun afterSave(obj: VacationDO) {
        super.afterSave(obj)
        vacationSendMailService.checkAndSendMail(obj, OperationType.INSERT)
    }

    override fun afterUpdate(obj: VacationDO, dbObj: VacationDO?) {
        super.afterUpdate(obj, dbObj)
        vacationSendMailService.checkAndSendMail(obj, OperationType.UPDATE, dbObj)
    }

    override fun afterDelete(obj: VacationDO) {
        super.afterDelete(obj)
        vacationSendMailService.checkAndSendMail(obj, OperationType.DELETE)
    }

    override fun afterUndelete(obj: VacationDO) {
        super.afterDelete(obj)
        vacationSendMailService.checkAndSendMail(obj, OperationType.UNDELETE)
    }

    open fun getVacationForPeriod(
        employeeId: Int?,
        startVacationDate: LocalDate?,
        endVacationDate: LocalDate?,
        withSpecial: Boolean
    ): List<VacationDO> {
        val baseSQL =
            "SELECT v FROM VacationDO v WHERE v.employee.id = :employeeId AND v.endDate >= :startDate AND v.startDate <= :endDate"
        val sql = baseSQL + if (withSpecial) META_SQL_WITH_SPECIAL else META_SQL
        return persistenceService.query(
            sql,
            VacationDO::class.java,
            Pair("employeeId", employeeId),
            Pair("startDate", startVacationDate),
            Pair("endDate", endVacationDate),
            Pair("deleted", false),
        )
    }

    open fun getVacationForPeriod(
        employee: EmployeeDO,
        startVacationDate: LocalDate?,
        endVacationDate: LocalDate?,
        withSpecial: Boolean
    ): List<VacationDO> {
        return getVacationForPeriod(employee.id, startVacationDate, endVacationDate, withSpecial)
    }

    override fun getList(filter: BaseSearchFilter): List<VacationDO> {
        val myFilter: VacationFilter = if (filter is VacationFilter) {
            filter
        } else {
            VacationFilter(filter)
        }
        val queryFilter = createQueryFilter(myFilter)
        if (!accessChecker.hasLoggedInUserRight(
                UserRightId.HR_VACATION, false, UserRightValue.READONLY,
                UserRightValue.READWRITE
            )
        ) {
            val employeeId = myFilter.employeeId
            persistenceService.selectById(EmployeeDO::class.java, employeeId)?.let { employeeFromFilter ->
                queryFilter.createJoin("replacement")
                queryFilter.add(
                    or(
                        eq("employee", employeeFromFilter),
                        eq("manager", employeeFromFilter),
                        eq("replacement.id", employeeId) // does not work with the whole employee object, need id
                    )
                )
            }
        }
        if (myFilter.vacationstatus != null) {
            queryFilter.add(eq("status", myFilter.vacationstatus))
        }
        queryFilter.addOrder(asc("startDate"))
        var resultList = getList(queryFilter)
        if (myFilter.vacationmode != null) {
            resultList =
                resultList.stream().filter { vac: VacationDO? -> vac!!.getVacationmode() == myFilter.vacationmode }
                    .collect(Collectors.toList())
        }
        return resultList
    }

    open fun getActiveVacationForYear(employee: EmployeeDO?, year: Int, withSpecial: Boolean): List<VacationDO> {
        val startYear = LocalDate.of(year, Month.JANUARY, 1)
        val endYear = LocalDate.of(year, Month.DECEMBER, 31)
        val baseSQL =
            "SELECT v FROM VacationDO v WHERE v.employee = :employee AND v.endDate >= :startDate AND v.startDate <= :endDate"
        val sql = baseSQL + if (withSpecial) META_SQL_WITH_SPECIAL else META_SQL
        return persistenceService.query(
            sql,
            VacationDO::class.java,
            Pair("employee", employee),
            Pair("startDate", startYear),
            Pair("endDate", endYear),
            Pair("deleted", false),
        )
    }

    open fun getOpenLeaveApplicationsForEmployee(employee: EmployeeDO?): Long {
        val baseSQL = "SELECT COUNT(v) FROM VacationDO v WHERE v.manager = :employee AND v.status = :status"
        val sql = baseSQL + META_SQL_WITH_SPECIAL
        return persistenceService.selectSingleResult(
            sql,
            Long::class.java,
            Pair("employee", employee),
            Pair("status", VacationStatus.IN_PROGRESS),
            Pair("deleted", false),
            nullAllowed = false,
        ) ?: 0L
    }

    companion object {
        private const val META_SQL = " AND v.special = false AND v.deleted = :deleted order by startDate desc"
        private const val META_SQL_WITH_SPECIAL = " AND v.deleted = :deleted order by startDate desc"
        private val ADDITIONAL_SEARCH_FIELDS = arrayOf("employee.user.firstname", "employee.user.lastname")
        private val DEFAULT_SORT_PROPERTIES = arrayOf(
            SortProperty("employee.user.firstname"),
            SortProperty("employee.user.lastname"),
            SortProperty("startDate", SortOrder.DESCENDING)
        )
    }
}
