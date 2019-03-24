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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.test.AbstractBase;
import org.projectforge.test.AbstractTestNGBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class Task2GanttTaskConverterTest extends AbstractTestNGBase
{
  @Autowired
  private GanttChartDao ganttChartDao;

  @Autowired
  private TaskDao taskDao;

  @Test
  public void testConvertingTaskTree()
  {
    logon(AbstractBase.TEST_ADMIN_USER);
    final String prefix = "task2Gantt";
    initTestDB.addTask(prefix, "root");
    initTestDB.addTask(prefix + "1", prefix);
    initTestDB.addTask(prefix + "1.1", prefix + "1");
    initTestDB.addTask(prefix + "1.1.1", prefix + "1.1");
    initTestDB.addTask(prefix + "1.1.2", prefix + "1.1");
    initTestDB.addTask(prefix + "1.2", prefix + "1");
    initTestDB.addTask(prefix + "1.2.1", prefix + "1.2");
    initTestDB.addTask(prefix + "1.2.2", prefix + "1.2");
    initTestDB.addTask(prefix + "2", prefix);
    initTestDB.addTask(prefix + "2.1", prefix + "2");
    initTestDB.addTask(prefix + "2.2", prefix + "2");
    initTestDB.addTask(prefix + "2.3", prefix + "2");
    final DayHolder day = new DayHolder();
    day.setDate(2010, Calendar.AUGUST, 16);
    taskDao.update(getTask(prefix + "2.1").setStartDate(day.getDate()).setDuration(BigDecimal.TEN));
    taskDao.update(getTask(prefix + "2.2").setGanttPredecessor(getTask(prefix + "2.1")).setDuration(BigDecimal.TEN));

    taskDao.update(getTask(prefix + "1.1.1").setGanttPredecessor(getTask(prefix + "2.1")).setDuration(BigDecimal.TEN));
    taskDao
        .update(getTask(prefix + "1.1.2").setGanttPredecessor(getTask(prefix + "1.1.1")).setDuration(BigDecimal.TEN));

    taskDao.update(getTask(prefix + "1.2.1").setGanttPredecessor(getTask(prefix + "2.2")).setDuration(BigDecimal.TEN));
    taskDao
        .update(getTask(prefix + "1.2.2").setGanttPredecessor(getTask(prefix + "1.2.1")).setDuration(BigDecimal.TEN));

    final GanttChartData ganttChartData = Task2GanttTaskConverter.convertToGanttObjectTree(taskDao.getTaskTree(),
        getTask(prefix + "1"));
    assertEquals("Two external objects (2.1 and 2.2) exptected.", 2, ganttChartData.getExternalObjects().size());
    assertExternalTasks(ganttChartData, prefix);
    final GanttChartDO ganttChartDO = new GanttChartDO().setTask(getTask(prefix + "1"));
    ganttChartDao.writeGanttObjects(ganttChartDO, ganttChartData.getRootObject());
    assertEquals("No output because there is no further information in the GanttObject tree.", "",
        ganttChartDO.getGanttObjectsAsXml());
    GanttChartData data = ganttChartDao.readGanttObjects(ganttChartDO);
    assertExternalTasks(data, prefix);
    final GanttTask external2_1 = ganttChartData.getExternalObject(getTask(prefix + "2.1").getId());
    // Change predecessors:
    findById(ganttChartData, prefix, "1.2")
        .setPredecessor(ganttChartData.ensureAndGetExternalGanttObject(getTask(prefix + "2.3")));
    findById(ganttChartData, prefix, "1.1.1").setPredecessor(null);
    findById(ganttChartData, prefix, "1.2.1").setPredecessor(external2_1);
    findById(ganttChartData, prefix, "1.1.2").setPredecessor(findById(ganttChartData, prefix, "1.2.2"));
    findById(ganttChartData, prefix, "1.2.2").setPredecessor(null);
    ganttChartDao.writeGanttObjects(ganttChartDO, ganttChartData.getRootObject());
    final String xml = transform(prefix, "<ganttObject id='{1}'>" //
        + "<children>"
        + "<ganttObject id='{1.1}'>"
        + "<children>"
        + "<ganttObject id='{1.1.1}' predecessor='null'/>" // Write null predecessor (modified).
        + "<ganttObject id='{1.1.2}'><predecessor id='{1.2.2}' predecessor='null' o-id='0'/></ganttObject>" // Write null predecessor
    // (modified).
        + "</children>"
        + "</ganttObject>"
        + "<ganttObject id='{1.2}'><predecessor id='{2.3}'/>"
        + "<children>"
        + "<ganttObject id='{1.2.1}'><predecessor id='{2.1}'/></ganttObject>" // Write external Gantt object only with id
        + "<ganttObject ref-id='0'/>"
        + "</children>"
        + "</ganttObject>"
        + "</children>"
        + "</ganttObject>");
    assertEquals("Gantt objects as xml.", xml, ganttChartDO.getGanttObjectsAsXml());
    data = ganttChartDao.readGanttObjects(ganttChartDO);
    ganttChartDao.writeGanttObjects(ganttChartDO, data.getRootObject());
    assertEquals("Gantt objects as xml.", xml, ganttChartDO.getGanttObjectsAsXml());
    assertNull("Predecessor was set to null.", findById(data, prefix, "1.2.2").getPredecessor());
    assertEquals("External predecessor expected.", prefix + "2.3",
        findById(data, prefix, "1.2").getPredecessor().getTitle());
  }

  private GanttTask findById(final GanttChartData ganttChartData, final String prefix, final String id)
  {
    return ganttChartData.findById(getTask(prefix + id).getId());
  }

  private void assertExternalTasks(final GanttChartData ganttChartData, final String prefix)
  {
    GanttTask externalGanttTask = ganttChartData.getExternalObject(getTaskId(prefix + "2.1"));
    assertNull("Predecessor should be null.", externalGanttTask.getPredecessor());
    assertDate("Start date unmodified.", 2010, Calendar.AUGUST, 16, externalGanttTask.getStartDate());
    assertDate("End date should have been calculated and set.", 2010, Calendar.AUGUST, 30,
        externalGanttTask.getEndDate());
    externalGanttTask = ganttChartData.getExternalObject(getTaskId(prefix + "2.2"));
    assertNull("Predecessor should be null.", externalGanttTask.getPredecessor());
    assertDate("Start date should have been calculated and set.", 2010, Calendar.AUGUST, 30,
        externalGanttTask.getStartDate());
    assertDate("End date should have been calculated and set.", 2010, Calendar.SEPTEMBER, 13,
        externalGanttTask.getEndDate());
  }

  private void assertDate(final String message, final int year, final int month, final int dayOfMonth, final Date date)
  {
    final DayHolder dh = new DayHolder(date);
    assertEquals(message, year, dh.getYear());
    assertEquals(message, month, dh.getMonth());
    assertEquals(message, dayOfMonth, dh.getDayOfMonth());
  }

  private Integer getTaskId(final String taskName)
  {
    final TaskDO task = getTask(taskName);
    if (task != null) {
      return task.getId();
    }
    return null;
  }

  private String transform(final String prefix, final String str)
  {
    final String text = str.replace('\'', '"');
    final Pattern p = Pattern.compile("\\{([0-9\\.]*)\\}", Pattern.MULTILINE);
    final StringBuffer buf = new StringBuffer();
    final Matcher m = p.matcher(text);
    while (m.find()) {
      if (m.group(1) != null) {
        final TaskDO task = getTask(prefix + m.group(1));
        if (task != null) {
          m.appendReplacement(buf, String.valueOf(task.getId()));
        } else {
          m.appendReplacement(buf, "*** task " + m.group(1) + " not-found***");
        }
      }
    }
    m.appendTail(buf);
    return buf.toString();
  }

}
