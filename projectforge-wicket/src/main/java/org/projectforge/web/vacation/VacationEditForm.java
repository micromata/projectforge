/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.service.TeamCalServiceImpl;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.employee.DefaultEmployeeWicketProvider;
import org.projectforge.web.teamcal.admin.TeamCalsProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.CheckBoxPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.LabelPanel;
import org.projectforge.web.wicket.flowlayout.Select2SingleChoicePanel;

import com.vaynberg.wicket.select2.Select2Choice;
import com.vaynberg.wicket.select2.Select2MultiChoice;

public class VacationEditForm extends AbstractEditForm<VacationDO, VacationEditPage>
{
  private static final long serialVersionUID = 8746545901236124484L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(VacationEditForm.class);

  @SpringBean
  private VacationService vacationService;

  @SpringBean
  private EmployeeService employeeService;

  @SpringBean
  private TenantService tenantService;

  @SpringBean
  private ConfigurationService configService;

  @SpringBean
  private AccessChecker accessChecker;

  @SpringBean
  private TeamCalServiceImpl teamCalService;

  @SpringBean
  private TeamCalCache teamCalCache;

  private Label neededVacationDaysLabel;

  private Model<String> neededVacationDaysModel;

  private Label availableVacationDaysLabel;

  private Model<String> availableVacationDaysModel;

  private VacationStatus statusBeforeModification;

  MultiChoiceListHelper<TeamCalDO> assignCalendarListHelper;

  public VacationEditForm(final VacationEditPage parentPage, final VacationDO data)
  {
    super(parentPage, data);
    vacationService.couldUserUseVacationService(ThreadLocalUserContext.getUser(), true);
    if (data.getEmployee() == null) {
      if (parentPage.employeeIdFromPageParameters != null) {
        data.setEmployee(employeeService.selectByPkDetached(parentPage.employeeIdFromPageParameters));
      } else {
        data.setEmployee(employeeService.getEmployeeByUserId(ThreadLocalUserContext.getUserId()));
      }
    }
    if (isNew() == false) {
      statusBeforeModification = data.getStatus();
    }
  }

  @Override
  public void onBeforeRender()
  {
    super.onBeforeRender();
    //Check write access
    //If status is approved only HR can make changes
    if ((isNew() == false && checkWriteAccess() == false) || (VacationStatus.APPROVED.equals(data.getStatus())
        && accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READWRITE) == false)) {
      markAsDeletedButtonPanel.setVisible(false);
      deleteButtonPanel.setVisible(false);
      updateButtonPanel.setVisible(false);
    }
  }

  @Override
  protected void init()
  {
    super.init();
    if (checkReadAccess() == false) {
      throw new AccessException("access.exception.userHasNotRight");
    }
    VacationFormValidator formValidator = new VacationFormValidator(vacationService, data);
    add(formValidator);

    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Employee
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("vacation.employee"));
      final Select2Choice<EmployeeDO> employeeSelect = new Select2Choice<>(
          Select2SingleChoicePanel.WICKET_ID,
          new PropertyModel<>(data, "employee"),
          new DefaultEmployeeWicketProvider(employeeService, true));
      employeeSelect.setRequired(true).setMarkupId("vacation-employee").setOutputMarkupId(true);
      employeeSelect.setEnabled(checkHRWriteRight());
      employeeSelect.add(new AjaxFormComponentUpdatingBehavior("onchange")
      {
        private static final long serialVersionUID = 2462231234993745889L;

        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          BigDecimal availableVacationDays = getAvailableVacationDays(data);
          availableVacationDaysModel.setObject(availableVacationDays.toString());
          target.add(availableVacationDaysLabel);
        }
      });
      formValidator.getDependentFormComponents()[3] = employeeSelect;
      fs.add(new Select2SingleChoicePanel<EmployeeDO>(fs.newChildId(), employeeSelect));
    }

    {
      // Start date
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "startDate");
      DatePanel startDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(data, "startDate"),
          DatePanelSettings.get().withTargetType(java.sql.Date.class), true);
      startDatePanel.getDateField().add(new AjaxFormComponentUpdatingBehavior("onchange")
      {
        private static final long serialVersionUID = 2462233190993745889L;

        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          BigDecimal availableVacationDays = getAvailableVacationDays(data);
          availableVacationDaysModel.setObject(availableVacationDays.toString());
          target.add(availableVacationDaysLabel);

          if (getData().getStartDate() != null && getData().getEndDate() != null) {
            String value = DayHolder.getNumberOfWorkingDays(data.getStartDate(), data.getEndDate()).toString();
            neededVacationDaysModel.setObject(value);
            target.add(neededVacationDaysLabel);
          }
        }
      });
      startDatePanel.setRequired(true).setMarkupId("vacation-startdate").setOutputMarkupId(true);
      startDatePanel.setEnabled(checkEnableInputField());
      formValidator.getDependentFormComponents()[0] = startDatePanel;
      fs.add(startDatePanel);
    }

    {
      // End date
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "endDate");
      DatePanel endDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(data, "endDate"),
          DatePanelSettings.get().withTargetType(java.sql.Date.class), true);
      endDatePanel.getDateField().add(new AjaxFormComponentUpdatingBehavior("onchange")
      {
        private static final long serialVersionUID = 2462233112393745889L;

        @Override
        protected void onUpdate(final AjaxRequestTarget target)
        {
          if (getData().getStartDate() != null && getData().getEndDate() != null) {
            String value = DayHolder.getNumberOfWorkingDays(data.getStartDate(), data.getEndDate()).toString();
            neededVacationDaysModel.setObject(value);
            target.add(neededVacationDaysLabel);
          }
        }
      });
      endDatePanel.setRequired(true).setMarkupId("vacation-enddate").setOutputMarkupId(true);
      endDatePanel.setEnabled(checkEnableInputField());
      formValidator.getDependentFormComponents()[1] = endDatePanel;
      fs.add(endDatePanel);
    }

    {
      // Special holiday
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "isSpecial");
      CheckBoxPanel checkboxPanel = new CheckBoxPanel(fs.newChildId(), new PropertyModel<>(data, "isSpecial"), "");
      checkboxPanel.setMarkupId("vacation-isSpecial").setOutputMarkupId(true);
      checkboxPanel.setEnabled(checkEnableInputField());
      formValidator.setIsSpecialCheckbox(checkboxPanel);
      fs.add(checkboxPanel);
    }

    {
      // Available vacation days
      final FieldsetPanel fs = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage("vacation.availabledays"));
      BigDecimal availableVacationDays = getAvailableVacationDays(data);
      this.availableVacationDaysModel = new Model<>(availableVacationDays.toString());
      LabelPanel availablePanel = new LabelPanel(fs.newChildId(), availableVacationDaysModel);
      availablePanel.setMarkupId("vacation-availableDays").setOutputMarkupId(true);
      this.availableVacationDaysLabel = availablePanel.getLabel();
      this.availableVacationDaysLabel.setOutputMarkupId(true);
      fs.add(availablePanel);
    }

    {
      // Needed vacation days
      FieldsetPanel neededVacationDaysFs = gridBuilder
          .newFieldset(I18nHelper.getLocalizedMessage("vacation.neededdays"));
      String value = I18nHelper.getLocalizedMessage("vacation.setStartAndEndFirst");
      if (data.getStartDate() != null && data.getEndDate() != null) {
        value = DayHolder.getNumberOfWorkingDays(data.getStartDate(), data.getEndDate()).toString();
      }
      this.neededVacationDaysModel = new Model<>(value);
      LabelPanel neededVacationDaysPanel = new LabelPanel(neededVacationDaysFs.newChildId(), neededVacationDaysModel);
      neededVacationDaysPanel.setMarkupId("vacation-neededVacationDays").setOutputMarkupId(true);
      this.neededVacationDaysLabel = neededVacationDaysPanel.getLabel();
      this.neededVacationDaysLabel.setOutputMarkupId(true);
      neededVacationDaysFs.add(neededVacationDaysPanel);
    }

    {
      // Manager
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("vacation.manager"));
      final Select2Choice<EmployeeDO> managerSelect = new Select2Choice<>(
          Select2SingleChoicePanel.WICKET_ID,
          new PropertyModel<>(data, "manager"),
          new DefaultEmployeeWicketProvider(employeeService, checkHRWriteRight()));
      managerSelect.setRequired(true).setMarkupId("vacation-manager").setOutputMarkupId(true);
      managerSelect.setEnabled(checkEnableInputField());
      fs.add(new Select2SingleChoicePanel<EmployeeDO>(fs.newChildId(), managerSelect));
    }

    {
      // Substitution
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("vacation.substitution"));
      final Select2Choice<EmployeeDO> substitutionSelect = new Select2Choice<>(
          Select2SingleChoicePanel.WICKET_ID,
          new PropertyModel<>(data, "substitution"),
          new DefaultEmployeeWicketProvider(employeeService, checkHRWriteRight()));
      substitutionSelect.setRequired(true).setMarkupId("vacation-substitution").setOutputMarkupId(true);
      substitutionSelect.setEnabled(checkEnableInputField());
      fs.add(new Select2SingleChoicePanel<EmployeeDO>(fs.newChildId(), substitutionSelect));
    }

    {
      // CALENDAR
      final FieldsetPanel fieldSet = gridBuilder.newFieldset(getString("vacation.calendar"));
      Collection<TeamCalDO> fullList = teamCalService.getFullAccessCalendar();
      TeamCalDO vacationCalendar = configService.getVacationCalendar();
      if (vacationCalendar != null) {
        fullList.add(vacationCalendar);
      }
      assignCalendarListHelper = new MultiChoiceListHelper<TeamCalDO>()
          .setComparator(new Comparator<TeamCalDO>()
          {
            @Override
            public int compare(TeamCalDO o1, TeamCalDO o2)
            {
              return o1.getPk().compareTo(o2.getPk());
            }

          }).setFullList(fullList);
      if (vacationCalendar != null) {
        assignCalendarListHelper.addOriginalAssignedItem(vacationCalendar).assignItem(vacationCalendar);
      }

      if (vacationService.getCalendarsForVacation(this.data) != null && vacationService.getCalendarsForVacation(this.data).size() > 0) {
        for (final TeamCalDO calendar : vacationService.getCalendarsForVacation(this.data)) {
          if (vacationCalendar == null || calendar.getId().equals(vacationCalendar.getId()) == false) {
            assignCalendarListHelper.addOriginalAssignedItem(calendar).assignItem(calendar);
          }
        }
      }

      final Select2MultiChoice<TeamCalDO> calendars = new Select2MultiChoice<TeamCalDO>(
          fieldSet.getSelect2MultiChoiceId(),
          new PropertyModel<Collection<TeamCalDO>>(assignCalendarListHelper, "assignedItems"),
          new TeamCalsProvider(teamCalCache));
      calendars.setMarkupId("calenders").setOutputMarkupId(true);
      calendars.setEnabled(checkEnableInputField());
      formValidator.getDependentFormComponents()[4] = calendars;
      fieldSet.add(calendars);
    }

    {
      // DropDownChoice status
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "status");
      final LabelValueChoiceRenderer<VacationStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<>(
          this,
          VacationStatus.values());
      final DropDownChoice<VacationStatus> statusChoice = new DropDownChoice<>(fs.getDropDownChoiceId(),
          new PropertyModel<>(data, "status"), statusChoiceRenderer.getValues(), statusChoiceRenderer);
      statusChoice.setNullValid(false).setRequired(true);
      statusChoice.setMarkupId("vacation-status").setOutputMarkupId(true);
      statusChoice.setEnabled(hasUserEditStatusRight());
      formValidator.getDependentFormComponents()[2] = statusChoice;
      fs.add(statusChoice);
    }

  }

  private BigDecimal getAvailableVacationDays(VacationDO vacationData)
  {
    BigDecimal availableVacationDays;
    if (vacationData.getStartDate() != null) {
      Calendar startDateCalendar = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
      startDateCalendar.setTime(vacationData.getStartDate());
      availableVacationDays = vacationService.getAvailableVacationdaysForYear(vacationData.getEmployee(), startDateCalendar.get(Calendar.YEAR), true);
    } else {
      Calendar now = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
      availableVacationDays = vacationService.getAvailableVacationdaysForYear(vacationData.getEmployee(), now.get(Calendar.YEAR), true);
    }
    return availableVacationDays;
  }

  private boolean checkEnableInputField()
  {
    boolean result = false;
    if (data != null) {
      if (VacationStatus.APPROVED.equals(data.getStatus()) == true) {
        if (accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READWRITE) == true) {
          result = true;
        }
      } else {
        if (data.getEmployee().getUser().getPk().equals(ThreadLocalUserContext.getUserId()) || accessChecker
            .hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READWRITE)) {
          result = true;
        }
      }
    }
    return result;
  }

  private boolean hasUserEditStatusRight()
  {
    if (VacationStatus.APPROVED.equals(data.getStatus())) {
      //Only HR can change approved applications
      if (checkWriteAccess()) {
        if (data.getEmployee().getUser().getPk().equals(ThreadLocalUserContext.getUserId()) == true
            || data.getManager().getUser().getPk().equals(ThreadLocalUserContext.getUserId()) == true) {
          return false;
        } else {
          return true;
        }
      }
    }
    //Only HR and manager can edit status when in progress
    if (checkWriteAccess()) {
      if (data.getEmployee().getUser().getPk().equals(ThreadLocalUserContext.getUserId()) == true) {
        return false;
      } else {
        return true;
      }
    }
    return false;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  private boolean checkHRWriteRight()
  {
    if (accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READWRITE)) {
      return true;
    }
    return false;
  }

  private boolean checkWriteAccess()
  {
    if (data.getEmployee().getUser().getPk().equals(ThreadLocalUserContext.getUserId()) == true || (data.getManager() != null
        && data.getManager().getUser().getPk().equals(ThreadLocalUserContext.getUserId())) == true) {
      return true;
    }
    if (checkHRWriteRight()) {
      return true;
    }
    return false;
  }

  private boolean checkReadAccess()
  {
    if (data.getEmployee().getUser().getPk().equals(ThreadLocalUserContext.getUserId()) == true || (data.getManager() != null
        && data.getManager().getUser().getPk().equals(ThreadLocalUserContext.getUserId())) == true || (data.getSubstitution() != null
        && data.getSubstitution().getUser().getPk().equals(ThreadLocalUserContext.getUserId())) == true) {
      return true;
    }
    if (accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READONLY,
        UserRightValue.READWRITE)) {
      return true;
    }
    return false;
  }

  public VacationStatus getStatusBeforeModification()
  {
    return statusBeforeModification;
  }

  @Override
  protected void updateButtonVisibility()
  {
    super.updateButtonVisibility();
    //Set delete button only for employee or hr write right
    markAsDeletedButtonPanel.setVisible(false);
    if (data.getEmployee().getUser().getPk().equals(ThreadLocalUserContext.getUserId()) || checkHRWriteRight()) {
      markAsDeletedButtonPanel.setVisible(true);
    }
  }
}
