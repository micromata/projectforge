/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.marketing;

import org.projectforge.business.address.AddressbookDao;
import org.projectforge.business.address.AddressbookRight;
import org.projectforge.business.user.ProjectForgeGroup;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.WicketSupport;

/**
 * @author Kai Reinhard (k.reinhard@me.de)
 */
public class AddressCampaignRight extends UserRightAccessCheck<AddressCampaignDO> {
  private static final long serialVersionUID = 4021610615575404717L;

  private AddressbookRight addressbookRight = new AddressbookRight();

  public AddressCampaignRight() {
    super(MarketingPluginUserRightId.PLUGIN_MARKETING_ADDRESS_CAMPAIGN,
        UserRightCategory.PLUGINS,
        UserRightValue.TRUE);
  }

  /**
   * @return true for select access for marketing users and true for admin users, otherwise false.
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final AddressCampaignDO obj, final AddressCampaignDO oldObj,
                           final OperationType operationType) {
    var accessChecker = WicketSupport.getAccessChecker();
    if (accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.MARKETING_GROUP)) {
      return true;
    }
    if (operationType != OperationType.SELECT) {
      return false;
    }
    return WicketSupport.get(AddressbookDao.class).hasAccessToGlobalAddressBook(user);
  }
}
