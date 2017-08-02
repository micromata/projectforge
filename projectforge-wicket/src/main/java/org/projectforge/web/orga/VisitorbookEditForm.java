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

package org.projectforge.web.orga;

import java.util.Collection;
import java.util.function.Function;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeComparator;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.business.orga.VisitorType;
import org.projectforge.business.orga.VisitorbookDO;
import org.projectforge.business.orga.VisitorbookService;
import org.projectforge.business.orga.VisitorbookTimedDO;
import org.projectforge.framework.persistence.attr.impl.GuiAttrSchemaService;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LabelValueChoiceRenderer;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.wicketstuff.select2.Select2MultiChoice;

import de.micromata.genome.db.jpa.tabattr.api.AttrGroup;

public class VisitorbookEditForm extends AbstractEditForm<VisitorbookDO, VisitorbookEditPage>
{
  private static final long serialVersionUID = 8746545908106124484L;

  private static final Logger log = Logger.getLogger(VisitorbookEditForm.class);

  @SpringBean
  private VisitorbookService visitorbookService;

  @SpringBean
  private EmployeeService employeeService;

  @SpringBean
  private GuiAttrSchemaService attrSchemaService;

  protected MultiChoiceListHelper<EmployeeDO> assignContactPersonsListHelper;

  protected VisitorbookEmployeeWicketProvider employeeWicketProvider;

  public VisitorbookEditForm(final VisitorbookEditPage parentPage, final VisitorbookDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void init()
  {
    super.init();

    gridBuilder.newSplitPanel(GridSize.COL50, true).newSubSplitPanel(GridSize.COL100);
    {
      // Firstname
      final FieldsetPanel fs = gridBuilder.newFieldset(VisitorbookDO.class, "firstname");
      MaxLengthTextField firstname = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "firstname"));
      firstname.setMarkupId("firstname").setOutputMarkupId(true);
      firstname.setRequired(true);
      fs.add(firstname);
    }
    {
      // Lastname
      final FieldsetPanel fs = gridBuilder.newFieldset(VisitorbookDO.class, "lastname");
      MaxLengthTextField lastname = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "lastname"));
      lastname.setMarkupId("lastname").setOutputMarkupId(true);
      lastname.setRequired(true);
      fs.add(lastname);
    }
    {
      // Company
      final FieldsetPanel fs = gridBuilder.newFieldset(VisitorbookDO.class, "company");
      MaxLengthTextField company = new MaxLengthTextField(InputPanel.WICKET_ID, new PropertyModel<>(data, "company"));
      company.setMarkupId("company").setOutputMarkupId(true);
      fs.add(company);
    }
    {
      // ATTENDEE
      final FieldsetPanel fieldSet = gridBuilder.newFieldset(getString("orga.visitorbook.contactPerson"));

      final Collection<Integer> set = visitorbookService.getAssignedContactPersonsIds(data);
      assignContactPersonsListHelper = new MultiChoiceListHelper<EmployeeDO>()
          .setComparator(new EmployeeComparator()).setFullList(
              employeeService.getAll(false));
      if (set != null) {
        for (final Integer employeeId : set) {
          final EmployeeDO employee = employeeService.selectByPkDetached(employeeId);
          if (employee != null) {
            assignContactPersonsListHelper.addOriginalAssignedItem(employee).assignItem(employee);
          }
        }
      }
      employeeWicketProvider = new VisitorbookEmployeeWicketProvider(data, employeeService);
      final Select2MultiChoice<EmployeeDO> employees = new Select2MultiChoice<EmployeeDO>(
          fieldSet.getSelect2MultiChoiceId(),
          new PropertyModel<Collection<EmployeeDO>>(this.assignContactPersonsListHelper, "assignedItems"),
          employeeWicketProvider);
      employees.setMarkupId("contactPersons").setOutputMarkupId(true);
      fieldSet.add(employees);
    }

    {
      // DropDownChoice visitor type
      final FieldsetPanel fs = gridBuilder.newFieldset(VisitorbookDO.class, "visitortype");
      final LabelValueChoiceRenderer<VisitorType> visitortypeChoiceRenderer = new LabelValueChoiceRenderer<>(this,
          VisitorType.values());
      final DropDownChoice<VisitorType> statusChoice = new DropDownChoice<>(
          fs.getDropDownChoiceId(),
          new PropertyModel<>(data, "visitortype"),
          visitortypeChoiceRenderer.getValues(),
          visitortypeChoiceRenderer);
      statusChoice.setNullValid(false).setRequired(true);
      statusChoice.setMarkupId("visitortype").setOutputMarkupId(true);
      fs.add(statusChoice);
    }

    gridBuilder.newSplitPanel(GridSize.COL100, true); // set hasSubSplitPanel to true to remove borders from this split panel
    {
      // AttrPanels
      final Function<AttrGroup, VisitorbookTimedDO> addNewEntryFunction = group -> visitorbookService.addNewTimeAttributeRow(data, group.getName());
      attrSchemaService.createTimedAttrPanels(gridBuilder.getPanel(), data, parentPage, addNewEntryFunction);
    }

  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
