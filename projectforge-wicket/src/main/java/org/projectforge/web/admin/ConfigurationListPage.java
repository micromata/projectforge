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

package org.projectforge.web.admin;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.configuration.ConfigurationType;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.web.wicket.AbstractListPage;
import org.projectforge.web.wicket.CellItemListener;
import org.projectforge.web.wicket.CellItemListenerPropertyColumn;
import org.projectforge.web.wicket.ListPage;
import org.projectforge.web.wicket.ListSelectActionPanel;

@ListPage(editPage = ConfigurationEditPage.class)
public class ConfigurationListPage extends AbstractListPage<ConfigurationListForm, ConfigurationDao, ConfigurationDO>
{
  private static final long serialVersionUID = 5745110028112481137L;

  @SpringBean
  private ConfigurationDao configurationDao;

  @SpringBean
  private TeamCalCache teamCalCache;

  private transient TaskTree taskTree;

  public ConfigurationListPage(final PageParameters parameters)
  {
    super(parameters, "administration.configuration");
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    newItemMenuEntry.setVisible(false);
    configurationDao.checkAndUpdateDatabaseEntries();
    final List<IColumn<ConfigurationDO, String>> columns = new ArrayList<IColumn<ConfigurationDO, String>>();
    final CellItemListener<ConfigurationDO> cellItemListener = new CellItemListener<ConfigurationDO>()
    {
      public void populateItem(final Item<ICellPopulator<ConfigurationDO>> item, final String componentId,
          final IModel<ConfigurationDO> rowModel)
      {
        final ConfigurationDO configuration = rowModel.getObject();
        appendCssClasses(item, configuration.getId(), configuration.isDeleted());
      }
    };
    columns.add(new CellItemListenerPropertyColumn<ConfigurationDO>(
        new Model<String>(getString("administration.configuration.parameter")),
        null, null, cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<ConfigurationDO>> item, final String componentId,
          final IModel<ConfigurationDO> rowModel)
      {
        final ConfigurationDO configuration = rowModel.getObject();
        item.add(new ListSelectActionPanel(componentId, rowModel, ConfigurationEditPage.class, configuration.getId(),
            ConfigurationListPage.this, getString(configuration.getI18nKey())));
        addRowClick(item);
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<ConfigurationDO>(
        new Model<String>(getString("administration.configuration.value")),
        "value", null, cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<ConfigurationDO>> item, final String componentId,
          final IModel<ConfigurationDO> rowModel)
      {
        final ConfigurationDO configuration = rowModel.getObject();
        final String value;
        if (configuration.getValue() == null) {
          value = "";
        } else if (configuration.getConfigurationType() == ConfigurationType.TASK) {
          final TaskDO task = getTaskTree().getTaskById(configuration.getTaskId());
          if (task != null) {
            value = task.getId() + ": " + task.getTitle();
          } else {
            value = "???";
          }
        } else if (configuration.getConfigurationType() == ConfigurationType.CALENDAR) {
          final TeamCalDO calendar = teamCalCache.getCalendar(configuration.getCalendarId());
          if (calendar != null) {
            value = calendar.getTitle();
          } else {
            value = "???";
          }
        } else {
          value = String.valueOf(configuration.getValue());
        }
        item.add(new Label(componentId, value));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    columns.add(new CellItemListenerPropertyColumn<ConfigurationDO>(new Model<String>(getString("description")), null,
        null, cellItemListener)
    {
      @Override
      public void populateItem(final Item<ICellPopulator<ConfigurationDO>> item, final String componentId,
          final IModel<ConfigurationDO> rowModel)
      {
        final ConfigurationDO configuration = rowModel.getObject();
        item.add(new Label(componentId, getString(configuration.getDescriptionI18nKey())));
        cellItemListener.populateItem(item, componentId, rowModel);
      }
    });
    dataTable = createDataTable(columns, null, SortOrder.ASCENDING);
    form.add(dataTable);
  }

  private TaskTree getTaskTree()
  {
    if (taskTree == null) {
      taskTree = TaskTreeHelper.getTaskTree();
    }
    return taskTree;
  }

  @Override
  protected ConfigurationListForm newListForm(final AbstractListPage<?, ?, ?> parentPage)
  {
    return new ConfigurationListForm(this);
  }

  @Override
  public ConfigurationDao getBaseDao()
  {
    return configurationDao;
  }
}
