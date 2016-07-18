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
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskFilter;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TaskTreeProvider implements ITreeProvider<TaskNode>
{
  private static final long serialVersionUID = 1416146119319068085L;

  private transient TaskTree taskTree;

  private transient TaskDao taskDao;

  private final TaskFilter taskFilter;

  private boolean showRootNode;

  /**
   * Construct.
   */
  public TaskTreeProvider(TaskDao taskDao, final TaskFilter taskFilter)
  {
    this.taskFilter = taskFilter;
    this.taskDao = taskDao;
    taskFilter.resetMatch();
  }

  /**
   * Nothing to do.
   */
  @Override
  public void detach()
  {
  }

  @Override
  public Iterator<TaskNode> getRoots()
  {
    ensureTaskTree();
    return iterator(taskTree.getRootTaskNode().getChilds(), showRootNode);
  }

  @Override
  public boolean hasChildren(final TaskNode taskNode)
  {
    if (taskNode.isRootNode() == true) {
      // Don't show children of root node again.
      return false;
    }
    return taskNode.hasChilds();
  }

  @Override
  public Iterator<TaskNode> getChildren(final TaskNode taskNode)
  {
    if (taskNode.isRootNode() == true) {
      // Don't show children of root node again.
      return new LinkedList<TaskNode>().iterator();
    }
    return iterator(taskNode.getChilds());
  }

  /**
   * Creates a {@link FooModel}.
   */
  @Override
  public IModel<TaskNode> model(final TaskNode taskNode)
  {
    return new TaskNodeModel(taskNode);
  }

  private Iterator<TaskNode> iterator(final List<TaskNode> nodes)
  {
    return iterator(nodes, false);
  }

  private Iterator<TaskNode> iterator(final List<TaskNode> nodes, final boolean appendRootNode)
  {
    ensureTaskTree();
    final SortedSet<TaskNode> list = new TreeSet<TaskNode>(new Comparator<TaskNode>()
    {
      /**
       * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
       */
      @Override
      public int compare(final TaskNode taskNode1, final TaskNode taskNode2)
      {
        if (taskNode1.isRootNode() == true) {
          // Show root node at last position.
          return 1;
        }
        if (taskNode2.isRootNode() == true) {
          // Show root node at last position.
          return -1;
        }
        String title1 = taskNode1.getTask().getTitle();
        title1 = title1 != null ? title1.toLowerCase() : "";
        String title2 = taskNode2.getTask().getTitle();
        title2 = title2 != null ? title2.toLowerCase() : "";
        return title1.compareTo(title2);
      }
    });
    if (appendRootNode == true) {
      if (taskFilter.match(taskTree.getRootTaskNode(), null, null) == true) {
        list.add(taskTree.getRootTaskNode());
      }
    }
    if (nodes == null || nodes.isEmpty() == true) {
      return list.iterator();
    }
    final PFUserDO user = ThreadLocalUserContext.getUser();
    for (final TaskNode node : nodes) {
      if (taskFilter.match(node, taskDao, user) == true
          && taskDao.hasSelectAccess(user, node.getTask(), false) == true) {
        list.add(node);
      }
    }
    return list.iterator();
  }

  /**
   * A {@link Model} which uses an id to load its {@link Foo}.
   * 
   * If {@link Foo}s were {@link Serializable} you could just use a standard {@link Model}.
   * 
   * @see #equals(Object)
   * @see #hashCode()
   */
  private static class TaskNodeModel extends LoadableDetachableModel<TaskNode>
  {
    private static final long serialVersionUID = 1L;

    private final Integer id;

    private transient TaskTree taskTree;

    public TaskNodeModel(final TaskNode taskNode)
    {
      super(taskNode);
      id = taskNode.getId();
    }

    @Override
    protected TaskNode load()
    {
      if (taskTree == null) {
        taskTree = TaskTreeHelper.getTaskTree();
      }
      return taskTree.getTaskNodeById(id);
    }

    /**
     * Important! Models must be identifyable by their contained object.
     */
    @Override
    public boolean equals(final Object obj)
    {
      if (obj instanceof TaskNodeModel) {
        return ((TaskNodeModel) obj).id.equals(id);
      }
      return false;
    }

    /**
     * Important! Models must be identifyable by their contained object.
     */
    @Override
    public int hashCode()
    {
      return id.hashCode();
    }
  }

  /**
   * @param showRootNode the showRootNode to set
   * @return this for chaining.
   */
  public TaskTreeProvider setShowRootNode(final boolean showRootNode)
  {
    this.showRootNode = showRootNode;
    return this;
  }

  private void ensureTaskTree()
  {
    if (this.taskTree == null) {
      this.taskTree = TaskTreeHelper.getTaskTree();
    }
  }
}
