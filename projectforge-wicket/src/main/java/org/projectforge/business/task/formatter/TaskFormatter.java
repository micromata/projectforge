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
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hibernate.Hibernate;
import org.projectforge.business.common.OutputType;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.common.task.TaskStatus;
import org.projectforge.framework.i18n.AbstractFormatter;
import org.projectforge.web.common.WicketHtmlHelper;

public class TaskFormatter extends AbstractFormatter
{
  /**
   * enableLinks = false, lineThroughDeletedTasks = true
   * 
   * @param taskId
   * @see #getTaskPath(Integer, boolean)
   */
  public static String getTaskPath(final RequestCycle requestCycle, final Integer taskId)
  {
    return getTaskPath(requestCycle, taskId, true);
  }

  /**
   * Gets the path of the task as String: ProjectForge -&gt; ... -&gt; database -&gt; backup strategy.
   * 
   * @param taskId
   * @param enableLinks If true, every task title is associated with a link to EditTask.
   */
  public static String getTaskPath(final RequestCycle requestCycle, final Integer taskId,
      final boolean lineThroughDeletedTasks)
  {
    return getTaskPath(requestCycle, taskId, null, lineThroughDeletedTasks);
  }

  /**
   * Gets the path of the task as String: ProjectForge -&gt; ... -&gt; database -&gt; backup strategy.
   * 
   * @param taskId
   * @param lineThroughDeletedTasks If true, deleted task will be visualized by line through.
   * @param ancestorTaskId If not null, the path will shown between taskId and ancestorTaskId. If mainTaskId is not an
   *          ancestor of taskId, the whole path will be shown.
   */
  public static String getTaskPath(final RequestCycle requestCycle, final Integer taskId, final Integer ancestorTaskId,
      final boolean lineThroughDeletedTasks)
  {
    final TaskTree taskTree = TaskTreeHelper.getTaskTree();
    if (taskId == null || taskTree.getTaskNodeById(taskId) == null) {
      return null;
    }
    final List<TaskNode> list = taskTree.getPath(taskId, ancestorTaskId);
    if (list.size() > 0) {
      final StringBuffer buf = new StringBuffer();
      int i = 0;
      for (final TaskNode node : list) {
        final TaskDO task = node.getTask();
        if (i++ > 0) {
          buf.append(" -&gt; ");
        }
        appendFormattedTask(requestCycle, buf, task, false, lineThroughDeletedTasks);
      }
      return buf.toString();
    } else if (ancestorTaskId != null) {
      return "";
    } else {
      return getI18nMessage("task.path.rootTask");
    }
  }

  /**
   * Formats path to root: "task1 -> task2 -> task3".
   * 
   * @param taskId
   * @param showCurrentTask if true also the given task by id will be added to the path, otherwise the path of the
   *          parent task will be shown.
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
   *          parent task will be shown.
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
      buf.append(task.getTitle());
    }
    if (outputType == OutputType.HTML) {
      return StringEscapeUtils.escapeHtml(buf.toString());
    } else if (outputType == OutputType.XML) {
      return StringEscapeUtils.escapeXml(buf.toString());
    } else {
      return buf.toString();
    }
  }

  /**
   * Writes the html formatted task to the given StringBuffer.
   * 
   * @param buf
   * @param task
   * @param enableLink If true, the task has a link to the EditTask.action.
   * @param showPathAsTooltip If true, an info icon with the whole task path as tooltip will be added.
   */
  public static void appendFormattedTask(final RequestCycle requestCycle, final StringBuffer buf, TaskDO task,
      final boolean showPathAsTooltip,
      final boolean lineThroughDeletedTask)
  {
    Validate.notNull(buf);
    Validate.notNull(task);
    if (showPathAsTooltip == true) {
      final String taskPath = getTaskPath(requestCycle, task.getId(), null, false);
      if (taskPath != null) {
        WicketHtmlHelper.appendImageTag(requestCycle, buf, HtmlHelper.getInfoImage(), taskPath);
      }
    }
    // if (enableLink == true) {
    // htmlHelper.appendAncorStartTag(locUrlBuilder, buf,
    // WicketUtils.getBookmarkablePageUrl(TaskEditPage.class, "id", String.valueOf(task.getId())));
    // }
    if (Hibernate.isInitialized(task) == false) {
      final TaskTree taskTree = TaskTreeHelper.getTaskTree();
      task = taskTree.getTaskById(task.getId());
    }
    if (task.isDeleted() == true) {
      if (lineThroughDeletedTask == true) {
        buf.append("<span");
        HtmlHelper.attribute(buf, "style", "text-decoration: line-through;");
        buf.append(">");
        buf.append(HtmlHelper.escapeXml(task.getTitle()));
        buf.append("</span>");
      } else {
        buf.append(HtmlHelper.escapeXml(task.getTitle())).append(" (");
        buf.append(getI18nMessage("task.deleted"));
        buf.append(")");
      }
    } else {
      buf.append(HtmlHelper.escapeXml(task.getTitle()));
    }
    // if (enableLink == true) {
    // htmlHelper.appendAncorEndTag(buf);
    // }
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
