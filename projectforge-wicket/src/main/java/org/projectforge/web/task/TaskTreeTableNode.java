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

import java.io.Serializable;

import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskNode;
import org.projectforge.common.i18n.Priority;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.tree.TreeTableNode;


/**
 * Represents a single node as part of the TreeTable.
 */
public class TaskTreeTableNode extends TreeTableNode implements Serializable
{
  private static final long serialVersionUID = -7215399905310521392L;

  private TaskNode taskNode;

  /**
   * Only for deserialization.
   */
  protected TaskTreeTableNode()
  {
  }

  protected TaskTreeTableNode(final TaskTreeTableNode parent, final TaskNode taskNode)
  {
    super(parent, taskNode.getId());
    this.taskNode = taskNode;
  }

  public TaskNode getTaskNode()
  {
    return taskNode;
  }

  public TaskDO getTask()
  {
    return taskNode.getTask();
  }

  public String getTaskTitle()
  {
    return getTask().getTitle();
  }

  public String getId()
  {
    return taskNode.getId().toString();
  }

  public String getShortDescription()
  {
    return getTask().getShortDescription();
  }

  public Priority getPriority()
  {
    return getTask().getPriority();
  }

  public TaskStatus getStatus()
  {
    return getTask().getStatus();
  }

  public boolean isDeleted()
  {
    return taskNode.isDeleted();
  }

  public PFUserDO getResponsibleUser()
  {
    return getTask().getResponsibleUser();
  }

  /**
   * Return a String representation of this object.
   */
  @Override
  public String toString()
  {
    final StringBuffer sb = new StringBuffer("TaskTreeTableNode[taskName=");
    sb.append(getTaskTitle());
    sb.append(",id=");
    sb.append(getId());
    sb.append("]");
    return (sb.toString());
  }

  /** Should be overwrite by derived classes. */
  @Override
  public int compareTo(final TreeTableNode obj)
  {
    final TaskTreeTableNode node = (TaskTreeTableNode) obj;
    return taskNode.getTask().getTitle().compareTo(node.taskNode.getTask().getTitle());
  }
}
