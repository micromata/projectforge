/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.user.GroupDao;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.task.TaskTreePage;
import org.projectforge.web.wicket.AbstractStandardFormPage;

public class TaskWizardPage extends AbstractStandardFormPage implements ISelectCallerPage, WizardPage
{
  enum GroupType{ MANAGER("managerGroup"), TEAM("team"), EXTERNAL("externalGroup");
    final String key;
    GroupType(final String key) {
      this.key = key;
    }
  }
  private static final long serialVersionUID = -297781176304100445L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskWizardPage.class);

  GroupType createdGroup;

  private final TaskWizardForm form;

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
    final TaskNode taskNode = WicketSupport.getTaskDao().getTaskTree().getTaskNodeById(form.task.getId());
    createAccessRights(taskNode, form.managerGroup, GroupType.MANAGER, true);
    createAccessRights(taskNode, form.externalGroup, GroupType.EXTERNAL, true);
    createAccessRights(taskNode, form.team, GroupType.TEAM, true);
    setResponsePage(getReturnToPage());
  }

  private void createAccessRights(final TaskNode taskNode, final GroupDO group, final GroupType groupType,
      final boolean isLeaf)
  {
    if (taskNode == null || group == null || taskNode.getId() == null || group.getId() == null) {
      return;
    }
    if (WicketSupport.getTaskDao().getTaskTree().isRootNode(taskNode) == true) {
      return;
    }
    AccessDao accessDao = WicketSupport.getAccessDao();
    GroupTaskAccessDO access = accessDao.getEntry(taskNode.getTask(), group);
    if (access == null) {
      access = new GroupTaskAccessDO();
      accessDao.setTask(access, taskNode.getId());
      accessDao.setGroup(access, group.getId());
    } else {
      if (access.getDeleted() == true) {
        accessDao.undelete(access);
      }
    }
    if (isLeaf == false) {
      access.guest();
      access.setRecursive(false);
    } else if (groupType == GroupType.MANAGER) {
      access.leader();
      access.setRecursive(true);
    } else if (groupType == GroupType.EXTERNAL) {
      access.external();
      access.setRecursive(true);
    } else if (groupType == GroupType.TEAM) {
      access.employee();
      access.setRecursive(true);
    } else {
        log.error("Unknown group type: " + groupType);
        return;
    }
    accessDao.insertOrUpdate(access);
    // Set minimal access rights for parent task up to the root task.
    createAccessRights(taskNode.getParent(), group, groupType, false);
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
    GroupDao groupDao = WicketSupport.get(GroupDao.class);
    if ("taskId".equals(property) == true) {
      form.task = WicketSupport.getTaskDao().find((Long) selectedValue);
    } else if ("managerGroupId".equals(property) == true) {
      form.managerGroup = groupDao.find((Long) selectedValue);
      form.groupSelectPanelManager.getTextField().modelChanged();
    } else if ("teamId".equals(property) == true) {
      form.team = groupDao.find((Long) selectedValue);
      form.groupSelectPanelTeam.getTextField().modelChanged();
    } else if ("externalGroupId".equals(property) == true) {
      form.externalGroup = groupDao.find((Long) selectedValue);
      form.groupSelectPanelExternal.getTextField().modelChanged();
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
    } else if ("externalGroupId".equals(property) == true) {
      form.externalGroup = null;
      form.groupSelectPanelExternal.getTextField().modelChanged();
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
      if (createdGroup == GroupType.MANAGER) {
        form.managerGroup = (GroupDO) createdObject;
      } else if (createdObject == GroupType.EXTERNAL) {
        form.externalGroup = (GroupDO) createdObject;
      } else {
        form.team = (GroupDO) createdObject;
      }
    }
  }
}
