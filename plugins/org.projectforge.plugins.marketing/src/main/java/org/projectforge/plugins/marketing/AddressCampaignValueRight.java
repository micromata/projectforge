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

package org.projectforge.plugins.marketing;

import org.projectforge.business.address.AddressDao;
import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Kai Reinhard (k.reinhard@me.de)
 */
public class AddressCampaignValueRight extends UserRightAccessCheck<AddressCampaignValueDO>
{
  private static final long serialVersionUID = 4021610615575404717L;

  AddressDao addressDao;

  public AddressCampaignValueRight(AccessChecker accessChecker, AddressDao addressDao)
  {
    super(accessChecker, MarketingPluginUserRightId.PLUGIN_MARKETING_ADDRESS_CAMPAIGN_VALUE,
        UserRightCategory.PLUGINS,
        UserRightValue.TRUE);
    this.addressDao = addressDao;
  }

  /**
   * @return true for select access for marketing users and true for admin users, otherwise false.
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final AddressCampaignValueDO obj,
      final AddressCampaignValueDO oldObj,
      final OperationType operationType)
  {
    if (operationType.isIn(OperationType.SELECT, OperationType.INSERT) && obj == null) {
      return addressDao.hasInsertAccess(user);
    }
    return addressDao.hasAccess(user, obj != null ? obj.getAddress() : null,
        oldObj != null ? oldObj.getAddress() : null, operationType,
        false);
  }
}
