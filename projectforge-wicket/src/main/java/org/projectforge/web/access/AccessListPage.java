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

package org.projectforge.web.access;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.formatter.WicketTaskFormatter;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.access.AccessEntryDO;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.IListPageColumnsCreator;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

@ListPage(editPage = AccessEditPage.class)
public class AccessListPage extends AbstractListPage<AccessListForm, AccessDao, GroupTaskAccessDO> implements
    IListPageColumnsCreator<GroupTaskAccessDO>
{
  /**
   * Key for pre-setting the task id.
   */
  public static final String PARAMETER_KEY_TASK_ID = "taskId";

  private static final long serialVersionUID = 7017404582337466883L;

  @SpringBean
  private AccessDao accessDao;

  public AccessListPage(final PageParameters parameters)
  {
    super(parameters, "access");
    if (WicketUtils.contains(parameters, PARAMETER_KEY_TASK_ID) == true) {
      final Integer id = WicketUtils.getAsInteger(parameters, PARAMETER_KEY_TASK_ID);
      form.getSearchFilter().setTaskId(id);
    }
  }

  @SuppressWarnings("serial")
  @Override
  public List<IColumn<GroupTaskAccessDO, String>> createColumns(final WebPage returnToPage, final boolean sortable)
  {
    final List<IColumn<GroupTaskAccessDO, String>> columns = new ArrayList<IColumn<GroupTaskAccessDO, String>>();
    final CellItemListener<GroupTaskAccessDO> cellItemListener = new CellItemListener<GroupTaskAccessDO>()
    {
      public void populateItem(final Item<ICellPopulator<GroupTaskAccessDO>> item, final String componentId,
          final IModel<GroupTaskAccessDO> rowModel)
      {
        final GroupTaskAccessDO acces = rowModel.getObject();
        appendCssClasses(item, acces.getId(), acces.isDeleted());
      }
    };
    columns.add(new CellItemListenerPropertyColumn<GroupTaskAccessDO>(new Model<String>(getString("task")),
        getSortable("task.title",
            sortable),
        "task.title", cellItemListener)
    {
      /**
       * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
       *      java.lang.String, org.apache.wicket.model.IModel)
       */
      @Override
      public void populateItem(final Item<ICellPopulator<GroupTaskAccessDO>> item, final String componentId,
          final IModel<GroupTaskAccessDO> rowModel)
      {
        final GroupTaskAccessDO access = rowModel.getObject();
        final TaskDO task = access.getTask();
        final StringBuffer buf = new StringBuffer();
        WicketTaskFormatter.appendFormattedTask(getRequestCycle(), buf, task, true, false);
        final Label formattedTaskLabel = new Label(ListSelectActionPanel.LABEL_ID, buf.toString());
        formattedTaskLabel.setEscapeModelStrings(false);
        item.add(new ListSelectActionPanel(componentId, rowModel, AccessEditPage.class, access.getId(), returnToPage,
            formattedTaskLabel));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<GroupTaskAccessDO>(new Model<String>(getString("group")),
        getSortable("group.name",
            sortable),
        "group.name", cellItemListener));
    columns.add(new CellItemListenerPropertyColumn<GroupTaskAccessDO>(new Model<String>(getString("recursive")),
        getSortable("recursive",
            sortable),
        "recursive", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<GroupTaskAccessDO>> item, final String componentId,
          final IModel<GroupTaskAccessDO> rowModel)
      {
        final GroupTaskAccessDO access = rowModel.getObject();
        if (access.isRecursive() == true) {
          item.add(new IconPanel(componentId, IconType.ACCEPT));
        } else {
          item.add(createInvisibleDummyComponent(componentId));
        }
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<GroupTaskAccessDO>(new Model<String>(getString("access.type")), null,
        "accessEntries",
        cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<GroupTaskAccessDO>> item, final String componentId,
          final IModel<GroupTaskAccessDO> rowModel)
      {
        final int rowIndex = ((Item<?>) item.findParent(Item.class)).getIndex();
        final GroupTaskAccessDO access = rowModel.getObject();
        final List<AccessEntryDO> accessEntries = access.getOrderedEntries();
        final AccessTablePanel accessTablePanel = new AccessTablePanel(componentId, accessEntries);
        if (rowIndex == 0) {
          accessTablePanel.setDrawHeader(true);
        }
        item.add(accessTablePanel);
        accessTablePanel.init();
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<GroupTaskAccessDO>(getString("description"),
        getSortable("description", sortable),
        "description", cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<GroupTaskAccessDO>> item, final String componentId,
          final IModel<GroupTaskAccessDO> rowModel)
      {
        final GroupTaskAccessDO access = rowModel.getObject();
        final Label label = new Label(componentId, StringUtils.abbreviate(access.getDescription(), 100));
        cellItemListener.populateItem(item, componentId, rowModel);
        item.add(label);
      }
    });
    return columns;
  }

  @Override
  protected void init()
  {
    dataTable = createDataTable(createColumns(this, true), "group.name", SortOrder.ASCENDING);
    form.add(dataTable);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("taskId".equals(property) == true) {
      form.getSearchFilter().setTaskId((Integer) selectedValue);
      refresh();
    } else if ("groupId".equals(property) == true) {
      form.getSearchFilter().setGroupId((Integer) selectedValue);
      form.groupSelectPanel.getTextField().modelChanged();
      refresh();
    } else if ("userId".equals(property) == true) {
      form.getSearchFilter().setUserId((Integer) selectedValue);
      refresh();
    } else {
      super.select(property, selectedValue);
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {
    if ("taskId".equals(property) == true) {
      form.getSearchFilter().setTaskId(null);
      refresh();
    } else if ("groupId".equals(property) == true) {
      form.getSearchFilter().setGroupId(null);
      form.groupSelectPanel.getTextField().modelChanged();
      refresh();
    } else if ("userId".equals(property) == true) {
      form.getSearchFilter().setUserId(null);
      refresh();
    } else {
      super.unselect(property);
    }
  }

  @Override
  protected AccessListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new AccessListForm(this);
  }

  @Override
  public AccessDao getBaseDao()
  {
    return accessDao;
  }

  protected AccessDao getAccessDao()
  {
    return accessDao;
  }
}
