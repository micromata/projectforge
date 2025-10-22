/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.availability.repository

import jakarta.annotation.PostConstruct
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.PfCaches
import org.projectforge.business.availability.AvailabilityTypeConfiguration
import org.projectforge.business.availability.model.AvailabilityDO
import org.projectforge.business.availability.service.AvailabilityValidator
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Month

/**
 * DAO for employee availability entries.
 *
 * @author Kai Reinhard
 */
@Service
open class AvailabilityDao : BaseDao<AvailabilityDO>(AvailabilityDO::class.java) {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var availabilityTypeConfiguration: AvailabilityTypeConfiguration

    private lateinit var availabilityValidator: AvailabilityValidator

    init {
        supportAfterUpdate = true
    }

    @PostConstruct
    private fun postConstruct() {
        // Lazy initialization to avoid circular dependencies
        availabilityValidator = applicationContext.getBean(AvailabilityValidator::class.java)
    }

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    override val defaultSortProperties: Array<SortProperty>
        get() = DEFAULT_SORT_PROPERTIES

    override fun newInstance(): AvailabilityDO {
        return AvailabilityDO()
    }

    override fun hasHistoryAccess(user: PFUserDO, obj: AvailabilityDO, throwException: Boolean): Boolean {
        return if (hasHrRights(user) || isOwnEntry(user, obj)) {
            true
        } else throwOrReturnFalse(throwException)
    }

    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    override fun hasUserSelectAccess(user: PFUserDO, obj: AvailabilityDO, throwException: Boolean): Boolean {
        if (hasHrRights(user) || isOwnEntry(user, obj) || isReplacement(user, obj)) {
            return true
        }
        return if (obj.employee != null && accessChecker.areUsersInSameGroup(user, obj.employee!!.user) &&
            // Don't show availability entries of others older than 31 days (end date):
            obj.endDate?.isAfter(LocalDate.now().minusDays(31L)) == true
        ) {
            true
        } else throwOrReturnFalse(throwException)
    }

    override fun hasInsertAccess(user: PFUserDO, obj: AvailabilityDO?, throwException: Boolean): Boolean {
        requireNotNull(obj) { "Given AvailabilityDO as obj parameter mustn't be null." }
        return hasAvailabilityAccess(user, obj, null, throwException)
    }

    override fun hasUpdateAccess(
        user: PFUserDO,
        obj: AvailabilityDO,
        dbObj: AvailabilityDO?,
        throwException: Boolean
    ): Boolean {
        return hasAvailabilityAccess(user, obj, dbObj, throwException)
    }

    private fun hasAvailabilityAccess(
        user: PFUserDO,
        obj: AvailabilityDO,
        dbObj: AvailabilityDO?,
        throwException: Boolean,
    ): Boolean {
        if (hasHrRights(user)) {
            return true // HR staff members are allowed to do everything.
        }

        // Check if availability type is HR-only
        obj.availabilityType?.let { typeKey ->
            val typeConfig = availabilityTypeConfiguration.getTypeByKey(typeKey)
            if (typeConfig?.hrOnly == true && !hasHrRights(user)) {
                return throwOrReturnFalse("access.exception.hrOnlyAvailabilityType", throwException)
            }
        }

        if (!isOwnEntry(user, obj)) {
            return throwOrReturnFalse(throwException)
        }

        obj.startDate?.let { startDate ->
            if (startDate.isBefore(LocalDate.now())) {
                if (dbObj != null && isTimePeriodAndEmployeeUnchanged(obj, dbObj)) {
                    // Past entries may be modified on non-time period values
                    return true
                }
                // Users aren't allowed to insert/update old entries.
                return throwOrReturnFalse(throwException)
            }
        }

        return true
    }

    private fun isTimePeriodAndEmployeeUnchanged(obj: AvailabilityDO, dbObj: AvailabilityDO): Boolean {
        return obj.startDate == dbObj.startDate &&
                obj.endDate == dbObj.endDate &&
                obj.employee?.id == dbObj.employee?.id &&
                obj.availabilityType == dbObj.availabilityType &&
                obj.percentage == dbObj.percentage
    }

    override fun afterLoad(obj: AvailabilityDO) {
        if (StringUtils.isNotBlank(obj.comment)) {
            val user = ThreadLocalUserContext.loggedInUser!!
            if (!isOwnEntry(user, obj) && !hasUpdateAccess(user, obj, obj, false)) {
                // Entry is not own entry and user has no update access to it, so hide comment due to data privacy.
                obj.comment = "..."
            }
        }
    }

    override fun hasDeleteAccess(
        user: PFUserDO,
        obj: AvailabilityDO,
        dbObj: AvailabilityDO?,
        throwException: Boolean
    ): Boolean {
        if (hasHrRights(user)) {
            return true
        }

        // Check if availability type is HR-only
        obj.availabilityType?.let { typeKey ->
            val typeConfig = availabilityTypeConfiguration.getTypeByKey(typeKey)
            if (typeConfig?.hrOnly == true && !hasHrRights(user)) {
                return throwOrReturnFalse("access.exception.hrOnlyAvailabilityType", throwException)
            }
        }

        if (!isOwnEntry(user, obj, dbObj)) {
            return throwOrReturnFalse(throwException)
        }

        return if (obj.startDate!!.isBefore(LocalDate.now())) {
            // The user isn't allowed to delete entries of the past.
            throwOrReturnFalse(throwException)
        } else true
    }

    open fun hasLoggedInUserHRAvailabilityAccess(): Boolean {
        return accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READWRITE)
    }

    open fun hasHrRights(loggedInUser: PFUserDO?): Boolean {
        return accessChecker.hasRight(loggedInUser, UserRightId.HR_VACATION, false, UserRightValue.READWRITE)
    }

    private fun isOwnEntry(loggedInUser: PFUserDO, obj: AvailabilityDO, oldObj: AvailabilityDO?): Boolean {
        return if (!isOwnEntry(loggedInUser, obj)) {
            false
        } else oldObj?.let { isOwnEntry(loggedInUser, it) } ?: true
    }

    private fun isOwnEntry(loggedInUser: PFUserDO, obj: AvailabilityDO): Boolean {
        return isUserEqualsEmployee(loggedInUser, obj.employee)
    }

    private fun isReplacement(loggedInUser: PFUserDO, obj: AvailabilityDO): Boolean {
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
        val empl = caches.getEmployeeIfNotInitialized(employee)
        return empl?.user?.id == loggedInUser.id
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

    override fun onInsertOrModify(obj: AvailabilityDO, operationType: OperationType) {
        // Validation is done separately
    }

    override fun onInsert(obj: AvailabilityDO) {
        availabilityValidator.validate(obj, null, true)
    }

    override fun onUpdate(obj: AvailabilityDO, dbObj: AvailabilityDO) {
        availabilityValidator.validate(obj, dbObj, true)
    }

    open fun getAvailabilityForPeriod(
        employeeId: Long?,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): List<AvailabilityDO> {
        val sql =
            "SELECT a FROM AvailabilityDO a WHERE a.employee.id = :employeeId AND a.endDate >= :startDate AND a.startDate <= :endDate AND a.deleted = :deleted ORDER BY a.startDate DESC"
        return persistenceService.executeQuery(
            sql,
            AvailabilityDO::class.java,
            Pair("employeeId", employeeId),
            Pair("startDate", startDate),
            Pair("endDate", endDate),
            Pair("deleted", false),
        )
    }

    override fun createQueryFilter(filter: BaseSearchFilter?): QueryFilter {
        return super.createQueryFilter(filter).also {
            it.entityGraphName = AvailabilityDO.ENTITY_GRAPH_WITH_OTHER_REPLACEMENTIDS
        }
    }

    open fun getActiveAvailabilityForYear(employee: EmployeeDO?, year: Int): List<AvailabilityDO> {
        val startYear = LocalDate.of(year, Month.JANUARY, 1)
        val endYear = LocalDate.of(year, Month.DECEMBER, 31)
        val sql =
            "SELECT a FROM AvailabilityDO a WHERE a.employee = :employee AND a.endDate >= :startDate AND a.startDate <= :endDate AND a.deleted = :deleted ORDER BY a.startDate DESC"
        return persistenceService.executeQuery(
            sql,
            AvailabilityDO::class.java,
            Pair("employee", employee),
            Pair("startDate", startYear),
            Pair("endDate", endYear),
            Pair("deleted", false),
        )
    }

    companion object {
        private val ADDITIONAL_SEARCH_FIELDS = arrayOf("employee.user.firstname", "employee.user.lastname")
        private val DEFAULT_SORT_PROPERTIES = arrayOf(
            SortProperty("employee.user.firstname"),
            SortProperty("employee.user.lastname"),
            SortProperty("startDate", org.projectforge.framework.persistence.api.SortOrder.DESCENDING)
        )
    }
}
