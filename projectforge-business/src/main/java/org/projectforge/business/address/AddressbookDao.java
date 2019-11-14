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

package org.projectforge.business.address;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.group.service.GroupService;
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
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Florian blumenstein
 */
@Repository
public class AddressbookDao extends BaseDao<AddressbookDO> {
  public static final int GLOBAL_ADDRESSBOOK_ID = 1;
  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"usersgroups", "owner.username",
          "owner.firstname",
          "owner.lastname"};
  @Autowired
  private UserDao userDao;

  @Autowired
  private GroupService groupService;

  @Autowired
  private UserService userService;

  public AddressbookDao() {
    super(AddressbookDO.class);
    userRightId = UserRightId.MISC_ADDRESSBOOK;
  }

  @Override
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  public void setOwner(final AddressbookDO ab, final Integer userId) {
    final PFUserDO user = userDao.getOrLoad(userId);
    ab.setOwner(user);
  }

  @Override
  public AddressbookDO newInstance() {
    return new AddressbookDO();
  }

  @Override
  public List<AddressbookDO> getList(final BaseSearchFilter filter) {
    AddressbookFilter myFilter;
    if (filter instanceof AddressbookFilter)
      myFilter = (AddressbookFilter) filter;
    else {
      myFilter = new AddressbookFilter(filter);
    }
    final PFUserDO user = ThreadLocalUserContext.getUser();
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    queryFilter.addOrder(SortProperty.asc("title"));
    final List<AddressbookDO> list = getList(queryFilter);
    if (myFilter.isDeleted()) {
      // No further filtering, show all deleted calendars.
      return list;
    }
    final List<AddressbookDO> result = new ArrayList<>();
    final AddressbookRight right = (AddressbookRight) getUserRight();
    final Integer userId = user.getId();
    final boolean adminAccessOnly = (myFilter.isAdmin()
            && accessChecker.isUserMemberOfAdminGroup(user));
    for (final AddressbookDO ab : list) {
      final boolean isOwn = right.isOwner(user, ab);
      if (isOwn) {
        // User is owner.
        if (adminAccessOnly) {
          continue;
        }
        if (myFilter.isAll() || myFilter.isOwn()) {
          // Calendar matches the filter:
          result.add(ab);
        }
      } else {
        // User is not owner.
        if (myFilter.isAll() || myFilter.isOthers() || adminAccessOnly) {
          if ((myFilter.isFullAccess() && right.hasFullAccess(ab, userId))
                  || (myFilter.isReadonlyAccess() && right.hasReadonlyAccess(ab, userId))) {
            // Calendar matches the filter:
            if (!adminAccessOnly) {
              result.add(ab);
            }
          } else if (adminAccessOnly) {
            result.add(ab);
          }
        }
      }
    }
    return result;
  }

  /**
   * Gets a list of all addressbooks with full access of the current logged-in user as well as the addressbooks owned by the
   * current logged-in user.
   *
   * @return
   */
  public List<AddressbookDO> getAllAddressbooksWithFullAccess() {
    final AddressbookFilter filter = new AddressbookFilter();
    filter.setOwnerType(AddressbookFilter.OwnerType.ALL);
    filter.setFullAccess(true).setReadonlyAccess(false);
    List<AddressbookDO> resultList = getList(filter);
    if (resultList.stream().filter(ab -> ab.getId().equals(GLOBAL_ADDRESSBOOK_ID)).count() < 1) {
      resultList.add(getGlobalAddressbook());
    }
    return resultList;
  }

  public AddressbookDO getGlobalAddressbook() {
    return internalGetById(GLOBAL_ADDRESSBOOK_ID);
  }

  /**
   * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
   *
   * @param addressbook
   * @param fullAccessGroups
   */
  public void setFullAccessGroups(final AddressbookDO ab, final Collection<GroupDO> fullAccessGroups) {
    ab.setFullAccessGroupIds(groupService.getGroupIds(fullAccessGroups));
  }

  public Collection<GroupDO> getSortedFullAccessGroups(final AddressbookDO ab) {
    return groupService.getSortedGroups(ab.getFullAccessGroupIds());
  }

  /**
   * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
   *
   * @param addressbook
   * @param fullAccessUsers
   */
  public void setFullAccessUsers(final AddressbookDO ab, final Collection<PFUserDO> fullAccessUsers) {
    ab.setFullAccessUserIds(userService.getUserIds(fullAccessUsers));
  }

  public Collection<PFUserDO> getSortedFullAccessUsers(final AddressbookDO ab) {
    return userService.getSortedUsers(ab.getFullAccessUserIds());
  }

  /**
   * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
   *
   * @param addressbook
   * @param readonlyAccessGroups
   */
  public void setReadonlyAccessGroups(final AddressbookDO ab, final Collection<GroupDO> readonlyAccessGroups) {
    ab.setReadonlyAccessGroupIds(groupService.getGroupIds(readonlyAccessGroups));
  }

  public Collection<GroupDO> getSortedReadonlyAccessGroups(final AddressbookDO ab) {
    return groupService.getSortedGroups(ab.getReadonlyAccessGroupIds());
  }

  /**
   * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
   *
   * @param addressbook
   * @param readonlyAccessUsers
   */
  public void setReadonlyAccessUsers(final AddressbookDO ab, final Collection<PFUserDO> readonlyAccessUsers) {
    ab.setReadonlyAccessUserIds(userService.getUserIds(readonlyAccessUsers));
  }

  public Collection<PFUserDO> getSortedReadonlyAccessUsers(final AddressbookDO ab) {
    return userService.getSortedUsers(ab.getReadonlyAccessUserIds());
  }

  /**
   * @see BaseDao#getDisplayHistoryEntries(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final AddressbookDO obj) {
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

  @Override
  protected void onDelete(final AddressbookDO obj) {
    super.onDelete(obj);
    emgrFactory.runInTrans(emgr -> {
      List<AddressDO> addressList = emgr
              .selectAttached(AddressDO.class, "SELECT a FROM AddressDO a WHERE :addressbook MEMBER OF a.addressbookList", "addressbook", obj);
      for (AddressDO address : addressList) {
        if (address.getAddressbookList().size() == 1 && address.getAddressbookList().contains(obj)) {
          address.getAddressbookList().add(getGlobalAddressbook());
        }
        address.getAddressbookList().remove(obj);
        emgr.update(address);
      }
      return addressList;
    });
  }

}
