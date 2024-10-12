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

import org.projectforge.business.fibu.AuftragsPositionVO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.timesheet.TimesheetDO
import java.math.BigDecimal

/**
 * Proxy of TaskTree for scripting.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class ScriptingTaskTree {
  private val __baseDao: TaskTree = TaskTree.instance

  val rootTaskNode: ScriptingTaskNode
    get() = ScriptingTaskNode(__baseDao.rootTaskNode)

  fun getPath(taskId: Long?, ancestorTaskId: Long?): List<ScriptingTaskNode>? {
    return convert(__baseDao.getPath(taskId, ancestorTaskId))
  }

  fun getPathToRoot(taskId: Long?): List<ScriptingTaskNode>? {
    return getPath(taskId, null)
  }

  fun getTaskNodeById(id: Long?): ScriptingTaskNode? {
    return convert(__baseDao.getTaskNodeById(id))
  }

  /**
   * Return true, if the given task or any ancestor task matches the given marker.
   * A task matches, if the title, shortDescription, reference or description of a task
   * contains the given marker.
   * @param taskId Check this task for matching and any ancestor task.
   * @param marker Marker must be part of task or any ancestor task. If null or blank, any task matches.
   */
  @JvmOverloads
  fun matchesMarker(taskId: Long?, marker: String?, ignoreCase: Boolean = true): Boolean {
    var taskNode: TaskNode? = __baseDao.getTaskNodeById(taskId) ?: return false
    if (marker.isNullOrBlank()) {
      return true
    }
    for (i in 0..1000) { // For loop as paranoia loop for avoiding circular references in task nodes.
      val task = taskNode?.task
      if (task != null
        && (task.title?.contains(marker, ignoreCase = ignoreCase) == true
            || task.shortDescription?.contains(marker, ignoreCase = ignoreCase) == true
            || task.reference?.contains(marker, ignoreCase = ignoreCase) == true
            || task.description?.contains(marker, ignoreCase = ignoreCase) == true)
      ) {
        return true
      }
      taskNode = taskNode?.parent
    }
    return false
  }

  class FilteredTimesheets {
    val filteredTimesheets = mutableListOf<TimesheetDO>()
    val matchingTasks = mutableSetOf<TaskDO>()
    val notMatchingTasks = mutableSetOf<TaskDO>()
  }

  /**
   * Filter the timesheets by matching tasks.
   * @see matchesMarker
   */
  fun filterTimesheets(timesheets: List<TimesheetDO>, marker: String?, ignoreCase: Boolean = true): FilteredTimesheets {
    val result = FilteredTimesheets()
    timesheets.forEach { timesheet ->
      val task = timesheet.task
      if (task != null) {
        if (matchesMarker(task.id, marker, ignoreCase)) {
          if (!result.matchingTasks.any { it.id == task.id }) {
            result.matchingTasks.add(task)
          }
          result.filteredTimesheets.add(timesheet)
        } else {
          if (!result.notMatchingTasks.any { it.id == task.id }) {
            result.notMatchingTasks.add(task)
          }
        }
      }
    }
    return result
  }

  /**
   * Gets a copy of the found projekt.
   * @param taskId
   * @return
   */
  fun getProjekt(taskId: Long?): ProjektDO? {
    val projekt = __baseDao.getProjekt(taskId) ?: return null
    val result = ProjektDO()
    result.copyValuesFrom(projekt)
    return result
  }

  fun getProjekt(node: ScriptingTaskNode): ProjektDO? {
    return getProjekt(node.id)
  }

  fun isRootNode(node: ScriptingTaskNode): Boolean {
    return __baseDao.isRootNode(node.__baseObject)
  }

  fun isRootNode(task: TaskDO): Boolean {
    return __baseDao.isRootNode(task)
  }

  fun hasOrderPositionsEntries(): Boolean {
    return __baseDao.hasOrderPositionsEntries()
  }

  fun hasOrderPositions(taskId: Long?, recursive: Boolean): Boolean {
    return __baseDao.hasOrderPositions(taskId, recursive)
  }

  fun hasOrderPositionsUpwards(taskId: Long?): Boolean {
    return __baseDao.hasOrderPositionsUpwards(taskId)
  }

  fun getOrderPositionsUpwards(taskId: Long?): Set<AuftragsPositionVO> {
    return __baseDao.getOrderPositionsUpwards(taskId)
  }

  fun getPersonDays(taskId: Long?): BigDecimal {
    taskId ?: return BigDecimal.ZERO
    return __baseDao.getPersonDays(taskId) ?: BigDecimal.ZERO
  }

  fun getPersonDays(node: ScriptingTaskNode?): BigDecimal {
    if (node?.__baseObject == null) {
      return BigDecimal.ZERO
    }
    return __baseDao.getPersonDays(node.__baseObject) ?: BigDecimal.ZERO
  }

  fun getOrderedPersonDaysSum(node: ScriptingTaskNode?): BigDecimal {
    if (node?.__baseObject == null) {
      return BigDecimal.ZERO
    }
    return __baseDao.getOrderedPersonDaysSum(node.__baseObject) ?: BigDecimal.ZERO
  }

  fun getPersonDaysNode(node: ScriptingTaskNode): TaskNode? {
    return __baseDao.getPersonDaysNode(node.__baseObject)
  }

  companion object {
    @JvmStatic
    fun convert(list: List<TaskNode?>?): List<ScriptingTaskNode>? {
      if (list == null) {
        return null
      }
      val result: MutableList<ScriptingTaskNode> = ArrayList(list.size)
      for (node in list) {
        result.add(ScriptingTaskNode(node))
      }
      return result
    }

    @JvmStatic
    fun convert(node: TaskNode?): ScriptingTaskNode? {
      return node?.let { ScriptingTaskNode(it) }
    }
  }
}
