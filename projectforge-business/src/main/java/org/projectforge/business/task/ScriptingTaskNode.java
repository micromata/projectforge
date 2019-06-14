/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import java.util.List;

import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.i18n.InternalErrorException;

/**
 * Proxy of TaskNode for scripting.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ScriptingTaskNode
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ScriptingTaskNode.class);

  TaskNode __baseObject;

  private TaskDO task;

  ScriptingTaskNode(final TaskNode node)
  {
    __baseObject = node;

    try {
      task = (TaskDO) node.getTask().clone();
    } catch (CloneNotSupportedException ex) {
      log.error("Exception encountered " + ex, ex);
      throw new InternalErrorException();
    }
  }

  public Integer getId()
  {
    return task.getId();
  }

  public boolean isRootNode()
  {
    return __baseObject.isRootNode();
  }

  public TaskDO getTask()
  {
    return this.task;
  }

  public Integer getTaskId()
  {
    return task.getId();
  }

  public Integer getParentId()
  {
    return __baseObject.getParentId();
  }

  public ScriptingTaskNode getParent()
  {
    return new ScriptingTaskNode(__baseObject.getParent());
  }

  public String getReference()
  {
    return __baseObject.getReference();
  }

  public boolean isDeleted()
  {
    return __baseObject.isDeleted();
  }

  public boolean isFinished()
  {
    return __baseObject.isFinished();
  }

  public List<Integer> getDescendantIds()
  {
    return __baseObject.getDescendantIds();
  }

  public List<ScriptingTaskNode> getChilds()
  {
    return ScriptingTaskTree.convert(__baseObject.getChilds());
  }

  public boolean hasChilds()
  {
    return __baseObject.hasChilds();
  }

  public boolean isParentOf(final ScriptingTaskNode node)
  {
    return __baseObject.isParentOf(node.__baseObject);
  }

  public List<ScriptingTaskNode> getPathToRoot()
  {
    return getPathToAncestor(null);
  }

  public List<ScriptingTaskNode> getPathToAncestor(Integer ancestorTaskId)
  {
    return ScriptingTaskTree.convert(__baseObject.getPathToAncestor(ancestorTaskId));
  }

  public boolean hasPermission(Integer groupId, AccessType accessType, OperationType opType)
  {
    return __baseObject.hasPermission(groupId, accessType, opType);
  }

  public long getDuration(final ScriptingTaskTree taskTree, final boolean recursive)
  {
    return __baseObject.getDuration(taskTree.__baseDao, recursive);
  }

  @Override
  public String toString()
  {
    return __baseObject.toString();
  }
}
