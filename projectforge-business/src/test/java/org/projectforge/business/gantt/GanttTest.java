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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.configuration.ConfigXmlTest;
import org.projectforge.framework.renderer.BatikImageRenderer;
import org.projectforge.framework.renderer.ImageFormat;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.test.WorkFileHelper;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GanttTest
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GanttTest.class);

  @BeforeAll
  public static void setUp()
  {
    // Needed if this tests runs before the ConfigurationTest.
    ConfigXmlTest.createTestConfiguration();
  }

  @Test
  public void createImage() throws IOException
  {
    final GanttTask root = createTestChart();
    final GanttChartStyle style = new GanttChartStyle();
    final GanttChartSettings settings = new GanttChartSettings();
    final GanttChart diagram = new GanttChart(root, style, settings, "test-chart");
    writeFile("ganttTest.jpg", BatikImageRenderer.getByteArray(diagram.create(), 800, ImageFormat.JPEG));
    writeFile("ganttTest.png", BatikImageRenderer.getByteArray(diagram.create(), 800, ImageFormat.PNG));
    writeFile("ganttTest.svg", BatikImageRenderer.getByteArray(diagram.create(), 800, ImageFormat.SVG));
    writeFile("ganttTest.pdf", BatikImageRenderer.getByteArray(diagram.create(), 800, ImageFormat.PDF));
  }

  private void writeFile(final String filename, final byte[] ba) throws IOException
  {
    final File file = WorkFileHelper.getWorkFile(filename);
    log.info("Writing Gantt test image to work directory: " + file.getAbsolutePath());
    FileUtils.writeByteArrayToFile(file, ba);
  }

  @Test
  public void testTestDiagram()
  {
    final GanttTask root = createTestChart();
    assertDate(2010, Calendar.JUNE, 1, root.getCalculatedStartDate());
    assertDate(2010, Calendar.JUNE, 1, root.findByWorkpackageCode("001").getCalculatedStartDate());
    // 2010-06-03 is an holiday.
    assertDate(2010, Calendar.JUNE, 16, root.findByWorkpackageCode("001").getCalculatedEndDate()); // Duration of 10 working days
    assertDate(2010, Calendar.JUNE, 30, root.findByWorkpackageCode("002").getCalculatedStartDate()); // 10 days after EB.
  }

  private GanttTask createTestChart()
  {
    // final GanttObjectImpl root = createGanttObject(null, "--", "rootNode", "0");
    final GanttTaskImpl phase1 = createGanttObject(null, "P1", "Phase 1", "5");
    final GanttTaskImpl task1 = createGanttObject(phase1, "001", "Task 1", "10");
    final DayHolder day = new DayHolder();
    day.setDate(2010, Calendar.JUNE, 1);
    task1.setStartDate(day.getDate());
    createGanttObject(phase1, "002", "Task 2 (finish-start)", "10", task1, GanttRelationType.FINISH_START, 10);
    final GanttTaskImpl task3 = createGanttObject(phase1, "003", "Task 3 (finish-start II)", "5", task1, null, 0);
    createGanttObject(phase1, "004", "Task 4 (finish-start, depth = 2)", "5", task3, null, -3);
    final GanttTaskImpl task5 = createGanttObject(phase1, "005", "Task 5", "5");
    day.add(Calendar.MONTH, 1);
    task5.setStartDate(day.getDate());
    createGanttObject(phase1, "006", "Task 6 (finish-finish)", "3", task5, GanttRelationType.FINISH_FINISH, 0);
    createGanttObject(phase1, "007", "Task 7 (finish_finish)", "3", task5, GanttRelationType.FINISH_FINISH, -8);
    final GanttTaskImpl task8 = createGanttObject(phase1, "008", "Task 8 (finish_finish)", "3", task5,
        GanttRelationType.FINISH_FINISH, 3);
    createGanttObject(phase1, "009", "Task 9 (start-start)", "3", task8, GanttRelationType.START_START, 0);
    createGanttObject(phase1, "010", "Task 10 (start-start)", "3", task8, GanttRelationType.START_START, 5);
    createGanttObject(phase1, "011", "Task 11 (start-start)", "3", task8, GanttRelationType.START_START, -5);
    final GanttTaskImpl task12 = createGanttObject(phase1, "012", "Task 12", "5");
    day.add(Calendar.WEEK_OF_YEAR, 3);
    task12.setStartDate(day.getDate());
    createGanttObject(phase1, "013", "Task 13 (start-finish)", "3", task12, GanttRelationType.START_FINISH, -5);
    createGanttObject(phase1, "014", "Task 14 (start-finish)", "3", task12, GanttRelationType.START_FINISH, 5);
    createGanttObject(phase1, "015", "Task 15 (start-finish)", "3", task12, GanttRelationType.START_FINISH, 0);
    final GanttTaskImpl task16 = createGanttObject(phase1, "016", "Task 16", "6");
    day.add(Calendar.WEEK_OF_YEAR, 1);
    task16.setStartDate(day.getDate());
    return phase1;
  }

  private GanttTaskImpl createGanttObject(final GanttTaskImpl parent, final String workpackageCode, final String name,
      final String durationDays)
  {
    return createGanttObject(parent, workpackageCode, name, durationDays, null, null, 0);
  }

  private GanttTaskImpl createGanttObject(final GanttTaskImpl parent, final String workpackageCode, final String title,
      final String durationDays, final GanttTask dependsOnGanttObject, final GanttRelationType type,
      final int dependDayOffset)
  {
    final GanttTaskImpl node = new GanttTaskImpl();
    node.setTitle(title);
    node.setWorkpackageCode(workpackageCode);
    node.setDuration(new BigDecimal(durationDays));
    node.setVisible(true);
    if (dependsOnGanttObject != null) {
      node.setPredecessor(dependsOnGanttObject);
    }
    if (type != null) {
      node.setRelationType(type);
    }
    if (dependDayOffset != 0) {
      node.setPredecessorOffset(dependDayOffset);
    }
    if (parent != null) {
      parent.addChild(node);
    }
    node.setPredecessor(dependsOnGanttObject);
    return node;
  }

  private void assertDate(final int year, final int month, final int day, final Date date)
  {
    final DayHolder dh = new DayHolder(date);
    assertEquals(year, dh.getYear());
    assertEquals(month, dh.getMonth());
    assertEquals(day, dh.getDayOfMonth());
  }
}
