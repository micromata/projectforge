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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.configuration.ConfigurationServiceImpl;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.vacation.model.VacationAttrProperty;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.business.vacation.service.VacationServiceImpl;
import org.projectforge.framework.time.PFDay;
import org.projectforge.test.TestSetup;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.LocalDatePanel;
import org.wicketstuff.select2.Select2Choice;
import org.wicketstuff.select2.Select2MultiChoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collection;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatePanel.class, Form.class})
@PowerMockIgnore({"javax.activation.*", "javax.management.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xerces.*", "ch.qos.logback.*", "org.slf4j.*"})
public class VacationFormValidatorTest {
  private EmployeeDO employee;

  private VacationService vacationService;

  private ConfigurationService configService;

  private LocalDate startDate;

  private LocalDate endDate;

  private LocalDatePanel startDatePanel;

  private LocalDatePanel endDatePanel;

  private boolean halfDay;

  private CheckBox halfDayCheckBox;

  private boolean isSpecial;

  private CheckBox isSpecialCheckBox;

  private DropDownChoice<VacationStatus> statusChoice;

  private Select2Choice<EmployeeDO> employeeSelect;

  private Select2MultiChoice<TeamCalDO> calendars;

  private Collection<TeamCalDO> teamCalDO;

  @Before
  public void setUp() {
    TestSetup.init();
    this.employee = mock(EmployeeDO.class);
    when(this.employee.getUrlaubstage()).thenReturn(30);
    this.vacationService = mock(VacationServiceImpl.class);
    this.configService = mock(ConfigurationServiceImpl.class);
    this.startDate = LocalDate.of(2017, Month.JANUARY, 1);
    this.endDate = LocalDate.of(2017, Month.JANUARY, 1);
    this.startDatePanel = mock(LocalDatePanel.class);
    this.endDatePanel = mock(LocalDatePanel.class);
    this.halfDay = false;
    this.halfDayCheckBox = mock(CheckBox.class);
    this.isSpecial = false;
    this.isSpecialCheckBox = mock(CheckBox.class);
    this.statusChoice = mock(DropDownChoice.class);
    this.employeeSelect = mock(Select2Choice.class);
    this.calendars = mock(Select2MultiChoice.class);
    this.teamCalDO = mock(Collection.class);
    LocalDate vacationEndDate = LocalDate.now().with(Month.MARCH).withDayOfMonth(31);
    when(this.vacationService.getEndDateVacationFromLastYear()).thenReturn(vacationEndDate);
    when(this.configService.getVacationCalendar()).thenReturn(null);
    when(this.vacationService.getVacationDays(any(LocalDate.class), any(LocalDate.class), any(Boolean.class))).thenCallRealMethod();
  }

  @Test
  public void afterMarchTest() {
    this.startDate = this.startDate.with(Month.APRIL).withDayOfMonth(3);
    this.endDate = this.endDate.with(Month.APRIL).withDayOfMonth(13);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.getYear())).thenReturn(new BigDecimal(10));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(5));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(0)).error(any());
  }

  @Test
  public void afterMarchNegativeTest() {
    this.startDate = this.startDate.with(Month.APRIL).withDayOfMonth(3);
    this.endDate = this.endDate.with(Month.APRIL).withDayOfMonth(13);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.getYear())).thenReturn(new BigDecimal(30));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class)).thenReturn(new BigDecimal(5));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(1)).error(any());
  }

  @Test
  public void beforeMarchTest() {
    this.startDate = this.startDate.with(Month.MARCH).withDayOfMonth(3);
    this.endDate = this.endDate.with(Month.MARCH).withDayOfMonth(13);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.getYear())).thenReturn(new BigDecimal(10));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class))
            .thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class))
            .thenReturn(new BigDecimal(3));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(0)).error(any());
  }

  @Test
  public void beforeMarchNoPreviousYearVacationTest() {
    this.startDate = this.startDate.with(Month.MARCH).withDayOfMonth(3);
    this.endDate = this.endDate.with(Month.MARCH).withDayOfMonth(13);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.getYear())).thenReturn(new BigDecimal(10));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class))
            .thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class))
            .thenReturn(new BigDecimal(5));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(0)).error(any());
  }

  @Test
  public void beforeMarchWithPreviousYearVacationTest() {
    this.startDate = this.startDate.with(Month.MARCH).withDayOfMonth(3);
    this.endDate = this.endDate.with(Month.MARCH).withDayOfMonth(13);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.getYear())).thenReturn(new BigDecimal(10));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class))
            .thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class))
            .thenReturn(new BigDecimal(3));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(0)).error(any());
  }

  @Test
  public void beforeMarchNoPreviousYearNegativVacationTest() {
    this.startDate = this.startDate.with(Month.MARCH).withDayOfMonth(3);
    this.endDate = this.endDate.with(Month.MARCH).withDayOfMonth(13);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.getYear())).thenReturn(new BigDecimal(30));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class))
            .thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class))
            .thenReturn(new BigDecimal(3));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(1)).error(any());
  }

  @Test
  public void overMarchWithPreviousYearVacationTest() {
    this.startDate = this.startDate.with(Month.MARCH).withDayOfMonth(30);
    this.endDate = this.endDate.with(Month.APRIL).withDayOfMonth(10);
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.getYear())).thenReturn(new BigDecimal(10));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVE.getPropertyName(), BigDecimal.class))
            .thenReturn(new BigDecimal(5));
    when(this.employee.getAttribute(VacationAttrProperty.PREVIOUSYEARLEAVEUSED.getPropertyName(), BigDecimal.class))
            .thenReturn(new BigDecimal(3));
    VacationFormValidator validator = createValidator();
    Form<?> form = mock(Form.class);
    validator.validate(form);
    verify(form, times(0)).error(any());
  }

  @Test
  public void oneDayAndHalfDaySelectedTest() {
    this.startDate = this.startDate.with(Month.APRIL).withDayOfMonth(3);
    this.endDate = this.endDate.with(Month.APRIL).withDayOfMonth(3);
    this.halfDay = true;
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.getYear())).thenReturn(new BigDecimal(0));

    final VacationFormValidator validator = createValidator();
    final Form<?> form = mock(Form.class);
    validator.validate(form);

    verify(form, times(0)).error(any());
  }

  @Test
  public void moreThanOneDayAndHalfDaySelectedTest() {
    this.startDate = this.startDate.with(Month.APRIL).withDayOfMonth(2);
    this.endDate = this.endDate.with(Month.APRIL).withDayOfMonth(20);
    this.halfDay = true;
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.getYear())).thenReturn(new BigDecimal(0));

    final VacationFormValidator validator = createValidator();
    final Form<?> form = mock(Form.class);
    validator.validate(form);

    verify(form, times(1)).error(any());
  }

  @Test
  public void zeroDaysOfVacation() {
    this.startDate = this.startDate.with(Month.APRIL).withDayOfMonth(2);
    this.endDate = this.endDate.with(Month.APRIL).withDayOfMonth(2);
    this.halfDay = true;
    when(this.vacationService.getApprovedAndPlanedVacationdaysForYear(this.employee, startDate.getYear())).thenReturn(new BigDecimal(0));

    final VacationFormValidator validator = createValidator();
    final Form<?> form = mock(Form.class);
    validator.validate(form);

    verify(form, times(1)).error(any());
  }

  private VacationFormValidator createValidator() {
    LocalDate date = LocalDate.of(2017, Month.JANUARY, 1);
    final VacationFormValidator validator = new VacationFormValidator(vacationService, configService, new VacationDO(), date);

    validator.getDependentFormComponents()[0] = startDatePanel;
    validator.getDependentFormComponents()[1] = endDatePanel;
    validator.getDependentFormComponents()[2] = statusChoice;
    validator.getDependentFormComponents()[3] = employeeSelect;
    validator.getDependentFormComponents()[4] = halfDayCheckBox;
    validator.getDependentFormComponents()[5] = isSpecialCheckBox;
    validator.getDependentFormComponents()[6] = calendars;

    when(startDatePanel.getConvertedInput()).thenReturn(PFDay.from(startDate).getSqlDate());
    when(endDatePanel.getConvertedInput()).thenReturn(PFDay.from(endDate).getSqlDate());
    when(statusChoice.getConvertedInput()).thenReturn(VacationStatus.IN_PROGRESS);
    when(employeeSelect.getConvertedInput()).thenReturn(this.employee);
    when(halfDayCheckBox.getConvertedInput()).thenReturn(this.halfDay);
    when(isSpecialCheckBox.getConvertedInput()).thenReturn(this.isSpecial);
    when(calendars.getConvertedInput()).thenReturn(this.teamCalDO);

    return validator;
  }

}
