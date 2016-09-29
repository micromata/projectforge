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

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.multitenancy.TenantService;
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
import org.projectforge.web.employee.EmployeeWicketProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.LabelPanel;
import org.projectforge.web.wicket.flowlayout.Select2SingleChoicePanel;

import com.vaynberg.wicket.select2.Select2Choice;

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
  private AccessChecker accessChecker;

  private Label neededVacationDaysLabel;

  private Model<String> neededVacationDaysModel;

  public VacationEditForm(final VacationEditPage parentPage, final VacationDO data)
  {
    super(parentPage, data);
    data.setEmployee(employeeService.getEmployeeByUserId(ThreadLocalUserContext.getUserId()));
  }

  @Override
  protected void init()
  {
    if (checkReadAccess() == false) {
      throw new AccessException("access.exception.userHasNotRight");
    }
    if (isNew() == false && checkWriteAccess() == false) {
      markAsDeletedButtonPanel.setVisible(false);
      deleteButtonPanel.setVisible(false);
      updateButtonPanel.setVisible(false);
    }
    super.init();
    VacationFormValidator formValidator = new VacationFormValidator(vacationService, data);
    add(formValidator);

    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Start date
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "startDate");
      DatePanel startDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(data, "startDate"),
          new DatePanelSettings(), true);
      startDatePanel.getDateField().add(new AjaxFormComponentUpdatingBehavior("onchange")
      {
        private static final long serialVersionUID = 2462233190993745889L;

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
      startDatePanel.setRequired(true).setMarkupId("vacation-startdate").setOutputMarkupId(true);
      formValidator.getDependentFormComponents()[0] = startDatePanel;
      fs.add(startDatePanel);
    }

    {
      // End date
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "endDate");
      DatePanel endDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(data, "endDate"),
          new DatePanelSettings(), true);
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
      formValidator.getDependentFormComponents()[1] = endDatePanel;
      fs.add(endDatePanel);
    }

    {
      // Available vacation days
      final FieldsetPanel fs = gridBuilder.newFieldset(I18nHelper.getLocalizedMessage("vacation.availabledays"));
      BigDecimal availableVacationDays = vacationService.getAvailableVacationdays(data.getEmployee());
      LabelPanel availablePanel = new LabelPanel(fs.newChildId(), availableVacationDays.toString());
      availablePanel.setMarkupId("vacation-availableDays").setOutputMarkupId(true);
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
      neededVacationDaysPanel.setMarkupId("vacation-availableDays").setOutputMarkupId(true);
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
          new EmployeeWicketProvider(employeeService));
      managerSelect.setRequired(true).setMarkupId("vacation-manager").setOutputMarkupId(true);
      fs.add(new Select2SingleChoicePanel<EmployeeDO>(fs.newChildId(), managerSelect));
    }

    {
      // Substitution
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("vacation.substitution"));
      final Select2Choice<EmployeeDO> substitutionSelect = new Select2Choice<>(
          Select2SingleChoicePanel.WICKET_ID,
          new PropertyModel<>(data, "substitution"),
          new EmployeeWicketProvider(employeeService));
      substitutionSelect.setRequired(true).setMarkupId("vacation-substitution").setOutputMarkupId(true);
      fs.add(new Select2SingleChoicePanel<EmployeeDO>(fs.newChildId(), substitutionSelect));
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
      fs.add(statusChoice);
    }
  }

  private boolean hasUserEditStatusRight()
  {
    return false;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  private boolean checkWriteAccess()
  {
    if (data.getEmployee().getUser().getPk().equals(ThreadLocalUserContext.getUserId()) == true
        || (data.getManager() != null
            && data.getManager().getUser().getPk().equals(ThreadLocalUserContext.getUserId())) == true) {
      return true;
    }
    if (accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READWRITE)) {
      return true;
    }
    return false;
  }

  private boolean checkReadAccess()
  {
    if (data.getEmployee().getUser().getPk().equals(ThreadLocalUserContext.getUserId()) == true
        || (data.getManager() != null
            && data.getManager().getUser().getPk().equals(ThreadLocalUserContext.getUserId())) == true) {
      return true;
    }
    if (accessChecker.hasLoggedInUserRight(UserRightId.HR_VACATION, false, UserRightValue.READONLY,
        UserRightValue.READWRITE)) {
      return true;
    }
    return false;
  }

}
