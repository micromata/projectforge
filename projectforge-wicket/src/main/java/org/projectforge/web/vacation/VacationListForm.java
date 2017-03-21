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

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.vacation.VacationFilter;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.model.VacationMode;
import org.projectforge.business.vacation.model.VacationStatus;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class VacationListForm extends AbstractListForm<VacationFilter, VacationListPage>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(VacationListForm.class);

  private static final long serialVersionUID = -5969136444233092172L;

  @SpringBean
  private EmployeeService employeeService;

  @SpringBean
  private VacationService vacationService;

  public VacationListForm(final VacationListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected void init()
  {
    super.init();
    final VacationFilter filter = getSearchFilter();
    {
      gridBuilder.newSplitPanel(GridSize.COL66);
      {
        // DropDownChoice visitor type
        final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "status");
        final LabelValueChoiceRenderer<VacationStatus> vacationstatusChoiceRenderer = new LabelValueChoiceRenderer<>(parentPage,
            VacationStatus.values());
        final DropDownChoice<VacationStatus> statusChoice = new DropDownChoice<>(
            fs.getDropDownChoiceId(),
            new PropertyModel<>(filter, "vacationstatus"),
            vacationstatusChoiceRenderer.getValues(),
            vacationstatusChoiceRenderer);
        statusChoice.setMarkupId("filter_vacationstatus").setOutputMarkupId(true);
        statusChoice.setNullValid(true);
        fs.add(statusChoice);
      }
      {
        // DropDownChoice visitor type
        final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "vacationmode");
        final LabelValueChoiceRenderer<VacationMode> vacationmodeChoiceRenderer = new LabelValueChoiceRenderer<>(parentPage,
            VacationMode.values());
        final DropDownChoice<VacationMode> modeChoice = new DropDownChoice<>(
            fs.getDropDownChoiceId(),
            new PropertyModel<>(filter, "vacationmode"),
            vacationmodeChoiceRenderer.getValues(),
            vacationmodeChoiceRenderer);
        modeChoice.setMarkupId("filter_vacationmode").setOutputMarkupId(true);
        modeChoice.setNullValid(true);
        fs.add(modeChoice);
      }
    }
  }

  @Override
  protected VacationFilter newSearchFilterInstance()
  {
    EmployeeDO employee = employeeService.getEmployeeByUserId(ThreadLocalUserContext.getUserId());
    if (employee == null) {
      throw new AccessException("access.exception.noEmployeeToUser");
    }
    return new VacationFilter(employee.getPk());
  }

  @Override
  public VacationFilter getSearchFilter()
  {
    super.getSearchFilter();
    if (this.searchFilter == null || this.searchFilter.getEmployeeId() == null) {
      VacationFilter vf = newSearchFilterInstance();
      if (this.searchFilter != null) {
        this.searchFilter.setEmployeeId(vf.getEmployeeId());
      } else {
        this.searchFilter = vf;
      }
    }
    return this.searchFilter;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

}
