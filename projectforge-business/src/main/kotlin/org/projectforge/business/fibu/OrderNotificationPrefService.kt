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

import org.projectforge.business.user.service.UserPrefService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/** Reads and writes the [OrderNotificationPrefs] of a user (a persistent user preference). */
@Service
class OrderNotificationPrefService {
    @Autowired
    private lateinit var userPrefService: UserPrefService

    /** The settings of the user, the defaults (all mails at once) if never set. */
    fun get(userId: Long): OrderNotificationPrefs {
        return userPrefService.getEntry(AREA, NAME, OrderNotificationPrefs::class.java, userId)
            ?: OrderNotificationPrefs()
    }

    fun save(userId: Long, prefs: OrderNotificationPrefs) {
        userPrefService.putEntry(AREA, NAME, prefs, true, userId)
    }

    /** The mode applying to the given order for the user. */
    fun resolve(userId: Long, auftrag: AuftragDO): OrderNotificationMode {
        val projekt = auftrag.projekt
        return get(userId).resolve(projekt?.id, auftrag.kunde?.nummer, projekt?.kunde?.nummer)
    }

    companion object {
        const val AREA = "orderNotification"
        const val NAME = "prefs"
    }
}
