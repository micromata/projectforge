/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.user;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public class UsersProvider extends ChoiceProvider<PFUserDO>
{
  private static final long serialVersionUID = 6228672635966093252L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UsersProvider.class);

  private transient UserGroupCache userGroupCache;

  private transient UserDao userDao;

  private int pageSize = 20;

  private final UsersComparator usersComparator = new UsersComparator();

  private Collection<PFUserDO> sortedUsers;

  public UsersProvider(UserDao userDao)
  {
    this.userDao = userDao;
  }

  public Collection<PFUserDO> getSortedUsers()
  {
    if (sortedUsers == null) {
      sortedUsers = new TreeSet<PFUserDO>(usersComparator);
      final Collection<PFUserDO> allusers = UserGroupCache.getInstance().getAllUsers();
      final PFUserDO loggedInUser = ThreadLocalUserContext.getLoggedInUser();
      for (final PFUserDO user : allusers) {
        if (user.getDeleted() == false && user.getDeactivated() == false
            && userDao.hasUserSelectAccess(loggedInUser, user, false) == true) {
          sortedUsers.add(user);
        }
      }
    }
    return sortedUsers;
  }

  /**
   * @param userIds
   * @return
   */
  public Collection<PFUserDO> getSortedUsers(final String userIds)
  {
    if (StringUtils.isEmpty(userIds) == true) {
      return null;
    }
    sortedUsers = new TreeSet<PFUserDO>(usersComparator);
    final long[] ids = StringHelper.splitToLongs(userIds, ",");
    for (final long id : ids) {
      final PFUserDO user = UserGroupCache.getInstance().getUser(id);
      if (user != null) {
        sortedUsers.add(user);
      } else {
        log.warn("Group with id '" + id + "' not found in UserGroupCache. groupIds string was: " + userIds);
      }
    }
    return sortedUsers;
  }

  public String getUserIds(final Collection<PFUserDO> users)
  {
    final StringBuilder buf = new StringBuilder();
    boolean first = true;
    for (final PFUserDO user : users) {
      if (user.getId() != null) {
        first = StringHelper.append(buf, first, String.valueOf(user.getId()), ",");
      }
    }
    return buf.toString();
  }

  /**
   * @param pageSize the pageSize to set
   * @return this for chaining.
   */
  public UsersProvider setPageSize(final int pageSize)
  {
    this.pageSize = pageSize;
    return this;
  }

  @Override
  public String getDisplayValue(final PFUserDO choice)
  {
    return choice.getFullname();
  }

  @Override
  public String getIdValue(final PFUserDO choice)
  {
    return String.valueOf(choice.getId());
  }

  @Override
  public void query(String term, final int page, final Response<PFUserDO> response)
  {
    final Collection<PFUserDO> sortedUsers = getSortedUsers();
    final List<PFUserDO> result = new ArrayList<>();
    term = term != null ? term.toLowerCase() : "";

    final int offset = page * pageSize;

    int matched = 0;
    boolean hasMore = false;
    for (final PFUserDO user : sortedUsers) {
      if (result.size() == pageSize) {
        hasMore = true;
        break;
      }
      if (user.getFullname().toLowerCase().contains(term) == true
          || user.getUsername().toLowerCase().contains(term) == true) {
        matched++;
        if (matched > offset) {
          result.add(user);
        }
      }
    }
    response.addAll(result);
    response.setHasMore(hasMore);
  }

  @Override
  public Collection<PFUserDO> toChoices(final Collection<String> ids)
  {
    final List<PFUserDO> list = new ArrayList<PFUserDO>();
    if (ids == null) {
      return list;
    }
    for (final String str : ids) {
      final Long userId = NumberHelper.parseLong(str);
      if (userId == null) {
        continue;
      }
      final PFUserDO user = UserGroupCache.getInstance().getUser(userId);
      if (user != null) {
        list.add(user);
      }
    }
    return list;
  }
}
