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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.RecurrenceFrequency;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.parameter.Value;

public class ICal4JUtilsTest
{

  @Test
  public void recurTests()
  {
    final TimeZone timeZone = DateHelper.EUROPE_BERLIN;
    final Recur recur = new Recur();
    recur.setFrequency(ICal4JUtils.getCal4JFrequencyString(RecurrenceFrequency.WEEKLY));
    recur.setUntil(getDate("2013-01-31", timeZone));
    recur.setInterval(2);
    final DateList dateList = recur.getDates(getDate("2013-01-01", timeZone), getDate("2012-01-02", timeZone),
        getDate("2013-03-31", timeZone), Value.TIME);
    Assert.assertEquals(3, dateList.size());
    final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MINUTES);
    df.setTimeZone(timeZone);
    Assert.assertEquals("2013-01-01 00:00", df.format(dateList.get(0)));
    Assert.assertEquals("2013-01-15 00:00", df.format(dateList.get(1)));
    Assert.assertEquals("2013-01-29 00:00", df.format(dateList.get(2)));
  }

  @Test
  public void testSqlDate()
  {
    final net.fortuna.ical4j.model.Date date = ICal4JUtils.getICal4jDate(
        DateHelper.parseIsoDate("2012-12-22", DateHelper.EUROPE_BERLIN),
        DateHelper.EUROPE_BERLIN);
    Assert.assertEquals("20121222", date.toString());
  }

  @Test
  public void parseIsoDate()
  {
    final java.util.Date date = ICal4JUtils.parseISODateString("2013-03-21 08:47:00");
    Assert.assertNotNull(date);
    Assert.assertEquals("2013-03-21 08:47:00", ICal4JUtils.asISODateTimeString(date));
    Assert.assertNull(ICal4JUtils.parseISODateString(null));
    Assert.assertNull(ICal4JUtils.parseISODateString(""));
    Assert.assertNull(ICal4JUtils.asISODateTimeString(null));
  }

  private net.fortuna.ical4j.model.Date getDate(final String dateString, final TimeZone timeZone)
  {
    final java.util.Date date = DateHelper.parseIsoDate(dateString, timeZone);
    return ICal4JUtils.getICal4jDate(date, timeZone);
  }

  @Test
  public void parseISODateStringsAsICal4jDates()
  {
    parseISODateStringsAsICal4jDates(DateHelper.EUROPE_BERLIN);
    parseISODateStringsAsICal4jDates(DateHelper.UTC);
    parseISODateStringsAsICal4jDates(TimeZone.getTimeZone("America/Los_Angeles"));
  }

  private void parseISODateStringsAsICal4jDates(final TimeZone timeZone)
  {
    final List<net.fortuna.ical4j.model.Date> dates = ICal4JUtils.parseISODateStringsAsICal4jDates(
        "2013-03-21 08:47:00,20130327T090000",
        ICal4JUtils.getTimeZone(timeZone));
    Assert.assertEquals(2, dates.size());
    final DateFormat dfLocal = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MINUTES);
    dfLocal.setTimeZone(timeZone);
    final DateFormat dfUtc = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MINUTES);
    dfUtc.setTimeZone(DateHelper.UTC);
    Assert.assertEquals("2013-03-21 08:47", dfUtc.format(dates.get(0)));
    Assert.assertEquals("2013-03-27 09:00", dfLocal.format(dates.get(1)));
  }
}
