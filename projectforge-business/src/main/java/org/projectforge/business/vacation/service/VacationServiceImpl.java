package org.projectforge.business.vacation.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.vacation.model.VacationAttrProperty;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.repository.VacationDao;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.impl.CorePersistenceServiceImpl;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.mail.Mail;
import org.projectforge.mail.SendMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Standard implementation of the vacation service interface.
 *
 * @author Florian Blumenstein
 */
@Service
public class VacationServiceImpl extends CorePersistenceServiceImpl<Integer, VacationDO>
    implements VacationService
{
  @Autowired
  private VacationDao vacationDao;

  @Autowired
  private SendMail sendMailService;

  @Autowired
  private ConfigurationService configService;

  @Autowired
  private EmployeeDao employeeDao;

  @Autowired
  private EmployeeService employeeService;

  @Override
  public BigDecimal getUsedAndPlanedVacationdaysForYear(EmployeeDO employee, int year)
  {
    BigDecimal usedVacationdays = getUsedVacationdaysForYear(employee, year);
    BigDecimal planedVacationdays = getPlanedVacationdaysForYear(employee, year);
    return usedVacationdays.add(planedVacationdays);
  }

  @Override
  public void sendMailToVacationInvolved(VacationDO vacationData, boolean isNew, boolean isDeleted)
  {
    //Send mail to manager (employee in copy)
    Mail mail = new Mail();
    String i18nPMContent = "";
    String i18nPMSubject = "";
    String i18nSubContent = "";
    String i18nSubSubject = "";
    if (isNew == true && isDeleted == false) {
      i18nPMContent = I18nHelper.getLocalizedMessage("vacation.mail.pm.application", vacationData.getManager().getUser().getFirstname(),
          vacationData.getEmployee().getUser().getFullname(), vacationData.getStartDate().toString(), vacationData.getEndDate().toString());
      i18nPMSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject", vacationData.getEmployee().getUser().getFullname());
      i18nSubContent = I18nHelper.getLocalizedMessage("vacation.mail.sub.application", vacationData.getSubstitution().getUser().getFirstname(),
          vacationData.getEmployee().getUser().getFullname(), vacationData.getStartDate().toString(), vacationData.getEndDate().toString());
      i18nSubSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject", vacationData.getEmployee().getUser().getFullname());
    }
    if (isNew == false && isDeleted == false) {
      i18nPMContent = I18nHelper.getLocalizedMessage("vacation.mail.pm.application.edit", vacationData.getManager().getUser().getFirstname(),
          vacationData.getEmployee().getUser().getFullname(), vacationData.getStartDate().toString(), vacationData.getEndDate().toString());
      i18nPMSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.edit", vacationData.getEmployee().getUser().getFullname());
      i18nSubContent = I18nHelper.getLocalizedMessage("vacation.mail.sub.application.edit", vacationData.getSubstitution().getUser().getFirstname(),
          vacationData.getEmployee().getUser().getFullname(), vacationData.getStartDate().toString(), vacationData.getEndDate().toString());
      i18nSubSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.edit", vacationData.getEmployee().getUser().getFullname());
    }
    if (isDeleted) {
      i18nPMContent = I18nHelper.getLocalizedMessage("vacation.mail.application.deleted", vacationData.getManager().getUser().getFirstname(),
          vacationData.getEmployee().getUser().getFullname(), vacationData.getStartDate().toString(), vacationData.getEndDate().toString());
      i18nPMSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.deleted", vacationData.getEmployee().getUser().getFullname());
      i18nSubContent = I18nHelper.getLocalizedMessage("vacation.mail.application.deleted", vacationData.getSubstitution().getUser().getFirstname(),
          vacationData.getEmployee().getUser().getFullname(), vacationData.getStartDate().toString(), vacationData.getEndDate().toString());
      i18nSubSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.deleted", vacationData.getEmployee().getUser().getFullname());
    }
    mail.setContent(i18nPMContent);
    mail.setSubject(i18nPMSubject);
    mail.setContentType(Mail.CONTENTTYPE_HTML);
    mail.setTo(vacationData.getManager().getUser());
    mail.setTo(vacationData.getEmployee().getUser());
    sendMailService.send(mail, null, null);

    //Send mail to substitution (employee in copy)
    mail = new Mail();
    mail.setContent(i18nSubContent);
    mail.setSubject(i18nSubSubject);
    mail.setContentType(Mail.CONTENTTYPE_HTML);
    mail.setTo(vacationData.getSubstitution().getUser());
    mail.setTo(vacationData.getEmployee().getUser());
    sendMailService.send(mail, null, null);
  }

  @Override
  public void sendMailToEmployeeAndHR(VacationDO vacationData, boolean approved)
  {
    Mail mail = new Mail();
    if (approved) {
      //Send mail to HR (employee in copy)
      mail.setContent(I18nHelper.getLocalizedMessage("vacation.mail.hr.approved", vacationData.getEmployee().getUser().getFullname(),
          vacationData.getStartDate().toString(), vacationData.getEndDate().toString(), vacationData.getSubstitution().getUser().getFullname(),
          vacationData.getManager().getUser().getFullname()));
      mail.setSubject(I18nHelper.getLocalizedMessage("vacation.mail.subject", vacationData.getEmployee().getUser().getFullname()));
      mail.setContentType(Mail.CONTENTTYPE_HTML);
      mail.setTo(vacationData.getManager().getUser());
      mail.setTo(vacationData.getEmployee().getUser());
      sendMailService.send(mail, null, null);
    }

    //Send mail to substitution (employee in copy)
    String decision = approved ? "approved" : "declined";
    mail = new Mail();
    mail.setContent(I18nHelper.getLocalizedMessage("vacation.mail.employee." + decision, vacationData.getEmployee().getUser().getFirstname(),
        vacationData.getSubstitution().getUser().getFirstname(), vacationData.getEmployee().getUser().getFullname(),
        vacationData.getStartDate().toString(), vacationData.getEndDate().toString(), vacationData.getSubstitution().getUser().getFullname()));
    mail.setSubject(I18nHelper.getLocalizedMessage("vacation.mail.subject.edit", vacationData.getEmployee().getUser().getFullname()));
    mail.setContentType(Mail.CONTENTTYPE_HTML);
    mail.setTo(vacationData.getSubstitution().getUser());
    mail.setTo(vacationData.getEmployee().getUser());
    sendMailService.send(mail, null, null);
  }

  @Override
  public Calendar getEndDateVacationFromLastYear()
  {
    return configService.getEndDateVacationFromLastYear();
  }

  @Override
  public BigDecimal updateUsedVacationDaysFromLastYear(VacationDO vacationData)
  {
    if (vacationData == null || vacationData.getEmployee() == null || vacationData.getStartDate() == null || vacationData.getEndDate() == null) {
      return BigDecimal.ZERO;
    }
    Calendar now = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    Calendar startDate = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    if (startDate.get(Calendar.YEAR) > now.get(Calendar.YEAR)) {
      return BigDecimal.ZERO;
    }
    Calendar endDateVacationFromLastYear = getEndDateVacationFromLastYear();
    endDateVacationFromLastYear.add(Calendar.DAY_OF_MONTH, 1);
    if (vacationData.getStartDate().before(endDateVacationFromLastYear.getTime()) == false) {
      return BigDecimal.ZERO;
    }
    endDateVacationFromLastYear.add(Calendar.DAY_OF_MONTH, -1);
    BigDecimal neededDaysForVacationFromLastYear = null;
    if (vacationData.getEndDate().after(endDateVacationFromLastYear.getTime())) {
      neededDaysForVacationFromLastYear = DayHolder.getNumberOfWorkingDays(vacationData.getStartDate(), endDateVacationFromLastYear.getTime());
    } else {
      neededDaysForVacationFromLastYear = DayHolder.getNumberOfWorkingDays(vacationData.getStartDate(), vacationData.getEndDate());
    }
    EmployeeDO employee = vacationData.getEmployee();
    BigDecimal actualUsedDaysOfLastYear = employee
        .getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class) != null
        ? employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)
        : BigDecimal.ZERO;
    BigDecimal vacationFromPreviousYear = employee
        .getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class) != null
        ? employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)
        : BigDecimal.ZERO;
    BigDecimal freeDaysFromLastYear = vacationFromPreviousYear.subtract(actualUsedDaysOfLastYear);
    BigDecimal remainValue = freeDaysFromLastYear.subtract(neededDaysForVacationFromLastYear).compareTo(BigDecimal.ZERO) < 0 ?
        BigDecimal.ZERO :
        freeDaysFromLastYear.subtract(neededDaysForVacationFromLastYear);
    BigDecimal newValue = vacationFromPreviousYear.subtract(remainValue);
    employee.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), newValue);
    employeeDao.internalSave(employee);
    return newValue;
  }

  @Override
  public void updateUsedNewVacationDaysFromLastYear(EmployeeDO employee, int year)
  {
    BigDecimal availableVacationdays = getAvailableVacationdaysForYear(employee, year, false);
    employee.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), availableVacationdays);
    employee.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.ZERO);
    employeeDao.internalSave(employee);
  }

  @Override
  public boolean couldUserUseVacationService(PFUserDO user, boolean throwException)
  {
    boolean result = true;
    if (user == null || user.getId() == null) {
      return false;
    }
    EmployeeDO employee = employeeService.getEmployeeByUserId(user.getId());
    if (employee == null) {
      if (throwException) {
        throw new AccessException("access.exception.noEmployeeToUser");
      }
      result = false;
    } else if (employee.getUrlaubstage() == null) {
      if (throwException) {
        throw new AccessException("access.exception.employeeHasNoVacationDays");
      }
      result = false;
    }
    return result;
  }

  @Override
  public BigDecimal getUsedVacationdaysForYear(EmployeeDO employee, int year)
  {
    BigDecimal usedDays = BigDecimal.ZERO;
    Calendar now = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    for (VacationDO vac : getActiveVacationForYear(employee, year, false)) {
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
  public BigDecimal getPlanedVacationdaysForYear(EmployeeDO employee, int year)
  {
    BigDecimal usedDays = BigDecimal.ZERO;
    Calendar now = new GregorianCalendar(ThreadLocalUserContext.getTimeZone());
    for (VacationDO vac : getActiveVacationForYear(employee, year, false)) {
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
  public BigDecimal getAvailableVacationdaysForYear(EmployeeDO employee, int year, boolean checkLastYear)
  {
    if (employee == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal vacationDays = new BigDecimal(employee.getUrlaubstage());
    BigDecimal vacationFromPreviousYear = employee
        .getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class) != null
        ? employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)
        : BigDecimal.ZERO;
    BigDecimal vacationFromPreviousYearUsed = employee
        .getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class) != null
        ? employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)
        : BigDecimal.ZERO;
    Calendar now = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    if (year > now.get(Calendar.YEAR)) {
      vacationFromPreviousYear = BigDecimal.ZERO;
      vacationFromPreviousYearUsed = BigDecimal.ZERO;
    }
    BigDecimal usedVacation = getUsedVacationdaysForYear(employee, year);
    BigDecimal planedVacation = getPlanedVacationdaysForYear(employee, year);
    Calendar endDateVacationFromLastYear = configService.getEndDateVacationFromLastYear();
    if (now.after(endDateVacationFromLastYear) || checkLastYear == false) {
      usedVacation = getUsedVacationdaysForYear(employee, year).subtract(vacationFromPreviousYearUsed);
      return vacationDays.subtract(usedVacation).subtract(planedVacation);
    }
    return vacationDays.add(vacationFromPreviousYear).subtract(usedVacation).subtract(planedVacation);
  }

  @Override
  public List<VacationDO> getActiveVacationForYear(EmployeeDO employee, int year, boolean withSpecial)
  {
    return vacationDao.getActiveVacationForYear(employee, year, withSpecial);
  }

  @Override
  public List<VacationDO> getAllActiveVacation(EmployeeDO employee, boolean withSpecial)
  {
    return vacationDao.getAllActiveVacation(employee, withSpecial);
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