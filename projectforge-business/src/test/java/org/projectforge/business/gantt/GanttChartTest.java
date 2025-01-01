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

import org.junit.jupiter.api.Test;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskTree;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Month;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public class GanttChartTest extends AbstractTestBase {
    @Autowired
    private GanttChartDao ganttChartDao;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private TaskTree taskTree;

    @Test
    public void testReadWriteGanttObjects() {
        logon(AbstractTestBase.TEST_ADMIN_USER);
        persistenceService.runInTransaction(context -> {
            final String prefix = "GantChartTest";
            final TaskDO rootTask = initTestDB.addTask(prefix, "root");
            final PFDateTime dt = PFDateTime.withDate(2010, Month.AUGUST, 3);

            TaskDO task = initTestDB.addTask(prefix + "1", prefix);
            task.setStartDate(dt.getLocalDate());
            task.setDuration(BigDecimal.TEN);

            taskDao.update(task);
            initTestDB.addTask(prefix + "1.1", prefix + "1");
            task = initTestDB.addTask(prefix + "2", prefix);
            task.setGanttPredecessor(getTask(prefix + "1"));
            task.setDuration(BigDecimal.ONE);
            taskDao.update(task);

            task = initTestDB.addTask(prefix + "3", prefix);
            task.setGanttPredecessor(getTask(prefix + "2"));
            task.setGanttPredecessorOffset(10);
            task.setDuration(BigDecimal.TEN);
            taskDao.update(task);
            final GanttChartData data = Task2GanttTaskConverter.convertToGanttObjectTree(taskTree, rootTask);
            final GanttTask rootObject = data.getRootObject();
            final GanttChartDO ganttChartDO = new GanttChartDO();
            ganttChartDO.setTask(rootTask);
            ganttChartDao.writeGanttObjects(ganttChartDO, rootObject);
            assertEquals("", ganttChartDO.getGanttObjectsAsXml(), "No output because there is no further information in the GanttObject tree.");
            findById(rootObject, getTask(prefix + "2").getId()).setPredecessorOffset(5).setDuration(new BigDecimal("12"));
            findById(rootObject, getTask(prefix + "1.1").getId()).setDuration(new BigDecimal("2"));
            ganttChartDao.writeGanttObjects(ganttChartDO, rootObject);
            String xml = transform(prefix, "<ganttObject id='{}'>"
                    + "<children>"
                    + "<ganttObject id='{1}'>"
                    + "<children>"
                    + "<ganttObject id='{1.1}' duration='2'/>"
                    + "</children>"
                    + "</ganttObject>"
                    + "<ganttObject id='{2}' predecessorOffset='5' duration='12'/></children></ganttObject>");
            assertEquals(xml, ganttChartDO.getGanttObjectsAsXml(), "3 has no further information.");

            GanttTask ganttObject = ganttChartDao.readGanttObjects(ganttChartDO).getRootObject();
            ganttChartDao.writeGanttObjects(ganttChartDO, ganttObject);
            assertEquals(xml, ganttChartDO.getGanttObjectsAsXml());
            BigDecimal duration = findById(ganttObject, getTask(prefix + "1").getId()).getDuration();
            assertEquals(0, BigDecimal.TEN.compareTo(duration), "duration " + duration + "!=10!");
            assertEquals(dt.getLocalDate(), findById(ganttObject, getTask(prefix + "1").getId()).getStartDate(), "startDate");

            initTestDB.addTask(prefix + "II", "root");

            task = getTask(prefix + "1.1");
            task.setParentTask(getTask(prefix));

            taskDao.update(task); // One level higher

            task = getTask(prefix + "2");
            task.setParentTask(getTask(prefix + "II"));

            taskDao.update(task); // Moved anywhere.

            task = getTask(prefix + "3");
            task.setParentTask(getTask(prefix + "II"));

            taskDao.update(task); // Moved anywhere.

            ganttObject = ganttChartDao.readGanttObjects(ganttChartDO).getRootObject();
            ganttChartDao.writeGanttObjects(ganttChartDO, ganttObject);
            assertEquals(transform(prefix, "<ganttObject id='{}'>"
                            + "<children>"
                            + "<ganttObject id='{1.1}' duration='2'/>"
                            + "</children>"
                            + "</ganttObject>"), ganttChartDO.getGanttObjectsAsXml(),
                    "1 has no further information, 2 and 3 are moved anywhere.");
            findById(ganttObject, getTask(prefix + "1").getId()).setStartDate(null);
            ganttChartDao.writeGanttObjects(ganttChartDO, ganttObject);
            ganttObject = ganttChartDao.readGanttObjects(ganttChartDO).getRootObject();
            assertNull(findById(ganttObject, getTask(prefix + "1").getId())
                            .getStartDate(),
                    "Start date should be stored as null (start date of task is set).");
            findById(ganttObject, getTask(prefix + "1").getId()).addChild(new GanttTaskImpl(-1L).setTitle("Child of 1"));
            findById(ganttObject, getTask(prefix + "1.1").getId()).addChild(
                    new GanttTaskImpl(-2L).setTitle("Child of 1.1").addChild(new GanttTaskImpl(-3L).setTitle("Grand child of 1.1")));
            ganttChartDao.writeGanttObjects(ganttChartDO, ganttObject);
            xml = transform(prefix, "<ganttObject id='{}'>"
                    + "<children>"
                    + "<ganttObject id='{1}' startDate='null'>"
                    + "<children>"
                    + "<ganttObject id='-1'><title>Child of 1</title></ganttObject>"
                    + "</children>"
                    + "</ganttObject>"
                    + "<ganttObject id='{1.1}' duration='2'>"
                    + "<children>"
                    + "<ganttObject id='-2'><title>Child of 1.1</title>"
                    + "<children>"
                    + "<ganttObject id='-3'><title>Grand child of 1.1</title></ganttObject>"
                    + "</children>"
                    + "</ganttObject>"
                    + "</children>"
                    + "</ganttObject>"
                    + "</children>"
                    + "</ganttObject>");
            assertEquals(xml, ganttChartDO.getGanttObjectsAsXml(), "Test with activities not related to tasks.");
            ganttObject = ganttChartDao.readGanttObjects(ganttChartDO).getRootObject();
            ganttChartDao.writeGanttObjects(ganttChartDO, ganttObject);
            assertEquals(xml, ganttChartDO.getGanttObjectsAsXml(), "Read-write-cycle (identity check).");
            return null;
        });
    }

    @Test
    public void testIgnoringOfNumberFields() {
        logon(AbstractTestBase.TEST_ADMIN_USER);
        persistenceService.runInTransaction(context -> {
            final String prefix = "GanttTest3";
            final TaskDO rootTask = initTestDB.addTask(prefix, "root");
            final Long id1 = addTask(prefix + "1", null, null);
            final Long id2 = addTask(prefix + "2", null, null);
            final Long id3 = addTask(prefix + "3", BigDecimal.TEN, 10);
            final Long id4 = addTask(prefix + "4", BigDecimal.TEN, 10);
            final Long id5 = addTask(prefix + "5", BigDecimal.TEN, 10);
            // final Long id3 = task.getId();
            final GanttChartData data = Task2GanttTaskConverter.convertToGanttObjectTree(taskTree, rootTask);
            final GanttTask rootObject = data.getRootObject();
            final GanttChartDO ganttChartDO = new GanttChartDO();
            ganttChartDO.setTask(rootTask);
            findById(rootObject, id1).setDuration(new BigDecimal("10.000")).setProgress(10); // Modified.
            findById(rootObject, id3).setDuration(new BigDecimal("2")).setProgress(2); // Modified.
            findById(rootObject, id4).setDuration(new BigDecimal("10.000")).setProgress(10); // Unmodified
            findById(rootObject, id5).setDuration(null).setProgress(null); // Modified
            ganttChartDao.writeGanttObjects(ganttChartDO, rootObject);
            final String xml = transform(prefix, "<ganttObject id='{}'>"
                    + "<children>"
                    + "<ganttObject id='{1}' duration='10.000' progress='10'/>"
                    + "<ganttObject id='{3}' duration='2' progress='2'/>"
                    + "<ganttObject id='{5}' duration='null' progress='null'/>"
                    + "</children>"
                    + "</ganttObject>");
            assertEquals(xml, ganttChartDO.getGanttObjectsAsXml(), "check xml output.");
            GanttTask ganttObject = ganttChartDao.readGanttObjects(ganttChartDO).getRootObject();
            assertDurationAndProgress(ganttObject, id1, BigDecimal.TEN, 10);
            assertDurationAndProgress(ganttObject, id2, null, null);
            assertDurationAndProgress(ganttObject, id3, new BigDecimal("2"), 2);
            assertDurationAndProgress(ganttObject, id4, BigDecimal.TEN, 10);
            assertDurationAndProgress(ganttObject, id5, null, null);
            return null;
        });
    }

    private void assertDurationAndProgress(final GanttTask root, final Long id, final BigDecimal expectedDuration,
                                           final Integer expectedProgress) {
        final GanttTask task = root.findById(id);
        assertTrue(NumberHelper.isEqual(expectedDuration, task.getDuration()));
        if (expectedProgress == null) {
            assertNull(task.getProgress());
        } else {
            assertEquals(expectedProgress, task.getProgress());
        }
    }

    private Long addTask(final String name, final BigDecimal duration, final Integer progress) {
        final TaskDO task = initTestDB.addTask(name, "GanttTest3");
        task.setDuration(duration);
        task.setProgress(progress);
        taskDao.update(task);
        return task.getId();
    }

    private GanttTaskImpl findById(final GanttTask ganttObject, final Serializable id) {
        return (GanttTaskImpl) ganttObject.findById(id);
    }

    private String transform(final String prefix, final String str) {
        final String text = str.replace('\'', '"');
        final Pattern p = Pattern.compile("\\{([0-9\\.]*)\\}", Pattern.MULTILINE);
        final StringBuilder buf = new StringBuilder();
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
