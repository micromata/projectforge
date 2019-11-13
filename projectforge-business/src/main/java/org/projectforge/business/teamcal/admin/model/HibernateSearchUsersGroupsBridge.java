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

package org.projectforge.business.teamcal.admin.model;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.projectforge.business.common.BaseUserGroupRightsDO;
import org.projectforge.business.user.GroupsComparator;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.common.DatabaseDialect;
import org.projectforge.common.StringHelper;
import org.projectforge.continuousdb.DatabaseSupport;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.util.Collection;
import java.util.TreeSet;

/**
 * Users and groups bridge for hibernate search.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class HibernateSearchUsersGroupsBridge implements TwoWayStringBridge {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
          .getLogger(HibernateSearchUsersGroupsBridge.class);

  private final GroupsComparator groupsComparator = new GroupsComparator();

  private final UsersComparator usersComparator = new UsersComparator();

  /**
   * Get all names of groups and users and creates an index containing all user and group names separated by '|'. <br/>
   */
  @Override
  public String objectToString(Object object) {
    if (object instanceof String) return (String)object;
    UserGroupCache userGroupCache = UserGroupCache.getTenantInstance();
    final BaseUserGroupRightsDO doObject = (BaseUserGroupRightsDO) object;
    final StringBuilder sb = new StringBuilder();
    // query information in Bridge results in a deadlock in HSQLDB
    if (DatabaseSupport.getInstance().getDialect() != DatabaseDialect.HSQL) {
      appendGroups(getSortedGroups(userGroupCache, doObject.getFullAccessGroupIds()), sb);
      appendGroups(getSortedGroups(userGroupCache, doObject.getReadonlyAccessGroupIds()), sb);
      appendGroups(getSortedGroups(userGroupCache, doObject.getMinimalAccessGroupIds()), sb);
      appendUsers(getSortedUsers(userGroupCache, doObject.getFullAccessUserIds()), sb);
      appendUsers(getSortedUsers(userGroupCache, doObject.getReadonlyAccessUserIds()), sb);
      appendUsers(getSortedUsers(userGroupCache, doObject.getMinimalAccessUserIds()), sb);
    }

    if (log.isDebugEnabled()) {
      log.debug(sb.toString());
    }
    return sb.toString();
  }

  @Override
  public Object stringToObject(String stringValue) {
    // Not supported.
    return null;
  }

  private Collection<GroupDO> getSortedGroups(final UserGroupCache userGroupCache, final String groupIds) {
    if (StringUtils.isEmpty(groupIds)) {
      return null;
    }
    Collection<GroupDO> sortedGroups = new TreeSet<>(groupsComparator);
    final int[] ids = StringHelper.splitToInts(groupIds, ",", false);
    for (final int id : ids) {
      final GroupDO group = userGroupCache.getGroup(id);
      if (group != null) {
        sortedGroups.add(group);
      } else {
        log.warn("Group with id '" + id + "' not found in UserGroupCache. groupIds string was: " + groupIds);
      }
    }
    return sortedGroups;
  }

  private Collection<PFUserDO> getSortedUsers(final UserGroupCache userGroupCache, final String userIds) {
    if (StringUtils.isEmpty(userIds)) {
      return null;
    }
    Collection<PFUserDO> sortedUsers = new TreeSet<>(usersComparator);
    final int[] ids = StringHelper.splitToInts(userIds, ",", false);
    for (final int id : ids) {
      final PFUserDO user = userGroupCache.getUser(id);
      if (user != null) {
        sortedUsers.add(user);
      } else {
        log.warn("Group with id '" + id + "' not found in UserGroupCache. groupIds string was: " + userIds);
      }
    }
    return sortedUsers;
  }

  private void appendGroups(final Collection<GroupDO> groups, final StringBuilder sb) {
    if (groups == null) {
      return;
    }
    for (final GroupDO group : groups) {
      sb.append(group.getName()).append("|");
    }
  }

  private void appendUsers(final Collection<PFUserDO> users, final StringBuilder sb) {
    if (users == null) {
      return;
    }
    for (final PFUserDO user : users) {
      sb.append(user.getFullname()).append(user.getUsername()).append("|");
    }
  }
}
