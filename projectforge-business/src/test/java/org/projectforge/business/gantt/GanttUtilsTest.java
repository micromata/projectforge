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
import static org.testng.AssertJUnit.assertTrue;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.projectforge.framework.configuration.ConfigXmlTest;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.test.AbstractTestBase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GanttUtilsTest extends AbstractTestBase
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GanttUtilsTest.class);

  private static int counter = 0;

  private static BigDecimal TWO = new BigDecimal("2");

  @BeforeClass
  public static void setUpThisTest()
  {
    // Needed if this tests runs before the ConfigurationTest.
    ConfigXmlTest.createTestConfiguration();
  }

  @Test
  public void calculateFromDate()
  {
    final GanttTaskImpl activity1 = createActivity(10).setTitle("activity1");
    final DayHolder day = new DayHolder();
    day.setDate(2010, Calendar.FEBRUARY, 5);
    activity1.setStartDate(day.getDate());
    assertDates("2010-02-05", "2010-02-19", activity1);
    activity1.setStartDate(null);
    day.addWorkingDays(10);
    activity1.setEndDate(day.getDate());
    assertDates("2010-02-05", "2010-02-19", activity1);

    final GanttTaskImpl activity2 = createActivity(5).setTitle("activity2");
    activity2.setPredecessor(activity1);
    assertDate(2010, Calendar.FEBRUARY, 19, GanttUtils.getCalculatedStartDate(activity2));
    activity2.setPredecessorOffset(2);
    assertDate(2010, Calendar.FEBRUARY, 19, GanttUtils.getCalculatedStartDate(activity2));
    activity2.recalculate();
    assertDate(2010, Calendar.FEBRUARY, 23, GanttUtils.getCalculatedStartDate(activity2));

    final GanttTaskImpl a1 = createActivity(1).setTitle("a1");
    day.setDate(2010, Calendar.FEBRUARY, 1);
    a1.setStartDate(day.getDate());
    final GanttTaskImpl a2 = createActivity(10).setTitle("a2");
    a2.setPredecessor(a1);
    final GanttTaskImpl a2_1 = createActivity(10).setTitle("a2_1");
    a2_1.setPredecessor(a2).setRelationType(GanttRelationType.START_START);
    a2.addChild(a2_1);
    final GanttTaskImpl a2_2 = createActivity(2).setTitle("a2_2");
    a2_2.setPredecessor(a2).setRelationType(GanttRelationType.START_START);
    a2.addChild(a2_2);
    assertDate(2010, Calendar.FEBRUARY, 1, GanttUtils.getCalculatedStartDate(a1));
    assertDate(2010, Calendar.FEBRUARY, 2, GanttUtils.getCalculatedStartDate(a2));
    assertDate(2010, Calendar.FEBRUARY, 2, GanttUtils.getCalculatedStartDate(a2_1));
    assertDate(2010, Calendar.FEBRUARY, 2, GanttUtils.getCalculatedStartDate(a2_2));
  }

  @Test
  public void subActivities()
  {
    final DayHolder day = new DayHolder();
    final GanttTaskImpl a1 = createActivity(1).setTitle("a1");
    day.setDate(2010, Calendar.SEPTEMBER, 1);
    a1.setStartDate(day.getDate());
    final GanttTaskImpl a2 = createActivity(-1).setTitle("a2");
    final GanttTaskImpl a2_1 = createActivity(2).setTitle("a2_1");
    a2_1.setPredecessor(a1);
    a2.addChild(a2_1);
    final GanttTaskImpl a2_2 = createActivity(2).setTitle("a2_2");
    a2_2.setPredecessor(a2_1);
    a2.addChild(a2_2);
    assertDates("2010-09-01", "2010-09-02", a1);
    assertDates("2010-09-02", "2010-09-06", a2_1); // 2010-09-04 to 2010-09-05 is a weekend.
    assertDates("2010-09-06", "2010-09-08", a2_2);
    assertDates("2010-09-02", "2010-09-08", a2);
    a2.setDuration(TWO);
    assertDates("Start date calculated from children and duration is fixed", "2010-09-02", "2010-09-06", a2);
    a2.setStartDate(day.getDate());
    assertDates("Start date and duration are fixed", "2010-09-01", "2010-09-03", a2);

    a2.setStartDate(null).setDuration(null).setPredecessor(a1).setPredecessorOffset(1);
    assertDates("Start date is calculated from predecessor.", "2010-09-03", "2010-09-08", a2);
    a2.setDuration(TWO);
    assertDates("Start date is calculated from predecessor and duration is fixed.", "2010-09-03", "2010-09-07", a2);
    a2.setRelationType(GanttRelationType.FINISH_START).setDuration(null); // Default -> same results:
    assertDates("Start date is calculated from predecessor.", "2010-09-03", "2010-09-08", a2);
    a2.setDuration(TWO);
    assertDates("Start date is calculated from predecessor and duration is fixed.", "2010-09-03", "2010-09-07", a2);
    a2.setRelationType(GanttRelationType.START_START).setDuration(null);
    assertDates("Start date is calculated from predecessor.", "2010-09-02", "2010-09-08", a2);
    a2.setDuration(TWO);
    assertDates("Start date is calculated from predecessor and duration is fixed.", "2010-09-02", "2010-09-06", a2);
    a2.setRelationType(GanttRelationType.START_FINISH).setDuration(null).setPredecessorOffset(4);
    assertDates("End date is calculated from predecessor.", "2010-09-02", "2010-09-07", a2);
    a2.setDuration(TWO);
    // assertDates("End date is calculated from predecessor and duration is fixed.", "2010-09-03", "2010-09-07", a2);

  }

  @Test
  public void circularReferences()
  {
    final GanttTaskImpl a1 = createActivity(1).setTitle("a1");
    final GanttTaskImpl a1_1 = createActivity(10).setTitle("a1_1");
    a1_1.setPredecessor(a1);
    a1.addChild(a1_1);
    log.error("The two following error messages about circular reference detection are OK and part of this test.");
    assertNull(GanttUtils.getCalculatedStartDate(a1));
    assertNull(GanttUtils.getCalculatedEndDate(a1));
    assertNull(GanttUtils.getCalculatedStartDate(a1_1));
    assertNull(GanttUtils.getCalculatedEndDate(a1_1));
  }

  @Test
  public void compareTo()
  {
    final GanttTaskImpl a1 = new GanttTaskImpl(1).setTitle("B");
    assertEquals(0, GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a1));
    GanttTaskImpl a2 = new GanttTaskImpl(1).setTitle("A");
    assertEquals(0, GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a1)); // Same id
    a2 = new GanttTaskImpl(2).setTitle("A");
    assertTrue("Start date not given, use alphabetical order instead.",
        GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a2) > 0);
    assertTrue("Start date not given, use alphabetical order instead.",
        GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a2, a1) < 0);

    final DayHolder day = new DayHolder();
    day.setDate(2010, Calendar.JUNE, 1);
    a1.setStartDate(day.getDate());
    assertTrue("a1.startDate before a2.startDate = null (now).",
        GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a2) < 0);
    assertTrue("a1.startDate before a2.startDate = null (now).",
        GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a2, a1) > 0);
    day.addWorkingDays(2);
    a2.setStartDate(day.getDate());
    assertTrue("a1.startDate before a2.startDate.", GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a2) < 0);
    assertTrue("a1.startDate before a2.startDate.", GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a2, a1) > 0);
    a1.setStartDate(day.getDate());
    assertTrue("Same start date -> alphabetical order", GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a2) > 0);
    assertTrue("Same start date -> alphabetical order", GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a2, a1) < 0);
    day.addWorkingDays(2);
    a2.setEndDate(day.getDate());
    final Date a1StartDate = a1.getStartDate();
    a1.setStartDate(null);
    assertTrue("a1.endDate = null after a2.endDate", GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a2) > 0);
    assertTrue("a1.endDate = null before a2.endDate", GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a2, a1) < 0);
    a1.setEndDate(day.getDate());
    a1.setStartDate(a1StartDate);
    assertTrue("Same start and end date -> alphabetical order", GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a2) > 0);
    assertTrue("Same start and end date -> alphabetical order", GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a2, a1) < 0);
  }

  private GanttTaskImpl createActivity(final int durationDays)
  {
    final GanttTaskImpl activity = new GanttTaskImpl();
    if (durationDays >= 0) {
      activity.setDuration(new BigDecimal(durationDays));
    }
    activity.setId(counter++);
    return activity;
  }

  private void assertDate(final int year, final int month, final int day, final Date date)
  {
    final DayHolder dh = new DayHolder();
    dh.setDate(year, month, day);
    assertEquals(DateHelper.formatIsoDate(dh.getDate()), DateHelper.formatIsoDate(date));
  }

  private void assertDates(final String expectedCalculatedStartDate, final String expectedCalculatedEndDate,
      final GanttTask task)
  {
    assertEquals(expectedCalculatedStartDate, DateHelper.formatIsoDate(task.recalculate().getCalculatedStartDate()));
    assertEquals(expectedCalculatedEndDate, DateHelper.formatIsoDate(task.getCalculatedEndDate()));
  }

  private void assertDates(final String msg, final String expectedCalculatedStartDate,
      final String expectedCalculatedEndDate,
      final GanttTask task)
  {
    assertEquals(msg, expectedCalculatedStartDate,
        DateHelper.formatIsoDate(task.recalculate().getCalculatedStartDate()));
    assertEquals(msg, expectedCalculatedEndDate, DateHelper.formatIsoDate(task.getCalculatedEndDate()));
  }
}
