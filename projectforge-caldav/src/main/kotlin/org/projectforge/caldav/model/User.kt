/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity(name = "t_pf_user")
class User {
    @Id
    var pk: Long? = null
    @get:Name
    @Column(name = "username")
    var username: String? = null
    @Column(name = "deleted")
    var deleted: Boolean? = null
    @Column(name = "authentication_token")
    var authenticationToken: String? = null

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val user = o as User
        return if (pk != null) pk == user.pk else user.pk == null
    }

    override fun hashCode(): Int {
        return if (pk != null) 42 * pk.hashCode() else 0
    }

    override fun toString(): String {
        return String.format("User[id=%d, username='%s']", pk, username)
    }
}
