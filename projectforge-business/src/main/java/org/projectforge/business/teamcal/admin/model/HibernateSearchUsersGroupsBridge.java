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

package org.projectforge.business.teamcal.admin.model;

import java.util.Collection;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.GroupsComparator;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.common.DatabaseDialect;
import org.projectforge.common.StringHelper;
import org.projectforge.continuousdb.DatabaseSupport;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.context.ApplicationContext;

/**
 * Users and groups bridge for hibernate search.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class HibernateSearchUsersGroupsBridge implements FieldBridge
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(HibernateSearchUsersGroupsBridge.class);

  private final GroupsComparator groupsComparator = new GroupsComparator();

  private final UsersComparator usersComparator = new UsersComparator();

  private GroupDao groupDao;

  private UserDao userDao;

  /**
   * Get all names of groups and users and creates an index containing all user and group names separated by '|'. <br/>
   *
   * @see org.hibernate.search.bridge.FieldBridge#set(java.lang.String, java.lang.Object,
   * org.apache.lucene.document.Document, org.hibernate.search.bridge.LuceneOptions)
   */
  @Override
  public void set(final String name, final Object value, final Document document, final LuceneOptions luceneOptions)
  {
    final TeamCalDO calendar = (TeamCalDO) value;
    final ApplicationContext appContext = ApplicationContextProvider.getApplicationContext();
    if (appContext == null) {
      log.error("ApplicationContext not available!");
      return;
    }
    this.groupDao = appContext.getBean(GroupDao.class);
    this.userDao = appContext.getBean(UserDao.class);
    if (groupDao == null) {
      log.error("GroupDao not found in application context!");
      return;
    }
    final StringBuffer buf = new StringBuffer();

    // query information in Bridge results in a deadlock in HSQLDB
    if (DatabaseSupport.getInstance().getDialect() != DatabaseDialect.HSQL) {
      appendGroups(getSortedGroups(calendar.getFullAccessGroupIds()), buf);
      appendGroups(getSortedGroups(calendar.getReadonlyAccessGroupIds()), buf);
      appendGroups(getSortedGroups(calendar.getMinimalAccessGroupIds()), buf);
      appendUsers(getSortedUsers(calendar.getFullAccessUserIds()), buf);
      appendUsers(getSortedUsers(calendar.getReadonlyAccessUserIds()), buf);
      appendUsers(getSortedUsers(calendar.getMinimalAccessUserIds()), buf);
    }

    if (log.isDebugEnabled() == true) {
      log.debug(buf.toString());
    }
    luceneOptions.addFieldToDocument(name, buf.toString(), document);
  }

  private Collection<GroupDO> getSortedGroups(final String groupIds)
  {
    if (StringUtils.isEmpty(groupIds) == true) {
      return null;
    }
    Collection<GroupDO> sortedGroups = new TreeSet<GroupDO>(groupsComparator);
    final int[] ids = StringHelper.splitToInts(groupIds, ",", false);
    for (final int id : ids) {
      final GroupDO group = groupDao.internalGetById(id);
      if (group != null) {
        sortedGroups.add(group);
      } else {
        log.warn("Group with id '" + id + "' not found in UserGroupCache. groupIds string was: " + groupIds);
      }
    }
    return sortedGroups;
  }

  private Collection<PFUserDO> getSortedUsers(final String userIds)
  {
    if (StringUtils.isEmpty(userIds) == true) {
      return null;
    }
    Collection<PFUserDO> sortedUsers = new TreeSet<PFUserDO>(usersComparator);
    final int[] ids = StringHelper.splitToInts(userIds, ",", false);
    for (final int id : ids) {
      final PFUserDO user = userDao.internalGetById(id);
      if (user != null) {
        sortedUsers.add(user);
      } else {
        log.warn("Group with id '" + id + "' not found in UserGroupCache. groupIds string was: " + userIds);
      }
    }
    return sortedUsers;
  }

  private void appendGroups(final Collection<GroupDO> groups, final StringBuffer buf)
  {
    if (groups == null) {
      return;
    }
    for (final GroupDO group : groups) {
      buf.append(group.getName()).append("|");
    }
  }

  private void appendUsers(final Collection<PFUserDO> users, final StringBuffer buf)
  {
    if (users == null) {
      return;
    }
    for (final PFUserDO user : users) {
      buf.append(user.getFullname()).append(user.getUsername()).append("|");
    }
  }
}
