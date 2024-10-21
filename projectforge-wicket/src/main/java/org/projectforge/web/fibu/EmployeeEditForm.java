/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.fibu;

import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.PatternValidator;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.EmployeeService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.*;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.List;

public class EmployeeEditForm extends AbstractEditForm<EmployeeDO, EmployeeEditPage> {
  private static final long serialVersionUID = 8746545908106124484L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeEditForm.class);

  private static final BigDecimal NUMBER_OF_WEEK_HOURS = new BigDecimal(168);

  public EmployeeEditForm(final EmployeeEditPage parentPage, final EmployeeDO data) {
    super(parentPage, data);
  }

  @Override
  protected void init() {
    super.init();

    // replace the GridBuilder from superclass by our TabPanel
    final TabPanel tabPanel = new TabPanel("flowform");
    replace(tabPanel);
    gridBuilder = tabPanel.getOrCreateTab("fibu.employee.coredata", true); // create the default tab

    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL100);
    {
      // User
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "user");
      final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(),
          new PropertyModel<>(data, "user"), parentPage,
          "userId");
      userSelectPanel.getWrappedComponent().setMarkupId("user").setOutputMarkupId(true);
      userSelectPanel.setShowSelectMeButton(false).setRequired(true);
      userSelectPanel.add((IValidator<PFUserDO>) validatable -> {
        PFUserDO user = validatable.getModel().getObject();
        if (user != null && user.getId() != null) {
          EmployeeDO employeeByUserId = WicketSupport.get(EmployeeService.class).findByUserId(user.getId());
          if (employeeByUserId != null && employeeByUserId.getId().equals(data.getId()) == false) {
            validatable.error(new ValidationError().addKey("fibu.employee.error.employeWithUserExists"));
          }
        }
      });
      fs.add(userSelectPanel);
      userSelectPanel.init();
    }
    {
      // cost 1
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "kost1");
      Kost1FormComponent kost1 = new Kost1FormComponent(InputPanel.WICKET_ID, new PropertyModel<>(data, "kost1"), true);
      kost1.setMarkupId("kost1").setOutputMarkupId(true);
      fs.add(kost1);
    }
    {
      // Division
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "abteilung");
      final PFAutoCompleteTextField<String> abteilungField = new PFAutoCompleteTextField<String>(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "abteilung")) {
        @Override
        protected List<String> getChoices(final String input) {
          return parentPage.getBaseDao().getAutocompletion("abteilung", input);
        }
      };
      abteilungField.withMatchContains(true).withMinChars(2);
      //abteilung.setMarkupId("abteilung").setOutputMarkupId(true);
      fs.add(abteilungField);
    }
    {
      // Position
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "position");
      final PFAutoCompleteTextField<String> positionField = new PFAutoCompleteTextField<String>(InputPanel.WICKET_ID,
          new PropertyModel<String>(data, "position")) {
        @Override
        protected List<String> getChoices(final String input) {
          return parentPage.getBaseDao().getAutocompletion("position", input);
        }
      };
      positionField.withMatchContains(true).withMinChars(2);
      //position.setMarkupId("position").setOutputMarkupId(true);
      fs.add(positionField);
    }

    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL100);
    {
      // Staff number
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "staffNumber");
      MaxLengthTextField position = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "staffNumber"));
      position.setMarkupId("staffNumber").setOutputMarkupId(true);
      position.add(new PatternValidator("[a-zA-Z0-9]*"));
      fs.add(position);
    }
    {
      // Weekly hours
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "weeklyWorkingHours");
      fs.add(new MinMaxNumberField<>(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "weeklyWorkingHours"), BigDecimal.ZERO, NUMBER_OF_WEEK_HOURS));
    }
    /*{
      // Holidays
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "urlaubstage");
      MinMaxNumberField<Integer> fieldHolidays = new MinMaxNumberField<>(InputPanel.WICKET_ID, new PropertyModel<>(data, "urlaubstage"), 0, 366);
      fieldHolidays.setMarkupId("urlaubstage").setOutputMarkupId(true);
      fs.add(fieldHolidays);
    }*/
    {
      // Start date
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "eintrittsDatum");
      fs.add(new LocalDatePanel(fs.newChildId(), new LocalDateModel(new PropertyModel<>(data, "eintrittsDatum"))));
    }
    {
      // End date
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "austrittsDatum");
      LocalDatePanel austrittsDatum = new LocalDatePanel(fs.newChildId(), new LocalDateModel(new PropertyModel<>(data, "austrittsDatum")));
      austrittsDatum.getDateField().setMarkupId("endDate").setOutputMarkupId(true);
      fs.add(austrittsDatum);
    }

 /*   {
      // AttrPanels
      final Function<AttrGroup, EmployeeTimedDO> addNewEntryFunction = group -> employeeService
          .addNewTimeAttributeRow(data, group.getName());
      attrSchemaService.createAttrPanels(tabPanel, data, parentPage, addNewEntryFunction);
    }*/

    gridBuilder.newSplitPanel(GridSize.COL100, true).newSubSplitPanel(GridSize.COL100);
    {
      // Comment
      final FieldsetPanel fs = gridBuilder.newFieldset(EmployeeDO.class, "comment");
      fs.add(new MaxLengthTextArea(TextAreaPanel.WICKET_ID, new PropertyModel<>(data, "comment")), true);
    }
  }

  @Override
  protected Logger getLogger() {
    return log;
  }
}
