/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.test.TestSetup;

import java.util.Calendar;
import java.util.Locale;

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
    final Calendar cal = Calendar.getInstance(Locale.GERMAN);
    WeekHolder week = new WeekHolder(cal);
    assertEquals(7, week.getDays().length);
    assertEquals(2, week.getDays()[0].getDayOfWeek());
    assertEquals("monday", week.getDays()[0].getDayKey());
    final DateHolder dateHolder = new DateHolder(DatePrecision.DAY, Locale.GERMAN);
    dateHolder.setDate(1970, Calendar.NOVEMBER, 21, 4, 50, 23);
    week = new WeekHolder(dateHolder.getCalendar());
    assertEquals(7, week.getDays().length);
    assertEquals(2, week.getDays()[0].getDayOfWeek());
    assertEquals("monday", week.getDays()[0].getDayKey());
    assertEquals("sunday", week.getDays()[6].getDayKey());
    assertEquals(16, week.getDays()[0].getDayOfMonth());
    assertEquals("saturday", week.getDays()[5].getDayKey());
    assertEquals(21, week.getDays()[5].getDayOfMonth());
    dateHolder.setDate(2007, Calendar.MARCH, 1, 4, 50, 23);
    assertEquals(Calendar.MARCH, dateHolder.getMonth());
    week = new WeekHolder(dateHolder.getCalendar(), dateHolder.getMonth());
    assertEquals("monday", week.getDays()[0].getDayKey());
    assertEquals(26, week.getDays()[0].getDayOfMonth());
    assertEquals(true, week.getDays()[0].isMarker()); // February, 26
    assertEquals(true, week.getDays()[1].isMarker()); // February, 27
    assertEquals(true, week.getDays()[2].isMarker()); // February, 28
    assertEquals(1, week.getDays()[3].getDayOfMonth());
    assertEquals(false, week.getDays()[3].isMarker(), "Day is not of current month and should be marked."); // March, 1
  }
}
