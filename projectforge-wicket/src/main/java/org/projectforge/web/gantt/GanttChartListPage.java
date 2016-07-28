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

package org.projectforge.web.gantt;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.gantt.GanttChartDO;
import org.projectforge.business.gantt.GanttChartDao;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.web.task.TaskPropertyColumn;
import org.projectforge.web.user.UserPropertyColumn;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;

@ListPage(editPage = GanttChartEditPage.class)
public class GanttChartListPage extends AbstractListPage<GanttChartListForm, GanttChartDao, GanttChartDO>
{
  private static final long serialVersionUID = 671935723386728113L;

  @SpringBean
  private GanttChartDao ganttChartDao;

  @SpringBean
  private UserFormatter userFormatter;

  public GanttChartListPage(final PageParameters parameters)
  {
    super(parameters, "gantt");
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    final List<IColumn<GanttChartDO, String>> columns = new ArrayList<IColumn<GanttChartDO, String>>();
    final TaskTree taskTree = TaskTreeHelper.getTaskTree();
    final CellItemListener<GanttChartDO> cellItemListener = new CellItemListener<GanttChartDO>()
    {
      @Override
      public void populateItem(final Item<ICellPopulator<GanttChartDO>> item, final String componentId,
          final IModel<GanttChartDO> rowModel)
      {
        final GanttChartDO ganttChart = rowModel.getObject();
        appendCssClasses(item, ganttChart.getId(), ganttChart.isDeleted());
      }
    };
    columns.add(
        new CellItemListenerPropertyColumn<GanttChartDO>(new Model<String>(getString("gantt.name")), "name", "number",
            cellItemListener)
        {
          /**
           * @see org.projectforge.web.wicket.CellItemListenerPropertyColumn#populateItem(org.apache.wicket.markup.repeater.Item,
           *      java.lang.String, org.apache.wicket.model.IModel)
           */
          @Override
          public void populateItem(final Item<ICellPopulator<GanttChartDO>> item, final String componentId,
              final IModel<GanttChartDO> rowModel)
          {
            final GanttChartDO ganttChart = rowModel.getObject();
            item.add(new ListSelectActionPanel(componentId, rowModel, GanttChartEditPage.class, ganttChart.getId(),
                GanttChartListPage.this,
                ganttChart.getName()));
            cellItemListener.populateItem(item, componentId, rowModel);
            addRowClick(item);
          }
        });
    columns.add(
        new CellItemListenerPropertyColumn<GanttChartDO>(new Model<String>(getString("created")), "created", "created",
            cellItemListener));
    columns
        .add(new UserPropertyColumn<GanttChartDO>(getUserGroupCache(), getString("gantt.owner"), "user.fullname",
            "owner",
            cellItemListener)
                .withUserFormatter(userFormatter));
    columns.add(
        new TaskPropertyColumn<GanttChartDO>(getString("task"), "task.title", "task", cellItemListener)
            .withTaskTree(taskTree));
    dataTable = createDataTable(columns, "name", SortOrder.DESCENDING);
    form.add(dataTable);
  }

  @Override
  protected GanttChartListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new GanttChartListForm(this);
  }

  @Override
  protected GanttChartDao getBaseDao()
  {
    return ganttChartDao;
  }
}
