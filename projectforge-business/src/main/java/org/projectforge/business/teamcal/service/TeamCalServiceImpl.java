package org.projectforge.business.teamcal.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.TeamCalsComparator;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.calendar.CalendarUtils;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DayHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Name;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Trigger;

@Service
public class TeamCalServiceImpl
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalServiceImpl.class);

  public static final String PARAM_EXPORT_REMINDER = "exportReminders";

  private final TeamCalsComparator calsComparator = new TeamCalsComparator();

  private Collection<TeamCalDO> sortedCals;

  @Autowired
  private TeamCalCache teamCalCache;

  @Autowired
  private CalendarFeedService calendarFeedService;

  @Autowired
  private TeamEventDao teamEventDao;

  public List<Integer> getCalIdList(final Collection<TeamCalDO> teamCals)
  {
    return teamEventDao.getCalIdList(teamCals);
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
    for (int i = 0; i < teamCalIds.length; i++) {
      final Integer id = Integer.valueOf(teamCalIds[i]);
      eventFilter.setTeamCalId(id);
      final List<TeamEvent> teamEvents = teamEventDao.getEventList(eventFilter, false);
      if (teamEvents != null && teamEvents.size() > 0) {
        for (final TeamEvent teamEventObject : teamEvents) {
          if (teamEventObject instanceof TeamEventDO == false) {
            log.warn("Oups, shouldn't occur, please contact the developer: teamEvent isn't of type TeamEventDO: "
                + teamEventObject);
            continue;
          }
          final TeamEventDO teamEvent = (TeamEventDO) teamEventObject;
          final String uid = TeamCalConfig.get().createEventUid(teamEvent.getId());
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
          // TODO add attendees
          events.add(vEvent);
        }
      }
    }
    return events;
  }

}
