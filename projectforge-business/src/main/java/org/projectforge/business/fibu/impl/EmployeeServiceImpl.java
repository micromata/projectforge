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

package org.projectforge.business.fibu.impl;

import de.micromata.genome.db.jpa.tabattr.api.AttrSchemaService;
import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import org.apache.commons.collections.CollectionUtils;
import org.projectforge.business.fibu.*;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost1Dao;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.attr.impl.InternalAttrSchemaConstants;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.impl.CorePersistenceServiceImpl;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Standard implementation of the Employee service interface.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Service
public class EmployeeServiceImpl extends CorePersistenceServiceImpl<Integer, EmployeeDO>
        implements EmployeeService {
  private static final BigDecimal FULL_TIME_WEEKLY_WORKING_HOURS = new BigDecimal(40);

  private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal(12);

  @Autowired
  private UserDao userDao;

  @Autowired
  private Kost1Dao kost1Dao;

  @Autowired
  private EmployeeDao employeeDao;

  @Autowired
  private AttrSchemaService attrSchemaService;

  @Autowired
  private TimeableService timeableService;

  @Autowired
  private VacationService vacationService;

  @Autowired
  private TimesheetDao timesheetDao;

  @Override
  public List<EmployeeDO> getList(BaseSearchFilter filter) {
    return employeeDao.getList(filter);
  }

  @Override
  public void setPfUser(EmployeeDO employee, Integer userId) {
    final PFUserDO user = userDao.getOrLoad(userId);
    employee.setUser(user);
  }

  @Override
  public EmployeeTimedDO addNewTimeAttributeRow(final EmployeeDO employee, final String groupName) {
    final EmployeeTimedDO nw = new EmployeeTimedDO();
    nw.setEmployee(employee);
    nw.setGroupName(groupName);
    employee.getTimeableAttributes().add(nw);
    return nw;
  }

  @Override
  public EmployeeDO getEmployeeByUserId(Integer userId) {
    return employeeDao.findByUserId(userId);
  }

  @Override
  public ModificationStatus updateAttribute(Integer userId, Object attribute, String attributeName) {
    EmployeeDO employeeDO = getEmployeeByUserId(userId);
    try {
      Class<?> type = EmployeeDO.class.getDeclaredField(attributeName).getType();
      Method declaredMethod = EmployeeDO.class.getDeclaredMethod(
              "set" + attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1, attributeName.length()),
              type);
      declaredMethod.invoke(employeeDO, type.cast(attribute));

    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }

    ArrayList<String> attributes = new ArrayList<>();

    for (Field field : EmployeeDO.class.getDeclaredFields()) {
      attributes.add(field.getName());
    }

    attributes.removeIf((s) -> {
      return s.equals(attributeName);
    });
    return update(employeeDO, attributes.toArray(new String[]{}));
  }

  @Override
  public void setKost1(EmployeeDO employee, final Integer kost1Id) {
    final Kost1DO kost1 = kost1Dao.getOrLoad(kost1Id);
    employee.setKost1(kost1);
  }

  @Override
  public boolean hasLoggedInUserInsertAccess() {
    return employeeDao.hasLoggedInUserInsertAccess();
  }

  @Override
  public boolean hasLoggedInUserInsertAccess(EmployeeDO obj, boolean throwException) {
    return employeeDao.hasLoggedInUserInsertAccess(obj, throwException);
  }

  @Override
  public boolean hasLoggedInUserUpdateAccess(EmployeeDO obj, EmployeeDO dbObj, boolean throwException) {
    return employeeDao.hasLoggedInUserUpdateAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasLoggedInUserDeleteAccess(EmployeeDO obj, EmployeeDO dbObj, boolean throwException) {
    return employeeDao.hasLoggedInUserDeleteAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasDeleteAccess(PFUserDO user, EmployeeDO obj, EmployeeDO dbObj, boolean throwException) {
    return employeeDao.hasDeleteAccess(user, obj, dbObj, throwException);
  }

  @Override
  public EmployeeDO getById(Serializable id) throws AccessException {
    return employeeDao.getById(id);
  }

  @Override
  public List<String> getAutocompletion(String property, String searchString) {
    return employeeDao.getAutocompletion(property, searchString);
  }

  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(EmployeeDO obj) {
    return employeeDao.getDisplayHistoryEntries(obj);
  }

  @Override
  public void rebuildDatabaseIndex4NewestEntries() {
    employeeDao.rebuildDatabaseIndex4NewestEntries();
  }

  @Override
  public void rebuildDatabaseIndex() {
    employeeDao.rebuildDatabaseIndex();
  }

  @Override
  public boolean isEmployeeActive(final EmployeeDO employee) {
    if (employee.getAustrittsDatum() == null) {
      return true;
    }
    final Calendar now = Calendar.getInstance();
    final Calendar austrittsdatum = Calendar.getInstance();
    austrittsdatum.setTime(employee.getAustrittsDatum());
    return now.before(austrittsdatum);
  }

  @Override
  public BigDecimal getMonthlySalary(EmployeeDO employee, Calendar selectedDate) {
    final EmployeeTimedDO attribute = timeableService.getAttrRowValidAtDate(employee, "annuity", selectedDate.getTime());
    final BigDecimal annualSalary = attribute != null ? attribute.getAttribute("annuity", BigDecimal.class) : null;
    final BigDecimal weeklyWorkingHours = employee.getWeeklyWorkingHours();

    if (annualSalary != null && weeklyWorkingHours != null && BigDecimal.ZERO.compareTo(weeklyWorkingHours) < 0) {
      // do the multiplication before the division to minimize rounding problems
      // we need a rounding mode to avoid ArithmeticExceptions when the exact result cannot be represented in the result
      return annualSalary
              .multiply(weeklyWorkingHours)
              .divide(MONTHS_PER_YEAR, BigDecimal.ROUND_HALF_UP)
              .divide(FULL_TIME_WEEKLY_WORKING_HOURS, BigDecimal.ROUND_HALF_UP);
    }

    return null;
  }

  @Override
  public List<EmployeeDO> findAllActive(final boolean checkAccess) {
    final Collection<EmployeeDO> employeeList;
    if (checkAccess) {
      employeeList = employeeDao.getList(new EmployeeFilter());
    } else {
      employeeList = employeeDao.internalLoadAll();
    }
    return employeeList.stream()
            .filter(this::isEmployeeActive)
            .collect(Collectors.toList());
  }

  @Override
  public EmployeeDO getEmployeeByStaffnumber(String staffnumber) {
    return employeeDao.getEmployeeByStaffnumber(staffnumber);
  }

  @Override
  public List<EmployeeDO> getAll(boolean checkAccess) {
    return checkAccess ? employeeDao.getList(new EmployeeFilter()) : employeeDao.internalLoadAll();
  }

  @Override
  public EmployeeStatus getEmployeeStatus(final EmployeeDO employee) {
    final EmployeeTimedDO attrRow = timeableService
            .getAttrRowValidAtDate(employee, InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME, new Date());
    if (attrRow != null && StringUtils.isEmpty(attrRow.getStringAttribute(InternalAttrSchemaConstants.EMPLOYEE_STATUS_DESC_NAME)) == false) {
      return EmployeeStatus.findByi18nKey(attrRow.getStringAttribute(InternalAttrSchemaConstants.EMPLOYEE_STATUS_DESC_NAME));
    }
    return null;
  }

  @Override
  public String getStudentVacationCountPerDay(EmployeeDO currentEmployee) {
    String vacationCountPerDay = "";
    Calendar now = new GregorianCalendar(ThreadLocalUserContext.getTimeZone());
    Calendar eintrittsDatum = new GregorianCalendar(ThreadLocalUserContext.getTimeZone());
    Calendar deadline = new GregorianCalendar(ThreadLocalUserContext.getTimeZone());

    eintrittsDatum.setTime(currentEmployee.getEintrittsDatum());
    deadline.add(Calendar.MONTH, -7);
    now.add(Calendar.MONTH, -1);

    if (eintrittsDatum.before(now)) {
      if (eintrittsDatum.before(deadline)) {
        if (now.get(Calendar.MONTH) >= Calendar.JUNE) {
          vacationCountPerDay = vacationService
                  .getVacationCount(now.get(Calendar.YEAR), now.get(Calendar.MONTH) - 5, now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                          currentEmployee.getUser());
        } else {
          vacationCountPerDay = vacationService
                  .getVacationCount(now.get(Calendar.YEAR) - 1, 12 - (6 - now.get(Calendar.MONTH) + 1), now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                          currentEmployee.getUser());
        }
      } else {
        vacationCountPerDay = vacationService
                .getVacationCount(eintrittsDatum.get(Calendar.YEAR), eintrittsDatum.get(Calendar.MONTH), now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                        currentEmployee.getUser());
      }
    }
    return vacationCountPerDay;
  }

  @Override
  public MonthlyEmployeeReport getReportOfMonth(final int year, final int month, final PFUserDO user) {
    MonthlyEmployeeReport monthlyEmployeeReport = new MonthlyEmployeeReport(this, vacationService, user, year, month);
    monthlyEmployeeReport.init();
    TimesheetFilter filter = new TimesheetFilter();
    filter.setDeleted(false);
    filter.setStartTime(monthlyEmployeeReport.getFromDate());
    filter.setStopTime(monthlyEmployeeReport.getToDate());
    filter.setUserId(user.getId());
    List<TimesheetDO> list = timesheetDao.getList(filter);
    if (CollectionUtils.isNotEmpty(list) == true) {
      for (TimesheetDO sheet : list) {
        monthlyEmployeeReport.addTimesheet(sheet);
      }
    }
    monthlyEmployeeReport.calculate();
    return monthlyEmployeeReport;
  }

  @Override
  public boolean isFulltimeEmployee(final EmployeeDO employee, final Calendar selectedDate) {
    final Calendar date = (Calendar) selectedDate.clone(); // create a clone to avoid changing the original object
    final Date startOfMonth = date.getTime();
    date.add(Calendar.MONTH, 1);
    date.add(Calendar.DATE, -1);
    final Date endOfMonth = date.getTime();

    final List<EmployeeTimedDO> attrRows = timeableService
            .getAttrRowsWithinDateRange(employee, InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME, startOfMonth, endOfMonth);

    final EmployeeTimedDO rowValidAtBeginOfMonth = timeableService
            .getAttrRowValidAtDate(employee, InternalAttrSchemaConstants.EMPLOYEE_STATUS_GROUP_NAME, selectedDate.getTime());

    if (rowValidAtBeginOfMonth != null) {
      attrRows.add(rowValidAtBeginOfMonth);
    }

    return attrRows
            .stream()
            .map(row -> row.getStringAttribute(InternalAttrSchemaConstants.EMPLOYEE_STATUS_DESC_NAME))
            .filter(Objects::nonNull)
            .anyMatch(s -> EmployeeStatus.FEST_ANGESTELLTER.getI18nKey().equals(s) || EmployeeStatus.BEFRISTET_ANGESTELLTER.getI18nKey().equals(s));
  }

}
