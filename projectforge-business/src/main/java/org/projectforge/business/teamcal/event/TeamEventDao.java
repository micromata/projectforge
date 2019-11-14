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

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.RRule;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.projectforge.business.calendar.event.model.ICalendarEvent;
import org.projectforge.business.calendar.event.model.SeriesModificationMode;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.TeamCalDao;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.calendar.CalendarUtils;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.time.DateHolder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Repository
public class TeamEventDao extends BaseDao<TeamEventDO> {
  public static final long MIN_DATE_1800 = -5364662400000L;

  public static final long MAX_DATE_3000 = 32535216000000L;

  /**
   * For storing the selected element of the series in the transient attribute map for correct handling in {@link #onDelete(TeamEventDO)}
   * and {@link #onSaveOrModify(TeamEventDO)} of series (all, future, selected).
   */
  public static final String ATTR_SELECTED_ELEMENT = "selectedSeriesElement";

  /**
   * For series elements: what to modify in {@link #onDelete(TeamEventDO)} and {@link #onSaveOrModify(TeamEventDO)} of series (all, future, selected)?
   */
  public static final String ATTR_SERIES_MODIFICATION_MODE = "seriesModificationMode";

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamEventDao.class);

  private static final long ONE_DAY = 1000 * 60 * 60 * 24;

  private static final Class<?>[] ADDITIONAL_HISTORY_SEARCH_DOS = new Class[]{TeamEventAttendeeDO.class};

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"calendar.id", "calendar.title"};

  private final static String META_SQL_WITH_SPECIAL = " AND e.deleted = :deleted AND e.tenant = :tenant";

  @Autowired
  private TeamCalDao teamCalDao;

  @Autowired
  private TeamCalCache teamCalCache;

  @Autowired
  private TeamEventExternalSubscriptionCache teamEventExternalSubscriptionCache;

  @Autowired
  private PfEmgrFactory emgrFac;

  @Autowired
  private TenantService tenantService;

  public TeamEventDao() {
    super(TeamEventDO.class);
    userRightId = UserRightId.PLUGIN_CALENDAR_EVENT;
  }

  @Override
  public ModificationStatus internalUpdate(final TeamEventDO obj, final boolean checkAccess) {
    logReminderChange(obj);
    return super.internalUpdate(obj, checkAccess);
  }

  private void logReminderChange(final TeamEventDO newObj) {
    boolean reminderHasChanged = false;
    StringBuilder message = new StringBuilder();
    final TeamEventDO dbObj = emgrFac.runRoTrans(emgr -> emgr.selectByPk(TeamEventDO.class, newObj.getId()));
    if ((dbObj.getReminderActionType() == null && newObj.getReminderActionType() != null)
            || (dbObj.getReminderDuration() == null && newObj.getReminderDuration() != null)
            || (dbObj.getReminderDurationUnit() == null && newObj.getReminderDurationUnit() != null)) {
      reminderHasChanged = true;
      message.append("DBObj was null -> new values were set; ");
    }
    if (dbObj.getReminderActionType() != null) {
      if (!dbObj.getReminderActionType().equals(newObj.getReminderActionType())) {
        reminderHasChanged = true;
        message.append(
                "DBObj.getReminderActionType() was " + dbObj.getReminderActionType() + " NewObj.getReminderActionType() is " + newObj.getReminderActionType()
                        + "; ");
      }
    }
    if (dbObj.getReminderDuration() != null) {
      if (!dbObj.getReminderDuration().equals(newObj.getReminderDuration())) {
        reminderHasChanged = true;
        message.append(
                "DBObj.getReminderDuration() was " + dbObj.getReminderActionType() + " NewObj.getReminderDuration() is " + newObj.getReminderActionType() + "; ");
      }
    }
    if (dbObj.getReminderDurationUnit() != null) {
      if (!dbObj.getReminderDurationUnit().equals(newObj.getReminderDurationUnit())) {
        reminderHasChanged = true;
        message.append(
                "DBObj.getReminderDurationUnit() was " + dbObj.getReminderActionType() + " NewObj.getReminderDurationUnit() is " + newObj.getReminderActionType()
                        + "; ");
      }
    }
    if (reminderHasChanged) {
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      boolean changedByWebView =
              Arrays.stream(stackTrace).filter(ste -> ste.getClassName().contains("org.projectforge.web.wicket.EditPageSupport")).count() > 0;
      log.info("TeamEventDao.internalUpdate -> Changed reminder of team event. Changed by: " + (changedByWebView ? "WebView" : "REST") + " Message: " + message
              .toString());
    }
  }

  public List<Integer> getCalIdList(final Collection<TeamCalDO> teamCals) {
    final List<Integer> list = new ArrayList<>();
    if (teamCals != null) {
      for (final TeamCalDO cal : teamCals) {
        list.add(cal.getId());
      }
    }
    return list;
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * @param teamEvent
   * @param teamCalendarId If null, then team calendar will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setCalendar(final TeamEventDO teamEvent, final Integer teamCalendarId) {
    final TeamCalDO teamCal = teamCalDao.getOrLoad(teamCalendarId);
    teamEvent.setCalendar(teamCal);
  }

  public TeamEventDO getByUid(Integer calendarId, final String uid) {
    return this.getByUid(calendarId, uid, true);
  }

  public TeamEventDO getByUid(Integer calendarId, final String uid, final boolean excludeDeleted) {
    if (uid == null) {
      return null;
    }

    final StringBuilder sqlQuery = new StringBuilder();
    final List<Object> params = new ArrayList<>();

    sqlQuery.append("select e from TeamEventDO e where e.uid = :uid AND e.tenant = :tenant");

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
      return emgrFac.runRoTrans(emgr -> emgr.selectSingleAttached(TeamEventDO.class, sqlQuery.toString(), params.toArray()));
    } catch (NoResultException | NonUniqueResultException e) {
      return null;
    }
  }

  @Override
  protected void onChange(final TeamEventDO obj, final TeamEventDO dbObj) {
    handleSeriesUpdates(obj);
    // only increment sequence if PF has ownership!
    if (obj.getOwnership() != null && !obj.getOwnership()) {
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
    }
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
  private void handleSeriesUpdates(final TeamEventDO event) {
    ICalendarEvent selectedEvent = (ICalendarEvent) event.removeTransientAttribute(ATTR_SELECTED_ELEMENT); // Must be removed, otherwise save below will handle this attrs again.
    SeriesModificationMode mode = (SeriesModificationMode) event.removeTransientAttribute(ATTR_SERIES_MODIFICATION_MODE);
    if (selectedEvent == null || mode == null || mode == SeriesModificationMode.ALL) {
      // Nothing to do.
      return;
    }
    TeamEventDO newEvent = event.clone();
    newEvent.setSequence(0);
    TeamEventDO masterEvent = getById(event.getId());
    event.copyValuesFrom(masterEvent); // Restore db fields of master event. Do only modify single or future events.
    if (mode == SeriesModificationMode.FUTURE) {
      TeamEventRecurrenceData recurrenceData = masterEvent.getRecurrenceData(ThreadLocalUserContext.getTimeZone());
      // Set the end date of the master date one day before current date and save this event.
      recurrenceData.setUntil(getUntilDate(selectedEvent.getStartDate()));
      event.setRecurrence(recurrenceData);
      save(newEvent);
      if (log.isDebugEnabled()) {
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
      if (log.isDebugEnabled()) {
        log.debug("Recurrency ex date of master entry is now added: "
                + DateHelper.formatAsUTC(selectedEvent.getStartDate())
                + ". The new string is: "
                + event.getRecurrenceExDate());
        log.debug("The new event is: " + newEvent);
      }
    }
  }

  /**
   * Handles deletion of series element (if any) for future and single events of a series.
   */
  @Override
  public void internalMarkAsDeleted(final TeamEventDO obj) {
    ICalendarEvent selectedEvent = (ICalendarEvent) obj.removeTransientAttribute(ATTR_SELECTED_ELEMENT); // Must be removed, otherwise update below will handle this attrs again.
    SeriesModificationMode mode = (SeriesModificationMode) obj.removeTransientAttribute(ATTR_SERIES_MODIFICATION_MODE);
    if (selectedEvent == null || mode == null || mode == SeriesModificationMode.ALL) {
      // Nothing to do special:
      super.internalMarkAsDeleted(obj);
      return;
    }
    TeamEventDO masterEvent = getById(obj.getId());
    obj.copyValuesFrom(masterEvent); // Restore db fields of master event. Do only modify single or future events.
    if (mode == SeriesModificationMode.FUTURE) {
      TeamEventRecurrenceData recurrenceData = obj.getRecurrenceData(ThreadLocalUserContext.getTimeZone());
      Date recurrenceUntil = getUntilDate(selectedEvent.getStartDate());
      recurrenceData.setUntil(recurrenceUntil);
      obj.setRecurrence(recurrenceData);
      update(obj);
    } else if (mode == SeriesModificationMode.SINGLE) { // only current date
      Validate.notNull(selectedEvent);
      obj.addRecurrenceExDate(selectedEvent.getStartDate());
      update(obj);
    }
  }

  @Override
  protected void onSave(final TeamEventDO event) {
    // set ownership if empty
    if (event.getOwnership() == null) {
      event.setOwnership(true);
    }

    // set DTSTAMP if empty
    if (event.getDtStamp() == null) {
      event.setDtStamp(new Timestamp(event.getCreated().getTime()));
    }

    // create uid if empty
    if (StringUtils.isBlank(event.getUid())) {
      event.setUid(TeamCalConfig.get().createEventUid());
    }
  }

  /**
   * Sets midnight (UTC) of all day events.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#onSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void onSaveOrModify(final TeamEventDO event) {
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
    List<TeamEventDO> list = getList(filter);
    if (CollectionUtils.isNotEmpty(list)) {
      for (final TeamEventDO eventDO : list) {
        if (eventDO.hasRecurrence()) {
          // Added later.
          continue;
        }
        result.add(eventDO);
      }
    }
    final TeamEventFilter teamEventFilter = filter.clone().setOnlyRecurrence(true);
    final QueryFilter qFilter = buildQueryFilter(teamEventFilter);
    qFilter.add(QueryFilter.isNotNull("recurrenceRule"));
    list = getList(qFilter);
    list = selectUnique(list);
    // add all abo events
    final List<TeamEventDO> recurrenceEvents = teamEventExternalSubscriptionCache
            .getRecurrenceEvents(teamEventFilter);
    if (recurrenceEvents != null && recurrenceEvents.size() > 0) {
      list.addAll(recurrenceEvents);
    }
    final TimeZone timeZone = ThreadLocalUserContext.getTimeZone();
    if (list != null) {
      for (final TeamEventDO eventDO : list) {
        if (!eventDO.hasRecurrence()) {
          // This event was handled above.
          continue;
        }
        if (!calculateRecurrenceEvents) {
          result.add(eventDO);
          continue;
        }
        final Collection<ICalendarEvent> events = this.rollOutRecurrenceEvents(teamEventFilter.getStartDate(), teamEventFilter.getEndDate(), eventDO, timeZone);
        if (events == null) {
          continue;
        }
        for (final ICalendarEvent event : events) {
          if (!matches(event.getStartDate(), event.getEndDate(), event.getAllDay(), teamEventFilter)) {
            continue;
          }
          result.add(event);
        }
      }
    }
    return result;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#getListForSearchDao(org.projectforge.framework.persistence.api.BaseSearchFilter)
   */
  @Override
  public List<TeamEventDO> getListForSearchDao(final BaseSearchFilter filter) {
    final TeamEventFilter teamEventFilter = new TeamEventFilter(filter); // May-be called by SeachPage
    final Collection<TeamCalDO> allAccessibleCalendars = teamCalCache.getAllAccessibleCalendars();
    if (CollectionUtils.isEmpty(allAccessibleCalendars)) {
      // No calendars accessible, nothing to search.
      return new ArrayList<>();
    }
    teamEventFilter.setTeamCals(getCalIdList(allAccessibleCalendars));
    return getList(teamEventFilter);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#getList(org.projectforge.framework.persistence.api.BaseSearchFilter)
   */
  @Override
  public List<TeamEventDO> getList(final BaseSearchFilter filter) {
    final TeamEventFilter teamEventFilter;
    if (filter instanceof TeamEventFilter) {
      teamEventFilter = ((TeamEventFilter) filter).clone();
    } else {
      teamEventFilter = new TeamEventFilter(filter);
    }
    if (CollectionUtils.isEmpty(teamEventFilter.getTeamCals()) && teamEventFilter.getTeamCalId() == null) {
      return new ArrayList<>();
    }
    final QueryFilter qFilter = buildQueryFilter(teamEventFilter);
    final List<TeamEventDO> list = getList(qFilter);
    final List<TeamEventDO> result = new ArrayList<>();
    if (list != null) {
      for (final TeamEventDO event : list) {
        if (matches(event.getStartDate(), event.getEndDate(), event.getAllDay(), teamEventFilter)) {
          result.add(event);
        }
      }
    }
    // subscriptions
    final List<Integer> alreadyAdded = new ArrayList<>();
    // precondition for abos: existing teamcals in filter
    if (teamEventFilter.getTeamCals() != null) {
      for (final Integer calendarId : teamEventFilter.getTeamCals()) {
        if (teamEventExternalSubscriptionCache.isExternalSubscribedCalendar(calendarId)) {
          addEventsToList(teamEventFilter, result, teamEventExternalSubscriptionCache, calendarId);
          alreadyAdded.add(calendarId);
        }
      }
    }
    // if the getTeamCalId is not null and we do not added this before, do it now
    final Integer teamCalId = teamEventFilter.getTeamCalId();
    if (teamCalId != null && !alreadyAdded.contains(teamCalId)) {
      if (teamEventExternalSubscriptionCache.isExternalSubscribedCalendar(teamCalId)) {
        addEventsToList(teamEventFilter, result, teamEventExternalSubscriptionCache, teamCalId);
      }
    }
    return result;
  }

  /**
   * Get all locations of the user's calendar events (not deleted ones) with modification date within last year.
   *
   * @param searchString
   */
  @SuppressWarnings("unchecked")
  public List<String> getLocationAutocompletion(final String searchString, final TeamCalDO[] calendars) {
    if (calendars == null || calendars.length == 0) {
      return null;
    }
    if (StringUtils.isBlank(searchString)) {
      return null;
    }
    checkLoggedInUserSelectAccess();
    final String s = "select distinct location from "
            + clazz.getSimpleName()
            + " t where deleted=false and t.calendar in :cals and lastUpdate > :lastUpdate and lower(t.location) like :location) order by t.location";
    final TypedQuery<String> query = em.createQuery(s, String.class);
    query.setParameter("cals", calendars);
    final DateHolder dh = new DateHolder();
    dh.add(Calendar.YEAR, -1);
    query.setParameter("lastUpdate", dh.getDate());
    query.setParameter("location", "%" + StringUtils.lowerCase(searchString) + "%");
    return query.getResultList();
  }

  private void addEventsToList(final TeamEventFilter teamEventFilter, final List<TeamEventDO> result,
                               final TeamEventExternalSubscriptionCache aboCache, final Integer calendarId) {
    final Date startDate = teamEventFilter.getStartDate();
    final Date endDate = teamEventFilter.getEndDate();
    final Long startTime = startDate == null ? 0 : startDate.getTime();
    final Long endTime = endDate == null ? MAX_DATE_3000 : endDate.getTime();
    final List<TeamEventDO> events = aboCache.getEvents(calendarId, startTime, endTime);
    if (events != null && events.size() > 0) {
      result.addAll(events);
    }
  }

  private boolean matches(final Date eventStartDate, final Date eventEndDate, final boolean allDay,
                          final TeamEventFilter teamEventFilter) {
    final Date startDate = teamEventFilter.getStartDate();
    final Date endDate = teamEventFilter.getEndDate();
    if (allDay) {
      // Check date match:
      final Calendar utcCal = Calendar.getInstance(DateHelper.UTC);
      utcCal.setTime(eventStartDate);
      if (startDate != null && eventEndDate.before(startDate)) {
        // Check same day (eventStartDate in UTC and startDate of filter in user's time zone):
        final Calendar userCal = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
        userCal.setTime(startDate);
        return CalendarUtils.isSameDay(utcCal, utcCal);
      }
      if (endDate != null && eventStartDate.after(endDate)) {
        // Check same day (eventEndDate in UTC and endDate of filter in user's time zone):
        final Calendar userCal = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
        userCal.setTime(endDate);
        return CalendarUtils.isSameDay(utcCal, utcCal);
      }
      return true;
    } else {
      // Check start and stop date due to extension of time period of buildQueryFilter:
      if (startDate != null && eventEndDate.before(startDate)) {
        return false;
      }
      return endDate == null || !eventStartDate.after(endDate);
    }
  }

  /**
   * The time period of the filter will be extended by one day. This is needed due to all day events which are stored in
   * UTC. The additional events in the result list not matching the time period have to be removed by caller!
   *
   * @param filter
   * @return
   */
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
      if (!filter.isOnlyRecurrence()) {
        queryFilter.add(QueryFilter.or(
                (QueryFilter.or(QueryFilter.between("startDate", startDate, endDate),
                        QueryFilter.between("endDate", startDate, endDate))),
                // get events whose duration overlap with chosen duration.
                (QueryFilter.and(QueryFilter.le("startDate", startDate), QueryFilter.ge("endDate", endDate)))));
      } else {
        queryFilter.add(
                // "startDate" < endDate && ("recurrenceUntil" == null || "recurrenceUntil" > startDate)
                (QueryFilter.and(QueryFilter.lt("startDate", endDate),
                        QueryFilter.or(QueryFilter.isNull("recurrenceUntil"),
                                QueryFilter.gt("recurrenceUntil", startDate)))));
      }
    } else if (startDate != null) {
      if (!filter.isOnlyRecurrence()) {
        queryFilter.add(QueryFilter.ge("startDate", startDate));
      } else {
        // This branch is reached for subscriptions and calendar downloads.
        queryFilter.add(
                // "recurrenceUntil" == null || "recurrenceUntil" > startDate
                QueryFilter.or(QueryFilter.isNull("recurrenceUntil"), QueryFilter.gt("recurrenceUntil", startDate)));
      }
    } else if (endDate != null) {
      queryFilter.add(QueryFilter.le("startDate", endDate));
    }
    queryFilter.addOrder(SortProperty.desc("startDate"));
    if (log.isDebugEnabled()) {
      log.debug(ToStringBuilder.reflectionToString(filter));
    }
    return queryFilter;
  }

  /**
   * Gets history entries of super and adds all history entries of the TeamEventAttendeeDO children.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#getDisplayHistoryEntries(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final TeamEventDO obj) {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (!hasLoggedInUserHistoryAccess(obj, false)) {
      return list;
    }
    if (CollectionUtils.isNotEmpty(obj.getAttendees())) {
      for (final TeamEventAttendeeDO attendee : obj.getAttendees()) {
        final List<DisplayHistoryEntry> entries = internalGetDisplayHistoryEntries(attendee);
        for (final DisplayHistoryEntry entry : entries) {
          final String propertyName = entry.getPropertyName();
          if (propertyName != null) {
            entry.setPropertyName(
                    attendee.toString() + ":" + entry.getPropertyName()); // Prepend user name or url to identify.
          } else {
            entry.setPropertyName(attendee.toString());
          }
        }
        list.addAll(entries);
      }
    }
    list.sort((o1, o2) -> (o2.getTimestamp().compareTo(o1.getTimestamp())));
    return list;
  }

  @Override
  protected Class<?>[] getAdditionalHistorySearchDOs() {
    return ADDITIONAL_HISTORY_SEARCH_DOS;
  }

  /**
   * Returns also true, if idSet contains the id of any attendee.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#contains(Set, ExtendedBaseDO)
   */
  @Override
  public boolean contains(final Set<Integer> idSet, final TeamEventDO entry) {
    if (super.contains(idSet, entry)) {
      return true;
    }
    for (final TeamEventAttendeeDO pos : entry.getAttendees()) {
      if (idSet.contains(pos.getId())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public TeamEventDO newInstance() {
    return new TeamEventDO();
  }

  /**
   * @return the log
   */
  public Logger getLog() {
    return log;
  }

  /**
   * @param teamCalDao the teamCalDao to set
   */
  public void setTeamCalDao(final TeamCalDao teamCalDao) {
    this.teamCalDao = teamCalDao;
  }

  public Collection<ICalendarEvent> rollOutRecurrenceEvents(final java.util.Date startDate, final java.util.Date endDate,
                                                            final TeamEventDO event, final java.util.TimeZone timeZone) {
    if (!event.hasRecurrence()) {
      return null;
    }
    final Recur recur = event.getRecurrenceObject();

    if (recur == null) {
      // Shouldn't happen:
      return null;
    }
    final java.util.TimeZone timeZone4Calc = timeZone;
    final String eventStartDateString = event.getAllDay()
            ? DateHelper.formatIsoDate(event.getStartDate(), timeZone) : DateHelper
            .formatIsoTimestamp(event.getStartDate(), DateHelper.UTC);
    java.util.Date eventStartDate = event.getStartDate();
    if (log.isDebugEnabled()) {
      log.debug("---------- startDate=" + DateHelper.formatIsoTimestamp(eventStartDate, timeZone) + ", timeZone="
              + timeZone.getID());
    }
    net.fortuna.ical4j.model.TimeZone ical4jTimeZone;
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
    final Collection<ICalendarEvent> col = new ArrayList<>();
    if (dateList != null) {
      OuterLoop:
      for (final Object obj : dateList) {
        final net.fortuna.ical4j.model.DateTime dateTime = (net.fortuna.ical4j.model.DateTime) obj;
        final String isoDateString = event.getAllDay() ? DateHelper.formatIsoDate(dateTime, timeZone)
                : DateHelper.formatIsoTimestamp(dateTime, DateHelper.UTC);
        if (exDates != null && exDates.size() > 0) {
          for (Date exDate : exDates) {
            if (!event.getAllDay()) {
              Date recurDateJavaUtil = new Date(dateTime.getTime());
              if (recurDateJavaUtil.equals(exDate)) {
                if (log.isDebugEnabled()) {
                  log.debug("= ex-dates equals: " + isoDateString + " == " + exDate);
                }
                // this date is part of ex dates, so don't use it.
                continue OuterLoop;
              }
            } else {
              // Allday event.
              final String isoExDateString = DateHelper.formatIsoDate(exDate, DateHelper.UTC);
              if (isoDateString.equals(isoExDateString)) {
                if (log.isDebugEnabled()) {
                  log.debug(String.format("= ex-dates equals: %s == %s", isoDateString, isoExDateString));
                }
                // this date is part of ex dates, so don't use it.
                continue OuterLoop;
              }
            }
            if (log.isDebugEnabled()) {
              log.debug("ex-dates not equals: " + isoDateString + " != " + exDate);
            }
          }
        }
        if (isoDateString.equals(eventStartDateString)) {
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
    if (log.isDebugEnabled()) {
      for (final ICalendarEvent ev : col) {
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
}
