/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.todo;

import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.task.TaskSelectPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;

public class ToDoListForm extends AbstractListForm<ToDoFilter, ToDoListPage>
{
  private static final long serialVersionUID = -8310609149068611648L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ToDoListForm.class);

  private transient TaskTree taskTree;

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL66);
    {
      // Assignee
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.todo.assignee"));
      final UserSelectPanel assigneeSelectPanel = new UserSelectPanel(fs.newChildId(), new Model<PFUserDO>()
      {
        @Override
        public PFUserDO getObject()
        {
          return getTenantRegistry().getUserGroupCache().getUser(getSearchFilter().getAssigneeId());
        }

        @Override
        public void setObject(final PFUserDO object)
        {
          if (object == null) {
            getSearchFilter().setAssigneeId(null);
          } else {
            getSearchFilter().setAssigneeId(object.getId());
          }
        }
      }, parentPage, "assigneeId");
      fs.add(assigneeSelectPanel);
      assigneeSelectPanel.setDefaultFormProcessing(false);
      assigneeSelectPanel.init().withAutoSubmit(true);
    }
    gridBuilder.newSplitPanel(GridSize.COL33);
    {
      // Reporter
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.todo.reporter"));
      final UserSelectPanel reporterSelectPanel = new UserSelectPanel(fs.newChildId(), new Model<PFUserDO>()
      {

        @Override
        public PFUserDO getObject()
        {
          return getTenantRegistry().getUserGroupCache().getUser(getSearchFilter().getReporterId());
        }

        @Override
        public void setObject(final PFUserDO object)
        {
          if (object == null) {
            getSearchFilter().setReporterId(null);
          } else {
            getSearchFilter().setReporterId(object.getId());
          }
        }
      }, parentPage, "reporterId");
      fs.add(reporterSelectPanel);
      reporterSelectPanel.setDefaultFormProcessing(false);
      reporterSelectPanel.init().withAutoSubmit(true);
    }
    {
      gridBuilder.newSplitPanel(GridSize.COL100);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task")).suppressLabelForWarning();
      final TaskSelectPanel taskSelectPanel = new TaskSelectPanel(fs, new Model<TaskDO>()
      {
        @Override
        public TaskDO getObject()
        {
          return getTaskTree().getTaskById(getSearchFilter().getTaskId());
        }

        @Override
        public void setObject(final TaskDO task)
        {
          if (task != null) {
            getSearchFilter().setTaskId(task.getId());
          } else {
            getSearchFilter().setTaskId(null);
          }
        }
      }, parentPage, "taskId");
      fs.add(taskSelectPanel);
      taskSelectPanel.init();
      taskSelectPanel.setRequired(false);
    }
  }

  public ToDoListForm(final ToDoListPage parentPage)
  {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   * org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    optionsCheckBoxesPanel
        .add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<>(
            getSearchFilter(), "opened"), getString(ToDoStatus.OPENED.getI18nKey())));
    optionsCheckBoxesPanel
        .add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<>(
            getSearchFilter(), "reopened"), getString(ToDoStatus.RE_OPENED.getI18nKey())));
    optionsCheckBoxesPanel
        .add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<>(
            getSearchFilter(), "inprogress"), getString(ToDoStatus.IN_PROGRESS.getI18nKey())));
    optionsCheckBoxesPanel
        .add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<>(
            getSearchFilter(), "closed"), getString(ToDoStatus.CLOSED.getI18nKey())));
    optionsCheckBoxesPanel
        .add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<>(
            getSearchFilter(), "postponed"), getString(ToDoStatus.POSTPONED.getI18nKey())));

    optionsCheckBoxesPanel.add(createCheckBoxButton(
        optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<>(getSearchFilter(), "onlyRecent"),
        getString("plugins.todo.status.onlyRecent"),
        getString("plugins.todo.status.onlyRecent.tooltip"),
        true
    ));
  }

  @Override
  protected ToDoFilter newSearchFilterInstance()
  {
    return new ToDoFilter();
  }

  private TaskTree getTaskTree()
  {
    if (taskTree == null) {
      taskTree = TaskTreeHelper.getTaskTree();
    }
    return taskTree;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
