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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.vacation.model.VacationDO;
import org.projectforge.business.vacation.service.VacationService;
import org.projectforge.web.employee.EmployeeWicketProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
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

  public VacationEditForm(final VacationEditPage parentPage, final VacationDO data)
  {
    super(parentPage, data);
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
      startDatePanel.setRequired(true).setMarkupId("vacation.startdate").setOutputMarkupId(true);
      fs.add(startDatePanel);
    }

    {
      // End date
      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "endDate");
      DatePanel endDatePanel = new DatePanel(fs.newChildId(), new PropertyModel<>(data, "endDate"),
          new DatePanelSettings());
      endDatePanel.setRequired(true).setMarkupId("vacation.enddate").setOutputMarkupId(true);
      fs.add(endDatePanel);
    }

    {
      // Manager
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("vacation.manager"));
      final Select2Choice<EmployeeDO> managerSelect = new Select2Choice<EmployeeDO>(
          Select2SingleChoicePanel.WICKET_ID,
          new PropertyModel<EmployeeDO>(data, "manager"),
          new EmployeeWicketProvider(data.getManager(), employeeService));
      managerSelect.setMarkupId("vacation.manager").setOutputMarkupId(true);
      fs.add(new Select2SingleChoicePanel<EmployeeDO>(fs.newChildId(), managerSelect));
    }

    // replace the GridBuilder from superclass by our TabPanel
    //    final TabPanel tabPanel = new TabPanel("flowform");
    //    replace(tabPanel);
    //    gridBuilder = tabPanel.getOrCreateTab("fibu.Vacation.coredata", true); // create the default tab
    //
    //    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL100);
    //    {
    //      // User
    //      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "user");
    //      final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(),
    //          new PropertyModel<>(data, "user"), parentPage,
    //          "userId");
    //      userSelectPanel.getWrappedComponent().setMarkupId("user").setOutputMarkupId(true);
    //      userSelectPanel.setShowSelectMeButton(false).setRequired(true);
    //      fs.add(userSelectPanel);
    //      userSelectPanel.init();
    //    }
    //    {
    //      // cost 1
    //      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "kost1");
    //      Kost1FormComponent kost1 = new Kost1FormComponent(InputPanel.WICKET_ID, new PropertyModel<>(data, "kost1"), true);
    //      kost1.setMarkupId("kost1").setOutputMarkupId(true);
    //      fs.add(kost1);
    //    }
    //    {
    //      // DropDownChoice status
    //      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "status");
    //      final LabelValueChoiceRenderer<VacationStatus> statusChoiceRenderer = new LabelValueChoiceRenderer<>(
    //          this,
    //          VacationStatus.values());
    //      final DropDownChoice<VacationStatus> statusChoice = new DropDownChoice<>(fs.getDropDownChoiceId(),
    //          new PropertyModel<>(data, "status"), statusChoiceRenderer.getValues(), statusChoiceRenderer);
    //      statusChoice.setNullValid(false).setRequired(true);
    //      statusChoice.setMarkupId("status").setOutputMarkupId(true);
    //      fs.add(statusChoice);
    //    }
    //    {
    //      // Division
    //      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "abteilung");
    //      MaxLengthTextField abteilung = new MaxLengthTextField(InputPanel.WICKET_ID,
    //          new PropertyModel<>(data, "abteilung"));
    //      abteilung.setMarkupId("abteilung").setOutputMarkupId(true);
    //      fs.add(abteilung);
    //    }
    //    {
    //      // Position
    //      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "position");
    //      MaxLengthTextField position = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "position"));
    //      position.setMarkupId("position").setOutputMarkupId(true);
    //      fs.add(position);
    //    }
    //
    //    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL100);
    //    {
    //      // Staff number
    //      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "staffNumber");
    //      MaxLengthTextField position = new MaxLengthTextField(InputPanel.WICKET_ID,
    //          new PropertyModel<>(data, "staffNumber"));
    //      position.setMarkupId("staffNumber").setOutputMarkupId(true);
    //      position.add(new PatternValidator("[a-zA-Z0-9]*"));
    //      fs.add(position);
    //    }
    //    {
    //      // Weekly hours
    //      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "weeklyWorkingHours");
    //      fs.add(new MinMaxNumberField<>(InputPanel.WICKET_ID,
    //          new PropertyModel<>(data, "weeklyWorkingHours"), BigDecimal.ZERO, NUMBER_OF_WEEK_HOURS));
    //    }
    //    {
    //      // Holidays
    //      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "urlaubstage");
    //      fs.add(new MinMaxNumberField<>(InputPanel.WICKET_ID, new PropertyModel<>(data, "urlaubstage"), 0, 366));
    //    }
    //    {
    //      // Start date
    //      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "eintrittsDatum");
    //      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<>(data, "eintrittsDatum"), new DatePanelSettings()));
    //    }
    //    {
    //      // End date
    //      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "austrittsDatum");
    //      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<>(data, "austrittsDatum"), new DatePanelSettings()));
    //    }
    //
    //    gridBuilder.newSplitPanel(GridSize.COL50, true);
    //    gridBuilder.newSubSplitPanel(GridSize.COL50);
    //    generateStreetZipCityFields(gridBuilder, data);
    //
    //    gridBuilder.newSubSplitPanel(GridSize.COL50);
    //    generateCountryStateFields(gridBuilder, data);
    //
    //    gridBuilder.newSplitPanel(GridSize.COL25, true).newSubSplitPanel(GridSize.COL100);
    //    createBirthdayPanel(gridBuilder, data);
    //
    //    {
    //      // DropDownChoice gender
    //      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "gender");
    //      final LabelValueChoiceRenderer<Gender> genderChoiceRenderer = new LabelValueChoiceRenderer<>(this,
    //          Gender.values());
    //      final DropDownChoice<Gender> statusChoice = new DropDownChoice<>(
    //          fs.getDropDownChoiceId(),
    //          new PropertyModel<>(data, "gender"),
    //          genderChoiceRenderer.getValues(),
    //          genderChoiceRenderer);
    //      statusChoice.setNullValid(false).setRequired(true);
    //      statusChoice.setMarkupId("gender").setOutputMarkupId(true);
    //      fs.add(statusChoice);
    //    }
    //    gridBuilder.newSplitPanel(GridSize.COL25, true).newSubSplitPanel(GridSize.COL100);
    //    createBankingDetails(gridBuilder, data);
    //
    //    gridBuilder.newSplitPanel(GridSize.COL100, true); // set hasSubSplitPanel to true to remove borders from this split panel
    //    {
    //      // AttrPanels
    //      final Function<AttrGroup, VacationTimedDO> addNewEntryFunction = group -> VacationService
    //          .addNewTimeAttributeRow(data, group.getName());
    //      attrSchemaService.createAttrPanels(tabPanel, data, parentPage, addNewEntryFunction);
    //    }
    //
    //    gridBuilder.newSplitPanel(GridSize.COL100, true).newSubSplitPanel(GridSize.COL100);
    //    {
    //      // Comment
    //      final FieldsetPanel fs = gridBuilder.newFieldset(VacationDO.class, "comment");
    //      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<>(data, "comment")), true);
    //    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
