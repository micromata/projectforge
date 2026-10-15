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
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberHelper
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

/**
 * Verifies the lazy XML->JSON migration: a legacy row in `T_USER_XML_PREFS` is read on a cache miss, exposed via
 * [UserPrefService] under the shared [UserPrefService.LEGACY_XML_AREA], and persisted as a JSON row in `T_USER_PREF`
 * on the next flush. Broken legacy rows must never break the load path (return null instead of throwing).
 *
 * Must be a `*TestFork`: the preferences cache and shutdown flushing are process-global.
 */
class LegacyXmlPrefMigrationTestFork : AbstractTestBase() {
    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var userPrefCache: UserPrefCache

    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    @Autowired
    private lateinit var userXmlPreferencesDao: UserXmlPreferencesDao

    @Test
    fun lazyMigrationTest() {
        logon(TEST_USER)
        val userId = getUserId(TEST_USER)
        val key = "LegacyPref:${NumberHelper.getSecureRandomAlphanumeric(10)}"
        val legacyValue = "legacy-value-${NumberHelper.getSecureRandomAlphanumeric(6)}"

        // Seed a legacy XML row (as the old UserXmlPreferencesCache would have written it):
        userXmlPreferencesDao.saveOrUpdate(userId, key, legacyValue, checkAccess = false)

        // No JSON entry yet -> the lazy migration hook reads the legacy XML row:
        assertEquals(
            legacyValue,
            userPrefService.getEntry(UserPrefService.LEGACY_XML_AREA, key, String::class.java),
            "Legacy XML value should be read on cache miss.",
        )

        // Next flush must persist it as a JSON row in T_USER_PREF:
        userPrefCache.flushToDB(userId)
        val jsonPref = userPrefDao.selectUserPrefs(userId)
            .find { it.area == UserPrefService.LEGACY_XML_AREA && it.name == key }
        assertNotNull(jsonPref, "Migrated legacy entry should be stored as a JSON user pref.")
        assertEquals("^JSON:\"$legacyValue\"", jsonPref!!.serializedValue)

        // The legacy XML row is kept as a fallback and must not be touched:
        assertNotNull(
            userXmlPreferencesDao.internalGetDeserialized(userId, key),
            "Legacy XML row must be preserved (no destructive migration).",
        )

        // After a cache reload the value now comes from JSON (still the same value):
        userPrefCache.setExpired()
        logon(TEST_USER)
        assertEquals(
            legacyValue,
            userPrefService.getEntry(UserPrefService.LEGACY_XML_AREA, key, String::class.java),
            "After reload the value should be served from the JSON store.",
        )
    }

    @Test
    fun brokenLegacyRowIsIgnoredTest() {
        logon(TEST_USER)
        val userId = getUserId(TEST_USER)
        val key = "BrokenPref:${NumberHelper.getSecureRandomAlphanumeric(10)}"

        // Insert a legacy row whose serialized value references a class that no longer exists:
        persistenceService.runInTransaction { context ->
            val entry = UserXmlPreferencesDO().also {
                it.user = PFUserDO().also { u -> u.id = userId }
                it.key = key
                it.serializedValue = "<com.example.removed.LegacyClass>garbage</com.example.removed.LegacyClass>"
                it.created = Date()
                it.lastUpdate = Date()
                it.setVersion()
            }
            context.insert(entry)
        }

        // The load path must never throw on an undeserializable legacy row:
        assertNull(userXmlPreferencesDao.internalGetDeserialized(userId, key))
        assertNull(
            userPrefService.getEntry(UserPrefService.LEGACY_XML_AREA, key, String::class.java),
            "A broken legacy row must resolve to null without throwing.",
        )
    }
}
