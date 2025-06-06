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

import net.ftlines.wicket.fullcalendar.Event;
import net.ftlines.wicket.fullcalendar.callback.EventDroppedCallbackScriptGenerator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Period;
import org.projectforge.business.calendar.event.model.ICalendarEvent;
import org.projectforge.business.teamcal.admin.right.TeamCalRight;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventFilter;
import org.projectforge.business.teamcal.event.TeamEventRecurrenceData;
import org.projectforge.business.teamcal.event.TeamRecurrenceEvent;
import org.projectforge.business.teamcal.event.model.TeamCalEventId;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.event.right.TeamEventRight;
import org.projectforge.business.teamcal.filter.TeamCalCalendarFilter;
import org.projectforge.business.teamcal.filter.TemplateEntry;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.RecurrenceFrequency;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.WicketSupport;
import org.projectforge.web.calendar.MyFullCalendarEventsProvider;

import java.util.*;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TeamCalEventProvider extends MyFullCalendarEventsProvider
{
  private static final long serialVersionUID = -5609599079385073490L;

  private int days;

  private final TeamCalCalendarFilter filter;

  private final Map<String, ICalendarEvent> teamEventMap = new HashMap<String, ICalendarEvent>();

  /**
   * the name of the event class.
   */
  public static final String EVENT_CLASS_NAME = "teamEvent";

  private final TeamEventRight eventRight;

  public TeamCalEventProvider(final TeamCalCalendarFilter filter)
  {
    this.filter = filter;
    this.eventRight = new TeamEventRight();
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#getEvents(org.joda.time.DateTime,
   * org.joda.time.DateTime)
   */
  @Override
  public Collection<Event> getEvents(final DateTime start, final DateTime end)
  {
    final Collection<Event> events = super.getEvents(start, end);
    return events;
  }

  /**
   * @see org.projectforge.web.calendar.MyFullCalendarEventsProvider#buildEvents(org.joda.time.DateTime,
   * org.joda.time.DateTime)
   */
  @Override
  protected void buildEvents(final DateTime start, final DateTime end)
  {
    final TemplateEntry activeTemplateEntry = filter.getActiveTemplateEntry();
    if (activeTemplateEntry == null) {
      // Nothing to build.
      return;
    }
    final Set<Long> visibleCalendars = activeTemplateEntry.getVisibleCalendarIds();
    if (CollectionUtils.isEmpty(visibleCalendars) == true) {
      // Nothing to build.
      return;
    }
    final TeamEventFilter eventFilter = new TeamEventFilter();
    eventFilter.setTeamCals(visibleCalendars);
    eventFilter.setStartDate(start.toDate());
    eventFilter.setEndDate(end.toDate());
    eventFilter.setUser(ThreadLocalUserContext.getLoggedInUser());
    final List<ICalendarEvent> teamEvents = WicketSupport.get(TeamEventDao.class).getEventList(eventFilter, true);

    days = Days.daysBetween(start, end).getDays();
    // Week or day view:
    final boolean longFormat = days < 10;

    final TeamCalRight right = new TeamCalRight();
    final PFUserDO user = ThreadLocalUserContext.getLoggedInUser();
    final TimeZone timeZone = ThreadLocalUserContext.getTimeZone();
    if (CollectionUtils.isNotEmpty(teamEvents) == true) {
      for (final ICalendarEvent teamEvent : teamEvents) {
        final TeamEventDO eventDO;
        if (teamEvent instanceof TeamEventDO) {
          eventDO = (TeamEventDO) teamEvent;
        } else {
          eventDO = ((TeamRecurrenceEvent) teamEvent).getMaster();
        }

        // Use UTC for all day events to prevent date shift for timezones with negative offset
        final DateTimeZone dateTimeZone = eventDO.getAllDay() ? DateTimeZone.forTimeZone(DateHelper.UTC) : ThreadLocalUserContext.getDateTimeZone();
        final DateTime startDate = new DateTime(teamEvent.getStartDate(), dateTimeZone);
        final DateTime endDate = new DateTime(teamEvent.getEndDate(), dateTimeZone);

        final TeamCalEventId id = new TeamCalEventId(teamEvent, timeZone);

        teamEventMap.put(id.toString(), teamEvent);
        final MyWicketEvent event = new MyWicketEvent();
        event.setClassName(EVENT_CLASS_NAME + " " + EventDroppedCallbackScriptGenerator.NO_CONTEXTMENU_INDICATOR);
        event.setId("" + id);
        event.setColor(activeTemplateEntry.getColorCode(eventDO.getCalendarId()));

        if (eventRight.hasUpdateAccess(ThreadLocalUserContext.getLoggedInUser(), eventDO, null)) {
          event.setEditable(true);
        } else {
          event.setEditable(false);
        }

        // id <= 0 is hint for abo events -> not editable
        if (eventDO.getId() != null && eventDO.getId() <= 0) {
          event.setEditable(false);
        }

        if (teamEvent.getAllDay() == true) {
          event.setAllDay(true);
        }

        event.setStart(startDate);
        event.setEnd(endDate);
        event.setTooltip(eventDO.getCalendar().getTitle(), createTooltipLabelValues(eventDO));

        final String title;
        String durationString = "";
        if (longFormat == true) {
          final Period period = new Period(startDate, endDate);
          int hourInt = period.getHours();
          if (period.getDays() > 0) {
            hourInt += period.getDays() * 24;
          }
          final String hour = hourInt < 10 ? "0" + hourInt : "" + hourInt;

          final int minuteInt = period.getMinutes();
          final String minute = minuteInt < 10 ? "0" + minuteInt : "" + minuteInt;

          if (event.isAllDay() == false) {
            durationString = "\n" + ThreadLocalUserContext.getLocalizedString("plugins.teamcal.event.duration") + ": "
                + hour + ":" + minute;
          }
          final StringBuilder buf = new StringBuilder();
          buf.append(teamEvent.getSubject());
          if (StringUtils.isNotBlank(teamEvent.getNote()) == true) {
            buf.append("\n").append(ThreadLocalUserContext.getLocalizedString("plugins.teamcal.event.note"))
                .append(": ")
                .append(teamEvent.getNote());
          }
          buf.append(durationString);
          title = buf.toString();
        } else {
          title = teamEvent.getSubject();
        }
        if (right.hasMinimalAccess(eventDO.getCalendar(), user.getId()) == true) {
          // for minimal access
          event.setTitle("");
          event.setEditable(false);
        } else {
          event.setTitle(title);
        }
        events.put(String.valueOf(id), event);
      }
    }
  }

  private String[][] createTooltipLabelValues(final TeamEventDO eventDO)
  {
    String recurrence = null;
    if (eventDO.hasRecurrence() == true) {
      final TeamEventRecurrenceData recurrenceData = eventDO.getRecurrenceData(ThreadLocalUserContext.getTimeZone());
      final RecurrenceFrequency frequency = recurrenceData.getFrequency();
      if (frequency != null) {
        final String unitI18nKey = frequency.getUnitI18nKey();
        if (unitI18nKey != null) {
          recurrence = recurrenceData.getInterval() + " " + getString(unitI18nKey);
        }
      }
    }

    String reminder = null;
    if (eventDO.getReminderActionType() != null
        && NumberHelper.greaterZero(eventDO.getReminderDuration()) == true
        && eventDO.getReminderDurationUnit() != null) {
      reminder = getString(eventDO.getReminderActionType().getI18nKey())
          + " "
          + eventDO.getReminderDuration()
          + " "
          + getString(eventDO.getReminderDurationUnit().getI18nKey());
    }

    final String[][] tooltipContentWithoutAttendees = {
        { eventDO.getSubject() },
        { eventDO.getLocation(), getString("timesheet.location") },
        { eventDO.getNote(), getString("plugins.teamcal.event.note") },
        { recurrence, getString("plugins.teamcal.event.recurrence") },
        { reminder, getString("plugins.teamcal.event.reminder") }
    };

    final List<String[]> tooltipContent = new ArrayList<>(Arrays.asList(tooltipContentWithoutAttendees));

    if (eventDO.getAttendees() != null && eventDO.getAttendees().isEmpty() == false) {
      tooltipContent.add(new String[] { getString("plugins.teamcal.attendees") + ":" });

      for (TeamEventAttendeeDO teamEventAttendeeDO : eventDO.getAttendees()) {
        final StringBuilder buf = new StringBuilder();
        buf.append("- ");

        if (teamEventAttendeeDO.getUser() != null) {
          buf.append(teamEventAttendeeDO.getUser().getFullname());
        } else if (teamEventAttendeeDO.getUrl() != null) {
          buf.append(teamEventAttendeeDO.getUrl());
        } else {
          buf.append(teamEventAttendeeDO.getAddress().getFullName());
        }

        if (teamEventAttendeeDO.getStatus() != null) {
          buf.append("  [")
              .append(I18nHelper.getLocalizedMessage(teamEventAttendeeDO.getStatus().getI18nKey()))
              .append("]");
        } else {
          buf.append("  [")
              .append(I18nHelper.getLocalizedMessage(TeamEventAttendeeStatus.IN_PROCESS.getI18nKey()))
              .append("]");
        }

        tooltipContent.add(new String[] { buf.toString() });
      }
    }

    return tooltipContent.toArray(new String[tooltipContent.size()][]);
  }

  public ICalendarEvent getTeamEvent(final String id)
  {
    return teamEventMap.get(id);
  }
}
