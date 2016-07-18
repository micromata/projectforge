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

import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.CSSColor;
import org.projectforge.web.calendar.QuickSelectPanel;
import org.projectforge.web.task.TaskSelectPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.HtmlCommentPanel;
import org.projectforge.web.wicket.flowlayout.IconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

public class TimesheetListForm extends AbstractListForm<TimesheetListFilter, TimesheetListPage>
{
  private static final long serialVersionUID = 3167681159669386691L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TimesheetListForm.class);

  private transient TaskTree taskTree;

  @SpringBean
  private DateTimeFormatter dateTimeFormatter;

  protected DatePanel startDate;

  protected DatePanel stopDate;

  private String exportFormat;

  // Components for form validation.
  private final FormComponent<?>[] dependentFormComponents = new FormComponent<?>[2];

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    add(new IFormValidator()
    {
      @Override
      public FormComponent<?>[] getDependentFormComponents()
      {
        return dependentFormComponents;
      }

      @Override
      public void validate(final Form<?> form)
      {
        if (parentPage.isMassUpdateMode() == true) {

        } else {
          final TimesheetFilter filter = getSearchFilter();
          final Date from = startDate.getConvertedInput();
          final Date to = stopDate.getConvertedInput();
          if (from == null && to == null && filter.getTaskId() == null) {
            error(getString("timesheet.error.filter.needMore"));
          } else if (from != null && to != null && from.after(to) == true) {
            error(getString("timesheet.error.startTimeAfterStopTime"));
          }
        }
      }
    });
    final TimesheetFilter filter = getSearchFilter();
    {
      gridBuilder.newSplitPanel(GridSize.COL66);
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
    {
      // Assignee
      gridBuilder.newSplitPanel(GridSize.COL33);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("user"));
      final UserSelectPanel assigneeSelectPanel = new UserSelectPanel(fs.newChildId(), new Model<PFUserDO>()
      {
        @Override
        public PFUserDO getObject()
        {
          return getTenantRegistry().getUserGroupCache().getUser(filter.getUserId());
        }

        @Override
        public void setObject(final PFUserDO object)
        {
          if (object == null) {
            filter.setUserId(null);
          } else {
            filter.setUserId(object.getId());
          }
        }
      }, parentPage, "userId");
      fs.add(assigneeSelectPanel);
      assigneeSelectPanel.setDefaultFormProcessing(false);
      assigneeSelectPanel.init().withAutoSubmit(true);
    }
    {
      gridBuilder.newSplitPanel(GridSize.COL66);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("timePeriod"));
      startDate = new DatePanel(fs.newChildId(), new PropertyModel<Date>(filter, "startTime"), DatePanelSettings.get()
          .withSelectPeriodMode(true));
      fs.add(dependentFormComponents[0] = startDate);
      fs.setLabelFor(startDate);
      fs.add(new DivTextPanel(fs.newChildId(), " - "));
      stopDate = new DatePanel(fs.newChildId(), new PropertyModel<Date>(filter, "stopTime"),
          DatePanelSettings.get().withSelectPeriodMode(
              true));
      fs.add(dependentFormComponents[1] = stopDate);
      {
        final SubmitLink unselectPeriodLink = new SubmitLink(IconLinkPanel.LINK_ID)
        {
          @Override
          public void onSubmit()
          {
            getSearchFilter().setStartTime(null);
            getSearchFilter().setStopTime(null);
            clearInput();
            parentPage.refresh();
          };
        };
        unselectPeriodLink.setDefaultFormProcessing(false);
        fs.add(new IconLinkPanel(fs.newChildId(), IconType.REMOVE_SIGN,
            new ResourceModel("calendar.tooltip.unselectPeriod"),
            unselectPeriodLink).setColor(CSSColor.RED));
      }
      final QuickSelectPanel quickSelectPanel = new QuickSelectPanel(fs.newChildId(), parentPage, "quickSelect",
          startDate);
      fs.add(quickSelectPanel);
      quickSelectPanel.init();
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return WicketUtils.getCalendarWeeks(TimesheetListForm.this, filter.getStartTime(), filter.getStopTime());
        }
      }));
      fs.add(new HtmlCommentPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          return WicketUtils.getUTCDates(filter.getStartTime(), filter.getStopTime());
        }
      }));
    }
    {
      // Duration
      gridBuilder.newSplitPanel(GridSize.COL33);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("timesheet.totalDuration")).suppressLabelForWarning();
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>()
      {
        @Override
        public String getObject()
        {
          long duration = 0;
          if (parentPage.getList() != null) {
            for (final TimesheetDO sheet : parentPage.getList()) {
              duration += sheet.getDuration();
            }
          }
          return dateTimeFormatter.getPrettyFormattedDuration(duration);
        }
      }));
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel,
   *      org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    optionsCheckBoxesPanel
        .add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(
            getSearchFilter(), "longFormat"), getString("longFormat")));
    optionsCheckBoxesPanel
        .add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(
            getSearchFilter(), "recursive"), getString("task.recursive")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<Boolean>(getSearchFilter(), "marked"), getString("timesheet.filter.withTimeperiodCollision"),
        getString("timesheet.filter.withTimeperiodCollision.tooltip")).setWarning());
  }

  /**
   * @return the exportFormat
   */
  public String getExportFormat()
  {

    if (exportFormat == null) {
      exportFormat = (String) parentPage.getUserPrefEntry(this.getClass().getName() + ":exportFormat");
    }
    if (exportFormat == null) {
      exportFormat = "Micromata";
    }

    return exportFormat;
  }

  /**
   * @param exportFormat the exportFormat to set
   */
  public void setExportFormat(final String exportFormat)
  {
    this.exportFormat = exportFormat;
    parentPage.putUserPrefEntry(this.getClass().getName() + ":exportFormat", this.exportFormat, true);
  }

  public TimesheetListForm(final TimesheetListPage parentPage)
  {
    super(parentPage);
  }

  @Override
  protected TimesheetListFilter newSearchFilterInstance()
  {
    return new TimesheetListFilter();
  }

  @Override
  protected boolean isFilterVisible()
  {
    return parentPage.isMassUpdateMode() == false;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  private TaskTree getTaskTree()
  {
    if (taskTree == null) {
      taskTree = TaskTreeHelper.getTaskTree();
    }
    return taskTree;
  }
}
