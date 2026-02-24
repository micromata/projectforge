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

package org.projectforge.business.availability

import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * DAO for availability entries.
 */
@Service
open class AvailabilityDao : BaseDao<AvailabilityDO>(AvailabilityDO::class.java) {
    @Autowired
    private lateinit var employeeDao: EmployeeDao

    @Autowired
    private lateinit var caches: PfCaches

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    override val defaultSortProperties: Array<SortProperty>
        get() = DEFAULT_SORT_PROPERTIES

    override fun newInstance(): AvailabilityDO {
        return AvailabilityDO()
    }

    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    override fun hasUserSelectAccess(user: PFUserDO, obj: AvailabilityDO, throwException: Boolean): Boolean {
        if (hasHrRights(user) || isOwnEntry(user, obj)) {
            return true
        }
        // Allow select access if users are in the same group.
        if (obj.employee != null && accessChecker.areUsersInSameGroup(user, obj.employee!!.user)) {
            return true
        }
        return throwOrReturnFalse(throwException)
    }

    override fun hasInsertAccess(user: PFUserDO, obj: AvailabilityDO?, throwException: Boolean): Boolean {
        requireNotNull(obj) { "Given AvailabilityDO as obj parameter mustn't be null." }
        if (hasHrRights(user) || isOwnEntry(user, obj)) {
            return true
        }
        return throwOrReturnFalse(throwException)
    }

    override fun hasUpdateAccess(
        user: PFUserDO,
        obj: AvailabilityDO,
        dbObj: AvailabilityDO?,
        throwException: Boolean
    ): Boolean {
        if (hasHrRights(user) || isOwnEntry(user, obj)) {
            return true
        }
        return throwOrReturnFalse(throwException)
    }

    override fun hasDeleteAccess(
        user: PFUserDO,
        obj: AvailabilityDO,
        dbObj: AvailabilityDO?,
        throwException: Boolean
    ): Boolean {
        if (hasHrRights(user) || isOwnEntry(user, obj)) {
            return true
        }
        return throwOrReturnFalse(throwException)
    }

    override fun hasHistoryAccess(user: PFUserDO, obj: AvailabilityDO, throwException: Boolean): Boolean {
        return if (hasHrRights(user) || isOwnEntry(user, obj)) {
            true
        } else throwOrReturnFalse(throwException)
    }

    open fun hasHrRights(user: PFUserDO?): Boolean {
        return accessChecker.hasRight(user, UserRightId.HR_VACATION, false, UserRightValue.READWRITE)
    }

    private fun isOwnEntry(user: PFUserDO, obj: AvailabilityDO): Boolean {
        val employee = caches.getEmployeeIfNotInitialized(obj.employee) ?: return false
        return employee.user?.id == user.id
    }

    private fun throwOrReturnFalse(throwException: Boolean): Boolean {
        if (throwException) {
            throw AccessException("access.exception.userHasNotRight", UserRightId.HR_VACATION, UserRightValue.READWRITE)
        }
        return false
    }

    companion object {
        private val ADDITIONAL_SEARCH_FIELDS = arrayOf("employee.user.firstname", "employee.user.lastname")
        private val DEFAULT_SORT_PROPERTIES = arrayOf(
            SortProperty("employee.user.firstname"),
            SortProperty("employee.user.lastname"),
            SortProperty("startDate", SortOrder.DESCENDING)
        )
    }
}
