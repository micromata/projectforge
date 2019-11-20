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

package org.projectforge.business.humanresources;

import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.timesheet.TimesheetFilter;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.IDao;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHolder;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class HRViewDao implements IDao<HRViewData> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRViewDao.class);

  @Autowired
  private TimesheetDao timesheetDao;

  @Autowired
  private HRPlanningDao hrPlanningDao;

  @Autowired
  private UserDao userDao;

  /**
   * Rows contains the users and the last row contains the total sums. Columns of each rows are the man days of the
   * projects (see getProjectNames)
   */
  public HRViewData getResources(final HRFilter filter) {
    final HRViewData data = new HRViewData(filter);
    if (filter.getStartTime() == null) {
      final DayHolder day = new DayHolder();
      day.setBeginOfWeek();
      filter.setStartTime(day.getDate());
    }
    if (filter.getStopTime() == null) {
      final DayHolder day = new DayHolder(filter.getStartTime());
      day.setEndOfWeek();
      filter.setStopTime(day.getDate());
    }
    if (filter.isShowBookedTimesheets()) {
      final TimesheetFilter tsFilter = new TimesheetFilter();
      tsFilter.setStartTime(filter.getStartTime());
      tsFilter.setStopTime(filter.getStopTime());
      final List<TimesheetDO> sheets = timesheetDao.getList(tsFilter);
      final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
      for (final TimesheetDO sheet : sheets) {
        final PFUserDO user = userGroupCache.getUser(sheet.getUserId());
        if (user == null) {
          log.error("Oups, user of time sheet is null or unknown? Ignoring entry: " + sheet);
          continue;
        }
        final TaskTree taskTree = TaskTreeHelper.getTaskTree();
        final ProjektDO projekt = taskTree.getProjekt(sheet.getTaskId());
        final Object targetObject = getTargetObject(userGroupCache, filter, projekt);
        if (targetObject == null) {
          data.addTimesheet(sheet, user);
        } else if (targetObject instanceof ProjektDO) {
          data.addTimesheet(sheet, user, (ProjektDO) targetObject);
        } else if (targetObject instanceof KundeDO) {
          data.addTimesheet(sheet, user, (KundeDO) targetObject);
        } else {
          log.error("Target object of type " + targetObject + " not supported.");
          data.addTimesheet(sheet, user);
        }
      }
    }
    if (filter.isShowPlanning()) {
      final HRPlanningFilter hrFilter = new HRPlanningFilter();
      final DateHolder date = new DateHolder(filter.getStartTime());
      hrFilter.setStartTime(date.getSQLDate()); // Considers the user's time zone.
      date.setDate(filter.getStopTime());
      hrFilter.setStopTime(date.getSQLDate()); // Considers the user's time zone.
      final List<HRPlanningDO> plannings = hrPlanningDao.getList(hrFilter);
      final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
      for (final HRPlanningDO planning : plannings) {
        if (planning.getEntries() == null) {
          continue;
        }
        for (final HRPlanningEntryDO entry : planning.getEntries()) {
          if (entry.isDeleted()) {
            continue;
          }
          final PFUserDO user = userGroupCache.getUser(planning.getUserId());
          final ProjektDO projekt = entry.getProjekt();
          final Object targetObject = getTargetObject(userGroupCache, filter, projekt);
          if (targetObject == null) {
            data.addHRPlanningEntry(entry, user);
          } else if (targetObject instanceof ProjektDO) {
            data.addHRPlanningEntry(entry, user, (ProjektDO) targetObject);
          } else if (targetObject instanceof KundeDO) {
            data.addHRPlanningEntry(entry, user, (KundeDO) targetObject);
          } else {
            log.error("Target object of type " + targetObject + " not supported.");
            data.addHRPlanningEntry(entry, user);
          }
        }
      }
    }
    if (filter.isOnlyMyProjects()) {
      // remove all user entries which have no planning or booking on my projects.
      final List<HRViewUserData> list = data.getUserDatas();
      if (list != null) {
        final Iterator<HRViewUserData> it = list.iterator();
        while (it.hasNext()) {
          final HRViewUserData entry = it.next();
          boolean hasEntries = false;
          if (entry.entries != null) {
            for (final HRViewUserEntryData entryData : entry.entries) {
              if (entryData.projekt != null || entryData.kunde != null) {
                hasEntries = true;
                break;
              }
            }
          }
          if (!hasEntries) {
            it.remove();
          }
        }
      }
    }
    return data;
  }

  /**
   * Returns a list of all users which are accessible by the current logged in user and not planned in the given
   * HRViewData object.
   *
   * @return Result list (may be empty but never null).
   */
  public List<PFUserDO> getUnplannedResources(final HRViewData data) {
    final List<PFUserDO> users = new ArrayList<>();
    final QueryFilter queryFilter = new QueryFilter(new BaseSearchFilter());
    queryFilter.addOrder(SortProperty.asc("firstname")).addOrder(SortProperty.asc("lastname"));
    final List<PFUserDO> allUsers = userDao.getList(queryFilter);
    if (allUsers != null) {
      for (final PFUserDO user : allUsers) {
        final HRViewUserData userData = data.getUserData(user);
        if (userData == null || !NumberHelper.isNotZero(userData.getPlannedDaysSum())) {
          users.add(user);
        }
      }
    }
    return users;
  }

  /**
   * Return the target object (ProjektDO, KundeDO or null) to which the entry (time sheet or planning) should be
   * assigned to. The results depends on the filter settings.
   *
   * @param filter
   * @param projekt
   * @return
   */
  private Object getTargetObject(final UserGroupCache userGroupCache, final HRFilter filter, final ProjektDO projekt) {
    if (projekt == null) {
      return null;
    }
    final KundeDO kunde = projekt.getKunde();
    if (filter.isOnlyMyProjects()) {
      if (isMyProject(userGroupCache, projekt)) {
        if (filter.isAllProjectsGroupedByCustomer()) {
          return kunde;
        } else {
          return projekt;
        }
      } else {
        return null;
      }
    } else if (filter.isAllProjectsGroupedByCustomer()) {
      return kunde;
    } else if (filter.isOtherProjectsGroupedByCustomer()) {
      if (isMyProject(userGroupCache, projekt)) {
        return projekt;
      } else {
        return kunde;
      }
    } else {
      // Show all projects
      return projekt;
    }
  }

  private boolean isMyProject(final UserGroupCache userGroupCache, final ProjektDO projekt) {
    return (projekt != null && projekt.getProjektManagerGroup() != null
            && userGroupCache.isLoggedInUserMemberOfGroup(projekt
            .getProjektManagerGroupId()));
  }

  /**
   * Throws UnsupportedOperationException.
   *
   * @see org.projectforge.framework.persistence.api.IDao#getList(org.projectforge.framework.persistence.api.BaseSearchFilter)
   */
  @Override
  public List<HRViewData> getList(final BaseSearchFilter filter) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return false.
   * @see org.projectforge.framework.persistence.api.IDao#isHistorizable()
   */
  @Override
  public boolean isHistorizable() {
    return false;
  }

  /**
   * @return true.
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user) {
    return hrPlanningDao.hasInsertAccess(user);
  }
}
