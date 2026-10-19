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

package org.projectforge.business.fibu

import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.StringHelper2

/**
 * The contacts of an order or a project: one main contact person plus further contacts, stored as comma separated
 * user ids (see [AuftragDO.additionalContactUserIds], [ProjektDO.additionalContactUserIds]). All contacts are
 * equal regarding access, the main contact is just the one named first.
 */
object ContactUserIds {
    /**
     * The further contacts as stored: in the given order, without nulls, duplicates and the main contact person
     * (who is a contact anyway), ", " separated.
     * @return null if none is left.
     */
    fun normalize(contactPersonId: Long?, ids: Iterable<Long?>?): String? {
        return ids?.filterNotNull()?.distinct()?.filter { it != contactPersonId }
            ?.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    /**
     * The ids of the stored further contacts.
     */
    fun toList(additionalContactUserIds: String?): List<Long> {
        return StringHelper2.splitToListOfLongValues(additionalContactUserIds)
    }

    /**
     * True if the given user is the main contact person or one of the further contacts.
     */
    fun isContact(contactPersonId: Long?, additionalContactUserIds: String?, userId: Long?): Boolean {
        userId ?: return false
        return contactPersonId == userId || toList(additionalContactUserIds).contains(userId)
    }

    /**
     * The full names of the given contacts (main contact first), "; " separated. Unknown ids are skipped.
     */
    fun fullNames(contactPersonId: Long?, additionalContactUserIds: String?): String {
        val cache = UserGroupCache.getInstance()
        return (listOfNotNull(contactPersonId) + toList(additionalContactUserIds))
            .mapNotNull { cache.getUser(it)?.getFullname() }
            .joinToString("; ")
    }
}
