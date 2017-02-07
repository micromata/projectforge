package org.projectforge.business.vacation.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.vacation.model.VacationAttrProperty;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.repository.VacationDao;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.impl.CorePersistenceServiceImpl;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateTimeFormatter;
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

  private static final String vacationEditPagePath = "/wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationEditPage";

  private static final BigDecimal HALF_DAY = new BigDecimal(0.5);

  @Override
  public BigDecimal getApprovedAndPlanedVacationdaysForYear(EmployeeDO employee, int year)
  {
    final BigDecimal approved = getApprovedVacationdaysForYear(employee, year);
    final BigDecimal planned = getPlannedVacationdaysForYear(employee, year);
    return approved.add(planned);
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
    DateTimeFormatter dateFormatter = DateTimeFormatter.instance();
    if (isNew == true && isDeleted == false) {
      i18nPMContent = I18nHelper.getLocalizedMessage("vacation.mail.pm.application", vacationData.getManager().getUser().getFirstname(),
          vacationData.getEmployee().getUser().getFullname(), dateFormatter.getFormattedDate(vacationData.getStartDate()),
          dateFormatter.getFormattedDate(vacationData.getEndDate()),
          configService.getDomain() + vacationEditPagePath + "?id=" + vacationData.getId());
      i18nPMSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject", vacationData.getEmployee().getUser().getFullname());
      i18nSubContent = I18nHelper.getLocalizedMessage("vacation.mail.sub.application", vacationData.getSubstitution().getUser().getFirstname(),
          vacationData.getEmployee().getUser().getFullname(), dateFormatter.getFormattedDate(vacationData.getStartDate()),
          dateFormatter.getFormattedDate(vacationData.getEndDate()),
          configService.getDomain() + vacationEditPagePath + "?id=" + vacationData.getId());
      i18nSubSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject", vacationData.getEmployee().getUser().getFullname());
    }
    if (isNew == false && isDeleted == false) {
      i18nPMContent = I18nHelper.getLocalizedMessage("vacation.mail.pm.application.edit", vacationData.getManager().getUser().getFirstname(),
          vacationData.getEmployee().getUser().getFullname(), dateFormatter.getFormattedDate(vacationData.getStartDate()),
          dateFormatter.getFormattedDate(vacationData.getEndDate()),
          configService.getDomain() + vacationEditPagePath + "?id=" + vacationData.getId());
      i18nPMSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.edit", vacationData.getEmployee().getUser().getFullname());
      i18nSubContent = I18nHelper.getLocalizedMessage("vacation.mail.sub.application.edit", vacationData.getSubstitution().getUser().getFirstname(),
          vacationData.getEmployee().getUser().getFullname(), dateFormatter.getFormattedDate(vacationData.getStartDate()),
          dateFormatter.getFormattedDate(vacationData.getEndDate()),
          configService.getDomain() + vacationEditPagePath + "?id=" + vacationData.getId());
      i18nSubSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.edit", vacationData.getEmployee().getUser().getFullname());
    }
    if (isDeleted) {
      i18nPMContent = I18nHelper.getLocalizedMessage("vacation.mail.application.deleted", vacationData.getManager().getUser().getFirstname(),
          vacationData.getEmployee().getUser().getFullname(), dateFormatter.getFormattedDate(vacationData.getStartDate()),
          dateFormatter.getFormattedDate(vacationData.getEndDate()),
          configService.getDomain() + vacationEditPagePath + "?id=" + vacationData.getId());
      i18nPMSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.deleted", vacationData.getEmployee().getUser().getFullname());
      i18nSubContent = I18nHelper.getLocalizedMessage("vacation.mail.application.deleted", vacationData.getSubstitution().getUser().getFirstname(),
          vacationData.getEmployee().getUser().getFullname(), dateFormatter.getFormattedDate(vacationData.getStartDate()),
          dateFormatter.getFormattedDate(vacationData.getEndDate()),
          configService.getDomain() + vacationEditPagePath + "?id=" + vacationData.getId());
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
    DateTimeFormatter dateFormatter = DateTimeFormatter.instance();
    if (approved) {
      //Send mail to HR (employee in copy)
      mail.setContent(I18nHelper.getLocalizedMessage("vacation.mail.hr.approved", vacationData.getEmployee().getUser().getFullname(),
          dateFormatter.getFormattedDate(vacationData.getStartDate()), dateFormatter.getFormattedDate(vacationData.getEndDate()),
          vacationData.getSubstitution().getUser().getFullname(),
          vacationData.getManager().getUser().getFullname(),
          configService.getDomain() + vacationEditPagePath + "?id=" + vacationData.getId()));
      mail.setSubject(I18nHelper.getLocalizedMessage("vacation.mail.subject", vacationData.getEmployee().getUser().getFullname()));
      mail.setContentType(Mail.CONTENTTYPE_HTML);
      if (configService.getHREmailadress() == null) {
        throw new UserException("HR email address not configured!");
      }
      mail.setTo(configService.getHREmailadress(), "HR-MANAGEMENT");
      mail.setTo(vacationData.getManager().getUser());
      mail.setTo(vacationData.getEmployee().getUser());
      sendMailService.send(mail, null, null);
    }

    //Send mail to substitution (employee in copy)
    String decision = approved ? "approved" : "declined";
    mail = new Mail();
    mail.setContent(I18nHelper.getLocalizedMessage("vacation.mail.employee." + decision, vacationData.getEmployee().getUser().getFirstname(),
        vacationData.getSubstitution().getUser().getFirstname(), vacationData.getEmployee().getUser().getFullname(),
        dateFormatter.getFormattedDate(vacationData.getStartDate()), dateFormatter.getFormattedDate(vacationData.getEndDate()),
        vacationData.getSubstitution().getUser().getFullname(),
        configService.getDomain() + vacationEditPagePath + "?id=" + vacationData.getId()));
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
  public BigDecimal updateUsedVacationDaysFromLastYear(final VacationDO vacationData)
  {
    if (vacationData == null || vacationData.getEmployee() == null || vacationData.getStartDate() == null || vacationData.getEndDate() == null) {
      return BigDecimal.ZERO;
    }
    final Calendar now = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    final Calendar startDate = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    final Calendar endDateVacationFromLastYear = getEndDateVacationFromLastYear();
    if (vacationData.getIsSpecial() == true) {
      if (vacationData.getId() != null) {
        VacationDO vacation = vacationDao.getById(vacationData.getId());
        if (vacation.getIsSpecial() == false) {
          return deleteUsedVacationDaysFromLastYear(vacation);
        }
      }
      return BigDecimal.ZERO;
    }
    startDate.setTime(vacationData.getStartDate());
    if (startDate.get(Calendar.YEAR) > now.get(Calendar.YEAR) && vacationData.getStartDate().before(endDateVacationFromLastYear.getTime()) == false) {
      return BigDecimal.ZERO;
    }

    final Date endDate = vacationData.getEndDate().before(endDateVacationFromLastYear.getTime())
        ? vacationData.getEndDate()
        : endDateVacationFromLastYear.getTime();

    final BigDecimal neededDaysForVacationFromLastYear = getVacationDays(vacationData.getStartDate(), endDate, vacationData.getHalfDay());

    final EmployeeDO employee = vacationData.getEmployee();
    final BigDecimal actualUsedDaysOfLastYear = getVacationFromPreviousYearUsed(employee);
    final BigDecimal vacationFromPreviousYear = getVacationFromPreviousYear(employee);

    final BigDecimal freeDaysFromLastYear = vacationFromPreviousYear.subtract(actualUsedDaysOfLastYear);
    final BigDecimal remainValue = freeDaysFromLastYear.subtract(neededDaysForVacationFromLastYear).compareTo(BigDecimal.ZERO) < 0 ?
        BigDecimal.ZERO :
        freeDaysFromLastYear.subtract(neededDaysForVacationFromLastYear);
    final BigDecimal newValue = vacationFromPreviousYear.subtract(remainValue);
    employee.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), newValue);
    employeeDao.internalUpdate(employee);
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
  public BigDecimal deleteUsedVacationDaysFromLastYear(VacationDO vacationData)
  {
    if (vacationData.getIsSpecial() == true || vacationData == null || vacationData.getEmployee() == null || vacationData.getStartDate() == null
        || vacationData.getEndDate() == null) {
      return BigDecimal.ZERO;
    }
    final EmployeeDO employee = vacationData.getEmployee();
    final BigDecimal actualUsedDaysOfLastYear = getVacationFromPreviousYearUsed(employee);
    final BigDecimal vacationFromPreviousYear = getVacationFromPreviousYear(employee);
    final Calendar startDateCalender = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    startDateCalender.setTime(vacationData.getStartDate());
    final Calendar firstOfJanOfStartYearCalender = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    firstOfJanOfStartYearCalender.set(startDateCalender.get(Calendar.YEAR), Calendar.JANUARY, 1);
    final Calendar endDateCalender = configService.getEndDateVacationFromLastYear();
    final List<VacationDO> vacationList = getVacationForDate(vacationData.getEmployee(), startDateCalender.getTime(), endDateCalender.getTime(), false);

    // sum vacation days until "configured end date vacation from last year"
    final BigDecimal dayCount = vacationList.stream()
        .map(vacation -> {
          final Date endDate = vacation.getEndDate().before(endDateCalender.getTime())
              ? vacation.getEndDate()
              : endDateCalender.getTime();
          return getVacationDays(vacation.getStartDate(), endDate, vacation.getHalfDay());
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal newDays = BigDecimal.ZERO;

    if (dayCount.compareTo(vacationFromPreviousYear) < 0) // dayCount < vacationFromPreviousYear
    {
      if (vacationData.getEndDate().compareTo(endDateCalender.getTime()) < 0) {
        newDays = actualUsedDaysOfLastYear.subtract(getVacationDays(vacationData));
      } else {
        newDays = actualUsedDaysOfLastYear.subtract(getVacationDays(vacationData.getStartDate(), endDateCalender.getTime(), vacationData.getHalfDay()));
      }
      if (newDays.compareTo(BigDecimal.ZERO) < 0) {
        newDays = BigDecimal.ZERO;
      }
    }
    employee.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), newDays);
    employeeDao.internalUpdate(employee);
    return newDays;
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
  public BigDecimal getApprovedVacationdaysForYear(final EmployeeDO employee, final int year)
  {
    return getVacationDaysForYearByStatus(employee, year, VacationStatus.APPROVED);
  }

  @Override
  public BigDecimal getPlannedVacationdaysForYear(final EmployeeDO employee, final int year)
  {
    return getVacationDaysForYearByStatus(employee, year, VacationStatus.IN_PROGRESS);
  }

  private BigDecimal getVacationDaysForYearByStatus(final EmployeeDO employee, int year, final VacationStatus status)
  {
    return getActiveVacationForYear(employee, year, false)
        .stream()
        .filter(vac -> vac.getStatus().equals(status))
        .map(this::getVacationDays)
        .reduce(BigDecimal.ZERO, BigDecimal::add); // sum
  }

  @Override
  public BigDecimal getAvailableVacationdaysForYear(PFUserDO user, int year, boolean checkLastYear)
  {
    if (user == null) {
      return BigDecimal.ZERO;
    }
    EmployeeDO employee = employeeService.getEmployeeByUserId(user.getPk());
    if (employee == null) {
      return BigDecimal.ZERO;
    }
    return getAvailableVacationdaysForYear(employee, year, checkLastYear);
  }

  @Override
  public BigDecimal getAvailableVacationdaysForYear(EmployeeDO employee, int year, boolean checkLastYear)
  {
    if (employee == null) {
      return BigDecimal.ZERO;
    }
    final BigDecimal vacationDays = new BigDecimal(employee.getUrlaubstage());

    final BigDecimal vacationFromPreviousYear;
    final BigDecimal vacationFromPreviousYearUsed;
    final Calendar now = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    if (year > now.get(Calendar.YEAR)) {
      vacationFromPreviousYear = BigDecimal.ZERO;
      vacationFromPreviousYearUsed = BigDecimal.ZERO;
    } else {
      vacationFromPreviousYear = getVacationFromPreviousYear(employee);
      vacationFromPreviousYearUsed = getVacationFromPreviousYearUsed(employee);
    }

    final BigDecimal approvedVacation = getApprovedVacationdaysForYear(employee, year);
    final BigDecimal planedVacation = getPlannedVacationdaysForYear(employee, year);
    final Calendar endDateVacationFromLastYear = configService.getEndDateVacationFromLastYear();
    if (checkLastYear == false || now.after(endDateVacationFromLastYear)) {
      return vacationDays
          .subtract(vacationFromPreviousYearUsed)
          .subtract(approvedVacation)
          .subtract(planedVacation);
    }
    return vacationDays
        .add(vacationFromPreviousYear)
        .subtract(approvedVacation)
        .subtract(planedVacation);
  }

  private BigDecimal getVacationFromPreviousYearUsed(EmployeeDO employee)
  {
    final BigDecimal prevYearLeaveUsed = employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class);
    return prevYearLeaveUsed != null ? prevYearLeaveUsed : BigDecimal.ZERO;
  }

  private BigDecimal getVacationFromPreviousYear(EmployeeDO employee)
  {
    final BigDecimal prevYearLeave = employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class);
    return prevYearLeave != null ? prevYearLeave : BigDecimal.ZERO;
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
  public List<VacationDO> getVacationForDate(EmployeeDO employee, Date startDate, Date endDate, boolean withSpecial)
  {
    return vacationDao.getVacationForPeriod(employee, startDate, endDate, withSpecial);
  }

  @Override
  public BigDecimal getOpenLeaveApplicationsForUser(PFUserDO user)
  {
    EmployeeDO employee = employeeService.getEmployeeByUserId(user.getId());
    if (employee == null) {
      return BigDecimal.ZERO;
    }
    return vacationDao.getOpenLeaveApplicationsForEmployee(employee);
  }

  @Override
  public BigDecimal getSpecialVacationCount(EmployeeDO employee, int year, VacationStatus status)
  {
    return vacationDao
        .getSpecialVacation(employee, year, status)
        .stream()
        .map(this::getVacationDays)
        .reduce(BigDecimal.ZERO, BigDecimal::add); // sum
  }

  @Override
  public BigDecimal getVacationDays(final VacationDO vacationData)
  {
    final Date startDate = vacationData.getStartDate();
    final Date endDate = vacationData.getEndDate();

    if (startDate != null && endDate != null) {
      return getVacationDays(startDate, endDate, vacationData.getHalfDay());
    }
    return null;
  }

  @Override
  public BigDecimal getVacationDays(final Date from, final Date to, final Boolean isHalfDayVacation)
  {
    return Boolean.TRUE.equals(isHalfDayVacation) // null evaluates to false
        ? HALF_DAY
        : DayHolder.getNumberOfWorkingDays(from, to);
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