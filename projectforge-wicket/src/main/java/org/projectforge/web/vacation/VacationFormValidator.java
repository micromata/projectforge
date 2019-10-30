package org.projectforge.web.vacation;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.vacation.model.VacationAttrProperty;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.components.DatePanel;
import org.wicketstuff.select2.Select2Choice;
import org.wicketstuff.select2.Select2MultiChoice;

public class VacationFormValidator implements IFormValidator
{
  private static final long serialVersionUID = -8478416045860851983L;

  // Components for form validation.
  private final FormComponent<?>[] dependentFormComponents = new FormComponent[7];

  private final VacationService vacationService;

  private final VacationDO data;

  private ConfigurationService configService;

  private final Calendar now;

  public VacationFormValidator(VacationService vacationService, ConfigurationService configService, VacationDO data)
  {
    this(vacationService, configService, data, Calendar.getInstance(ThreadLocalUserContext.getTimeZone()));
  }

  /**
   * FOR TEST USE ONLY!
   *
   * @param vacationService
   * @param configService
   * @param data
   * @param now
   */
  protected VacationFormValidator(VacationService vacationService, ConfigurationService configService, VacationDO data, Calendar now)
  {
    this.configService = configService;
    this.vacationService = vacationService;
    this.data = data;
    this.now = now;
  }

  @Override
  public void validate(final Form<?> form)
  {
    final DatePanel startDatePanel = (DatePanel) dependentFormComponents[0];
    final DatePanel endDatePanel = (DatePanel) dependentFormComponents[1];
    final DropDownChoice<VacationStatus> statusChoice = (DropDownChoice<VacationStatus>) dependentFormComponents[2];
    final Select2Choice<EmployeeDO> employeeSelect = (Select2Choice<EmployeeDO>) dependentFormComponents[3];
    final CheckBox isHalfDayCheckbox = (CheckBox) dependentFormComponents[4];
    final CheckBox isSpecialCheckbox = (CheckBox) dependentFormComponents[5];
    final Select2MultiChoice<TeamCalDO> calendars = (Select2MultiChoice<TeamCalDO>) dependentFormComponents[6];

    EmployeeDO employee = employeeSelect.getConvertedInput();
    if (employee == null) {
      employee = data.getEmployee();
    }

    if (checkOnlyStatusChange(statusChoice)) {
      return;
    }

    final Calendar startDate = getValueFromFormOrData(startDatePanel);
    final Calendar endDate = getValueFromFormOrData(endDatePanel);

    //Getting selected calendars from form component or direct from data
    final Collection<TeamCalDO> selectedCalendars = getSelectedCalendars(calendars);

    //Is new vacation data
    if (data.getId() == null) {
      if(startDate.before(this.now) && vacationService.hasLoggedInUserHRVacationAccess() == false) {
        form.error(I18nHelper.getLocalizedMessage("vacation.validate.startDateBeforeNow"));
        return;
      }
    }

    if (validateStartAndEndDate(form, startDate, endDate)) {
      return;
    }

    // only one day allowed if half day checkbox is active
    if (isOn(isHalfDayCheckbox) && endDate.equals(startDate) == false) {
      form.error(I18nHelper.getLocalizedMessage("vacation.validate.moreThanOneDaySelectedOnHalfDay"));
      return;
    }

    if (validateOnlyOneVacationPerPeriod(form, employee, startDate, endDate)) {
      return;
    }

    //vacationdays < 0.5 days
    if (vacationService.getVacationDays(startDate.getTime(), endDate.getTime(), isOn(isHalfDayCheckbox)).compareTo(new BigDecimal(0.5)) < 0) {
      form.error(I18nHelper.getLocalizedMessage("vacation.validate.daysarenull"));
      return;
    }

    // check Vacation Calender
    final TeamCalDO configuredVacationCalendar = configService.getVacationCalendar();
    if (configuredVacationCalendar != null) {
      if (selectedCalendars == null || selectedCalendars.contains(configuredVacationCalendar) == false) {
        form.error(I18nHelper.getLocalizedMessage("vacation.validate.noCalender", configuredVacationCalendar.getTitle()));
        return;
      }
    }

    if (isOn(isSpecialCheckbox)) {
      return;
    }

    if (isEnoughDaysLeft(isHalfDayCheckbox, employee, startDate, endDate) == false) {
      form.error(I18nHelper.getLocalizedMessage("vacation.validate.notEnoughVacationDaysLeft"));
      return;
    }
  }

  private boolean isEnoughDaysLeft(final CheckBox isHalfDayCheckbox, final EmployeeDO employee, final Calendar startDate, final Calendar endDate)
  {
    boolean enoughDaysLeft = true;
    final Calendar endDateVacationFromLastYear = vacationService.getEndDateVacationFromLastYear();

    //Positiv
    final BigDecimal vacationDays = new BigDecimal(employee.getUrlaubstage());
    final BigDecimal vacationDaysFromLastYear = employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class) != null
        ? employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)
        : BigDecimal.ZERO;

    //Negative
    final BigDecimal usedVacationDaysWholeYear = vacationService.getApprovedAndPlanedVacationdaysForYear(employee, startDate.get(Calendar.YEAR));
    final BigDecimal usedVacationDaysFromLastYear =
        employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class) != null
            ? employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)
            : BigDecimal.ZERO;
    final BigDecimal usedVacationDaysWithoutDaysFromLastYear = usedVacationDaysWholeYear.subtract(usedVacationDaysFromLastYear);

    //Available
    BigDecimal availableVacationDays = vacationDays.subtract(usedVacationDaysWithoutDaysFromLastYear);
    final BigDecimal availableVacationDaysFromLastYear = vacationDaysFromLastYear.subtract(usedVacationDaysFromLastYear);

    //Add the old data working days to available days
    if (data.getPk() != null) {
      final BigDecimal oldDataWorkingDays = vacationService.getVacationDays(data.getStartDate(), data.getEndDate(), data.getHalfDay());
      availableVacationDays = availableVacationDays.add(oldDataWorkingDays);
    }

    //Need
    final BigDecimal neededVacationDays = vacationService
        .getVacationDays(startDate.getTime(), endDate.getTime(), isHalfDayCheckbox.getConvertedInput());

    //Vacation after end days from last year
    if (startDate.after(endDateVacationFromLastYear)) {
      if (availableVacationDays.subtract(neededVacationDays).compareTo(BigDecimal.ZERO) < 0) {
        enoughDaysLeft = false;
      }
    }
    //Vacation before end days from last year
    if (endDate.before(endDateVacationFromLastYear)
        || endDate.equals(endDateVacationFromLastYear)) {
      if (availableVacationDays.add(availableVacationDaysFromLastYear).subtract(neededVacationDays)
          .compareTo(BigDecimal.ZERO) < 0) {
        enoughDaysLeft = false;
      }
    }
    //Vacation over end days from last year
    if ((startDate.before(endDateVacationFromLastYear)
        || startDate.equals(endDateVacationFromLastYear))
        && endDate.after(endDateVacationFromLastYear)) {
      final BigDecimal neededVacationDaysBeforeEndFromLastYear = vacationService
          .getVacationDays(startDate.getTime(), endDateVacationFromLastYear.getTime(),
              false); // here we are sure that it is no half day vacation

      final BigDecimal restFromLastYear = availableVacationDaysFromLastYear.subtract(neededVacationDaysBeforeEndFromLastYear);
      if (restFromLastYear.compareTo(BigDecimal.ZERO) <= 0) {
        if (availableVacationDays.subtract(neededVacationDays).compareTo(BigDecimal.ZERO) < 0) {
          enoughDaysLeft = false;
        }
      } else {
        if (availableVacationDays.subtract(neededVacationDays.subtract(restFromLastYear))
            .compareTo(BigDecimal.ZERO) < 0) {
          enoughDaysLeft = false;
        }
      }
    }
    return enoughDaysLeft;
  }

  private boolean validateOnlyOneVacationPerPeriod(final Form<?> form, final EmployeeDO employee, final Calendar startDate, final Calendar endDate)
  {
    // check if there is already a leave application in the period
    List<VacationDO> vacationListForPeriod = vacationService
        .getVacationForDate(employee, startDate.getTime(), endDate.getTime(), true);
    if (vacationListForPeriod != null && data.getPk() != null) {
      vacationListForPeriod = vacationListForPeriod
          .stream()
          .filter(vac -> vac.getPk().equals(data.getPk()) == false) // remove current vacation from list in case this is an update
          .collect(Collectors.toList());
    }
    if (vacationListForPeriod != null && vacationListForPeriod.size() > 0) {
      form.error(I18nHelper.getLocalizedMessage("vacation.validate.leaveapplicationexists"));
      return true;
    }
    return false;
  }

  private boolean validateStartAndEndDate(final Form<?> form, final Calendar startDate, final Calendar endDate)
  {
    if (startDate == null || endDate == null) {
      form.error(I18nHelper.getLocalizedMessage("vacation.validate.datenotset"));
      return true;
    }

    //Check, if start date is before end date
    if (endDate.before(startDate)) {
      form.error(I18nHelper.getLocalizedMessage("vacation.validate.endbeforestart"));
      return true;
    }

    //Check, if both dates are in same year
    if (endDate.get(Calendar.YEAR) > startDate.get(Calendar.YEAR)) {
      form.error(I18nHelper.getLocalizedMessage("vacation.validate.vacationIn2Years"));
      return true;
    }
    return false;
  }

  private Collection<TeamCalDO> getSelectedCalendars(final Select2MultiChoice<TeamCalDO> calendars)
  {
    final Collection<TeamCalDO> selectedCalendars = new HashSet<>();
    if (calendars != null && calendars.getConvertedInput() != null && calendars.getConvertedInput().size() > 0) {
      selectedCalendars.addAll(calendars.getConvertedInput());
    } else {
      selectedCalendars.addAll(vacationService.getCalendarsForVacation(this.data));
    }
    return selectedCalendars;
  }

  private Calendar getValueFromFormOrData(final DatePanel datePanel)
  {
    final Calendar date = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    if (datePanel != null && datePanel.getConvertedInput() != null) {
      date.setTime(datePanel.getConvertedInput());
    } else {
      date.setTime(data.getStartDate());
    }
    return date;
  }

  private boolean checkOnlyStatusChange(final DropDownChoice<VacationStatus> statusChoice)
  {
    if (statusChoice != null && statusChoice.getConvertedInput() != null && data.getStatus() != null) {
      if (
        //Changes from IN_PROGRESS to APPROVED or REJECTED
          (VacationStatus.IN_PROGRESS.equals(data.getStatus()) && (VacationStatus.APPROVED.equals(statusChoice.getConvertedInput()) || VacationStatus.REJECTED
              .equals(statusChoice.getConvertedInput())))
              ||
              //Changes from REJECTED to APPROVED or IN_PROGRESS
              (VacationStatus.REJECTED.equals(data.getStatus()) && (VacationStatus.APPROVED.equals(statusChoice.getConvertedInput())
                  || VacationStatus.IN_PROGRESS.equals(statusChoice.getConvertedInput())))
          ) {
        return true;
      }
    }
    return false;
  }

  private boolean isOn(final CheckBox checkBox)
  {
    final Boolean value = checkBox.getConvertedInput();
    return Boolean.TRUE.equals(value);
  }

  @Override
  public FormComponent<?>[] getDependentFormComponents()
  {
    return dependentFormComponents;
  }

}
