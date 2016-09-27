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

package org.projectforge.business.vacation.repository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.vacation.VacationFilter;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * DAO für Urlaubsanträge.
 *
 * @author Florian Blumenstein
 *
 */
@Repository
public class VacationDao extends BaseDao<VacationDO>
{
  private final static String META_SQL = " AND v.deleted = :deleted AND v.tenant = :tenant";

  @Autowired
  private PfEmgrFactory emgrFactory;

  public VacationDao()
  {
    super(VacationDO.class);
  }

  @Override
  public VacationDO newInstance()
  {
    return new VacationDO();
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final VacationDO obj, final VacationDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    return true;
  }

  public List<VacationDO> getVacationForPeriod(EmployeeDO employee, Date startVacationDate, Date endVacationDate)
  {
    List<VacationDO> result = new ArrayList<>();
    result = emgrFactory.runRoTrans(emgr -> {
      String baseSQL = "SELECT v FROM VacationDO v WHERE v.employee = :employee AND v.endDate >= :startDate AND v.startDate <= :endDate";
      List<VacationDO> dbResultList = emgr.selectDetached(VacationDO.class, baseSQL + META_SQL, "employee", employee,
          "startDate", startVacationDate, "endDate", endVacationDate, "deleted", false, "tenant",
          ThreadLocalUserContext.getUser().getTenant());
      return dbResultList;
    });
    return result;
  }

  @Override
  public List<VacationDO> getList(final BaseSearchFilter filter)
  {
    final VacationFilter myFilter;
    if (filter instanceof VacationFilter) {
      myFilter = (VacationFilter) filter;
    } else {
      myFilter = new VacationFilter(filter);
    }
    final QueryFilter queryFilter = createQueryFilter(myFilter);
    queryFilter.add(Restrictions.or(
        Restrictions.eq("employee", myFilter.getEmployeeForUser()),
        Restrictions.eq("manager", myFilter.getEmployeeForUser())));
    queryFilter.addOrder(Order.asc("startDate"));
    return getList(queryFilter);
  }

  public List<VacationDO> getActiveVacationForCurrentYear(EmployeeDO employee)
  {
    List<VacationDO> result = new ArrayList<>();
    Calendar today = new GregorianCalendar();
    Calendar startYear = new GregorianCalendar(today.get(Calendar.YEAR), Calendar.JANUARY, 1);
    Calendar endYear = new GregorianCalendar(today.get(Calendar.YEAR), Calendar.DECEMBER, 31);
    result = emgrFactory.runRoTrans(emgr -> {
      String baseSQL = "SELECT v FROM VacationDO v WHERE v.employee = :employee AND v.startDate >= :startDate AND v.endDate <= :endDate";
      List<VacationDO> dbResultList = emgr.selectDetached(VacationDO.class, baseSQL + META_SQL, "employee", employee,
          "startDate", startYear.getTime(), "endDate", endYear.getTime(),
          "deleted", false, "tenant", ThreadLocalUserContext.getUser().getTenant());
      return dbResultList;
    });
    return result;
  }

}