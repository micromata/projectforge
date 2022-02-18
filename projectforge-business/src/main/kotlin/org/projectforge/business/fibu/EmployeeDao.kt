/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.Validate
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserRightId
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.attr.impl.InternalAttrSchemaConstants
import org.projectforge.framework.persistence.jpa.PfEmgr
import org.projectforge.framework.persistence.utils.SQLHelper.ensureUniqueResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*
import javax.persistence.NoResultException

/**
 * Ein Mitarbeiter ist einem ProjectForge-Benutzer zugeordnet und trägt einige buchhalterische Angaben.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class EmployeeDao : BaseDao<EmployeeDO>(EmployeeDO::class.java) {
  @Autowired
  private lateinit var userDao: UserDao

  @Autowired
  private lateinit var kost1Dao: Kost1Dao

  override fun getAdditionalSearchFields(): Array<String> {
    return ADDITIONAL_SEARCH_FIELDS
  }

  override fun getDefaultSortProperties(): Array<SortProperty> {
    return DEFAULT_SORT_PROPERTIES
  }

  open fun findByUserId(userId: Int?): EmployeeDO? {
    return ensureUniqueResult(
      em
        .createNamedQuery(EmployeeDO.FIND_BY_USER_ID, EmployeeDO::class.java)
        .setParameter("userId", userId)
    )
  }

  open fun getEmployeeIdByByUserId(userId: Int?): Int? {
    return ensureUniqueResult(
      em
        .createNamedQuery(EmployeeDO.GET_EMPLOYEE_ID_BY_USER_ID, Integer::class.java)
        .setParameter("userId", userId)
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
    return ensureUniqueResult(
      em
        .createNamedQuery(EmployeeDO.FIND_BY_LASTNAME_AND_FIRST_NAME, EmployeeDO::class.java)
        .setParameter("firstname", firstname)
        .setParameter("lastname", lastname)
    )
  }

  /**
   * @param employee
   * @param userId   If null, then user will be set to null;
   * @see BaseDao.getOrLoad
   */
  @Deprecated("")
  open fun setUser(employee: EmployeeDO, userId: Int?) {
    val user = userDao.getOrLoad(userId)
    employee.user = user
  }

  /**
   * @param employee
   * @param kost1Id  If null, then kost1 will be set to null;
   * @see BaseDao.getOrLoad
   */
  @Deprecated("")
  open fun setKost1(employee: EmployeeDO, kost1Id: Int?) {
    val kost1 = kost1Dao.getOrLoad(kost1Id)
    employee.kost1 = kost1
  }

  open fun internalGetEmployeeList(filter: BaseSearchFilter, showOnlyActiveEntries: Boolean = true): List<EmployeeDO> {
    val queryFilter = QueryFilter(filter)
    var employees = internalGetList(queryFilter)
    if (showOnlyActiveEntries) {
      val now = LocalDate.now()
      employees = employees.filter { employee ->
        val user = employee.user
        if (user == null || user.deactivated || user.isDeleted) {
          false
        } else if (employee.eintrittsDatum != null && now.isBefore(employee.eintrittsDatum)) {
          false
        } else {
          employee.austrittsDatum == null || !now.isAfter(employee.austrittsDatum)
        }
      }
    }
    return employees
  }

  override fun getList(filter: BaseSearchFilter): List<EmployeeDO> {
    val myFilter = if (filter is EmployeeFilter) filter else EmployeeFilter(filter)
    val queryFilter = QueryFilter(myFilter)
    var list = getList(queryFilter)
    val now = LocalDate.now()
    if (myFilter.isShowOnlyActiveEntries) {
      list = list.filter { employee ->
        if (employee.eintrittsDatum != null && now.isBefore(employee.eintrittsDatum)) {
          false
        } else employee.austrittsDatum == null || !now.isAfter(employee.austrittsDatum)
      }
    }
    for (employeeDO in list) {
      for (employeeTimedDO in employeeDO!!.timeableAttributes) {
        if (employeeTimedDO.groupName == InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME) {
          try {
            employeeDO.status = EmployeeStatus.findByi18nKey(employeeTimedDO.getAttribute("status") as String)
          } catch (e: Exception) {
            log.error("Exception while setting timeable status to deprecated status employee field. Message: " + e.message)
          }
        }
      }
    }
    return list
  }

  override fun newInstance(): EmployeeDO {
    return EmployeeDO()
  }

  fun newEmployeeTimeAttrRow(employee: EmployeeDO): EmployeeTimedDO {
    val nw = EmployeeTimedDO()
    nw.employee = employee
    employee.timeableAttributes.add(nw)
    return nw
  }

  open fun getEmployeeByStaffnumber(staffnumber: String): EmployeeDO? {
    var result: EmployeeDO? = null
    try {
      result = emgrFactory.runRoTrans { emgr: PfEmgr ->
        val baseSQL = "SELECT e FROM EmployeeDO e WHERE e.staffNumber = :staffNumber"
        emgr.selectSingleDetached(
          EmployeeDO::class.java, baseSQL + META_SQL, "staffNumber", staffnumber, "deleted", false
        )
      }
    } catch (ex: NoResultException) {
      log.warn("No employee found for staffnumber: $staffnumber")
    }
    return result
  }

  init {
    userRightId = USER_RIGHT_ID
  }

  companion object {
    val USER_RIGHT_ID = UserRightId.HR_EMPLOYEE
    private val log = LoggerFactory.getLogger(EmployeeDao::class.java)
    private val ADDITIONAL_SEARCH_FIELDS = arrayOf(
      "user.firstname", "user.lastname", "user.username",
      "user.description",
      "user.organization"
    )
    private val DEFAULT_SORT_PROPERTIES = arrayOf(SortProperty("user.firstname"), SortProperty("user.lastname"))
    private const val META_SQL = " AND e.deleted = :deleted"
  }
}
