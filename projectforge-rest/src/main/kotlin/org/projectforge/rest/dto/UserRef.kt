/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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


package org.projectforge.rest.dto

import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * A user as a view shows it by name: id, display name and whether it is still active, for the views that would
 * otherwise send a name and a flag (HR view, booking grants, notification log, …).
 *
 * [deleted] and [deactivated] are null unless true, so `NON_NULL` leaves them off the wire. The frontend shows the
 * ref with `RefName` (`inactiveRefClass`): a deleted user struck through in red, a deactivated one in grey.
 */
class UserRef(
    val id: Long?,
    val displayName: String?,
    val deleted: Boolean? = null,
    val deactivated: Boolean? = null,
) {
    /** The user's full name and inactive flags. */
    constructor(user: PFUserDO) : this(
        id = user.id,
        displayName = user.getFullname(),
        deleted = user.deleted.takeIf { it },
        deactivated = user.deactivated.takeIf { it },
    )

    companion object {
        /** As the constructor; null for no user. */
        fun of(user: PFUserDO?): UserRef? = user?.let { UserRef(it) }
    }
}
