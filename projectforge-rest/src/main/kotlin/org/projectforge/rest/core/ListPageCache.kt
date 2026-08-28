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

package org.projectforge.rest.core

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseDOModifiedListener
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val log = KotlinLogging.logger {}

/**
 * The materialized ordered id list of a server-side paged list, cached per (session, filter). See
 * [ListPageCache] and `MIGRATION-list-paging.md`.
 *
 * @param fingerprint [org.projectforge.framework.persistence.api.MagicFilter.resultFingerprint] the list was built for.
 * @param ids The ordered ids the filter selected (already access-checked and sorted).
 * @param truncated True if the underlying query hit its row cap, so [ids] may be incomplete.
 * @param changeCounter The value of [ListPageChangeCounter] for the entity at build time; a later value means
 *   the entity changed and the list must be rebuilt.
 * @param createdMillis Build time (informational; the effective TTL is enforced by [ExpiringSessionAttributes]).
 */
class CachedIdList(
    val fingerprint: String,
    val ids: LongArray,
    val truncated: Boolean,
    val changeCounter: Long,
    val createdMillis: Long,
) {
    /**
     * The whole-result statistics of this id list ([AbstractEntityRest.aggregate]), computed once and reused
     * while paging through the same filter. Null until the first page of this id list is served. Tied to the
     * id list on purpose: both depend only on the filter and the entity's change counter, so a change that
     * rebuilds the ids rebuilds these too. Without this, every page flip would recompute the statistics, which
     * for a list like the invoices means reloading the whole result set (all invoices plus the previous-year
     * set) from the database on each flip.
     */
    @Volatile
    var statistics: Any? = null
}

/**
 * A monotonically increasing change counter per entity class.
 *
 * Registered once on every [BaseDao] as a [BaseDOModifiedListener]; each insert, update or delete bumps the
 * counter for that entity. [ListPageCache] compares the counter it stored against the current one, so a cached
 * paged id list built before a change is transparently discarded ("someone added an order while I was on page 3"
 * self-heals). This is a cheap, coarse invalidation: any change to the entity invalidates every user's cached
 * lists of it, which is fine — the id list is cheap to rebuild and correctness never depends on it (a stale
 * list can only yield a short page, never a forbidden or wrong row, see [ListPageCache]).
 */
@Service
class ListPageChangeCounter {
    @Autowired
    private lateinit var baseDaos: List<BaseDao<*>>

    private val counters = ConcurrentHashMap<Class<*>, AtomicLong>()

    @PostConstruct
    private fun registerListeners() {
        baseDaos.forEach { dao ->
            val doClass = dao.doClass
            @Suppress("UNCHECKED_CAST")
            (dao as BaseDao<ExtendedBaseDO<Long>>).register(object : BaseDOModifiedListener<ExtendedBaseDO<Long>> {
                override fun afterInsertOrModify(obj: ExtendedBaseDO<Long>, operationType: OperationType) {
                    counters.getOrPut(doClass) { AtomicLong() }.incrementAndGet()
                }
            })
        }
        log.info { "Registered list-page change counter on ${baseDaos.size} DAOs." }
    }

    /** The current change counter for the given entity class (0 if it never changed). */
    fun getCounter(doClass: Class<*>): Long = counters[doClass]?.get() ?: 0L
}

/**
 * Session cache of the ordered id list materialized for a server-side paged list (Stage 2 of
 * `MIGRATION-list-paging.md`).
 *
 * Stored via [ExpiringSessionAttributes] (per session, sliding TTL, swept by a background timer) — the same
 * store [org.projectforge.rest.multiselect.MultiSelectionSupport] uses, deliberately not [UserPrefService]:
 * these are scratch data that must never outlive the session or hit the database.
 *
 * A cache entry is used only when the filter fingerprint matches **and** the entity's [ListPageChangeCounter]
 * is unchanged since the list was built. It is never a correctness authority: the served page always runs
 * through `getListByIds` → `BaseDao.select(ids)` → per-row access check, so a stale entry can at worst produce
 * a short page, never a forbidden row.
 */
@Service
class ListPageCache {
    @Autowired
    private lateinit var changeCounter: ListPageChangeCounter

    /**
     * @return the cached id list for [fingerprint] if present and not invalidated by an entity change, else null.
     */
    fun get(
        request: HttpServletRequest,
        category: String,
        doClass: Class<*>,
        fingerprint: String,
    ): CachedIdList? {
        val store = getStore(request, category) ?: return null
        synchronized(store) {
            val cached = store[fingerprint] ?: return null
            if (cached.changeCounter != changeCounter.getCounter(doClass)) {
                store.remove(fingerprint)
                return null
            }
            // Touch for LRU: re-insert as most-recently-used.
            store.remove(fingerprint)
            store[fingerprint] = cached
            return cached
        }
    }

    /**
     * Builds a [CachedIdList] for the given ids (stamping the current change counter) and stores it, capped at
     * [MAX_ENTRIES_PER_CATEGORY] most-recently-used entries per category.
     */
    fun put(
        request: HttpServletRequest,
        category: String,
        doClass: Class<*>,
        fingerprint: String,
        ids: LongArray,
        truncated: Boolean,
    ): CachedIdList {
        val cached = CachedIdList(
            fingerprint = fingerprint,
            ids = ids,
            truncated = truncated,
            changeCounter = changeCounter.getCounter(doClass),
            createdMillis = System.currentTimeMillis(),
        )
        val store = getOrCreateStore(request, category)
        synchronized(store) {
            store.remove(fingerprint)
            store[fingerprint] = cached
            while (store.size > MAX_ENTRIES_PER_CATEGORY) {
                val eldest = store.keys.iterator().next()
                store.remove(eldest)
            }
        }
        return cached
    }

    private fun attributeName(category: String) = "$SESSION_ATTRIBUTE_PREFIX$category"

    @Suppress("UNCHECKED_CAST")
    private fun getStore(request: HttpServletRequest, category: String): LinkedHashMap<String, CachedIdList>? {
        return ExpiringSessionAttributes.getAttribute(request, attributeName(category)) as? LinkedHashMap<String, CachedIdList>
    }

    private fun getOrCreateStore(request: HttpServletRequest, category: String): LinkedHashMap<String, CachedIdList> {
        getStore(request, category)?.let { return it }
        val store = LinkedHashMap<String, CachedIdList>()
        ExpiringSessionAttributes.setAttribute(request, attributeName(category), store, TTL_MINUTES)
        return store
    }

    companion object {
        private const val SESSION_ATTRIBUTE_PREFIX = "listPageIds:"
        private const val MAX_ENTRIES_PER_CATEGORY = 4
        private const val TTL_MINUTES = 30
    }
}
