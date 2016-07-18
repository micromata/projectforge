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

package org.projectforge.web.task;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskHelper;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.access.AccessListPage;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.gantt.GanttChartEditPage;
import org.projectforge.web.timesheet.TimesheetEditPage;
import org.projectforge.web.timesheet.TimesheetListPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

@EditPage(defaultReturnPage = TaskTreePage.class)
public class TaskEditPage extends AbstractEditPage<TaskDO, TaskEditForm, TaskDao> implements ISelectCallerPage
{
  public static final String PARAM_PARENT_TASK_ID = "parentTaskId";

  private static final long serialVersionUID = 5176663429783524587L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TaskEditPage.class);

  @SpringBean
  private TaskDao taskDao;

  private transient TaskTree taskTree;

  @SpringBean
  private Kost2Dao kost2Dao;

  /**
   * Used by the TutorialPage.
   * 
   * @param task
   */
  public TaskEditPage(final TaskDO task)
  {
    super(new PageParameters(), "task");
    super.init(task);
    addTopMenuPanel();
  }

  public TaskEditPage(final PageParameters parameters)
  {
    super(parameters, "task");
    init();
    addTopMenuPanel();
    final Integer parentTaskId = WicketUtils.getAsInteger(parameters, PARAM_PARENT_TASK_ID);
    if (NumberHelper.greaterZero(parentTaskId) == true) {
      taskDao.setParentTask(getData(), parentTaskId);
    }
  }

  @Override
  protected TaskDao getBaseDao()
  {
    return taskDao;
  }

  @Override
  protected TaskEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final TaskDO data)
  {
    return new TaskEditForm(this, data);
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Integer)
   */
  public void select(final String property, final Object selectedValue)
  {
    if ("parentTaskId".equals(property) == true) {
      taskDao.setParentTask(getData(), (Integer) selectedValue);
    } else if ("ganttPredecessorId".equals(property) == true) {
      taskDao.setGanttPredecessor(getData(), (Integer) selectedValue);
    } else if ("responsibleUserId".equals(property) == true) {
      taskDao.setResponsibleUser(getData(), (Integer) selectedValue);
    } else if ("kost2Id".equals(property) == true) {
      final Integer kost2Id = (Integer) selectedValue;
      if (kost2Id != null) {
        final Kost2DO kost2 = kost2Dao.getById(kost2Id);
        if (kost2 != null) {
          final String newKost2String = TaskHelper.addKost2(getTaskTree(), getData(), kost2);
          getData().setKost2BlackWhiteList(newKost2String);
          form.kost2BlackWhiteTextField.modelChanged();
        }
      }
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  public void unselect(final String property)
  {
    if ("parentTaskId".equals(property) == true) {
      getData().setParentTask(null);
    } else if ("ganttPredecessorId".equals(property) == true) {
      getData().setGanttPredecessor(null);
    } else if ("responsibleUserId".equals(property) == true) {
      getData().setResponsibleUser(null);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @SuppressWarnings("serial")
  private void addTopMenuPanel()
  {
    if (isNew() == false) {
      final Integer id = form.getData().getId();
      ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new Link<Void>(ContentMenuEntryPanel.LINK_ID)
          {
            @Override
            public void onClick()
            {
              final PageParameters params = new PageParameters();
              params.set(PARAM_PARENT_TASK_ID, id);
              final TaskEditPage taskEditPage = new TaskEditPage(params);
              taskEditPage.setReturnToPage(TaskEditPage.this);
              setResponsePage(taskEditPage);
            };
          }, getString("task.menu.addSubTask"));
      addContentMenuEntry(menu);

      menu = new ContentMenuEntryPanel(getNewContentMenuChildId(), new Link<Void>(ContentMenuEntryPanel.LINK_ID)
      {
        @Override
        public void onClick()
        {
          final PageParameters params = new PageParameters();
          params.add(TimesheetEditPage.PARAMETER_KEY_TASK_ID, id);
          final TimesheetEditPage timesheetEditPage = new TimesheetEditPage(params);
          timesheetEditPage.setReturnToPage(TaskEditPage.this);
          setResponsePage(timesheetEditPage);
        };
      }, getString("task.menu.addTimesheet"));
      addContentMenuEntry(menu);

      final BookmarkablePageLink<Void> showTimesheetsLink = new BookmarkablePageLink<Void>("link",
          TimesheetListPage.class);
      showTimesheetsLink.getPageParameters().set(TimesheetListPage.PARAMETER_KEY_TASK_ID, id);
      menu = new ContentMenuEntryPanel(getNewContentMenuChildId(), showTimesheetsLink,
          getString("task.menu.showTimesheets"));
      addContentMenuEntry(menu);

      menu = new ContentMenuEntryPanel(getNewContentMenuChildId(), new Link<Void>(ContentMenuEntryPanel.LINK_ID)
      {
        @Override
        public void onClick()
        {
          final PageParameters params = new PageParameters();
          params.set(GanttChartEditPage.PARAM_KEY_TASK, id);
          final GanttChartEditPage ganttChartEditPage = new GanttChartEditPage(params);
          ganttChartEditPage.setReturnToPage(TaskEditPage.this);
          setResponsePage(ganttChartEditPage);
        };
      }, getString("gantt.title.add"));
      addContentMenuEntry(menu);

      final BookmarkablePageLink<Void> showAccessRightsLink = new BookmarkablePageLink<Void>("link",
          AccessListPage.class);
      if (form.getData().getId() != null) {
        showAccessRightsLink.getPageParameters().set(AccessListPage.PARAMETER_KEY_TASK_ID, form.getData().getId());
      }
      final ContentMenuEntryPanel extendedMenu = contentMenuBarPanel.ensureAndAddExtendetMenuEntry();
      menu = new ContentMenuEntryPanel(extendedMenu.newSubMenuChildId(), showAccessRightsLink,
          getString("task.menu.showAccessRights"));
      extendedMenu.addSubMenuEntry(menu);
    }
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
