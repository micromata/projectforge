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

package org.projectforge.business.group.service;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.GroupsComparator;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GroupServiceImpl implements GroupService
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GroupServiceImpl.class);

  private UserGroupCache userGroupCache;

  @Autowired
  private GroupDao groupDao;

  private final GroupsComparator groupsComparator = new GroupsComparator();


  @Override
  public GroupDO getGroup(final Integer groupId)
  {
    return groupDao.getOrLoad(groupId);
  }

  @Override
  public String getGroupname(final Integer groupId)
  {
    final GroupDO group = getGroup(groupId);
    return group == null ? null : group.getName();
  }

  @Override
  public String getGroupnames(final Integer userId)
  {
    final Set<Integer> groupSet = getUserGroupCache().getUserGroupIdMap().get(userId);
    if (groupSet == null) {
      return "";
    }
    final List<String> list = new ArrayList<>();
    for (final Integer groupId : groupSet) {
      final GroupDO group = userGroupCache.getGroup(groupId);
      if (group != null) {
        list.add(group.getName());
      } else {
        log.error("Group with id " + groupId + " not found.");
      }
    }
    return StringHelper.listToString(list, "; ", true);
  }

  /**
   * @param groupIds
   * @return
   */
  @Override
  public List<String> getGroupNames(final String groupIds)
  {
    if (StringUtils.isEmpty(groupIds)) {
      return null;
    }
    final int[] ids = StringHelper.splitToInts(groupIds, ",", false);
    final List<String> list = new ArrayList<>();
    for (final int id : ids) {
      final GroupDO group = userGroupCache.getGroup(id);
      if (group != null) {
        list.add(group.getName());
      } else {
        log.warn("Group with id '" + id + "' not found in UserGroupCache. groupIds string was: " + groupIds);
      }
    }
    return list;
  }

  /**
   *
   * @param groupIds
   * @return
   */
  @Override
  public Collection<GroupDO> getSortedGroups(final String groupIds)
  {
    if (StringUtils.isEmpty(groupIds)) {
      return null;
    }
    Collection<GroupDO> sortedGroups = new TreeSet<>(groupsComparator);
    final int[] ids = StringHelper.splitToInts(groupIds, ",", false);
    for (final int id : ids) {
      final GroupDO group = getGroup(id);
      if (group != null) {
        sortedGroups.add(group);
      } else {
        log.warn("Group with id '" + id + "' not found in UserGroupCache. groupIds string was: " + groupIds);
      }
    }
    return sortedGroups;
  }

  @Override
  public String getGroupIds(final Collection<GroupDO> groups)
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final GroupDO group : groups) {
      if (group.getId() != null) {
        first = StringHelper.append(buf, first, String.valueOf(group.getId()), ",");
      }
    }
    return buf.toString();
  }

  @Override
  public Collection<GroupDO> getSortedGroups()
  {

      final Collection<GroupDO> allGroups = getUserGroupCache().getAllGroups();
    TreeSet<GroupDO> sortedGroups = new TreeSet<>(groupsComparator);
      final PFUserDO loggedInUser = ThreadLocalUserContext.getUser();
      for (final GroupDO group : allGroups) {
        if (!group.isDeleted() && groupDao.hasUserSelectAccess(loggedInUser, group, false)) {
          sortedGroups.add(group);
        }
      }
    return sortedGroups;
  }

  /**
   * @return the userGroupCache
   */
  private UserGroupCache getUserGroupCache()
  {
    if (userGroupCache == null) {
      userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    }
    return userGroupCache;
  }

  /**
   * ONLY for Tests
   *
   * @param userGroupCache
   */
  public void setUserGroupCache(UserGroupCache userGroupCache)
  {
    this.userGroupCache = userGroupCache;
  }

  /**
   * ONLY for Tests
   *
   * @param groupDao
   */
  public void setGroupDao(GroupDao groupDao)
  {
    this.groupDao = groupDao;
  }

  @Override
  public List<GroupDO> getAllGroups()
  {
    return groupDao.internalLoadAll();
  }

}
