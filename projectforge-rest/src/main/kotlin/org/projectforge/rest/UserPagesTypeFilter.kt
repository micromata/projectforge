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

package org.projectforge.rest

import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO

class UserPagesTypeFilter(val type: TYPE) : CustomResultFilter<PFUserDO> {
    enum class TYPE(val key: String) : I18nEnum {
        ALL("filter.all"), ADMINS("user.adminUsers"), PRIVILEGED("user.filter.privileged"),
        RESTRICTED("user.filter.restricted");

        /**
         * @return The full i18n key including the i18n prefix "book.type.".
         */
        override val i18nKey: String
            get() = key
    }

    override fun match(list: MutableList<PFUserDO>, element: PFUserDO): Boolean {
        return type == TYPE.ALL ||
                type == TYPE.ADMINS && accessChecker.isUserMemberOfAdminGroup(element) ||
                type == TYPE.PRIVILEGED &&
                accessChecker.isUserMemberOfGroup(
                    element,
                    ProjectForgeGroup.HR_GROUP,
                    ProjectForgeGroup.ADMIN_GROUP,
                    ProjectForgeGroup.FINANCE_GROUP,
                    ProjectForgeGroup.CONTROLLING_GROUP,
                    ProjectForgeGroup.ORGA_TEAM,
                ) || type == TYPE.RESTRICTED && element.restrictedUser
    }

    companion object {
        private val accessChecker: AccessChecker by lazy {
            ApplicationContextProvider.getApplicationContext().getBean(AccessChecker::class.java)
        }
    }
}
