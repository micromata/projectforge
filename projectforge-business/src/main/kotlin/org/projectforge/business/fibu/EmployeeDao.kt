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

import jakarta.persistence.NoResultException
import mu.KotlinLogging
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.Validate
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserRightId
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Ein Mitarbeiter ist einem ProjectForge-Benutzer zugeordnet und trägt einige buchhalterische Angaben.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class EmployeeDao : BaseDao<EmployeeDO>(EmployeeDO::class.java) {
    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var kost1Dao: Kost1Dao

    // Set by EmployeeService in PostConstruct for avoiding circular dependencies.
    internal lateinit var employeeService: EmployeeService

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    override val defaultSortProperties: Array<SortProperty>
        get() = DEFAULT_SORT_PROPERTIES

    open fun findAllActive(checkAccess: Boolean): List<EmployeeDO> {
        val result = internalGetEmployeeList(EmployeeFilter(), true)
        if (checkAccess) {
            return result.filter { hasSelectAccess(it, ThreadLocalUserContext.requiredLoggedInUser) }
        } else {
            return result
        }
    }

    open fun getEmployeeStatus(employee: EmployeeDO): EmployeeStatus? {
        return employeeService.getEmployeeStatus(employee)
    }

    open fun findByUserId(userId: Int?): EmployeeDO? {
        val employee = persistenceService.selectNamedSingleResult(
            EmployeeDO.FIND_BY_USER_ID,
            EmployeeDO::class.java,
            Pair("userId", userId),
        )
        setEmployeeStatus(employee)
        return employee
    }

    open fun getEmployeeIdByByUserId(userId: Int?): Int? {
        userId ?: return null
        return persistenceService.selectNamedSingleResult(
            EmployeeDO.GET_EMPLOYEE_ID_BY_USER_ID,
            java.lang.Integer::class.java,
            Pair("userId", userId),
        )?.toInt()
    }


    /**
     * If more than one employee is found, null will be returned.
     *
     * @param fullname Format: &lt;last name&gt;, &lt;first name&gt;
     */
    open fun findByName(fullname: String): EmployeeDO? {
        val tokenizer = StringTokenizer(fullname, ",")
        if (tokenizer.countTokens() != 2) {
            log.error("EmployeeDao.getByName: Token '$fullname' not supported.")
        }
        Validate.isTrue(tokenizer.countTokens() == 2)
        val lastname = tokenizer.nextToken().trim { it <= ' ' }
        val firstname = tokenizer.nextToken().trim { it <= ' ' }
        val employee = persistenceService.selectNamedSingleResult(
            EmployeeDO.FIND_BY_LASTNAME_AND_FIRST_NAME,
            EmployeeDO::class.java,
            Pair("firstname", firstname),
            Pair("lastname", lastname),
        )
        setEmployeeStatus(employee)
        return employee
    }

    /**
     * @param employee
     * @param userId   If null, then user will be set to null;
     * @see BaseDao.getOrLoad
     */
    @Deprecated("")
    open fun setUser(employee: EmployeeDO, userId: Int) {
        val user = userDao.getOrLoad(userId)
        employee.user = user
    }

    /**
     * @param employee
     * @param kost1Id  If null, then kost1 will be set to null;
     * @see BaseDao.getOrLoad
     */
    @Deprecated("")
    open fun setKost1(employee: EmployeeDO, kost1Id: Int) {
        val kost1 = kost1Dao.getOrLoad(kost1Id)
        employee.kost1 = kost1
    }

    open fun internalGetEmployeeList(
        filter: BaseSearchFilter,
        showOnlyActiveEntries: Boolean = true
    ): List<EmployeeDO> {
        val queryFilter = QueryFilter(filter)
        var employees = internalGetList(queryFilter)
        if (showOnlyActiveEntries) {
            val now = LocalDate.now()
            employees = employees.filter { employee ->
                val user = employee.user
                if (employee.austrittsDatum == null && (user == null || user.deactivated || user.deleted)) {
                    false
                    // } else if (employee.eintrittsDatum != null && now.isBefore(employee.eintrittsDatum)) {
                    //  false
                } else {
                    // Show also deleted users if austrittsdatum not long ago
                    employee.austrittsDatum == null || !now.minusMonths(3).isAfter(employee.austrittsDatum)
                }
            }
        }
        setEmployeeStatus(employees)
        return employees
    }

    /**
     * Sets the (deprecated) employee status from timeableAttributes.
     */
    private fun setEmployeeStatus(employees: List<EmployeeDO>) {
        for (employeeDO in employees) {
            setEmployeeStatus(employeeDO)
        }
    }

    /**
     * Sets the employee status from validity period attrs.
     */
    open fun setEmployeeStatus(employeeDO: EmployeeDO?) {
        employeeDO ?: return
        employeeDO.status = employeeService.getEmployeeStatus(employeeDO)
    }

    override fun getList(filter: BaseSearchFilter): List<EmployeeDO> {
        val myFilter = if (filter is EmployeeFilter) filter else EmployeeFilter(filter)
        val queryFilter = QueryFilter(myFilter)
        var list = getList(queryFilter)
        if (myFilter.isShowOnlyActiveEntries) {
            list = list.filter { it.active }
        }
        setEmployeeStatus(list)
        return list
    }

    override fun newInstance(): EmployeeDO {
        return EmployeeDO()
    }

    open fun getEmployeeByStaffnumber(staffnumber: String): EmployeeDO? {
        var result: EmployeeDO? = null
        try {
            val baseSQL = "SELECT e FROM EmployeeDO e WHERE e.staffNumber = :staffNumber"
            result = persistenceService.selectSingleResult(
                "$baseSQL$META_SQL",
                EmployeeDO::class.java,
                Pair("staffNumber", staffnumber),
            )
        } catch (ex: NoResultException) {
            log.warn("No employee found for staffnumber: $staffnumber")
        }
        setEmployeeStatus(result)
        return result
    }

    override fun isAutocompletionPropertyEnabled(property: String?): Boolean {
        return ArrayUtils.contains(ENABLED_AUTOCOMPLETION_PROPERTIES, property)
    }

    init {
        userRightId = USER_RIGHT_ID
    }

    companion object {
        val USER_RIGHT_ID = UserRightId.HR_EMPLOYEE
        private val ADDITIONAL_SEARCH_FIELDS = arrayOf(
            "user.firstname", "user.lastname", "user.username",
            "user.description",
            "user.organization"
        )
        private val DEFAULT_SORT_PROPERTIES = arrayOf(SortProperty("user.firstname"), SortProperty("user.lastname"))
        private const val META_SQL = " AND e.deleted = :deleted"
        private val ENABLED_AUTOCOMPLETION_PROPERTIES = arrayOf("abteilung", "position")
    }
}
