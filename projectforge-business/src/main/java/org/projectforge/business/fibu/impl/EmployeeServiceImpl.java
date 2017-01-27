package org.projectforge.business.fibu.impl;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.EmployeeFilter;
import org.projectforge.business.fibu.EmployeeTimedDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost1Dao;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.impl.CorePersistenceServiceImpl;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.micromata.genome.db.jpa.tabattr.api.AttrSchemaService;
import de.micromata.genome.db.jpa.tabattr.api.TimeableService;

/**
 * Standard implementation of the Employee service interface.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Service
public class EmployeeServiceImpl extends CorePersistenceServiceImpl<Integer, EmployeeDO>
    implements EmployeeService
{
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

  @Override
  public ModificationStatus update(EmployeeDO obj) throws AccessException
  {
    ModificationStatus mod = super.update(obj);
    if (mod != ModificationStatus.NONE) {
      TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache().refreshEmployee(obj.getUserId());
    }
    return mod;
  }

  @Override
  public List<EmployeeDO> getList(BaseSearchFilter filter)
  {
    return employeeDao.getList(filter);
  }

  @Override
  public void setPfUser(EmployeeDO employee, Integer userId)
  {
    final PFUserDO user = userDao.getOrLoad(userId);
    employee.setUser(user);
  }

  @Override
  public EmployeeTimedDO addNewTimeAttributeRow(final EmployeeDO employee, final String groupName)
  {
    final EmployeeTimedDO nw = new EmployeeTimedDO();
    nw.setEmployee(employee);
    nw.setGroupName(groupName);
    employee.getTimeableAttributes().add(nw);
    return nw;
  }

  @Override
  public EmployeeDO getEmployeeByUserId(Integer userId)
  {
    return employeeDao.findByUserId(userId);
  }

  @Override
  public ModificationStatus updateAttribute(Integer userId, Object attribute, String attributeName)
  {
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
    return update(employeeDO, attributes.toArray(new String[] {}));
  }

  @Override
  public void setKost1(EmployeeDO employee, final Integer kost1Id)
  {
    final Kost1DO kost1 = kost1Dao.getOrLoad(kost1Id);
    employee.setKost1(kost1);
  }

  @Override
  public boolean hasLoggedInUserInsertAccess()
  {
    return employeeDao.hasLoggedInUserInsertAccess();
  }

  @Override
  public boolean hasLoggedInUserInsertAccess(EmployeeDO obj, boolean throwException)
  {
    return employeeDao.hasLoggedInUserInsertAccess(obj, throwException);
  }

  @Override
  public boolean hasLoggedInUserUpdateAccess(EmployeeDO obj, EmployeeDO dbObj, boolean throwException)
  {
    return employeeDao.hasLoggedInUserUpdateAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasLoggedInUserDeleteAccess(EmployeeDO obj, EmployeeDO dbObj, boolean throwException)
  {
    return employeeDao.hasLoggedInUserDeleteAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasDeleteAccess(PFUserDO user, EmployeeDO obj, EmployeeDO dbObj, boolean throwException)
  {
    return employeeDao.hasDeleteAccess(user, obj, dbObj, throwException);
  }

  @Override
  public EmployeeDO getById(Serializable id) throws AccessException
  {
    return employeeDao.getById(id);
  }

  @Override
  public List<String> getAutocompletion(String property, String searchString)
  {
    return employeeDao.getAutocompletion(property, searchString);
  }

  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(EmployeeDO obj)
  {
    return employeeDao.getDisplayHistoryEntries(obj);
  }

  @Override
  public void rebuildDatabaseIndex4NewestEntries()
  {
    employeeDao.rebuildDatabaseIndex4NewestEntries();
  }

  @Override
  public void rebuildDatabaseIndex()
  {
    employeeDao.rebuildDatabaseIndex();
  }

  @Override
  public boolean isEmployeeActive(final EmployeeDO employee)
  {
    if (employee.getAustrittsDatum() == null) {
      return true;
    }
    final Calendar now = Calendar.getInstance();
    final Calendar austrittsdatum = Calendar.getInstance();
    austrittsdatum.setTime(employee.getAustrittsDatum());
    return now.before(austrittsdatum);
  }

  @Override
  public BigDecimal getMonthlySalary(EmployeeDO employee, Calendar selectedDate)
  {
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
  public List<EmployeeDO> findAllActive(final boolean checkAccess)
  {
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
  public EmployeeDO getEmployeeByStaffnumber(String staffnumber)
  {
    return employeeDao.getEmployeeByStaffnumber(staffnumber);
  }

  @Override
  public List<EmployeeDO> getAll(boolean checkAccess)
  {
    return checkAccess ? employeeDao.getList(new EmployeeFilter()) : employeeDao.internalLoadAll();
  }

}