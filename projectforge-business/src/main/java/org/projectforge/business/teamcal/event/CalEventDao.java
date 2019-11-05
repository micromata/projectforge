/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.projectforge.business.calendar.event.model.ICalendarEvent;
import org.projectforge.business.calendar.event.model.SeriesModificationMode;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.event.model.CalEventDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.calendar.CalendarUtils;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import java.util.*;

@Repository
public class CalEventDao extends BaseDao<CalEventDO> {
  /**
   * For storing the selected element of the series in the transient attribute map for correct handling in {@link #onDelete(CalEventDO)}
   * and {@link #onSaveOrModify(CalEventDO)} of series (all, future, selected).
   */
  public static final String ATTR_SELECTED_ELEMENT = "selectedSeriesElement";

  /**
   * For series elements: what to modify in {@link #onDelete(CalEventDO)} and {@link #onSaveOrModify(CalEventDO)} of series (all, future, selected)?
   */
  public static final String ATTR_SERIES_MODIFICATION_MODE = "seriesModificationMode";

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CalEventDao.class);

  private static final long ONE_DAY = 1000 * 60 * 60 * 24;

  @Autowired
  private PfEmgrFactory emgrFac;

  @Autowired
  private TenantService tenantService;

  public CalEventDao() {
    super(CalEventDO.class);
    userRightId = UserRightId.CALENDAR_EVENT;
  }

  public CalEventDO getByUid(Integer calendarId, final String uid) {
    return this.getByUid(calendarId, uid, true);
  }

  public CalEventDO getByUid(Integer calendarId, final String uid, final boolean excludeDeleted) {
    if (uid == null) {
      return null;
    }

    final StringBuilder sqlQuery = new StringBuilder();
    final List<Object> params = new ArrayList<>();

    sqlQuery.append("select e from CalEventDO e where e.uid = :uid AND e.tenant = :tenant");

    params.add("uid");
    params.add(uid);
    params.add("tenant");
    params.add(ThreadLocalUserContext.getUser() != null ? ThreadLocalUserContext.getUser().getTenant() : tenantService.getDefaultTenant());

    if (excludeDeleted) {
      sqlQuery.append(" AND e.deleted = :deleted");
      params.add("deleted");
      params.add(false);
    }

    // workaround to still handle old requests
    if (calendarId != null) {
      sqlQuery.append(" AND e.calendar.id = :calendarId");
      params.add("calendarId");
      params.add(calendarId);
    }

    try {
      return emgrFac.runRoTrans(emgr -> emgr.selectSingleAttached(CalEventDO.class, sqlQuery.toString(), params.toArray()));
    } catch (NoResultException | NonUniqueResultException e) {
      return null;
    }
  }

  @Override
  protected void onChange(final CalEventDO obj, final CalEventDO dbObj) {
    handleSeriesUpdates(obj);
    // only increment sequence if PF has ownership!
    /*if (obj.getOwnership() != null && obj.getOwnership() == false) {
      return;
    }

    // compute diff
    if (obj.mustIncSequence(dbObj)) {
      if (obj.getSequence() == null) {
        obj.setSequence(0);
      } else {
        obj.setSequence(obj.getSequence() + 1);
      }

      if (obj.getDtStamp() == null || obj.getDtStamp().equals(dbObj.getDtStamp())) {
        obj.setDtStamp(new Timestamp(System.currentTimeMillis()));
      }
    }*/
  }

  private Date getUntilDate(Date untilUTC) {
    // move one day to past, the TeamEventDO will post process this value while setting
    return new Date(untilUTC.getTime() - 24 * 60 * 60 * 1000);
  }

  /**
   * Handles updates of series element (if any) for future and single events of a series.
   *
   * @param event
   */
  private void handleSeriesUpdates(final CalEventDO event) {
    ICalendarEvent selectedEvent = (ICalendarEvent) event.removeTransientAttribute(ATTR_SELECTED_ELEMENT); // Must be removed, otherwise save below will handle this attrs again.
    SeriesModificationMode mode = (SeriesModificationMode) event.removeTransientAttribute(ATTR_SERIES_MODIFICATION_MODE);
    if (selectedEvent == null || mode == null || mode == SeriesModificationMode.ALL) {
      // Nothing to do.
      return;
    }
    // TODO
    /*
    CalEventDO newEvent = event.clone();
    newEvent.setSequence(0);
    CalEventDO masterEvent = getById(event.getId());
    event.copyValuesFrom(masterEvent); // Restore db fields of master event. Do only modify single or future events.
    if (mode == SeriesModificationMode.FUTURE) {
      TeamEventRecurrenceData recurrenceData = masterEvent.getRecurrenceData(ThreadLocalUserContext.getTimeZone());
      // Set the end date of the master date one day before current date and save this event.
      recurrenceData.setUntil(getUntilDate(selectedEvent.getStartDate()));
      event.setRecurrence(recurrenceData);
      save(newEvent);
      if (log.isDebugEnabled() == true) {
        log.debug("Recurrence until date of master entry will be set to: " + DateHelper.formatAsUTC(recurrenceData.getUntil()));
        log.debug("The new event is: " + newEvent);
      }
      return;
    } else if (mode == SeriesModificationMode.SINGLE) { // only current date
      // Add current date to the master date as exclusion date and save this event (without recurrence settings).
      event.addRecurrenceExDate(selectedEvent.getStartDate());
      if (newEvent.hasRecurrence()) {
        log.warn("User tries to modifiy single event of a series, the given recurrence is ignored.");
      }
      newEvent.setRecurrence((RRule) null); // User only wants to modify single event, ignore recurrence.
      save(newEvent);
      if (log.isDebugEnabled() == true) {
        log.debug("Recurrency ex date of master entry is now added: "
                + DateHelper.formatAsUTC(selectedEvent.getStartDate())
                + ". The new string is: "
                + event.getRecurrenceExDate());
        log.debug("The new event is: " + newEvent);
      }
    }*/
  }

  /**
   * Handles deletion of series element (if any) for future and single events of a series.
   */
  @Override
  public void internalMarkAsDeleted(final CalEventDO obj) {
    ICalendarEvent selectedEvent = (ICalendarEvent) obj.removeTransientAttribute(ATTR_SELECTED_ELEMENT); // Must be removed, otherwise update below will handle this attrs again.
    SeriesModificationMode mode = (SeriesModificationMode) obj.removeTransientAttribute(ATTR_SERIES_MODIFICATION_MODE);
    if (selectedEvent == null || mode == null || mode == SeriesModificationMode.ALL) {
      // Nothing to do special:
      super.internalMarkAsDeleted(obj);
      return;
    }
    CalEventDO masterEvent = getById(obj.getId());
    obj.copyValuesFrom(masterEvent); // Restore db fields of master event. Do only modify single or future events.
    if (mode == SeriesModificationMode.FUTURE) {
      // TODO
      /*
      TeamEventRecurrenceData recurrenceData = obj.getRecurrenceData(ThreadLocalUserContext.getTimeZone());
      Date recurrenceUntil = getUntilDate(selectedEvent.getStartDate());
      recurrenceData.setUntil(recurrenceUntil);
      obj.setRecurrence(recurrenceData);
      update(obj);*/
    } else if (mode == SeriesModificationMode.SINGLE) { // only current date
      // TODO
      /*
      Validate.notNull(selectedEvent);
      obj.addRecurrenceExDate(selectedEvent.getStartDate());
      update(obj);*/
    }
  }

  @Override
  public CalEventDO newInstance() {
    return null;
  }

  /**
   * This method also returns recurrence events outside the time period of the given filter but affecting the
   * time-period (e. g. older recurrence events without end date or end date inside or after the given time period). If
   * calculateRecurrenceEvents is true, only the recurrence events inside the given time-period are returned, if false
   * only the origin recurrence event (may-be outside the given time-period) is returned.
   *
   * @param filter
   * @param calculateRecurrenceEvents If true, recurrence events inside the given time-period are calculated.
   * @return list of team events (same as {@link #getList(BaseSearchFilter)} but with all calculated and matching
   * recurrence events (if calculateRecurrenceEvents is true). Origin events are of type {@link TeamEventDO},
   * calculated events of type {@link ICalendarEvent}.
   */
  public List<ICalendarEvent> getEventList(final TeamEventFilter filter, final boolean calculateRecurrenceEvents) {
    final List<ICalendarEvent> result = new ArrayList<>();
    List<CalEventDO> list = getList(filter);
    if (CollectionUtils.isNotEmpty(list)) {
      for (final CalEventDO eventDO : list) {
        result.add(eventDO);
      }
    }
    final TeamEventFilter teamEventFilter = filter.clone().setOnlyRecurrence(true);
    final QueryFilter qFilter = buildQueryFilter(teamEventFilter);
    list = getList(qFilter);
    list = selectUnique(list);
    final TimeZone timeZone = ThreadLocalUserContext.getTimeZone();
    if (list != null) {
      for (final CalEventDO eventDO : list) {
        if (!calculateRecurrenceEvents) {
          result.add(eventDO);
          continue;
        }
      }
    }
    return result;
  }

  /**
   * Sets midnight (UTC) of all day events.
   */
  @Override
  protected void onSaveOrModify(final CalEventDO event) {
    super.onSaveOrModify(event);
    Validate.notNull(event.getCalendar());

    if (event.getAllDay()) {
      if (event.getEndDate().getTime() < event.getStartDate().getTime()) {
        throw new UserException("plugins.teamcal.event.duration.error"); // "Duration of time sheet must be at minimum 60s!
      }
    } else if (event.getEndDate().getTime() - event.getStartDate().getTime() < 60000) {
      throw new UserException("plugins.teamcal.event.duration.error"); // "Duration of time sheet must be at minimum 60s!
      // Or, end date is before start date.
    }

    // If is all day event, set start and stop to midnight
    if (event.getAllDay()) {
      final Date startDate = event.getStartDate();
      if (startDate != null) {
        event.setStartDate(CalendarUtils.getUTCMidnightTimestamp(startDate));
      }
      final Date endDate = event.getEndDate();
      if (endDate != null) {
        event.setEndDate(CalendarUtils.getUTCMidnightTimestamp(endDate));
      }
    }
  }

  @Override
  protected void onSave(final CalEventDO event) {
    // create uid if empty
    if (StringUtils.isBlank(event.getUid())) {
      event.setUid(TeamCalConfig.get().createEventUid());
    }
  }

  private QueryFilter buildQueryFilter(final TeamEventFilter filter) {
    final QueryFilter queryFilter = new QueryFilter(filter);
    final Collection<Integer> cals = filter.getTeamCals();
    if (CollectionUtils.isNotEmpty(cals)) {
      queryFilter.add(QueryFilter.isIn("calendar.id", cals));
    } else if (filter.getTeamCalId() != null) {
      queryFilter.add(QueryFilter.eq("calendar.id", filter.getTeamCalId()));
    }
    // Following period extension is needed due to all day events which are stored in UTC. The additional events in the result list not
    // matching the time period have to be removed by caller!
    Date startDate = filter.getStartDate();
    if (startDate != null) {
      startDate = new Date(startDate.getTime() - ONE_DAY);
    }
    Date endDate = filter.getEndDate();
    if (endDate != null) {
      endDate = new Date(endDate.getTime() + ONE_DAY);
    }
    // limit events to load to chosen date view.
    if (startDate != null && endDate != null) {
      queryFilter.add(QueryFilter.or(
              (QueryFilter.or(QueryFilter.between("startDate", startDate, endDate),
                      QueryFilter.between("endDate", startDate, endDate))),
              // get events whose duration overlap with chosen duration.
              (QueryFilter.and(QueryFilter.le("startDate", startDate), QueryFilter.ge("endDate", endDate)))));

    } else if (endDate != null) {
      queryFilter.add(QueryFilter.le("startDate", endDate));
    }
    queryFilter.addOrder(SortProperty.desc("startDate"));
    return queryFilter;
  }

}
