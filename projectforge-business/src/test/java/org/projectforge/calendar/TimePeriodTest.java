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

import org.jfree.data.time.Month;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.TimePeriod;
import org.projectforge.test.TestSetup;

import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimePeriodTest
{
  //private final static Logger log = Logger.getLogger(WeekHolderTest.class);

  @BeforeAll
  public static void setUp()
  {
    // Needed if this tests runs before the ConfigurationTest.
    TestSetup.init();
  }

  @Test
  public void testTimePeriod()
  {
    final PFDateTime dateTime1 = PFDateTime.from(new Date(), true, null, Locale.GERMAN).withPrecision(DatePrecision.MINUTE)
        .withYear(1970).withMonth(Month.NOVEMBER).withDayOfMonth(21).withHour(0).withMinute(0).withSecond(0);

    PFDateTime dateTime2 = dateTime1.withHour(10);

    TimePeriod timePeriod = new TimePeriod(dateTime1.getUtilDate(), dateTime2.getUtilDate());
    assertResultArray(new int[] { 0, 10, 0 }, timePeriod.getDurationFields());
    assertResultArray(new int[] { 1, 2, 0 }, timePeriod.getDurationFields(8));

    dateTime2 = dateTime2.withDayOfMonth(22).withHour(0);
    timePeriod = new TimePeriod(dateTime1.getUtilDate(), dateTime2.getUtilDate());
    assertResultArray(new int[] { 1, 0, 0 }, timePeriod.getDurationFields());
    assertResultArray(new int[] { 3, 0, 0 }, timePeriod.getDurationFields(8));
    assertResultArray(new int[] { 0, 24, 0 }, timePeriod.getDurationFields(8, 25));
    assertResultArray(new int[] { 3, 0, 0 }, timePeriod.getDurationFields(8, 24));

    dateTime2 = dateTime2.withDayOfMonth(21).withHour(23).withMinute(59).withSecond(59);
    timePeriod = new TimePeriod(dateTime1.getUtilDate(), dateTime2.getUtilDate());
    assertResultArray(new int[] { 0, 23, 59 }, timePeriod.getDurationFields());
    assertResultArray(new int[] { 2, 7, 59 }, timePeriod.getDurationFields(8));
    assertResultArray(new int[] { 0, 23, 59 }, timePeriod.getDurationFields(8, 24));
    assertResultArray(new int[] { 2, 7, 59 }, timePeriod.getDurationFields(8, 22));

    dateTime2 = dateTime2.withDayOfMonth(23).withHour(5).withMinute(30).withSecond(0);
    timePeriod = new TimePeriod(dateTime1.getUtilDate(), dateTime2.getUtilDate());
    assertResultArray(new int[] { 2, 5, 30 }, timePeriod.getDurationFields());
    assertResultArray(new int[] { 6, 5, 30 }, timePeriod.getDurationFields(8));
    assertResultArray(new int[] { 0, 53, 30 }, timePeriod.getDurationFields(8, 54));
    assertResultArray(new int[] { 6, 5, 30 }, timePeriod.getDurationFields(8, 53));
  }

  private void assertResultArray(final int[] required, final int[] result)
  {
    assertEquals( required[0], result[0],"days");
    assertEquals( required[1], result[1],"hours");
    assertEquals( required[2], result[2],"minutes");
  }
}
