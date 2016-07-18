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

import static org.testng.AssertJUnit.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.projectforge.framework.configuration.ConfigXmlTest;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DatePrecision;
import org.projectforge.framework.time.TimePeriod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TimePeriodTest
{
  //private final static Logger log = Logger.getLogger(WeekHolderTest.class);

  @BeforeClass
  public static void setUp()
  {
    // Needed if this tests runs before the ConfigurationTest.
    ConfigXmlTest.createTestConfiguration();
  }

  @Test
  public void testTimePeriod()
  {
    final DateHolder date1 = new DateHolder(new Date(), DatePrecision.MINUTE, Locale.GERMAN);
    date1.setDate(1970, Calendar.NOVEMBER, 21, 0, 0, 0);

    final DateHolder date2 = new DateHolder(new Date(), DatePrecision.MINUTE, Locale.GERMAN);
    date2.setDate(1970, Calendar.NOVEMBER, 21, 10, 0, 0);

    TimePeriod timePeriod = new TimePeriod(date1.getDate(), date2.getDate());
    assertResultArray(new int[] { 0, 10, 0 }, timePeriod.getDurationFields());
    assertResultArray(new int[] { 1, 2, 0 }, timePeriod.getDurationFields(8));

    date2.setDate(1970, Calendar.NOVEMBER, 22, 0, 0, 0);
    timePeriod = new TimePeriod(date1.getDate(), date2.getDate());
    assertResultArray(new int[] { 1, 0, 0 }, timePeriod.getDurationFields());
    assertResultArray(new int[] { 3, 0, 0 }, timePeriod.getDurationFields(8));
    assertResultArray(new int[] { 0, 24, 0 }, timePeriod.getDurationFields(8, 25));
    assertResultArray(new int[] { 3, 0, 0 }, timePeriod.getDurationFields(8, 24));

    date2.setDate(1970, Calendar.NOVEMBER, 21, 23, 59, 59);
    timePeriod = new TimePeriod(date1.getDate(), date2.getDate());
    assertResultArray(new int[] { 0, 23, 59 }, timePeriod.getDurationFields());
    assertResultArray(new int[] { 2, 7, 59 }, timePeriod.getDurationFields(8));
    assertResultArray(new int[] { 0, 23, 59 }, timePeriod.getDurationFields(8, 24));
    assertResultArray(new int[] { 2, 7, 59 }, timePeriod.getDurationFields(8, 22));

    date2.setDate(1970, Calendar.NOVEMBER, 23, 5, 30, 0);
    timePeriod = new TimePeriod(date1.getDate(), date2.getDate());
    assertResultArray(new int[] { 2, 5, 30 }, timePeriod.getDurationFields());
    assertResultArray(new int[] { 6, 5, 30 }, timePeriod.getDurationFields(8));
    assertResultArray(new int[] { 0, 53, 30 }, timePeriod.getDurationFields(8, 54));
    assertResultArray(new int[] { 6, 5, 30 }, timePeriod.getDurationFields(8, 53));
  }

  private void assertResultArray(final int[] required, final int[] result)
  {
    assertEquals("days", required[0], result[0]);
    assertEquals("hours", required[1], result[1]);
    assertEquals("minutes", required[2], result[2]);
  }
}
