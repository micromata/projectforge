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

package org.projectforge.common;

import static org.testng.AssertJUnit.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.joda.time.DateTimeConstants;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.test.AbstractTestBase;
import org.testng.annotations.Test;

public class DateHelperTest extends AbstractTestBase
{
  private static transient final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(KeyValuePairWriterTest.class);

  @Test
  public void testTimeZone() throws ParseException
  {
    final DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm");
    df.setTimeZone(DateHelper.EUROPE_BERLIN);
    final Date mezDate = df.parse("2008-03-14 17:25");
    final long mezMillis = mezDate.getTime();
    df.setTimeZone(DateHelper.UTC);
    final Date utcDate = df.parse("2008-03-14 16:25");
    final long utcMillis = utcDate.getTime();
    assertEquals(mezMillis, utcMillis);
  }

  @Test
  public void formatIsoDate()
  {
    assertEquals("1970-11-21", DateHelper
        .formatIsoDate(createDate(1970, Calendar.NOVEMBER, 21, 16, 0, 0, 0, ThreadLocalUserContext.getTimeZone())));
    assertEquals("1970-11-21", DateHelper
        .formatIsoDate(createDate(1970, Calendar.NOVEMBER, 21, 16, 35, 27, 968, ThreadLocalUserContext.getTimeZone())));
  }

  @Test
  public void formatIsoTimestamp()
  {
    assertEquals("1970-11-21 17:00:00.000",
        DateHelper.formatIsoTimestamp(
            createDate(1970, Calendar.NOVEMBER, 21, 17, 0, 0, 0, ThreadLocalUserContext.getTimeZone())));
    assertEquals("1970-11-21 17:05:07.123",
        DateHelper.formatIsoTimestamp(
            createDate(1970, Calendar.NOVEMBER, 21, 17, 5, 7, 123, ThreadLocalUserContext.getTimeZone())));
  }

  @Test
  public void getDuration()
  {
    final DateHolder dateHolder = new DateHolder(DatePrecision.MINUTE, Locale.GERMAN);
    dateHolder.setDate(1970, Calendar.NOVEMBER, 21, 4, 50, 0);
    final Date startTime = dateHolder.getDate();
    dateHolder.setDate(1970, Calendar.NOVEMBER, 21, 6, 59, 0);
    final Date stopTime = dateHolder.getDate();
    assertEquals(129, DateHelper.getDuration(startTime, stopTime));
    assertEquals(0, DateHelper.getDuration(stopTime, startTime));
    assertEquals(0, DateHelper.getDuration(null, stopTime));
    assertEquals(0, DateHelper.getDuration(startTime, null));
    assertEquals(0, DateHelper.getDuration(null, null));
    assertEquals(0, DateHelper.getDuration(startTime, startTime));
  }

  @Test
  public void formatMonth()
  {
    assertEquals("2009-01", DateHelper.formatMonth(2009, 0));
    assertEquals("2009-01", DateHelper.formatMonth(2009, Calendar.JANUARY));
    assertEquals("2009-03", DateHelper.formatMonth(2009, Calendar.MARCH));
    assertEquals("2009-09", DateHelper.formatMonth(2009, Calendar.SEPTEMBER));
    assertEquals("2009-10", DateHelper.formatMonth(2009, Calendar.OCTOBER));
    assertEquals("2009-12", DateHelper.formatMonth(2009, Calendar.DECEMBER));
  }

  @Test
  public void dateOfYearBetween()
  {
    assertTrue(DateHelper.dateOfYearBetween(1, 3, 1, 3, 1, 3)); // 1/3 is between 1/3 and 1/3
    // 1/3 - 1/20
    assertTrue(DateHelper.dateOfYearBetween(1, 3, 1, 3, 1, 20)); // 1/3 is between 1/3 and 1/20
    assertTrue(DateHelper.dateOfYearBetween(1, 20, 1, 3, 1, 20)); // 1/20 is between 1/3 and 1/20
    assertFalse(DateHelper.dateOfYearBetween(1, 2, 1, 3, 1, 20)); // 1/2 isn't between 1/3 and 1/20
    assertFalse(DateHelper.dateOfYearBetween(1, 21, 1, 3, 1, 20)); // 1/21 isn't between 1/3 and 1/20

    // 1/3 - 2/20
    assertTrue(DateHelper.dateOfYearBetween(1, 3, 1, 3, 2, 20)); // 1/3 is between 1/3 and 2/20
    assertTrue(DateHelper.dateOfYearBetween(1, 30, 1, 3, 2, 20)); // 1/30 is between 1/3 and 2/20
    assertTrue(DateHelper.dateOfYearBetween(2, 20, 1, 3, 2, 20)); // 2/20 is between 1/3 and 2/20
    assertFalse(DateHelper.dateOfYearBetween(1, 2, 1, 3, 2, 20)); // 1/2 isn't between 1/3 and 1/20
    assertFalse(DateHelper.dateOfYearBetween(2, 21, 1, 3, 2, 20)); // 2/21 isn't between 1/3 and 1/20

    // 1/3 - 3/20
    assertTrue(DateHelper.dateOfYearBetween(2, 1, 1, 3, 2, 20)); // 2/1 is between 1/3 and 3/20
    assertFalse(DateHelper.dateOfYearBetween(1, 2, 1, 3, 2, 20)); // 1/2 isn't between 1/3 and 3/20
    assertFalse(DateHelper.dateOfYearBetween(3, 21, 1, 3, 2, 20)); // 3/21 isn't between 1/3 and 3/20
    assertFalse(DateHelper.dateOfYearBetween(0, 21, 1, 3, 2, 20)); // 0/21 isn't between 1/3 and 3/20
    assertFalse(DateHelper.dateOfYearBetween(4, 21, 1, 3, 2, 20)); // 4/21 isn't between 1/3 and 3/20

    // 11/3 - 0/20
    assertTrue(DateHelper.dateOfYearBetween(0, 1, 11, 3, 0, 20)); // 0/1 is between 11/3 and 0/20
    assertTrue(DateHelper.dateOfYearBetween(11, 3, 11, 3, 0, 20)); // 11/3 is between 11/3 and 0/20
    assertTrue(DateHelper.dateOfYearBetween(0, 20, 11, 3, 0, 20)); // 11/3 is between 11/3 and 0/20
    assertFalse(DateHelper.dateOfYearBetween(0, 21, 11, 3, 0, 20)); // 0/21 isn't between 11/3 and 0/20
    assertFalse(DateHelper.dateOfYearBetween(11, 2, 11, 3, 0, 20)); // 11/21 isn't between 11/3 and 0/20

    // 10/3 - 2/20
    assertTrue(DateHelper.dateOfYearBetween(0, 1, 10, 3, 2, 20)); // 0/1 is between 10/3 and 2/20
    assertTrue(DateHelper.dateOfYearBetween(11, 3, 10, 3, 2, 20)); // 11/3 is between 10/3 and 2/20
    assertTrue(DateHelper.dateOfYearBetween(0, 20, 10, 3, 2, 20)); // 11/3 is between 10/3 and 2/20
    assertTrue(DateHelper.dateOfYearBetween(0, 21, 10, 3, 2, 20)); // 0/21 is between 10/3 and 2/20
    assertTrue(DateHelper.dateOfYearBetween(11, 2, 10, 3, 2, 20)); // 11/21 is between 10/3 and 2/20
    assertTrue(DateHelper.dateOfYearBetween(10, 3, 10, 3, 2, 20)); // 10/03 is between 10/3 and 2/20
    assertTrue(DateHelper.dateOfYearBetween(2, 20, 10, 3, 2, 20)); // 2/20 is between 10/3 and 2/20
    assertFalse(DateHelper.dateOfYearBetween(10, 2, 10, 3, 2, 20)); // 10/2 isn't between 10/3 and 2/20
    assertFalse(DateHelper.dateOfYearBetween(2, 21, 10, 3, 2, 20)); // 2/21 isn't between 10/3 and 2/20

    assertFalse(DateHelper.dateOfYearBetween(3, 21, 10, 3, 2, 20)); // 3/21 isn't between 10/3 and 2/20
    assertFalse(DateHelper.dateOfYearBetween(9, 21, 10, 3, 2, 20)); // 9/21 isn't between 10/3 and 2/20
  }

  @Test
  public void convertCalendarDayOfWeekToJoda()
  {
    assertEquals(DateTimeConstants.MONDAY, DateHelper.convertCalendarDayOfWeekToJoda(Calendar.MONDAY));
    assertEquals(DateTimeConstants.TUESDAY, DateHelper.convertCalendarDayOfWeekToJoda(Calendar.TUESDAY));
    assertEquals(DateTimeConstants.WEDNESDAY, DateHelper.convertCalendarDayOfWeekToJoda(Calendar.WEDNESDAY));
    assertEquals(DateTimeConstants.THURSDAY, DateHelper.convertCalendarDayOfWeekToJoda(Calendar.THURSDAY));
    assertEquals(DateTimeConstants.FRIDAY, DateHelper.convertCalendarDayOfWeekToJoda(Calendar.FRIDAY));
    assertEquals(DateTimeConstants.SATURDAY, DateHelper.convertCalendarDayOfWeekToJoda(Calendar.SATURDAY));
    assertEquals(DateTimeConstants.SUNDAY, DateHelper.convertCalendarDayOfWeekToJoda(Calendar.SUNDAY));
  }

  @Test
  public void convertDateIntoOtherTimezone()
  {
    final Date d1 = createDate(2016, 8, 22, 1, 2, 3, 4, TimeZone.getTimeZone("GMT+2:00"));
    assertEquals(d1, DateHelper.convertDateIntoOtherTimezone(d1, TimeZone.getTimeZone("GMT+5:00"), TimeZone.getTimeZone("GMT+5:00")));
    assertEquals(d1, DateHelper.convertDateIntoOtherTimezone(d1, TimeZone.getTimeZone("GMT-5:00"), TimeZone.getTimeZone("GMT-5:00")));
    assertEquals(d1, DateHelper.convertDateIntoOtherTimezone(d1, TimeZone.getTimeZone("GMT"), TimeZone.getTimeZone("GMT")));

    final Date d2 = createDate(2016, 8, 21, 23, 2, 3, 4, TimeZone.getTimeZone("GMT+2:00"));
    final Date d3 = createDate(2016, 8, 22, 6, 2, 3, 4, TimeZone.getTimeZone("GMT+2:00"));
    assertEquals(d2, DateHelper.convertDateIntoOtherTimezone(d1, TimeZone.getTimeZone("GMT+2:00"), TimeZone.getTimeZone("GMT")));
    assertEquals(d3, DateHelper.convertDateIntoOtherTimezone(d1, TimeZone.getTimeZone("GMT"), TimeZone.getTimeZone("GMT+5:00")));
  }

  public static Date createDate(final int year, final int month, final int day, final int hour, final int minute,
      final int second,
      final int millisecond, TimeZone timeZone)
  {
    final Calendar cal = Calendar.getInstance(timeZone);
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month);
    cal.set(Calendar.DAY_OF_MONTH, day);
    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, minute);
    cal.set(Calendar.SECOND, second);
    cal.set(Calendar.MILLISECOND, millisecond);
    return cal.getTime();
  }

  public static Date createDate(final int year, final int month, final int day, final int hour, final int minute,
      final int second,
      final int millisecond)
  {
    return createDate(year, month, day, hour, minute, second, millisecond, TimeZone.getDefault());
  }
}
