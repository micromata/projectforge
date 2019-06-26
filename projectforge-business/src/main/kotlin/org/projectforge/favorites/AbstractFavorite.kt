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

package org.projectforge.favorites

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext

/**
 * Base class of a favorite (used for user's preferences, such as filters, common used tasks,
 * time sheet templates etc.).
 *
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractFavorite(var name: String? = null,
                                var id: Int? = null)
    : Comparable<AbstractFavorite> {
    /**
     * Uses the locale of the thread local user for comparing.
     * @see ThreadLocalUserContext.localeCompare
     */
    override fun compareTo(other: AbstractFavorite): Int {
        if (name == null) {
            return if (other.name == null) 0 else 1
        } else if (other.name == null) {
            return -1
        }
        return ThreadLocalUserContext.localeCompare(name, other.name)
    }
}
