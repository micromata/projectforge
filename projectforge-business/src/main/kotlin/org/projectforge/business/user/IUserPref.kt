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

package org.projectforge.business.user

import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.io.Serializable

/**
 * For persistence of UserPreferencesData (stores them serialized).
 * The data are stored as xml.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
interface IUserPref : Serializable, IdObject<Long> {
    /**
     * The owner of this preference.
     */
    var user: PFUserDO?

    /**
     * Contains the serialized settings, stored in the database.
     */
    var serializedValue: String?

    var area: String?

    /**
     * Optional, if the user preference should be stored in its own database entry.
     */
    var identifier: String?

    companion object {
        internal fun equals(me: IUserPref, other: Any?): Boolean {
            if (me === other) return true
            if (other == null || me.javaClass != other.javaClass) return false
            other as IUserPref
            return me.user?.id == other.user?.id &&
                    me.area == other.area &&
                    me.identifier == other.identifier
        }

        internal fun hashCode(pref: IUserPref): Int {
            var result = pref.user?.id?.hashCode() ?: 0
            result = 31 * result + pref.area.hashCode()
            result = 31 * result + pref.identifier.hashCode()
            return result
        }
    }
}
