package org.projectforge.business.teamcal.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.converter.DOConverter;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.TeamCalFilter;
import org.projectforge.business.teamcal.admin.TeamCalsComparator;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.TeamEventRecurrenceData;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.TeamRecurrenceEvent;
import org.projectforge.business.teamcal.event.model.ReminderActionType;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.calendar.CalendarUtils;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.time.RecurrenceFrequency;
import org.projectforge.model.rest.CalendarEventObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Name;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.property.Version;

@Service
public class TeamCalServiceImpl
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalServiceImpl.class);

  public static final String PARAM_EXPORT_REMINDER = "exportReminders";

  public static final String PARAM_EXPORT_ATTENDEES = "exportAttendees";

  private final TeamCalsComparator calsComparator = new TeamCalsComparator();

  private Collection<TeamCalDO> sortedCals;

  private static final RecurrenceFrequency[] SUPPORTED_FREQUENCIES = new RecurrenceFrequency[] {
      RecurrenceFrequency.NONE,
      RecurrenceFrequency.DAILY, RecurrenceFrequency.WEEKLY, RecurrenceFrequency.MONTHLY, RecurrenceFrequency.YEARLY };

  // needed to convert weeks into days
  private static final int DURATION_OF_WEEK = 7;

  @Autowired
  private TeamCalCache teamCalCache;

  @Autowired
  private CalendarFeedService calendarFeedService;

  @Autowired
  private ConfigurationService configService;

  @Autowired
  private TeamEventService teamEventService;

  @Autowired
  private TeamCalDao teamCalDao;

  public List<Integer> getCalIdList(final Collection<TeamCalDO> teamCals)
  {
    return teamEventService.getCalIdList(teamCals);
  }

  public List<TeamCalDO> getCalList(TeamCalCache teamCalCache, final Collection<Integer> teamCalIds)
  {
    final List<TeamCalDO> list = new ArrayList<TeamCalDO>();
    if (teamCalIds != null) {
      for (final Integer calId : teamCalIds) {
        final TeamCalDO cal = teamCalCache.getCalendar(calId);
        if (cal != null) {
          list.add(cal);
        } else {
          log.warn("Calendar with id " + calId + " not found in cache.");
        }
      }
    }
    return list;
  }

  /**
   * @param calIds
   * @return
   */
  public List<String> getCalendarNames(final String calIds)
  {
    if (StringUtils.isEmpty(calIds) == true) {
      return null;
    }
    final int[] ids = StringHelper.splitToInts(calIds, ",", false);
    final List<String> list = new ArrayList<String>();
    for (final int id : ids) {
      final TeamCalDO cal = teamCalCache.getCalendar(id);
      if (cal != null) {
        list.add(cal.getTitle());
      } else {
        log.warn("TeamCalDO with id '" + id + "' not found. calIds string was: " + calIds);
      }
    }
    return list;
  }

  /**
   * @param calIds
   * @return
   */
  public Collection<TeamCalDO> getSortedCalendars(final String calIds)
  {
    if (StringUtils.isEmpty(calIds) == true) {
      return null;
    }
    sortedCals = new TreeSet<TeamCalDO>(calsComparator);
    final int[] ids = StringHelper.splitToInts(calIds, ",", false);
    for (final int id : ids) {
      final TeamCalDO cal = teamCalCache.getCalendar(id);
      if (cal != null) {
        sortedCals.add(cal);
      } else {
        log.warn("TeamCalDO with id '" + id + "' not found. calIds string was: " + calIds);
      }
    }
    return sortedCals;
  }

  public String getCalendarIds(final Collection<TeamCalDO> calendars)
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final TeamCalDO calendar : calendars) {
      if (calendar.getId() != null) {
        first = StringHelper.append(buf, first, String.valueOf(calendar.getId()), ",");
      }
    }
    return buf.toString();
  }

  public Collection<TeamCalDO> getSortedCalenders()
  {
    if (sortedCals == null) {
      final Collection<TeamCalDO> allCalendars = teamCalCache.getAllAccessibleCalendars();
      sortedCals = new TreeSet<TeamCalDO>(calsComparator);
      for (final TeamCalDO cal : allCalendars) {
        if (cal.isDeleted() == false) {
          sortedCals.add(cal);
        }
      }
    }
    return sortedCals;
  }

  public Collection<VEvent> getConfiguredHolidaysAsVEvent(DateTime holidaysFrom, DateTime holidayTo)
  {
    final List<VEvent> events = new ArrayList<VEvent>();
    DateMidnight day = new DateMidnight(holidaysFrom);
    int idCounter = 0;
    int paranoiaCounter = 0;
    do {
      if (++paranoiaCounter > 4000) {
        log.error(
            "Paranoia counter exceeded! Dear developer, please have a look at the implementation of buildEvents.");
        break;
      }
      final Date date = day.toDate();
      final TimeZone timeZone = day.getZone().toTimeZone();
      final DayHolder dh = new DayHolder(date, timeZone, null);
      if (dh.isHoliday() == false) {
        day = day.plusDays(1);
        continue;
      }

      String title;
      final String holidayInfo = dh.getHolidayInfo();
      if (holidayInfo != null && holidayInfo.startsWith("calendar.holiday.") == true) {
        title = ThreadLocalUserContext.getLocalizedString(holidayInfo);
      } else {
        title = holidayInfo;
      }
      final VEvent vEvent = ICal4JUtils.createVEvent(holidaysFrom.toDate(), holidayTo.toDate(),
          "pf-holiday" + (++idCounter), title, true);
      //      event.setBackgroundColor(backgroundColor);
      //      event.setColor(color);
      //      event.setTextColor(textColor);
      events.add(vEvent);
      day = day.plusDays(1);
    } while (day.isAfter(holidayTo) == false);
    return events;
  }

  public String getUrl(final String teamCalIds, final String additionalParameterString)
  {
    String buf = "&teamCals=" + teamCalIds;
    if (additionalParameterString != null) {
      buf += additionalParameterString;
    }
    return calendarFeedService.getUrl(buf);
  }

  public String getUrl(final Integer teamCalId, final String additionalParameterString)
  {
    return getUrl(teamCalId != null ? teamCalId.toString() : "", additionalParameterString);
  }

  /**
   * @see org.projectforge.business.teamcal.service.calendar.CalendarFeedHook#getEvents(net.fortuna.ical4j.model.TimeZone,
   * java.util.Calendar)
   */
  public List<VEvent> getEvents(final Map<String, String> params, final net.fortuna.ical4j.model.TimeZone timeZone)
  {
    final String teamCals = params.get("teamCals");
    if (teamCals == null) {
      return null;
    }
    final String[] teamCalIds = StringUtils.split(teamCals, ";");
    if (teamCalIds == null) {
      return null;
    }
    final List<VEvent> events = new LinkedList<VEvent>();
    final TeamEventFilter eventFilter = new TeamEventFilter();
    eventFilter.setDeleted(false);
    final DateTime now = DateTime.now();
    final Date eventDateLimit = now.minusYears(1).toDate();
    eventFilter.setStartDate(eventDateLimit);
    final boolean exportReminders = "true".equals(params.get(PARAM_EXPORT_REMINDER)) == true;
    final boolean exportAttendees = "true".equals(params.get(PARAM_EXPORT_ATTENDEES)) == true;
    for (int i = 0; i < teamCalIds.length; i++) {
      final Integer id = Integer.valueOf(teamCalIds[i]);
      eventFilter.setTeamCalId(id);
      final List<TeamEvent> teamEvents = teamEventService.getEventList(eventFilter, false);
      if (teamEvents != null && teamEvents.size() > 0) {
        for (final TeamEvent teamEventObject : teamEvents) {
          if (teamEventObject instanceof TeamEventDO == false) {
            log.warn("Oups, shouldn't occur, please contact the developer: teamEvent isn't of type TeamEventDO: "
                + teamEventObject);
            continue;
          }
          final TeamEventDO teamEvent = (TeamEventDO) teamEventObject;
          final String uid = TeamCalConfig.get().createEventUid(teamEvent.getId());
          final VEvent vEvent = getVEvent(teamEvent, teamCalIds, uid, exportReminders, exportAttendees, timeZone);
          events.add(vEvent);
        }
      }
    }
    return events;
  }

  public VEvent getVEvent(final TeamEventDO teamEvent, final String[] teamCalIds, final String uid, final boolean exportReminders,
      final boolean exportAttendees, final net.fortuna.ical4j.model.TimeZone timeZone)
  {
    String summary;
    if (teamCalIds.length > 1) {
      summary = teamEvent.getSubject() + " (" + teamEvent.getCalendar().getTitle() + ")";
    } else {
      summary = teamEvent.getSubject();
    }
    final VEvent vEvent = ICal4JUtils.createVEvent(teamEvent.getStartDate(), teamEvent.getEndDate(), uid, summary,
        teamEvent.isAllDay());
    if (StringUtils.isNotBlank(teamEvent.getLocation()) == true) {
      vEvent.getProperties().add(new Location(teamEvent.getLocation()));
    }
    vEvent.getProperties().add(new Name(teamEvent.getCalendar().getTitle()));
    if (StringUtils.isNotBlank(teamEvent.getNote()) == true) {
      vEvent.getProperties().add(new Description(teamEvent.getNote()));
    }

    // add alarm if necessary
    if (exportReminders == true && teamEvent.getReminderDuration() != null
        && teamEvent.getReminderActionType() != null) {
      final VAlarm alarm = new VAlarm();
      Dur dur = null;
      // (-1) * needed to set alert before
      if (ReminderDurationUnit.MINUTES.equals(teamEvent.getReminderDurationUnit())) {
        dur = new Dur(0, 0, (-1) * teamEvent.getReminderDuration(), 0);
      } else if (ReminderDurationUnit.HOURS.equals(teamEvent.getReminderDurationUnit())) {
        dur = new Dur(0, (-1) * teamEvent.getReminderDuration(), 0, 0);
      } else if (ReminderDurationUnit.DAYS.equals(teamEvent.getReminderDurationUnit())) {
        dur = new Dur((-1) * teamEvent.getReminderDuration(), 0, 0, 0);
      }
      if (dur != null) {
        alarm.getProperties().add(new Trigger(dur));
        alarm.getProperties().add(new Action(teamEvent.getReminderActionType().getType()));
        vEvent.getAlarms().add(alarm);
      }
    }

    if (teamEvent.hasRecurrence() == true) {
      final Recur recur = teamEvent.getRecurrenceObject();
      if (recur.getUntil() != null) {
        recur.setUntil(
            ICal4JUtils.getICal4jDateTime(CalendarUtils.getEndOfDay(recur.getUntil(), timeZone), timeZone));
      }
      final RRule rrule = new RRule(recur);
      vEvent.getProperties().add(rrule);
      if (teamEvent.getRecurrenceExDate() != null) {
        final List<net.fortuna.ical4j.model.Date> exDates = ICal4JUtils.parseISODateStringsAsICal4jDates(
            teamEvent.getRecurrenceExDate(), timeZone);
        if (CollectionUtils.isEmpty(exDates) == false) {
          for (final net.fortuna.ical4j.model.Date date : exDates) {
            final DateList dateList;
            if (teamEvent.isAllDay() == true) {
              dateList = new DateList(Value.DATE);
            } else {
              dateList = new DateList();
            }
            dateList.add(date);
            ExDate exDate;
            exDate = new ExDate(dateList);
            vEvent.getProperties().add(exDate);
          }
        }
      }
    }
    if (exportAttendees) {
      if (teamEvent.getAttendees() != null) {
        for (TeamEventAttendeeDO a : teamEvent.getAttendees()) {
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
          vEvent.getProperties().add(attendee);
        }
      }
    }
    return vEvent;
  }

  public CalendarEventObject getEventObject(final TeamEvent src, boolean generateICS, final boolean exportAttendees)
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
      event.setIcsData(Base64.encodeBase64String(getIcsFileForTeamEvent(src, exportAttendees).toByteArray()));
    }
    return event;
  }

  public CalendarEventObject getEventObject(final TeamEventDO src, boolean generateICS, final boolean exportAttendees)
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
      event.setIcsData(Base64.encodeBase64String(getIcsFile(src, exportAttendees).toByteArray()));
    }
    return event;
  }

  public ByteArrayOutputStream getIcsFile(final TeamEventDO data, final boolean exportAttendees)
  {
    ByteArrayOutputStream baos = null;

    try {
      baos = new ByteArrayOutputStream();

      final net.fortuna.ical4j.model.Calendar cal = new net.fortuna.ical4j.model.Calendar();
      final Locale locale = ThreadLocalUserContext.getLocale();
      cal.getProperties().add(
          new ProdId("-//" + ThreadLocalUserContext.getUser().getDisplayUsername() + "//ProjectForge//" + locale.toString().toUpperCase()));
      cal.getProperties().add(Version.VERSION_2_0);
      cal.getProperties().add(CalScale.GREGORIAN);

      final net.fortuna.ical4j.model.TimeZone timezone = ICal4JUtils.getUserTimeZone();
      String[] teamCalIds = { data.getCalendar().getPk().toString() };
      VEvent event = getVEvent(data, teamCalIds, data.getUid(), true, exportAttendees, timezone);
      cal.getComponents().add(event);
      CalendarOutputter outputter = new CalendarOutputter();
      outputter.output(cal, baos);
    } catch (IOException | ValidationException e) {
      log.error("Error while exporting calendar event. " + e.getMessage());
      return null;
    }
    return baos;
  }

  public ByteArrayOutputStream getIcsFileForTeamEvent(TeamEvent data, final boolean exportAttendees)
  {
    TeamEventDO eventDO = new TeamEventDO();
    eventDO.setEndDate(new Timestamp(data.getEndDate().getTime()));
    eventDO.setLocation(data.getLocation());
    eventDO.setNote(data.getNote());
    eventDO.setStartDate(new Timestamp(data.getStartDate().getTime()));
    eventDO.setSubject(data.getSubject());
    eventDO.setUid(data.getUid());
    eventDO.setAllDay(data.isAllDay());
    return getIcsFile(eventDO, exportAttendees);
  }

  public static VEvent createVEvent(final TeamEventDO eventDO, final net.fortuna.ical4j.model.TimeZone timezone)
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
    net.fortuna.ical4j.model.TimeZone ical4jTimeZone = null;
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
    final List<net.fortuna.ical4j.model.Date> exDates = ICal4JUtils.parseISODateStringsAsICal4jDates(
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
    } else {
      teamEvent.setLocation("");
    }
    if (event.getDescription() != null) {
      teamEvent.setNote(event.getDescription().getValue());
    } else {
      teamEvent.setNote("");
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
    if (rule != null) {
      teamEvent.setRecurrenceRule(rule.getValue());
    }
    PropertyList exDateProperties = event.getProperties(Property.EXDATE);
    if (exDateProperties != null) {
      List<String> exDateList = new ArrayList<>();
      exDateProperties.forEach(exDateProp -> {
        exDateList.add(exDateProp.getValue());
      });
      teamEvent.setRecurrenceExDate(String.join(",", exDateList));
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

  public List<TeamCalDO> getFullAccessCalendar()
  {
    final TeamCalFilter filter = new TeamCalFilter();
    filter.setFullAccess(true);
    filter.setMinimalAccess(false);
    filter.setReadonlyAccess(false);
    return teamCalDao.getList(filter);
  }

}
