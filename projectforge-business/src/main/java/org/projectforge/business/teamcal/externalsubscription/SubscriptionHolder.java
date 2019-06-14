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

package org.projectforge.business.teamcal.externalsubscription;

import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import java.io.Serializable;
import java.util.*;

/**
 * Own abstraction of a RangeMap. You can add TeamEvents and access them through their start and end date.
 *
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
public class SubscriptionHolder implements Serializable {
  private static final long serialVersionUID = 1188093201413097949L;

  // one day in milliseconds
  private static final int ONE_DAY = 86400000; // 60*60*24*1000

  // private static final long MIN_DATE_1800 = lendar

  private final List<TeamEventDO> eventList;

  private boolean sorted;

  public SubscriptionHolder() {
    eventList = new ArrayList<TeamEventDO>();
    sorted = false;
  }

  public void clear() {
    eventList.clear();
    sorted = false;
  }

  public void add(final TeamEventDO value) {
    eventList.add(value);
    sorted = false;
  }

  public void sort() {
    // the following comparator compares by startDate
    final Comparator<TeamEventDO> comparator = new Comparator<TeamEventDO>() {
      @Override
      public int compare(final TeamEventDO o1, final TeamEventDO o2) {
        if ((o1 == null || o1.getStartDate() == null) && (o2 == null || o2.getStartDate() == null)) {
          return 0;
        }
        if (o1 == null || o1.getStartDate() == null) {
          return -1;
        }
        if (o2 == null || o2.getStartDate() == null) {
          return 1;
        }
        // at this point, no NPE could occur
        return o1.getStartDate().compareTo(o2.getStartDate());
      }
    };
    Collections.sort(eventList, comparator);
    sorted = true;
  }

  public TeamEventDO getEvent(final String uid) {
    for (final TeamEventDO teamEventDO : eventList) {
      if (teamEventDO.getUid() != null && Objects.equals(uid, teamEventDO.getUid()))
        return teamEventDO;
    }
    return null;
  }

  public List<TeamEventDO> getResultList(final Long startTime, final Long endTime, final boolean minimalAccess) {
    // sorting should by synchronized
    synchronized (this) {
      if (sorted == false) {
        sort();
      }
    }
    final List<TeamEventDO> result = new ArrayList<TeamEventDO>();
    for (final TeamEventDO teamEventDo : eventList) {
      // all our events are sorted, if we find a event which starts
      // after the end date, we can break this iteration
      if (teamEventDo.getStartDate().getTime() > endTime) {
        break;
      }
      if (matches(teamEventDo, startTime, endTime) == true) {
        if (minimalAccess == true) {
          result.add(teamEventDo.createMinimalCopy());
        } else {
          result.add(teamEventDo);
        }
      }
    }
    // and return
    return result;
  }

  public int size() {
    return eventList.size();
  }

  private boolean matches(final TeamEventDO teamEventDo, Long startTime, Long endTime) {
    // Following period extension is needed due to all day events which are stored in UTC. The additional events in the result list not
    // matching the time period have to be removed by caller!
    startTime = startTime - ONE_DAY;
    endTime = endTime + ONE_DAY;

    // the following implementation is inspired by TeamEventDao with the following lines:

    // queryFilter.add(Restrictions.or(
    // (Restrictions.or(Restrictions.between("startDate", startDate, endDate), Restrictions.between("endDate", startDate, endDate))),
    // // get events whose duration overlap with chosen duration.
    // (Restrictions.and(Restrictions.le("startDate", startDate), Restrictions.ge("endDate", endDate)))));

    final Long eventStartTime = teamEventDo.getStartDate() != null ? teamEventDo.getStartDate().getTime()
            : TeamEventDao.MIN_DATE_1800;
    final Long eventEndTime = teamEventDo.getEndDate() != null ? teamEventDo.getEndDate().getTime()
            : TeamEventDao.MAX_DATE_3000;
    if (between(eventStartTime, startTime, endTime) || between(eventEndTime, startTime, endTime)) {
      return true;
    }
    if (eventStartTime <= startTime && eventEndTime >= endTime) {
      return true;
    }
    return false;
  }

  private boolean between(final Long searchTime, final Long startTime, final Long endTime) {
    return searchTime >= startTime && searchTime <= endTime;
  }
}
