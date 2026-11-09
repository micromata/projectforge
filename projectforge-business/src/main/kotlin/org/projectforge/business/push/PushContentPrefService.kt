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


package org.projectforge.business.push

import org.projectforge.business.user.service.UserPrefService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Whether a push shows the title and the beginning of the notification, or only a generic text. Apple sees the payload:
 * the server may switch the content off for everybody (`projectforge.push.content=false`), else each user decides
 * ([PushSettings], a persistent user preference, edited in "My account" and in the app).
 */
@Service
class PushContentPrefService {
    @Value("\${projectforge.push.content:true}")
    internal var enabled: Boolean = true

    @Autowired
    private lateinit var userPrefService: UserPrefService

    /** False if the server keeps the content out of every push: the users have nothing to choose then. */
    val isAvailable: Boolean
        get() = enabled

    /** The settings of the user, the defaults (with content) if never set. */
    fun get(userId: Long): PushSettings {
        return userPrefService.getEntry(AREA, NAME, PushSettings::class.java, userId) ?: PushSettings()
    }

    fun save(userId: Long, settings: PushSettings) {
        userPrefService.putEntry(AREA, NAME, settings, true, userId)
    }

    /** True if a push to the user shows the content of the notification. */
    fun contentAllowed(userId: Long): Boolean {
        return enabled && get(userId).content
    }

    companion object {
        const val AREA = "push"
        const val NAME = "settings"
    }
}

/** Serializable value stored in the user's preferences; a class (not a bare Boolean) so it can grow without a migration. */
class PushSettings(var content: Boolean = true)
