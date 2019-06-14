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

package org.projectforge.business.user;

import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Dummy right which matches always to true.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class UserRightAlwaysTrue extends UserRight
{
  private static final long serialVersionUID = -1696193387393250294L;

  public UserRightAlwaysTrue(final UserRightId id, final UserRightCategory category)
  {
    super(id, category, UserRightValue.TRUE);
  }

  /**
   * @param userGroupCache
   * @param user
   * @param value
   * @return Always true.
   * @see UserGroupsRight#matches(UserGroupCache, PFUserDO, UserRightValue)
   */
  public boolean matches(final UserGroupCache userGroupCache, final PFUserDO user, final UserRightValue value)
  {
    return true;
  }
}
