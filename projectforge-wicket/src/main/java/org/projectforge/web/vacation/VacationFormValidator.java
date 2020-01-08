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

package org.projectforge.web.vacation;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.service.VacationCalendarService;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.business.vacation.service.VacationValidator;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.web.wicket.components.LocalDatePanel;
import org.wicketstuff.select2.Select2Choice;
import org.wicketstuff.select2.Select2MultiChoice;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;

public class VacationFormValidator implements IFormValidator {
  private static final long serialVersionUID = -8478416045860851983L;

  // Components for form validation.
  private final FormComponent<?>[] dependentFormComponents = new FormComponent[7];

  private final VacationService vacationService;

  private final VacationCalendarService vacationCalendarService;

  private final VacationDO data;

  private ConfigurationService configService;

  private final LocalDate now;

  public VacationFormValidator(VacationService vacationService, VacationService vacationServiceNew, VacationCalendarService vacationCalendarService, ConfigurationService configService, VacationDO data) {
    this(vacationService, vacationServiceNew, vacationCalendarService, configService, data, LocalDate.now());
  }

  /**
   * FOR TEST USE ONLY!
   *
   * @param vacationService
   * @param configService
   * @param data
   * @param now
   */
  protected VacationFormValidator(VacationService vacationService, VacationService vacationServiceNew, VacationCalendarService vacationCalendarService, ConfigurationService configService, VacationDO data, LocalDate now) {
    this.configService = configService;
    this.vacationService = vacationService;
    this.vacationCalendarService = vacationCalendarService;
    this.data = data;
    this.now = now;
  }

  @Override
  public void validate(final Form<?> form) {
    final LocalDatePanel startDatePanel = (LocalDatePanel) dependentFormComponents[0];
    final LocalDatePanel endDatePanel = (LocalDatePanel) dependentFormComponents[1];
    final DropDownChoice<VacationStatus> statusChoice = (DropDownChoice<VacationStatus>) dependentFormComponents[2];
    final Select2Choice<EmployeeDO> employeeSelect = (Select2Choice<EmployeeDO>) dependentFormComponents[3];
    final CheckBox isHalfDayCheckbox = (CheckBox) dependentFormComponents[4];
    final CheckBox isSpecialCheckbox = (CheckBox) dependentFormComponents[5];
    final Select2MultiChoice<TeamCalDO> calendars = (Select2MultiChoice<TeamCalDO>) dependentFormComponents[6];

    EmployeeDO employee = employeeSelect.getConvertedInput();
    if (employee == null) {
      employee = data.getEmployee();
    }

    final LocalDate startDate = startDatePanel.getConvertedInputAsLocalDate();
    final LocalDate endDate = endDatePanel.getConvertedInputAsLocalDate();

    VacationDO vacation = new VacationDO();
    vacation.setId(data.getId());
    vacation.setStartDate(startDate);
    vacation.setEndDate(endDate);
    vacation.setEmployee(employee);
    vacation.setSpecial(isOn(isSpecialCheckbox));
    vacation.setHalfDayBegin(isOn(isHalfDayCheckbox));
    vacation.setStatus(statusChoice.getConvertedInput());

    VacationValidator.Error error = vacationService.validate(vacation, null,false);

    if (error != null) {
      form.error(I18nHelper.getLocalizedMessage(error.getMessageKey()));
    }

    //Getting selected calendars from form component or direct from data
    final Collection<TeamCalDO> selectedCalendars = getSelectedCalendars(calendars);

    // check Vacation Calender
    final TeamCalDO configuredVacationCalendar = configService.getVacationCalendar();
    if (configuredVacationCalendar != null) {
      if (selectedCalendars == null || selectedCalendars.contains(configuredVacationCalendar) == false) {
        form.error(I18nHelper.getLocalizedMessage("vacation.validate.noCalender", configuredVacationCalendar.getTitle()));
        return;
      }
    }
  }

  private Collection<TeamCalDO> getSelectedCalendars(final Select2MultiChoice<TeamCalDO> calendars) {
    final Collection<TeamCalDO> selectedCalendars = new HashSet<>();
    if (calendars != null && calendars.getConvertedInput() != null && calendars.getConvertedInput().size() > 0) {
      selectedCalendars.addAll(calendars.getConvertedInput());
    } else {
      selectedCalendars.addAll(vacationCalendarService.getCalendarsForVacation(this.data));
    }
    return selectedCalendars;
  }

  private boolean isOn(final CheckBox checkBox) {
    final Boolean value = checkBox.getConvertedInput();
    return Boolean.TRUE.equals(value);
  }

  @Override
  public FormComponent<?>[] getDependentFormComponents() {
    return dependentFormComponents;
  }

}
