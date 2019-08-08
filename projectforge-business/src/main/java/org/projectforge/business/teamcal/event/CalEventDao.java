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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.event.model.CalEventDO;
import org.projectforge.business.calendar.event.model.ICalendarEvent;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.calendar.CalendarUtils;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import java.util.*;

@Repository
public class CalEventDao extends BaseDao<CalEventDO>
{
  private static final long ONE_DAY = 1000 * 60 * 60 * 24;

  @Autowired
  private PfEmgrFactory emgrFac;

  @Autowired
  private TenantService tenantService;

  public CalEventDao() {
    super(CalEventDO.class);
    userRightId = UserRightId.CALENDAR_EVENT;
  }

  public CalEventDO getByUid(Integer calendarId, final String uid)
  {
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
  public CalEventDO newInstance()
  {
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
  public List<ICalendarEvent> getEventList(final TeamEventFilter filter, final boolean calculateRecurrenceEvents)
  {
    final List<ICalendarEvent> result = new ArrayList<>();
    List<CalEventDO> list = getList(filter);
    if (CollectionUtils.isNotEmpty(list) == true) {
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
        if (calculateRecurrenceEvents == false) {
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
  protected void onSaveOrModify(final CalEventDO event)
  {
    super.onSaveOrModify(event);
    Validate.notNull(event.getCalendar());

    if (event.getEndDate().getTime() - event.getStartDate().getTime() < 60000) {
      throw new UserException("plugins.teamcal.event.duration.error"); // "Duration of time sheet must be at minimum 60s!
      // Or, end date is before start date.
    }

    // If is all day event, set start and stop to midnight
    if (event.getAllDay() == true) {
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
  protected void onSave(final CalEventDO event)
  {
    // create uid if empty
    if (StringUtils.isBlank(event.getUid())) {
      event.setUid(TeamCalConfig.get().createEventUid());
    }
  }

  private QueryFilter buildQueryFilter(final TeamEventFilter filter)
  {
    final QueryFilter queryFilter = new QueryFilter(filter);
    final Collection<Integer> cals = filter.getTeamCals();
    if (CollectionUtils.isNotEmpty(cals) == true) {
      queryFilter.add(Restrictions.in("calendar.id", cals));
    } else if (filter.getTeamCalId() != null) {
      queryFilter.add(Restrictions.eq("calendar.id", filter.getTeamCalId()));
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
        queryFilter.add(Restrictions.or(
          (Restrictions.or(Restrictions.between("startDate", startDate, endDate),
            Restrictions.between("endDate", startDate, endDate))),
          // get events whose duration overlap with chosen duration.
          (Restrictions.and(Restrictions.le("startDate", startDate), Restrictions.ge("endDate", endDate)))));

    } else if (endDate != null) {
      queryFilter.add(Restrictions.le("startDate", endDate));
    }
    queryFilter.addOrder(Order.desc("startDate"));
    return queryFilter;
  }

}
