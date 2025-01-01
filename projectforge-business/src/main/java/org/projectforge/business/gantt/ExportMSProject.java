/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import net.sf.mpxj.*;
import net.sf.mpxj.mpx.MPXWriter;
import net.sf.mpxj.mspdi.MSPDIWriter;
import net.sf.mpxj.writer.ProjectWriter;
import org.projectforge.framework.calendar.Holidays;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDayUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Uses the implementation of http://mpxj.sourceforge.net/, which is distributed under the terms of the GNU LGPL.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ExportMSProject {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExportMSProject.class);

  public static byte[] exportXml(final GanttChart ganttChart) {
    return export(new MSPDIWriter(), ganttChart);
  }

  public static byte[] exportMpx(final GanttChart ganttChart) {
    return export(new MPXWriter(), ganttChart);
  }

  private static byte[] export(final ProjectWriter result, final GanttChart ganttChart) {
    final ProjectFile file = new ProjectFile();

    //
    // Configure the file to automatically generate identifiers for tasks.
    //
    file.setAutoTaskID(true);
    file.setAutoTaskUniqueID(true);

    //
    // Configure the file to automatically generate identifiers for resources.
    //
    file.setAutoResourceID(true);
    file.setAutoResourceUniqueID(true);

    //
    // Configure the file to automatically generate outline levels
    // and outline numbers.
    //
    file.setAutoOutlineLevel(true);
    file.setAutoOutlineNumber(true);

    //
    // Configure the file to automatically generate WBS labels
    //
    file.setAutoWBS(true);

    //
    // Configure the file to automatically generate identifiers for calendars
    // (not strictly necessary here, but required if generating MSPDI files)
    //
    file.setAutoCalendarUniqueID(true);

    //
    // Retrieve the project header and set the start date. Note Microsoft
    // Project appears to reset all task dates relative to this date, so this
    // date must match the start date of the earliest task for you to see
    // the expected results. If this value is not set, it will default to
    // today's date.
    //
    ganttChart.recalculate();
    final ProjectHeader header = file.getProjectHeader();
    header.setStartDate(PFDayUtils.convertToUtilDate(ganttChart.getCalculatedStartDate()));

    //
    // Add a default calendar called "Standard"
    //
    final ProjectCalendar calendar = file.addDefaultBaseCalendar();
    calendar.setWorkingDay(Day.SATURDAY, false);
    calendar.setWorkingDay(Day.SUNDAY, false);
    PFDateTime dt = PFDateTime.from(ganttChart.getCalculatedStartDate()); // not null
    for (int i = 0; i < 3000; i++) { // Endless loop protection (paranoia)
      dt = dt.plusDays(1);
      Holidays holidays = Holidays.getInstance();
      if (!holidays.isWorkingDay(dt.getDateTime()) && holidays.isHoliday(dt) && !dt.isWeekend()) {
        // Add this holiday to the calendar:
        final Date date = dt.getSqlDate();
        calendar.addCalendarException(date, date);
        if (log.isDebugEnabled()) {
          log.debug("Add holiday: " + date);
        }
      }
      PFDateTime dtEnd = PFDateTime.from(ganttChart.getCalculatedEndDate()); // not null
      if (!dt.isBefore(dtEnd)) {
        break;
      }
    }

    final List<GanttTask> children = ganttChart.getRootNode().getChildren();
    if (children != null) {
      final Map<Serializable, Task> taskMap = new HashMap<>();
      for (final GanttTask child : children) {
        addTask(file, taskMap, null, child);
      }
      for (final GanttTask child : children) {
        setPredecessors(taskMap, child);
      }
    }

    //
    // Write the file
    //
    final ByteArrayOutputStream ba = new ByteArrayOutputStream();
    try {
      result.write(file, ba);
    } catch (final IOException ex) {
      log.error("Exception encountered " + ex, ex);
    }
    return ba.toByteArray();
  }

  private static void addTask(final ProjectFile file, final Map<Serializable, Task> taskMap, final Task parentTask,
                              final GanttTask ganttTask) {
    final Task task;
    if (parentTask == null) {
      task = file.addTask();
    } else {
      task = parentTask.addTask();
    }
    taskMap.put(ganttTask.getId(), task);
    task.setName(ganttTask.getTitle());
    if (ganttTask.getStartDate() != null) {
      task.setStart(PFDayUtils.convertToUtilDate(ganttTask.getStartDate()));
    }
    if (ganttTask.getEndDate() != null) {
      task.setFinish(PFDayUtils.convertToUtilDate(ganttTask.getEndDate()));
    }
    final BigDecimal duration = ganttTask.getDuration();
    final double value;
    if (duration == null) {
      value = 0.0;
    } else {
      value = duration.doubleValue();
    }
    task.setDuration(Duration.getInstance(value, TimeUnit.DAYS));
    if (ganttTask.getProgress() != null) {
      task.setPercentageComplete(ganttTask.getProgress());
    }
    // task2.setActualStart(df.parse("01/01/2003"));
    // milestone1.setDuration(Duration.getInstance(0, TimeUnit.DAYS));

    final List<GanttTask> children = ganttTask.getChildren();
    if (children == null) {
      return;
    }
    for (final GanttTask child : children) {
      addTask(file, taskMap, task, child);
    }
  }

  private static void setPredecessors(final Map<Serializable, Task> taskMap, final GanttTask ganttTask) {
    if (ganttTask.getPredecessor() != null) {
      final Task task = taskMap.get(ganttTask.getId());
      final Task predecessor = taskMap.get(ganttTask.getPredecessorId());
      if (task == null) {
        log.error("Oups, task with id '" + ganttTask.getId() + "' not found.");
      } else if (predecessor == null) {
        log.error("Oups, predecessor task with id '" + ganttTask.getPredecessorId() + "' not found.");
      } else {
        final Integer predecessorOffset = ganttTask.getPredecessorOffset();
        final int value;
        if (predecessorOffset == null) {
          value = 0;
        } else {
          value = predecessorOffset;
        }
        task.addPredecessor(predecessor, getRelationType(ganttTask.getRelationType()), Duration.getInstance(value, TimeUnit.DAYS));
      }
    }
    final List<GanttTask> children = ganttTask.getChildren();
    if (children == null) {
      return;
    }
    for (final GanttTask child : children) {
      setPredecessors(taskMap, child);
    }
  }

  private static RelationType getRelationType(final GanttRelationType type) {
    if (type == null || type == GanttRelationType.FINISH_START) {
      return RelationType.FINISH_START;
    } else if (type == GanttRelationType.FINISH_FINISH) {
      return RelationType.FINISH_FINISH;
    } else if (type == GanttRelationType.START_START) {
      return RelationType.START_START;
    } else if (type == GanttRelationType.START_FINISH) {
      return RelationType.START_FINISH;
    } else {
      log.error("Unsupported relation type: " + type);
      return RelationType.FINISH_START;
    }
  }
}
