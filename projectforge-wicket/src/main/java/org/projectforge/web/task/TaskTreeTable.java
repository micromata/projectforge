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

package org.projectforge.web.task;

import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.web.tree.TreeTable;
import org.projectforge.web.tree.TreeTableFilter;
import org.projectforge.web.tree.TreeTableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * The implementation of TreeTable for tasks. Used for browsing the tasks (tree view).
 */
public class TaskTreeTable extends TreeTable<TaskTreeTableNode>
{
  private static final long serialVersionUID = -4776489786818696163L;

  private static final Logger log = LoggerFactory.getLogger(TaskTreeTable.class);

  private TaskNode rootNode;

  /** Time of last modification in milliseconds from 1970-01-01. */
  private long timeOfLastModification = 0;

  public TaskTreeTable()
  {
    reload();
  }

  public TaskTreeTable(final TaskNode rootNode)
  {
    this();
    this.rootNode = rootNode;
  }

  public TreeTableNode setOpenedStatusOfNode(String eventKey, Integer hashId)
  {
    return super.setOpenedStatusOfNode(eventKey, hashId);
  }

  @Override
  public List<TaskTreeTableNode> getNodeList(TreeTableFilter<TreeTableNode> filter)
  {
    if (timeOfLastModification < TaskTree.getInstance().getTimeOfLastModification()) {
      reload();
    }
    return super.getNodeList(filter);
  }

  protected void addDescendantNodes(TaskTreeTableNode parent)
  {
    TaskNode task = parent.getTaskNode();
    if (task.getChildren() != null) {
      for (TaskNode node : task.getChildren()) {
        if (TaskTree.getInstance().hasSelectAccess(node) == true) {
          // The logged in user has select access, so add this task node
          // to this tree table:
          TaskTreeTableNode child = new TaskTreeTableNode(parent, node);
          addTreeTableNode(child);
          addDescendantNodes(child);
        }
      }
    }
  }

  protected synchronized void reload()
  {
    log.debug("Reloading task tree.");
    allNodes.clear();
    if (rootNode != null) {
      root = new TaskTreeTableNode(null, rootNode);
    } else {
      root = new TaskTreeTableNode(null, TaskTree.getInstance().getRootTaskNode());
    }
    addDescendantNodes(root);
    updateOpenStatus();
    timeOfLastModification = new Date().getTime();
  }

  boolean hasSelectAccess(TaskNode node)
  {
    return TaskTree.getInstance().hasSelectAccess(node);
  }
}
