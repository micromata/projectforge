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

package org.projectforge.web.teamcal.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.TeamCalsComparator;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.utils.NumberHelper;

import com.vaynberg.wicket.select2.Response;
import com.vaynberg.wicket.select2.TextChoiceProvider;

public class TeamCalsProvider extends TextChoiceProvider<TeamCalDO>
{
  private static final long serialVersionUID = -7219524032951522997L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalsProvider.class);

  private int pageSize = 20;

  private final TeamCalsComparator calsComparator = new TeamCalsComparator();

  private Collection<TeamCalDO> sortedCals;

  private transient TeamCalCache teamCalCache;

  private transient List<TeamCalDO> additionalCalendarList;

  public TeamCalsProvider(TeamCalCache teamCalCache)
  {
    this(teamCalCache, new ArrayList<>());
  }

  public TeamCalsProvider(TeamCalCache teamCalCache, List<TeamCalDO> additionalCalendars)
  {
    this.teamCalCache = teamCalCache;
    this.additionalCalendarList = additionalCalendars;
  }

//  public static List<Integer> getCalIdList(final Collection<TeamCalDO> teamCals)
//  {
//    final List<Integer> list = new ArrayList<Integer>();
//    if (teamCals != null) {
//      for (final TeamCalDO cal : teamCals) {
//        list.add(cal.getId());
//      }
//    }
//    return list;
//  }
//
//  public static List<TeamCalDO> getCalList(TeamCalCache teamCalCache, final Collection<Integer> teamCalIds)
//  {
//    final List<TeamCalDO> list = new ArrayList<TeamCalDO>();
//    if (teamCalIds != null) {
//      for (final Integer calId : teamCalIds) {
//        final TeamCalDO cal = teamCalCache.getCalendar(calId);
//        if (cal != null) {
//          list.add(cal);
//        } else {
//          log.warn("Calendar with id " + calId + " not found in cache.");
//        }
//      }
//    }
//    return list;
//  }

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
        boolean found = false;
        for(final TeamCalDO calendar : additionalCalendarList) {
          if(calendar.getId().equals(id)) {
            list.add(calendar.getTitle());
          }
        };
        if(found == false) {
          log.warn("TeamCalDO with id '" + id + "' not found. calIds string was: " + calIds);
        }
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
        boolean found = false;
        for(final TeamCalDO calendar : additionalCalendarList) {
          if(calendar.getId().equals(id)) {
            sortedCals.add(calendar);
          }
        };
        if(found == false) {
          log.warn("TeamCalDO with id '" + id + "' not found. calIds string was: " + calIds);
        }
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
      for(final TeamCalDO calendar : additionalCalendarList) {
        if (calendar.isDeleted() == false) {
          sortedCals.add(calendar);
        }
      }
    }
    return sortedCals;
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

  /**
   * @see com.vaynberg.wicket.select2.TextChoiceProvider#getDisplayText(java.lang.Object)
   */
  @Override
  protected String getDisplayText(final TeamCalDO choice)
  {
    return choice.getTitle();
  }

  /**
   * @see com.vaynberg.wicket.select2.TextChoiceProvider#getId(java.lang.Object)
   */
  @Override
  protected Object getId(final TeamCalDO choice)
  {
    return choice.getId();
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#query(java.lang.String, int, com.vaynberg.wicket.select2.Response)
   */
  @Override
  public void query(String term, final int page, final Response<TeamCalDO> response)
  {
    final Collection<TeamCalDO> sortedCals = getSortedCalenders();
    final List<TeamCalDO> result = new ArrayList<TeamCalDO>();
    term = term.toLowerCase();

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

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#toChoices(java.util.Collection)
   */
  @Override
  public Collection<TeamCalDO> toChoices(final Collection<String> ids)
  {
    final List<TeamCalDO> list = new ArrayList<TeamCalDO>();
    if (ids == null) {
      return list;
    }
    for (final String str : ids) {
      final Integer calId = NumberHelper.parseInteger(str);
      if (calId == null) {
        continue;
      }
      final TeamCalDO cal = teamCalCache.getCalendar(calId);
      if (cal != null) {
        list.add(cal);
      }
      for(final TeamCalDO calendar : additionalCalendarList) {
        list.add(calendar);
      }
    }
    return list;
  }
}