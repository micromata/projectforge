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

/** The stored choice of a user; a class (not a bare Int) so it can grow without a migration. */
class TimesheetTimeGridPref(var minutes: Int? = null)

/**
 * The grid of the logged-in user's time sheet times ([TimesheetTimeGridConfig]), kept in the user's preferences like the
 * colour theme (no DB migration), so it follows the user across devices: chosen in "My account" of the web pages or in
 * the settings of the app.
 */
@Service
class TimesheetTimeGridService {
    @Autowired
    private lateinit var userPrefService: UserPrefService

    /** The grid in minutes of the logged-in user, [TimesheetTimeGridConfig.DEFAULT] without a (valid) choice. */
    fun get(): Int {
        val pref = userPrefService.getEntry(PREF_AREA, PREF_NAME, TimesheetTimeGridPref::class.java)
        return TimesheetTimeGridConfig.parse(pref?.minutes)
    }

    /** Stores [minutes] (one of [TimesheetTimeGridConfig.ALLOWED], else the default) and returns what was stored. */
    fun set(minutes: Int?): Int {
        val value = TimesheetTimeGridConfig.parse(minutes)
        userPrefService.putEntry(PREF_AREA, PREF_NAME, TimesheetTimeGridPref(value))
        return value
    }

    companion object {
        const val PREF_AREA = "timesheet"
        const val PREF_NAME = "timeGrid"
    }
}
