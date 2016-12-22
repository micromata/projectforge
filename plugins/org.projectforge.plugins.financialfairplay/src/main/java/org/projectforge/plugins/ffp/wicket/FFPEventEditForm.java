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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.business.fibu.api.EmployeeService;
import org.projectforge.framework.persistence.api.IdObject;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.MyBeanComparator;
import org.projectforge.plugins.ffp.model.FFPAccountingDO;
import org.projectforge.plugins.ffp.model.FFPEventDO;
import org.projectforge.plugins.ffp.repository.FFPEventService;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.employee.EmployeeWicketProvider;
import org.projectforge.web.wicket.AbstractEditForm;
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

  private TablePanel tablePanel;

  private DataTable<FFPAccountingDO, String> dataTable;

  protected Set<FFPAccountingDO> accountingList;

  @Override
  protected void init()
  {
    super.init();
    if (isNew()) {
      EmployeeDO userEmployee = employeeService.getEmployeeByUserId(ThreadLocalUserContext.getUserId());
      if (userEmployee != null) {
        data.addAttendee(userEmployee);
      }
    }

    gridBuilder.newSplitPanel(GridSize.COL25, true).newSubSplitPanel(GridSize.COL100);
    {
      // Event date
      final FieldsetPanel fs = gridBuilder.newFieldset(FFPEventDO.class, "eventDate");
      DatePanel eventDate = new DatePanel(fs.newChildId(), new PropertyModel<>(data, "eventDate"), new DatePanelSettings());
      eventDate.setRequired(true);
      eventDate.setMarkupId("eventDate").setOutputMarkupId(true);
      fs.add(eventDate);
    }
    {
      // Division
      final FieldsetPanel fs = gridBuilder.newFieldset(FFPEventDO.class, "title");
      MaxLengthTextField titel = new MaxLengthTextField(InputPanel.WICKET_ID,
          new PropertyModel<>(data, "title"));
      titel.setRequired(true);
      titel.setMarkupId("eventTitel").setOutputMarkupId(true);
      fs.add(titel);
    }
    {
      // ATTENDEES
      final FieldsetPanel fieldSet = gridBuilder.newFieldset(getString("plugins.ffp.attendees"));

      Set<Integer> set = null;
      if (getData().getAttendeeList().size() > 0) {
        set = getData().getAttendeeList().stream().map(EmployeeDO::getPk)
            .collect(Collectors.toCollection(HashSet::new));
      }
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
          new EmployeeWicketProvider(employeeService, true));
      attendees.setRequired(true).setMarkupId("attendees").setOutputMarkupId(true);
      attendees.add(new OnChangeAjaxBehavior()
      {
        @Override
        protected void onUpdate(AjaxRequestTarget target)
        {
          dataTable = createDataTable(createColumns(), "attendee.user.fullname", SortOrder.ASCENDING, getData());
          tablePanel.addOrReplace(dataTable);
          target.add(dataTable);
        }
      });
      fieldSet.add(attendees);
    }
    //Transactions
    createOrRefreshDataTable(gridBuilder);
  }

  private void refreshDataTable()
  {

  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  public void createOrRefreshDataTable(GridBuilder gridBuilder)
  {
    DivPanel section = gridBuilder.getPanel();
    this.tablePanel = new TablePanel(section.newChildId());
    this.tablePanel.setOutputMarkupId(true);
    section.add(tablePanel);
    this.dataTable = createDataTable(createColumns(), "attendee.user.fullname", SortOrder.ASCENDING, getData());
    tablePanel.add(this.dataTable);
  }

  private DataTable<FFPAccountingDO, String> createDataTable(final List<IColumn<FFPAccountingDO, String>> columns,
      final String sortProperty, final SortOrder sortOrder, final FFPEventDO event)
  {
    final SortParam<String> sortParam = sortProperty != null
        ? new SortParam<String>(sortProperty, sortOrder == SortOrder.ASCENDING) : null;
    DefaultDataTable<FFPAccountingDO, String> localDataTable = new DefaultDataTable<>(TablePanel.TABLE_ID, columns,
        createSortableDataProvider(sortParam, event), 50);
    localDataTable.setOutputMarkupId(true);
    localDataTable.setMarkupId("attendeeDataTable");
    return localDataTable;
  }

  private ISortableDataProvider<FFPAccountingDO, String> createSortableDataProvider(final SortParam<String> sortParam,
      FFPEventDO event)
  {
    return new FFPAccountingPageSortableDataProvider<FFPAccountingDO>(sortParam, getAccountings(), event);
  }

  public Set<FFPAccountingDO> getAccountings()
  {
    if (this.accountingList == null) {
      this.accountingList = new HashSet<>();
    }
    //Existing attendee data
    if (getData().getAccountingList() != null && getData().getAccountingList().size() > 0) {
      getData().getAccountingList().forEach(acc -> {
        this.accountingList.add(acc);
      });
    }
    //New added attendee data
    if (assignAttendeesListHelper != null && assignAttendeesListHelper.getItemsToAssign() != null) {
      assignAttendeesListHelper.getItemsToAssign().forEach(emp -> {
        FFPAccountingDO accounting = new FFPAccountingDO();
        accounting.setEvent(getData());
        accounting.setAttendee(emp);
        this.accountingList.add(accounting);
      });
    }
    //Removed attendee data
    if (assignAttendeesListHelper != null && assignAttendeesListHelper.getItemsToUnassign() != null) {
      assignAttendeesListHelper.getItemsToUnassign().forEach(emp -> {
        Set<FFPAccountingDO> toRemove = new HashSet<>();
        this.accountingList.forEach(acc -> {
          if (acc.getAttendee().getPk().equals(emp.getPk())) {
            toRemove.add(acc);
          }
        });
        this.accountingList.removeAll(toRemove);
      });
    }
    return this.accountingList;
  }

  private List<IColumn<FFPAccountingDO, String>> createColumns()
  {
    final List<IColumn<FFPAccountingDO, String>> columns = new ArrayList<>();
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
    columns.add(new PropertyColumn<FFPAccountingDO, String>(new ResourceModel("plugins.ffp.weighting"), "weighting")
    {
      private static final long serialVersionUID = 367295074123610620L;

      @Override
      public void populateItem(Item<ICellPopulator<FFPAccountingDO>> item, String componentId,
          IModel<FFPAccountingDO> rowModel)
      {
        item.add(new InputPanel(componentId,
            new MinMaxNumberField<BigDecimal>(InputPanel.WICKET_ID, new PropertyModel<>(rowModel.getObject(), "weighting"),
                new BigDecimal(Integer.MIN_VALUE), new BigDecimal(Integer.MAX_VALUE))));
      }

    });
    return columns;
  }

  private class FFPAccountingPageSortableDataProvider<T extends IdObject<?>>
      extends SortableDataProvider<FFPAccountingDO, String>
  {
    private static final long serialVersionUID = 1517715512369991765L;

    /**
     * Complete list is needed every time the sort parameters or filter settings were changed.
     */
    private List<FFPAccountingDO> completeList;

    private List<FFPAccountingDO> eventAttendeeList;

    /**
     * Stores only the id's of the result set.
     */
    private List<Serializable> idList;

    private Long first, count;

    private SortParam<String> sortParam;

    private SortParam<String> secondSortParam;

    private FFPEventDO event;

    public FFPAccountingPageSortableDataProvider(final SortParam<String> sortParam, Set<FFPAccountingDO> eventAttendeeList, FFPEventDO event)
    {
      this.eventAttendeeList = new ArrayList<>(eventAttendeeList);
      this.event = event;
      // set default sort
      if (sortParam != null) {
        setSort(sortParam);
      } else {
        setSort("NOSORT", SortOrder.ASCENDING);
      }
    }

    public FFPAccountingPageSortableDataProvider<T> setCompleteList(final List<FFPAccountingDO> completeList)
    {
      this.completeList = completeList;
      this.idList = new LinkedList<Serializable>();
      if (this.completeList != null) {
        sortList(this.completeList);
        for (final FFPAccountingDO entry : completeList) {
          this.idList.add(entry.getId());
        }
      }
      return this;
    }

    @Override
    public Iterator<FFPAccountingDO> iterator(final long first, final long count)
    {
      if ((this.first != null && this.first != first) || (this.count != null && this.count != count)) {
        this.completeList = null; // Force to load all elements from data-base (avoid lazy initialization exceptions).
      }
      final SortParam<String> sp = getSort();
      if (ObjectUtils.equals(sortParam, sp) == false) {
        // The sort parameters were changed, force reload from data-base:
        reloadList();
      }
      this.first = first;
      this.count = count;
      if (idList == null) {
        return null;
      }
      List<FFPAccountingDO> result;
      int fromIndex = (int) first;
      if (fromIndex < 0) {
        fromIndex = 0;
      }
      int toIndex = (int) (first + count);
      if (this.completeList != null) {
        // The completeList is already load, don't need to load objects from data-base:
        result = completeList;
        if (toIndex > idList.size()) {
          toIndex = idList.size();
        }
        result = new LinkedList<FFPAccountingDO>();
        for (final FFPAccountingDO entry : completeList.subList(fromIndex, toIndex)) {
          result.add(entry);
        }
        this.completeList = null; // Don't store the complete list on the server anymore.
        return result.iterator();
      } else {
        if (toIndex > idList.size()) {
          toIndex = idList.size();
        }

        final List<FFPAccountingDO> list = this.eventAttendeeList;
        sortList(list);
        return list.iterator();
      }
    }

    protected Comparator<FFPAccountingDO> getComparator(final SortParam<String> sortParam,
        final SortParam<String> secondSortParam)
    {
      final String sortProperty = sortParam != null ? sortParam.getProperty() : null;
      final boolean ascending = sortParam != null ? sortParam.isAscending() : true;
      final String secondSortProperty = secondSortParam != null ? secondSortParam.getProperty() : null;
      final boolean secondAscending = secondSortParam != null ? secondSortParam.isAscending() : true;
      return new MyBeanComparator<FFPAccountingDO>(sortProperty, ascending, secondSortProperty, secondAscending);
    }

    /**
     * @see org.apache.wicket.markup.repeater.data.IDataProvider#size()
     */
    @Override
    public long size()
    {
      if (idList == null) {
        reloadList();
      }
      return this.idList != null ? this.idList.size() : 0;
    }

    private void reloadList()
    {
      setCompleteList(this.eventAttendeeList);
    }

    private void sortList(final List<FFPAccountingDO> list)
    {
      final SortParam<String> sp = getSort();
      if (sp != null && "NOSORT".equals(sp.getProperty()) == false) {
        if (this.sortParam != null && StringUtils.equals(this.sortParam.getProperty(), sp.getProperty()) == false) {
          this.secondSortParam = this.sortParam;
        }
        final Comparator<FFPAccountingDO> comp = getComparator(sp, secondSortParam);
        Collections.sort(list, comp);
      }
      this.sortParam = sp;
    }

    @Override
    public IModel<FFPAccountingDO> model(final FFPAccountingDO object)
    {
      return new Model<FFPAccountingDO>(object);
    }

    /**
     * @see ISortableDataProvider#detach()
     */
    @Override
    public void detach()
    {
      this.completeList = null;
    }

  }
}
