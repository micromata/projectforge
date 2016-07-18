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

package org.projectforge.business.task;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.GroupTaskAccessDO;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.IdObject;

/**
 * Represents a single task as part of the TaskTree. The data of a task node is stored in the database.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TaskNode implements IdObject<Integer>, Serializable
{
  private static final long serialVersionUID = -3759574521842841341L;

  /** For log messages. */
  private static final Logger log = Logger.getLogger(TaskNode.class);

  /** Reference to the parent task node with the parentTaskID. */
  TaskNode parent = null;

  ProjektDO projekt;

  /** Total duration of all time sheets of this task (excluding the child tasks) in seconds. */
  long totalDuration = 0;

  /**
   * Sum of all ordered person days excluding descendant nodes. Ordered person days are defined by the sum of all
   * assigned order position's person days. Used and set by task tree.
   */
  BigDecimal orderedPersonDays;

  /**
   * References to all child nodes in an ArrayList from element typ TaskNode.
   */
  List<TaskNode> childs = null;

  /** The data of this TaskNode. */
  TaskDO task = null;

  boolean bookableForTimesheets;

  /**
   * For every group with access to this node the permissions will be stored here.
   */
  private final List<GroupTaskAccessDO> groupTaskAccessList = new ArrayList<GroupTaskAccessDO>();

  public TaskNode()
  {
  }

  /**
   * @return True, if the parent task id of the underlying task is null, false otherwise.
   */
  public boolean isRootNode()
  {
    return this.task.getParentTaskId() == null;
  }

  public void setTask(final TaskDO task)
  {
    this.task = task;
  }

  public TaskDO getTask()
  {
    return this.task;
  }

  /** The id of this task given by the database. */
  public Integer getId()
  {
    return task.getId();
  }

  /** The id of this task given by the database. */
  public Integer getTaskId()
  {
    return task.getId();
  }

  public Integer getParentId()
  {
    if (parent == null) {
      return null;
    }
    return parent.getId();
  }

  /** Returns the parent task. */
  public TaskNode getParent()
  {
    return this.parent;
  }

  public void internalSetParent(final TaskNode parent)
  {
    this.parent = parent;
    if (parent != null) {
      parent.addChild(this);
    }
  }

  /**
   * @return The reference of the assigned task, or if not given or blank of the parent task node. If the parent task
   *         node has no reference the grand parent task reference will be assumed and so on.
   */
  public String getReference()
  {
    if (StringUtils.isNotBlank(task.getReference()) == true) {
      return task.getReference();
    } else if (parent != null) {
      return parent.getReference();
    } else {
      return "";
    }
  }

  /**
   * Gets the project, which is assigned to the task or if not found to the parent task or grand parent task etc.
   * 
   * @param taskId
   * @return null, if now project is assigned to this task or ancestor tasks.
   * @see ProjektDO#getTask()
   */
  public ProjektDO getProjekt()
  {
    return getProjekt(true);
  }

  /**
   * Gets the project, which is assigned to the task or if not found to the parent task or grand parent task etc.
   * 
   * @param taskId
   * @param recursive If true then search the ancestor nodes for a given project.
   * @return null, if now project is assigned to this task or ancestor tasks.
   * @see ProjektDO#getTask()
   */
  public ProjektDO getProjekt(final boolean recursive)
  {
    if (projekt != null) {
      return projekt;
    } else if (recursive == true && parent != null) {
      return parent.getProjekt();
    } else {
      return null;
    }
  }

  public boolean isDeleted()
  {
    return task.isDeleted();
  }

  /**
   * @return True if this node is closed/deleted or any ancestor node is closed/deleted.
   */
  public boolean isFinished()
  {
    if (task.isDeleted() || task.getStatus() == TaskStatus.C) {
      return true;
    }
    if (parent != null) {
      return parent.isFinished();
    }
    return false;
  }

  /**
   * @return the bookableForTimesheets
   */
  public boolean isBookableForTimesheets()
  {
    return bookableForTimesheets;
  }

  public List<Integer> getDescendantIds()
  {
    final List<Integer> descendants = new ArrayList<Integer>();
    getDescendantIds(descendants);
    return descendants;
  }

  private void getDescendantIds(final List<Integer> descendants)
  {
    if (this.childs != null) {
      for (final TaskNode node : this.childs) {
        if (descendants.contains(node.getId()) == false) {
          // Paranoia setting for cyclic references.
          descendants.add(node.getId());
          node.getDescendantIds(descendants);
        }
      }
    }
  }

  public List<Integer> getAncestorIds()
  {
    final List<Integer> ancestors = new ArrayList<Integer>();
    getAncestorIds(ancestors);
    return ancestors;
  }

  private void getAncestorIds(final List<Integer> ancestors)
  {
    if (this.parent != null) {
      if (ancestors.contains(this.parent.getId()) == false) {
        // Paranoia setting for cyclic references.
        ancestors.add(this.parent.getId());
        this.parent.getAncestorIds(ancestors);
      }
    }
  }

  /**
   * Returns all childs of this task in an ArrayList with elements from type TaskNode.
   */
  public List<TaskNode> getChilds()
  {
    if (this.childs == null) {
      this.childs = new ArrayList<TaskNode>();
    }
    return this.childs;
  }

  /** Has this task any childs? */
  public boolean hasChilds()
  {
    return this.childs != null && this.childs.size() > 0 ? true : false;
  }

  /** Checks if the given node is a child / descendant of this node. */
  public boolean isParentOf(final TaskNode node)
  {
    if (this.childs == null) {
      return false;
    }
    for (final TaskNode child : this.childs) {
      if (child.equals(node) == true) {
        return true;
      } else if (child.isParentOf(node) == true) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the path to the root node in an ArrayList. The list contains also the current task.
   */
  public List<TaskNode> getPathToRoot()
  {
    return getPathToAncestor(null);
  }

  /**
   * Returns the path to the parent node in an ArrayList.
   */
  public List<TaskNode> getPathToAncestor(final Integer ancestorTaskId)
  {
    if (this.parent == null || this.task.getId().equals(ancestorTaskId) == true) {
      return new ArrayList<TaskNode>();
    }
    final List<TaskNode> path = this.parent.getPathToAncestor(ancestorTaskId);
    path.add(this);
    return path;
  }

  /**
   * Sets / changes the parent of this node. This method does not modify the parent task! So it should be called only by
   * TaskTree.
   */
  void setParent(final TaskNode parent)
  {
    if (parent != null) {
      if (parent.getId().equals(getId()) == true || this.isParentOf(parent)) {
        log.error("Oups, cyclic reference detection: taskId = " + getId() + ", parentTaskId = " + parent.getId());
        throw new UserException(TaskDao.I18N_KEY_ERROR_CYCLIC_REFERENCE);
      }
      this.parent = parent;
      this.task.setParentTask(parent.getTask());
    }
  }

  /**
   * Adds a new task as a child of this node. It does not check wether this task already exist as child or not! This
   * method does not modify the child task!
   */
  void addChild(final TaskNode child)
  {
    if (child != null) {
      if (child.getId().equals(getId()) == true || child.isParentOf(this)) {
        log.error("Oups, cyclic reference detection: taskId = " + getId() + ", parentTaskId = " + parent.getId());
        return;
      }
      if (this.childs == null) {
        this.childs = new ArrayList<TaskNode>();
      }
      this.childs.add(child);
    }
  }

  /**
   * Removes a child task of this node. This method does not modify the child task!
   */
  void removeChild(final TaskNode child)
  {
    if (child == null) {
      log.error("Oups, child is null, can't remove it from parent.");
    } else if (this.childs == null) {
      log.error("Oups, this node has no childs to remove.");
    } else if (this.childs.contains(child) == false) {
      log.error("Oups, this node doesn't contain given child.");
    } else {
      log.debug("Removing child " + child.getTaskId() + " from parent " + this.getTaskId());
      this.childs.remove(child);
    }
  }

  /**
   * Checks the desired permission for the given group to this task. If no GroupTaskAccess is defined for this task for
   * the given group, hasPermission will be called of the parent task.
   * 
   * @param groupId The id of the group to check.
   * @param accessType TASK_ACCESS, ...
   * @param opType Select, insert, update or delete.
   * @see AccessType
   * @see OperationType
   */
  public boolean hasPermission(final Integer groupId, final AccessType accessType, final OperationType opType)
  {
    final GroupTaskAccessDO groupAccess = getGroupTaskAccess(groupId);
    if (groupAccess == null) {
      if (parent != null) {
        return parent.isPermissionRecursive(groupId) && parent.hasPermission(groupId, accessType, opType);
      }
      // This is the root node.
      return false;
    }
    return groupAccess.hasPermission(accessType, opType);
  }

  public boolean isPermissionRecursive(final Integer groupId)
  {
    final GroupTaskAccessDO groupAccess = getGroupTaskAccess(groupId);
    return groupAccess == null || groupAccess.isRecursive() == true;
  }

  /**
   * Gets the GroupTaskAccessDO for the given group.
   * 
   * @param groupId
   * @return The GroupTaskAccessDO or null if not exists.
   */
  GroupTaskAccessDO getGroupTaskAccess(final Integer groupId)
  {
    Validate.notNull(groupId);
    for (final GroupTaskAccessDO access : groupTaskAccessList) {
      if (groupId.equals(access.getGroupId()) == true) {
        return access;
      }
    }
    return null;
  }

  /**
   * Sets the task group access to this task node for the given group. Removes any previous stored GroupTaskAccessDO for
   * the same group if exists. Multiple GroupTaskAccessDO entries for one group will be avoided.
   * 
   * @param GroupTaskAccessDO
   */
  void setGroupTaskAccess(final GroupTaskAccessDO groupTaskAccess)
  {
    Validate.isTrue(ObjectUtils.equals(this.getTaskId(), groupTaskAccess.getTaskId()) == true);
    // TODO: Should be called after update and insert into database.
    if (log.isInfoEnabled() == true) {
      log.debug("Set explicit access, taskId = " + getTaskId() + ", groupId = " + groupTaskAccess.getGroupId());
    }
    synchronized (groupTaskAccessList) {
      removeGroupTaskAccess(groupTaskAccess.getGroupId());
      groupTaskAccessList.add(groupTaskAccess);
    }
  }

  /**
   * Removes the GroupTaskAccessDO for the given group if exists.
   * 
   * @param groupId
   * @return true if an entry was found and removed, otherwise false.
   */
  boolean removeGroupTaskAccess(final Integer groupId)
  {
    // TODO: Should be called after deleting from database.
    Validate.notNull(groupId);
    boolean result = false;
    synchronized (groupTaskAccessList) {
      final Iterator<GroupTaskAccessDO> it = groupTaskAccessList.iterator();
      while (it.hasNext() == true) {
        final GroupTaskAccessDO access = it.next();
        if (groupId.equals(access.getGroupId()) == true) {
          it.remove();
          result = true;
        }
      }
    }
    return result;
  }

  /**
   * Gets the total duration of all time sheets in seconds.
   * 
   * @param recursive If true, then the durations of all time sheets of the sub tasks will be added.
   * @return
   */
  public long getDuration(final TaskTree taskTree, final boolean recursive)
  {
    if (totalDuration < 0) {
      taskTree.readTotalDuration(this.getId());
    }
    if (recursive == false || childs == null) {
      return totalDuration;
    }
    long duration = totalDuration;
    for (final TaskNode child : childs) {
      duration += child.getDuration(taskTree, true);
    }
    return duration;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof TaskNode) {
      final TaskNode other = (TaskNode) o;
      return ObjectUtils.equals(this.getParentId(), other.getParentId()) == true
          && ObjectUtils.equals(this.getTask().getTitle(), other.getTask().getTitle()) == true;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(this.getParentId()).append(this.getTask().getTitle());
    return hcb.toHashCode();
  }

  @Override
  public String toString()
  {
    final ToStringBuilder sb = new ToStringBuilder(this);
    sb.append("id", getId());
    Object parentId = null;
    if (this.parent != null) {
      parentId = this.parent.getId();
    }
    log.debug("id: " + this.getId() + ", parentId: " + parentId);
    sb.append("parent", parentId);
    sb.append("title", task.getTitle());
    sb.append("childs", this.childs);
    return sb.toString();
  }

  Element addXMLElement(final Element parent)
  {
    final Element el = parent.addElement("task").addAttribute("id", String.valueOf(this.getId()))
        .addAttribute("name", this.task.getTitle());
    if (this.childs != null) {
      for (final TaskNode node : this.childs) {
        node.addXMLElement(el);
      }
    }
    return el;
  }
}
