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
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.web.task.TaskSelectPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.LambdaModel;
import org.projectforge.web.wicket.TimePeriodPanel;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanel;
import org.projectforge.web.wicket.flowlayout.CheckBoxButton;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

public class TimesheetListForm extends AbstractListForm<TimesheetListFilter, TimesheetListPage>
{
  private static final long serialVersionUID = 3167681159669386691L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TimesheetListForm.class);

  private transient TaskTree taskTree;

  @SpringBean
  private DateTimeFormatter dateTimeFormatter;

  DatePanel startDate;

  DatePanel stopDate;

  private String exportFormat;

  // Components for form validation.
  private final FormComponent<?>[] dependentFormComponents = new FormComponent<?>[2];

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init(false);

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
        if (parentPage.isMassUpdateMode() == false) {
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

    // Task
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

    // Assignee
    final TimesheetFilter filter = getSearchFilter();
    {
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
      assigneeSelectPanel.init();
    }
    {
      gridBuilder.newSplitPanel(GridSize.COL66);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("timePeriod"));
      final TimePeriodPanel timePeriodPanel = new TimePeriodPanel(
          fs.newChildId(),
          LambdaModel.of(filter::getStartTime, filter::setStartTime),
          LambdaModel.of(filter::getStopTime, filter::setStopTime),
          parentPage
      );
      fs.add(timePeriodPanel);
      dependentFormComponents[0] = startDate = timePeriodPanel.getStartDatePanel();
      dependentFormComponents[1] = stopDate = timePeriodPanel.getEndDatePanel();
      fs.setLabelFor(startDate);
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
   * org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel)
  {
    optionsCheckBoxesPanel.add(new CheckBoxButton(
        optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<>(getSearchFilter(), "longFormat"),
        getString("longFormat")
    ));

    optionsCheckBoxesPanel.add(new CheckBoxButton(
        optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<>(getSearchFilter(), "recursive"),
        getString("task.recursive")
    ));

    final CheckBoxButton markedButton = new CheckBoxButton(
        optionsCheckBoxesPanel.newChildId(),
        new PropertyModel<>(getSearchFilter(), "marked"),
        getString("timesheet.filter.withTimeperiodCollision")
    );
    markedButton.setWarning();
    markedButton.setTooltip(getString("timesheet.filter.withTimeperiodCollision.tooltip"));
    optionsCheckBoxesPanel.add(markedButton);
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
