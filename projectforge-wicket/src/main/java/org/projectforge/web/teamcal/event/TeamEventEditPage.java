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

package org.projectforge.web.teamcal.event;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.TeamRecurrenceEvent;
import org.projectforge.business.teamcal.event.diff.TeamEventDiffType;
import org.projectforge.business.teamcal.event.ical.ICalGenerator;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.web.calendar.CalendarPage;
import org.projectforge.web.teamcal.integration.TeamCalCalendarPage;
import org.projectforge.web.timesheet.TimesheetEditPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.AbstractSecuredBasePage;
import org.projectforge.web.wicket.DownloadUtils;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
public class TeamEventEditPage extends AbstractEditPage<TeamEventDO, TeamEventEditForm, TeamEventDao>
{
  private static final long serialVersionUID = 1221484611148024273L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamEventEditPage.class);

  @SpringBean
  private TimesheetDao timesheetDao;

  @SpringBean
  private TeamEventService teamEventService;

  private RecurrencyChangeType recurrencyChangeType;

  /**
   * Only given if called by recurrence dialog.
   */
  private TeamEvent eventOfCaller;

  /**
   * Used for recurrence events in {@link #onSaveOrUpdate()} and {@link #afterSaveOrUpdate()}
   */
  private TeamEventDO newEvent;

  private TeamEventDO teamEventBeforeSaveOrUpdate;

  private boolean isNew;

  /**
   * @param parameters
   */
  public TeamEventEditPage(final PageParameters parameters)
  {
    super(parameters, "plugins.teamcal.event");
    super.init();
  }

  /**
   * @param parameters
   */
  public TeamEventEditPage(final PageParameters parameters, final TeamEventDO event)
  {
    super(parameters, "plugins.teamcal.event");
    init(event);
  }

  /**
   * @param parameters
   */
  public TeamEventEditPage(final PageParameters parameters, final TeamEvent event, final Timestamp newStartDate,
      final Timestamp newEndDate, final RecurrencyChangeType recurrencyChangeType)
  {
    super(parameters, "plugins.teamcal.event");
    Validate.notNull(event);
    Validate.notNull(recurrencyChangeType);
    // event contains the new start and/or stop date if modified.
    if (log.isDebugEnabled() == true) {
      log.debug("TeamEvent is: newStartDate="
          + newStartDate
          + ", newEndDate="
          + newEndDate
          + ", event=["
          + event
          + "], recurrencyChangeType=["
          + recurrencyChangeType
          + "]");
    }
    this.eventOfCaller = event;
    this.recurrencyChangeType = recurrencyChangeType;
    Integer id;
    if (event instanceof TeamEventDO) {
      id = ((TeamEventDO) event).getId();
    } else {
      id = ((TeamRecurrenceEvent) event).getMaster().getId();
    }
    final TeamEventDO teamEventDO = teamEventService.getById(id);
    if (recurrencyChangeType == RecurrencyChangeType.ALL) {
      // The user wants to edit all events, so check if the user changes start and/or end date. If so, move the date of the original event.
      if (newStartDate != null) {
        final long startDateMove = newStartDate.getTime() - event.getStartDate().getTime();
        teamEventDO.setStartDate(new Timestamp(teamEventDO.getStartDate().getTime() + startDateMove));
      }
      if (newEndDate != null) {
        final long endDateMove = newEndDate.getTime() - event.getEndDate().getTime();
        teamEventDO.setEndDate(new Timestamp(teamEventDO.getEndDate().getTime() + endDateMove));
      }
    } else {
      if (newStartDate != null) {
        teamEventDO.setStartDate(newStartDate);
      } else {
        teamEventDO.setStartDate(new Timestamp(event.getStartDate().getTime()));
      }
      if (newEndDate != null) {
        teamEventDO.setEndDate(newEndDate);
      } else {
        teamEventDO.setEndDate(new Timestamp(event.getEndDate().getTime()));
      }
    }
    if (recurrencyChangeType == RecurrencyChangeType.ONLY_CURRENT) {
      // The user wants to change only the current event, so remove all recurrency fields.
      teamEventDO.clearAllRecurrenceFields();
    }
    init(teamEventDO);
  }

  @Override
  protected void init(final TeamEventDO data)
  {
    super.init(data);
    if (isNew() == false) {
      @SuppressWarnings("serial")
      final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
          new Link<Void>(ContentMenuEntryPanel.LINK_ID)
          {
            @Override
            public void onClick()
            {
              final TimesheetDO timesheet = new TimesheetDO();
              timesheet.setStartDate(getData().getStartDate())//
                  .setStopTime(getData().getEndDate()) //
                  .setLocation(getData().getLocation());
              final StringBuffer buf = new StringBuffer();
              buf.append(getData().getSubject());
              final String note = getData().getNote();
              if (StringUtils.isNotBlank(note) == true) {
                buf.append("\n").append(note);
              }
              timesheet.setDescription(buf.toString());
              timesheetDao.setUser(timesheet, getUserId());
              final TimesheetEditPage timesheetEditPage = new TimesheetEditPage(timesheet);
              timesheetEditPage.setReturnToPage(getReturnToPage());
              setResponsePage(timesheetEditPage);
            }

          }, getString("plugins.teamcal.event.convert2Timesheet"));
      addContentMenuEntry(menu);
    }
    if (isNew() == true) {
      @SuppressWarnings("serial")
      final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(), new SubmitLink(
          ContentMenuEntryPanel.LINK_ID, form)
      {
        @Override
        public void onSubmit()
        {
          final TeamEventDO event = getData();
          final TimesheetDO timesheet = new TimesheetDO();
          if (event != null) {
            timesheet.setStartDate(event.getStartDate());
            timesheet.setStopTime(event.getEndDate());
          }
          if (returnToPage == null) {
            returnToPage = new TeamCalCalendarPage(new PageParameters());
          }
          setResponsePage(new TimesheetEditPage(timesheet).setReturnToPage(returnToPage));
        }

      }.setDefaultFormProcessing(false), getString("plugins.teamcal.switchToTimesheetButton"));
      addContentMenuEntry(menu);
    } else {
      @SuppressWarnings("serial")
      final ContentMenuEntryPanel menu = new ContentMenuEntryPanel(getNewContentMenuChildId(), new SubmitLink(ContentMenuEntryPanel.LINK_ID, form)
      {
        @Override
        public void onSubmit()
        {
          final TeamEventDO event = getData();
          log.info("Export ics for: " + event.getSubject());

          final ICalGenerator generator = ICalGenerator.exportAllFields();
          generator.addEvent(event);
          ByteArrayOutputStream icsFile = generator.getCalendarAsByteStream();

          if (icsFile != null) {
            DownloadUtils.setDownloadTarget(icsFile.toByteArray(), event.getSubject().replace(" ", "") + ".ics");
          }
        }

      }.setDefaultFormProcessing(false), getString("plugins.teamcal.exportIcsButton"));
      addContentMenuEntry(menu);
    }
  }

  /**
   * @return the recurrencyChangeType
   */
  public RecurrencyChangeType getRecurrencyChangeType()
  {
    return recurrencyChangeType;
  }

  @Override
  public void setResponsePage()
  {
    if (returnToPage == null) {
      returnToPage = new TeamCalCalendarPage(new PageParameters());
    }
    super.setResponsePage();
    if (returnToPage instanceof CalendarPage) {
      // Display the date of this time sheet in the CalendarPage (useful if the time sheet was moved).
      if (newEvent != null) {
        ((CalendarPage) returnToPage).setStartDate(newEvent.getStartDate());
      } else if (eventOfCaller != null) {
        ((CalendarPage) returnToPage).setStartDate(eventOfCaller.getStartDate());
      } else {
        ((CalendarPage) returnToPage).setStartDate(getData().getStartDate());
      }
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#afterUndelete()
   */
  @Override
  public AbstractSecuredBasePage afterUndelete()
  {
    super.afterUndelete();
    teamEventService.checkAndSendMail(getData(), TeamEventDiffType.RESTORED);

    return null;
  }

  private Date getUntilDate(Date untilUTC)
  {
    // move one day to past, the TeamEventDO will post process this value while setting
    return new Date(untilUTC.getTime() - 24 * 60 * 60 * 1000);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#onDelete()
   */
  @Override
  public AbstractSecuredBasePage onDelete()
  {
    super.onDelete();
    teamEventService.checkAndSendMail(this.getData(), TeamEventDiffType.DELETED);
    if (recurrencyChangeType == null || recurrencyChangeType == RecurrencyChangeType.ALL) {
      return null;
    }
    final Integer masterId = getData().getId(); // Store the id of the master entry.
    final TeamEventDO masterEvent = teamEventService.getById(masterId);
    if (recurrencyChangeType == RecurrencyChangeType.ALL_FUTURE) {
      final Date recurrenceUntil = this.getUntilDate(eventOfCaller.getStartDate());
      form.recurrenceData.setUntil(recurrenceUntil);
      masterEvent.setRecurrence(form.recurrenceData);
      getBaseDao().update(masterEvent);
    } else if (recurrencyChangeType == RecurrencyChangeType.ONLY_CURRENT) { // only current date
      masterEvent.addRecurrenceExDate(eventOfCaller.getStartDate());
      getBaseDao().update(masterEvent);
    }
    return (AbstractSecuredBasePage) getReturnToPage();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#onSaveOrUpdate()
   */
  @Override
  public AbstractSecuredBasePage onSaveOrUpdate()
  {
    super.onSaveOrUpdate();
    if (getData().getCreator() == null) {
      getData().setCreator(ThreadLocalUserContext.getUser());
    }

    if (getData() != null && getData().getId() != null) {
      this.teamEventBeforeSaveOrUpdate = teamEventService.getById(getData().getPk()).clone();
      this.isNew = false;
    } else {
      this.isNew = true;
    }

    getData().setRecurrence(form.recurrenceData);
    if (recurrencyChangeType == null || recurrencyChangeType == RecurrencyChangeType.ALL) {
      return null;
    }
    final Integer masterId = getData().getId(); // Store the id of the master entry.
    getData().setId(null); // Clone object.
    final TeamEventDO oldDataObject = getData();
    final TeamEventDO masterEvent = teamEventService.getById(masterId);
    if (masterEvent == null) {
      log.error("masterEvent is null?! Do nothing more after saving team event.");
      return null;
    }
    if (eventOfCaller == null) {
      log.error("eventOfCaller is null?! Do nothing more after saving team event.");
      return null;
    }
    form.setData(masterEvent);
    if (recurrencyChangeType == RecurrencyChangeType.ALL_FUTURE) {
      newEvent = oldDataObject.clone();
      // Set the end date of the master date one day before current date and save this event.
      final Date recurrenceUntil = this.getUntilDate(eventOfCaller.getStartDate());
      form.recurrenceData.setUntil(recurrenceUntil);
      getData().setRecurrence(form.recurrenceData);
      if (log.isDebugEnabled() == true) {
        log.debug("Recurrency until date of master entry will be set to: " + DateHelper.formatAsUTC(recurrenceUntil));
        log.debug("The new event is: " + newEvent);
      }
      return null;
    } else if (recurrencyChangeType == RecurrencyChangeType.ONLY_CURRENT) { // only current date
      // Add current date to the master date as exclusion date and save this event (without recurrence settings).
      masterEvent.addRecurrenceExDate(eventOfCaller.getStartDate());
      newEvent = oldDataObject.clone();
      newEvent.setRecurrenceDate(eventOfCaller.getStartDate(), ThreadLocalUserContext.getTimeZone());
      newEvent.setRecurrenceReferenceId(masterEvent.getId());
      if (log.isDebugEnabled() == true) {
        log.debug("Recurrency ex date of master entry is now added: "
            + DateHelper.formatAsUTC(eventOfCaller.getStartDate())
            + ". The new string is: "
            + masterEvent.getRecurrenceExDate());
        log.debug("The new event is: " + newEvent);
      }
      return null;
    }
    return null;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#afterSaveOrUpdate()
   */
  @Override
  public AbstractSecuredBasePage afterSaveOrUpdate()
  {
    super.afterSaveOrUpdate();

    if (newEvent != null) {
      // changed one element of an recurring event
      newEvent.setSequence(0);
      newEvent.getAttendees().clear();
      teamEventService.save(newEvent);

      Set<TeamEventAttendeeDO> toAssign = new HashSet<>();
      form.assignAttendeesListHelper.getAssignedItems().stream().forEach(a -> toAssign.add(a.clone()));

      teamEventService.assignAttendees(newEvent, toAssign, null);
      teamEventService.checkAndSendMail(newEvent, TeamEventDiffType.NEW);
    } else {
      TeamEventDO teamEventAfterSaveOrUpdate = teamEventService.getById(getData().getPk());
      teamEventService.assignAttendees(teamEventAfterSaveOrUpdate, form.assignAttendeesListHelper.getItemsToAssign(),
          form.assignAttendeesListHelper.getItemsToUnassign());
      teamEventService.checkAndSendMail(teamEventAfterSaveOrUpdate, this.teamEventBeforeSaveOrUpdate);
    }

    return null;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#cloneData()
   */
  @Override
  protected void cloneData()
  {
    log.info("Clone of data chosen: " + getData());
    TeamEventDO teamEventClone = getData().clone();
    Set<TeamEventAttendeeDO> originAssignedAttendees = new HashSet<>();
    teamEventClone.getAttendees().forEach(attendee -> {
      originAssignedAttendees.add(attendee);
    });
    teamEventClone.setAttendees(new HashSet<>());
    this.form = newEditForm(this, teamEventClone);
    body.addOrReplace(this.form);
    this.form.init();
    originAssignedAttendees.forEach(attendee -> {
      if (attendee.getAddress() != null) {
        this.form.attendeeWicketProvider.initSortedAttendees();
        this.form.attendeeWicketProvider.getSortedAttendees().forEach(sortedAttendee -> {
          if (sortedAttendee.getAddress() != null && sortedAttendee.getAddress().getPk().equals(attendee.getAddress().getPk())) {
            sortedAttendee.setId(this.form.attendeeWicketProvider.getAndDecreaseInternalNewAttendeeSequence());
            this.form.assignAttendeesListHelper.assignItem(sortedAttendee);
          }
        });
      } else {
        attendee.setId(this.form.attendeeWicketProvider.getAndDecreaseInternalNewAttendeeSequence());
        this.form.attendeeWicketProvider.getCustomAttendees().add(attendee);
        this.form.assignAttendeesListHelper.assignItem(attendee);
      }
    });
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getBaseDao()
   */
  @Override
  protected TeamEventDao getBaseDao()
  {
    return teamEventService.getTeamEventDao();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected TeamEventEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final TeamEventDO data)
  {
    return new TeamEventEditForm(this, data);
  }

}
