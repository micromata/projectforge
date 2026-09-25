/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.wicket.model.IModel;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.user.service.UserPrefService;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.wicket.tree.TableTreeExpansion;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Kai Reinhard
 */
@XStreamAlias("TaskTreeExpansion")
public class TaskTreeExpansion extends TableTreeExpansion<Long, TaskNode>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TaskTreeExpansion.class);

  private static final long serialVersionUID = 5151537746424532422L;

  private static TaskTreeExpansion get()
  {
    final TaskTreeExpansion expansion = new TaskTreeExpansion();
    try {
      final UserPrefService userPrefService = WicketSupport.get(UserPrefService.class);
      final Set<?> rawIds = (Set<?>) userPrefService.getEntry(UserPrefService.LEGACY_XML_AREA, TaskTree.USER_PREFS_KEY_OPEN_TASKS);
      if (rawIds != null) {
        if (isAllLong(rawIds)) {
          // The set already holds Long ids. Wrap this very instance (the live reference held in the user-pref
          // cache): Wicket mutates it via TableTreeExpansion.add()/remove() when a node is expanded/collapsed,
          // and those mutations must survive across requests (and be persisted on flush).
          @SuppressWarnings("unchecked") final Set<Long> ids = (Set<Long>) rawIds;
          expansion.setIds(ids);
        } else {
          // The JSON/legacy store doesn't preserve the element type of a raw Set, so ids may come back as
          // Integer, Long or even String. Normalize to Long (the type TaskNode ids are compared against) once,
          // then write the clean Set back so subsequent reads return this same live reference (see above).
          final Set<Long> ids = new HashSet<>();
          for (final Object id : rawIds) {
            if (id == null) {
              continue;
            }
            try {
              ids.add(id instanceof Number ? ((Number) id).longValue() : Long.parseLong(id.toString().trim()));
            } catch (final NumberFormatException ex) {
              log.warn("Ignoring non-numeric open-task id in user prefs: '" + id + "'");
            }
          }
          userPrefService.putEntry(UserPrefService.LEGACY_XML_AREA, TaskTree.USER_PREFS_KEY_OPEN_TASKS, ids, true);
          expansion.setIds(ids);
        }
      } else {
        // Persist the open entries in the data-base.
        userPrefService.putEntry(UserPrefService.LEGACY_XML_AREA, TaskTree.USER_PREFS_KEY_OPEN_TASKS, expansion.getIds(), true);
      }
    } catch (final Exception ex) {
      log.error(ex.getMessage(), ex);
    }
    return expansion;
  }

  /**
   * @return true if every element of the given set is a {@link Long} (an empty set counts as all-Long). Such a set can
   * be reused as-is; a set containing Integer/String elements has to be normalized first (see {@link #get()}).
   */
  private static boolean isAllLong(final Set<?> ids)
  {
    for (final Object id : ids) {
      if (!(id instanceof Long)) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return The expansion model. Any previous persisted state of open rows will be restored from
   * {@link UserPrefService}.
   */
  @SuppressWarnings("serial")
  public static IModel<Set<TaskNode>> getExpansionModel()
  {
    return new IModel<Set<TaskNode>>()
    {
      @Override
      public Set<TaskNode> getObject()
      {
        return get();
      }
    };
  }
}
