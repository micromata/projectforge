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

package org.projectforge.common;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.business.test.AbstractTestBase;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

public class DateHelperTest extends AbstractTestBase {
  private static transient final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DateHelperTest.class);

  @Test
  public void testTimeZone() throws ParseException {
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
  public void formatIsoDate() {
    assertEquals("1970-11-21", DateHelper
            .formatIsoDate(createDate(1970, Month.NOVEMBER, 21, 16, 0, 0, 0, ThreadLocalUserContext.getTimeZone())));
    assertEquals("1970-11-21", DateHelper
            .formatIsoDate(createDate(1970, Month.NOVEMBER, 21, 16, 35, 27, 968, ThreadLocalUserContext.getTimeZone())));
  }

  @Test
  public void formatIsoTimestamp() {
    assertEquals("1970-11-21 17:00:00.000",
            DateHelper.formatIsoTimestamp(
                    createDate(1970, Month.NOVEMBER, 21, 17, 0, 0, 0, ThreadLocalUserContext.getTimeZone())));
    assertEquals("1970-11-21 17:05:07.123",
            DateHelper.formatIsoTimestamp(
                    createDate(1970, Month.NOVEMBER, 21, 17, 5, 7, 123, ThreadLocalUserContext.getTimeZone())));
  }

  @Test
  public void formatMonth() {
    assertEquals("2009-01", DateHelper.formatMonth(2009, Month.JANUARY));
    assertEquals("2009-03", DateHelper.formatMonth(2009, Month.MARCH));
    assertEquals("2009-09", DateHelper.formatMonth(2009, Month.SEPTEMBER));
    assertEquals("2009-10", DateHelper.formatMonth(2009, Month.OCTOBER));
    assertEquals("2009-12", DateHelper.formatMonth(2009, Month.DECEMBER));
  }

  @Test
  public void dateOfYearBetween() {
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

  public static Date createDate(final int year, final Month month, final int day, final int hour, final int minute,
                                final int second, final int millisecond, TimeZone timeZone) {
    return PFDateTime.withDate(year, month, day, hour, minute, second, millisecond, timeZone.toZoneId()).getUtilDate();
  }
}
