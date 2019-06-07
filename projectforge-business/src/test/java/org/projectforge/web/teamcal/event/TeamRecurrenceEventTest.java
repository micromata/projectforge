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

package org.projectforge.web.teamcal.event;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.projectforge.business.teamcal.event.TeamRecurrenceEvent;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;
import org.junit.jupiter.api.Test;

public class TeamRecurrenceEventTest
{
  @Test
  public void testConstructor()
  {
    final TimeZone timeZone = DateHelper.EUROPE_BERLIN;
    final TeamEventDO master = new TeamEventDO();
    master.setStartDate(getTimestamp("2013-01-01 08:00", timeZone));
    master.setEndDate(getTimestamp("2013-01-01 10:30", timeZone));
    TeamRecurrenceEvent recurEvent = new TeamRecurrenceEvent(master,
        getCalendar(2013, Calendar.JANUARY, 5, 8, 0, timeZone));
    assertDateTime("2013-01-05 08:00", recurEvent.getStartDate(), timeZone);
    assertDateTime("2013-01-05 10:30", recurEvent.getEndDate(), timeZone);

    master.setEndDate(getTimestamp("2013-01-02 10:30", timeZone));
    recurEvent = new TeamRecurrenceEvent(master, getCalendar(2013, Calendar.JANUARY, 5, 8, 0, timeZone));
    assertDateTime("2013-01-05 08:00", recurEvent.getStartDate(), timeZone);
    assertDateTime("2013-01-06 10:30", recurEvent.getEndDate(), timeZone);
  }

  private Timestamp getTimestamp(final String dateString, final TimeZone timeZone)
  {
    final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MINUTES);
    df.setTimeZone(timeZone);
    try {
      return new Timestamp(df.parse(dateString).getTime());
    } catch (final ParseException ex) {
      fail("Can't parse date '" + dateString + "': " + ex.getMessage());
      return null;
    }
  }

  private Calendar getCalendar(final int year, final int month, final int dayOfMonth, final int hourOfDay,
      final int minutes,
      final TimeZone timeZone)
  {
    final Calendar cal = Calendar.getInstance(timeZone);
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month);
    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
    cal.set(Calendar.MINUTE, minutes);
    cal.clear(Calendar.SECOND);
    cal.clear(Calendar.MILLISECOND);
    return cal;
  }

  private void assertDateTime(final String expectedDateTime, final Date date, final TimeZone timeZone)
  {
    final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MINUTES);
    df.setTimeZone(timeZone);
    assertEquals(expectedDateTime, df.format(date));
  }
}
