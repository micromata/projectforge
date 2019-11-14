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

package org.projectforge.business.user;

import org.apache.commons.lang3.Validate;
import org.projectforge.business.login.Login;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.SortProperty;
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class GroupDao extends BaseDao<GroupDO> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GroupDao.class);

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"assignedUsers.username",
          "assignedUsers.firstname",
          "assignedUsers.lastname"};

  @Autowired
  private UserDao userDao;

  private boolean doHistoryUpdate = true;

  // private final GroupsProvider groupsProvider = new GroupsProvider();

  public GroupDao() {
    super(GroupDO.class);
    this.supportAfterUpdate = true;
  }

  @Override
  public List<GroupDO> getList(final BaseSearchFilter filter) {
    final GroupFilter myFilter;
    if (filter instanceof GroupFilter) {
      myFilter = (GroupFilter) filter;
    } else {
      myFilter = new GroupFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (Login.getInstance().hasExternalUsermanagementSystem()) {
      // Check hasExternalUsermngmntSystem because otherwise the filter is may-be preset for an user and the user can't change the filter
      // (because the fields aren't visible).
      if (myFilter.getLocalGroup() != null) {
        queryFilter.add(QueryFilter.eq("localGroup", myFilter.getLocalGroup()));
      }
    }
    queryFilter.addOrder(SortProperty.asc("name"));
    return getList(queryFilter);
  }

  /**
   * Does a group with the given name already exists? Works also for existing users (if group name was modified).
   */
  public boolean doesGroupnameAlreadyExist(final GroupDO group) {
    Validate.notNull(group);
    GroupDO dbGroup;
    if (group.getId() == null) {
      // New group
      dbGroup = getByName(group.getName());
    } else {
      // group already exists. Check maybe changed name:
      dbGroup = SQLHelper.ensureUniqueResult(em.createNamedQuery(GroupDO.FIND_OTHER_GROUP_BY_NAME, GroupDO.class)
              .setParameter("name", group.getName())
              .setParameter("id", group.getId()));
    }
    return dbGroup != null;
  }

  /**
   * Please note: Any existing assigned user in group object is ignored!
   *
   * @param assignedUsers Full list of all users which have to assigned to this group.
   */
  public void setAssignedUsers(final GroupDO group, final Collection<PFUserDO> assignedUsers) throws AccessException {
    final Set<PFUserDO> origAssignedUsers = group.getAssignedUsers();
    if (origAssignedUsers != null) {
      final Iterator<PFUserDO> it = origAssignedUsers.iterator();
      while (it.hasNext()) {
        final PFUserDO user = it.next();
        if (!assignedUsers.contains(user)) {
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
      if (origAssignedUsers == null || !origAssignedUsers.contains(dbUser)) {
        group.addUser(dbUser);
      }
    }
  }

  /**
   * Creates for every user an history entry if the user is part of this new group.
   */
  @Override
  public void afterSave(final GroupDO group) {
    final Collection<GroupDO> groupList = new ArrayList<>();
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
   */
  @Override
  protected void afterUpdate(final GroupDO group, final GroupDO dbGroup) {
    if (doHistoryUpdate) {
      final Set<PFUserDO> origAssignedUsers = dbGroup.getAssignedUsers();
      final Set<PFUserDO> assignedUsers = group.getAssignedUsers();
      final Collection<PFUserDO> assignedList = new ArrayList<>(); // List of new assigned users.
      final Collection<PFUserDO> unassignedList = new ArrayList<>(); // List of unassigned users.
      for (final PFUserDO user : group.getAssignedUsers()) {
        if (!origAssignedUsers.contains(user)) {
          assignedList.add(user);
        }
      }
      for (final PFUserDO user : dbGroup.getAssignedUsers()) {
        if (!assignedUsers.contains(user)) {
          unassignedList.add(user);
        }
      }
      final Collection<GroupDO> groupList = new ArrayList<>();
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
   * @param groupsToAssign   Groups to assign (nullable).
   * @param groupsToUnassign Groups to unassign (nullable).
   * @throws AccessException
   */
  public void assignGroups(final PFUserDO user, final Set<GroupDO> groupsToAssign, final Set<GroupDO> groupsToUnassign, final boolean updateUserGroupCache) {
    final List<GroupDO> assignedGroups = new ArrayList<>();
    final List<GroupDO> unassignedGroups = new ArrayList<>();
    emgrFactory.runInTrans(emgr -> {
      PFUserDO dbUser = emgr.selectByPkAttached(PFUserDO.class, user.getPk());
      if (groupsToAssign != null) {
        for (final GroupDO group : groupsToAssign) {
          final GroupDO dbGroup = emgr.selectByPkAttached(GroupDO.class, group.getId());
          HistoryBaseDaoAdapter.wrappHistoryUpdate(dbGroup, () -> {
            Set<PFUserDO> assignedUsers = dbGroup.getAssignedUsers();
            if (assignedUsers == null) {
              assignedUsers = new HashSet<>();
              dbGroup.setAssignedUsers(assignedUsers);
            }
            if (!assignedUsers.contains(dbUser)) {
              log.info("Assigning user '" + dbUser.getUsername() + "' to group '" + dbGroup.getName() + "'.");
              assignedUsers.add(dbUser);
              assignedGroups.add(dbGroup);
              dbGroup.setLastUpdate(); // Needed, otherwise GroupDO is not detected for hibernate history!
            } else {
              log.info("User '" + dbUser.getUsername() + "' already assigned to group '" + dbGroup.getName() + "'.");
            }
            emgr.update(dbGroup);
            return null;
          });
        }
      }
      if (groupsToUnassign != null) {
        for (final GroupDO group : groupsToUnassign) {
          final GroupDO dbGroup = emgr.selectByPkAttached(GroupDO.class, group.getId());
          HistoryBaseDaoAdapter.wrappHistoryUpdate(dbGroup, () -> {
            final Set<PFUserDO> assignedUsers = dbGroup.getAssignedUsers();
            if (assignedUsers != null && assignedUsers.contains(dbUser)) {
              log.info("Unassigning user '" + dbUser.getUsername() + "' from group '" + dbGroup.getName() + "'.");
              assignedUsers.remove(dbUser);
              unassignedGroups.add(dbGroup);
              dbGroup.setLastUpdate(); // Needed, otherwise GroupDO is not detected for hibernate history!
            } else {
              log.info("User '" + dbUser.getUsername() + "' is not assigned to group '" + dbGroup.getName() + "' (can't unassign).");
            }
            emgr.update(dbGroup);
            return null;
          });
        }
      }
      return null;
    });

    createHistoryEntry(user, unassignedGroups, assignedGroups);
    if (updateUserGroupCache) {
      getUserGroupCache().setExpired();
    }
  }

  public void assignGroups(final PFUserDO user, final Set<GroupDO> groupsToAssign, final Set<GroupDO> groupsToUnassign)
          throws AccessException {
    assignGroups(user, groupsToAssign, groupsToUnassign, true);
  }

  private void createHistoryEntry(final PFUserDO user, Collection<GroupDO> unassignedList,
                                  Collection<GroupDO> assignedList) {
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
   * Prevents changing the group name for ProjectForge groups.
   */
  @Override
  protected void onChange(final GroupDO obj, final GroupDO dbObj) {
    for (final ProjectForgeGroup group : ProjectForgeGroup.values()) {
      if (group.getName().equals(dbObj.getName())) {
        // A group of ProjectForge will be changed.
        if (!group.getName().equals(obj)) {
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
  protected void afterSaveOrModify(final GroupDO group) {
    getUserGroupCache().setExpired();
  }

  @Override
  protected void afterDelete(final GroupDO obj) {
    getUserGroupCache().setExpired();
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * return Always true, no generic select access needed for group objects.
   */
  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final boolean throwException) {
    return true;
  }

  /**
   * @return false, if no admin user and the context user is not member of the group. Also deleted groups are only
   * visible for admin users.
   */
  @Override
  public boolean hasUserSelectAccess(final PFUserDO user, final GroupDO obj, final boolean throwException) {
    Validate.notNull(obj);
    boolean result = accessChecker.isUserMemberOfAdminGroup(user);
    if (result) {
      return true;
    }
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP,
            ProjectForgeGroup.CONTROLLING_GROUP)) {
      return true;
    }
    if (!obj.isDeleted()) {
      Validate.notNull(user);
      result = getUserGroupCache().isUserMemberOfGroup(user.getId(), obj.getId());
    }
    if (result) {
      return true;
    }
    if (throwException) {
      throw new AccessException(AccessType.GROUP, OperationType.SELECT);
    }
    return result;
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final GroupDO obj, final GroupDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final boolean throwException) {
    return accessChecker.isUserMemberOfAdminGroup(user, throwException);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasInsertAccess(org.projectforge.framework.persistence.user.entities.PFUserDO)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user) {
    return accessChecker.isUserMemberOfAdminGroup(user);
  }

  @Override
  public GroupDO newInstance() {
    return new GroupDO();
  }

  public GroupDO getByName(final String name) {
    if (name == null) {
      return null;
    }
    return SQLHelper.ensureUniqueResult(
            em
                    .createNamedQuery(GroupDO.FIND_BY_NAME, GroupDO.class)
                    .setParameter("name", name));
  }
}
