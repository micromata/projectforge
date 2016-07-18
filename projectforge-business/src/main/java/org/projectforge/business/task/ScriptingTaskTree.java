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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.projectforge.business.fibu.AuftragsPositionVO;
import org.projectforge.business.fibu.ProjektDO;


/**
 * Proxy of TaskTree for scripting.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ScriptingTaskTree
{
  TaskTree __baseDao;

  static List<ScriptingTaskNode> convert(final List<TaskNode> list)
  {
    if (list == null) {
      return null;
    }
    final List<ScriptingTaskNode> result = new ArrayList<ScriptingTaskNode>(list.size());
    for (final TaskNode node : list) {
      result.add(new ScriptingTaskNode(node));
    }
    return result;
  }

  static ScriptingTaskNode convert(final TaskNode node)
  {
    if (node == null) {
      return null;
    }
    return new ScriptingTaskNode(node);
  }

  public ScriptingTaskTree(final TaskTree taskTree)
  {
    this.__baseDao = taskTree;
  }

  public ScriptingTaskNode getRootTaskNode()
  {
    return new ScriptingTaskNode(__baseDao.getRootTaskNode());
  }

  public List<ScriptingTaskNode> getPath(Integer taskId, Integer ancestorTaskId)
  {
    return convert(__baseDao.getPath(taskId, ancestorTaskId));
  }

  public List<ScriptingTaskNode> getPathToRoot(Integer taskId)
  {
    return getPath(taskId, null);
  }

  public ScriptingTaskNode getTaskNodeById(final Integer id)
  {
    return convert(__baseDao.getTaskNodeById(id));
  }

  /**
   * Gets a copy of the found projekt.
   * @param taskId
   * @return
   */
  public ProjektDO getProjekt(final Integer taskId)
  {
    final ProjektDO projekt = __baseDao.getProjekt(taskId);
    if (projekt == null) {
      return null;
    }
    final ProjektDO result = new ProjektDO();
    result.copyValuesFrom(projekt);
    return result;
  }

  public ProjektDO getProjekt(final ScriptingTaskNode node)
  {
    return getProjekt(node.getId());
  }

  public boolean isRootNode(final ScriptingTaskNode node)
  {
    return __baseDao.isRootNode(node.__baseObject);
  }

  public boolean isRootNode(final TaskDO task)
  {
    return __baseDao.isRootNode(task);
  }

  public boolean hasOrderPositionsEntries()
  {
    return __baseDao.hasOrderPositionsEntries();
  }

  public boolean hasOrderPositions(final Integer taskId, final boolean recursive)
  {
    return __baseDao.hasOrderPositions(taskId, recursive);
  }

  public boolean hasOrderPositionsUpwards(final Integer taskId)
  {
    return __baseDao.hasOrderPositionsUpwards(taskId);
  }

  public Set<AuftragsPositionVO> getOrderPositionsUpwards(final Integer taskId)
  {
    return __baseDao.getOrderPositionsUpwards(taskId);
  }

  public BigDecimal getPersonDays(final Integer taskId)
  {
    return __baseDao.getPersonDays(taskId);
  }

  public BigDecimal getPersonDays(final ScriptingTaskNode node)
  {
    return __baseDao.getPersonDays(node.__baseObject);
  }

  public BigDecimal getOrderedPersonDaysSum(final ScriptingTaskNode node)
  {
    return __baseDao.getOrderedPersonDaysSum(node.__baseObject);
  }

  public TaskNode getPersonDaysNode(final ScriptingTaskNode node)
  {
    return __baseDao.getPersonDaysNode(node.__baseObject);
  }
}
