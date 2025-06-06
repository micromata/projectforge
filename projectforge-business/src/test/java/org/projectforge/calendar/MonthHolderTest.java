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

package org.projectforge.calendar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.calendar.MonthHolder;
import org.projectforge.framework.calendar.WeekHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.PFDay;
import org.projectforge.business.test.AbstractTestBase;
import org.projectforge.business.test.TestSetup;

import java.time.DayOfWeek;
import java.time.Month;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonthHolderTest {

  @BeforeAll
  static void beforeAll() {
    TestSetup.init();
  }

  @Test
  public void testMonthHolder() {
    final PFDateTime dateTime = PFDateTime.from(new Date(), null, Locale.GERMAN)
            .withPrecision(DatePrecision.DAY).withDate(1970, Month.NOVEMBER, 21, 0, 0, 0);
    final MonthHolder month = new MonthHolder(dateTime);
    assertEquals(6, month.getWeeks().size());
    WeekHolder week = month.getFirstWeek();
    assertEquals(DayOfWeek.MONDAY, week.getDays()[0].getDayOfWeek());
    assertEquals(26, week.getDays()[0].getDayOfMonth());
    assertEquals(Month.OCTOBER, week.getDays()[0].getMonth());
    //assertTrue(week.getDays()[0].isMarker(), "Day is marked, because it is not part of the month.");
    week = month.getWeeks().get(5);
    assertEquals(DayOfWeek.MONDAY, week.getDays()[0].getDayOfWeek());
    assertEquals(30, week.getDays()[0].getDayOfMonth());
    //assertFalse(week.getDays()[0].isMarker(), "Day is not marked, because it is part of the month.");
    assertEquals(6, week.getDays()[6].getDayOfMonth());
    //assertTrue(week.getDays()[6].isMarker(), "Day is marked, because it is not part of the month.");
    assertEquals(Month.DECEMBER, week.getDays()[6].getMonth());
  }

  @Test
  public void testNumberOfWorkingDays() {
    final PFDateTime dateTime = PFDateTime.from(new Date(), null, Locale.GERMAN)
            .withPrecision(DatePrecision.DAY).withDate(2009, Month.JANUARY, 16, 0, 0, 0);
    MonthHolder month = new MonthHolder(dateTime.getUtilDate());
    AbstractTestBase.assertBigDecimal(21, month.getNumberOfWorkingDays());
    month = new MonthHolder(dateTime.withMonth(Month.FEBRUARY).getUtilDate());
    AbstractTestBase.assertBigDecimal(20, month.getNumberOfWorkingDays());
    month = new MonthHolder(dateTime.withMonth(Month.NOVEMBER).getUtilDate());
    AbstractTestBase.assertBigDecimal(21, month.getNumberOfWorkingDays());
    month = new MonthHolder(dateTime.withMonth(Month.DECEMBER).getUtilDate());
    AbstractTestBase.assertBigDecimal(21, month.getNumberOfWorkingDays());
  }

  @Test
  public void testDays() {
    final MonthHolder mh = new MonthHolder(2013, Month.MAY);
    final List<PFDay> list = mh.getDays();
    Assertions.assertEquals(31, list.size());
    for (final PFDay dt : list) {
      Assertions.assertEquals(Month.MAY, dt.getMonth());
    }
    Assertions.assertEquals(1, list.get(0).getDayOfMonth());
    Assertions.assertEquals(31, list.get(30).getDayOfMonth());
  }
}
