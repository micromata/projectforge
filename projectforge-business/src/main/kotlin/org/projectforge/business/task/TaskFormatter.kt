/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.task

import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.text.StringEscapeUtils
import org.projectforge.business.common.OutputType
import org.projectforge.business.utils.HtmlHelper
import org.projectforge.common.task.TaskStatus
import org.projectforge.framework.i18n.AbstractFormatter

open class TaskFormatter : AbstractFormatter() {
  companion object {
    /**
     * Formats path to root: "task1 -> task2 -> task3".
     *
     * @param taskId
     * @param showCurrentTask if true also the given task by id will be added to the path, otherwise the path of the
     * parent task will be shown.
     */
    @JvmStatic
    @JvmOverloads
    fun getTaskPath(
      taskId: Long?,
      showCurrentTask: Boolean,
      outputType: OutputType,
      abreviationLength: Int? = null
    ): String? {
      return getTaskPath(taskId, null, showCurrentTask, outputType, abreviationLength)
    }

    /**
     * Formats path to ancestor task if given or to root: "task1 -> task2 -> task3".
     *
     * @param taskId
     * @param ancestorTaskId
     * @param showCurrentTask if true also the given task by id will be added to the path, otherwise the path of the
     * parent task will be shown.
     * @param abreviationLength The parent path will be shorted for achieving the given length. But the length isn't garantueed.
     * Example: "Micromata - Business Unit Green - ProjectForge - Development - Release 1.3" -> "M..BUG..P..Development..Release 1.3"
     */
    @JvmStatic
    @JvmOverloads
    fun getTaskPath(
      taskId: Long?, ancestorTaskId: Long?, showCurrentTask: Boolean,
      outputType: OutputType,
      abreviationLength: Int? = null,
    ): String? {
      var currentTaskId = taskId ?: return null
      val taskTree = TaskTree.getInstance()
      var n: TaskNode? = taskTree.getTaskNodeById(currentTaskId) ?: return null
      if (!showCurrentTask) {
        n = n?.parent
        if (n == null) {
          return null
        }
        currentTaskId = n.taskId
      }
      val list = taskTree.getPath(currentTaskId, ancestorTaskId)
      if (CollectionUtils.isEmpty(list)) {
        return ""
      }
      val path = mutableListOf<String?>()
      list.forEach { node ->
        path.add(node.task?.title)
      }
      val text = if (abreviationLength != null) {
        asShortForm(abreviationLength, path.toTypedArray())
      } else {
        path.joinToString(" -> ")
      }
      return if (outputType == OutputType.HTML) {
        StringEscapeUtils.escapeHtml4(text)
      } else if (outputType == OutputType.XML) {
        StringEscapeUtils.escapeXml11(text)
      } else {
        text
      }
    }

    @JvmStatic
    fun getFormattedTaskStatus(status: TaskStatus): String {
      if (status == TaskStatus.N) {
        // Show 'not opened' as blank field:
        return ""
      }
      val buf = StringBuffer()
      buf.append("<span")
      HtmlHelper.attribute(buf, "class", "taskStatus_" + status.key)
      buf.append(">")
      buf.append(getI18nMessage("task.status." + status.key))
      buf.append("</span>")
      return buf.toString()
    }

    internal fun asShortForm(length: Int, str: Array<String?>?): String {
      str ?: return ""
      val result = str.copyOf()
      for (i in 0 until str.size - 1) {
        if (countChars(result) <= length) {
          break
        } else {
          result[i] = asShortForm(str[i])
        }
      }
      return result.joinToString("..") { it ?: "?" }
    }

    private fun countChars(sa: Array<String?>): Int {
      var counter = -2
      sa.forEach {
        counter += it?.length ?: 1
        counter += 2
      }
      return counter
    }

    internal fun asShortForm(str: String?): String {
      str ?: return "?"
      val sb = StringBuilder()
      var wordStarted = false
      str.forEach { ch ->
        if (!ch.isLetterOrDigit()) {
          wordStarted = false
        } else if (wordStarted) {
          // Do nothing (wait for end of word).
        } else {
          sb.append(ch)
          wordStarted = true
        }
      }
      return sb.toString()
    }
  }
}
