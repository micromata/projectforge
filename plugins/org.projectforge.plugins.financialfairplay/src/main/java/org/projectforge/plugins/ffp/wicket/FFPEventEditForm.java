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

package org.projectforge.plugins.ffp.wicket;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.plugins.ffp.model.FFPAccountingDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.projectforge.plugins.ffp.repository.FFPEventService;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.employee.EmployeeWicketProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.MyListDataProvider;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.components.MinMaxNumberField;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.InputPanel;
import org.projectforge.web.wicket.flowlayout.TablePanel;

import com.vaynberg.wicket.select2.Select2MultiChoice;

public class FFPEventEditForm extends AbstractEditForm<FFPEventDO, FFPEventEditPage>
{
  private static final long serialVersionUID = 8746545908106124484L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(FFPEventEditForm.class);

  @SpringBean
  private EmployeeService employeeService;

  @SpringBean
  private FFPEventService eventService;

  protected MultiChoiceListHelper<EmployeeDO> assignAttendeesListHelper;

  public FFPEventEditForm(final FFPEventEditPage parentPage, final FFPEventDO data)
  {
    super(parentPage, data);
  }

  @Override
  protected void init()
  {
    super.init();
    if (isNew()) {
      data.addAttendee(employeeService.getEmployeeByUserId(ThreadLocalUserContext.getUserId()));
    }

    gridBuilder.newSplitPanel(GridSize.COL25, true).newSubSplitPanel(GridSize.COL100);
    {
      // Event date
      final FieldsetPanel fs = gridBuilder.newFieldset(FFPEventDO.class, "eventDate");
      fs.add(new DatePanel(fs.newChildId(), new PropertyModel<>(data, "eventDate"), new DatePanelSettings()));
    }
    {
      // Division
      final FieldsetPanel fs = gridBuilder.newFieldset(FFPEventDO.class, "title");
      MaxLengthTextField titel = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "title"));
      titel.setMarkupId("titel").setOutputMarkupId(true);
      fs.add(titel);
    }
    {
      // ATTENDEES
      final FieldsetPanel fieldSet = gridBuilder.newFieldset(getString("plugins.ffp.attendees"));

      Set<Integer> set = getData().getAttendeeList().stream().map(EmployeeDO::getPk)
          .collect(Collectors.toCollection(TreeSet::new));
      assignAttendeesListHelper = new MultiChoiceListHelper<EmployeeDO>()
          .setComparator(new Comparator<EmployeeDO>()
          {

            @Override
            public int compare(EmployeeDO o1, EmployeeDO o2)
            {
              return o1.getPk().compareTo(o2.getPk());
            }

          }).setFullList(employeeService.findAllActive(false));
      if (set != null) {
        for (final Integer attendeeId : set) {
          final EmployeeDO attendee = employeeService.selectByPkDetached(attendeeId);
          if (attendee != null) {
            assignAttendeesListHelper.addOriginalAssignedItem(attendee).assignItem(attendee);
          }
        }
      }

      final Select2MultiChoice<EmployeeDO> attendees = new Select2MultiChoice<EmployeeDO>(
          fieldSet.getSelect2MultiChoiceId(),
          new PropertyModel<Collection<EmployeeDO>>(this.assignAttendeesListHelper, "assignedItems"),
          new EmployeeWicketProvider(employeeService));
      attendees.setRequired(true).setMarkupId("attendees").setOutputMarkupId(true);
      fieldSet.add(attendees);
    }
    //Transactions
    createViewOnlyDataTable(gridBuilder);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  public void createViewOnlyDataTable(GridBuilder gridBuilder)
  {
    DivPanel section = gridBuilder.getPanel();
    TablePanel tablePanel = new TablePanel(section.newChildId());
    section.add(tablePanel);
    final DataTable<FFPAccountingDO, String> dataTable = new DataTable<FFPAccountingDO, String>(TablePanel.TABLE_ID,
        createColumns(),
        new MyListDataProvider<FFPAccountingDO>()
        {
          private static final long serialVersionUID = 8665407746104587402L;

          @Override
          protected List<FFPAccountingDO> loadList()
          {
            return data.getAccountingList();
          }

        }, 50);
    tablePanel.add(dataTable);
  }

  private List<IColumn<FFPAccountingDO, String>> createColumns()
  {
    final List<IColumn<FFPAccountingDO, String>> columns = new ArrayList<IColumn<FFPAccountingDO, String>>();
    columns.add(new PropertyColumn<FFPAccountingDO, String>(new ResourceModel("name"), "attendee.user.fullname"));
    columns.add(new PropertyColumn<FFPAccountingDO, String>(new ResourceModel("plugins.ffp.value"), "value")
    {
      private static final long serialVersionUID = 3672950740712610620L;

      @Override
      public void populateItem(Item<ICellPopulator<FFPAccountingDO>> item, String componentId,
          IModel<FFPAccountingDO> rowModel)
      {
        item.add(new InputPanel(componentId,
            new MinMaxNumberField<BigDecimal>(InputPanel.WICKET_ID, new PropertyModel<>(rowModel.getObject(), "value"),
                new BigDecimal(Integer.MIN_VALUE), new BigDecimal(Integer.MAX_VALUE))));
      }

    });
    return columns;
  }
}
