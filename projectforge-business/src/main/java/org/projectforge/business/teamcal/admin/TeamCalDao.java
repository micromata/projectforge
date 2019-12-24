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

package org.projectforge.business.teamcal.admin;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.teamcal.admin.TeamCalFilter.OwnerType;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.admin.right.TeamCalRight;
import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.service.UserService;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Repository
public class TeamCalDao extends BaseDao<TeamCalDO> {
  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"usersgroups", "owner.username",
          "owner.firstname",
          "owner.lastname"};

  @Autowired
  private UserDao userDao;

  @Autowired
  private TeamCalCache teamCalCache;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private GroupService groupService;

  @Autowired
  private UserService userService;

  public TeamCalDao() {
    super(TeamCalDO.class);
    userRightId = UserRightId.PLUGIN_CALENDAR;
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  public void setOwner(final TeamCalDO calendar, final Integer userId) {
    final PFUserDO user = userDao.getOrLoad(userId);
    calendar.setOwner(user);
  }

  @Override
  public TeamCalDO newInstance() {
    return new TeamCalDO();
  }

  @Override
  public List<TeamCalDO> getList(final BaseSearchFilter filter) {
    TeamCalFilter myFilter;
    if (filter instanceof TeamCalFilter)
      myFilter = (TeamCalFilter) filter;
    else {
      myFilter = new TeamCalFilter(filter);
    }
    final PFUserDO user = ThreadLocalUserContext.getUser();
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    queryFilter.addOrder(SortProperty.asc("title"));
    final List<TeamCalDO> list = getList(queryFilter);
    if (myFilter.isDeleted()) {
      // No further filtering, show all deleted calendars.
      return list;
    }
    final List<TeamCalDO> result = new ArrayList<>();
    final TeamCalRight right = (TeamCalRight) getUserRight();
    final Integer userId = user.getId();
    final boolean adminAccessOnly = (myFilter.isAdmin()
            && accessChecker.isUserMemberOfAdminGroup(user));
    for (final TeamCalDO cal : list) {
      final boolean isOwn = right.isOwner(user, cal);
      if (isOwn) {
        // User is owner.
        if (adminAccessOnly) {
          continue;
        }
        if (myFilter.isAll() || myFilter.isOwn()) {
          // Calendar matches the filter:
          result.add(cal);
        }
      } else {
        // User is not owner.
        if (myFilter.isAll() || myFilter.isOthers() || adminAccessOnly) {
          if ((myFilter.isFullAccess() && right.hasFullAccess(cal, userId))
                  || (myFilter.isReadonlyAccess() && right.hasReadonlyAccess(cal, userId))
                  || (myFilter.isMinimalAccess() && right.hasMinimalAccess(cal, userId))) {
            // Calendar matches the filter:
            if (!adminAccessOnly) {
              result.add(cal);
            }
          } else if (adminAccessOnly) {
            result.add(cal);
          }
        }
      }
    }
    return result;
  }

  /**
   * Gets a list of all calendars with full access of the current logged-in user as well as the calendars owned by the
   * current logged-in user.
   *
   * @return
   */
  public List<TeamCalDO> getAllCalendarsWithFullAccess() {
    final TeamCalFilter filter = new TeamCalFilter();
    filter.setOwnerType(OwnerType.ALL);
    filter.setFullAccess(true).setReadonlyAccess(false).setMinimalAccess(false);
    return getList(filter);
  }

  /**
   * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
   *
   * @param calendar
   * @param fullAccessGroups
   */
  public void setFullAccessGroups(final TeamCalDO calendar, final Collection<GroupDO> fullAccessGroups) {
    calendar.setFullAccessGroupIds(groupService.getGroupIds(fullAccessGroups));
  }

  public Collection<GroupDO> getSortedFullAccessGroups(final TeamCalDO calendar) {
    return groupService.getSortedGroups(calendar.getFullAccessGroupIds());
  }

  /**
   * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
   *
   * @param calendar
   * @param fullAccessUsers
   */
  public void setFullAccessUsers(final TeamCalDO calendar, final Collection<PFUserDO> fullAccessUsers) {
    calendar.setFullAccessUserIds(userService.getUserIds(fullAccessUsers));
  }

  public Collection<PFUserDO> getSortedFullAccessUsers(final TeamCalDO calendar) {
    return userService.getSortedUsers(calendar.getFullAccessUserIds());
  }

  /**
   * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
   *
   * @param calendar
   * @param readonlyAccessGroups
   */
  public void setReadonlyAccessGroups(final TeamCalDO calendar, final Collection<GroupDO> readonlyAccessGroups) {
    calendar.setReadonlyAccessGroupIds(groupService.getGroupIds(readonlyAccessGroups));
  }

  public Collection<GroupDO> getSortedReadonlyAccessGroups(final TeamCalDO calendar) {
    return groupService.getSortedGroups(calendar.getReadonlyAccessGroupIds());
  }

  /**
   * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
   *
   * @param calendar
   * @param readonlyAccessUsers
   */
  public void setReadonlyAccessUsers(final TeamCalDO calendar, final Collection<PFUserDO> readonlyAccessUsers) {
    calendar.setReadonlyAccessUserIds(userService.getUserIds(readonlyAccessUsers));
  }

  public Collection<PFUserDO> getSortedReadonlyAccessUsers(final TeamCalDO calendar) {
    return userService.getSortedUsers(calendar.getReadonlyAccessUserIds());
  }

  /**
   * Please note: Only the string group.minimalAccessGroupIds will be modified (but not be saved)!
   *
   * @param calendar
   * @param minimalAccessGroups
   */
  public void setMinimalAccessGroups(final TeamCalDO calendar, final Collection<GroupDO> minimalAccessGroups) {
    calendar.setMinimalAccessGroupIds(groupService.getGroupIds(minimalAccessGroups));
  }

  public Collection<GroupDO> getSortedMinimalAccessGroups(final TeamCalDO calendar) {
    return groupService.getSortedGroups(calendar.getMinimalAccessGroupIds());
  }

  /**
   * Please note: Only the string group.minimalAccessGroupIds will be modified (but not be saved)!
   *
   * @param calendar
   * @param minimalAccessUsers
   */
  public void setMinimalAccessUsers(final TeamCalDO calendar, final Collection<PFUserDO> minimalAccessUsers) {
    calendar.setMinimalAccessUserIds(userService.getUserIds(minimalAccessUsers));
  }

  public Collection<PFUserDO> getSortedMinimalAccessUsers(final TeamCalDO calendar) {
    return userService.getSortedUsers(calendar.getMinimalAccessUserIds());
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#getDisplayHistoryEntries(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final TeamCalDO obj) {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (CollectionUtils.isEmpty(list)) {
      return list;
    }
    for (final DisplayHistoryEntry entry : list) {
      if (entry.getPropertyName() == null) {
        continue;
      } else if (entry.getPropertyName().endsWith("GroupIds")) {
        final String oldValue = entry.getOldValue();
        if (StringUtils.isNotBlank(oldValue) && !"null" .equals(oldValue)) {
          final List<String> oldGroupNames = groupService.getGroupNames(oldValue);
          entry.setOldValue(StringHelper.listToString(oldGroupNames, ", ", true));
        }
        final String newValue = entry.getNewValue();
        if (StringUtils.isNotBlank(newValue) && !"null" .equals(newValue)) {
          final List<String> newGroupNames = groupService.getGroupNames(newValue);
          entry.setNewValue(StringHelper.listToString(newGroupNames, ", ", true));
        }
      } else if (entry.getPropertyName().endsWith("UserIds")) {
        final String oldValue = entry.getOldValue();
        if (StringUtils.isNotBlank(oldValue) && !"null" .equals(oldValue)) {
          final List<String> oldGroupNames = userService.getUserNames(oldValue);
          entry.setOldValue(StringHelper.listToString(oldGroupNames, ", ", true));
        }
        final String newValue = entry.getNewValue();
        if (StringUtils.isNotBlank(newValue) && !"null" .equals(newValue)) {
          final List<String> newGroupNames = userService.getUserNames(newValue);
          entry.setNewValue(StringHelper.listToString(newGroupNames, ", ", true));
        }
      }
    }
    return list;
  }

  /**
   * Calls {@link TeamCalCache#setExpired()}.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void afterSaveOrModify(final TeamCalDO obj) {
    super.afterSaveOrModify(obj);
    teamCalCache.setExpired();
  }

  @Override
  protected void afterSave(final TeamCalDO obj) {
    super.afterSave(obj);
    if (obj.getExternalSubscription()) {
      getTeamEventExternalSubscriptionCache().updateCache(obj);
    }
  }

  @Override
  protected void afterUpdate(final TeamCalDO obj, final TeamCalDO dbObj) {
    super.afterUpdate(obj, dbObj);
    if (obj != null
            && dbObj != null
            && obj.getExternalSubscription()
            && !StringUtils.equals(obj.getExternalSubscriptionUrl(), dbObj.getExternalSubscriptionUrl())) {
      // only update if the url has changed!
      getTeamEventExternalSubscriptionCache().updateCache(obj);
    }
    // if calendar is present in subscription cache and is not an external subscription anymore -> cleanup!
    if (obj != null
            && !obj.getExternalSubscription()
            && getTeamEventExternalSubscriptionCache().isExternalSubscribedCalendar(obj.getId())) {
      obj.setExternalSubscriptionCalendarBinary(null);
      obj.setExternalSubscriptionUrl(null);
      obj.setExternalSubscriptionUpdateInterval(null);
      obj.setExternalSubscriptionHash(null);
      getTeamEventExternalSubscriptionCache().updateCache(obj, true);
    }
  }

  private TeamEventExternalSubscriptionCache getTeamEventExternalSubscriptionCache() {
    return applicationContext.getBean(TeamEventExternalSubscriptionCache.class);
  }
}
