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


package org.projectforge.framework.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.projectforge.framework.i18n.TimeAgo
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

private val log = KotlinLogging.logger {}

/** The state of a cache on the dashboard, see [CacheAdminService.stateOf]. */
enum class CacheState {
    /** A refresh is running longer than allowed (e.g. blocked on a database lock). */
    STUCK,
    REFRESHING,

    /** The last refresh failed: the cache serves older data and refreshes again on the next access. */
    FAILED,

    /** Not yet filled: not used since the start. */
    NOT_FILLED,

    /** Invalidated, refreshed on the next access. */
    EXPIRED,
    OK,
}

/**
 * A cache of the dashboard's list.
 * @param id The class name, with the identity hash appended if there are several instances of the class.
 */
class CacheEntry(
    val id: String,
    val state: CacheState,
    val status: CacheStatus,
    val lastRefreshTimeAgo: String?,
)

/**
 * The tab "Caches" of the system dashboard: all caches ([AbstractCache.allCaches]) with their state, and the manual
 * refresh of one or all of them. A refresh runs in the background, the dashboard polls the state meanwhile.
 */
@Service
class CacheAdminService {
    private val refreshAllRunning = AtomicBoolean(false)

    fun list(): List<CacheEntry> = caches().map { (id, cache) ->
        val status = cache.status()
        CacheEntry(
            id = id,
            state = stateOf(status),
            status = status,
            lastRefreshTimeAgo = status.lastRefreshStart?.let { TimeAgo.getMessage(Date(it)) },
        )
    }

    /** @return false, if there is no cache with this id. */
    fun refresh(id: String): Boolean {
        val cache = caches().firstOrNull { it.first == id }?.second ?: return false
        log.info { "Manual refresh of cache ${cache.status().name}." }
        cache.forceReloadAsync()
        return true
    }

    enum class TerminateStatus { TERMINATED, NOT_FOUND, NOT_RUNNING }

    /**
     * Terminates the running refresh of the cache, see [AbstractCache.terminateRefresh].
     * @param user Only logged.
     */
    fun terminate(id: String, user: String?): TerminateStatus {
        val cache = caches().firstOrNull { it.first == id }?.second ?: return TerminateStatus.NOT_FOUND
        return if (cache.terminateRefresh(user)) TerminateStatus.TERMINATED else TerminateStatus.NOT_RUNNING
    }

    /**
     * Refreshes all caches one after the other in the background, the dependent ones last (see [REFRESH_LAST]).
     * If a refresh is terminated ([terminate]) whose thread doesn't react, this run only goes on (and a new one may
     * only start) once that thread returns.
     * @return false, if a refresh of all caches is still running.
     */
    fun refreshAll(): Boolean {
        if (!refreshAllRunning.compareAndSet(false, true)) {
            return false
        }
        log.info { "Manual refresh of all caches." }
        scope.launch {
            try {
                caches().map { it.second }
                    .sortedBy { REFRESH_LAST.indexOf(it::class.simpleName) }
                    .forEach { cache ->
                        try {
                            cache.forceReload()
                        } catch (ex: Throwable) {
                            log.error(ex) { "Refresh of cache ${cache::class.simpleName} failed: ${ex.message}" }
                        }
                    }
            } finally {
                refreshAllRunning.set(false)
            }
        }
        return true
    }

    /** The caches with their ids, sorted by name. */
    private fun caches(): List<Pair<String, AbstractCache>> {
        val all = AbstractCache.allCaches()
        val counts = all.groupingBy { it::class.java.name }.eachCount()
        return all.map { cache ->
            val name = cache::class.java.name
            val id = if ((counts[name] ?: 0) > 1) "$name@${System.identityHashCode(cache)}" else name
            id to cache
        }.sortedBy { it.first }
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        /**
         * Refreshed after all others, in this order: they read each other while refreshing (as in
         * FibuCacheInitializer). Index -1 (all others) sorts first.
         */
        private val REFRESH_LAST = listOf("RechnungCache", "EingangsrechnungCache", "AuftragsCache", "AuftragsRechnungCache")

        internal fun stateOf(status: CacheStatus): CacheState = when {
            status.stuck -> CacheState.STUCK
            status.refreshInProgress -> CacheState.REFRESHING
            status.lastRefreshError != null -> CacheState.FAILED
            !status.everFilled -> CacheState.NOT_FILLED
            status.expired -> CacheState.EXPIRED
            else -> CacheState.OK
        }
    }
}
