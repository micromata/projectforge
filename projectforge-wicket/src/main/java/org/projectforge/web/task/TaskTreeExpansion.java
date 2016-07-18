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

import java.util.Set;

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.web.user.UserPreferencesHelper;
import org.projectforge.web.wicket.tree.TableTreeExpansion;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XStreamAlias("TaskTreeExpansion")
public class TaskTreeExpansion extends TableTreeExpansion<Integer, TaskNode>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TaskTreeExpansion.class);

  private static final long serialVersionUID = 5151537746424532422L;

  private static TaskTreeExpansion get()
  {
    final TaskTreeExpansion expansion = new TaskTreeExpansion();
    try {
      @SuppressWarnings("unchecked")
      final Set<Integer> ids = (Set<Integer>) UserPreferencesHelper.getEntry(TaskTree.USER_PREFS_KEY_OPEN_TASKS);
      if (ids != null) {
        expansion.setIds(ids);
      } else {
        // Persist the open entries in the data-base.
        UserPreferencesHelper.putEntry(TaskTree.USER_PREFS_KEY_OPEN_TASKS, expansion.getIds(), true);
      }
    } catch (final Exception ex) {
      log.error(ex.getMessage(), ex);
    }
    return expansion;
  }

  /**
   * @return The expansion model. Any previous persisted state of open rows will be restored from
   *         {@link UserPreferencesHelper}.
   */
  @SuppressWarnings("serial")
  public static IModel<Set<TaskNode>> getExpansionModel()
  {
    return new AbstractReadOnlyModel<Set<TaskNode>>()
    {
      /**
       * @see org.apache.wicket.model.AbstractReadOnlyModel#getObject()
       */
      @Override
      public Set<TaskNode> getObject()
      {
        return get();
      }
    };
  }
}
