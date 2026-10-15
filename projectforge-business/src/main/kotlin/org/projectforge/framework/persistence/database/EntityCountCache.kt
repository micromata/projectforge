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

package org.projectforge.framework.persistence.database

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.cache.AbstractCache
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * Number of rows per entity, for rough size checks such as the limit of a full re-index for regular users
 * (BaseDao.isFullReindexAllowedForLoggedInUser). Deliberately stale: counts hardly change and such a check needn't
 * be exact, so an entity is counted once and its count kept for [EXPIRE_MILLIS] — instead of a `count(*)` on every
 * list load. A full re-index of an entity drops its count ([invalidate], see DatabaseDao), so it is recounted then.
 *
 * Filled lazily per entity, not an [AbstractCache]: that refreshes all at once, which here would count every table.
 */
@Service
class EntityCountCache {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    private class Entry(val count: Long, val timestamp: Long)

    private val map = ConcurrentHashMap<Class<*>, Entry>()

    /**
     * @return The (cached) number of rows of the given entity, including deleted (historized) ones. 0 for a class
     * that isn't available (entity of an inactive plugin).
     */
    fun count(clazz: Class<*>): Long {
        // compute: one class is counted only once at a time, a concurrent caller waits for the result.
        return map.compute(clazz) { _, entry ->
            if (entry != null && System.currentTimeMillis() - entry.timestamp < EXPIRE_MILLIS) {
                entry
            } else {
                Entry(selectCount(clazz), System.currentTimeMillis())
            }
        }!!.count
    }

    /**
     * Drops the cached count of the given entity, so the next [count] asks the database again.
     */
    fun invalidate(clazz: Class<*>) {
        map.remove(clazz)
    }

    private fun selectCount(clazz: Class<*>): Long {
        return try {
            persistenceService.selectSingleResult("select count(*) from ${clazz.simpleName} t", Long::class.java) ?: 0L
        } catch (ex: Exception) {
            log.info { "Class '${clazz.name}' not available for counting (OK for non-active plugins): ${ex.message}" }
            0L
        }
    }

    companion object {
        const val EXPIRE_MILLIS: Long = 7 * AbstractCache.TICKS_PER_DAY
    }
}
