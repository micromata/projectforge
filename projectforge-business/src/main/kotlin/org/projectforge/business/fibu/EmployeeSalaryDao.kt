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

import jakarta.persistence.Tuple
import mu.KotlinLogging
import org.apache.commons.collections4.CollectionUtils
import org.projectforge.business.user.UserRightId
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.SortProperty.Companion.desc
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.utils.SQLHelper.getYearsByTupleOfYears
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
open class EmployeeSalaryDao : BaseDao<EmployeeSalaryDO>(EmployeeSalaryDO::class.java) {
    @Autowired
    private val employeeDao: EmployeeDao? = null

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    init {
        userRightId = USER_RIGHT_ID
    }

    val years: IntArray
        /**
         * List of all years with employee salaries: select min(year), max(year) from t_fibu_employee_salary.
         */
        get() {
            val minMaxDate = persistenceService.selectNamedSingleResult(
                EmployeeSalaryDO.SELECT_MIN_MAX_YEAR,
                Tuple::class.java,
            )
            return getYearsByTupleOfYears(minMaxDate)
        }

    override fun getList(filter: BaseSearchFilter, context: PfPersistenceContext): List<EmployeeSalaryDO> {
        val myFilter = if (filter is EmployeeSalaryFilter) {
            filter
        } else {
            EmployeeSalaryFilter(filter)
        }
        val queryFilter = QueryFilter(myFilter)
        if (myFilter.year != null) {
            queryFilter.add(eq("year", myFilter.year!!))
            if (myFilter.month != null) {
                queryFilter.add(eq("month", myFilter.month!!))
            }
        }
        queryFilter.addOrder(desc("year")).addOrder(desc("month"))

        val list = getList(queryFilter, context)
        return list
    }

    override fun onSaveOrModify(obj: EmployeeSalaryDO, context: PfPersistenceContext) {
        if (obj.id == null) {
            val list = persistenceService.executeQuery(
                "SELECT s FROM EmployeeSalaryDO s WHERE s.year = :year and s.month = :month and s.employee.id = :employeeid",
                EmployeeSalaryDO::class.java,
                Pair("year", obj.year),
                Pair("month", obj.month),
                Pair("employeeid", obj.employeeId),
            )
            if (CollectionUtils.isNotEmpty(list)) {
                log.info(
                    "Insert of EmployeeSalaryDO not possible. There is a existing one for employee with id: " + obj.employeeId + " and year: " +
                            obj.year + " and month: " + obj.month + " . Existing one: " + list[0].toString()
                )
                throw UserException("fibu.employee.salary.error.salaryAlreadyExist")
            }
        } else {
            val list = persistenceService.executeQuery(
                "SELECT s FROM EmployeeSalaryDO s WHERE s.year = :year and s.month = :month and s.employee.id = :employeeid and s.id <> :id",
                EmployeeSalaryDO::class.java,
                Pair("year", obj.year),
                Pair("month", obj.month),
                Pair("employeeid", obj.employeeId),
                Pair("id", obj.id),
            )
            if (CollectionUtils.isNotEmpty(list)) {
                log.info(
                    "Update of EmployeeSalaryDO not possible. There is a existing one for employee with id: " + obj.employeeId + " and year: " +
                            obj.year + " and month: " + obj.month + " and ID: " + obj.id + " . Existing one: " + list[0].toString()
                )
                throw UserException("fibu.employee.salary.error.salaryAlreadyExist")
            }
        }
    }

    /**
     * @param employeeSalary
     * @param employeeId     If null, then employee will be set to null;
     * @see BaseDao.getOrLoad
     */
    fun setEmployee(employeeSalary: EmployeeSalaryDO, employeeId: Long) {
        val employee = employeeDao!!.getOrLoad(employeeId)
        employeeSalary.employee = employee
    }

    override fun newInstance(): EmployeeSalaryDO {
        return EmployeeSalaryDO()
    }

    fun findByEmployee(employee: EmployeeDO?): List<EmployeeSalaryDO> {
        return persistenceService.executeQuery(
            "from EmployeeSalaryDO sal where sal.employee = :emp",
            EmployeeSalaryDO::class.java,
            Pair("emp", employee),
        )
    }

    companion object {
        val USER_RIGHT_ID: UserRightId = UserRightId.HR_EMPLOYEE_SALARY
        private val ADDITIONAL_SEARCH_FIELDS =
            arrayOf("employee.user.lastname", "employee.user.firstname", "employee.staffNumber")
    }
}
