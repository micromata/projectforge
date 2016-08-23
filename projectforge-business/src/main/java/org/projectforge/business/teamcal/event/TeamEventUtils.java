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

package org.projectforge.business.teamcal.event;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.RecurrenceFrequency;

import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.RRule;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TeamEventUtils
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamEventUtils.class);

  private static final RecurrenceFrequency[] SUPPORTED_FREQUENCIES = new RecurrenceFrequency[] {
      RecurrenceFrequency.NONE,
      RecurrenceFrequency.DAILY, RecurrenceFrequency.WEEKLY, RecurrenceFrequency.MONTHLY, RecurrenceFrequency.YEARLY };

  // needed to convert weeks into days
  private static final int DURATION_OF_WEEK = 7;

  public static VEvent createVEvent(final TeamEventDO eventDO, final TimeZone timezone)
  {
    final VEvent vEvent = ICal4JUtils.createVEvent(eventDO.getStartDate(), eventDO.getEndDate(), eventDO.getUid(),
        eventDO.getSubject(),
        eventDO.isAllDay(), timezone);
    if (eventDO.hasRecurrence() == true) {
      final RRule rrule = eventDO.getRecurrenceRuleObject();
      vEvent.getProperties().add(rrule);
    }
    return vEvent;
  }

  public static String calculateRRule(final TeamEventRecurrenceData recurData)
  {
    if (recurData == null || recurData.getFrequency() == null || recurData.getFrequency() == RecurrenceFrequency.NONE) {
      return null;
    }
    if (recurData.isCustomized() == false) {
      recurData.setInterval(1);
    }
    final Recur recur = new Recur();
    final net.fortuna.ical4j.model.Date untilDate = ICal4JUtils.getICal4jDate(recurData.getUntil(),
        recurData.getTimeZone());
    if (untilDate != null) {
      recur.setUntil(untilDate);
    }
    recur.setInterval(recurData.getInterval());
    recur.setFrequency(ICal4JUtils.getCal4JFrequencyString(recurData.getFrequency()));
    final RRule rrule = new RRule(recur);
    return rrule.getValue();
  }

  public static Collection<TeamEvent> getRecurrenceEvents(final Date startDate, final Date endDate,
      final TeamEventDO event,
      final java.util.TimeZone timeZone)
  {
    if (event.hasRecurrence() == false) {
      return null;
    }
    final Recur recur = event.getRecurrenceObject();
    if (recur == null) {
      // Shouldn't happen:
      return null;
    }
    final java.util.TimeZone timeZone4Calc = timeZone;
    final String eventStartDateString = event.isAllDay() == true
        ? DateHelper.formatIsoDate(event.getStartDate(), timeZone) : DateHelper
            .formatIsoTimestamp(event.getStartDate(), DateHelper.UTC);
    Date eventStartDate = event.getStartDate();
    if (event.isAllDay() == true) {
      // eventStartDate should be midnight in user's time zone.
      eventStartDate = DateHelper.parseIsoDate(eventStartDateString, timeZone);
    }
    if (log.isDebugEnabled() == true) {
      log.debug("---------- startDate=" + DateHelper.formatIsoTimestamp(eventStartDate, timeZone) + ", timeZone="
          + timeZone.getID());
    }
    final TimeZone ical4jTimeZone = ICal4JUtils.getTimeZone(timeZone4Calc);
    final net.fortuna.ical4j.model.DateTime ical4jStartDate = new net.fortuna.ical4j.model.DateTime(startDate);
    ical4jStartDate.setTimeZone(ical4jTimeZone);
    final net.fortuna.ical4j.model.DateTime ical4jEndDate = new net.fortuna.ical4j.model.DateTime(endDate);
    ical4jEndDate.setTimeZone(ICal4JUtils.getTimeZone(timeZone4Calc));
    final net.fortuna.ical4j.model.DateTime seedDate = new net.fortuna.ical4j.model.DateTime(eventStartDate);
    seedDate.setTimeZone(ICal4JUtils.getTimeZone(timeZone4Calc));
    if (ical4jStartDate == null || ical4jEndDate == null || seedDate == null) {
      log.error("Can't get recurrence events of event "
          + event.getId()
          + ". Not all three dates are given: startDate="
          + ical4jStartDate
          + ", endDate="
          + ical4jEndDate
          + ", seed="
          + seedDate);
      return null;
    }
    final List<net.fortuna.ical4j.model.Date> exDates = ICal4JUtils.parseISODateStringsAsICal4jDates(
        event.getRecurrenceExDate(),
        ical4jTimeZone);
    final DateList dateList = recur.getDates(seedDate, ical4jStartDate, ical4jEndDate, Value.DATE_TIME);
    final Collection<TeamEvent> col = new ArrayList<TeamEvent>();
    if (dateList != null) {
      OuterLoop: for (final Object obj : dateList) {
        final net.fortuna.ical4j.model.DateTime dateTime = (net.fortuna.ical4j.model.DateTime) obj;
        final String isoDateString = event.isAllDay() == true ? DateHelper.formatIsoDate(dateTime, timeZone)
            : DateHelper
                .formatIsoTimestamp(dateTime, DateHelper.UTC);
        if (exDates != null && exDates.size() > 0) {
          for (final net.fortuna.ical4j.model.Date exDate : exDates) {
            if (event.isAllDay() == false) {
              if (exDate.getTime() == dateTime.getTime()) {
                if (log.isDebugEnabled() == true) {
                  log.debug("= ex-dates equals: " + isoDateString + " == " + exDate);
                }
                // this date is part of ex dates, so don't use it.
                continue OuterLoop;
              }
            } else {
              // Allday event.
              final String isoExDateString = DateHelper.formatIsoDate(exDate, DateHelper.UTC);
              if (isoDateString.equals(isoExDateString) == true) {
                if (log.isDebugEnabled() == true) {
                  log.debug("= ex-dates equals: " + isoDateString + " == " + isoExDateString);
                }
                // this date is part of ex dates, so don't use it.
                continue OuterLoop;
              }
            }
            if (log.isDebugEnabled() == true) {
              log.debug("ex-dates not equals: " + isoDateString + " != " + exDate);
            }
          }
        }
        if (isoDateString.equals(eventStartDateString) == true) {
          // Put event itself to the list.
          col.add(event);
        } else {
          // Now we need this event as date with the user's time-zone.
          final Calendar userCal = Calendar.getInstance(timeZone);
          userCal.setTime(dateTime);
          final TeamRecurrenceEvent recurEvent = new TeamRecurrenceEvent(event, userCal);
          col.add(recurEvent);
        }
      }
    }
    if (log.isDebugEnabled() == true) {
      for (final TeamEvent ev : col) {
        log.debug("startDate="
            + DateHelper.formatIsoTimestamp(ev.getStartDate(), timeZone)
            + "; "
            + DateHelper.formatAsUTC(ev.getStartDate())
            + ", endDate="
            + DateHelper.formatIsoTimestamp(ev.getStartDate(), timeZone)
            + "; "
            + DateHelper.formatAsUTC(ev.getEndDate()));
      }
    }
    return col;
  }

  public static TeamEventDO createTeamEventDO(final VEvent event)
  {
    return createTeamEventDO(event, ThreadLocalUserContext.getTimeZone());
  }

  public static TeamEventDO createTeamEventDO(final VEvent event, java.util.TimeZone timeZone)
  {
    final TeamEventDO teamEvent = new TeamEventDO();
    teamEvent.setTimeZone(timeZone);
    final DtStart dtStart = event.getStartDate();
    final DtEnd dtEnd = event.getEndDate();
    if (dtStart != null && dtEnd == null) {
      if (dtStart.getValue().contains("VALUE=DATE") == true
          && dtStart.getValue().contains("VALUE=DATE-TIME") == false) {
        teamEvent.setAllDay(true);
      }
    }
    Timestamp timestamp = ICal4JUtils.getSqlTimestamp(dtStart.getDate());
    teamEvent.setStartDate(timestamp);
    if (teamEvent.isAllDay() == true) {
      final org.joda.time.DateTime jodaTime = new org.joda.time.DateTime(event.getEndDate().getDate());
      final net.fortuna.ical4j.model.Date fortunaEndDate = new net.fortuna.ical4j.model.Date(
          jodaTime.plusDays(-1).toDate());
      timestamp = new Timestamp(fortunaEndDate.getTime());
    } else {
      timestamp = ICal4JUtils.getSqlTimestamp(event.getEndDate().getDate());
    }
    teamEvent.setEndDate(timestamp);
    if (event.getUid() != null) {
      teamEvent.setExternalUid(event.getUid().getValue());
    }
    if (event.getLocation() != null) {
      teamEvent.setLocation(event.getLocation().getValue());
    }
    if (event.getDescription() != null) {
      teamEvent.setNote(event.getDescription().getValue());
    }
    if (event.getSummary() != null) {
      teamEvent.setSubject(event.getSummary().getValue());
    } else {
      teamEvent.setSubject("");
    }
    if (event.getOrganizer() != null) {
      teamEvent.setOrganizer(event.getOrganizer().getValue());
    }
    @SuppressWarnings("unchecked")
    final List<VAlarm> alarms = event.getAlarms();
    if (alarms != null && alarms.size() >= 1) {
      final Dur dur = alarms.get(0).getTrigger().getDuration();
      if (dur != null) { // Might be null.
        // consider weeks
        int weeksToDays = 0;
        if (dur.getWeeks() != 0) {
          weeksToDays = dur.getWeeks() * DURATION_OF_WEEK;
        }
        if (dur.getDays() != 0) {
          teamEvent.setReminderDuration(dur.getDays() + weeksToDays);
          teamEvent.setReminderDurationUnit(ReminderDurationUnit.DAYS);
        } else if (dur.getHours() != 0) {
          teamEvent.setReminderDuration(dur.getHours());
          teamEvent.setReminderDurationUnit(ReminderDurationUnit.HOURS);
        } else if (dur.getMinutes() != 0) {
          teamEvent.setReminderDuration(dur.getMinutes());
          teamEvent.setReminderDurationUnit(ReminderDurationUnit.MINUTES);
        }
      }
    }
    final RRule rule = (RRule) event.getProperty(Property.RRULE);
    if (rule != null) {
      teamEvent.setRecurrenceRule(rule.getValue());
    }
    final ExDate exDate = (ExDate) event.getProperty(Property.EXDATE);
    if (exDate != null) {
      teamEvent.setRecurrenceExDate(exDate.getValue());
    }
    return teamEvent;
  }

  public static RecurrenceFrequency[] getSupportedRecurrenceFrequencies()
  {
    return SUPPORTED_FREQUENCIES;
  }

  public static List<VEvent> getVEvents(final net.fortuna.ical4j.model.Calendar calendar)
  {
    final List<VEvent> events = new ArrayList<VEvent>();
    @SuppressWarnings("unchecked")
    final List<CalendarComponent> list = calendar.getComponents(Component.VEVENT);
    if (list == null || list.size() == 0) {
      return events;
    }
    // Temporary not used, because multiple events are not supported.
    for (final Component c : list) {
      final VEvent event = (VEvent) c;

      if (StringUtils.equals(event.getSummary().getValue(), TeamCalConfig.SETUP_EVENT) == true) {
        // skip setup event!
        continue;
      }
      events.add(event);
    }
    return events;
  }

  public static List<TeamEventDO> getTeamEvents(final net.fortuna.ical4j.model.Calendar calendar)
  {
    final List<VEvent> list = getVEvents(calendar);
    final List<TeamEventDO> events = convert(list);
    return events;
  }

  public static List<TeamEventDO> convert(final List<VEvent> list)
  {
    final List<TeamEventDO> events = new ArrayList<TeamEventDO>();
    if (list == null || list.size() == 0) {
      return events;
    }
    for (final VEvent vEvent : list) {
      events.add(createTeamEventDO(vEvent));
    }
    Collections.sort(events, new Comparator<TeamEventDO>()
    {
      public int compare(final TeamEventDO o1, final TeamEventDO o2)
      {
        final Date startDate1 = o1.getStartDate();
        final Date startDate2 = o2.getStartDate();
        if (startDate1 == null) {
          if (startDate2 == null) {
            return 0;
          }
          return -1;
        }
        return startDate1.compareTo(startDate2);
      };
    });
    return events;
  }
}
