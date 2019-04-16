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

package org.projectforge.timesheet;

import org.junit.jupiter.api.Test;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetStats;
import org.projectforge.business.timesheet.TimesheetUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.test.AbstractTestBase;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TimesheetUtilsTest extends AbstractTestBase {
  private TimeZone timeZone;

  private Locale locale;

  @Test
  public void testBeginOfTimesheets() {
    timeZone = DateHelper.EUROPE_BERLIN;
    locale = new Locale("DE_de");
    ThreadLocalUserContext.setUser(getUserGroupCache(), new PFUserDO().setTimeZone(timeZone).setLocale(locale));
    final PFUserDO user1 = new PFUserDO();
    user1.setId(1);
    final PFUserDO user2 = new PFUserDO();
    user1.setId(2);
    final List<TimesheetDO> list = new LinkedList<TimesheetDO>();
    add(list, user1, "2013-07-20 12:15", "2013-07-20 13:00");
    add(list, user2, "2013-07-20 10:00", "2013-07-20 16:00");
    add(list, user2, "2013-07-20 08:00", "2013-07-20 09:00");
    add(list, user2, "2013-07-20 17:00", "2013-07-20 19:00");

    add(list, user1, "2013-07-22 12:00", "2013-07-22 13:00");
    add(list, user1, "2013-07-21 23:00", "2013-07-22 06:00");

    add(list, user1, "2013-07-25 12:00", "2013-07-25 13:00");
    add(list, user1, "2013-07-24 23:00", "2013-07-25 06:00");
    add(list, user1, "2013-07-25 23:00", "2013-07-26 06:00");

    assertStats("2013-07-20 12:15", "2013-07-20 13:00", 45, 0,
            TimesheetUtils.getStats(list, parseDate("2013-07-20"), user1.getId()));
    assertStats("2013-07-20 08:00", "2013-07-20 19:00", 9 * 60, 120,
            TimesheetUtils.getStats(list, parseDate("2013-07-20"), user2.getId()));

    assertStats("2013-07-22 00:00", "2013-07-22 13:00", 7 * 60, 6 * 60,
            TimesheetUtils.getStats(list, parseDate("2013-07-22"), user1.getId()));

    assertStats("2013-07-25 00:00", "2013-07-26 00:00", 8 * 60, 16 * 60,
            TimesheetUtils.getStats(list, parseDate("2013-07-25"), user1.getId()));

    assertNull(TimesheetUtils.getStats(list, parseDate("2013-07-19"), user1.getId()).getEarliestStartDate());
  }

  private void add(final List<TimesheetDO> list, final PFUserDO user, final String start, final String stop) {
    final Date startDate = parseTimestamp(start);
    final Date stopDate = parseTimestamp(stop);
    final TimesheetDO ts = new TimesheetDO();
    ts.setStartDate(startDate);
    ts.setStopDate(stopDate);
    ts.setUser(user);
    list.add(ts);
  }

  private void assertStats(final String expectedEarliestStartDate, final String expectedEarliestStopDate,
                           final long expectedTotalMinutes,
                           final long expectedBreakMinutes, final TimesheetStats stats) {
    assertTimestamp("earliest start date", expectedEarliestStartDate, stats.getEarliestStartDate());
    assertTimestamp("latest stop date", expectedEarliestStopDate, stats.getLatestStopDate());
    assertEquals(expectedTotalMinutes, stats.getTotalMillis() / 60000, "total millis");
    assertEquals(expectedBreakMinutes, stats.getTotalBreakMillis() / 60000, "total break millis");
  }

  private void assertTimestamp(final String title, final String expected, final Date date) {
    assertNotNull(date);
    final String suffix = ":00.000"; //expected.endsWith(":59") == true ? ":59.999" : ":00.000";
    assertEquals(expected + suffix, DateHelper.formatIsoTimestamp(date, timeZone), title);
  }

  private Date parseTimestamp(final String dateString) {
    final Date result = DateHelper.parseIsoTimestamp(dateString + ":00.000", timeZone);
    assertNotNull(result);
    return result;
  }

  private Date parseDate(final String dateString) {
    final Date result = DateHelper.parseIsoTimestamp(dateString + " 08:00:00.000", timeZone);
    assertNotNull(result);
    return result;
  }
}
