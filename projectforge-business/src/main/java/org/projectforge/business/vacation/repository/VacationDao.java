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
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.business.vacation.VacationFilter;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.business.vacation.service.VacationValidator;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * DAO für Urlaubsanträge.
 *
 * @author Florian Blumenstein, Kai Reinhard
 */
@Repository
public class VacationDao extends BaseDao<VacationDO> {
  private final static String META_SQL = " AND v.special = false AND v.deleted = :deleted AND v.tenant = :tenant";

  private final static String META_SQL_WITH_SPECIAL = " AND v.deleted = :deleted AND v.tenant = :tenant";

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"employee.user.firstname", "employee.user.lastname"};

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VacationDao.class);

  private static final SortProperty[] DEFAULT_SORT_PROPERTIES = new SortProperty[]{
          new SortProperty("employee.user.firstname"),
          new SortProperty("employee.user.lastname"),
          new SortProperty("startDate", SortOrder.DESCENDING)};

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private EmployeeDao employeeDao;

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
  public SortProperty[] getDefaultSortProperties() {
    return DEFAULT_SORT_PROPERTIES;
  }

  @Override
  public VacationDO newInstance() {
    return new VacationDO();
  }

  @Override
  public boolean hasHistoryAccess(PFUserDO user, VacationDO obj, boolean throwException) {
    if (hasHrRights(user, obj) || isOwnEntry(user, obj) || isManager(user, obj)) {
      return true;
    }
    return throwOrReturn(throwException);
  }

  @Override
  public boolean hasUserSelectAccess(PFUserDO user, VacationDO obj, boolean throwException) {
    if (hasHrRights(user, obj) || isOwnEntry(user, obj) || isManager(user, obj) || isReplacement(user, obj)) {
      return true;
    }
    if (obj.getEmployee() != null && accessChecker.areUsersInSameGroup(user, obj.getEmployee().getUser())) {
      return true;
    }
    return throwOrReturn(throwException);
  }

  @Override
  public boolean hasInsertAccess(PFUserDO user, VacationDO obj, boolean throwException) {
    return hasUpdateAccess(user, obj, null, throwException);
  }

  @Override
  public boolean hasUpdateAccess(PFUserDO user, VacationDO obj, VacationDO dbObj, boolean throwException) {
    if (hasHrRights(user, obj)) {
      return true; // HR staff member are allowed to do everything.
    }
    if (!isOwnEntry(user, obj, dbObj)) {
      if (dbObj != null && isManager(user, obj, dbObj)) {
        if (Objects.equals(obj.getStartDate(), dbObj.getStartDate()) &&
                Objects.equals(obj.getEndDate(), dbObj.getEndDate()) &&
                obj.getSpecial() == dbObj.getSpecial() &&
                Objects.equals(obj.getEmployeeId(), dbObj.getEmployeeId()) &&
                obj.getHalfDayBegin() == dbObj.getHalfDayBegin() &&
                obj.getHalfDayEnd() == dbObj.getHalfDayEnd() &&
                (obj.getSpecial() == null || !obj.getSpecial())) {
          // Manager is only allowed to change status and replacement, but not allowed to approve special vacations.
          return true;
        }
        return throwOrReturn(throwException); // Normal user isn't allowed to insert foreign entries.
      }
      return throwOrReturn(throwException); // Normal user isn't allowed to insert foreign entries.
    }
    if (obj.getStatus() == VacationStatus.APPROVED) {
      // Normal user isn't allowed to insert/update approved entries:
      return throwOrReturn(VacationValidator.Error.NOT_ALLOWED_TO_APPROVE.getMessageKey(), throwException);
    }
    if (obj.getStartDate().isBefore(LocalDate.now())) {
      // Normal user isn't allowed to insert/update old entries.
      return throwOrReturn(throwException); // Normal user isn't allowed to insert foreign entries.
    }
    return true;
  }

  @Override
  public boolean hasDeleteAccess(PFUserDO user, VacationDO obj, VacationDO dbObj, boolean throwException) {
    if (hasHrRights(user, obj)) {
      return true;
    }
    if (!isOwnEntry(user, obj, dbObj)) {
      return throwOrReturn(throwException); // Normal user isn't allowed to insert foreign entries.
    }
    if (obj.getStatus() == VacationStatus.APPROVED && obj.getStartDate().isBefore(LocalDate.now())) {
      // The user isn't allowed to delete approved entries of the past.
      return throwOrReturn(throwException);
    }
    return true;
  }

  public boolean hasLoggedInUserHRVacationAccess() {
    return accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READWRITE);
  }

  private boolean hasHrRights(final PFUserDO loggedInUser, final VacationDO obj) {
    return accessChecker.hasRight(loggedInUser, UserRightId.HR_VACATION, false, UserRightValue.READWRITE);
  }

  private boolean isOwnEntry(final PFUserDO loggedInUser, final VacationDO obj, final VacationDO oldObj) {
    if (!isOwnEntry(loggedInUser, obj)) {
      return false;
    }
    if (oldObj != null) {
      return isOwnEntry(loggedInUser, oldObj);
    }
    return true;
  }

  private boolean isOwnEntry(final PFUserDO loggedInUser, final VacationDO obj) {
    EmployeeDO employee = obj.getEmployee();
    if (employee == null) {
      return false;
    }
    if (employee.getUserId() == null) {
      // Object wasn't loaded from data base:
      employee = employeeDao.internalGetById(employee.getId());
    }
    return Objects.equals(employee.getUserId(), loggedInUser.getId());
  }

  private boolean isManager(final PFUserDO loggedInUser, final VacationDO obj, final VacationDO oldObj) {
    if (!isManager(loggedInUser, obj)) {
      return false;
    }
    if (oldObj != null) {
      return isManager(loggedInUser, oldObj);
    }
    return true;
  }

  private boolean isManager(final PFUserDO loggedInUser, final VacationDO obj) {
    EmployeeDO manager = obj.getManager();
    if (manager == null) {
      return false;
    }
    if (manager.getUserId() == null) {
      // Object wasn't loaded from data base:
      manager = employeeDao.internalGetById(manager.getId());
    }
    return Objects.equals(manager.getUserId(), loggedInUser.getId());
  }

  private boolean isReplacement(final PFUserDO loggedInUser, final VacationDO obj) {
    EmployeeDO replacement = obj.getReplacement();
    if (replacement == null) {
      return false;
    }
    if (replacement.getUserId() == null) {
      // Object wasn't loaded from data base:
      replacement = employeeDao.internalGetById(replacement.getId());
    }
    return Objects.equals(replacement.getUserId(), loggedInUser.getId());
  }

  private boolean throwOrReturn(boolean throwException) {
    if (throwException) {
      throw new AccessException("access.exception.userHasNotRight", UserRightId.HR_VACATION, UserRightValue.READWRITE);
    }
    return false;
  }

  private boolean throwOrReturn(final String messageKey, boolean throwException) {
    if (throwException) {
      throw new AccessException(messageKey);
    }
    return false;
  }

  @Override
  protected void onSaveOrModify(VacationDO obj) {
    super.onSaveOrModify(obj);
    if (obj.getSpecial() == null) {
      obj.setSpecial(false); // Avoid null value of special.
    }
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

  public List<VacationDO> getVacationForPeriod(Integer employeeId, LocalDate startVacationDate, LocalDate endVacationDate, boolean withSpecial) {
    List<VacationDO> result = emgrFactory.runRoTrans(emgr -> {
      String baseSQL = "SELECT v FROM VacationDO v WHERE v.employee.id = :employeeId AND v.endDate >= :startDate AND v.startDate <= :endDate";
      List<VacationDO> dbResultList = emgr.selectDetached(VacationDO.class, baseSQL + (withSpecial ? META_SQL_WITH_SPECIAL : META_SQL), "employeeId", employeeId,
              "startDate", startVacationDate, "endDate", endVacationDate, "deleted", false, "tenant",
              getTenant());
      return dbResultList;
    });
    return result;
  }

  public List<VacationDO> getVacationForPeriod(EmployeeDO employee, LocalDate startVacationDate, LocalDate endVacationDate, boolean withSpecial) {
    return getVacationForPeriod(employee.getId(), startVacationDate, endVacationDate, withSpecial);
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
      String baseSQL = "SELECT v FROM VacationDO v WHERE v.employee = :employee AND v.endDate >= :startDate AND v.startDate <= :endDate";
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

  public int getOpenLeaveApplicationsForEmployee(EmployeeDO employee) {
    final List<VacationDO> resultList = emgrFactory.runRoTrans(emgr -> {
      String baseSQL = "SELECT v FROM VacationDO v WHERE v.manager = :employee AND v.status = :status";
      List<VacationDO> dbResultList = emgr
              .selectDetached(VacationDO.class, baseSQL + META_SQL_WITH_SPECIAL, "employee", employee, "status", VacationStatus.IN_PROGRESS, "deleted", false,
                      "tenant", getTenant());
      return dbResultList;
    });
    if (resultList != null) {
      return resultList.size();
    }
    return 0;
  }
}
