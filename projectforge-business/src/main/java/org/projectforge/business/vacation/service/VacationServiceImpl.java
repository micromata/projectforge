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

package org.projectforge.business.vacation.service;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.configuration.DomainService;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeDao;
import org.projectforge.business.fibu.MonthlyEmployeeReport;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.CalEventDao;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.model.CalEventDO;
import org.projectforge.business.vacation.model.VacationAttrProperty;
import org.projectforge.business.vacation.model.VacationCalendarDO;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.repository.VacationDao;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.impl.CorePersistenceServiceImpl;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.mail.Mail;
import org.projectforge.mail.SendMail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Standard implementation of the vacation service interface.
 *
 * @author Florian Blumenstein
 */
@Service
public class VacationServiceImpl extends CorePersistenceServiceImpl<Integer, VacationDO>
    implements VacationService
{
  private static final Logger log = LoggerFactory.getLogger(VacationServiceImpl.class);

  @Autowired
  private VacationDao vacationDao;

  @Autowired
  private SendMail sendMailService;

  @Autowired
  private ConfigurationService configService;

  @Autowired
  private DomainService domainService;

  @Autowired
  private EmployeeDao employeeDao;

  @Autowired
  private EmployeeService employeeService;

  @Autowired
  private TeamEventDao teamEventDao;

  @Autowired
  private CalEventDao calEventDao;

  private static final String vacationEditPagePath = "/wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationEditPage";

  private static final BigDecimal HALF_DAY = new BigDecimal(0.5);

  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.instance();

  @Override
  public BigDecimal getApprovedAndPlanedVacationdaysForYear(final EmployeeDO employee, final int year)
  {
    final BigDecimal approved = getApprovedVacationdaysForYear(employee, year);
    final BigDecimal planned = getPlannedVacationdaysForYear(employee, year);
    return approved.add(planned);
  }

  @Override
  public void sendMailToVacationInvolved(final VacationDO vacationData, final boolean isNew, final boolean isDeleted)
  {
    final String urlOfVacationEditPage = domainService.getDomain() + vacationEditPagePath + "?id=" + vacationData.getId();
    final String employeeFullName = vacationData.getEmployee().getUser().getFullname();
    final String managerFirstName = vacationData.getManager().getUser().getFirstname();

    final String periodI18nKey = vacationData.getHalfDay() ? "vacation.mail.period.halfday" : "vacation.mail.period.fromto";
    final String vacationStartDate = dateFormatter.getFormattedDate(vacationData.getStartDate());
    final String vacationEndDate = dateFormatter.getFormattedDate(vacationData.getEndDate());
    final String periodText = I18nHelper.getLocalizedMessage(periodI18nKey, vacationStartDate, vacationEndDate);

    final String i18nSubject;
    final String i18nPMContent;

    if (isNew && !isDeleted) {
      i18nSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject", employeeFullName);
      i18nPMContent = I18nHelper
          .getLocalizedMessage("vacation.mail.pm.application", managerFirstName, employeeFullName, periodText, urlOfVacationEditPage);
    } else if (!isNew && !isDeleted) {
      i18nSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.edit", employeeFullName);
      i18nPMContent = I18nHelper
          .getLocalizedMessage("vacation.mail.pm.application.edit", managerFirstName, employeeFullName, periodText, urlOfVacationEditPage);
    } else {
      // isDeleted
      i18nSubject = I18nHelper.getLocalizedMessage("vacation.mail.subject.deleted", employeeFullName);
      i18nPMContent = I18nHelper
          .getLocalizedMessage("vacation.mail.application.deleted", managerFirstName, employeeFullName, periodText, urlOfVacationEditPage);
    }

    // Send mail to manager and employee
    sendMail(i18nSubject, i18nPMContent,
        vacationData.getManager().getUser(),
        vacationData.getEmployee().getUser()
    );

    // Send mail to substitutions and employee
    for (final EmployeeDO substitution : vacationData.getSubstitutions()) {
      final PFUserDO substitutionUser = substitution.getUser();
      final String substitutionFirstName = substitutionUser.getFirstname();
      final String i18nSubContent;

      if (isNew && !isDeleted) {
        i18nSubContent = I18nHelper
            .getLocalizedMessage("vacation.mail.sub.application", substitutionFirstName, employeeFullName, periodText, urlOfVacationEditPage);
      } else if (!isNew && !isDeleted) {
        i18nSubContent = I18nHelper
            .getLocalizedMessage("vacation.mail.sub.application.edit", substitutionFirstName, employeeFullName, periodText, urlOfVacationEditPage);
      } else {
        // isDeleted
        i18nSubContent = I18nHelper
            .getLocalizedMessage("vacation.mail.application.deleted", substitutionFirstName, employeeFullName, periodText, urlOfVacationEditPage);
      }

      sendMail(i18nSubject, i18nSubContent,
          substitutionUser,
          vacationData.getEmployee().getUser()
      );
    }
  }

  @Override
  public void sendMailToEmployeeAndHR(final VacationDO vacationData, final boolean approved)
  {
    final String urlOfVacationEditPage = domainService.getDomain() + vacationEditPagePath + "?id=" + vacationData.getId();
    final String employeeFullName = vacationData.getEmployee().getUser().getFullname();
    final String managerFullName = vacationData.getManager().getUser().getFullname();
    final String substitutionFullNames = vacationData.getSubstitutions().stream()
        .map(EmployeeDO::getUser)
        .map(PFUserDO::getFullname)
        .collect(Collectors.joining(", "));

    final String periodI18nKey = vacationData.getHalfDay() ? "vacation.mail.period.halfday" : "vacation.mail.period.fromto";
    final String vacationStartDate = dateFormatter.getFormattedDate(vacationData.getStartDate());
    final String vacationEndDate = dateFormatter.getFormattedDate(vacationData.getEndDate());
    final String periodText = I18nHelper.getLocalizedMessage(periodI18nKey, vacationStartDate, vacationEndDate);

    if (approved && configService.getHREmailadress() != null) {
      //Send mail to HR (employee in copy)
      final String subject = I18nHelper.getLocalizedMessage("vacation.mail.subject", employeeFullName);
      final String content = I18nHelper
          .getLocalizedMessage("vacation.mail.hr.approved", employeeFullName, periodText, substitutionFullNames, managerFullName, urlOfVacationEditPage);

      sendMail(subject, content,
          configService.getHREmailadress(), "HR-MANAGEMENT",
          vacationData.getManager().getUser(),
          vacationData.getEmployee().getUser()
      );
    }

    // Send mail to substitutions and employee
    final String subject = I18nHelper.getLocalizedMessage("vacation.mail.subject.edit", employeeFullName);
    final String i18nKey = approved ? "vacation.mail.employee.approved" : "vacation.mail.employee.declined";
    final String content = I18nHelper.getLocalizedMessage(i18nKey, employeeFullName, periodText, substitutionFullNames, urlOfVacationEditPage);
    final PFUserDO[] recipients = Stream.concat(vacationData.getSubstitutions().stream(), Stream.of(vacationData.getEmployee()))
        .map(EmployeeDO::getUser)
        .toArray(PFUserDO[]::new);
    sendMail(subject, content, recipients);
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
    if (vacationData.getSpecial()) {
      if (vacationData.getId() != null) {
        final VacationDO vacation = vacationDao.getById(vacationData.getId());
        if (!vacation.getSpecial()) {
          return deleteUsedVacationDaysFromLastYear(vacation);
        }
      }
      return BigDecimal.ZERO;
    }
    startDate.setTime(vacationData.getStartDate());
    if (startDate.get(Calendar.YEAR) > now.get(Calendar.YEAR) && !vacationData.getStartDate().before(endDateVacationFromLastYear.getTime())) {
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
  public void updateVacationDaysFromLastYearForNewYear(final EmployeeDO employee, final int year)
  {
    final BigDecimal availableVacationdaysFromActualYear = getAvailableVacationdaysForGivenYear(employee, year, false);
    employee.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), availableVacationdaysFromActualYear);

    // find approved vacations in new year
    Calendar from = Calendar.getInstance();
    from.setTimeZone(DateHelper.UTC);
    from.set(year + 1, Calendar.JANUARY, 1, 0, 0, 0);
    Calendar to = getEndDateVacationFromLastYear();
    to.set(Calendar.YEAR, year + 1);
    List<VacationDO> vacationNewYear = vacationDao.getVacationForPeriod(employee, from.getTime(), to.getTime(), false);

    BigDecimal usedInNewYear = BigDecimal.ZERO;

    for (VacationDO vacation : vacationNewYear) {
      if (vacation.getStatus() != VacationStatus.APPROVED) {
        continue;
      }

      // compute used days until EndDateVacationFromLastYear
      BigDecimal days = this
          .getVacationDays(vacation.getStartDate(), vacation.getEndDate().after(to.getTime()) ? to.getTime() : vacation.getEndDate(), vacation.getHalfDay());
      usedInNewYear = usedInNewYear.add(days);
    }

    // compute used days
    final BigDecimal usedDays = availableVacationdaysFromActualYear.compareTo(usedInNewYear) < 1 ? availableVacationdaysFromActualYear : usedInNewYear;

    employee.putAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), usedDays);
    employeeDao.internalUpdate(employee);
  }

  private BigDecimal getAvailableVacationdaysForGivenYear(final EmployeeDO currentEmployee, final int year, final boolean b)
  {
    Calendar endDatePreviousYearVacation = configService.getEndDateVacationFromLastYear();
    endDatePreviousYearVacation.set(Calendar.YEAR, year);

    BigDecimal vacationdays = currentEmployee.getUrlaubstage() != null ? new BigDecimal(currentEmployee.getUrlaubstage()) : BigDecimal.ZERO;
    BigDecimal vacationdaysPreviousYear = currentEmployee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class) != null
        ? currentEmployee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class) : BigDecimal.ZERO;
    BigDecimal subtotal1 = vacationdays.add(vacationdaysPreviousYear);
    BigDecimal approvedVacationdays = getApprovedVacationdaysForYear(currentEmployee, year);
    BigDecimal availableVacation = subtotal1.subtract(approvedVacationdays);

    //Needed for left and middle part
    BigDecimal vacationdaysPreviousYearUsed =
        currentEmployee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class) != null ?
            currentEmployee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class) : BigDecimal.ZERO;
    BigDecimal vacationdaysPreviousYearUnused = vacationdaysPreviousYear.subtract(vacationdaysPreviousYearUsed);

    //If previousyearleaveunused > 0, then extend left area and display new row
    if (vacationdaysPreviousYearUnused.compareTo(BigDecimal.ZERO) > 0) {
      availableVacation = availableVacation.subtract(vacationdaysPreviousYearUnused);
    }
    return availableVacation;
  }

  @Override
  public BigDecimal deleteUsedVacationDaysFromLastYear(final VacationDO vacationData)
  {
    if (vacationData == null || vacationData.getSpecial() || vacationData.getEmployee() == null || vacationData.getStartDate() == null
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
  public boolean couldUserUseVacationService(final PFUserDO user, final boolean throwException)
  {
    boolean result = true;
    if (user == null || user.getId() == null) {
      return false;
    }
    final EmployeeDO employee = employeeService.getEmployeeByUserId(user.getId());
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

  private BigDecimal getVacationDaysForYearByStatus(final EmployeeDO employee, final int year, final VacationStatus status)
  {
    return getActiveVacationForYear(employee, year, false)
        .stream()
        .filter(vac -> vac.getStatus().equals(status))
        .map(this::getVacationDays)
        .reduce(BigDecimal.ZERO, BigDecimal::add); // sum
  }

  @Override
  public BigDecimal getAvailableVacationdaysForYear(final PFUserDO user, final int year, final boolean checkLastYear)
  {
    if (user == null) {
      return BigDecimal.ZERO;
    }
    final EmployeeDO employee = employeeService.getEmployeeByUserId(user.getPk());
    if (employee == null) {
      return BigDecimal.ZERO;
    }
    return getAvailableVacationdaysForYear(employee, year, checkLastYear);
  }

  @Override
  public BigDecimal getAvailableVacationdaysForYear(final EmployeeDO employee, final int year, final boolean checkLastYear)
  {
    if (employee == null || employee.getUrlaubstage() == null) {
      return BigDecimal.ZERO;
    }
    final BigDecimal vacationDays = new BigDecimal(employee.getUrlaubstage());

    final Calendar now = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    final Calendar endDateVacationFromLastYear = configService.getEndDateVacationFromLastYear();
    final BigDecimal vacationFromPreviousYear;
    if (year != now.get(Calendar.YEAR)) {
      vacationFromPreviousYear = BigDecimal.ZERO;
    } else if (!checkLastYear || now.after(endDateVacationFromLastYear)) {
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
  public BigDecimal getPlandVacationDaysForYearAtDate(final EmployeeDO employee, final Date queryDate)
  {
    final Calendar endDate = Calendar.getInstance();
    endDate.setTime(queryDate);
    endDate.set(Calendar.MONTH, Calendar.DECEMBER);
    endDate.set(Calendar.DAY_OF_MONTH, 31);
    endDate.set(Calendar.HOUR_OF_DAY, 0);
    endDate.set(Calendar.MINUTE, 0);
    endDate.set(Calendar.SECOND, 0);
    endDate.set(Calendar.MILLISECOND, 0);

    return getApprovedVacationDaysForYearUntilDate(employee, queryDate, endDate.getTime());
  }

  @Override
  public BigDecimal getAvailableVacationDaysForYearAtDate(final EmployeeDO employee, final Date queryDate)
  {
    if (employee == null || employee.getUrlaubstage() == null) {
      return BigDecimal.ZERO;
    }

    final Calendar startDate = Calendar.getInstance();
    startDate.setTime(queryDate);
    startDate.set(Calendar.MONTH, Calendar.JANUARY);
    startDate.set(Calendar.DAY_OF_MONTH, 1);
    startDate.set(Calendar.HOUR_OF_DAY, 0);
    startDate.set(Calendar.MINUTE, 0);
    startDate.set(Calendar.SECOND, 0);
    startDate.set(Calendar.MILLISECOND, 0);

    final BigDecimal vacationDays = new BigDecimal(employee.getUrlaubstage());
    final BigDecimal vacationDaysPrevYear = getVacationDaysFromPrevYearDependingOnDate(employee, queryDate);
    final BigDecimal approvedVacationDays = getApprovedVacationDaysForYearUntilDate(employee, startDate.getTime(), queryDate);

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

  private BigDecimal getApprovedVacationDaysForYearUntilDate(final EmployeeDO employee, final Date from, final Date until)
  {

    final List<VacationDO> vacations = getVacationForDate(employee, from, until, false);

    return vacations.stream()
        .filter(v -> v.getStatus().equals(VacationStatus.APPROVED))
        .map(v -> getVacationDays(v, until))
        .reduce(BigDecimal.ZERO, BigDecimal::add); // sum
  }

  private BigDecimal getVacationFromPreviousYearUsed(final EmployeeDO employee)
  {
    final BigDecimal prevYearLeaveUsed = employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class);
    return prevYearLeaveUsed != null ? prevYearLeaveUsed : BigDecimal.ZERO;
  }

  private BigDecimal getVacationFromPreviousYear(final EmployeeDO employee)
  {
    final BigDecimal prevYearLeave = employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class);
    return prevYearLeave != null ? prevYearLeave : BigDecimal.ZERO;
  }

  @Override
  public List<VacationDO> getActiveVacationForYear(final EmployeeDO employee, final int year, final boolean withSpecial)
  {
    return vacationDao.getActiveVacationForYear(employee, year, withSpecial);
  }

  @Override
  public List<VacationDO> getAllActiveVacation(final EmployeeDO employee, final boolean withSpecial)
  {
    return vacationDao.getAllActiveVacation(employee, withSpecial);
  }

  @Override
  public List<VacationDO> getList(final BaseSearchFilter filter)
  {
    return vacationDao.getList(filter);
  }

  @Override
  public List<VacationDO> getVacation(final List<Serializable> idList)
  {
    return vacationDao.internalLoad(idList);
  }

  @Override
  public List<VacationDO> getVacationForDate(final EmployeeDO employee, final Date startDate, final Date endDate, final boolean withSpecial)
  {
    return vacationDao.getVacationForPeriod(employee, startDate, endDate, withSpecial);
  }

  @Override
  public BigDecimal getOpenLeaveApplicationsForUser(final PFUserDO user)
  {
    final EmployeeDO employee = employeeService.getEmployeeByUserId(user.getId());
    if (employee == null) {
      return BigDecimal.ZERO;
    }
    return vacationDao.getOpenLeaveApplicationsForEmployee(employee);
  }

  @Override
  public BigDecimal getSpecialVacationCount(final EmployeeDO employee, final int year, final VacationStatus status)
  {
    return vacationDao
        .getSpecialVacation(employee, year, status)
        .stream()
        .map(this::getVacationDays)
        .reduce(BigDecimal.ZERO, BigDecimal::add); // sum
  }

  @Override
  public List<TeamCalDO> getCalendarsForVacation(final VacationDO vacation)
  {
    return vacationDao.getCalendarsForVacation(vacation);
  }

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
    final BigDecimal numberOfWorkingDays = DayHolder.getNumberOfWorkingDays(from, to);

    // don't return HALF_DAY if there is no working day
    return !numberOfWorkingDays.equals(BigDecimal.ZERO) && Boolean.TRUE.equals(isHalfDayVacation) // null evaluates to false
        ? HALF_DAY
        : numberOfWorkingDays;
  }

  @Override
  public boolean hasInsertAccess(final PFUserDO user)
  {
    return true;
  }

  @Override
  public boolean hasLoggedInUserInsertAccess()
  {
    return vacationDao.hasLoggedInUserInsertAccess();
  }

  @Override
  public boolean hasLoggedInUserInsertAccess(final VacationDO obj, final boolean throwException)
  {
    return vacationDao.hasLoggedInUserInsertAccess(obj, throwException);
  }

  @Override
  public boolean hasLoggedInUserUpdateAccess(final VacationDO obj, final VacationDO dbObj, final boolean throwException)
  {
    return vacationDao.hasLoggedInUserUpdateAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasLoggedInUserDeleteAccess(final VacationDO obj, final VacationDO dbObj, final boolean throwException)
  {
    return vacationDao.hasLoggedInUserDeleteAccess(obj, dbObj, throwException);
  }

  @Override
  public boolean hasDeleteAccess(final PFUserDO user, final VacationDO obj, final VacationDO dbObj, final boolean throwException)
  {
    return vacationDao.hasDeleteAccess(user, obj, dbObj, throwException);
  }

  @Override
  public boolean hasLoggedInUserHRVacationAccess() {
    return vacationDao.hasLoggedInUserHRVacationAccess();
  }

  @Override
  public List<String> getAutocompletion(final String property, final String searchString)
  {
    return vacationDao.getAutocompletion(property, searchString);
  }

  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final VacationDO obj)
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

  @Override
  public void saveOrUpdateVacationCalendars(final VacationDO vacation, final Collection<TeamCalDO> calendars)
  {
    if (calendars != null) {
      for (final TeamCalDO teamCalDO : calendars) {
        vacationDao.saveVacationCalendar(getOrCreateVacationCalendarDO(vacation, teamCalDO));
      }
    }
    final List<VacationCalendarDO> vacationCalendars = vacationDao.getVacationCalendarDOs(vacation);
    for (VacationCalendarDO vacationCalendar : vacationCalendars) {
      if (!calendars.contains(vacationCalendar.getCalendar())) {
        vacationDao.markAsDeleted(vacationCalendar);
      } else {
        vacationDao.markAsUndeleted(vacationCalendar);
      }
    }
  }

  @Override
  public void markTeamEventsOfVacationAsDeleted(final VacationDO vacation, boolean deleteIncludingVacationCalendarDO)
  {
    final List<VacationCalendarDO> vacationCalendarDOs = vacationDao.getVacationCalendarDOs(vacation);
    for (final VacationCalendarDO vacationCalendarDO : vacationCalendarDOs) {
      if (vacationCalendarDO.getEvent() != null) {
        if (deleteIncludingVacationCalendarDO) {
          vacationDao.markAsDeleted(vacationCalendarDO);
        }
        calEventDao.internalMarkAsDeleted(calEventDao.internalGetById((vacationCalendarDO.getEvent().getId())));
      }
    }
  }

  @Override
  public void undeleteTeamEventsOfVacation(final VacationDO vacation)
  {
    final List<VacationCalendarDO> vacationCalendarDOs = vacationDao.getVacationCalendarDOs(vacation);
    for (final VacationCalendarDO vacationCalendarDO : vacationCalendarDOs) {
      if (vacationCalendarDO.isDeleted()) {
        vacationDao.markAsUndeleted(vacationCalendarDO);
      }
    }
  }

  @Override
  public String getVacationCount(final int fromYear, final int fromMonth, final int toYear, final int toMonth, final PFUserDO user)
  {
    long hours = 0;
    BigDecimal days = BigDecimal.ZERO;
    if (fromYear == toYear) {
      for (int i = fromMonth; i <= toMonth; i++) {
        MonthlyEmployeeReport reportOfMonth = employeeService.getReportOfMonth(fromYear, i, user);
        hours += reportOfMonth.getTotalNetDuration();
        days = days.add(reportOfMonth.getNumberOfWorkingDays());
      }
    } else {
      for (int i = fromMonth; i <= 11; i++) {
        MonthlyEmployeeReport reportOfMonth = employeeService.getReportOfMonth(fromYear, i, user);
        hours += reportOfMonth.getTotalNetDuration();
        days = days.add(reportOfMonth.getNumberOfWorkingDays());
      }
      for (int i = 0; i <= toMonth; i++) {
        MonthlyEmployeeReport reportOfMonth = employeeService.getReportOfMonth(toYear, i, user);
        hours += reportOfMonth.getTotalNetDuration();
        days = days.add(reportOfMonth.getNumberOfWorkingDays());
      }
    }
    final BigDecimal big_hours = new BigDecimal(hours).divide(new BigDecimal(1000 * 60 * 60), 2,
        BigDecimal.ROUND_HALF_UP);

    return NumberHelper.formatFraction2(big_hours.doubleValue() / days.doubleValue());
  }

  @Override
  public void markAsUnDeleteEventsForVacationCalendars(final VacationDO vacation)
  {
    List<VacationCalendarDO> vacationCalendarDOs = vacationDao.getVacationCalendarDOs(vacation);
    for (VacationCalendarDO vacationCalendarDO : vacationCalendarDOs) {
      if (vacationCalendarDO.getEvent() != null) {
        calEventDao.internalUndelete(calEventDao.internalGetById(vacationCalendarDO.getEvent().getId()));
      }
    }
  }

  @Override
  public void createEventsForVacationCalendars(final VacationDO vacation)
  {
    final List<VacationCalendarDO> vacationCalendarDOs = vacationDao.getVacationCalendarDOs(vacation);
    for (VacationCalendarDO vacationCalendarDO : vacationCalendarDOs) {
      if (!vacationCalendarDO.isDeleted()) {
        vacationCalendarDO.setEvent(getAndUpdateOrCreateTeamEventDO(vacationCalendarDO));
        vacationDao.saveVacationCalendar(vacationCalendarDO);
      }
    }
  }

  private VacationCalendarDO getOrCreateVacationCalendarDO(final VacationDO vacation, final TeamCalDO teamCalDO)
  {
    final List<VacationCalendarDO> vacationCalendarDOs = vacationDao.getVacationCalendarDOs(vacation);
    for (final VacationCalendarDO vacationCalendarDO : vacationCalendarDOs) {
      if (vacationCalendarDO.getCalendar().equals(teamCalDO)) {
        vacationCalendarDO.setDeleted(false);
        return vacationCalendarDO;
      }
    }
    final VacationCalendarDO vacationCalendarDO = new VacationCalendarDO();
    vacationCalendarDO.setCalendar(teamCalDO);
    vacationCalendarDO.setVacation(vacation);
    return vacationCalendarDO;
  }

  private CalEventDO getAndUpdateOrCreateTeamEventDO(final VacationCalendarDO vacationCalendarDO)
  {
    final Timestamp startTimestamp = new Timestamp(vacationCalendarDO.getVacation().getStartDate().getTime());
    final Timestamp endTimestamp = new Timestamp(vacationCalendarDO.getVacation().getEndDate().getTime());

    if (vacationCalendarDO.getEvent() != null) {
      final CalEventDO vacationTeamEvent = calEventDao.internalGetById(vacationCalendarDO.getEvent().getId());
      if (vacationTeamEvent != null) {
        if (vacationTeamEvent.isDeleted()) {
          calEventDao.internalUndelete(vacationTeamEvent);
        }

        if (!vacationTeamEvent.getStartDate().equals(startTimestamp) || !vacationTeamEvent.getEndDate().equals(endTimestamp)) {
          vacationTeamEvent.setStartDate(startTimestamp);
          vacationTeamEvent.setEndDate(endTimestamp);
          calEventDao.internalSaveOrUpdate(vacationTeamEvent);
        }
      }
      return vacationTeamEvent;
    } else {
      final CalEventDO newCalEventDO = new CalEventDO();
      newCalEventDO.setAllDay(true);
      newCalEventDO.setStartDate(startTimestamp);
      newCalEventDO.setEndDate(endTimestamp);
      newCalEventDO.setSubject(vacationCalendarDO.getVacation().getEmployee().getUser().getFullname());
      newCalEventDO.setCalendar(vacationCalendarDO.getCalendar());
      calEventDao.internalSave(newCalEventDO);
      return newCalEventDO;
    }
  }
}
