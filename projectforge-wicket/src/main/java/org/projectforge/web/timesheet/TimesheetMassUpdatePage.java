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

package org.projectforge.web.timesheet;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.user.UserFormatter;
import org.projectforge.business.utils.HtmlDateTimeFormatter;
import org.projectforge.framework.utils.MyBeanComparator;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractMassEditPage;
import org.projectforge.web.wicket.AbstractSecuredPage;

public class TimesheetMassUpdatePage extends AbstractMassEditPage implements ISelectCallerPage
{
  private static final long serialVersionUID = -5549904132530779884L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TimesheetMassUpdatePage.class);

  @SpringBean
  private HtmlDateTimeFormatter dateTimeFormatter;

  @SpringBean
  private UserFormatter userFormatter;

  @SpringBean
  private TimesheetDao timesheetDao;

  private List<TimesheetDO> timesheets;

  private final TimesheetMassUpdateForm form;

  public TimesheetMassUpdatePage(final AbstractSecuredPage callerPage, final List<TimesheetDO> timesheets)
  {
    super(new PageParameters(), callerPage);
    final TaskTree taskTree = TaskTreeHelper.getTaskTree();
    this.timesheets = timesheets;
    form = new TimesheetMassUpdateForm(this);
    Integer taskId = null;
    for (final TimesheetDO sheet : timesheets) {
      if (taskId == null) {
        taskId = sheet.getTaskId();
      } else if (taskId.equals(sheet.getTaskId()) == false) {
        taskId = null;
        break;
      }
    }
    if (taskId != null) {
      // All time sheets have the same task, so pre-select this task.
      timesheetDao.setTask(form.data, taskId);
    }
    body.add(form);
    form.init();
    final List<IColumn<TimesheetDO, String>> columns = TimesheetListPage.createColumns(getUserGroupCache(), this,
        false, true,
        null,
        taskTree,
        userFormatter, dateTimeFormatter);
    @SuppressWarnings("serial")
    final SortableDataProvider<TimesheetDO, String> sortableDataProvider = new SortableDataProvider<TimesheetDO, String>()
    {
      @Override
      public Iterator<TimesheetDO> iterator(final long first, final long count)
      {
        final SortParam sp = getSort();
        final Comparator<TimesheetDO> comp = new MyBeanComparator<TimesheetDO>(sp.getProperty().toString(),
            sp.isAscending());
        Collections.sort(timesheets, comp);
        return timesheets.subList((int) first, (int) (first + count)).iterator();
      }

      @Override
      public long size()
      {
        return timesheets != null ? timesheets.size() : 0;
      }

      @Override
      public IModel<TimesheetDO> model(final TimesheetDO object)
      {
        return new Model<TimesheetDO>()
        {
          @Override
          public TimesheetDO getObject()
          {
            return object;
          }
        };
      }
    };
    sortableDataProvider.setSort("startTime", SortOrder.DESCENDING);

    final DefaultDataTable<TimesheetDO, String> dataTable = new DefaultDataTable<TimesheetDO, String>("table", columns,
        sortableDataProvider, 1000);
    body.add(dataTable);
  }

  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing.
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("taskId".equals(property) == true) {
      timesheetDao.setTask(form.data, (Integer) selectedValue);
      form.refresh();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    if ("taskId".equals(property) == true) {
      form.data.setTask(null);
      form.refresh();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  protected String getTitle()
  {
    return getString("timesheet.massupdate.title");
  }

  /**
   * @see org.projectforge.web.wicket.AbstractMassEditPage#updateAll()
   */
  @Override
  protected void updateAll()
  {
    if (form.updateTask == false) {
      form.data.setTask(null);
    }
    if (form.updateKost2 == false) {
      form.data.setKost2(null);
    }
    timesheetDao.massUpdate(timesheets, form.data);
    super.updateAll();
  }
}
