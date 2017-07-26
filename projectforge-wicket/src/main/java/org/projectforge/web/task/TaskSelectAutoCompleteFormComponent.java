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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.IConverter;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;

/**
 * @author Johannes Unterstein
 */
public abstract class TaskSelectAutoCompleteFormComponent extends PFAutoCompleteTextField<TaskDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(TaskSelectAutoCompleteFormComponent.class);

  private static final long serialVersionUID = 2278347191215880396L;

  @SpringBean
  private TaskDao taskDao;

  private transient TaskTree taskTree;

  private TaskDO taskDo;

  private boolean autocompleteOnlyTaskBookableForTimesheets;

  /**
   * @param id
   */
  public TaskSelectAutoCompleteFormComponent(final String id)
  {
    super(id, null);
    setModel(new PropertyModel<TaskDO>(this, "taskDo"));
    getSettings().withLabelValue(true).withMatchContains(true).withMinChars(2).withAutoSubmit(false);
    add(AttributeModifier.append("onkeypress", "if ( event.which == 13 ) { return false; }"));
    add(AttributeModifier.append("class", "mm_delayBlur"));
    add(new AjaxFormComponentUpdatingBehavior("change")
    {
      private static final long serialVersionUID = 3681828654557441560L;

      @Override
      protected void onUpdate(final AjaxRequestTarget target)
      {
        // just update the model
      }
    });
  }

  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    // this panel should always start with an empty input field, therefore delete the current model
    taskDo = null;
  }

  /**
   * @see org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField#getChoices(java.lang.String)
   */
  @Override
  protected List<TaskDO> getChoices(final String input)
  {
    final BaseSearchFilter filter = new BaseSearchFilter();
    filter.setSearchFields("title", "taskpath");
    filter.setSearchString(input);
    final List<TaskDO> list = taskDao.getList(filter);
    final List<TaskDO> choices = new ArrayList<TaskDO>();
    for (final TaskDO task : list) {
      if (autocompleteOnlyTaskBookableForTimesheets == false) {
        choices.add(task);
      } else {
        final TaskNode taskNode = getTaskTree().getTaskNodeById(task.getId());
        if (taskNode == null) {
          log.error("Oups, task node with id '" + task.getId() + "' not found in taskTree.");
        } else if (taskNode.isBookableForTimesheets() == true) {
          // Only add nodes which are bookable:
          choices.add(task);
        }
      }
    }
    return choices;
  }

  @Override
  protected String formatValue(final TaskDO value)
  {
    if (value == null) {
      return "";
    }
    return "" + value.getId();
  }

  @Override
  protected String formatLabel(final TaskDO value)
  {
    if (value == null) {
      return "";
    }

    return createPath(value.getId());
  }

  /**
   * create path to root
   *
   * @return
   */
  private String createPath(final Integer taskId)
  {
    final StringBuilder builder = new StringBuilder();
    final List<TaskNode> nodeList = getTaskTree().getPathToRoot(taskId);
    if (CollectionUtils.isEmpty(nodeList) == true) {
      return getString("task.path.rootTask");
    }
    final String pipeSeparator = " | ";
    String separator = "";
    for (final TaskNode node : nodeList) {
      builder.append(separator);
      builder.append(node.getTask().getTitle());
      separator = pipeSeparator;
    }
    return builder.toString();
  }

  protected void notifyChildren()
  {
    final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
    if (target != null) {
      onModelSelected(target, taskDo);
    }
  }

  /**
   * Hook method which is called when the model is changed with a valid durin an ajax call
   *
   * @param target
   * @param taskDo
   */
  protected abstract void onModelSelected(final AjaxRequestTarget target, TaskDO taskDo);

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public <C> IConverter<C> getConverter(final Class<C> type)
  {
    return new IConverter()
    {
      private static final long serialVersionUID = -7729322118285105516L;

      @Override
      public Object convertToObject(final String value, final Locale locale)
      {
        if (StringUtils.isEmpty(value) == true) {
          getModel().setObject(null);
          notifyChildren();
          return null;
        }
        try {
          final TaskDO task = getTaskTree().getTaskById(Integer.valueOf(value));
          if (task == null) {
            error(getString("timesheet.error.invalidTaskId"));
            return null;
          }
          getModel().setObject(task);
          notifyChildren();
          return task;
        } catch (final NumberFormatException e) {
          // just ignore the NumberFormatException, because this could happen during wrong inputs
          return null;
        }
      }

      @Override
      public String convertToString(final Object value, final Locale locale)
      {
        if (value == null) {
          return "";
        }
        final TaskDO task = (TaskDO) value;
        return task.getTitle();
      }
    };
  }

  /**
   * @param autocompleteOnlyTaskBookableForTimesheets the autocompleteOnlyTaskBookableForTimesheets to set
   * @return this for chaining.
   */
  void setAutocompleteOnlyTaskBookableForTimesheets(final boolean autocompleteOnlyTaskBookableForTimesheets)
  {
    this.autocompleteOnlyTaskBookableForTimesheets = autocompleteOnlyTaskBookableForTimesheets;
  }

  private TaskTree getTaskTree()
  {
    if (taskTree == null) {
      taskTree = TaskTreeHelper.getTaskTree();
    }
    return taskTree;
  }
}
