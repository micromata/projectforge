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

package org.projectforge.business.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.LockMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.login.Login;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class GroupDao extends BaseDao<GroupDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(GroupDao.class);

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "assignedUsers.username",
      "assignedUsers.firstname",
      "assignedUsers.lastname" };

  @Autowired
  private UserDao userDao;

  private boolean doHistoryUpdate = true;

  // private final GroupsProvider groupsProvider = new GroupsProvider();

  public GroupDao()
  {
    super(GroupDO.class);
    this.supportAfterUpdate = true;
  }

  /**
   * ONLY FOR GENERATING TEST DATA
   *
   * @param supportAfterUpdate
   */
  public void setDoHistoryUpdate(boolean historyUpdate)
  {
    this.doHistoryUpdate = historyUpdate;
  }

  public QueryFilter getDefaultFilter()
  {
    final QueryFilter queryFilter = new QueryFilter();
    return queryFilter;
  }

  @Override
  public List<GroupDO> getList(final BaseSearchFilter filter)
  {
    final GroupFilter myFilter;
    if (filter instanceof GroupFilter) {
      myFilter = (GroupFilter) filter;
    } else {
      myFilter = new GroupFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (Login.getInstance().hasExternalUsermanagementSystem() == true) {
      // Check hasExternalUsermngmntSystem because otherwise the filter is may-be preset for an user and the user can't change the filter
      // (because the fields aren't visible).
      if (myFilter.getLocalGroup() != null) {
        queryFilter.add(Restrictions.eq("localGroup", myFilter.getLocalGroup()));
      }
    }
    queryFilter.addOrder(Order.asc("name"));
    return getList(queryFilter);
  }

  /**
   * Does a group with the given name already exists? Works also for existing users (if group name was modified).
   *
   * @param username
   * @return
   */
  @SuppressWarnings("unchecked")
  public boolean doesGroupnameAlreadyExist(final GroupDO group)
  {
    Validate.notNull(group);
    List<GroupDO> list = null;
    if (group.getId() == null) {
      // New group
      list = (List<GroupDO>) getHibernateTemplate().find("from GroupDO g where g.name = ?", group.getName());
    } else {
      // group already exists. Check maybe changed name:
      list = (List<GroupDO>) getHibernateTemplate().find("from GroupDO g where g.name = ? and pk <> ?",
          new Object[] { group.getName(), group.getId() });
    }
    if (CollectionUtils.isNotEmpty(list) == true) {
      return true;
    }
    return false;
  }

  /**
   * Please note: Any existing assigned user in group object is ignored!
   *
   * @param group
   * @param assignedUsers Full list of all users which have to assigned to this group.
   * @return
   */
  public void setAssignedUsers(final GroupDO group, final Collection<PFUserDO> assignedUsers) throws AccessException
  {
    final Set<PFUserDO> origAssignedUsers = group.getAssignedUsers();
    if (origAssignedUsers != null) {
      final Iterator<PFUserDO> it = origAssignedUsers.iterator();
      while (it.hasNext() == true) {
        final PFUserDO user = it.next();
        if (assignedUsers.contains(user) == false) {
          it.remove();
        }
      }
    }
    for (final PFUserDO user : assignedUsers) {
      final PFUserDO dbUser = userDao.internalGetById(user.getId());
      if (dbUser == null) {
        throw new RuntimeException("User '"
            + user.getId()
            + "' not found. Could not add this unknown user to new group: "
            + group.getName());
      }
      if (origAssignedUsers == null || origAssignedUsers.contains(dbUser) == false) {
        group.addUser(dbUser);
      }
    }
  }

  // /**
  // * Please note: Only the string group.nestedGroups will be modified (but not be saved)!
  // * @param group
  // * @param nestedGroups Full list of all nested groups which have to assigned to this group.
  // * @return
  // */
  // public void setNestedGroups(final GroupDO group, final Collection<GroupDO> nestedGroups)
  // {
  // if (group.isNestedGroupsAllowed() == false && CollectionUtils.isNotEmpty(nestedGroups) == true) {
  // log.warn("Couldn't set nested groups because given group doesn't allow nested groups: " + group);
  // group.setNestedGroupIds(null);
  // return;
  // }
  // group.setNestedGroupIds(groupsProvider.getGroupIds(nestedGroups));
  // }
  //
  // public Collection<GroupDO> getSortedNestedGroups(final GroupDO group)
  // {
  // if (group.isNestedGroupsAllowed() == false && StringUtils.isNotEmpty(group.getNestedGroupIds()) == true) {
  // log.warn("Ignore nested groups because given group doesn't allow nested groups: " + group);
  // group.setNestedGroupIds(null);
  // return null;
  // }
  // return groupsProvider.getSortedGroups(group.getNestedGroupIds());
  // }

  /**
   * Creates for every user an history entry if the user is part of this new group.
   *
   * @param group
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSave(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  public void afterSave(final GroupDO group)
  {
    final Collection<GroupDO> groupList = new ArrayList<GroupDO>();
    groupList.add(group);
    if (group.getAssignedUsers() != null) {
      // Create history entry of PFUserDO for all assigned users:
      for (final PFUserDO user : group.getAssignedUsers()) {
        createHistoryEntry(user, null, groupList);
      }
    }
  }

  /**
   * Creates for every user an history if the user is assigned or unassigned from this updated group.
   *
   * @param group
   * @param dbGroup
   * @see org.projectforge.framework.persistence.api.BaseDao#afterUpdate(GroupDO, GroupDO)
   */
  @Override
  protected void afterUpdate(final GroupDO group, final GroupDO dbGroup)
  {
    if (doHistoryUpdate) {
      final Set<PFUserDO> origAssignedUsers = dbGroup.getAssignedUsers();
      final Set<PFUserDO> assignedUsers = group.getAssignedUsers();
      final Collection<PFUserDO> assignedList = new ArrayList<PFUserDO>(); // List of new assigned users.
      final Collection<PFUserDO> unassignedList = new ArrayList<PFUserDO>(); // List of unassigned users.
      for (final PFUserDO user : group.getAssignedUsers()) {
        if (origAssignedUsers.contains(user) == false) {
          assignedList.add(user);
        }
      }
      for (final PFUserDO user : dbGroup.getAssignedUsers()) {
        if (assignedUsers.contains(user) == false) {
          unassignedList.add(user);
        }
      }
      final Collection<GroupDO> groupList = new ArrayList<GroupDO>();
      groupList.add(group);
      // Create history entry of PFUserDO for all new assigned users:
      for (final PFUserDO user : assignedList) {
        createHistoryEntry(user, null, groupList);
      }
      // Create history entry of PFUserDO for all unassigned users:
      for (final PFUserDO user : unassignedList) {
        createHistoryEntry(user, groupList, null);
      }
    }
  }

  /**
   * Assigns groups to and unassigns groups from given user.
   *
   * @param user
   * @param groupsToAssign   Groups to assign (nullable).
   * @param groupsToUnassign Groups to unassign (nullable).
   * @throws AccessException
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void assignGroups(final PFUserDO user, final Set<GroupDO> groupsToAssign, final Set<GroupDO> groupsToUnassign, final boolean updateUserGroupCache)
  {
    getHibernateTemplate().refresh(user, LockMode.READ);

    final List<GroupDO> assignedGroups = new ArrayList<>();
    if (groupsToAssign != null) {
      for (final GroupDO group : groupsToAssign) {
        final GroupDO dbGroup = getHibernateTemplate().get(clazz, group.getId(), LockMode.PESSIMISTIC_WRITE);
        HistoryBaseDaoAdapter.wrappHistoryUpdate(dbGroup, () -> {
          Set<PFUserDO> assignedUsers = dbGroup.getAssignedUsers();
          if (assignedUsers == null) {
            assignedUsers = new HashSet<>();
            dbGroup.setAssignedUsers(assignedUsers);
          }
          if (assignedUsers.contains(user) == false) {
            log.info("Assigning user '" + user.getUsername() + "' to group '" + dbGroup.getName() + "'.");
            assignedUsers.add(user);
            assignedGroups.add(dbGroup);
            dbGroup.setLastUpdate(); // Needed, otherwise GroupDO is not detected for hibernate history!
          } else {
            log.info("User '" + user.getUsername() + "' already assigned to group '" + dbGroup.getName() + "'.");
          }
          return null;
        });
      }
    }

    final List<GroupDO> unassignedGroups = new ArrayList<>();
    if (groupsToUnassign != null) {
      for (final GroupDO group : groupsToUnassign) {
        final GroupDO dbGroup = getHibernateTemplate().get(clazz, group.getId(), LockMode.PESSIMISTIC_WRITE);
        HistoryBaseDaoAdapter.wrappHistoryUpdate(dbGroup, () -> {
          final Set<PFUserDO> assignedUsers = dbGroup.getAssignedUsers();
          if (assignedUsers != null && assignedUsers.contains(user) == true) {
            log.info("Unassigning user '" + user.getUsername() + "' from group '" + dbGroup.getName() + "'.");
            assignedUsers.remove(user);
            unassignedGroups.add(dbGroup);
            dbGroup.setLastUpdate(); // Needed, otherwise GroupDO is not detected for hibernate history!
          } else {
            log.info("User '" + user.getUsername() + "' is not assigned to group '" + dbGroup.getName() + "' (can't unassign).");
          }
          return null;
        });
      }
    }

    flushSession();
    createHistoryEntry(user, unassignedGroups, assignedGroups);
    if (updateUserGroupCache) {
      getUserGroupCache().setExpired();
    }
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void assignGroups(final PFUserDO user, final Set<GroupDO> groupsToAssign, final Set<GroupDO> groupsToUnassign)
      throws AccessException
  {
    assignGroups(user, groupsToAssign, groupsToUnassign, true);
  }

  private void createHistoryEntry(final PFUserDO user, Collection<GroupDO> unassignedList,
      Collection<GroupDO> assignedList)
  {
    if (unassignedList != null && unassignedList.size() == 0) {
      unassignedList = null;
    }
    if (assignedList != null && assignedList.size() == 0) {
      assignedList = null;
    }
    if (unassignedList == null && assignedList == null) {
      return;
    }
    createHistoryEntry(user, user.getId(), "assignedGroups", GroupDO.class, unassignedList, assignedList);
  }

  /**
   * Internal load of all tasks without checking any access.
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  List<GroupDO> loadAll()
  {
    final List<GroupDO> list = (List<GroupDO>) getHibernateTemplate().find("from GroupDO t join");
    return list;
  }

  /**
   * Prevents changing the group name for ProjectForge groups.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#onChange(org.projectforge.core.ExtendedBaseDO,
   * org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void onChange(final GroupDO obj, final GroupDO dbObj)
  {
    for (final ProjectForgeGroup group : ProjectForgeGroup.values()) {
      if (group.getName().equals(dbObj.getName()) == true) {
        // A group of ProjectForge will be changed.
        if (group.getName().equals(obj) == false) {
          // The group's name must be unmodified!
          log.warn(
              "Preventing the change of ProjectForge's group '" + group.getName() + "' in '" + obj.getName() + "'.");
          obj.setName(group.getName());
        }
        break;
      }
    }
  }

  @Override
  protected void afterSaveOrModify(final GroupDO group)
  {
    getUserGroupCache().setExpired();
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterDelete(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void afterDelete(final GroupDO obj)
  {
    getUserGroupCache().setExpired();
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * return Always true, no generic select access needed for group objects.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess()
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return true;
  }

  /**
   * @return false, if no admin user and the context user is not member of the group. Also deleted groups are only
   * visible for admin users.
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess(org.projectforge.core.BaseDO, boolean)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final GroupDO obj, final boolean throwException)
  {
    Validate.notNull(obj);
    boolean result = accessChecker.isUserMemberOfAdminGroup(user);
    if (result == true) {
      return true;
    }
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.CONTROLLING_GROUP)) {
      return true;
    }
    if (obj.isDeleted() == false) {
      Validate.notNull(user);
      result = getUserGroupCache().isUserMemberOfGroup(user.getId(), obj.getId());
    }
    if (result == true) {
      return true;
    }
    if (throwException == true) {
      throw new AccessException(AccessType.GROUP, OperationType.SELECT);
    }
    return result;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final GroupDO obj, final GroupDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final boolean throwException)
  {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasInsertAccess(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user)
  {
    return accessChecker.isUserMemberOfAdminGroup(user);
  }

  @Override
  public GroupDO newInstance()
  {
    return new GroupDO();
  }

  public GroupDO getByName(final String name)
  {
    if (name == null) {
      return null;
    }
    @SuppressWarnings("unchecked")
    final List<GroupDO> list = (List<GroupDO>) getHibernateTemplate().find("from GroupDO u where u.name = ?", name);
    if (CollectionUtils.isEmpty(list) == true) {
      return null;
    }
    return list.get(0);
  }
}
