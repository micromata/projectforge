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

package org.projectforge.business.fibu;

import java.util.List;

import org.hibernate.criterion.Order;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.stereotype.Repository;

@Repository
public class KundeDao extends BaseDao<KundeDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(KundeDao.class);

  public KundeDao()
  {
    super(KundeDO.class);
    avoidNullIdCheckBeforeSave = true;
  }

  @Override
  public List<KundeDO> getList(final BaseSearchFilter filter)
  {
    final QueryFilter queryFilter = new QueryFilter(filter);
    queryFilter.addOrder(Order.asc("id"));
    return getList(queryFilter);
  }

  /**
   * return Always true, no generic select access needed for address objects.
   * 
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess()
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.CONTROLLING_GROUP,
        ProjectForgeGroup.PROJECT_MANAGER, ProjectForgeGroup.PROJECT_ASSISTANT);
  }

  @Override
  public boolean hasSelectAccess(final PFUserDO user, final KundeDO obj, final boolean throwException)
  {
    if (obj == null) {
      return true;
    }
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.CONTROLLING_GROUP) == true) {
      return true;
    }
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.PROJECT_MANAGER,
        ProjectForgeGroup.PROJECT_ASSISTANT) == true) {
      if (obj.getStatus() != null
          && obj.getStatus().isIn(KundeStatus.ENDED, KundeStatus.NONACTIVE, KundeStatus.NONEXISTENT) == false
          && obj.isDeleted() == false) {
        // Ein Projektleiter sieht keine nicht mehr aktiven oder gelöschten Kunden.
        return true;
      }
    }
    if (throwException == true) {
      accessChecker.checkIsUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP);
      log.error("Should not occur! An exception should be thrown.");
    }
    return false;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final KundeDO obj, final KundeDO oldObj,
      final OperationType operationType,
      final boolean throwException)
  {
    return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.FINANCE_GROUP);
  }

  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final boolean throwException)
  {
    return accessChecker.isUserMemberOfGroup(user, throwException, ProjectForgeGroup.FINANCE_GROUP,
        ProjectForgeGroup.CONTROLLING_GROUP);
  }

  @Override
  public KundeDO newInstance()
  {
    return new KundeDO();
  }
}
