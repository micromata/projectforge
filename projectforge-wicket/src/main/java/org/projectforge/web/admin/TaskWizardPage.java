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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.user.GroupDao;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.task.TaskTreePage;
import org.projectforge.web.wicket.AbstractStandardFormPage;

public class TaskWizardPage extends AbstractStandardFormPage implements ISelectCallerPage, WizardPage
{
  private static final long serialVersionUID = -297781176304100445L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TaskWizardPage.class);

  boolean managerGroupCreated;

  private final TaskWizardForm form;

  @SpringBean
  private TaskDao taskDao;

  @SpringBean
  private GroupDao groupDao;

  @SpringBean
  private AccessDao accessDao;

  public TaskWizardPage(final PageParameters parameters)
  {
    super(parameters);
    form = new TaskWizardForm(this);
    body.add(form);
    form.init();
  }

  void create()
  {
    if (actionRequired() == false) {
      log.info("create: Nothing to do.");
      return;
    }
    final TaskNode taskNode = taskDao.getTaskTree().getTaskNodeById(form.task.getId());
    createAccessRights(taskNode, form.managerGroup, true, true);
    createAccessRights(taskNode, form.team, false, true);
    setResponsePage(TaskTreePage.class);
  }

  private void createAccessRights(final TaskNode taskNode, final GroupDO group, final boolean isManagerGroup,
      final boolean isLeaf)
  {
    if (taskNode == null || group == null || taskNode.getId() == null || group.getId() == null) {
      return;
    }
    if (taskDao.getTaskTree().isRootNode(taskNode) == true) {
      return;
    }
    GroupTaskAccessDO access = accessDao.getEntry(taskNode.getTask(), group);
    if (access == null) {
      access = new GroupTaskAccessDO();
      accessDao.setTask(access, taskNode.getId());
      accessDao.setGroup(access, group.getId());
    } else {
      if (access.isDeleted() == true) {
        accessDao.undelete(access);
      }
    }
    if (isLeaf == false) {
      access.guest();
      access.setRecursive(false);
    } else if (isManagerGroup) {
      access.leader();
      access.setRecursive(true);
    } else {
      access.employee();
      access.setRecursive(true);
    }
    accessDao.saveOrUpdate(access);
    createAccessRights(taskNode.getParent(), group, isManagerGroup, false);
  }

  /**
   * Visibility of the create button.
   */
  boolean actionRequired()
  {
    return form.task != null;
  }

  @Override
  public void cancelSelection(final String property)
  {
  }

  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("taskId".equals(property) == true) {
      form.task = taskDao.getById((Integer) selectedValue);
    } else if ("managerGroupId".equals(property) == true) {
      form.managerGroup = groupDao.getById((Integer) selectedValue);
      form.groupSelectPanelManager.getTextField().modelChanged();
    } else if ("teamId".equals(property) == true) {
      form.team = groupDao.getById((Integer) selectedValue);
      form.groupSelectPanelTeam.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property)
  {
    if ("taskId".equals(property) == true) {
      form.task = null;
    } else if ("managerGroupId".equals(property) == true) {
      form.managerGroup = null;
      form.groupSelectPanelManager.getTextField().modelChanged();
    } else if ("teamId".equals(property) == true) {
      form.team = null;
      form.groupSelectPanelTeam.getTextField().modelChanged();
    } else {
      log.error("Property '" + property + "' not supported for deselection.");
    }
  }

  @Override
  protected String getTitle()
  {
    return getString("task.wizard.pageTitle");
  }

  @Override
  public void setCreatedObject(final Object createdObject)
  {
    if (createdObject == null) {
      return;
    } else if (createdObject instanceof TaskDO) {
      form.task = (TaskDO) createdObject;
    } else if (createdObject instanceof GroupDO) {
      if (managerGroupCreated == true) {
        form.managerGroup = (GroupDO) createdObject;
      } else {
        form.team = (GroupDO) createdObject;
      }
    }
  }
}
