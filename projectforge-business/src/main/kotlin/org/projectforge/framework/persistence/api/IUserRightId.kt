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

package org.projectforge.framework.persistence.api

import org.projectforge.common.i18n.I18nEnum

/**
 * An User right.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
interface IUserRightId : I18nEnum {
    /**
     * ID of the right
     *
     * @return
     */
    val id: String?

    /**
     * how to order.
     *
     * @return
     */
    val orderString: String?

    operator fun compareTo(o: IUserRightId?): Int {
        return this.compareTo(o)
    }
}
