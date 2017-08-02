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

package org.projectforge.business.address;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Order;
import org.projectforge.business.group.service.GroupService;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.service.UserService;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * @author Florian blumenstein
 */
@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class AddressbookDao extends BaseDao<AddressbookDO>
{
  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "usersgroups", "owner.username",
      "owner.firstname",
      "owner.lastname" };

  public static final int GLOBAL_ADDRESSBOOK_ID = 1;

  @Autowired
  private UserDao userDao;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private GroupService groupService;

  @Autowired
  private UserService userService;

  public AddressbookDao()
  {
    super(AddressbookDO.class);
    userRightId = UserRightId.MISC_ADDRESSBOOK;
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  public void setOwner(final AddressbookDO ab, final Integer userId)
  {
    final PFUserDO user = userDao.getOrLoad(userId);
    ab.setOwner(user);
  }

  @Override
  public AddressbookDO newInstance()
  {
    return new AddressbookDO();
  }

  @Override
  public List<AddressbookDO> getList(final BaseSearchFilter filter)
  {
    AddressbookFilter myFilter;
    if (filter instanceof AddressbookFilter)
      myFilter = (AddressbookFilter) filter;
    else {
      myFilter = new AddressbookFilter(filter);
    }
    final PFUserDO user = ThreadLocalUserContext.getUser();
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    queryFilter.addOrder(Order.asc("title"));
    final List<AddressbookDO> list = getList(queryFilter);
    if (myFilter.isDeleted() == true) {
      // No further filtering, show all deleted calendars.
      return list;
    }
    final List<AddressbookDO> result = new ArrayList<>();
    final AddressbookRight right = (AddressbookRight) getUserRight();
    final Integer userId = user.getId();
    final boolean adminAccessOnly = (myFilter.isAdmin() == true
        && accessChecker.isUserMemberOfAdminGroup(user) == true);
    for (final AddressbookDO ab : list) {
      final boolean isOwn = right.isOwner(user, ab);
      if (isOwn == true) {
        // User is owner.
        if (adminAccessOnly == true) {
          continue;
        }
        if (myFilter.isAll() == true || myFilter.isOwn() == true) {
          // Calendar matches the filter:
          result.add(ab);
        }
      } else {
        // User is not owner.
        if (myFilter.isAll() == true || myFilter.isOthers() == true || adminAccessOnly == true) {
          if ((myFilter.isFullAccess() == true && right.hasFullAccess(ab, userId) == true)
              || (myFilter.isReadonlyAccess() == true && right.hasReadonlyAccess(ab, userId) == true)) {
            // Calendar matches the filter:
            if (adminAccessOnly == false) {
              result.add(ab);
            }
          } else if (adminAccessOnly == true) {
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
  public List<AddressbookDO> getAllAddressbooksWithFullAccess()
  {
    final AddressbookFilter filter = new AddressbookFilter();
    filter.setOwnerType(AddressbookFilter.OwnerType.ALL);
    filter.setFullAccess(true).setReadonlyAccess(false);
    List<AddressbookDO> resultList = getList(filter);
    if (resultList.stream().filter(ab -> ab.getId().equals(GLOBAL_ADDRESSBOOK_ID)).count() < 1) {
      resultList.add(getGlobalAddressbook());
    }
    return resultList;
  }

  public AddressbookDO getGlobalAddressbook()
  {
    return internalGetById(GLOBAL_ADDRESSBOOK_ID);
  }

  /**
   * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
   *
   * @param addressbook
   * @param fullAccessGroups
   */
  public void setFullAccessGroups(final AddressbookDO ab, final Collection<GroupDO> fullAccessGroups)
  {
    ab.setFullAccessGroupIds(groupService.getGroupIds(fullAccessGroups));
  }

  public Collection<GroupDO> getSortedFullAccessGroups(final AddressbookDO ab)
  {
    return groupService.getSortedGroups(ab.getFullAccessGroupIds());
  }

  /**
   * Please note: Only the string group.fullAccessGroupIds will be modified (but not be saved)!
   *
   * @param addressbook
   * @param fullAccessUsers
   */
  public void setFullAccessUsers(final AddressbookDO ab, final Collection<PFUserDO> fullAccessUsers)
  {
    ab.setFullAccessUserIds(userService.getUserIds(fullAccessUsers));
  }

  public Collection<PFUserDO> getSortedFullAccessUsers(final AddressbookDO ab)
  {
    return userService.getSortedUsers(ab.getFullAccessUserIds());
  }

  /**
   * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
   *
   * @param addressbook
   * @param readonlyAccessGroups
   */
  public void setReadonlyAccessGroups(final AddressbookDO ab, final Collection<GroupDO> readonlyAccessGroups)
  {
    ab.setReadonlyAccessGroupIds(groupService.getGroupIds(readonlyAccessGroups));
  }

  public Collection<GroupDO> getSortedReadonlyAccessGroups(final AddressbookDO ab)
  {
    return groupService.getSortedGroups(ab.getReadonlyAccessGroupIds());
  }

  /**
   * Please note: Only the string group.readonlyAccessGroupIds will be modified (but not be saved)!
   *
   * @param addressbook
   * @param readonlyAccessUsers
   */
  public void setReadonlyAccessUsers(final AddressbookDO ab, final Collection<PFUserDO> readonlyAccessUsers)
  {
    ab.setReadonlyAccessUserIds(userService.getUserIds(readonlyAccessUsers));
  }

  public Collection<PFUserDO> getSortedReadonlyAccessUsers(final AddressbookDO ab)
  {
    return userService.getSortedUsers(ab.getReadonlyAccessUserIds());
  }

  /**
   * @see BaseDao#getDisplayHistoryEntries(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  public List<DisplayHistoryEntry> getDisplayHistoryEntries(final AddressbookDO obj)
  {
    final List<DisplayHistoryEntry> list = super.getDisplayHistoryEntries(obj);
    if (CollectionUtils.isEmpty(list) == true) {
      return list;
    }
    for (final DisplayHistoryEntry entry : list) {
      if (entry.getPropertyName() == null) {
        continue;
      } else if (entry.getPropertyName().endsWith("GroupIds") == true) {
        final String oldValue = entry.getOldValue();
        if (StringUtils.isNotBlank(oldValue) == true && "null".equals(oldValue) == false) {
          final List<String> oldGroupNames = groupService.getGroupNames(oldValue);
          entry.setOldValue(StringHelper.listToString(oldGroupNames, ", ", true));
        }
        final String newValue = entry.getNewValue();
        if (StringUtils.isNotBlank(newValue) == true && "null".equals(newValue) == false) {
          final List<String> newGroupNames = groupService.getGroupNames(newValue);
          entry.setNewValue(StringHelper.listToString(newGroupNames, ", ", true));
        }
      } else if (entry.getPropertyName().endsWith("UserIds") == true) {
        final String oldValue = entry.getOldValue();
        if (StringUtils.isNotBlank(oldValue) == true && "null".equals(oldValue) == false) {
          final List<String> oldGroupNames = userService.getUserNames(oldValue);
          entry.setOldValue(StringHelper.listToString(oldGroupNames, ", ", true));
        }
        final String newValue = entry.getNewValue();
        if (StringUtils.isNotBlank(newValue) == true && "null".equals(newValue) == false) {
          final List<String> newGroupNames = userService.getUserNames(newValue);
          entry.setNewValue(StringHelper.listToString(newGroupNames, ", ", true));
        }
      }
    }
    return list;
  }

  /**
   * @see BaseDao#useOwnCriteriaCacheRegion()
   */
  @Override
  protected boolean useOwnCriteriaCacheRegion()
  {
    return true;
  }
}
