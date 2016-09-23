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

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.employee.EmployeeWicketProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
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

  // Components for form validation.
  private final FormComponent<?>[] dependentFormComponents = new FormComponent[2];

  public VacationEditForm(final VacationEditPage parentPage, final VacationDO data)
  {
    super(parentPage, data);
    data.setEmployee(employeeService.getEmployeeByUserId(ThreadLocalUserContext.getUserId()));
  }

  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL50);
    {
      // Start date
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "startDate");
      DatePanel startDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(data, "startDate"),
          new DatePanelSettings());
      startDatePanel.setRequired(true).setMarkupId("vacation-startdate").setOutputMarkupId(true);
      dependentFormComponents[0] = startDatePanel;
      fs.add(startDatePanel);
    }

    {
      // End date
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "endDate");
      DatePanel endDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(data, "endDate"),
          new DatePanelSettings());
      endDatePanel.setRequired(true).setMarkupId("vacation-enddate").setOutputMarkupId(true);
      dependentFormComponents[1] = endDatePanel;
      fs.add(endDatePanel);
    }

    add(new IFormValidator()
    {
      private static final long serialVersionUID = -8478416045860851983L;

      @Override
      public void validate(final Form<?> form)
      {
        final DatePanel startDatePanel = (DatePanel) dependentFormComponents[0];
        final DatePanel endDatePanel = (DatePanel) dependentFormComponents[1];

        if (endDatePanel.getConvertedInput().before(startDatePanel.getConvertedInput())) {
          error(I18nHelper.getLocalizedMessage("vacation.validate.endbeforestart"));
          return;
        }

        List<VacationDO> vacationListForPeriod = vacationService.getVacationForDate(data.getEmployee(),
            startDatePanel.getConvertedInput(), endDatePanel.getConvertedInput());
        if (vacationListForPeriod != null && vacationListForPeriod.size() > 0) {
          error(I18nHelper.getLocalizedMessage("vacation.validate.leaveapplicationexists"));
        }
      }

      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return dependentFormComponents;
      }
    });

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

}
