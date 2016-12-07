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

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.*;
import net.fortuna.ical4j.model.property.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.converter.DOConverter;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.event.model.*;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.RecurrenceFrequency;
import org.projectforge.model.rest.CalendarEventObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.*;
import java.util.Calendar;

/**
 * For conversion of TeamEvent to CalendarEventObject.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class TeamEventConverter
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamEventConverter.class);

  @Autowired
  private ConfigurationService configService;

  @Autowired
  private TeamEventService teamEventService;

  private static final RecurrenceFrequency[] SUPPORTED_FREQUENCIES = new RecurrenceFrequency[] {
      RecurrenceFrequency.NONE,
      RecurrenceFrequency.DAILY, RecurrenceFrequency.WEEKLY, RecurrenceFrequency.MONTHLY, RecurrenceFrequency.YEARLY };

  // needed to convert weeks into days
  private static final int DURATION_OF_WEEK = 7;

  public CalendarEventObject getEventObject(final TeamEvent src, boolean generateICS)
  {
    if (src == null) {
      return null;
    }
    final CalendarEventObject event = new CalendarEventObject();
    event.setUid(src.getUid());
    event.setStartDate(src.getStartDate());
    event.setEndDate(src.getEndDate());
    event.setLocation(src.getLocation());
    event.setNote(src.getNote());
    event.setSubject(src.getSubject());
    if (src instanceof TeamEventDO) {
      copyFields(event, (TeamEventDO) src);
    } else if (src instanceof TeamRecurrenceEvent) {
      final TeamEventDO master = ((TeamRecurrenceEvent) src).getMaster();
      if (master != null) {
        copyFields(event, master);
      }
    }
    if (generateICS) {
      event.setIcsData(Base64.encodeBase64String(getIcsFileForTeamEvent(src).toByteArray()));
    }
    return event;
  }

  public CalendarEventObject getEventObject(final TeamEventDO src, boolean generateICS)
  {
    if (src == null) {
      return null;
    }
    final CalendarEventObject event = new CalendarEventObject();
    event.setId(src.getPk());
    event.setUid(src.getUid());
    event.setStartDate(src.getStartDate());
    event.setEndDate(src.getEndDate());
    event.setLocation(src.getLocation());
    event.setNote(src.getNote());
    event.setSubject(src.getSubject());
    copyFields(event, src);
    if (generateICS) {
      event.setIcsData(Base64.encodeBase64String(getIcsFile(src).toByteArray()));
    }
    return event;
  }

  public ByteArrayOutputStream getIcsFile(TeamEventDO data)
  {
    ByteArrayOutputStream baos = null;

    try {
      baos = new ByteArrayOutputStream();

      net.fortuna.ical4j.model.Calendar cal = new net.fortuna.ical4j.model.Calendar();
      cal.getProperties().add(new ProdId("-//ProjectForge//iCal4j 1.0//EN"));
      cal.getProperties().add(Version.VERSION_2_0);
      cal.getProperties().add(CalScale.GREGORIAN);

      TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
      TimeZone timezone = registry.getTimeZone(configService.getTimezone().getID());

      VEvent event = null;
      if (data.isAllDay() == false) {
        DateTime start = new net.fortuna.ical4j.model.DateTime(data.getStartDate().getTime());
        start.setTimeZone(timezone);
        DateTime stop = new net.fortuna.ical4j.model.DateTime(data.getEndDate().getTime());
        stop.setTimeZone(timezone);
        event = new VEvent(start, stop, data.getSubject());
      } else {
        Date start = new net.fortuna.ical4j.model.Date(data.getStartDate().getTime());
        final org.joda.time.DateTime jodaTime = new org.joda.time.DateTime(data.getEndDate().getTime());
        final Date stop = new Date(jodaTime.plusDays(+1).toDate());
        event = new VEvent(start, stop, data.getSubject());
      }

      event.getProperties().add(new Description(data.getNote()));
      event.getProperties().add(new Location(data.getLocation()));

      if (data.getRecurrenceRuleObject() != null) {
        RRule rule = data.getRecurrenceRuleObject();
        event.getProperties().add(rule);
      }

      Uid uid = new Uid(data.getUid());
      event.getProperties().add(uid);

      cal.getComponents().add(event);

      if (data.getReminderActionType() != null && data.getReminderDurationUnit() != null) {
        VAlarm alarm = null;
        Dur dur = null;
        switch (data.getReminderDurationUnit()) {
          case DAYS:
            dur = new Dur(data.getReminderDuration() * -1, 0, 0, 0);
            alarm = new VAlarm(dur);
            break;
          case HOURS:
            dur = new Dur(0, data.getReminderDuration() * -1, 0, 0);
            alarm = new VAlarm(dur);
            break;
          case MINUTES:
            dur = new Dur(0, 0, data.getReminderDuration() * -1, 0);
            alarm = new VAlarm(dur);
            break;
          default:
            log.info("No valid reminder duration unit.");

        }
        if (alarm != null) {
          if (data.getReminderActionType().equals(ReminderActionType.MESSAGE)) {
            alarm.getProperties().add(Action.DISPLAY);
          } else if (data.getReminderActionType().equals(ReminderActionType.MESSAGE_SOUND)) {
            alarm.getProperties().add(Action.AUDIO);
          }
          alarm.getProperties().add(new Repeat(1));
          alarm.getProperties().add(new Duration(dur));
          event.getAlarms().add(alarm);
        }
      }

      if (data.getAttendees() != null) {
        for (TeamEventAttendeeDO a : data.getAttendees()) {
          String email = "mailto:";
          if (a.getAddress() != null) {
            email = email + a.getAddress().getEmail();
          } else {
            email = email + a.getUrl();
          }
          Attendee attendee = new Attendee(URI.create(email));
          String cnValue = a.getAddress() != null ? a.getAddress().getFullName() : a.getUrl();
          attendee.getParameters().add(new Cn(cnValue));
          attendee.getParameters().add(CuType.INDIVIDUAL);
          attendee.getParameters().add(Role.CHAIR);
          switch (a.getStatus()) {
            case ACCEPTED:
              attendee.getParameters().add(PartStat.ACCEPTED);
              break;
            case DECLINED:
              attendee.getParameters().add(PartStat.DECLINED);
              break;
            case IN_PROCESS:
            default:
              attendee.getParameters().add(PartStat.IN_PROCESS);
              break;
          }
          event.getProperties().add(attendee);
        }
      }

      CalendarOutputter outputter = new CalendarOutputter();
      //      outputter.setValidating(false);
      outputter.output(cal, baos);
    } catch (IOException | ValidationException e) {
      log.error("Error while exporting calendar event. " + e.getMessage());
      return null;
    }
    return baos;
  }

  public ByteArrayOutputStream getIcsFileForTeamEvent(TeamEvent data)
  {
    TeamEventDO eventDO = new TeamEventDO();
    eventDO.setEndDate(new Timestamp(data.getEndDate().getTime()));
    eventDO.setLocation(data.getLocation());
    eventDO.setNote(data.getNote());
    eventDO.setStartDate(new Timestamp(data.getStartDate().getTime()));
    eventDO.setSubject(data.getSubject());
    eventDO.setUid(data.getUid());
    eventDO.setAllDay(data.isAllDay());
    return getIcsFile(eventDO);
  }

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

  public static Collection<TeamEvent> getRecurrenceEvents(final java.util.Date startDate, final java.util.Date endDate,
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
    java.util.Date eventStartDate = event.getStartDate();
    if (event.isAllDay() == true) {
      // eventStartDate should be midnight in user's time zone.
      eventStartDate = DateHelper.parseIsoDate(eventStartDateString, timeZone);
    }
    if (log.isDebugEnabled() == true) {
      log.debug("---------- startDate=" + DateHelper.formatIsoTimestamp(eventStartDate, timeZone) + ", timeZone="
          + timeZone.getID());
    }
    TimeZone ical4jTimeZone = null;
    try {
      ical4jTimeZone = ICal4JUtils.getTimeZone(timeZone4Calc);
    } catch (final Exception e) {
      log.error("Error getting timezone from ical4j.");
      ical4jTimeZone = ICal4JUtils.getUserTimeZone();
    }
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
    final List<Date> exDates = ICal4JUtils.parseISODateStringsAsICal4jDates(
        event.getRecurrenceExDate(),
        ical4jTimeZone);
    final DateList dateList = recur.getDates(seedDate, ical4jStartDate, ical4jEndDate, Value.DATE_TIME);
    final Collection<TeamEvent> col = new ArrayList<TeamEvent>();
    if (dateList != null) {
      OuterLoop:
      for (final Object obj : dateList) {
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

  public TeamEventDO createTeamEventDO(final VEvent event)
  {
    return createTeamEventDO(event, ThreadLocalUserContext.getTimeZone(), true);
  }

  public TeamEventDO createTeamEventDO(final VEvent event, java.util.TimeZone timeZone)
  {
    return createTeamEventDO(event, timeZone, true);
  }

  public TeamEventDO createTeamEventDO(final VEvent event, java.util.TimeZone timeZone, boolean withUid)
  {
    final TeamEventDO teamEvent = new TeamEventDO();
    teamEvent.setTimeZone(timeZone);
    teamEvent.setCreator(ThreadLocalUserContext.getUser());
    final DtStart dtStart = event.getStartDate();
    final DtEnd dtEnd = event.getEndDate();
    if (dtStart != null && dtStart.getParameter("VALUE") != null && dtStart.getParameter("VALUE").getValue().contains("DATE") == true
        && dtStart.getParameter("VALUE").getValue().contains("DATE-TIME") == false) {
      teamEvent.setAllDay(true);
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
    if (withUid && event.getUid() != null) {
      teamEvent.setUid(event.getUid().getValue());
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

    final List<VAlarm> alarms = event.getAlarms();
    if (alarms != null && alarms.size() >= 1) {
      final VAlarm alarm = alarms.get(0);
      final Dur dur = alarm.getTrigger().getDuration();
      if (alarm.getAction() != null && dur != null) {
        if (Action.AUDIO.equals(alarm.getAction())) {
          teamEvent.setReminderActionType(ReminderActionType.MESSAGE_SOUND);
        } else {
          teamEvent.setReminderActionType(ReminderActionType.MESSAGE);
        }
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
        } else {
          teamEvent.setReminderDuration(15);
          teamEvent.setReminderDurationUnit(ReminderDurationUnit.MINUTES);
        }
      }
    }

    final PropertyList eventAttendees = event.getProperties(Attendee.ATTENDEE);
    if (eventAttendees != null && eventAttendees.size() > 0) {
      Set<String> foundAttendeeEmails = new HashSet<>();
      Integer internalNewAttendeeSequence = -10000;
      List<TeamEventAttendeeDO> attendeesFromDbList = teamEventService.getAddressesAndUserAsAttendee();
      for (int i = 0; i < eventAttendees.size(); i++) {
        Attendee attendee = (Attendee) eventAttendees.get(i);
        String email = null;
        URI attendeeEMailAddressUri = attendee.getCalAddress();
        if (attendeeEMailAddressUri != null) {
          email = attendeeEMailAddressUri.getSchemeSpecificPart();
        }
        if (email != null && EmailValidator.getInstance().isValid(email)) {
          if (foundAttendeeEmails.contains(email) == false) {
            foundAttendeeEmails.add(email);
          }
        } else {
          email = null;
        }
        if (email != null) {
          TeamEventAttendeeDO foundAttendee = null;
          for (TeamEventAttendeeDO dBAttendee : attendeesFromDbList) {
            if (dBAttendee.getAddress().getEmail().equals(email)) {
              foundAttendee = dBAttendee;
              foundAttendee.setId(internalNewAttendeeSequence);
              internalNewAttendeeSequence--;
            }
          }
          if (foundAttendee == null) {
            foundAttendee = new TeamEventAttendeeDO().setUrl(email);
            foundAttendee.setStatus(TeamEventAttendeeStatus.NEW);
            foundAttendee.setId(internalNewAttendeeSequence);
            internalNewAttendeeSequence--;
          }
          teamEvent.addAttendee(foundAttendee);
        }
      }
    }

    final RRule rule = (RRule) event.getProperty(Property.RRULE);
    if (rule != null)

    {
      teamEvent.setRecurrenceRule(rule.getValue());
    }

    final ExDate exDate = (ExDate) event.getProperty(Property.EXDATE);
    if (exDate != null)

    {
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

  public List<TeamEventDO> getTeamEvents(final net.fortuna.ical4j.model.Calendar calendar)
  {
    final List<VEvent> list = getVEvents(calendar);
    final List<TeamEventDO> events = convert(list);
    return events;
  }

  public List<TeamEventDO> convert(final List<VEvent> list)
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
        final java.util.Date startDate1 = o1.getStartDate();
        final java.util.Date startDate2 = o2.getStartDate();
        if (startDate1 == null) {
          if (startDate2 == null) {
            return 0;
          }
          return -1;
        }
        return startDate1.compareTo(startDate2);
      }

      ;
    });
    return events;
  }

  private void copyFields(final CalendarEventObject event, final TeamEventDO src)
  {
    event.setCalendarId(src.getCalendarId());
    event.setRecurrenceRule(src.getRecurrenceRule());
    event.setRecurrenceExDate(src.getRecurrenceExDate());
    event.setRecurrenceUntil(src.getRecurrenceUntil());
    DOConverter.copyFields(event, src);
    if (src.getReminderActionType() != null && src.getReminderDuration() != null
        && src.getReminderDurationUnit() != null) {
      event.setReminderType(src.getReminderActionType().toString());
      event.setReminderDuration(src.getReminderDuration());
      final ReminderDurationUnit unit = src.getReminderDurationUnit();
      event.setReminderUnit(unit.toString());
      final DateHolder date = new DateHolder(src.getStartDate());
      if (unit == ReminderDurationUnit.MINUTES) {
        date.add(Calendar.MINUTE, -src.getReminderDuration());
        event.setReminder(date.getDate());
      } else if (unit == ReminderDurationUnit.HOURS) {
        date.add(Calendar.HOUR, -src.getReminderDuration());
        event.setReminder(date.getDate());
      } else if (unit == ReminderDurationUnit.DAYS) {
        date.add(Calendar.DAY_OF_YEAR, -src.getReminderDuration());
        event.setReminder(date.getDate());
      } else {
        log.warn("ReminderDurationUnit '" + src.getReminderDurationUnit() + "' not yet implemented.");
      }
    }
  }
}
