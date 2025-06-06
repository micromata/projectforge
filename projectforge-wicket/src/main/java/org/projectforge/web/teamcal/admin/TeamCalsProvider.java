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

package org.projectforge.web.teamcal.admin;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.TeamCalsComparator;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.utils.NumberHelper;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public class TeamCalsProvider extends ChoiceProvider<TeamCalDO>
{
  private static final long serialVersionUID = -7219524032951522997L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamCalsProvider.class);

  private int pageSize = 20;

  private final TeamCalsComparator calsComparator = new TeamCalsComparator();

  private Collection<TeamCalDO> sortedCals;

  private transient TeamCalCache teamCalCache;

  private transient boolean onlyFullAccessCalendar = false;

  private List<TeamCalDO> additionalCalendarList;

  public TeamCalsProvider(TeamCalCache teamCalCache)
  {
    this(teamCalCache, false);
  }

  public TeamCalsProvider(TeamCalCache teamCalCache, boolean onlyFullAccessCalendar)
  {
    this(teamCalCache, onlyFullAccessCalendar, null);
  }

  public TeamCalsProvider(TeamCalCache teamCalCache, boolean onlyFullAccessCalendar, List<TeamCalDO> additionalCalendarList)
  {
    this.teamCalCache = teamCalCache;
    this.onlyFullAccessCalendar = onlyFullAccessCalendar;
    this.additionalCalendarList = additionalCalendarList;
  }

  public static List<Long> getCalIdList(final Collection<TeamCalDO> teamCals)
  {
    final List<Long> list = new ArrayList<>();
    if (teamCals != null) {
      for (final TeamCalDO cal : teamCals) {
        list.add(cal.getId());
      }
    }
    return list;
  }

  public static List<TeamCalDO> getCalList(TeamCalCache teamCalCache, final Collection<Long> teamCalIds)
  {
    final List<TeamCalDO> list = new ArrayList<>();
    if (teamCalIds != null) {
      for (final Long calId : teamCalIds) {
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
    final long[] ids = StringHelper.splitToLongs(calIds, ",", false);
    final List<String> list = new ArrayList<String>();
    for (final long id : ids) {
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
    final long[] ids = StringHelper.splitToLongs(calIds, ",", false);
    for (final long id : ids) {
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
    final StringBuilder buf = new StringBuilder();
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
      final Collection<TeamCalDO> allCalendars = getCalendarList();
      sortedCals = new TreeSet<TeamCalDO>(calsComparator);
      for (final TeamCalDO cal : allCalendars) {
        if (cal.getDeleted() == false) {
          sortedCals.add(cal);
        }
      }
    }
    return sortedCals;
  }

  private Collection<TeamCalDO> getCalendarList()
  {
    Collection<TeamCalDO> result = null;
    if (onlyFullAccessCalendar) {
      result = teamCalCache.getAllFullAccessCalendars();
    } else {
      result = teamCalCache.getAllAccessibleCalendars();
    }
    if (this.additionalCalendarList != null && this.additionalCalendarList.size() > 0) {
      result.addAll(this.additionalCalendarList);
    }
    return result;
  }

  /**
   * @param pageSize the pageSize to set
   * @return this for chaining.
   */
  public TeamCalsProvider setPageSize(final int pageSize)
  {
    this.pageSize = pageSize;
    return this;
  }

  @Override
  public String getDisplayValue(final TeamCalDO choice)
  {
    return choice.getTitle();
  }

  @Override
  public String getIdValue(final TeamCalDO choice)
  {
    return String.valueOf(choice.getId());
  }

  @Override
  public void query(String term, final int page, final Response<TeamCalDO> response)
  {
    final Collection<TeamCalDO> sortedCals = getSortedCalenders();
    final List<TeamCalDO> result = new ArrayList<>();
    term = term != null ? term.toLowerCase() : "";

    final int offset = page * pageSize;

    int matched = 0;
    boolean hasMore = false;
    for (final TeamCalDO cal : sortedCals) {
      if (result.size() == pageSize) {
        hasMore = true;
        break;
      }
      final String title = cal.getTitle();
      if (title != null && title.toLowerCase().contains(term) == true) {
        matched++;
        if (matched > offset) {
          result.add(cal);
        }
      }
    }
    response.addAll(result);
    response.setHasMore(hasMore);
  }

  @Override
  public Collection<TeamCalDO> toChoices(final Collection<String> ids)
  {
    final List<TeamCalDO> list = new ArrayList<TeamCalDO>();
    if (ids == null) {
      return list;
    }
    for (final String str : ids) {
      final Long calId = NumberHelper.parseLong(str);
      if (calId == null) {
        continue;
      }
      final TeamCalDO cal = teamCalCache.getCalendar(calId);
      if (cal != null) {
        list.add(cal);
      }
    }
    return list;
  }
}
