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

package org.projectforge.business.multitenancy;

import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.configuration.GlobalConfiguration;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;

/**
 * 
 * @author Kai Reinhard (k.reinhard@me.de)
 * 
 */
public class TenantRight extends UserRightAccessCheck<TenantDO>
{
  private static final long serialVersionUID = -558887908748357573L;

  TenantChecker tenantChecker;

  public TenantRight(AccessChecker accessChecker, TenantChecker tenantChecker)
  {
    super(accessChecker, TenantDao.USER_RIGHT_ID, UserRightCategory.ADMIN, UserRightValue.TRUE);
    this.tenantChecker = tenantChecker;
  }

  /**
   * @return true if user is member of group FINANCE.
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(java.lang.Object)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final TenantDO obj, final TenantDO oldObj,
      final OperationType operationType)
  {
    if (GlobalConfiguration.getInstance().isMultiTenancyConfigured() == false) {
      return false;
    }
    if (user.getSuperAdmin() == true) {
      return true;
    }
    if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.ADMIN_GROUP) == false) {
      return false;
    }
    if (operationType == OperationType.SELECT) {
      // Administrators (not super users) has the select access for tenants they're assigned to.
      return tenantChecker.isPartOfTenant(obj, user);
    }
    return false;
  }
}
