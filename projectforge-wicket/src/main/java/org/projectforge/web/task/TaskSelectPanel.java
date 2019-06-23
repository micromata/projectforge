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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.hibernate.Hibernate;
import org.projectforge.business.task.*;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.web.CSSColor;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.FavoritesChoicePanel;
import org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * Panel for showing and selecting one task.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TaskSelectPanel extends AbstractSelectPanel<TaskDO> implements ComponentWrapperPanel
{
  private static final long serialVersionUID = -7231190025292695850L;

  @SpringBean
  private TaskDao taskDao;

  private transient TaskTree taskTree;

  private boolean showPath = true;

  private final WebMarkupContainer divContainer;

  private RepeatingView ancestorRepeater;

  private Integer currentTaskId;

  private boolean ajaxTaskSelectMode;

  private WebMarkupContainer userselectContainer;

  private FieldsetPanel fieldsetPanel;

  private boolean autocompleteOnlyTaskBookableForTimesheets;

  public TaskSelectPanel(final FieldsetPanel fieldsetPanel, final IModel<TaskDO> model, final ISelectCallerPage caller,
      final String selectProperty)
  {
    super(fieldsetPanel.newChildId(), model, caller, selectProperty);
    this.fieldsetPanel = fieldsetPanel;
    fieldsetPanel.getFieldset().setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true);
    TaskDO task = model.getObject();
    if (Hibernate.isInitialized(task) == false) {
      task = getTaskTree().getTaskById(task.getId());
      model.setObject(task);
    }
    divContainer = new WebMarkupContainer("div")
    {
      private static final long serialVersionUID = -8150112323444983335L;

      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        // display only, if we are not in ajax task select mode
        return ajaxTaskSelectMode == false;
      }
    };
    divContainer.setOutputMarkupId(true);
    divContainer.setOutputMarkupPlaceholderTag(true);
    add(divContainer);
    ajaxTaskSelectMode = false;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractSelectPanel#onBeforeRender()
   */
  @SuppressWarnings("serial")
  @Override
  protected void onBeforeRender()
  {
    super.onBeforeRender();
    final TaskDO task = getModelObject();
    final Integer taskId = task != null ? task.getId() : null;
    if (Objects.equals(currentTaskId, taskId)) {
      return;
    }
    currentTaskId = taskId;
    if (showPath == true && task != null) {
      ancestorRepeater.removeAll();
      final TaskNode taskNode = getTaskTree().getTaskNodeById(task.getId());
      final List<Integer> ancestorIds = taskNode.getAncestorIds();
      final ListIterator<Integer> it = ancestorIds.listIterator(ancestorIds.size());
      while (it.hasPrevious() == true) {
        final Integer ancestorId = it.previous();
        final TaskDO ancestorTask = getTaskTree().getTaskById(ancestorId);
        if (ancestorTask.getParentTask() == null) {
          // Don't show root node:
          continue;
        }
        final WebMarkupContainer cont = new WebMarkupContainer(ancestorRepeater.newChildId());
        ancestorRepeater.add(cont);
        final SubmitLink selectTaskLink = new SubmitLink("ancestorTaskLink")
        {
          @Override
          public void onSubmit()
          {
            caller.select(selectProperty, ancestorTask.getId());
          }
        };
        selectTaskLink.setDefaultFormProcessing(false);
        cont.add(selectTaskLink);
        WicketUtils.addTooltip(selectTaskLink, getString("task.selectPanel.selectAncestorTask.tooltip"));
        selectTaskLink.add(new Label("name", ancestorTask.getTitle()));
      }
      ancestorRepeater.setVisible(true);
    } else {
      ancestorRepeater.setVisible(false);
    }
  }

  @Override
  @SuppressWarnings("serial")
  public TaskSelectPanel init()
  {
    super.init();
    ancestorRepeater = new RepeatingView("ancestorTasks");
    divContainer.add(ancestorRepeater);
    final SubmitLink taskLink = new SubmitLink("taskLink")
    {
      @Override
      public void onSubmit()
      {
        final TaskDO task = getModelObject();
        if (task == null) {
          return;
        }
        final PageParameters pageParams = new PageParameters();
        pageParams.add(AbstractEditPage.PARAMETER_KEY_ID, String.valueOf(task.getId()));
        final TaskEditPage editPage = new TaskEditPage(pageParams);
        editPage.setReturnToPage((AbstractSecuredPage) getPage());
        setResponsePage(editPage);
      }
    };
    taskLink.setDefaultFormProcessing(false);
    divContainer.add(taskLink);
    // auto complete panels
    initAutoCompletePanels();

    WicketUtils.addTooltip(taskLink, getString("task.selectPanel.displayTask.tooltip"));
    taskLink.add(new Label("name", new Model<String>()
    {
      /**
       * @see org.apache.wicket.model.Model#getObject()
       */
      @Override
      public String getObject()
      {
        final TaskDO task = getModelObject();
        return task != null ? task.getTitle() : "";
      }
    }));

    final SubmitLink selectButton = new SubmitLink("select")
    {
      @Override
      public void onSubmit()
      {
        final TaskTreePage taskTreePage = new TaskTreePage(caller, selectProperty);
        if (getModelObject() != null) {
          taskTreePage.setHighlightedRowId(getModelObject().getId()); // Preselect node for highlighting.
        }
        setResponsePage(taskTreePage);
      }
    };
    selectButton.setDefaultFormProcessing(false);
    divContainer.add(selectButton);
    selectButton.add(new IconPanel("selectHelp", IconType.TASK, getString("tooltip.selectTask")));

    final SubmitLink unselectButton = new SubmitLink("unselect")
    {
      @Override
      public void onSubmit()
      {
        caller.unselect(selectProperty);
      }

      @Override
      public boolean isVisible()
      {
        return isRequired() == false && getModelObject() != null;
      }
    };
    unselectButton.setDefaultFormProcessing(false);
    divContainer.add(unselectButton);
    unselectButton.add(new IconPanel("unselectHelp", IconType.REMOVE_SIGN, getString("tooltip.unselectTask")).setColor(CSSColor.RED));

    // DropDownChoice favorites
    final FavoritesChoicePanel<TaskDO, LegacyTaskFavorite> favoritesPanel = new FavoritesChoicePanel<TaskDO, LegacyTaskFavorite>(
        "favorites",
        UserPrefArea.TASK_FAVORITE, tabIndex, "full text")
    {
      @Override
      protected void select(final LegacyTaskFavorite favorite)
      {
        if (favorite.getTask() != null) {
          TaskSelectPanel.this.selectTask(favorite.getTask());
        }
      }

      @Override
      protected TaskDO getCurrentObject()
      {
        return TaskSelectPanel.this.getModelObject();
      }

      @Override
      protected LegacyTaskFavorite newFavoriteInstance(final TaskDO currentObject)
      {
        final LegacyTaskFavorite favorite = new LegacyTaskFavorite();
        favorite.setTask(currentObject);
        return favorite;
      }
    };
    divContainer.add(favoritesPanel);
    favoritesPanel.init();
    if (showFavorites == false) {
      favoritesPanel.setVisible(false);
    }

    return this;
  }

  /**
   *
   */
  private void initAutoCompletePanels()
  {
    userselectContainer = new WebMarkupContainer("userselectContainer")
    {
      private static final long serialVersionUID = -4871020567729661148L;

      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        // only show if we are in ajax select task mode
        return ajaxTaskSelectMode == true;
      }
    };
    add(userselectContainer);
    userselectContainer.setOutputMarkupId(true);
    userselectContainer.setOutputMarkupPlaceholderTag(true);
    final TaskSelectAutoCompleteFormComponent searchTaskInput = new TaskSelectAutoCompleteFormComponent(
        "searchTaskInput")
    {
      private static final long serialVersionUID = -7741009167252308262L;

      /**
       * @see org.projectforge.web.task.TaskSelectAutoCompleteFormComponent#onModelChanged(org.apache.wicket.ajax.AjaxRequestTarget)
       */
      @Override
      protected void onModelSelected(final AjaxRequestTarget target, final TaskDO taskDo)
      {
        ajaxTaskSelectMode = false;
        target.appendJavaScript("hideAllTooltips();");
        TaskSelectPanel.this.setModelObject(taskDo);
        TaskSelectPanel.this.onModelSelected(target, taskDo);
      }

    };
    searchTaskInput.setAutocompleteOnlyTaskBookableForTimesheets(autocompleteOnlyTaskBookableForTimesheets);
    userselectContainer.add(searchTaskInput);
    // opener link
    final WebMarkupContainer searchTaskInputOpen = new WebMarkupContainer("searchTaskInputOpen");
    WicketUtils.addTooltip(searchTaskInputOpen, getString("quickselect"));
    searchTaskInputOpen.add(new AjaxEventBehavior("click")
    {
      private static final long serialVersionUID = -938527474172868488L;

      @Override
      protected void onEvent(final AjaxRequestTarget target)
      {
        ajaxTaskSelectMode = true;
        target.appendJavaScript("hideAllTooltips();");
        target.add(divContainer);
        target.add(userselectContainer);
        target.focusComponent(searchTaskInput);
      }
    });
    // close link
    final WebMarkupContainer searchTaskInputClose = new WebMarkupContainer("searchTaskInputClose");
    divContainer.add(searchTaskInputClose);
    searchTaskInputClose.add(new AjaxEventBehavior("click")
    {
      private static final long serialVersionUID = -4334830387094758960L;

      @Override
      protected void onEvent(final AjaxRequestTarget target)
      {
        ajaxTaskSelectMode = false;
        target.appendJavaScript("hideAllTooltips();");
        target.add(divContainer);
        target.add(userselectContainer);
      }
    });
    userselectContainer.add(searchTaskInputClose);
    divContainer.add(searchTaskInputOpen);
  }

  /**
   * Hook method which is called, when the task is set by auto complete field
   *
   * @param target
   * @param taskDo
   */
  protected void onModelSelected(final AjaxRequestTarget target, final TaskDO taskDo)
  {
    target.add(fieldsetPanel.getFieldset());
    target.add(divContainer);
    target.add(userselectContainer);
  }

  /**
   * Will be called if the user has chosen an entry of the task favorites drop down choice.
   *
   * @param task
   */
  protected void selectTask(final TaskDO task)
  {
    setModelObject(task);
    caller.select(selectProperty, task.getId());
  }

  @Override
  public Component getClassModifierComponent()
  {
    return divContainer;
  }

  @Override
  public void convertInput()
  {
    setConvertedInput(getModelObject());
  }

  /**
   * If true (default) then the path from the root task to the currently selected will be shown, otherwise only the name
   * of the task is displayed.
   *
   * @param showPath
   */
  public void setShowPath(final boolean showPath)
  {
    this.showPath = showPath;
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getComponentOutputId()
   */
  @Override
  public String getComponentOutputId()
  {
    divContainer.setOutputMarkupId(true);
    return divContainer.getMarkupId();
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.ComponentWrapperPanel#getFormComponent()
   */
  @Override
  public FormComponent<?> getFormComponent()
  {
    return null;
  }

  /**
   * @param autocompleteOnlyTaskBookableForTimesheets the autocompleteOnlyTaskBookableForTimesheets to set
   * @return this for chaining.
   */
  public TaskSelectPanel setAutocompleteOnlyTaskBookableForTimesheets(
      final boolean autocompleteOnlyTaskBookableForTimesheets)
  {
    this.autocompleteOnlyTaskBookableForTimesheets = autocompleteOnlyTaskBookableForTimesheets;
    return this;
  }

  /**
   * If true then only task will be displayed in autocompletion list which are allowed to have timesheets.
   *
   * @return the autocompleteOnlyTaskBookableForTimesheets
   */
  public boolean isAutocompleteOnlyTaskBookableForTimesheets()
  {
    return autocompleteOnlyTaskBookableForTimesheets;
  }

  private TaskTree getTaskTree()
  {
    if (taskTree == null) {
      taskTree = TaskTreeHelper.getTaskTree();
    }
    return taskTree;
  }
}
