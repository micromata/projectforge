package org.projectforge.business.vacation.service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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

  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.instance();

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
    final String urlOfVacationEditPage = configService.getDomain() + vacationEditPagePath + "?id=" + vacationData.getId();
    final String employeeFullName = vacationData.getEmployee().getUser().getFullname();
    final String managerFirstName = vacationData.getManager().getUser().getFirstname();
    final String substitutionFirstName = vacationData.getSubstitution().getUser().getFirstname();

    final String periodI18nKey = vacationData.getHalfDay() ? "vacation.mail.period.halfday" : "vacation.mail.period.fromto";
    final String vacationStartDate = dateFormatter.getFormattedDate(vacationData.getStartDate());
    final String vacationEndDate = dateFormatter.getFormattedDate(vacationData.getEndDate());
    final String periodText = I18nHelper.getLocalizedMessage(periodI18nKey, vacationStartDate, vacationEndDate);

    final String i18nSubject;
    final String i18nPMContent;
    final String i18nSubContent;

    if (isNew == true && isDeleted == false) {
      i18nSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject", employeeFullName);
      i18nPMContent = I18nHelper
          .getLocalizedMessage("vacation.mail.pm.application", managerFirstName, employeeFullName, periodText, urlOfVacationEditPage);
      i18nSubContent = I18nHelper
          .getLocalizedMessage("vacation.mail.sub.application", substitutionFirstName, employeeFullName, periodText, urlOfVacationEditPage);
    } else if (isNew == false && isDeleted == false) {
      i18nSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.edit", employeeFullName);
      i18nPMContent = I18nHelper
          .getLocalizedMessage("vacation.mail.pm.application.edit", managerFirstName, employeeFullName, periodText, urlOfVacationEditPage);
      i18nSubContent = I18nHelper
          .getLocalizedMessage("vacation.mail.sub.application.edit", substitutionFirstName, employeeFullName, periodText, urlOfVacationEditPage);
    } else {
      // isDeleted
      i18nSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.deleted", employeeFullName);
      i18nPMContent = I18nHelper
          .getLocalizedMessage("vacation.mail.application.deleted", managerFirstName, employeeFullName, periodText, urlOfVacationEditPage);
      i18nSubContent = I18nHelper
          .getLocalizedMessage("vacation.mail.application.deleted", substitutionFirstName, employeeFullName, periodText, urlOfVacationEditPage);
    }

    //Send mail to manager (employee in copy)
    sendMail(i18nSubject, i18nPMContent,
        vacationData.getManager().getUser(),
        vacationData.getEmployee().getUser()
    );

    //Send mail to substitution (employee in copy)
    sendMail(i18nSubject, i18nSubContent,
        vacationData.getSubstitution().getUser(),
        vacationData.getEmployee().getUser()
    );
  }

  @Override
  public void sendMailToEmployeeAndHR(VacationDO vacationData, boolean approved)
  {
    final String urlOfVacationEditPage = configService.getDomain() + vacationEditPagePath + "?id=" + vacationData.getId();
    final String employeeFirstName = vacationData.getEmployee().getUser().getFirstname();
    final String employeeFullName = vacationData.getEmployee().getUser().getFullname();
    final String substitutionFirstName = vacationData.getSubstitution().getUser().getFirstname();
    final String substitutionFullName = vacationData.getSubstitution().getUser().getFullname();
    final String managerFullName = vacationData.getManager().getUser().getFullname();

    final String periodI18nKey = vacationData.getHalfDay() ? "vacation.mail.period.halfday" : "vacation.mail.period.fromto";
    final String vacationStartDate = dateFormatter.getFormattedDate(vacationData.getStartDate());
    final String vacationEndDate = dateFormatter.getFormattedDate(vacationData.getEndDate());
    final String periodText = I18nHelper.getLocalizedMessage(periodI18nKey, vacationStartDate, vacationEndDate);

    if (approved) {
      //Send mail to HR (employee in copy)
      if (configService.getHREmailadress() == null) {
        throw new UserException("HR email address not configured!");
      }

      final String subject = I18nHelper.getLocalizedMessage("vacation.mail.subject", employeeFullName);
      final String content = I18nHelper
          .getLocalizedMessage("vacation.mail.hr.approved", employeeFullName, periodText, substitutionFullName, managerFullName, urlOfVacationEditPage);

      sendMail(subject, content,
          configService.getHREmailadress(), "HR-MANAGEMENT",
          vacationData.getManager().getUser(),
          vacationData.getEmployee().getUser()
      );
    }

    //Send mail to substitution (employee in copy)
    final String subject = I18nHelper.getLocalizedMessage("vacation.mail.subject.edit", employeeFullName);
    final String i18nKey = approved ? "vacation.mail.employee.approved" : "vacation.mail.employee.declined";
    final String content = I18nHelper
        .getLocalizedMessage(i18nKey, employeeFirstName, substitutionFirstName, employeeFullName, periodText, substitutionFullName, urlOfVacationEditPage);

    sendMail(subject, content,
        vacationData.getSubstitution().getUser(),
        vacationData.getEmployee().getUser()
    );
  }

  private boolean sendMail(final String subject, final String content, final PFUserDO... recipients)
  {
    return sendMail(subject, content, null, null, recipients);
  }

  private boolean sendMail(final String subject, final String content, final String recipientMailAddress, final String recipientRealName,
      final PFUserDO... additionalRecipients)
  {
    final Mail mail = new Mail();
    mail.setContentType(Mail.CONTENTTYPE_HTML);
    mail.setSubject(subject);
    mail.setContent(content);
    if (StringUtils.isNotBlank(recipientMailAddress) && StringUtils.isNotBlank(recipientRealName)) {
      mail.setTo(recipientMailAddress, recipientRealName);
    }
    Arrays.stream(additionalRecipients).forEach(mail::setTo);
    return sendMailService.send(mail, null, null);
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
    if (vacationData == null || vacationData.getIsSpecial() == true || vacationData.getEmployee() == null || vacationData.getStartDate() == null
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

    final Calendar now = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    final Calendar endDateVacationFromLastYear = configService.getEndDateVacationFromLastYear();
    final BigDecimal vacationFromPreviousYear;
    if (year != now.get(Calendar.YEAR)) {
      vacationFromPreviousYear = BigDecimal.ZERO;
    } else if (checkLastYear == false || now.after(endDateVacationFromLastYear)) {
      vacationFromPreviousYear = getVacationFromPreviousYearUsed(employee);
    } else {
      // before or same day as endDateVacationFromLastYear
      vacationFromPreviousYear = getVacationFromPreviousYear(employee);
    }

    final BigDecimal approvedVacation = getApprovedVacationdaysForYear(employee, year);
    final BigDecimal planedVacation = getPlannedVacationdaysForYear(employee, year);

    return vacationDays
        .add(vacationFromPreviousYear)
        .subtract(approvedVacation)
        .subtract(planedVacation);
  }

  @Override
  public BigDecimal getAvailableVacationDaysForYearAtDate(final EmployeeDO employee, final Date queryDate)
  {
    final BigDecimal vacationDays = new BigDecimal(employee.getUrlaubstage());
    final BigDecimal vacationDaysPrevYear = getVacationDaysFromPrevYearDependingOnDate(employee, queryDate);
    final BigDecimal approvedVacationDays = getApprovedVacationDaysForYearUntilDate(employee, queryDate);

    return vacationDays
        .add(vacationDaysPrevYear)
        .subtract(approvedVacationDays);
  }

  private BigDecimal getVacationDaysFromPrevYearDependingOnDate(final EmployeeDO employee, final Date queryDate)
  {
    final Calendar endDateVacationFromLastYear = configService.getEndDateVacationFromLastYear();
    final Calendar queryDateCal = Calendar.getInstance();
    queryDateCal.setTime(queryDate);

    if (queryDateCal.get(Calendar.YEAR) != endDateVacationFromLastYear.get(Calendar.YEAR)) {
      // year of query is different form the year of endDateVacationFromLastYear
      // therefore the vacation from previous year values are from the wrong year
      // therefore we don't know the right values and return zero
      return BigDecimal.ZERO;
    }

    return queryDateCal.after(endDateVacationFromLastYear)
        ? getVacationFromPreviousYearUsed(employee)
        : getVacationFromPreviousYear(employee);
  }

  private BigDecimal getApprovedVacationDaysForYearUntilDate(final EmployeeDO employee, final Date until)
  {
    final Calendar startDate = Calendar.getInstance();
    startDate.setTime(until);
    startDate.set(Calendar.MONTH, Calendar.JANUARY);
    startDate.set(Calendar.DAY_OF_MONTH, 1);
    startDate.set(Calendar.HOUR_OF_DAY, 0);
    startDate.set(Calendar.MINUTE, 0);
    startDate.set(Calendar.SECOND, 0);
    startDate.set(Calendar.MILLISECOND, 0);

    final List<VacationDO> vacations = getVacationForDate(employee, startDate.getTime(), until, false);

    return vacations.stream()
        .filter(v -> v.getStatus().equals(VacationStatus.APPROVED))
        .map(v -> getVacationDays(v, until))
        .reduce(BigDecimal.ZERO, BigDecimal::add); // sum
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
    return getVacationDays(vacationData, null);
  }

  private BigDecimal getVacationDays(final VacationDO vacationData, final Date until)
  {
    final Date startDate = vacationData.getStartDate();
    final Date endDate = vacationData.getEndDate();

    if (startDate != null && endDate != null) {
      final Date endDateToUse = (until != null && until.before(endDate)) ? until : endDate;
      return getVacationDays(startDate, endDateToUse, vacationData.getHalfDay());
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