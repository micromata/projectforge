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

package org.projectforge.business.teamcal.admin.right;

import org.projectforge.business.common.BaseUserGroupRight;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightId;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;

/**
 * @author Kai Reinhard (k.reinhard@me.de)
 */
public class TeamCalRight extends BaseUserGroupRight<TeamCalDO> {
  private static final long serialVersionUID = -2928342166476350773L;

  public TeamCalRight(AccessChecker accessChecker) {
    super(accessChecker, UserRightId.PLUGIN_CALENDAR, UserRightCategory.PLUGINS,
            UserRightValue.TRUE);
  }
}
