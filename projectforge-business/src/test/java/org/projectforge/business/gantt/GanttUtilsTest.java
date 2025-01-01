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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.time.PFDay;
import org.projectforge.business.test.AbstractTestBase;
import org.projectforge.business.test.TestSetup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class GanttUtilsTest extends AbstractTestBase {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GanttUtilsTest.class);

  private static long counter = 0;

  private static BigDecimal TWO = new BigDecimal("2");

  @BeforeEach
  public void setUpThisTest() {
    // Needed if this tests runs before the ConfigurationTest.
    TestSetup.init();
  }

  @Test
  public void calculateFromDate() {
    final GanttTaskImpl activity1 = createActivity(10).setTitle("activity1");
    final DayHolder day = new DayHolder();
    day.setDate(2010, Month.FEBRUARY, 5);
    activity1.setStartDate(day.getLocalDate());
    assertDates("2010-02-05", "2010-02-19", activity1);
    activity1.setStartDate(null);
    day.addWorkingDays(10);
    activity1.setEndDate(day.getLocalDate());
    assertDates("2010-02-05", "2010-02-19", activity1);

    final GanttTaskImpl activity2 = createActivity(5).setTitle("activity2");
    activity2.setPredecessor(activity1);
    assertLocalDate(GanttUtils.getCalculatedStartDate(activity2), 2010, Month.FEBRUARY, 19);
    activity2.setPredecessorOffset(2);
    assertLocalDate(GanttUtils.getCalculatedStartDate(activity2),2010, Month.FEBRUARY, 19);
    activity2.recalculate();
    assertLocalDate(GanttUtils.getCalculatedStartDate(activity2),2010, Month.FEBRUARY, 23);

    final GanttTaskImpl a1 = createActivity(1).setTitle("a1");
    day.setDate(2010, Month.FEBRUARY, 1);
    a1.setStartDate(day.getLocalDate());
    final GanttTaskImpl a2 = createActivity(10).setTitle("a2");
    a2.setPredecessor(a1);
    final GanttTaskImpl a2_1 = createActivity(10).setTitle("a2_1");
    a2_1.setPredecessor(a2).setRelationType(GanttRelationType.START_START);
    a2.addChild(a2_1);
    final GanttTaskImpl a2_2 = createActivity(2).setTitle("a2_2");
    a2_2.setPredecessor(a2).setRelationType(GanttRelationType.START_START);
    a2.addChild(a2_2);
    assertLocalDate(GanttUtils.getCalculatedStartDate(a1), 2010, Month.FEBRUARY, 1);
    assertLocalDate(GanttUtils.getCalculatedStartDate(a2), 2010, Month.FEBRUARY, 2);
    assertLocalDate(GanttUtils.getCalculatedStartDate(a2), 2010, Month.FEBRUARY, 2);
    assertLocalDate(GanttUtils.getCalculatedStartDate(a2),2010, Month.FEBRUARY, 2);
  }

  @Test
  public void subActivities() {
    final DayHolder day = new DayHolder();
    final GanttTaskImpl a1 = createActivity(1).setTitle("a1");
    day.setDate(2010, Month.SEPTEMBER, 1);
    a1.setStartDate(day.getLocalDate());
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
    a2.setStartDate(day.getLocalDate());
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
  public void circularReferences() {
    final GanttTaskImpl a1 = createActivity(1).setTitle("a1");
    final GanttTaskImpl a1_1 = createActivity(10).setTitle("a1_1");
    a1_1.setPredecessor(a1);
    a1.addChild(a1_1);
    assertNull(GanttUtils.getCalculatedStartDate(a1));
    assertNull(GanttUtils.getCalculatedEndDate(a1));
    assertNull(GanttUtils.getCalculatedStartDate(a1_1));
    assertNull(GanttUtils.getCalculatedEndDate(a1_1));
  }

  @Test
  public void compareTo() {
    final GanttTaskImpl a1 = new GanttTaskImpl(1L).setTitle("B");
    assertEquals(0, GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a1));
    GanttTaskImpl a2 = new GanttTaskImpl(1L).setTitle("A");
    assertEquals(0, GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a1)); // Same id
    a2 = new GanttTaskImpl(2L).setTitle("A");
    assertTrue(GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a2) > 0,
            "Start date not given, use alphabetical order instead.");
    assertTrue(GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a2, a1) < 0,
            "Start date not given, use alphabetical order instead.");

    final DayHolder day = new DayHolder();
    day.setDate(2010, Month.JUNE, 1);
    a1.setStartDate(day.getLocalDate());
    assertTrue(GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a2) < 0,
            "a1.startDate before a2.startDate = null (now).");
    assertTrue(GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a2, a1) > 0,
            "a1.startDate before a2.startDate = null (now).");
    day.addWorkingDays(2);
    a2.setStartDate(day.getLocalDate());
    assertTrue(GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a2) < 0,
            "a1.startDate before a2.startDate.");
    assertTrue(GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a2, a1) > 0,
            "a1.startDate before a2.startDate.");
    a1.setStartDate(day.getLocalDate());
    assertTrue(GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a2) > 0,
            "Same start date -> alphabetical order");
    assertTrue(GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a2, a1) < 0,
            "Same start date -> alphabetical order");
    day.addWorkingDays(2);
    a2.setEndDate(day.getLocalDate());
    final LocalDate a1StartDate = a1.getStartDate();
    a1.setStartDate(null);
    assertTrue(GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a2) > 0,
            "a1.endDate = null after a2.endDate");
    assertTrue(GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a2, a1) < 0,
            "a1.endDate = null before a2.endDate");
    a1.setEndDate(day.getLocalDate());
    a1.setStartDate(a1StartDate);
    assertTrue(GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a1, a2) > 0,
            "Same start and end date -> alphabetical order");
    assertTrue(GanttUtils.GANTT_OBJECT_COMPARATOR.compare(a2, a1) < 0,
            "Same start and end date -> alphabetical order");
  }

  private GanttTaskImpl createActivity(final int durationDays) {
    final GanttTaskImpl activity = new GanttTaskImpl();
    if (durationDays >= 0) {
      activity.setDuration(new BigDecimal(durationDays));
    }
    activity.setId(counter++);
    return activity;
  }

  private void assertDate(final int year, final Month month, final int day, final Date date) {
    final DayHolder dh = new DayHolder();
    dh.setDate(year, month, day);
    assertEquals(DateHelper.formatIsoDate(dh.getUtilDate()), DateHelper.formatIsoDate(date));
  }

  private void assertDates(final String expectedCalculatedStartDate, final String expectedCalculatedEndDate,
                           final GanttTask task) {
    assertEquals(expectedCalculatedStartDate, DateHelper.formatIsoDate(PFDay.from(task.recalculate().getCalculatedStartDate()).getUtilDate()));
    assertEquals(expectedCalculatedEndDate, DateHelper.formatIsoDate(PFDay.from(task.getCalculatedEndDate()).getUtilDate()));
  }

  private void assertDates(final String msg, final String expectedCalculatedStartDate,
                           final String expectedCalculatedEndDate,
                           final GanttTask task) {
    assertEquals(expectedCalculatedStartDate, DateHelper.formatIsoDate(PFDay.from(task.recalculate().getCalculatedStartDate()).getUtilDate()), msg);
    assertEquals(expectedCalculatedEndDate, DateHelper.formatIsoDate(PFDay.from(task.getCalculatedEndDate()).getUtilDate()), msg);
  }
}
