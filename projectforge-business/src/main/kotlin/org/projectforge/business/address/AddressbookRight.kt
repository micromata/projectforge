/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.address

import org.projectforge.business.common.BaseUserGroupRight
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightCategory
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.web.WicketSupport

/**
 * @author Florian Blumenstein
 */
class AddressbookRight : BaseUserGroupRight<AddressbookDO>(
    UserRightId.MISC_ADDRESSBOOK, UserRightCategory.MISC,
    UserRightValue.TRUE
) {
    /**
     * @see UserRightAccessCheck.hasSelectAccess
     */
    override fun hasSelectAccess(user: PFUserDO?, obj: AddressbookDO): Boolean {
        return super.hasSelectAccess(user, obj)
                || WicketSupport.getAccessChecker().isUserMemberOfGroup(user, ProjectForgeGroup.ORGA_TEAM)
    }

    companion object {
        private const val serialVersionUID = -2928342166476350773L
    }
}
