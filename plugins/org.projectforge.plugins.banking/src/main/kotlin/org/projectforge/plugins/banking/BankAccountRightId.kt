/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.banking

import org.hibernate.search.annotations.ClassBridge
import org.hibernate.search.annotations.Indexed
import org.projectforge.business.user.HibernateSearchUserRightIdBridge
import org.projectforge.framework.persistence.api.IUserRightId

@Indexed
@ClassBridge(impl = HibernateSearchUserRightIdBridge::class)
enum class BankAccountRightId(override val id: String, override val orderString: String?, override val i18nKey: String?)
    : IUserRightId {
    PLUGIN_BANKING_ACCOUNT("PLUGIN_BANKING_ACCOUNT", null, null);
}
