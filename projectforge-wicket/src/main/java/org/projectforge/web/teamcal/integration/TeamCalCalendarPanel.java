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

package org.projectforge.web.teamcal.integration;

import java.sql.Timestamp;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.joda.time.DateTime;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamRecurrenceEvent;
import org.projectforge.business.teamcal.event.model.TeamCalEventId;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.event.right.TeamEventRight;
import org.projectforge.business.teamcal.filter.ICalendarFilter;
import org.projectforge.business.teamcal.filter.TeamCalCalendarFilter;
import org.projectforge.business.teamcal.filter.TemplateEntry;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.web.calendar.CalendarFilter;
import org.projectforge.web.calendar.CalendarPanel;
import org.projectforge.web.calendar.MyFullCalendarConfig;
import org.projectforge.web.teamcal.dialog.RecurrenceChangeDialog;
import org.projectforge.web.teamcal.event.TeamCalEventProvider;
import org.projectforge.web.teamcal.event.TeamEventEditPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.WicketRenderHeadUtils;
import org.projectforge.web.wicket.components.JodaDatePanel;

import net.ftlines.wicket.fullcalendar.CalendarResponse;
import net.ftlines.wicket.fullcalendar.Event;
import net.ftlines.wicket.fullcalendar.EventSource;
import net.ftlines.wicket.fullcalendar.callback.CalendarDropMode;
import net.ftlines.wicket.fullcalendar.callback.ClickedEvent;
import net.ftlines.wicket.fullcalendar.callback.SelectedRange;
import net.ftlines.wicket.fullcalendar.callback.View;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
public class TeamCalCalendarPanel extends CalendarPanel
{
  private static final long serialVersionUID = 5462271308502345885L;

  @SpringBean
  private transient TeamEventDao teamEventDao;

  @SpringBean
  private transient TeamCalDao teamCalDao;

  private TeamCalEventProvider eventProvider;

  private RecurrenceChangeDialog recurrenceChangeDialog;

  /**
   * @param id
   * @param currentDatePanel
   */
  public TeamCalCalendarPanel(final String id, final JodaDatePanel currentDatePanel)
  {
    super(id, currentDatePanel);
  }

  /**
   * @see org.apache.wicket.Component#renderHead(org.apache.wicket.markup.head.IHeaderResponse)
   */
  @Override
  public void renderHead(final IHeaderResponse response)
  {
    super.renderHead(response);
    WicketRenderHeadUtils.renderSelect2JavaScriptIncludes(response);
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
    final AbstractSecuredPage parentPage = (AbstractSecuredPage) getPage();
    recurrenceChangeDialog = new RecurrenceChangeDialog(parentPage.newModalDialogId(), new ResourceModel(
        "plugins.teamcal.event.recurrence.change.title"));
    parentPage.add(recurrenceChangeDialog);
    recurrenceChangeDialog.init();
  }

  /**
   * @see org.projectforge.web.calendar.CalendarPanel#onDateRangeSelectedHook(java.lang.String,
   * net.ftlines.wicket.fullcalendar.callback.SelectedRange, net.ftlines.wicket.fullcalendar.CalendarResponse)
   */
  @Override
  protected void onDateRangeSelectedHook(final String selectedCalendar, final SelectedRange range,
      final CalendarResponse response)
  {
    handleDateRangeSelection(this, getWebPage(), range, teamCalDao, selectedCalendar);
  }

  private void handleDateRangeSelection(final Component caller, final WebPage returnPage, final SelectedRange range,
      final TeamCalDao teamCalDao, final String calendarId)
  {
    if (filter instanceof TeamCalCalendarFilter) {
      final TemplateEntry activeTemplateEntry = ((TeamCalCalendarFilter) filter).getActiveTemplateEntry();
      if (activeTemplateEntry.getDefaultCalendarId() == null && activeTemplateEntry.getCalendars().size() > 0) {
        activeTemplateEntry.setDefaultCalendarId(activeTemplateEntry.getCalendars().get(0).getId());
      }
      final TeamCalDO calendar = teamCalDao.getById(activeTemplateEntry.getDefaultCalendarId());
      final TeamEventDO event = new TeamEventDO();
      event.setAllDay(range.isAllDay());
      event.setOwnership(true);
      event.setStartDate(new Timestamp(DateHelper.getDateTimeAsMillis(range.getStart()))).setEndDate(
          new Timestamp(DateHelper.getDateTimeAsMillis(range.getEnd())));
      event.setCalendar(calendar);
      final TeamEventEditPage page = new TeamEventEditPage(new PageParameters(), event);
      page.setReturnToPage(new TeamCalCalendarPage(returnPage.getPageParameters()));
      caller.setResponsePage(page);
    }
  }

  /**
   * @see org.projectforge.web.calendar.CalendarPanel#onEventClickedHook(net.ftlines.wicket.fullcalendar.callback.ClickedEvent,
   * net.ftlines.wicket.fullcalendar.CalendarResponse, net.ftlines.wicket.fullcalendar.Event, java.lang.String,
   * java.lang.String)
   */
  @Override
  protected void onEventClickedHook(final ClickedEvent clickedEvent, final CalendarResponse response, final Event event,
      final String eventId, final String eventClassName)
  {
    // skip abo events at this place please
    if (StringUtils.startsWith(eventId, "-")) {
      return;
    }
    // User clicked on teamEvent
    final TeamCalEventId id = new TeamCalEventId(event.getId(), ThreadLocalUserContext.getTimeZone());
    final TeamEventDO teamEventDO = teamEventDao.getById(id.getDataBaseId());
    final TeamEvent teamEvent = eventProvider.getTeamEvent(id.toString());
    if (new TeamEventRight(accessChecker).hasUpdateAccess(ThreadLocalUserContext.getUser(), teamEventDO,
        null)) {
      if (teamEventDO.hasRecurrence() == true) {
        // at this point the dbTeamEvent is already updated in time
        recurrenceChangeDialog.open(response.getTarget(), teamEvent, null, null);
        return;
      }

      final PageParameters parameters = new PageParameters();
      parameters.add(AbstractEditPage.PARAMETER_KEY_ID, id.getDataBaseId());
      final TeamEventEditPage teamEventPage = new TeamEventEditPage(parameters);
      setResponsePage(teamEventPage);
      teamEventPage.setReturnToPage((WebPage) getPage());
      return;
    }
  }

  /**
   * @see org.projectforge.web.calendar.CalendarPanel#onModifyEventHook(net.ftlines.wicket.fullcalendar.Event,
   * org.joda.time.DateTime, org.joda.time.DateTime, net.ftlines.wicket.fullcalendar.callback.CalendarDropMode,
   * net.ftlines.wicket.fullcalendar.CalendarResponse)
   */
  @Override
  protected void onModifyEventHook(final Event event, final DateTime newStartTime, final DateTime newEndTime,
      final CalendarDropMode dropMode, final CalendarResponse response)
  {
    modifyEvent(event, newStartTime, newEndTime, dropMode, response);
  }

  /**
   * @see org.projectforge.web.calendar.CalendarPanel#onRegisterEventSourceHook(MyFullCalendarConfig, CalendarFilter,
   * ICalendarFilter)
   */
  @Override
  protected void onRegisterEventSourceHook(final MyFullCalendarConfig config, final ICalendarFilter filter)
  {
    if (filter instanceof TeamCalCalendarFilter) {
      // Colors are handled event based, this is just the default value
      final EventSource eventSource = new EventSource();
      eventProvider = new TeamCalEventProvider(accessChecker, teamEventDao, (TeamCalCalendarFilter) filter);
      eventSource.setEventsProvider(eventProvider);
      eventSource.setBackgroundColor("#1AA118");
      eventSource.setColor("#000000");
      eventSource.setTextColor("#222222");
      config.add(eventSource);
    }

  }

  /**
   * @see org.projectforge.web.calendar.CalendarPanel#onRefreshEventProvider()
   */
  @Override
  protected void onRefreshEventProvider()
  {
    eventProvider.resetEventCache();
  }

  /**
   * @see org.projectforge.web.calendar.CalendarPanel#onCallGetEventsHook()
   */
  @Override
  protected void onCallGetEventsHook(final View view, final CalendarResponse response)
  {
    final TeamCalCalendarForm tempForm = (TeamCalCalendarForm) ((TeamCalCalendarPage) getPage()).getForm();
    if (tempForm != null && CollectionUtils.isNotEmpty(tempForm.getSelectedCalendars()) == true) {
      eventProvider.getEvents(view.getVisibleStart().toDateTime(), view.getVisibleEnd().toDateTime());
    }
  }

  /**
   * Modify options.<br />
   * Handle edit events like:
   * <ul>
   * <li>COPY_EDIT</li>
   * <li>COPY_SAVE</li>
   * <li>MOVE_EDIT</li>
   * <li>MOVE_SAVE</li>
   * </ul>
   *
   * @param event
   * @param newStartDate
   * @param newEndDate
   * @param dropMode
   * @param response
   */
  private void modifyEvent(final Event event, final DateTime newStartDate, final DateTime newEndDate,
      final CalendarDropMode dropMode,
      final CalendarResponse response)
  {
    // skip abo events at this place please
    if (StringUtils.startsWith(event.getId(), "-")) {
      return;
    }
    final TeamCalEventId id = new TeamCalEventId(event.getId(), ThreadLocalUserContext.getTimeZone());
    final TeamEvent teamEvent = eventProvider.getTeamEvent(id.toString());
    if (teamEvent == null) {
      return;
    }
    TeamEventDO teamEventDO;
    if (teamEvent instanceof TeamEventDO) {
      teamEventDO = (TeamEventDO) teamEvent;
    } else {
      teamEventDO = ((TeamRecurrenceEvent) teamEvent).getMaster();
    }
    final Long newStartTimeMillis = newStartDate != null ? DateHelper.getDateTimeAsMillis(newStartDate) : null;
    final Long newEndTimeMillis = newEndDate != null ? DateHelper.getDateTimeAsMillis(newEndDate) : null;
    final PFUserDO loggedInUser = ((AbstractSecuredBasePage) getPage()).getUser();
    if (teamEventDao.hasUpdateAccess(loggedInUser, teamEventDO, teamEventDO, false) == false) {
      // User has no update access, therefore ignore this request...
      event.setEditable(false);
      event.setTitle("");
      return;
    }

    if (teamEventDO.hasRecurrence() == true) {
      // at this point the dbTeamEvent is already updated in time
      recurrenceChangeDialog.open(response.getTarget(), teamEvent,
          newStartTimeMillis != null ? new Timestamp(newStartTimeMillis) : null,
          newEndTimeMillis != null ? new Timestamp(newEndTimeMillis) : null);
      return;
    }
    teamEventDO = teamEventDao.getById(teamEventDO.getId());
    // update start and end date
    if (newStartDate != null) {
      teamEventDO.setStartDate(new Timestamp(newStartTimeMillis));
    }
    if (newEndDate != null) {
      teamEventDO.setEndDate(new Timestamp(newEndTimeMillis));
    }

    // clone event if mode is copy_*
    if (CalendarDropMode.COPY_EDIT.equals(dropMode) || CalendarDropMode.COPY_SAVE.equals(dropMode)) {
      teamEventDO.setId(null);
      teamEventDO.setDeleted(false);
      teamEventDO.setOwnership(true);

      // and save the new event -> correct time is set already
      teamEventDao.save(teamEventDO);
    }

    if (dropMode == null || CalendarDropMode.MOVE_EDIT.equals(dropMode)
        || CalendarDropMode.COPY_EDIT.equals(dropMode)) {
      // first: "normal edit mode"

      // add start date
      if (newStartDate != null) {
        teamEventDO.setStartDate(new Timestamp(newStartTimeMillis));
      }
      // add end date
      if (newEndDate != null) {
        teamEventDO.setEndDate(new Timestamp(newEndTimeMillis));
      }
      final TeamEventEditPage teamEventEditPage = new TeamEventEditPage(new PageParameters(), teamEventDO);
      teamEventEditPage.setReturnToPage(getWebPage());
      setResponsePage(teamEventEditPage);
    } else if (CalendarDropMode.MOVE_SAVE.equals(dropMode) || CalendarDropMode.COPY_SAVE.equals(dropMode)) {
      // second mode: "quick save mode"
      if (CalendarDropMode.MOVE_SAVE.equals(dropMode)) {
        // we need update only in "move" mode, in "copy" mode it was saved a few lines above
        teamEventDao.update(teamEventDO);
      }
      setResponsePage(getWebPage().getClass(), getWebPage().getPageParameters());
    } else {
      // CANCEL -> should be handled through javascript now
      setResponsePage(getWebPage());
    }
  }
}
