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

package org.projectforge.caldav.model

import io.milton.annotations.Name

class User {
    var id: Long? = null
    @get:Name
    var username: String? = null
    var authenticationToken: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return if (id != null) 42 * id.hashCode() else 0
    }

    override fun toString(): String {
        return "User[id=$id, username='$username']"
    }
}
