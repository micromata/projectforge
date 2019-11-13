/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.business.vacation.VacationFilter;
import org.projectforge.business.vacation.model.VacationCalendarDO;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DAO für Urlaubsanträge.
 *
 * @author Florian Blumenstein
 */
@Repository
public class VacationDao extends BaseDao<VacationDO> {
  private final static String META_SQL = " AND v.special = false AND v.deleted = :deleted AND v.tenant = :tenant";

  private final static String META_SQL_WITH_SPECIAL = " AND v.deleted = :deleted AND v.tenant = :tenant";

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VacationDao.class);

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private PfEmgrFactory emgrFactory;

  public VacationDao() {
    super(VacationDO.class);
  }

  @Override
  public VacationDO newInstance() {
    return new VacationDO();
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final VacationDO obj, final VacationDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
    return true;
  }

  public boolean hasLoggedInUserHRVacationAccess() {
    return accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READWRITE);
  }

  public List<VacationDO> getVacationForPeriod(EmployeeDO employee, Date startVacationDate, Date endVacationDate, boolean withSpecial) {
    List<VacationDO> result = emgrFactory.runRoTrans(emgr -> {
      String baseSQL = "SELECT v FROM VacationDO v WHERE v.employee = :employee AND v.endDate >= :startDate AND v.startDate <= :endDate";
      List<VacationDO> dbResultList = emgr.selectDetached(VacationDO.class, baseSQL + (withSpecial ? META_SQL_WITH_SPECIAL : META_SQL), "employee", employee,
              "startDate", startVacationDate, "endDate", endVacationDate, "deleted", false, "tenant",
              getTenant());
      return dbResultList;
    });
    return result;
  }

  @Override
  public List<VacationDO> getList(final BaseSearchFilter filter) {
    final VacationFilter myFilter;
    if (filter instanceof VacationFilter) {
      myFilter = (VacationFilter) filter;
    } else {
      myFilter = new VacationFilter(filter);
    }
    final QueryFilter queryFilter = createQueryFilter(myFilter);
    if (!accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READONLY,
            UserRightValue.READWRITE)) {
      final Integer employeeId = myFilter.getEmployeeId();
      final EmployeeDO employeeFromFilter = emgrFactory.runRoTrans(emgr -> emgr.selectByPk(EmployeeDO.class, employeeId));
      queryFilter.createJoin("substitutions");
      queryFilter.add(QueryFilter.or(
              QueryFilter.eq("employee", employeeFromFilter),
              QueryFilter.eq("manager", employeeFromFilter),
              QueryFilter.eq("substitutions.id", employeeId) // does not work with the whole employee object, need id
      ));
    }
    if (myFilter.getVacationstatus() != null) {
      queryFilter.add(QueryFilter.eq("status", myFilter.getVacationstatus()));
    }
    queryFilter.addOrder(SortProperty.asc("startDate"));
    List<VacationDO> resultList = getList(queryFilter);
    if (myFilter.getVacationmode() != null) {
      resultList = resultList.stream().filter(vac -> vac.getVacationmode().equals(myFilter.getVacationmode())).collect(Collectors.toList());
    }
    return resultList;
  }

  public List<VacationDO> getActiveVacationForYear(EmployeeDO employee, int year, boolean withSpecial) {
    Calendar startYear = new GregorianCalendar(year, Calendar.JANUARY, 1);
    Calendar endYear = new GregorianCalendar(year, Calendar.DECEMBER, 31);
    final List<VacationDO> result = emgrFactory.runRoTrans(emgr -> {
      String baseSQL = "SELECT v FROM VacationDO v WHERE v.employee = :employee AND v.startDate >= :startDate AND v.startDate <= :endDate";
      List<VacationDO> dbResultList = emgr.selectDetached(VacationDO.class, baseSQL + (withSpecial ? META_SQL_WITH_SPECIAL : META_SQL), "employee", employee,
              "startDate", startYear.getTime(), "endDate", endYear.getTime(),
              "deleted", false, "tenant", getTenant());
      return dbResultList;
    });
    return result;
  }

  private TenantDO getTenant() {
    return ThreadLocalUserContext.getUser() != null && ThreadLocalUserContext.getUser().getTenant() != null ?
            ThreadLocalUserContext.getUser().getTenant() :
            tenantService.getDefaultTenant();
  }

  public List<VacationDO> getAllActiveVacation(EmployeeDO employee, boolean withSpecial) {
    final List<VacationDO> result = emgrFactory.runRoTrans(emgr -> {
      String baseSQL = "SELECT v FROM VacationDO v WHERE v.employee = :employee";
      List<VacationDO> dbResultList = emgr
              .selectDetached(VacationDO.class, baseSQL + (withSpecial ? META_SQL_WITH_SPECIAL : META_SQL), "employee", employee, "deleted", false, "tenant",
                      getTenant());
      return dbResultList;
    });
    return result;
  }

  public BigDecimal getOpenLeaveApplicationsForEmployee(EmployeeDO employee) {
    BigDecimal result = BigDecimal.ZERO;
    final List<VacationDO> resultList = emgrFactory.runRoTrans(emgr -> {
      String baseSQL = "SELECT v FROM VacationDO v WHERE v.manager = :employee AND v.status = :status";
      List<VacationDO> dbResultList = emgr
              .selectDetached(VacationDO.class, baseSQL + META_SQL_WITH_SPECIAL, "employee", employee, "status", VacationStatus.IN_PROGRESS, "deleted", false,
                      "tenant", getTenant());
      return dbResultList;
    });
    if (resultList != null) {
      result = new BigDecimal(resultList.size());
    }
    return result;
  }

  public List<VacationDO> getSpecialVacation(EmployeeDO employee, int year, VacationStatus status) {
    final Calendar startYear = new GregorianCalendar(year, Calendar.JANUARY, 1);
    final Calendar endYear = new GregorianCalendar(year, Calendar.DECEMBER, 31);
    final List<VacationDO> resultList = emgrFactory.runRoTrans(emgr -> {
      final String baseSQL = "SELECT v FROM VacationDO v WHERE v.employee = :employee AND v.startDate >= :startDate AND v.startDate <= :endDate AND v.status = :status AND v.special = :special";
      return emgr
              .selectDetached(VacationDO.class, baseSQL + META_SQL_WITH_SPECIAL, "employee", employee, "startDate", startYear.getTime(), "endDate",
                      endYear.getTime(), "status", status, "special", true, "deleted", false, "tenant", getTenant());
    });
    return resultList != null ? resultList : Collections.emptyList();
  }

  public List<TeamCalDO> getCalendarsForVacation(VacationDO vacation) {
    final List<TeamCalDO> calendarList = new ArrayList<>();
    if (vacation.getId() == null) {
      return calendarList;
    }
    final List<VacationCalendarDO> resultList = getVacationCalendarDOs(vacation);
    if (resultList != null && resultList.size() > 0) {
      resultList.forEach(res -> {
        if (!res.isDeleted())
          calendarList.add(res.getCalendar());
      });
    }
    return calendarList;
  }

  public List<VacationCalendarDO> getVacationCalendarDOs(VacationDO vacation) {
    final List<VacationCalendarDO> resultList = emgrFactory.runRoTrans(emgr -> {
      final String baseSQL = "SELECT vc FROM VacationCalendarDO vc WHERE vc.vacation = :vacation";
      return emgr.selectDetached(VacationCalendarDO.class, baseSQL, "vacation", vacation);
    });
    return resultList;
  }

  public void saveVacationCalendar(VacationCalendarDO obj) {
    try {
      emgrFactory.runInTrans(emgr -> {
        if (obj.getId() != null) {
          VacationCalendarDO vacationCalendarDO = emgr.selectByPkAttached(VacationCalendarDO.class, obj.getPk());
          vacationCalendarDO.setEvent(obj.getEvent());
          emgr.update(vacationCalendarDO);
        } else {
          emgr.insert(obj);
        }
        return null;
      });
    } catch (Exception ex) {
      log.error("Error while writing vacation event: " + ex.getMessage(), ex);
    }
  }

  public void markAsDeleted(VacationCalendarDO obj) {
    emgrFactory.runInTrans(emgr -> {
      emgr.markDeleted(obj);
      return null;
    });
  }

  public void markAsUndeleted(VacationCalendarDO obj) {
    emgrFactory.runInTrans(emgr -> {
      emgr.markUndeleted(obj);
      return null;
    });
  }
}
