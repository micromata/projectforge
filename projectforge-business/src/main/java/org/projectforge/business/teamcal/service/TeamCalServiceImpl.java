package org.projectforge.business.teamcal.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.TeamCalsComparator;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.common.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeamCalServiceImpl implements TeamCalService
{

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalService.class);

  private final TeamCalsComparator calsComparator = new TeamCalsComparator();

  private Collection<TeamCalDO> sortedCals;

  @Autowired
  private TeamCalCache teamCalCache;

  @Override
  public List<Integer> getCalIdList(final Collection<TeamCalDO> teamCals)
  {
    final List<Integer> list = new ArrayList<Integer>();
    if (teamCals != null) {
      for (final TeamCalDO cal : teamCals) {
        list.add(cal.getId());
      }
    }
    return list;
  }

  @Override
  public List<TeamCalDO> getCalList(TeamCalCache teamCalCache, final Collection<Integer> teamCalIds)
  {
    final List<TeamCalDO> list = new ArrayList<TeamCalDO>();
    if (teamCalIds != null) {
      for (final Integer calId : teamCalIds) {
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
  @Override
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
        log.warn("TeamCalDO with id '" + id + "' not found. calIds string was: " + calIds);
      }
    }
    return list;
  }

  /**
   * @param calIds
   * @return
   */
  @Override
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
        log.warn("TeamCalDO with id '" + id + "' not found. calIds string was: " + calIds);
      }
    }
    return sortedCals;
  }

  @Override
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

  @Override
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
    }
    return sortedCals;
  }

}
