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

package org.projectforge.plugins.liquidityplanning;

import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightServiceImpl;
import org.projectforge.framework.access.AccessChecker;

/**
 * @author Kai Reinhard (k.reinhard@me.de)
 * 
 */
public class LiquidityPlanningRight extends UserRightAccessCheck<LiquidityEntryDO>
{
  private static final long serialVersionUID = -7708532860353029199L;

  public LiquidityPlanningRight(AccessChecker accessChecker)
  {
    super(accessChecker, LiquidityplanningPluginUserRightId.PLUGIN_LIQUIDITY_PLANNING, UserRightCategory.FIBU,
        UserRightServiceImpl.FALSE_READONLY_READWRITE);
    initializeUserGroupsRight(UserRightServiceImpl.FALSE_READONLY_READWRITE, UserRightServiceImpl.FIBU_GROUPS)
        .setReadOnlyForControlling();
  }
}
