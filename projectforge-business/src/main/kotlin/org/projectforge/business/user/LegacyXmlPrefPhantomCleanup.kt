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

import mu.KotlinLogging
import org.projectforge.business.timesheet.TimesheetFavoritesService
import org.projectforge.business.user.service.UserPrefService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * One-time repair of "phantom" user preferences created by an earlier bug in the legacy XML->JSON migration.
 *
 * The migration hook [UserPrefCache.migrateLegacyEntryOnCacheMiss] used to look up the flat, per-user legacy XML store
 * by the requested key's identifier alone, ignoring its area. Because the legacy store is a flat key namespace, any
 * modern area whose identifier collided with an old flat XML key (notably [org.projectforge.favorites.Favorites.PREF_NAME_LIST]
 * `= "favorites.list"`, used by timesheet template favorites, calendar filters and every list page's filter favorites)
 * pulled the years-old XML blob on its first (cache-missing) read and persisted it as a JSON row - overwriting the
 * user's current preferences with ancient data. The hook is now gated on [UserPrefService.LEGACY_XML_AREA], so no new
 * phantoms are produced; this component removes the ones already written.
 *
 * A phantom is unambiguous: a JSON [UserPrefDO] row in a modern area whose serialized value is byte-identical to the
 * same user's legacy XML row of the same key. A user cannot independently produce content identical to their own
 * ancient XML in an unrelated modern area, and even in the astronomically unlikely coincidence the deletion is safe -
 * the feature simply re-derives the same value from its true source (e.g. [TimesheetFavoritesService.getFavorites]
 * falls back to `adoptFromCollidingArea()` / `migrateFromLegacyFavorites()` and restores the current templates that
 * still sit untouched in the old shared slot).
 *
 * The repair is idempotent and self-limiting: [UserPrefDao.selectLegacyXmlPhantomCandidates] returns near-empty once
 * the phantoms are gone (and re-derived rows differ from the ancient XML), so no persisted "already run" marker is
 * needed. Run on [ApplicationReadyEvent] (database migrated, caches usable, no user sessions yet). A failure here only
 * leaves phantoms in place; it never aborts the start.
 */
@Service
class LegacyXmlPrefPhantomCleanup {
    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    @Autowired
    private lateinit var userXmlPreferencesDao: UserXmlPreferencesDao

    @Autowired
    private lateinit var userPrefCache: UserPrefCache

    @Autowired
    private lateinit var timesheetFavoritesService: TimesheetFavoritesService

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        try {
            cleanup()
        } catch (ex: Exception) {
            // Logged, not rethrown: the start must not fail because of a best-effort repair.
            log.error(ex) { "Legacy XML phantom preference cleanup failed: ${ex.message}" }
        }
    }

    private fun cleanup() {
        val candidates = userPrefDao.selectLegacyXmlPhantomCandidates(UserPrefService.LEGACY_XML_AREA)
        if (candidates.isEmpty()) {
            return
        }
        val affectedUsers = mutableSetOf<Long>()
        var deleted = 0
        for (candidate in candidates) {
            val id = candidate.id ?: continue
            val userId = candidate.user?.id ?: continue
            val name = candidate.name ?: continue
            // The legacy XML value re-serialized must equal the stored JSON row for the row to be a phantom.
            val xmlValue = userXmlPreferencesDao.internalGetDeserialized(userId, name) ?: continue
            val xmlSerialized = UserPrefDao.serialize(xmlValue, compressBigContent = false)
            val rowSerialized = UserPrefDao.getUncompressed(candidate.serializedValue)
            if (xmlSerialized != rowSerialized) {
                // A legitimate modern row that merely shares its (user, name) with a legacy XML key - leave it alone.
                continue
            }
            userPrefDao.internalDelete(id)
            ++deleted
            affectedUsers.add(userId)
            log.info { "Deleted phantom user pref (from legacy XML migration): userId=$userId, area=${candidate.area}, name=$name" }
        }
        if (deleted > 0) {
            // Drop the stale in-memory cache so the next read re-derives from the true source.
            affectedUsers.forEach { userId ->
                userPrefCache.clear(userId)
                timesheetFavoritesService.refreshMigrationCache(userId)
            }
            log.info { "Legacy XML phantom preference cleanup done: deleted $deleted phantom row(s) for ${affectedUsers.size} user(s)." }
        }
    }
}
