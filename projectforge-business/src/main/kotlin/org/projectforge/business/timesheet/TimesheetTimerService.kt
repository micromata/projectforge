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

package org.projectforge.business.timesheet

import org.projectforge.business.user.service.UserPrefService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * The running timer of a user as kept in the user's preferences: when it was started and, optionally, what is booked
 * (the cost unit and the texts a stopped timer fills into the form). Plain values, so it can grow without a migration.
 */
class TimesheetTimerPref(
    /** UTC, milliseconds since 1970. */
    var startedAt: Long? = null,
    var taskId: Long? = null,
    var kost2Id: Long? = null,
    var description: String? = null,
    var location: String? = null,
    var reference: String? = null,
    var tag: String? = null,
)

/**
 * The timer of the logged-in user, started in the app or on the web pages and stopped on any of them: the server keeps
 * the start (the server's clock, so a wrong clock of a device does not matter), the clients round it to the user's grid
 * ([TimesheetTimeGridService]) when they stop it. Only the logged-in user's own preferences are touched.
 */
@Service
class TimesheetTimerService {
    @Autowired
    private lateinit var userPrefService: UserPrefService

    /** The running timer, null without one. */
    fun get(): TimesheetTimerPref? {
        val pref = userPrefService.getEntry(PREF_AREA, PREF_NAME, TimesheetTimerPref::class.java)
        return pref?.takeIf { it.startedAt != null }
    }

    /** Starts the timer now, replacing a running one. [what] is taken without its start. */
    fun start(what: TimesheetTimerPref?): TimesheetTimerPref {
        val pref = TimesheetTimerPref(
            startedAt = System.currentTimeMillis(),
            taskId = what?.taskId,
            kost2Id = what?.kost2Id,
            description = what?.description?.take(MAX_TEXT),
            location = what?.location?.take(MAX_SHORT_TEXT),
            reference = what?.reference?.take(MAX_SHORT_TEXT),
            tag = what?.tag?.take(MAX_SHORT_TEXT),
        )
        userPrefService.putEntry(PREF_AREA, PREF_NAME, pref)
        return pref
    }

    /** Stops the timer. An empty entry rather than a removed one, so the stored copy is replaced as well. */
    fun clear() {
        userPrefService.putEntry(PREF_AREA, PREF_NAME, TimesheetTimerPref())
    }

    companion object {
        const val PREF_AREA = "timesheet"
        const val PREF_NAME = "timer"
        const val MAX_TEXT = 4000
        const val MAX_SHORT_TEXT = 255
    }
}
