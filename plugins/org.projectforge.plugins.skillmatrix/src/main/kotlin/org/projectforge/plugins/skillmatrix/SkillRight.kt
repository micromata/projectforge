/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.skillmatrix

import org.projectforge.business.user.UserRightAccessCheck
import org.projectforge.business.user.UserRightCategory
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * Define the access rights.
 *
 * @author Kai Reinhard (k.reinhard@me.de)
 */
class SkillRight() : UserRightAccessCheck<SkillEntryDO>(SkillRightId.PLUGIN_SKILL_MATRIX, UserRightCategory.PLUGINS, UserRightValue.TRUE) {
    /**
     * @return true if the owner is equals to the logged-in user, otherwise false.
     */
    override fun hasAccess(user: PFUserDO, obj: SkillEntryDO?, oldObj: SkillEntryDO?,
                           operationType: OperationType): Boolean {
        // Check insert on own skill!
        if (operationType == OperationType.SELECT) {
            return true
        }
        val skill = oldObj ?: obj ?: return true // return true: general insert access.
        // Everybody may select the skill of other users but may only modify own skills.
        return user.id == skill.ownerId
    }
}
