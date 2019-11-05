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

package org.projectforge.plugins.licensemanagement;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Every user has access to own to-do's or to-do's he's assigned to. All other users have access if the to-do is
 * assigned to a task and the user has the task access.
 * 
 * @author Kai Reinhard (k.reinhard@me.de)
 * 
 */
public class LicenseManagementRight extends UserRightAccessCheck<LicenseDO>
{
  private static final long serialVersionUID = -2928342166476350773L;

  public LicenseManagementRight(AccessChecker accessChecker)
  {
    super(accessChecker, LicensemanagementPluginUserRightsId.PLUGIN_LICENSE_MANAGEMENT,
        UserRightCategory.PLUGINS,
        UserRightValue.TRUE);
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final LicenseDO obj, final LicenseDO oldObj,
      final OperationType operationType)
  {
    return true;
  }

  public boolean isLicenseKeyVisible(final PFUserDO user, final LicenseDO license)
  {
    if (license == null || license.getId() == null) {
      // Visible for new objects (created by current user):
      return true;
    }
    if (accessChecker.isLoggedInUserMemberOfAdminGroup()) {
      // Administrators have always access:
      return true;
    }
    if (StringUtils.isBlank(license.getOwnerIds())) {
      // No owners defined.
      return false;
    }
    final int[] ids = StringHelper.splitToInts(license.getOwnerIds(), ",", true);
    final int userId = user.getId();
    for (final int id : ids) {
      if (id == userId) {
        // User is member of owners:
        return true;
      }
    }
    return false;

  }

  /**
   * @see org.projectforge.business.user.UserRightAccessCheck#hasHistoryAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object)
   */
  @Override
  public boolean hasHistoryAccess(final PFUserDO user, final LicenseDO obj)
  {
    return isLicenseKeyVisible(user, obj);
  }
}
