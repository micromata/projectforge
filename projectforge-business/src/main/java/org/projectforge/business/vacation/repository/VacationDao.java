/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.business.vacation.VacationFilter;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.business.vacation.service.VacationValidator;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
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
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Objects;
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

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"employee.user.firstname", "employee.user.lastname"};

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VacationDao.class);

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private PfEmgrFactory emgrFactory;

  public VacationDao() {
    super(VacationDO.class);
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }


  @Override
  public VacationDO newInstance() {
    return new VacationDO();
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final VacationDO obj, final VacationDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
    if (accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READWRITE) ||
            obj == null || obj.getManager() != null && Objects.equals(obj.getManager().getUserId(), user.getId())) {
      // User is HR staff member or assigned manager.
      return true;
    }
    EmployeeDO employee = obj.getEmployee();
    if (employee == null || !Objects.equals(employee.getUserId(), user.getId())) {
      // User is not allowed to modify entries of other users.
      if (throwException) {
        throw new AccessException("access.exception.userHasNotRight", UserRightId.HR_VACATION, UserRightValue.READWRITE);
      }
      return false;
    }
    // User is owner of given object.
    if (operationType.isIn(OperationType.INSERT, OperationType.UPDATE, OperationType.UNDELETE)
            && !obj.isDeleted() && obj.getStatus() == VacationStatus.APPROVED) {
      if (oldObj == null || oldObj.getStatus() != VacationStatus.APPROVED) {
        // User tried to insert a new entry as approved or tries to approve a not yet approved entry.
        throw new AccessException(VacationValidator.Error.NOT_ALLOWED_TO_APPROVE.getMessageKey());
      }
      return false;
    }
    return true;
  }

  public boolean hasLoggedInUserHRVacationAccess() {
    return accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READWRITE);
  }

  @Override
  protected void onSave(VacationDO obj) {
    super.onSave(obj);
    VacationService service = applicationContext.getBean(VacationService.class);
    service.validate(obj, null, true);
  }

  @Override
  protected void onChange(VacationDO obj, VacationDO dbObj) {
    super.onChange(obj, dbObj);
    VacationService service = applicationContext.getBean(VacationService.class);
    service.validate(obj, dbObj, true);
  }

  public List<VacationDO> getVacationForPeriod(EmployeeDO employee, LocalDate startVacationDate, LocalDate endVacationDate, boolean withSpecial) {
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
      queryFilter.createJoin("replacement");
      queryFilter.add(QueryFilter.or(
              QueryFilter.eq("employee", employeeFromFilter),
              QueryFilter.eq("manager", employeeFromFilter),
              QueryFilter.eq("replacement.id", employeeId) // does not work with the whole employee object, need id
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
    final LocalDate startYear = LocalDate.of(year, Month.JANUARY, 1);
    final LocalDate endYear = LocalDate.of(year, Month.DECEMBER, 31);
    final List<VacationDO> result = emgrFactory.runRoTrans(emgr -> {
      String baseSQL = "SELECT v FROM VacationDO v WHERE v.employee = :employee AND v.startDate >= :startDate AND v.startDate <= :endDate";
      List<VacationDO> dbResultList = emgr.selectDetached(VacationDO.class, baseSQL + (withSpecial ? META_SQL_WITH_SPECIAL : META_SQL), "employee", employee,
              "startDate", startYear, "endDate", endYear,
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
}
