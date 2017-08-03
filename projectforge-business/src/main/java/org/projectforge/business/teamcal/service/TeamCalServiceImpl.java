package org.projectforge.business.teamcal.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.projectforge.business.teamcal.admin.TeamCalsComparator;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.RecurrenceFrequencyModeOne;
import org.projectforge.business.teamcal.event.RecurrenceFrequencyModeTwo;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.TeamRecurrenceEvent;
import org.projectforge.business.teamcal.event.model.ReminderActionType;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
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
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterFactoryImpl;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Created;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Sequence;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.property.Version;

@Service
public class TeamCalServiceImpl
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalServiceImpl.class);

  public static final String PARAM_EXPORT_REMINDER = "exportReminders";

  public static final String PARAM_EXPORT_ATTENDEES = "exportAttendees";

  private static final List<String> stepOver = Arrays.asList(Parameter.CN, Parameter.CUTYPE, Parameter.PARTSTAT, Parameter.RSVP, Parameter.ROLE);

  private final TeamCalsComparator calsComparator = new TeamCalsComparator();

  private Collection<TeamCalDO> sortedCals;

  private static final RecurrenceFrequency[] SUPPORTED_FREQUENCIES = new RecurrenceFrequency[] {
      RecurrenceFrequency.NONE,
      RecurrenceFrequency.DAILY, RecurrenceFrequency.WEEKLY, RecurrenceFrequency.MONTHLY, RecurrenceFrequency.YEARLY };

  private static final RecurrenceFrequencyModeOne[] SUPPORTED_MODE_ONE = new RecurrenceFrequencyModeOne[] {
      RecurrenceFrequencyModeOne.FIRST, RecurrenceFrequencyModeOne.SECOND, RecurrenceFrequencyModeOne.THIRD, RecurrenceFrequencyModeOne.FOURTH,
      RecurrenceFrequencyModeOne.FIFTH };

  private static final RecurrenceFrequencyModeTwo[] SUPPORTED_MODE_TWO = new RecurrenceFrequencyModeTwo[] {
      RecurrenceFrequencyModeTwo.MONDAY, RecurrenceFrequencyModeTwo.TUESDAY, RecurrenceFrequencyModeTwo.WEDNESDAY, RecurrenceFrequencyModeTwo.THURSDAY,
      RecurrenceFrequencyModeTwo.FRIDAY, RecurrenceFrequencyModeTwo.SATURDAY, RecurrenceFrequencyModeTwo.SUNDAY,
      RecurrenceFrequencyModeTwo.DAY, RecurrenceFrequencyModeTwo.WEEKDAY, RecurrenceFrequencyModeTwo.WEEKEND };

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
    for (final String teamCalId : teamCalIds) {
      final Integer id = Integer.valueOf(teamCalId);
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
          final VEvent vEvent = createVEvent(teamEvent, teamCalIds, exportReminders, exportAttendees, false, null);
          events.add(vEvent);
        }
      }
    }
    return events;
  }

  private VEvent createVEvent(final TeamEventDO teamEvent, final String[] teamCalIds, final boolean exportReminders,
      final boolean exportAttendees, final boolean editable, Method method)
  {
    final String summary;
    if (teamCalIds.length > 1) {
      summary = teamEvent.getSubject() + " (" + teamEvent.getCalendar().getTitle() + ")";
    } else {
      summary = teamEvent.getSubject();
    }

    // create vEvent
    final VEvent vEvent = ICal4JUtils.createVEvent(teamEvent.getStartDate(), teamEvent.getEndDate(), teamEvent.getUid(), summary, teamEvent.isAllDay());
    if (StringUtils.isNotBlank(teamEvent.getLocation()) == true) {
      vEvent.getProperties().add(new Location(teamEvent.getLocation()));
    }

    // set created
    net.fortuna.ical4j.model.DateTime created = new net.fortuna.ical4j.model.DateTime(teamEvent.getCreated());
    created.setUtc(true);
    vEvent.getProperties().add(new Created(created));

    // set DTSTAMP
    net.fortuna.ical4j.model.DateTime dtStampValue = new net.fortuna.ical4j.model.DateTime(teamEvent.getDtStamp());
    dtStampValue.setUtc(true);
    DtStamp dtStamp = (DtStamp) vEvent.getProperties().getProperty(Property.DTSTAMP);
    if (dtStamp != null) {
      dtStamp.setDate(dtStampValue);
    } else {
      vEvent.getProperties().add(new DtStamp(dtStampValue));
    }

    // set last edit
    //    net.fortuna.ical4j.model.DateTime lastModified = new net.fortuna.ical4j.model.DateTime(
    //        teamEvent.getDtStamp() != null ? teamEvent.getDtStamp() : teamEvent.getCreated());
    //    created.setUtc(true);
    //    vEvent.getProperties().add(new LastModified(lastModified));

    // set sequence number
    if (teamEvent.getSequence() != null) {
      vEvent.getProperties().add(new Sequence(teamEvent.getSequence()));
    } else {
      vEvent.getProperties().add(new Sequence(0));
    }

    // add owner
    Organizer organizer = null;

    try {
      if (teamEvent.isOwnership() != null && teamEvent.isOwnership()) {
        ParameterList param = new ParameterList();
        param.add(new Cn(teamEvent.getCreator().getFullname()));
        param.add(CuType.INDIVIDUAL);
        param.add(Role.CHAIR);
        param.add(PartStat.ACCEPTED);
        if (editable) {
          organizer = new Organizer(param, "mailto:null");
        } else {
          organizer = new Organizer(param, "mailto:" + teamEvent.getCreator().getEmail());
        }
      } else if (teamEvent.getOrganizer() != null) {
        // read owner from
        ParameterList param = new ParameterList();
        this.parseAdditionalParameters(param, teamEvent.getOrganizerAdditionalParams());
        if (param.getParameter(Parameter.CUTYPE) == null) {
          param.add(CuType.INDIVIDUAL);
        }
        if (param.getParameter(Parameter.ROLE) == null) {
          param.add(Role.CHAIR);
        }
        if (param.getParameter(Parameter.PARTSTAT) == null) {
          param.add(PartStat.ACCEPTED);
        }
        organizer = new Organizer(param, teamEvent.getOrganizer());
      } else {
        // TODO use better default value here
        organizer = new Organizer("mailto:null");
      }
    } catch (URISyntaxException e) {
      // TODO handle exception, write default?
      // e.printStackTrace();
    }

    vEvent.getProperties().add(organizer);

    // stop is method is cancel
    if (method == Method.CANCEL) {
      return vEvent;
    }

    // set note
    if (StringUtils.isNotBlank(teamEvent.getNote()) == true) {
      vEvent.getProperties().add(new Description(teamEvent.getNote()));
    }

    // set visibility
    // TODO vEvent.getProperties().add(Transp.OPAQUE);

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
      final RRule rrule = new RRule(recur);
      vEvent.getProperties().add(rrule);

      if (teamEvent.getRecurrenceExDate() != null) {
        final List<net.fortuna.ical4j.model.Date> exDates = ICal4JUtils.parseCSVDatesAsICal4jDates(
            teamEvent.getRecurrenceExDate(), (false == teamEvent.isAllDay()), ICal4JUtils.getUTCTimeZone());
        if (CollectionUtils.isEmpty(exDates) == false) {
          for (final net.fortuna.ical4j.model.Date date : exDates) {
            final DateList dateList;
            if (teamEvent.isAllDay() == true) {
              dateList = new DateList(Value.DATE);
            } else {
              dateList = new DateList();
              dateList.setUtc(true);
            }

            dateList.add(date);
            ExDate exDate;
            exDate = new ExDate(dateList);
            vEvent.getProperties().add(exDate);
          }
        }
      }
    }

    if (exportAttendees && teamEvent.getAttendees() != null) {
      // TODO add organizer user, most likely as chair
      for (TeamEventAttendeeDO a : teamEvent.getAttendees()) {
        String email = "mailto:" + (a.getAddress() != null ? a.getAddress().getEmail() : a.getUrl());

        Attendee attendee = new Attendee(URI.create(email));

        // set common name
        if (a.getAddress() != null) {
          attendee.getParameters().add(new Cn(a.getAddress().getFullName()));
        } else if (a.getCommonName() != null) {
          attendee.getParameters().add(new Cn(a.getCommonName()));
        } else {
          attendee.getParameters().add(new Cn(a.getUrl()));
        }

        attendee.getParameters().add(a.getCuType() != null ? new CuType(a.getCuType()) : CuType.INDIVIDUAL);
        attendee.getParameters().add(a.getRole() != null ? new Role(a.getRole()) : Role.REQ_PARTICIPANT);
        if (a.getRsvp() != null) {
          attendee.getParameters().add(new Rsvp(a.getRsvp()));
        }
        attendee.getParameters().add(a.getStatus() != null ? a.getStatus().getPartStat() : PartStat.NEEDS_ACTION);
        this.parseAdditionalParameters(attendee.getParameters(), a.getAdditionalParams());

        vEvent.getProperties().add(attendee);
      }
    }

    return vEvent;
  }

  private void parseAdditionalParameters(final ParameterList list, final String additonalParams)
  {
    if (list == null || additonalParams == null) {
      return;
    }

    ParameterFactoryImpl parameterFactory = ParameterFactoryImpl.getInstance();
    StringBuilder sb = new StringBuilder();
    boolean escaped = false;
    char[] chars = additonalParams.toCharArray();
    String name = null;

    for (char c : chars) {
      switch (c) {
        case ';':
          if (escaped == false && name != null && sb.length() > 0) {
            try {
              list.add(parameterFactory.createParameter(name, sb.toString().replaceAll("\"", "")));
            } catch (URISyntaxException e) {
              // TODO
              e.printStackTrace();
            }
            name = null;
            sb.setLength(0);
          }
          break;
        case '"':
          escaped = (escaped == false);
          break;
        case '=':
          if (escaped == false && sb.length() > 0) {
            name = sb.toString();
            sb.setLength(0);
          }
          break;
        default:
          sb.append(c);
          break;
      }
    }

    if (escaped == false && name != null && sb.length() > 0) {
      try {
        list.add(parameterFactory.createParameter(name, sb.toString().replaceAll("\"", "")));
      } catch (URISyntaxException e) {
        // TODO
        e.printStackTrace();
      }
    }
  }

  public CalendarEventObject getEventObject(final TeamEvent src, boolean generateICS, final boolean exportAttendees, final boolean editable)
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
      event.setIcsData(Base64.encodeBase64String(getIcsFileForTeamEvent(src, exportAttendees, editable).toByteArray()));
    }
    return event;
  }

  public CalendarEventObject getEventObject(final TeamEventDO src, boolean generateICS, final boolean exportAttendees, final boolean editable)
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
      event.setIcsData(Base64.encodeBase64String(getIcsFile(src, exportAttendees, editable, null).toByteArray()));
    }
    return event;
  }

  public ByteArrayOutputStream getIcsFile(final TeamEventDO data, final boolean exportAttendees, final boolean editable, final Method method)
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
      if (method != null) {
        cal.getProperties().add(method);
      }

      final net.fortuna.ical4j.model.TimeZone timezone = ICal4JUtils.getUserTimeZone();
      cal.getComponents().add(timezone.getVTimeZone());
      final String[] teamCalIds = { data.getCalendar().getPk().toString() };
      final VEvent event = createVEvent(data, teamCalIds, true, exportAttendees, editable, method);
      cal.getComponents().add(event);
      CalendarOutputter outputter = new CalendarOutputter();
      outputter.output(cal, baos);
    } catch (IOException e) {
      log.error("Error while exporting calendar event. " + e.getMessage());
      return null;
    }
    return baos;
  }

  public ByteArrayOutputStream getIcsFileForTeamEvent(TeamEvent data, final boolean exportAttendees, final boolean editable)
  {
    TeamEventDO eventDO = new TeamEventDO();
    eventDO.setEndDate(new Timestamp(data.getEndDate().getTime()));
    eventDO.setLocation(data.getLocation());
    eventDO.setNote(data.getNote());
    eventDO.setStartDate(new Timestamp(data.getStartDate().getTime()));
    eventDO.setSubject(data.getSubject());
    eventDO.setUid(data.getUid());
    eventDO.setAllDay(data.isAllDay());
    return getIcsFile(eventDO, exportAttendees, editable, null);
  }

  public static Collection<TeamEvent> getRecurrenceEvents(final java.util.Date startDate, final java.util.Date endDate,
      final TeamEventDO event, final java.util.TimeZone timeZone)
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

    // get ex dates of event
    final List<Date> exDates = ICal4JUtils.parseCSVDatesAsJavaUtilDates(event.getRecurrenceExDate(), DateHelper.UTC);

    // get events in time range
    final DateList dateList = recur.getDates(seedDate, ical4jStartDate, ical4jEndDate, Value.DATE_TIME);

    // remove ex range values
    final Collection<TeamEvent> col = new ArrayList<>();
    if (dateList != null) {
      OuterLoop:
      for (final Object obj : dateList) {
        final net.fortuna.ical4j.model.DateTime dateTime = (net.fortuna.ical4j.model.DateTime) obj;
        final String isoDateString = event.isAllDay() == true ? DateHelper.formatIsoDate(dateTime, timeZone)
            : DateHelper.formatIsoTimestamp(dateTime, DateHelper.UTC);
        if (exDates != null && exDates.size() > 0) {
          for (Date exDate : exDates) {
            if (event.isAllDay() == false) {
              Date recurDateJavaUtil = new Date(dateTime.getTime());
              if (recurDateJavaUtil.equals(exDate)) {
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
                  log.debug(String.format("= ex-dates equals: %s == %s", isoDateString, isoExDateString));
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
    final PFUserDO user = ThreadLocalUserContext.getUser();
    final DtStart dtStart = event.getStartDate();

    teamEvent.setCreator(user);
    teamEvent.setAllDay(dtStart != null && dtStart.getDate() instanceof net.fortuna.ical4j.model.DateTime == false);
    teamEvent.setStartDate(ICal4JUtils.getSqlTimestamp(dtStart.getDate()));
    if (teamEvent.isAllDay()) {
      // TODO sn change behaviour to iCal standard
      final org.joda.time.DateTime jodaTime = new org.joda.time.DateTime(event.getEndDate().getDate());
      final net.fortuna.ical4j.model.Date fortunaEndDate = new net.fortuna.ical4j.model.Date(jodaTime.plusDays(-1).toDate());
      teamEvent.setEndDate(new Timestamp(fortunaEndDate.getTime()));
    } else {
      teamEvent.setEndDate(ICal4JUtils.getSqlTimestamp(event.getEndDate().getDate()));
    }

    if (withUid && event.getUid() != null && StringUtils.isEmpty(event.getUid().getValue()) == false) {
      teamEvent.setUid(event.getUid().getValue());
    }

    teamEvent.setDtStamp(event.getDateStamp() != null ? new Timestamp(event.getDateStamp().getDate().getTime()) : null);
    teamEvent.setLocation(event.getLocation() != null ? event.getLocation().getValue() : null);
    teamEvent.setNote(event.getDescription() != null ? event.getDescription().getValue() : null);
    teamEvent.setSubject(event.getSummary() != null ? event.getSummary().getValue() : null);

    boolean ownership = false;

    Organizer organizer = event.getOrganizer();
    if (organizer != null) {
      Parameter organizerCNParam = organizer.getParameter(Parameter.CN);
      Parameter organizerMailParam = organizer.getParameter("EMAIL");

      String organizerCN = organizerCNParam != null ? organizerCNParam.getValue() : null;
      String organizerEMail = organizerMailParam != null ? organizerMailParam.getValue() : null;
      String organizerValue = organizer.getValue();

      // determine ownership
      if (user != null) {
        if ("mailto:null".equals(organizerValue)) {
          // owner mail to is missing (apple calender tool)
          ownership = true;
        } else if (organizerCN != null && organizerCN.equals(user.getUsername())) {
          // organizer name is user name
          ownership = true;
        } else if (organizerEMail != null && organizerEMail.equals(user.getEmail())) {
          // organizer email is user email
          ownership = true;
        }
      }

      // further parameters
      StringBuilder sb = new StringBuilder();
      Iterator<Parameter> iter = organizer.getParameters().iterator();

      while (iter.hasNext()) {
        final Parameter param = iter.next();
        if (param.getName() == null) {
          continue;
        }

        sb.append(";");
        sb.append(param.toString());
      }

      if (sb.length() > 0) {
        // remove first ';'
        teamEvent.setOrganizerAdditionalParams(sb.substring(1));
      }

      if ("mailto:null".equals(organizerValue) == false) {
        teamEvent.setOrganizer(organizer.getValue());
      }
    } else {
      // some clients, such as thunderbird lightning, does not send an organizer -> pf has ownership
      ownership = true;
    }

    teamEvent.setOwnership(ownership);

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
      Integer internalNewAttendeeSequence = -10000;

      List<TeamEventAttendeeDO> attendeesFromDbList = teamEventService.getAddressesAndUserAsAttendee();
      for (int i = 0; i < eventAttendees.size(); i++) {
        Attendee attendee = (Attendee) eventAttendees.get(i);
        URI attendeeUri = attendee.getCalAddress();
        final String email = (attendeeUri != null) ? attendeeUri.getSchemeSpecificPart() : null;

        if (email != null && EmailValidator.getInstance().isValid(email) == false) {
          continue; // TODO maybe validation is not necessary, could also be en url? check rfc
        }

        TeamEventAttendeeDO attendeeDO = null;

        // search for eMail in DB as possible attendee
        for (TeamEventAttendeeDO dBAttendee : attendeesFromDbList) {
          if (dBAttendee.getAddress().getEmail().equals(email)) {
            attendeeDO = dBAttendee;
            attendeeDO.setId(internalNewAttendeeSequence--);
            break;
          }
        }

        if (attendeeDO == null) {
          attendeeDO = new TeamEventAttendeeDO();
          attendeeDO.setUrl(email);
          attendeeDO.setId(internalNewAttendeeSequence--);
        }

        // set additional fields
        final Cn cn = (Cn) attendee.getParameter(Parameter.CN);
        final CuType cuType = (CuType) attendee.getParameter(Parameter.CUTYPE);
        final PartStat partStat = (PartStat) attendee.getParameter(Parameter.PARTSTAT);
        final Rsvp rsvp = (Rsvp) attendee.getParameter(Parameter.RSVP);
        final Role role = (Role) attendee.getParameter(Parameter.ROLE);

        attendeeDO.setCommonName(cn != null ? cn.getValue() : null);
        attendeeDO.setStatus(partStat != null ? TeamEventAttendeeStatus.getStatusForPartStat(partStat.getValue()) : null);
        attendeeDO.setCuType(cuType != null ? cuType.getValue() : null);
        attendeeDO.setRsvp(rsvp != null ? rsvp.getRsvp() : null);
        attendeeDO.setRole(role != null ? role.getValue() : null);

        // further params
        StringBuilder sb = new StringBuilder();
        Iterator<Parameter> iter = attendee.getParameters().iterator();

        while (iter.hasNext()) {
          final Parameter param = iter.next();
          if (param.getName() == null || stepOver.contains(param.getName())) {
            continue;
          }

          sb.append(";");
          sb.append(param.toString());
        }

        if (sb.length() > 0) {
          // remove first ';'
          attendeeDO.setAdditionalParams(sb.substring(1));
        }

        teamEvent.addAttendee(attendeeDO);
      }
    }

    // find recurrence rule
    final RRule rule = (RRule) event.getProperty(Property.RRULE);
    teamEvent.setRecurrence(rule);

    // parsing ExDates
    PropertyList exDateProperties = event.getProperties(Property.EXDATE);
    if (exDateProperties != null) {
      List<String> exDateList = new ArrayList<>();
      exDateProperties.forEach(exDateProp -> {
        // find timezone of exdate
        final Parameter tzidParam = exDateProp.getParameter(Parameter.TZID);
        final String timezoneId;
        if (tzidParam != null && tzidParam.getValue() != null) {
          timezoneId = tzidParam.getValue();
        } else {
          timezoneId = "UTC";
        }
        TimeZone timezone = TimeZone.getTimeZone(timezoneId);

        // parse ExDate with inherent timezone
        Date exDate = ICal4JUtils.parseICalDateString(exDateProp.getValue(), timezone);

        // add ExDate in UTC to list
        exDateList.add(ICal4JUtils.asICalDateString(exDate, DateHelper.UTC, teamEvent.isAllDay()));
      });

      // TODO compute diff? could help to improve concurrent requests
      if (exDateList.isEmpty()) {
        teamEvent.setRecurrenceExDate(null);
      } else {
        teamEvent.setRecurrenceExDate(String.join(",", exDateList));
      }
    } else {
      teamEvent.setRecurrenceExDate(null);
    }
    return teamEvent;
  }

  public static RecurrenceFrequency[] getSupportedRecurrenceFrequencies()
  {
    return SUPPORTED_FREQUENCIES.clone();
  }

  public static RecurrenceFrequencyModeOne[] getSupportedRecurrenceFrequenciesModeOne()
  {
    return SUPPORTED_MODE_ONE.clone();
  }

  public static RecurrenceFrequencyModeTwo[] getSupportedRecurrenceFrequenciesModeTwo()
  {
    return SUPPORTED_MODE_TWO.clone();
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

      if (event.getSummary() != null && StringUtils.equals(event.getSummary().getValue(), TeamCalConfig.SETUP_EVENT) == true) {
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
      @Override
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
    event.setLastUpdate(src.getDtStamp());
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
