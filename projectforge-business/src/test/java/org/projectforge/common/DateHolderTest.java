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

package org.projectforge.common;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class DateHolderTest
{
  @Test
  public void isSameDay()
  {
    final Calendar cal = Calendar.getInstance(DateHelper.EUROPE_BERLIN);
    cal.set(2008, Calendar.MARCH, 5, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    final DateHolder date = new DateHolder(cal.getTime(), DateHelper.EUROPE_BERLIN);
    assertEquals("2008-03-04 23:00:00.000", DateHelper.FOR_TESTCASE_OUTPUT_FORMATTER.get().format(date.getDate()));
    final java.sql.Date sqlDate = date.getSQLDate();
    assertEquals("2008-03-05 00:00:00.000", DateHelper.FOR_TESTCASE_OUTPUT_FORMATTER.get().format(sqlDate));
    assertTrue(date.isSameDay(sqlDate) == true);
  }

  @Test
  public void getSQLDate()
  {
    final Calendar cal = Calendar.getInstance(DateHelper.EUROPE_BERLIN);
    cal.set(2008, Calendar.MARCH, 5, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    final DateHolder date = new DateHolder(cal.getTime(), DateHelper.EUROPE_BERLIN);
    assertEquals("2008-03-04 23:00:00.000", DateHelper.FOR_TESTCASE_OUTPUT_FORMATTER.get().format(date.getDate()));
    final java.sql.Date sqlDate = date.getSQLDate();
    assertEquals("2008-03-05 00:00:00.000", DateHelper.FOR_TESTCASE_OUTPUT_FORMATTER.get().format(sqlDate));
  }

  @Test
  public void daysBetween()
  {
    final DateHolder date1 = new DateHolder(DatePrecision.DAY, Locale.GERMAN);
    date1.setDate(2008, 02, 23, 0, 0, 0);
    final DateHolder date2 = new DateHolder(DatePrecision.DAY, Locale.GERMAN);
    date2.setDate(2008, 02, 23, 0, 0, 0);
    assertEquals(0, date1.daysBetween(date2.getDate()));
    date2.setDate(2008, 02, 24, 0, 0, 0);
    assertEquals(1, date1.daysBetween(date2.getDate()));
    date2.setDate(2008, 02, 22, 0, 0, 0);
    assertEquals(-1, date1.daysBetween(date2.getDate()));
    date2.setDate(date1.getDate());
    date2.add(Calendar.DAY_OF_YEAR, 364);
    assertEquals(364, date1.daysBetween(date2.getDate()));

    date1.setDate(2010, Calendar.JANUARY, 01);
    date2.setDate(2010, Calendar.DECEMBER, 31);
    assertEquals(daysBetween(date1, date2), date1.daysBetween(date2));
    date2.setDate(2011, Calendar.JANUARY, 1);
    assertEquals(daysBetween(date1, date2), date1.daysBetween(date2));
    date1.setDate(2010, Calendar.DECEMBER, 31);
    date2.setDate(2010, Calendar.JANUARY, 01);
    assertEquals(daysBetween(date1, date2), date1.daysBetween(date2));
    date2.setDate(2011, Calendar.JANUARY, 01);
    assertEquals(1, date1.daysBetween(date2));
    date2.setDate(2015, Calendar.JANUARY, 01);
    final int expected = daysBetween(date1, date2);
    assertEquals(expected, date1.daysBetween(date2));
    assertEquals(-expected, date2.daysBetween(date1));
  }

  @Test
  public void isBetween()
  {
    final DateHolder date1 = new DateHolder(DatePrecision.DAY);
    date1.setDate(2010, Calendar.FEBRUARY, 12, 0, 0, 0);
    final DateHolder date2 = new DateHolder(DatePrecision.DAY);
    date2.setDate(2010, Calendar.FEBRUARY, 14, 0, 0, 0);
    final DateHolder date3 = new DateHolder(DatePrecision.DAY);
    date3.setDate(2010, Calendar.FEBRUARY, 15, 0, 0, 0);
    assertFalse(date1.isBetween((Date) null, (Date) null));
    assertFalse(date1.isBetween((DateHolder) null, (DateHolder) null));
    assertTrue(date1.isBetween(null, date2));
    assertFalse(date2.isBetween(null, date1));
    assertTrue(date2.isBetween(date1, null));
    assertFalse(date2.isBetween(date3, null));
    assertTrue(date2.isBetween(date1, date3));
    assertTrue(date1.isBetween(date1, date3));
    assertTrue(date3.isBetween(date1, date3));
    assertFalse(date3.isBetween(date1, date2));
  }

  @Test
  public void ensurePrecision()
  {
    final DateHolder dateHolder = new DateHolder(DatePrecision.DAY, DateHelper.UTC, Locale.GERMAN);
    assertPrecision("1970-11-21 00:00:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 50, 23);
    dateHolder.setPrecision(DatePrecision.HOUR_OF_DAY);
    assertPrecision("1970-11-21 04:00:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 50, 23);
    dateHolder.setPrecision(DatePrecision.MINUTE);
    assertPrecision("1970-11-21 04:50:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 50, 23);
    dateHolder.setPrecision(DatePrecision.MINUTE_15);
    assertPrecision("1970-11-21 04:00:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 00, 00);
    assertPrecision("1970-11-21 04:00:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 7, 59);
    assertPrecision("1970-11-21 04:15:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 8, 00);
    assertPrecision("1970-11-21 04:15:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 15, 00);
    assertPrecision("1970-11-21 04:15:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 22, 59);
    assertPrecision("1970-11-21 04:30:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 23, 00);
    assertPrecision("1970-11-21 04:30:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 30, 00);
    assertPrecision("1970-11-21 04:30:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 37, 59);
    assertPrecision("1970-11-21 04:45:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 38, 00);
    assertPrecision("1970-11-21 04:45:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 45, 00);
    assertPrecision("1970-11-21 04:45:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 52, 59);
    assertPrecision("1970-11-21 05:00:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 53, 00);
    dateHolder.setPrecision(DatePrecision.MINUTE_5);
    assertPrecision("1970-11-21 04:00:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 02, 59);
    assertPrecision("1970-11-21 04:05:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 03, 00);
    assertPrecision("1970-11-21 04:50:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 48, 00);
    assertPrecision("1970-11-21 04:50:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 52, 59);
    assertPrecision("1970-11-21 04:55:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 53, 00);
    assertPrecision("1970-11-21 04:55:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 57, 59);
    assertPrecision("1970-11-21 05:00:00.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 58, 00);
    dateHolder.setPrecision(DatePrecision.SECOND);
    assertPrecision("1970-11-21 04:50:23.000", dateHolder, 1970, Calendar.NOVEMBER, 21, 4, 50, 23);
  }

  private void assertPrecision(final String expected, final DateHolder dateHolder, final int year, final int month,
      final int date, final int hourOfDay, final int minute, final int second)
  {
    dateHolder.setDate(year, month, date, hourOfDay, minute, second);
    dateHolder.ensurePrecision();
    assertEquals(expected, DateHelper.getForTestCase(dateHolder.getDate()));
  }

  @Test
  public void getBeginAndEndOfMonth()
  {
    DateHolder dateHolder = new DateHolder(DatePrecision.DAY, DateHelper.UTC, Locale.GERMAN);
    dateHolder.setDate(1970, Calendar.NOVEMBER, 21, 4, 50, 23);
    assertEquals("1970-11-21 00:00:00.000", DateHelper.getForTestCase(dateHolder.getDate()));
    dateHolder.setBeginOfMonth();
    assertEquals("1970-11-01 00:00:00.000", DateHelper.getForTestCase(dateHolder.getDate()));
    dateHolder.setDate(1970, Calendar.NOVEMBER, 21, 4, 50, 23);
    assertEquals("1970-11-21 00:00:00.000", DateHelper.getForTestCase(dateHolder.getDate()));
    dateHolder.setEndOfMonth();
    assertEquals("1970-11-30 23:59:59.999", DateHelper.getForTestCase(dateHolder.getDate()));

    dateHolder = new DateHolder(DatePrecision.DAY, DateHelper.UTC, Locale.GERMAN);
    dateHolder.setDate(2007, Calendar.JANUARY, 21, 4, 50, 23);
    dateHolder.setEndOfMonth();
    assertEquals("2007-01-31 23:59:59.999", DateHelper.getForTestCase(dateHolder.getDate()));
    dateHolder.setDate(2007, Calendar.FEBRUARY, 1, 4, 50, 23);
    dateHolder.setEndOfMonth();
    assertEquals("2007-02-28 23:59:59.999", DateHelper.getForTestCase(dateHolder.getDate()));
    dateHolder.setDate(2004, Calendar.FEBRUARY, 1, 4, 50, 23);
    dateHolder.setEndOfMonth();
    assertEquals("2004-02-29 23:59:59.999", DateHelper.getForTestCase(dateHolder.getDate()));
  }

  @Test
  public void getBeginAndEndOfWeek()
  {
    DateHolder dateHolder = new DateHolder(DatePrecision.DAY, DateHelper.UTC, Locale.GERMAN);
    dateHolder.setDate(1970, Calendar.NOVEMBER, 21, 4, 50, 23);
    assertEquals("1970-11-21 00:00:00.000", DateHelper.getForTestCase(dateHolder.getDate()));
    dateHolder.setBeginOfWeek();
    assertEquals("1970-11-16 00:00:00.000", DateHelper.getForTestCase(dateHolder.getDate()));
    dateHolder.setEndOfWeek();
    assertEquals("1970-11-22 23:59:59.999", DateHelper.getForTestCase(dateHolder.getDate()));
    dateHolder.setBeginOfWeek();
    assertEquals("1970-11-16 00:00:00.000", DateHelper.getForTestCase(dateHolder.getDate()));

    dateHolder = new DateHolder(DatePrecision.DAY, DateHelper.UTC, Locale.GERMAN);
    dateHolder.setDate(1970, Calendar.NOVEMBER, 21, 4, 50, 23);
    dateHolder.setEndOfWeek();
    assertEquals("1970-11-22 23:59:59.999", DateHelper.getForTestCase(dateHolder.getDate()));
  }

  @Test
  public void addWorkingDays()
  {
    final DateHolder dateHolder = new DateHolder(DatePrecision.MINUTE, DateHelper.UTC, Locale.GERMAN);
    dateHolder.setDate(2010, Calendar.MAY, 21, 4, 50, 23); // Friday
    dateHolder.addWorkingDays(0);
    assertEquals("2010-05-21 04:50:00.000", DateHelper.getForTestCase(dateHolder.getDate()));
    dateHolder.addWorkingDays(1); // Skip saturday, sunday and whit monday and weekend.
    assertEquals("2010-05-25 04:50:00.000", DateHelper.getForTestCase(dateHolder.getDate()));
    dateHolder.addWorkingDays(-1); // Skip saturday, sunday and whit monday and weekend.
    assertEquals("2010-05-21 04:50:00.000", DateHelper.getForTestCase(dateHolder.getDate()));
    dateHolder.addWorkingDays(1); // Skip saturday, sunday and whit monday and weekend.
    dateHolder.addWorkingDays(-6); // Skip saturday, sunday and whit monday and weekends.
    assertEquals("2010-05-14 04:50:00.000", DateHelper.getForTestCase(dateHolder.getDate()));
  }

  private int daysBetween(final DateHolder date1, final DateHolder date2)
  {
    final DateHolder dh = new DateHolder(date2.getDate());
    int count = 1;
    if (date1.getTimeInMillis() > date2.getTimeInMillis()) {
      count = -1;
    }
    int result = 0;
    for (int i = 0; i < 5000; i++) {
      if (dh.isSameDay(date1.getDate()) == true) {
        break;
      }
      result += count;
      dh.add(Calendar.DAY_OF_YEAR, -count);
    }
    return result;
  }
}
