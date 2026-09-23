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

package org.projectforge.business.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.timesheet.TimesheetFavorite
import org.projectforge.business.timesheet.TimesheetFavoritesService
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.favorites.Favorites
import org.projectforge.framework.utils.NumberHelper
import org.springframework.beans.factory.annotation.Autowired

/**
 * Guards the fix for the area-agnostic legacy XML migration bug:
 *  - the migration hook must no longer leak a flat legacy XML key into an unrelated modern area, and
 *  - [LegacyXmlPrefPhantomCleanup] must delete the phantom rows already conjured by the old behaviour so the affected
 *    features re-derive their real, current data.
 *
 * Must be a `*TestFork`: the preferences cache is process-global.
 */
class LegacyXmlPrefPhantomCleanupTestFork : AbstractTestBase() {
    @Autowired
    private lateinit var cleanup: LegacyXmlPrefPhantomCleanup

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var userPrefCache: UserPrefCache

    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    @Autowired
    private lateinit var userXmlPreferencesDao: UserXmlPreferencesDao

    @Autowired
    private lateinit var timesheetFavoritesService: TimesheetFavoritesService

    @Test
    fun migrationDoesNotLeakFlatKeyIntoModernArea() {
        logon(TEST_USER)
        val userId = getUserId(TEST_USER)
        val key = "leakKey:${NumberHelper.getSecureRandomAlphanumeric(10)}"
        val legacyValue = "ancient-${NumberHelper.getSecureRandomAlphanumeric(6)}"
        userXmlPreferencesDao.saveOrUpdate(userId, key, legacyValue, checkAccess = false)

        // A modern area whose identifier collides with the flat XML key must NOT resurrect the ancient value:
        assertNull(
            userPrefService.getEntry("timesheetTemplateFavorites", key, String::class.java),
            "The flat legacy XML key must not leak into an unrelated modern area.",
        )
        // ...but the designated legacy area still migrates it (no regression):
        assertEquals(
            legacyValue,
            userPrefService.getEntry(UserPrefService.LEGACY_XML_AREA, key, String::class.java),
            "The legacy area must still read the XML value on a cache miss.",
        )
    }

    @Test
    fun deletesPhantomRowAndRestoresCurrentTemplates() {
        logon(TEST_USER)
        val userId = getUserId(TEST_USER)
        val key = Favorites.PREF_NAME_LIST

        // 1. Ancient timesheet favorites still sitting in the flat legacy XML store (what the bug pulled in):
        val ancientName = "ancient-${NumberHelper.getSecureRandomAlphanumeric(6)}"
        userXmlPreferencesDao.saveOrUpdate(userId, key, ancientName, checkAccess = false)
        val ancientValue = userXmlPreferencesDao.internalGetDeserialized(userId, key)
        assertNotNull(ancientValue, "Precondition: the seeded legacy XML row must be readable.")

        // 2. The user's CURRENT templates still live untouched in the old shared "timesheet" slot:
        val current = Favorites<TimesheetFavorite>()
        val currentName = "current-${NumberHelper.getSecureRandomAlphanumeric(6)}"
        current.add(TimesheetFavorite(name = currentName, taskId = 7L))
        userPrefService.putEntry(SHARED_AREA, key, current)

        // 3. The phantom: a modern-area JSON row byte-identical to the ancient XML value (as the old migration wrote it):
        userPrefService.putEntry(TEMPLATE_AREA, key, ancientValue)
        userPrefCache.flushToDB(userId)
        assertNotNull(
            userPrefDao.selectUserPrefs(userId).find { it.area == TEMPLATE_AREA && it.name == key },
            "Precondition: the phantom row must be persisted.",
        )

        // Run the one-time repair.
        cleanup.onApplicationReady()

        // The phantom row is gone; the current data in the shared slot (a different value) is untouched.
        assertNull(
            userPrefDao.selectUserPrefs(userId).find { it.area == TEMPLATE_AREA && it.name == key },
            "The phantom row must be deleted.",
        )

        // getFavorites() now falls through to adoption and restores the current templates.
        logon(TEST_USER)
        val list = timesheetFavoritesService.getList()
        assertTrue(list.any { it.name == currentName }, "Current templates must be restored from the shared slot.")
        assertFalse(list.any { it.name == ancientName }, "Ancient templates must not resurface.")
    }

    companion object {
        private const val SHARED_AREA = "timesheet"
        private const val TEMPLATE_AREA = "timesheetTemplateFavorites"
    }
}
