package org.projectforge.business.teamcal.service;

import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.model.ReminderActionType;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.RecurrenceFrequency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.RRule;

@Service
public class TeamCalServiceImpl
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalServiceImpl.class);

  private static final List<String> STEP_OVER = Arrays.asList(Parameter.CN, Parameter.CUTYPE, Parameter.PARTSTAT, Parameter.RSVP, Parameter.ROLE);

  private static final RecurrenceFrequency[] SUPPORTED_FREQUENCIES = new RecurrenceFrequency[] {
      RecurrenceFrequency.NONE, RecurrenceFrequency.DAILY, RecurrenceFrequency.WEEKLY, RecurrenceFrequency.MONTHLY, RecurrenceFrequency.YEARLY };

  // needed to convertVEvent weeks into days
  private static final int DURATION_OF_WEEK = 7;

  @Autowired
  private TeamEventService teamEventService;

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
          if (param.getName() == null || STEP_OVER.contains(param.getName())) {
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

  public static List<VEvent> getVEvents(final net.fortuna.ical4j.model.Calendar calendar)
  {
    final List<VEvent> events = new ArrayList<>();
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
    return convert(list);
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
    events.sort((o1, o2) -> {
      final Date startDate1 = o1.getStartDate();
      final Date startDate2 = o2.getStartDate();
      if (startDate1 == null) {
        if (startDate2 == null) {
          return 0;
        }
        return -1;
      }
      return startDate1.compareTo(startDate2);
    });
    return events;
  }

}
