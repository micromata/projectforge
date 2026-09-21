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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.favorites.Favorites
import org.springframework.beans.factory.annotation.Autowired

/**
 * Timesheet template favorites used to share the same `(area, name)` pref slot as the react list's filter favorites
 * (both `area = "timesheet"`, `name = `[Favorites.PREF_NAME_LIST]). Saving a template overwrote the filter favorites
 * and vice versa, and reading the slot with the wrong element type threw a `ClassCastException`. Template favorites now
 * live in their own area; [TimesheetFavoritesService.getFavorites] must adopt template favorites still stranded in the
 * old shared slot and clear that slot afterwards.
 *
 * Must be a `*TestFork`: the preferences cache is process-global.
 */
class TimesheetFavoritesCollisionTestFork : AbstractTestBase() {
    @Autowired
    private lateinit var timesheetFavoritesService: TimesheetFavoritesService

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Test
    fun adoptsStrandedTemplateFavoritesFromSharedSlot() {
        logon(TEST_USER)

        // Simulate the pre-fix state: a template favorite stored under the old shared "timesheet" slot.
        val stranded = Favorites<TimesheetFavorite>()
        stranded.add(TimesheetFavorite(name = "myTemplate", taskId = 42L))
        userPrefService.putEntry(LEGACY_SHARED_AREA, Favorites.PREF_NAME_LIST, stranded)

        // getFavorites() finds nothing in the dedicated area -> adopts from the shared slot.
        val favorites = timesheetFavoritesService.getFavorites()
        assertTrue(favorites.favoriteNames.contains("myTemplate"), "Stranded template favorite should be adopted.")

        // The dedicated area now holds the adopted list.
        val dedicated = userPrefService.getEntry(NEW_AREA, Favorites.PREF_NAME_LIST, Favorites::class.java) as? Favorites<*>
        assertNotNull(dedicated, "Adopted favorites should be stored under the dedicated area.")
        assertTrue(dedicated!!.favoriteNames.contains("myTemplate"))

        // The old shared slot is cleared so the react list no longer sees a foreign-typed entry.
        val shared = userPrefService.getEntry(LEGACY_SHARED_AREA, Favorites.PREF_NAME_LIST, Favorites::class.java) as? Favorites<*>
        assertTrue(shared == null || shared.favoriteNames.isEmpty(), "Old shared slot should be cleared after adoption.")
    }

    companion object {
        private const val LEGACY_SHARED_AREA = "timesheet"
        private const val NEW_AREA = "timesheetTemplateFavorites"
    }
}
