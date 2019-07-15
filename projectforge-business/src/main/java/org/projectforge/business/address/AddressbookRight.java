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

package org.projectforge.business.address;

import org.projectforge.business.common.BaseUserGroupRight;
import org.projectforge.business.user.*;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Florian Blumenstein
 */
public class AddressbookRight extends BaseUserGroupRight<AddressbookDO> {
  private static final long serialVersionUID = -2928342166476350773L;

  public AddressbookRight(AccessChecker accessChecker) {
    super(accessChecker, UserRightId.MISC_ADDRESSBOOK, UserRightCategory.MISC,
            UserRightValue.TRUE);
  }

  private boolean checkGlobal(final AddressbookDO obj) {
    return obj != null && obj.getId() != null && AddressbookDao.GLOBAL_ADDRESSBOOK_ID == obj.getId();
  }

  /**
   * @see UserRightAccessCheck#hasSelectAccess(PFUserDO,
   * Object)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final AddressbookDO obj) {
    return super.hasSelectAccess(user, obj)
            || checkGlobal(obj)
            || accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.ORGA_TEAM);
  }

  /**
   * Owners and administrators are able to insert new addressbooks.
   *
   * @see UserRightAccessCheck#hasInsertAccess(PFUserDO,
   * Object)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user, final AddressbookDO obj) {
    return super.hasInsertAccess(user, obj)
            || checkGlobal(obj)
            || accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.ORGA_TEAM)
            || hasFullAccess(obj, user.getId());
  }
}
