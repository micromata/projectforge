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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class UserRightDao extends BaseDao<UserRightDO>
{
  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "user.username", "user.firstname",
      "user.lastname" };

  @Autowired
  private UserRightService userRightService;

  protected UserRightDao()
  {
    super(UserRightDO.class);
  }

  public List<UserRightDO> getList(final PFUserDO user)
  {
    final UserRightFilter filter = new UserRightFilter();
    filter.setUser(user);
    return getList(filter);
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
  public void updateUserRights(final PFUserDO user, final List<UserRightVO> list, final boolean updateUserGroupCache)
  {
    final List<UserRightDO> dbList = getList(user);
    // evict all entities from the session cache to avoid that the update is already done in the copy method
    dbList.forEach(getHibernateTemplate()::evict);
    final UserGroupCache userGroupCache = getUserGroupCache();
    for (final UserRightVO rightVO : list) {
      UserRightDO rightDO = null;
      for (final UserRightDO dbItem : dbList) {
        IUserRightId rightid = userRightService.getRightId(dbItem.getRightIdString());
        if (rightid == rightVO.getRight().getId()) {
          rightDO = dbItem;
        }
      }
      if (rightDO == null) {
        if ((rightVO.isBooleanValue() == true && rightVO.getValue() == UserRightValue.FALSE)
            || rightVO.getValue() == null) {
          continue;
          // Right has no value and is not yet in data base.
          // Do nothing.
        }
        // Create new right instead of updating an existing one.
        rightDO = new UserRightDO(user, rightVO.getRight().getId()).setUser(user);
        copy(rightDO, rightVO);
        save(rightDO);
      } else {
        copy(rightDO, rightVO);
        IUserRightId rightId = userRightService.getRightId(rightDO.getRightIdString());
        final UserRight right = userRightService.getRight(rightId);
        if (right.isAvailable(userGroupCache, user) == false
            || right.isAvailable(userGroupCache, user, rightDO.getValue()) == false) {
          rightDO.setValue(null);
        }
        update(rightDO);
      }
    }
    // Set unavailable rights to null (if exists):
    for (final UserRightDO rightDO : dbList) {
      String rightId = rightDO.getRightIdString();
      UserRight right = userRightService.getRight(rightId);
      if (right.isAvailable(userGroupCache, user) == false
          || right.isAvailable(userGroupCache, user, rightDO.getValue()) == false) {
        rightDO.setValue(null);
        update(rightDO);
      }
    }
    if (updateUserGroupCache) {
      userGroupCache.setExpired();
    }
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
  public void updateUserRights(final PFUserDO user, final List<UserRightVO> list)
  {
    updateUserRights(user, list, true);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void afterSaveOrModify(final UserRightDO obj)
  {
    super.afterSaveOrModify(obj);
    TenantRegistryMap.getInstance().getTenantRegistry(obj).getUserGroupCache().setExpired();
  }

  private void copy(final UserRightDO dest, final UserRightVO src)
  {
    if (src.getRight().isBooleanType() == true) {
      if (src.isBooleanValue() == true) {
        dest.setValue(UserRightValue.TRUE);
      } else {
        dest.setValue(UserRightValue.FALSE);
      }
    } else {
      dest.setValue(src.getValue());
    }
  }

  public List<UserRightVO> getUserRights(final PFUserDO user)
  {
    final List<UserRightVO> list = new ArrayList<UserRightVO>();
    if (user == null || user.getId() == null) {
      return list;
    }
    final List<UserRightDO> dbList = getList(user);
    final UserGroupCache userGroupCache = getUserGroupCache();
    for (final UserRight right : userRightService.getOrderedRights()) {
      if (right.isAvailable(userGroupCache, user) == false) {
        continue;
      }
      final UserRightVO rightVO = new UserRightVO(right);
      for (final UserRightDO rightDO : dbList) {
        if (StringUtils.equals(rightDO.getRightIdString(), right.getId().getId()) == true) {
          rightVO.setValue(rightDO.getValue());
        }
      }
      list.add(rightVO);
    }
    return list;
  }

  @Override
  public List<UserRightDO> getList(final BaseSearchFilter filter)
  {
    final QueryFilter queryFilter = new QueryFilter(filter);
    final UserRightFilter myFilter = (UserRightFilter) filter;
    if (myFilter.getUser() != null) {
      queryFilter.add(Restrictions.eq("user", myFilter.getUser()));
    }
    queryFilter.createAlias("user", "u");
    queryFilter.addOrder(Order.asc("u.username")).addOrder(Order.asc("rightIdString"));
    final List<UserRightDO> list = getList(queryFilter);
    return list;
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  /**
   * User must member of group finance or controlling.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess()
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.ADMIN_GROUP);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess(PFUserDO,
   * org.projectforge.core.ExtendedBaseDO, boolean)
   * @see #hasSelectAccess(PFUserDO, boolean)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final UserRightDO obj, final boolean throwException)
  {
    return hasSelectAccess(user, throwException);
  }

  /**
   * User must member of group admin.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final UserRightDO obj, final UserRightDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.ADMIN_GROUP);
  }

  @Override
  public UserRightDO newInstance()
  {
    return new UserRightDO();
  }
}
