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

package org.projectforge.business.task.formatter;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.projectforge.business.common.OutputType;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.framework.i18n.AbstractFormatter;

public class TaskFormatter extends AbstractFormatter
{

  /**
   * Formats path to root: "task1 -> task2 -> task3".
   *
   * @param taskId
   * @param showCurrentTask if true also the given task by id will be added to the path, otherwise the path of the
   *                        parent task will be shown.
   * @param escapeHtml
   */
  public static String getTaskPath(final Integer taskId, final boolean showCurrentTask, final OutputType outputType)
  {
    return getTaskPath(taskId, null, showCurrentTask, outputType);
  }

  /**
   * Formats path to ancestor task if given or to root: "task1 -> task2 -> task3".
   *
   * @param taskId
   * @param ancestorTaskId
   * @param showCurrentTask if true also the given task by id will be added to the path, otherwise the path of the
   *                        parent task will be shown.
   * @param escapeHtml
   */
  public static String getTaskPath(Integer taskId, final Integer ancestorTaskId, final boolean showCurrentTask,
      final OutputType outputType)
  {
    if (taskId == null) {
      return null;
    }
    final TaskTree taskTree = TaskTreeHelper.getTaskTree();
    TaskNode n = taskTree.getTaskNodeById(taskId);
    if (n == null) {
      return null;
    }
    if (showCurrentTask == false) {
      n = n.getParent();
      if (n == null) {
        return null;
      }
      taskId = n.getTaskId();
    }
    final List<TaskNode> list = taskTree.getPath(taskId, ancestorTaskId);
    if (CollectionUtils.isEmpty(list) == true) {
      return "";
    }
    final StringBuffer buf = new StringBuffer();
    int i = 0;
    for (final TaskNode node : list) {
      final TaskDO task = node.getTask();
      if (i++ > 0) {
        buf.append(" -> ");
      }
      buf.append(StringEscapeUtils.escapeXml(task.getTitle()));
    }
    if (outputType == OutputType.HTML) {
      return StringEscapeUtils.escapeHtml4(buf.toString());
    } else if (outputType == OutputType.XML) {
      return StringEscapeUtils.escapeXml(buf.toString());
    } else {
      return buf.toString();
    }
  }

  public static String getFormattedTaskStatus(final TaskStatus status)
  {
    if (status == TaskStatus.N) {
      // Show 'not opened' as blank field:
      return "";
    }
    final StringBuffer buf = new StringBuffer();
    buf.append("<span");
    HtmlHelper.attribute(buf, "class", "taskStatus_" + status.getKey());
    buf.append(">");
    buf.append(getI18nMessage("task.status." + status.getKey()));
    buf.append("</span>");
    return buf.toString();
  }
}
