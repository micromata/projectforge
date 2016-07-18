package org.projectforge.business.fibu.impl;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
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

/**
 * Standard implementation of the Employee service interface.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Service
public class EmployeeServiceImpl extends CorePersistenceServiceImpl<Integer, EmployeeDO>
    implements EmployeeService
{
  @Autowired
  private UserDao userDao;

  @Autowired
  private Kost1Dao kost1Dao;
  @Autowired
  EmployeeDao employeeDao;

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
          "set" + attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1, attributeName.length()),type);
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

    attributes.removeIf((s)->{ return s.equals(attributeName);});
    return update(employeeDO, attributes.toArray(new String[]{}));
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

}
