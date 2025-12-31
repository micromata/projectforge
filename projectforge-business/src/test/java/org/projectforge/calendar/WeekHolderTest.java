/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.calendar;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.calendar.WeekHolder;
import org.projectforge.framework.time.PFDay;
import org.projectforge.business.test.TestSetup;

import java.time.DayOfWeek;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WeekHolderTest {
  //private final static Logger log = Logger.getLogger(WeekHolderTest.class);

  @BeforeAll
  public static void setUp() {
    // Needed if this tests runs before the ConfigurationTest.
    TestSetup.init();
  }

  @Test
  public void testWeekHolder() {
    final PFDay dt = PFDay.now();
    WeekHolder week = new WeekHolder(dt);
    assertEquals(7, week.getDays().length);
    assertEquals(1, week.getDays()[0].getDayOfWeek().getValue());
    assertEquals(DayOfWeek.MONDAY, week.getDays()[0].getDayOfWeek());
    PFDay date = PFDay.now().withDate(1970, Month.NOVEMBER, 21);
    week = new WeekHolder(date);
    assertEquals(7, week.getDays().length);
    assertEquals(1, week.getDays()[0].getDayOfWeek().getValue());
    assertEquals(DayOfWeek.MONDAY, week.getDays()[0].getDayOfWeek());
    assertEquals(DayOfWeek.SUNDAY, week.getDays()[6].getDayOfWeek());
    assertEquals(16, week.getDays()[0].getDayOfMonth());
    assertEquals(DayOfWeek.SATURDAY, week.getDays()[5].getDayOfWeek());
    assertEquals(21, week.getDays()[5].getDayOfMonth());
    date = date.withDate(2007, Month.MARCH, 1);
    assertEquals(Month.MARCH, date.getMonth());
    week = new WeekHolder(date);
    assertEquals(DayOfWeek.MONDAY, week.getDays()[0].getDayOfWeek());
    assertEquals(26, week.getDays()[0].getDayOfMonth());
    //assertTrue(week.getDays()[0].isMarker()); // February, 26
    //assertTrue(week.getDays()[1].isMarker()); // February, 27
    //assertTrue(week.getDays()[2].isMarker()); // February, 28
    assertEquals(1, week.getDays()[3].getDayOfMonth());
    //assertFalse(week.getDays()[3].isMarker(), "Day is not of current month and should be marked."); // March, 1
  }
}
