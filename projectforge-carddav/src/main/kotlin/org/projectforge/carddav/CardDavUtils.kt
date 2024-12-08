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

package org.projectforge.carddav

import org.projectforge.carddav.model.Contact
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*

internal object CardDavUtils {
    fun getETag(contact: Contact): String {
        val lastUpdated = contact.lastUpdated ?: Date()
        return lastUpdated.time.toString()
    }

    /**
     * Returns the display name of the user's addressbook.
     * This is the name that is shown in the CardDAV client.
     * @param user The user.
     */
    fun getUsersAddressbookDisplayName(user: PFUserDO): String {
        return translateMsg("address.cardDAV.addressbook.displayName", user.firstname)
    }
}
