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

package org.projectforge.web.teamcal.event;

import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.transform.recurrence.Frequency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.projectforge.business.calendar.event.model.ICalendarEvent;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventRecurrenceData;
import org.projectforge.business.teamcal.event.TeamRecurrenceEvent;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.ical.RRuleUtils;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.time.RecurrenceFrequency;
import org.projectforge.business.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

public class TeamEventDaoTest extends AbstractTestBase {
    @Autowired
    private TeamEventDao teamEventDao;

    @BeforeEach
    public void setUp() {
        final String domain = "projectforge.org";
        final Configuration config = Configuration.getInstance();
        config.forceReload();
        config.putParameterManual(ConfigurationParam.CALENDAR_DOMAIN, domain);
    }

    @Test
    public void testRRule() {
        testRRule(DateHelper.EUROPE_BERLIN);
        testRRule(DateHelper.UTC);
        testRRule(TimeZone.getTimeZone("America/Los_Angeles"));
    }

    @Test
    public void recurrenceEvents() {
        final TimeZone timeZone = DateHelper.EUROPE_BERLIN;
        {
            final TeamEventDO event = createEvent(timeZone, "2011-06-06 11:00", "2011-06-06 12:00",
                    RecurrenceFrequency.WEEKLY, 1, "2013-12-31", "2013-12-31 22:59");
            final Collection<ICalendarEvent> col = teamEventDao.rollOutRecurrenceEvents(getDate("2013-10-20", timeZone),
                    getDate("2013-10-29", timeZone), event, timeZone);
            assertEquals(2, col.size());
            final Iterator<ICalendarEvent> it = col.iterator();
            assertEquals(DateHelper.formatAsUTC(DateHelper.parseIsoTimestamp("2013-10-21 11:00:00.0", timeZone)),
                    DateHelper.formatAsUTC(it.next().getStartDate()));
            assertEquals(DateHelper.formatAsUTC(DateHelper.parseIsoTimestamp("2013-10-28 11:00:00.0", timeZone)),
                    DateHelper.formatAsUTC(it.next().getStartDate()));
        }
        {
            // Summer time (-2 hours)
            final TeamEventDO event = createEvent(timeZone, "2011-03-03 00:00", "2011-03-03 00:00",
                    RecurrenceFrequency.WEEKLY, 2, "2011-04-30", "2011-04-30 21:59");
            event.setAllDay(true);
            final Collection<ICalendarEvent> col = teamEventDao.rollOutRecurrenceEvents(getDate("2011-03-01", timeZone),
                    getDate("2011-03-31", timeZone), event, timeZone);
            assertEquals(3, col.size());
            final Iterator<ICalendarEvent> it = col.iterator();
            assertEquals(DateHelper.formatAsUTC(DateHelper.parseIsoTimestamp("2011-03-03 00:00:00.0", timeZone)),
                    DateHelper.formatAsUTC(it.next().getStartDate()));
            assertEquals(DateHelper.formatAsUTC(DateHelper.parseIsoTimestamp("2011-03-17 00:00:00.0", timeZone)),
                    DateHelper.formatAsUTC(it.next().getStartDate()));
        }
    }

    @Test
    public void exDates() {
        testExDates(DateHelper.EUROPE_BERLIN);
        testExDates(TimeZone.getTimeZone("Europe/London"));
        testExDates(TimeZone.getTimeZone("America/Los_Angeles"));
    }

    private void testExDates(final TimeZone timeZone) {
        {
            final TeamEventDO event = createEvent(timeZone, "2013-03-21 20:00", "2013-03-21 21:30",
                    RecurrenceFrequency.WEEKLY, 1, null, null);
            // 21.03., [28.03.], 04.04.
            event.addRecurrenceExDate(parseDateTime("2013-03-28 20:00", DateHelper.UTC));
            final Collection<ICalendarEvent> col = teamEventDao.rollOutRecurrenceEvents(getDate("2013-03-01", timeZone),
                    getDate("2013-04-05", timeZone), event, timeZone);
            assertEquals(2, col.size());
            final Iterator<ICalendarEvent> it = col.iterator();
            assertEquals(DateHelper.formatAsUTC(DateHelper.parseIsoTimestamp("2013-03-21 20:00:00.0", timeZone)),
                    DateHelper.formatAsUTC(it.next().getStartDate()));
            assertEquals(DateHelper.formatAsUTC(DateHelper.parseIsoTimestamp("2013-04-04 20:00:00.0", timeZone)),
                    DateHelper.formatAsUTC(it.next().getStartDate()));
        }
        {
            final TeamEventDO event = createEvent(timeZone, "2013-03-21 00:00", "2013-03-21 00:00",
                    RecurrenceFrequency.WEEKLY, 1, null, null);
            event.setAllDay(true);

            // check count of events without ex date
            final Collection<ICalendarEvent> colWithoutExDate = teamEventDao.rollOutRecurrenceEvents(getDate("2013-03-01", timeZone),
                    getDate("2013-04-05", timeZone), event, timeZone);
            assertEquals(3, colWithoutExDate.size());

            // check cout of events with ex date
            event.addRecurrenceExDate(DateHelper.parseIsoDate("2013-03-28", DateHelper.UTC));
            final Collection<ICalendarEvent> col = teamEventDao.rollOutRecurrenceEvents(getDate("2013-03-01", timeZone),
                    getDate("2013-04-05", timeZone), event, timeZone);
            assertEquals(2, col.size());

            final Iterator<ICalendarEvent> it = col.iterator();
            ICalendarEvent e = it.next();
            assertEquals("2013-03-21 00:00:00.000", DateHelper.formatIsoTimestamp(e.getStartDate(), timeZone));
            assertTrue(e instanceof TeamEventDO);
            e = it.next();
            assertEquals("2013-04-04 00:00:00.000", DateHelper.formatIsoTimestamp(e.getStartDate(), timeZone));
            assertFalse(e instanceof TeamEventDO);
        }
    }

    private void testRRule(final TimeZone timeZone) {
        TeamEventDO event = createEvent(timeZone, "2012-12-21 8:30", "2012-12-21 9:00", null, 1, null, null);
        assertNull(event.getRecurrenceObject());

        event = createEvent(timeZone, "2012-12-21 8:30", "2012-12-21 9:00", RecurrenceFrequency.WEEKLY, 1, null, null);
        assertEquals("FREQ=WEEKLY;INTERVAL=1", event.getRecurrenceRule());
        Collection<ICalendarEvent> events = getRecurrenceEvents("2012-12-01", "2013-01-31", timeZone, event);
        assertEvents(events, timeZone, "2012-12-21 08:30", "2012-12-28 08:30", "2013-01-04 08:30", "2013-01-11 08:30",
                "2013-01-18 08:30", "2013-01-25 08:30");

        String untilInTimeZone = PFDateTime.withDate(2013, Month.JANUARY, 31, 0, 0, 0, 0, timeZone.toZoneId()).getEndOfDay().getIsoString();

        event = createEvent(timeZone, "2012-12-21 18:30", "2012-12-22 9:00", RecurrenceFrequency.WEEKLY, 2,
                "2013-01-31", untilInTimeZone);
        RRule rRule = event.getRecurrenceRuleObject();

        final String utcString = PFDateTime.fromTemporal(RRuleUtils.getRecurUntil(rRule), timeZone.toZoneId()).getIsoString();

        assertEquals(Frequency.WEEKLY, rRule.getRecur().getFrequency());
        assertEquals(untilInTimeZone, utcString);
        assertEquals(2, rRule.getRecur().getInterval());

        events = getRecurrenceEvents("2012-12-01", "2013-03-31", timeZone, event);
        assertEvents(events, timeZone, "2012-12-21 18:30", "2013-01-04 18:30", "2013-01-18 18:30");
        assertTrue(events.iterator().next() instanceof TeamEventDO);
    }

    private TeamEventDO createEvent(final TimeZone timeZone, final String startDate, final String endDate,
                                    final RecurrenceFrequency frequency, final int interval, final String recurrenceUntil, final String expectedRecurrenceUntilInTimeZone) {
        final Date startTimestamp = new Date(parseDateTime(startDate, timeZone).getTime());
        final Date endTimestamp = new Date(parseDateTime(endDate, timeZone).getTime());
        final TeamEventDO event = new TeamEventDO();
        event.setStartDate(startTimestamp);
        event.setEndDate(endTimestamp);
        final TeamEventRecurrenceData recurData = new TeamEventRecurrenceData(timeZone);
        recurData.setFrequency(frequency);
        recurData.setInterval(interval);
        if (recurrenceUntil != null) {
            final Date untilDate = DateHelper.parseIsoDate(recurrenceUntil, timeZone);
            recurData.setUntil(untilDate);
        }
        event.setRecurrence(recurData);
        assertRecurrence(event, timeZone, frequency, interval, expectedRecurrenceUntilInTimeZone);
        return event;
    }

    private void assertRecurrence(final TeamEventDO event, final TimeZone timeZone, final RecurrenceFrequency frequency,
                                  final int interval,
                                  final String expectedRecurrenceUntilInTimeZone) {
        final Recur recur = event.getRecurrenceObject();
        if (frequency == null) {
            assertNull(recur);
            assertNull(event.getRecurrenceUntil());
            return;
        }
        assertEquals(frequency, ICal4JUtils.getFrequency(recur));
        assertEquals(interval, recur.getInterval());
        if (expectedRecurrenceUntilInTimeZone == null) {
            assertNull(event.getRecurrenceUntil());
        } else {
            final String utcString = PFDateTime.from(event.getRecurrenceUntil(), timeZone).getIsoString();
            assertEquals(expectedRecurrenceUntilInTimeZone, utcString, "Recurrence until date is not as expected.");
        }
    }

    private Collection<ICalendarEvent> getRecurrenceEvents(final String startDateString, final String endDateString,
                                                           final TimeZone timeZone,
                                                           final TeamEventDO event) {
        final java.util.Date startDate = DateHelper.parseIsoDate(startDateString, timeZone);
        final java.util.Date endDate = DateHelper.parseIsoDate(endDateString, timeZone);
        return teamEventDao.rollOutRecurrenceEvents(startDate, endDate, event, timeZone);
    }

    private java.util.Date parseDateTime(final String dateString, final TimeZone timeZone) {
        final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MINUTES);
        df.setTimeZone(timeZone);
        try {
            return df.parse(dateString);
        } catch (final ParseException ex) {
            fail("Can't parse dateString '" + dateString + "': " + ex.getMessage());
            return null;
        }
    }

    private void assertEvents(final Collection<ICalendarEvent> events, final TimeZone timeZone, final String... startDates) {
        assertEquals(startDates.length, events.size());
        int i = 0;
        final DateFormat df = new SimpleDateFormat(DateFormats.ISO_TIMESTAMP_MINUTES);
        df.setTimeZone(timeZone);
        for (final ICalendarEvent event : events) {
            if (event instanceof TeamRecurrenceEvent) {
                final long duration = ((TeamRecurrenceEvent) event).getMaster().getDuration();
                assertEquals(duration, event.getEndDate().getTime() - event.getStartDate().getTime());
            }
            final String startDate = startDates[i];
            assertEquals(startDate, df.format(event.getStartDate()));
            ++i;
        }
    }

    Date getDate(final String dateString, final TimeZone timeZone) {
        return DateHelper.parseIsoDate(dateString, timeZone);
    }
}
