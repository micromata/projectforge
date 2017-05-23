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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.web.task.TaskEditPage;
import org.projectforge.web.task.TaskSelectPanel;
import org.projectforge.web.task.TaskTreePage;
import org.projectforge.web.user.GroupEditPage;
import org.projectforge.web.user.NewGroupSelectPanel;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.projectforge.web.wicket.flowlayout.Heading3Panel;
import org.projectforge.web.wicket.flowlayout.IconLinkPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

public class TaskWizardForm extends AbstractStandardForm<TaskWizardForm, TaskWizardPage>
{
  private static final long serialVersionUID = -2450673501083584299L;

  private transient TaskTree taskTree;

  protected TaskDO task;

  protected GroupDO managerGroup, team;

  protected NewGroupSelectPanel groupSelectPanelTeam;

  protected NewGroupSelectPanel groupSelectPanelManager;

  public TaskWizardForm(final TaskWizardPage parentPage)
  {
    super(parentPage);
  }

  @Override
  @SuppressWarnings("serial")
  protected void init()
  {
    super.init();
    int number = 1;
    {
      gridBuilder.newFormHeading(getString("wizard"));
      final DivPanel section = gridBuilder.getPanel();
      section.add(new DivTextPanel(section.newChildId(), getString("task.wizard.intro")));
    }
    gridBuilder.newGridPanel();
    {
      final DivPanel section = gridBuilder.getPanel();
      section.add(new Heading3Panel(section.newChildId(), String.valueOf(number++) + ". " + getString("task")));
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("task"));
      final TaskSelectPanel taskSelectPanel = new TaskSelectPanel(fs, new PropertyModel<TaskDO>(this, "task"),
          parentPage, "taskId");
      fs.add(taskSelectPanel);
      taskSelectPanel.setShowFavorites(false).init();
      taskSelectPanel.setRequired(true);

      AjaxSubmitLink createTaskLink = new AjaxSubmitLink(IconLinkPanel.LINK_ID)
      {
        @Override
        protected void onSubmit(final AjaxRequestTarget target, final Form<?> form)
        {
          final PageParameters params = new PageParameters();
          params.add(TaskEditPage.PARAM_PARENT_TASK_ID, getTaskTree().getRootTaskNode().getId());
          final TaskEditPage editPage = new TaskEditPage(params);
          editPage.setReturnToPage(parentPage);
          setResponsePage(editPage);
        }
      };
      createTaskLink.setDefaultFormProcessing(false);
      fs.add(new IconLinkPanel(fs.newChildId(), IconType.PLUS_SIGN,
          new Model<String>(getString("task.wizard.button.createTask")), createTaskLink));
    }
    // Team
    groupSelectPanelTeam = createGroupComponents(number++, "team");

    // Manager group
    groupSelectPanelManager = createGroupComponents(number++, "managerGroup");

    gridBuilder.newGridPanel();
    {
      final DivPanel section = gridBuilder.getPanel();
      section.add(new Heading3Panel(section.newChildId(), getString("task.wizard.action")));
      section.add(new DivTextPanel(section.newChildId(), new Model<String>()
      {
        /**
         * @see org.apache.wicket.model.Model#getObject()
         */
        @Override
        public String getObject()
        {
          if (parentPage.actionRequired() == true) {
            return getString("task.wizard.action.taskAndgroupsGiven");
          } else {
            return getString("task.wizard.action.noactionRequired");
          }
        }
      }));
      addCancelButton(new Button(SingleButtonPanel.WICKET_ID, new Model<String>("cancel"))
      {
        @Override
        public final void onSubmit()
        {
          setResponsePage(TaskTreePage.class);
        }
      });
    }
    final Button finishButton = new Button(SingleButtonPanel.WICKET_ID, new Model<String>("finish"))
    {
      @Override
      public final void onSubmit()
      {
        parentPage.create();
      }
    };
    final SingleButtonPanel finishButtonPanel = new SingleButtonPanel(actionButtons.newChildId(), finishButton,
        getString("task.wizard.finish"), SingleButtonPanel.DEFAULT_SUBMIT);
    actionButtons.add(finishButtonPanel);
    this.setDefaultButton(finishButton);
  }

  @SuppressWarnings("serial")
  private NewGroupSelectPanel createGroupComponents(final int number, final String key)
  {
    gridBuilder.newGridPanel();
    final DivPanel section = gridBuilder.getPanel();
    section
        .add(new Heading3Panel(section.newChildId(), String.valueOf(number) + ". " + getString("task.wizard." + key)));
    section.add(new DivTextPanel(section.newChildId(), getString("task.wizard." + key + ".intro")));
    final FieldsetPanel fs = gridBuilder.newFieldset(getString("group")).suppressLabelForWarning();
    NewGroupSelectPanel groupSelectPanel = new NewGroupSelectPanel(fs.newChildId(),
        new PropertyModel<GroupDO>(this, key), parentPage,
        key + "Id");
    fs.add(groupSelectPanel);
    groupSelectPanel.setShowFavorites(false).init();
    AjaxSubmitLink createGroupLink = new AjaxSubmitLink(IconLinkPanel.LINK_ID)
    {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form)
      {
        parentPage.managerGroupCreated = "managerGroup".equals(key);
        final PageParameters params = new PageParameters();
        final StringBuffer buf = new StringBuffer();
        if (task != null) {
          buf.append(task.getTitle());
        }
        if (parentPage.managerGroupCreated == true) {
          if (task != null) {
            buf.append("-");
          }
          buf.append(getString("task.wizard.managerGroup.groupNameSuffix"));
        }
        params.add(GroupEditPage.PARAM_GROUP_NAME, buf.toString());
        final GroupEditPage editPage = new GroupEditPage(params, key + "Id");
        editPage.setReturnToPage(parentPage);
        setResponsePage(editPage);
      }
    };
    createGroupLink.setDefaultFormProcessing(false);
    fs.add(new IconLinkPanel(fs.newChildId(), IconType.PLUS_SIGN,
        new Model<String>(getString("task.wizard.button.createGroup.tooltip")), createGroupLink));
    return groupSelectPanel;
  }

  private TaskTree getTaskTree()
  {
    if (taskTree == null) {
      taskTree = TaskTreeHelper.getTaskTree();
    }
    return taskTree;
  }
}
