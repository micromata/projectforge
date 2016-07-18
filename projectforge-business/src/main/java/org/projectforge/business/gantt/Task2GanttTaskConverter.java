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

package org.projectforge.business.gantt;

import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskNode;
import org.projectforge.business.task.TaskTree;

public class Task2GanttTaskConverter
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Task2GanttTaskConverter.class);

  /**
   * Maximum depth of predecessors for avoiding circular predecessor settings..
   */
  public static final GanttChartData convertToGanttObjectTree(final TaskTree taskTree, final TaskDO rootTask)
  {
    final GanttChartData ganttChartData = new GanttChartData();
    convertToGanttObject(ganttChartData, taskTree, rootTask);
    processPredecessor(ganttChartData, taskTree, ganttChartData.getRootObject());
    return ganttChartData;
  }

  /**
   * Creates a new GanttTask and copies all fields from the given task (excluding the predecessor and any children).
   * @param task
   * @return
   */
  public static final GanttTask convertToGanttObject(final TaskDO task)
  {
    final GanttTaskImpl ganttObject = new GanttTaskImpl();
    ganttObject.setId(task.getId());
    ganttObject.setPredecessorOffset(task.getGanttPredecessorOffset());
    ganttObject.setRelationType(task.getGanttRelationType());
    ganttObject.setDuration(task.getDuration());
    ganttObject.setStartDate(task.getStartDate());
    ganttObject.setEndDate(task.getEndDate());
    ganttObject.setProgress(task.getProgress());
    ganttObject.setType(task.getGanttObjectType());
    ganttObject.setDescription(task.getDescription());
    ganttObject.setTitle(task.getTitle());
    return ganttObject;
  }

  /**
   * Creates a new GanttTask and copies all fields from the given task (excluding the predecessor and any children).
   * @param task
   * @return
   */
  public static final TaskDO convertToTask(final GanttTask ganttObject)
  {
    final TaskDO task = new TaskDO();
    // Do not copy the id! The id is given by the data base.
    task.setGanttPredecessorOffset(ganttObject.getPredecessorOffset());
    task.setGanttRelationType(ganttObject.getRelationType());
    task.setDuration(ganttObject.getDuration());
    task.setStartDate(ganttObject.getStartDate());
    task.setEndDate(ganttObject.getEndDate());
    task.setProgress(ganttObject.getProgress());
    task.setGanttObjectType(ganttObject.getType());
    task.setDescription(ganttObject.getDescription());
    task.setTitle(ganttObject.getTitle());
    return task;
  }

  private static final GanttTask convertToGanttObject(final GanttChartData ganttChartData, final TaskTree taskTree, final TaskDO task)
  {
    if (task == null) {
      log.warn("Oups, task shouldn't be null.");
      return null;
    }
    final GanttTask ganttObject = convertToGanttObject(task);
    if (ganttChartData.getRootObject() == null) {
      ganttChartData.setRootObject(ganttObject);
    }
    final TaskNode taskNode = taskTree.getTaskNodeById(task.getId());
    if (taskNode.hasChilds() == true) {
      for (final TaskNode childNode : taskNode.getChilds()) {
        if (childNode.isDeleted() == false) {
          ganttObject.addChild(convertToGanttObject(ganttChartData, taskTree, childNode.getTask()));
        }
      }
    }
    return ganttObject;
  }

  private static final void processPredecessor(final GanttChartData ganttChartData, final TaskTree taskTree, final GanttTask ganttTask)
  {
    if (ganttTask == null) {
      log.warn("Oups, Gantt task shouldn't be null.");
      return;
    }
    if (ganttTask.equals(ganttChartData.getRootObject()) == false) {
      final TaskDO task = taskTree.getTaskById((Integer) ganttTask.getId());
      final Integer predecessorId = task.getGanttPredecessorId();
      if (predecessorId != null) {
        GanttTask predecessor = ganttChartData.getRootObject().findById(predecessorId);
        if (predecessor == null) {
          // External task (outside the given Gantt task tree):
          final TaskDO predecessortTask = taskTree.getTaskById(predecessorId);
          if (predecessortTask != null) {
            predecessor = ganttChartData.ensureAndGetExternalGanttObject(predecessortTask);
          } else {
            log.warn("Oups, task with id '" + predecessorId + "' not found.");
          }
        }
        if (predecessor != null) {
          ganttTask.setPredecessor(predecessor);
        }
      }
    }
    if (ganttTask.getChildren() != null) {
      for (final GanttTask child : ganttTask.getChildren()) {
        processPredecessor(ganttChartData, taskTree, child);
      }
    }
  }
}
