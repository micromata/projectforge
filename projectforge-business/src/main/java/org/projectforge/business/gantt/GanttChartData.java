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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.projectforge.business.task.TaskDO;

public class GanttChartData implements Serializable
{
  private static final long serialVersionUID = 726187772438594384L;

  private GanttTask rootObject;

  private Collection<GanttTask> externalObjects;

  public void setRootObject(GanttTask rootObject)
  {
    this.rootObject = rootObject;
  }

  public GanttTask getRootObject()
  {
    return rootObject;
  }
  
  public GanttTask findById(final Serializable id) {
    if (rootObject == null) {
      return null;
    }
    return rootObject.findById(id);
  }

  /**
   * The returned object represents a task of the ProjectForge's task tree which should be outside from the Gantt object tree. External
   * tasks are stored as external tasks. The GanttTask has no predecessor. The start date and end date will be set to the given or if not
   * given to the calculated values.<br/>
   * Default is false. <br/>
   */
  public GanttTask ensureAndGetExternalGanttObject(final TaskDO task)
  {
    if (task == null || task.getId() == null) {
      return null;
    }
    if (externalObjects == null) {
      externalObjects = new ArrayList<GanttTask>();
    }
    GanttTask ganttTask = getExternalObject(task.getId());
    if (ganttTask == null) {
      ganttTask = getExternalGanttObject(task);
      externalObjects.add(ganttTask);
    }
    return ganttTask;
  }

  private GanttTask getExternalGanttObject(final TaskDO task)
  {
    final GanttTask ganttObject = convertToGanttObject(task);
    if (ganttObject.getStartDate() == null) {
      ganttObject.setStartDate(GanttUtils.getCalculatedStartDate(ganttObject));
    }
    if (ganttObject.getEndDate() == null) {
      ganttObject.setEndDate(GanttUtils.getCalculatedEndDate(ganttObject));
    }
    // Remove any existing predecessor chain:
    ganttObject.setPredecessor(null);
    return ganttObject;
  }

  private GanttTask convertToGanttObject(final TaskDO task)
  {
    final GanttTask ganttObject = Task2GanttTaskConverter.convertToGanttObject(task);
    final TaskDO predecessorTask = task.getGanttPredecessor();
    if (predecessorTask != null) {
      ganttObject.setPredecessor(convertToGanttObject(predecessorTask));
    }
    return ganttObject;
  }

  public GanttTask getExternalObject(final Serializable id)
  {
    if (id == null || externalObjects == null) {
      return null;
    }
    for (final GanttTask task : externalObjects) {
      if (id.equals(task.getId()) == true) {
        return task;
      }
    }
    return null;
  }

  /**
   * For test cases.
   */
  Collection<GanttTask> getExternalObjects()
  {
    return externalObjects;
  }
}
