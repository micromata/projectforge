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

package org.projectforge.business.teamcal.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.calendar.CalendarUtils;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZone;
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

/**
 * Hook for TeamCal feeds
 * 
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Service
public class TeamCalCalendarFeedHook implements CalendarFeedHook
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalCalendarFeedHook.class);

  public static final String PARAM_EXPORT_REMINDER = "exportReminders";

  @Autowired
  private TeamEventDao teamEventDao;

  @Autowired
  private CalendarFeedService calendarFeedService;

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
   *      java.util.Calendar)
   */
  @Override
  public List<VEvent> getEvents(final Map<String, String> params, final TimeZone timeZone)
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
