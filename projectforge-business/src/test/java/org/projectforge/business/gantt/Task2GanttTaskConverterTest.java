/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.jupiter.api.Test;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskTree;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Month;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class Task2GanttTaskConverterTest extends AbstractTestBase {
  @Autowired
  private GanttChartDao ganttChartDao;

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private TaskTree taskTree;

  @Test
  public void testConvertingTaskTree() {
    logon(AbstractTestBase.TEST_ADMIN_USER);
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
    final PFDateTime day = PFDateTime.withDate(2010, Month.AUGUST, 16);

    TaskDO task = getTask(prefix + "2.1");
    task.setStartDate(day.getLocalDate());
    task.setDuration(BigDecimal.TEN);
    taskDao.update(task);

    task = getTask(prefix + "2.2");
    task.setGanttPredecessor(getTask(prefix + "2.1"));
    task.setDuration(BigDecimal.TEN);
    taskDao.update(task);

    task = getTask(prefix + "1.1.1");
    task.setGanttPredecessor(getTask(prefix + "2.1"));
    task.setDuration(BigDecimal.TEN);
    taskDao.update(task);

    task = getTask(prefix + "1.1.2");
    task.setGanttPredecessor(getTask(prefix + "1.1.1"));
    task.setDuration(BigDecimal.TEN);
    taskDao.update(task);

    task = getTask(prefix + "1.2.1");
    task.setGanttPredecessor(getTask(prefix + "2.2"));
    task.setDuration(BigDecimal.TEN);
    taskDao.update(task);

    task = getTask(prefix + "1.2.2");
    task.setGanttPredecessor(getTask(prefix + "1.2.1"));
    task.setDuration(BigDecimal.TEN);
    taskDao.update(task);

    final GanttChartData ganttChartData = Task2GanttTaskConverter.convertToGanttObjectTree(taskTree,
            getTask(prefix + "1"));
    assertEquals(2, ganttChartData.getExternalObjects().size(),
            "Two external objects (2.1 and 2.2) exptected.");
    assertExternalTasks(ganttChartData, prefix);
    final GanttChartDO ganttChartDO = new GanttChartDO();
    ganttChartDO.setTask(getTask(prefix + "1"));
    ganttChartDao.writeGanttObjects(ganttChartDO, ganttChartData.getRootObject());
    assertEquals("", ganttChartDO.getGanttObjectsAsXml(), "No output because there is no further information in the GanttObject tree.");
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
            + "<ganttObject id='{1.2.1}'><predecessor id='{2.1}'><endDate>2010-08-30</endDate></predecessor></ganttObject>" // Write external Gantt object only with id
            + "<ganttObject ref-id='0'/>"
            + "</children>"
            + "</ganttObject>"
            + "</children>"
            + "</ganttObject>");
    assertEquals(xml, ganttChartDO.getGanttObjectsAsXml(), "Gantt objects as xml.");
    data = ganttChartDao.readGanttObjects(ganttChartDO);
    ganttChartDao.writeGanttObjects(ganttChartDO, data.getRootObject());
    assertEquals(xml, ganttChartDO.getGanttObjectsAsXml(), "Gantt objects as xml.");
    assertNull(findById(data, prefix, "1.2.2").getPredecessor(), "Predecessor was set to null.");
    assertEquals(prefix + "2.3", findById(data, prefix, "1.2").getPredecessor().getTitle(),
            "External predecessor expected.");
  }

  private GanttTask findById(final GanttChartData ganttChartData, final String prefix, final String id) {
    return ganttChartData.findById(getTask(prefix + id).getId());
  }

  private void assertExternalTasks(final GanttChartData ganttChartData, final String prefix) {
    GanttTask externalGanttTask = ganttChartData.getExternalObject(getTaskId(prefix + "2.1"));
    assertNull(externalGanttTask.getPredecessor(), "Predecessor should be null.");
    assertLocalDate(externalGanttTask.getStartDate(), 2010, Month.AUGUST, 16);
    assertLocalDate(externalGanttTask.getEndDate(),2010, Month.AUGUST, 30);
    externalGanttTask = ganttChartData.getExternalObject(getTaskId(prefix + "2.2"));
    assertNull(externalGanttTask.getPredecessor(), "Predecessor should be null.");
    assertLocalDate(externalGanttTask.getStartDate(), 2010, Month.AUGUST, 30);
    assertLocalDate(externalGanttTask.getEndDate(),2010, Month.SEPTEMBER, 13);
  }

  private void assertDate(final String message, final int year, final int month, final int dayOfMonth, final Date date) {
    final PFDateTime dt = PFDateTime.from(date); // not null
    assertEquals(year, dt.getYear(), message);
    assertEquals(month, dt.getMonthValue(), message);
    assertEquals(dayOfMonth, dt.getDayOfMonth(), message);
  }

  private Integer getTaskId(final String taskName) {
    final TaskDO task = getTask(taskName);
    if (task != null) {
      return task.getId();
    }
    return null;
  }

  private String transform(final String prefix, final String str) {
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
