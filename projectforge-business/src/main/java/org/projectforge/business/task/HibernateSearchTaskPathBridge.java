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

import org.hibernate.search.bridge.TwoWayStringBridge;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.framework.utils.NumberHelper;

import java.util.List;

/**
 * TaskPathBridge for hibernate search to search in the parent task titles.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class HibernateSearchTaskPathBridge implements TwoWayStringBridge {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
          .getLogger(HibernateSearchTaskPathBridge.class);

  private TaskTree getTaskTree() {
    return TaskTreeHelper.getTaskTree();
  }

  @Override
  public Object stringToObject(String stringValue) {
    if (!stringValue.matches("[0-9]+:.*")) {
      return null;
    }
    final Integer number = NumberHelper.parseInteger(stringValue.substring(0, stringValue.indexOf(':')));
    if (number == null) {
      return null;
    }
    return getTaskTree().getTaskById(number);
  }

  /**
   * Get all names of ancestor tasks and task itself and creates an index containing all task titles separated by '|'.
   * <br/>
   * Please note: does not work in JUnit test mode.
   */
  @Override
  public String objectToString(Object object) {
    if (object instanceof String) {
      return (String) object;
    }
    final TaskDO task = (TaskDO) object;
    final TaskNode taskNode = getTaskTree().getTaskNodeById(task.getId());
    if (taskNode == null) {
      return "";
    }
    final List<TaskNode> list = taskNode.getPathToRoot();
    final StringBuilder buf = new StringBuilder();
    buf.append(task.getId()).append(": "); // Adding the id for deserialization
    list.forEach(node -> {
      buf.append(node.getTask().getTitle()).append("|");
    });
    if (log.isDebugEnabled()) {
      log.debug(buf.toString());
    }
    return buf.toString();
  }
}
