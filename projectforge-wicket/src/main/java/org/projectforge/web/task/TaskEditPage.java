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

package org.projectforge.web.task;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskHelper;
import org.projectforge.business.task.TaskTree;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.access.AccessListPage;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.gantt.GanttChartEditPage;
import org.projectforge.web.timesheet.TimesheetEditPage;
import org.projectforge.web.timesheet.TimesheetListPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.slf4j.Logger;

@EditPage(defaultReturnPage = TaskTreePage.class)
public class TaskEditPage extends AbstractEditPage<TaskDO, TaskEditForm, TaskDao> implements ISelectCallerPage
{
  public static final String PARAM_PARENT_TASK_ID = "parentTaskId";

  private static final long serialVersionUID = 5176663429783524587L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskEditPage.class);

  public TaskEditPage(final PageParameters parameters)
  {
    super(parameters, "task");
    init();
    addTopMenuPanel();
    final Long parentTaskId = WicketUtils.getAsLong(parameters, PARAM_PARENT_TASK_ID);
    if (NumberHelper.greaterZero(parentTaskId) == true) {
      getBaseDao().setParentTask(getData(), parentTaskId);
    }
  }

  @Override
  protected TaskDao getBaseDao()
  {
    return WicketSupport.getTaskDao();
  }

  @Override
  protected TaskEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final TaskDO data)
  {
    return new TaskEditForm(this, data);
  }

  public void select(final String property, final Object selectedValue)
  {
    if ("parentTaskId".equals(property) == true) {
      getBaseDao().setParentTask(getData(), (Long) selectedValue);
    } else if ("ganttPredecessorId".equals(property) == true) {
      getBaseDao().setGanttPredecessor(getData(), (Long) selectedValue);
    } else if ("responsibleUserId".equals(property) == true) {
      getBaseDao().setResponsibleUser(getData(), (Long) selectedValue);
    } else if ("kost2Id".equals(property) == true) {
      final Long kost2Id = (Long) selectedValue;
      if (kost2Id != null) {
        final Kost2DO kost2 = WicketSupport.get(Kost2Dao.class).getById(kost2Id);
        if (kost2 != null) {
          final String newKost2String = TaskHelper.addKost2(TaskTree.getInstance(), getData(), kost2);
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
      final Long id = form.getData().getId();
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

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
