/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.business.fibu;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Repository
public class EmployeeSalaryDao extends BaseDao<EmployeeSalaryDO>
{
  public static final UserRightId USER_RIGHT_ID = UserRightId.HR_EMPLOYEE_SALARY;

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "employee.user.lastname",
      "employee.user.firstname" };

  public EmployeeSalaryDao()
  {
    super(EmployeeSalaryDO.class);
    userRightId = USER_RIGHT_ID;
  }

  @Autowired
  private PfEmgrFactory pfEmgrFactory;

  @Autowired
  private EmployeeDao employeeDao;

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * List of all years with employee salaries: select min(year), max(year) from t_fibu_employee_salary.
   */
  @SuppressWarnings("unchecked")
  public int[] getYears()
  {
    List<Object[]> list = (List<Object[]>) getSession()
        .createQuery("select min(year), max(year) from EmployeeSalaryDO t").list();
    return SQLHelper.getYears(list);
  }

  @Override
  public List<EmployeeSalaryDO> getList(BaseSearchFilter filter)
  {
    final EmployeeSalaryFilter myFilter;
    if (filter instanceof EmployeeSalaryFilter) {
      myFilter = (EmployeeSalaryFilter) filter;
    } else {
      myFilter = new EmployeeSalaryFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (myFilter.getYear() >= 0) {
      queryFilter.add(Restrictions.eq("year", myFilter.getYear()));
      if (myFilter.getMonth() >= 0) {
        queryFilter.add(Restrictions.eq("month", myFilter.getMonth()));
      }
    }
    queryFilter.addOrder(Order.desc("year")).addOrder(Order.desc("month"));

    List<EmployeeSalaryDO> list = getList(queryFilter);
    return list;
  }

  /**
   * Sets the scales of percentage and currency amounts. <br/>
   * Gutschriftsanzeigen dürfen keine Rechnungsnummer haben. Wenn eine Rechnungsnummer für neue Rechnungen gegeben
   * wurde, so muss sie fortlaufend sein. Berechnet das Zahlungsziel in Tagen, wenn nicht gesetzt, damit es indiziert
   * wird.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#onSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void onSaveOrModify(EmployeeSalaryDO obj)
  {
    if (obj.getId() == null) {
      List list = getHibernateTemplate().find(
          "from EmployeeSalaryDO s where s.year = ? and s.month = ? and s.employee.id = ?",
          new Object[] { obj.getYear(), obj.getMonth(), obj.getEmployeeId() });
      if (CollectionUtils.isNotEmpty(list) == true) {
        throw new UserException("fibu.employee.salary.error.salaryAlreadyExist");
      }
    } else {
      List list = getHibernateTemplate().find(
          "from EmployeeSalaryDO s where s.year = ? and s.month = ? and s.employee.id = ? and id <> ?",
          new Object[] { obj.getYear(), obj.getMonth(), obj.getEmployeeId(), obj.getId() });
      if (CollectionUtils.isNotEmpty(list) == true) {
        throw new UserException("fibu.employee.salary.error.salaryAlreadyExist");
      }
    }
  }

  /**
   * @param employeeSalary
   * @param employeeId If null, then employee will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setEmployee(final EmployeeSalaryDO employeeSalary, Integer employeeId)
  {
    EmployeeDO employee = employeeDao.getOrLoad(employeeId);
    employeeSalary.setEmployee(employee);
  }

  @Override
  public EmployeeSalaryDO newInstance()
  {
    return new EmployeeSalaryDO();
  }

  public List<EmployeeSalaryDO> findByEmployee(EmployeeDO employee)
  {
    List<EmployeeSalaryDO> salaryList = new ArrayList<>();
    salaryList = pfEmgrFactory.runRoTrans(emgr -> {
      return emgr
          .createQuery(EmployeeSalaryDO.class, "from EmployeeSalaryDO sal where sal.employee = :emp", "emp", employee)
          .getResultList();
    });
    return salaryList;
  }
}
