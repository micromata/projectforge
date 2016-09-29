package org.projectforge.business.vacation.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.vacation.model.VacationAttrProperty;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.repository.VacationDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.impl.CorePersistenceServiceImpl;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DayHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Standard implementation of the vacation service interface.
 * 
 * @author Florian Blumenstein
 *
 */
@Service
public class VacationServiceImpl extends CorePersistenceServiceImpl<Integer, VacationDO>
    implements VacationService
{
  @Autowired
  private VacationDao vacationDao;

  @Override
  public BigDecimal getUsedAndPlanedVacationdays(EmployeeDO employee)
  {
    BigDecimal usedVacationdays = getUsedVacationdays(employee);
    BigDecimal planedVacationdays = getPlanedVacationdays(employee);
    return usedVacationdays.add(planedVacationdays);
  }

  @Override
  public BigDecimal getUsedVacationdays(EmployeeDO employee)
  {
    BigDecimal usedDays = BigDecimal.ZERO;
    Calendar now = new GregorianCalendar(ThreadLocalUserContext.getTimeZone());
    for (VacationDO vac : getActiveVacationForCurrentYear(employee)) {
      if (vac.getStartDate().before(now.getTime()) == true && vac.getEndDate().before(now.getTime()) == true) {
        usedDays = usedDays.add(vac.getWorkingdays());
      } else {
        if (vac.getStartDate().before(now.getTime()) == true && vac.getEndDate().before(now.getTime()) == false) {
          usedDays = usedDays.add(DayHolder.getNumberOfWorkingDays(vac.getStartDate(), now.getTime()));
        }
      }
    }
    return usedDays;
  }

  @Override
  public BigDecimal getPlanedVacationdays(EmployeeDO employee)
  {
    BigDecimal usedDays = BigDecimal.ZERO;
    Calendar now = new GregorianCalendar(ThreadLocalUserContext.getTimeZone());
    for (VacationDO vac : getActiveVacationForCurrentYear(employee)) {
      if (vac.getStartDate().after(now.getTime()) == true && vac.getEndDate().after(now.getTime()) == true) {
        usedDays = usedDays.add(vac.getWorkingdays());
      } else {
        if (vac.getStartDate().after(now.getTime()) == true && vac.getEndDate().after(now.getTime()) == false) {
          usedDays = usedDays.add(DayHolder.getNumberOfWorkingDays(now.getTime(), vac.getEndDate()));
        }
      }
    }
    return usedDays;
  }

  @Override
  public BigDecimal getAvailableVacationdays(EmployeeDO employee)
  {
    BigDecimal vacationDays = new BigDecimal(employee.getUrlaubstage());
    BigDecimal vacationFromPreviousYear = employee
        .getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class) != null
            ? employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)
            : BigDecimal.ZERO;
    BigDecimal vacationFromPreviousYearUsed = employee
        .getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class) != null
            ? employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)
            : BigDecimal.ZERO;
    BigDecimal usedVacation = getUsedVacationdays(employee);
    BigDecimal planedVacation = getPlanedVacationdays(employee);
    Calendar endOfMarch = new GregorianCalendar(ThreadLocalUserContext.getTimeZone());
    endOfMarch.set(Calendar.MONTH, Calendar.MARCH);
    endOfMarch.set(Calendar.DAY_OF_MONTH, 31);
    Calendar now = new GregorianCalendar(ThreadLocalUserContext.getTimeZone());
    if (now.after(endOfMarch)) {
      usedVacation = getUsedVacationdays(employee).subtract(vacationFromPreviousYearUsed);
      return vacationDays.subtract(usedVacation).subtract(planedVacation);
    }
    return vacationDays.add(vacationFromPreviousYear).subtract(usedVacation).subtract(planedVacation);
  }

  @Override
  public List<VacationDO> getActiveVacationForCurrentYear(EmployeeDO employee)
  {
    return vacationDao.getActiveVacationForCurrentYear(employee);
  }

  @Override
  public List<VacationDO> getList(BaseSearchFilter filter)
  {
    return vacationDao.getList(filter);
  }

  @Override
  public List<VacationDO> getVacation(List<Serializable> idList)
  {
    return vacationDao.internalLoad(idList);
  }

  @Override
  public List<VacationDO> getVacationForDate(EmployeeDO employee, Date startDate, Date endDate)
  {
    return vacationDao.getVacationForPeriod(employee, startDate, endDate);
  }

  @Override
  public boolean hasInsertAccess(PFUserDO user)
  {
    return true;
  }

  @Override
  public boolean hasLoggedInUserInsertAccess()
  {
    return vacationDao.hasLoggedInUserInsertAccess();
  }

  @Override
  public boolean hasLoggedInUserInsertAccess(VacationDO obj, boolean throwException)
  {
    return vacationDao.hasLoggedInUserInsertAccess(obj, throwException);
  }

  @Override
  public boolean hasLoggedInUserUpdateAccess(VacationDO obj, VacationDO dbObj, boolean throwException)
  {
    return vacationDao.hasLoggedInUserUpdateAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasLoggedInUserDeleteAccess(VacationDO obj, VacationDO dbObj, boolean throwException)
  {
    return vacationDao.hasLoggedInUserDeleteAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasDeleteAccess(PFUserDO user, VacationDO obj, VacationDO dbObj, boolean throwException)
  {
    return vacationDao.hasDeleteAccess(user, obj, dbObj, throwException);
  }

  @Override
  public List<String> getAutocompletion(String property, String searchString)
  {
    return vacationDao.getAutocompletion(property, searchString);
  }

  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(VacationDO obj)
  {
    return vacationDao.getDisplayHistoryEntries(obj);
  }

  @Override
  public void rebuildDatabaseIndex4NewestEntries()
  {
    vacationDao.rebuildDatabaseIndex4NewestEntries();
  }

  @Override
  public void rebuildDatabaseIndex()
  {
    vacationDao.rebuildDatabaseIndex();
  }

}