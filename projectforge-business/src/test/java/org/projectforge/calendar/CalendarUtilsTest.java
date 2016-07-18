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

package org.projectforge.calendar;

import java.util.Calendar;
import java.util.TimeZone;

import org.projectforge.framework.calendar.CalendarUtils;
import org.projectforge.framework.time.DateHelper;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CalendarUtilsTest
{
  @Test
  public void testMidnightCalendars()
  {
    final TimeZone timeZone = DateHelper.EUROPE_BERLIN;
    final Calendar utcCal = CalendarUtils
        .getUTCMidnightCalendar(CalendarTestUtils.createDate("2012-12-23 08:33:24.123", timeZone), timeZone);
    Assert.assertEquals("2012-12-23 00:00:00.000", CalendarTestUtils.formatUTCIsoDate(utcCal.getTime()));
    final Calendar userCal = CalendarUtils.getMidnightCalendarFromUTC(utcCal.getTime(), timeZone);
    Assert.assertEquals("2012-12-22 23:00:00.000", CalendarTestUtils.formatUTCIsoDate(userCal.getTime()));
  }

  @Test
  public void daysBetween()
  {
    final TimeZone timeZone = DateHelper.EUROPE_BERLIN;
    assertDaysBetween(0, 2012, Calendar.JANUARY, 1, 2012, Calendar.JANUARY, 1, timeZone);
    assertDaysBetween(1, 2012, Calendar.JANUARY, 1, 2012, Calendar.JANUARY, 2, timeZone);
    assertDaysBetween(365, 2011, Calendar.JANUARY, 1, 2012, Calendar.JANUARY, 1, timeZone);
    assertDaysBetween(366, 2012, Calendar.JANUARY, 1, 2013, Calendar.JANUARY, 1, timeZone);
    assertDaysBetween(365, 2013, Calendar.JANUARY, 1, 2014, Calendar.JANUARY, 1, timeZone);
    assertDaysBetween(1096, 2011, Calendar.JANUARY, 1, 2014, Calendar.JANUARY, 1, timeZone);
    assertDaysBetween(2, 2013, Calendar.DECEMBER, 30, 2014, Calendar.JANUARY, 1, timeZone);
    assertDaysBetween(367, 2012, Calendar.DECEMBER, 30, 2014, Calendar.JANUARY, 1, timeZone);
  }

  private void assertDaysBetween(final int expected, final int year1, final int month1, final int dayOfMonth1,
      final int year2,
      final int month2, final int dayOfMonth2, final TimeZone timeZone)
  {
    final Calendar cal1 = Calendar.getInstance(timeZone);
    cal1.set(Calendar.YEAR, year1);
    cal1.set(Calendar.MONTH, month1);
    cal1.set(Calendar.DAY_OF_MONTH, dayOfMonth1);
    final Calendar cal2 = Calendar.getInstance(timeZone);
    cal2.set(Calendar.YEAR, year2);
    cal2.set(Calendar.MONTH, month2);
    cal2.set(Calendar.DAY_OF_MONTH, dayOfMonth2);
    Assert.assertEquals(expected, CalendarUtils.daysBetween(cal1, cal2));
    Assert.assertEquals(-expected, CalendarUtils.daysBetween(cal2, cal1));
  }
}
