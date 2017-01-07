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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.framework.persistence.api.IdObject;
import org.projectforge.framework.utils.MyBeanComparator;
import org.projectforge.plugins.ffp.model.FFPDebtDO;
import org.projectforge.plugins.ffp.repository.FFPEventService;
import org.projectforge.web.wicket.AbstractViewPage;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.flowlayout.ButtonPanel;
import org.projectforge.web.wicket.flowlayout.ButtonType;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.TablePanel;

public class FFPDeptViewPage extends AbstractViewPage
{
  private static final long serialVersionUID = 6317381238012316284L;

  private static final Logger log = Logger.getLogger(FFPDeptViewPage.class);

  @SpringBean
  private FFPEventService eventService;

  public FFPDeptViewPage(final PageParameters parameters)
  {
    super(parameters);
  }

  @Override
  protected void onInitialize()
  {
    super.onInitialize();
  }

  @Override
  protected void onConfigure()
  {
    super.onConfigure();
    createDataTable(gridBuilder);
  }

  @Override
  protected String getTitle()
  {
    return getString("plugins.ffp.dept.title");
  }

  public void createDataTable(GridBuilder gridBuilder)
  {
    DivPanel section = gridBuilder.getPanel();
    TablePanel tablePanel = new TablePanel(section.newChildId());
    tablePanel.setOutputMarkupId(true);
    section.add(tablePanel);
    DataTable<FFPDebtDO, String> dataTable = createDataTable(createColumns(), "attendee.user.fullname", SortOrder.ASCENDING);
    tablePanel.add(dataTable);
  }

  private DataTable<FFPDebtDO, String> createDataTable(final List<IColumn<FFPDebtDO, String>> columns, final String sortProperty, final SortOrder sortOrder)
  {
    final SortParam<String> sortParam = sortProperty != null
        ? new SortParam<String>(sortProperty, sortOrder == SortOrder.ASCENDING) : null;
    DefaultDataTable<FFPDebtDO, String> localDataTable = new DefaultDataTable<>(TablePanel.TABLE_ID, columns,
        createSortableDataProvider(sortParam), 50);
    localDataTable.setOutputMarkupId(true);
    localDataTable.setMarkupId("attendeeDataTable");
    return localDataTable;
  }

  private ISortableDataProvider<FFPDebtDO, String> createSortableDataProvider(final SortParam<String> sortParam)
  {
    return new FFPDebtPageSortableDataProvider<FFPDebtDO>(sortParam, eventService);
  }

  private List<IColumn<FFPDebtDO, String>> createColumns()
  {
    final List<IColumn<FFPDebtDO, String>> columns = new ArrayList<>();
    columns.add(new PropertyColumn<FFPDebtDO, String>(new ResourceModel("plugins.ffp.eventDate"), "event.eventDate"));
    columns.add(new PropertyColumn<FFPDebtDO, String>(new ResourceModel("plugins.ffp.title"), "event.title"));
    columns.add(new PropertyColumn<FFPDebtDO, String>(new ResourceModel("plugins.ffp.from"), "from.user.fullname"));
    columns.add(new PropertyColumn<FFPDebtDO, String>(new ResourceModel("plugins.ffp.to"), "to.user.fullname"));
    columns.add(new CellItemListenerPropertyColumn<FFPDebtDO>(FFPDebtDO.class, "plugins.ffp.value", "value", null));

    columns.add(new CellItemListenerPropertyColumn<FFPDebtDO>(FFPDebtDO.class, "plugins.ffp.approvedByFrom", "approvedByFrom", null)
    {
      private static final long serialVersionUID = 3672950740712610620L;

      @Override
      public void populateItem(Item<ICellPopulator<FFPDebtDO>> item, String componentId,
          IModel<FFPDebtDO> rowModel)
      {
        FFPDebtDO debt = rowModel.getObject();
        Button button = new Button(ButtonPanel.BUTTON_ID);
        if (debt.getFrom().equals(currentEmployee)) {
          button.setOutputMarkupId(true);
          button.add(new AjaxEventBehavior("click")
          {
            @Override
            protected void onEvent(AjaxRequestTarget target)
            {
              if (debt.isApprovedByFrom() == false) {
                eventService.updateDebtFrom(debt);
                button.add(AttributeModifier.append("class", ButtonType.GREEN.getClassAttrValue()));
                button.addOrReplace(new Label("title", I18nHelper.getLocalizedMessage("plugins.ffp.approved")));
                target.add(button);
              }
            }
          });
        }
        String label = debt.isApprovedByFrom() ?
            I18nHelper.getLocalizedMessage("plugins.ffp.approved") :
            I18nHelper.getLocalizedMessage("plugins.ffp.notApproved");
        ButtonType bt = debt.isApprovedByFrom() ? ButtonType.GREEN : ButtonType.RED;
        ButtonPanel buttonPanel = new ButtonPanel(componentId, label, button, bt);
        item.add(buttonPanel);
      }

    });
    columns.add(new CellItemListenerPropertyColumn<FFPDebtDO>(FFPDebtDO.class, I18nHelper.getLocalizedMessage("plugins.ffp.approvedByTo"), "approvedByTo", null)
    {
      private static final long serialVersionUID = 367295074123610620L;

      @Override
      public void populateItem(Item<ICellPopulator<FFPDebtDO>> item, String componentId,
          IModel<FFPDebtDO> rowModel)
      {
        FFPDebtDO debt = rowModel.getObject();
        Button button = new Button(ButtonPanel.BUTTON_ID);
        if (debt.isApprovedByFrom() && debt.getTo().equals(currentEmployee)) {
          button.setOutputMarkupId(true);
          button.add(new AjaxEventBehavior("click")
          {
            @Override
            protected void onEvent(AjaxRequestTarget target)
            {
              if (debt.isApprovedByTo() == false) {
                eventService.updateDebtTo(debt);
                button.add(AttributeModifier.append("class", ButtonType.GREEN.getClassAttrValue()));
                button.addOrReplace(new Label("title", I18nHelper.getLocalizedMessage("plugins.ffp.approved")));
                target.add(button);
              }
            }
          });
        }
        String label = debt.isApprovedByTo() ?
            I18nHelper.getLocalizedMessage("plugins.ffp.approved") :
            I18nHelper.getLocalizedMessage("plugins.ffp.notApproved");
        ButtonType bt = debt.isApprovedByTo() ? ButtonType.GREEN : ButtonType.RED;
        ButtonPanel buttonPanel = new ButtonPanel(componentId, label, button, bt);
        item.add(buttonPanel);
      }

    });
    return columns;
  }

  private class FFPDebtPageSortableDataProvider<T extends IdObject<?>>
      extends SortableDataProvider<FFPDebtDO, String>
  {
    private static final long serialVersionUID = 1517715512369991765L;

    private FFPEventService eventService;

    /**
     * Complete list is needed every time the sort parameters or filter settings were changed.
     */
    private List<FFPDebtDO> completeList;

    /**
     * Stores only the id's of the result set.
     */
    private List<Serializable> idList;

    private Long first, count;

    private SortParam<String> sortParam;

    private SortParam<String> secondSortParam;

    private FFPDebtDO debt;

    public FFPDebtPageSortableDataProvider(final SortParam<String> sortParam, FFPEventService eventService)
    {
      this.eventService = eventService;
      // set default sort
      if (sortParam != null) {
        setSort(sortParam);
      } else {
        setSort("NOSORT", SortOrder.ASCENDING);
      }
    }

    public FFPDebtPageSortableDataProvider<T> setCompleteList(final List<FFPDebtDO> completeList)
    {
      this.completeList = completeList;
      this.idList = new LinkedList<Serializable>();
      if (this.completeList != null) {
        sortList(this.completeList);
        for (final FFPDebtDO entry : completeList) {
          this.idList.add(entry.getId());
        }
      }
      return this;
    }

    @Override
    public Iterator<FFPDebtDO> iterator(final long first, final long count)
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
      List<FFPDebtDO> result;
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
        result = new LinkedList<FFPDebtDO>();
        for (final FFPDebtDO entry : completeList.subList(fromIndex, toIndex)) {
          result.add(entry);
        }
        this.completeList = null; // Don't store the complete list on the server anymore.
        return result.iterator();
      } else {
        if (toIndex > idList.size()) {
          toIndex = idList.size();
        }

        final List<FFPDebtDO> list = this.eventService.getDeptList(currentEmployee);
        sortList(list);
        return list.iterator();
      }
    }

    protected Comparator<FFPDebtDO> getComparator(final SortParam<String> sortParam,
        final SortParam<String> secondSortParam)
    {
      final String sortProperty = sortParam != null ? sortParam.getProperty() : null;
      final boolean ascending = sortParam != null ? sortParam.isAscending() : true;
      final String secondSortProperty = secondSortParam != null ? secondSortParam.getProperty() : null;
      final boolean secondAscending = secondSortParam != null ? secondSortParam.isAscending() : true;
      return new MyBeanComparator<FFPDebtDO>(sortProperty, ascending, secondSortProperty, secondAscending);
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
      final List<FFPDebtDO> list = eventService.getDeptList(currentEmployee);
      setCompleteList(list);
    }

    private void sortList(final List<FFPDebtDO> list)
    {
      final SortParam<String> sp = getSort();
      if (sp != null && "NOSORT".equals(sp.getProperty()) == false) {
        if (this.sortParam != null && StringUtils.equals(this.sortParam.getProperty(), sp.getProperty()) == false) {
          this.secondSortParam = this.sortParam;
        }
        final Comparator<FFPDebtDO> comp = getComparator(sp, secondSortParam);
        Collections.sort(list, comp);
      }
      this.sortParam = sp;
    }

    @Override
    public IModel<FFPDebtDO> model(final FFPDebtDO object)
    {
      return new Model<FFPDebtDO>(object);
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
