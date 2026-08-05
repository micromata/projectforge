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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

private val log = KotlinLogging.logger {}

/**
 * This class is useful, if the stored object of derived classes has to be cached. After reaching expireTime during a
 * request, the method refresh will be called.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractCache {
    private var cacheListeners: CopyOnWriteArrayList<CacheListener>? = null

    protected var expireTime: Long = 60 * TICKS_PER_MINUTE

    @Transient
    @Volatile
    private var timeOfLastRefresh: Long = -1

    @Transient
    @Volatile
    private var isExpired = true

    /**
     * @return true if currently a cache refresh is running, otherwise false.
     */
    @Transient
    @Volatile
    var isRefreshInProgress: Boolean = false
        private set

    /**
     * Guards [performRefresh], so a cache is never refreshed by two threads at the same time. Reentrant, because a
     * refresh may (indirectly) access this very cache again: such nested calls must not start a second refresh.
     */
    @Transient
    private val refreshLock = ReentrantLock()

    /**
     * Incremented by every [setExpired]. A caller waiting for a refresh of another thread remembers the value it has
     * seen and can therefore decide whether that refresh already covers its own invalidation or is older than it.
     */
    @Transient
    private val invalidationCounter = AtomicLong(0)

    /**
     * Value of [invalidationCounter] at the begin of the last refresh: the data of the cache reflects all
     * invalidations up to this value.
     */
    @Transient
    @Volatile
    private var refreshedInvalidation = -1L

    /**
     * @return true if the cache is initialized, otherwise false (no refresh has been made yet).
     */
    val initialized: Boolean
        get() = timeOfLastRefresh != -1L

    protected constructor()

    /**
     * @param expireTime in milliseconds.
     */
    protected constructor(expireTime: Long) {
        this.expireTime = expireTime
    }

    open fun setExpireTimeInMinutes(expireTime: Long) {
        this.expireTime = expireTime * TICKS_PER_MINUTE
    }

    fun setExpireTimeInSeconds(expireTime: Long) {
        this.expireTime = expireTime * TICKS_PER_SECOND
    }

    fun setExpireTimeInHours(expireTime: Long) {
        this.expireTime = expireTime * TICKS_PER_HOUR
    }

    /**
     * Cache will be refreshed before next use.
     */
    open fun setExpired() {
        this.invalidationCounter.incrementAndGet()
        this.isExpired = true
    }

    /**
     * Sets the cache to expired and performs a synchronous refresh.
     *
     * Does nothing but expiring the cache, if a refresh of this cache is already in progress (see [performRefresh]):
     * the running refresh will deliver the fresh data, a second concurrent run would only race with it.
     */
    fun forceReload() {
        setExpired()
        performRefresh()
    }

    fun register(listener: CacheListener) {
        cacheListeners = cacheListeners ?: CopyOnWriteArrayList()
        cacheListeners!!.add(listener)
    }

    fun unregister(listener: CacheListener) {
        cacheListeners?.remove(listener)
    }

    /**
     * Checks the expired time and triggers refresh if cache is expired.
     * When the cache was explicitly invalidated (via setExpired/forceReload/clear) or is not yet
     * initialized, the refresh runs synchronously. When the cache expired only due to time
     * (age > expireTime), the refresh runs asynchronously to avoid blocking threads that may
     * hold DB connections.
     */
    protected fun checkRefresh() {
        if (this.isExpired) {
            // Explicitly invalidated or not yet initialized: must refresh synchronously.
            performRefresh()
            return
        }

        if (System.currentTimeMillis() - this.timeOfLastRefresh <= this.expireTime) {
            return
        }

        if (!initialized) {
            performRefresh()
            return
        }

        // Cache is initialized and expired only by time: trigger async refresh, return stale data.
        if (!refreshLock.isLocked) {
            refreshScope.launch {
                performRefresh(byTimeExpiry = true)
            }
        }
    }

    /**
     * Refreshes the cache, guarded by [refreshLock]: only one thread at a time may refresh this cache. All other
     * callers return immediately instead of starting a competing refresh - two concurrent refreshes would both build
     * their own data and the slower one would overwrite the newer result of the faster one (last writer wins).
     *
     * A nested call from the refresh thread itself (a refresh indirectly accessing this same cache) also returns
     * immediately: the outer refresh is already underway and re-entering it would recurse endlessly.
     *
     * @param byTimeExpiry true, if the cache is only outdated by [expireTime] (and wasn't invalidated), see
     * [checkRefresh].
     */
    private fun performRefresh(byTimeExpiry: Boolean = false) {
        if (refreshLock.isHeldByCurrentThread) {
            // Nested access from within our own refresh: the outer call is doing the work.
            return
        }
        // Remember the state we want to be refreshed before waiting for the lock: a refresh started after this point
        // includes our invalidation, so we don't have to refresh again.
        val requiredInvalidation = invalidationCounter.get()
        if (!refreshLock.tryLock(MAX_WAIT_FOR_CONCURRENT_REFRESH_MS, TimeUnit.MILLISECONDS)) {
            log.warn { "Refresh of cache ${this::class.simpleName} is already in progress, using current data." }
            return
        }
        try {
            if (isUpToDate(byTimeExpiry, requiredInvalidation)) {
                // Another thread already refreshed while we waited for the lock.
                return
            }
            cacheListeners?.forEach { listener -> listener.onBeforeCacheRefresh() }
            try {
                isRefreshInProgress = true
                val invalidation = invalidationCounter.get()
                this.timeOfLastRefresh = System.currentTimeMillis()
                try {
                    this.refresh()
                } catch (ex: Throwable) {
                    log.error(ex.message, ex)
                }
                this.refreshedInvalidation = invalidation
                this.isExpired = false
            } finally {
                isRefreshInProgress = false
            }
        } finally {
            refreshLock.unlock()
        }
        // Notify the listeners after releasing the lock: a listener may invalidate and reload this cache again
        // (see AuftragsRechnungCache), which must not deadlock nor be swallowed as a nested call.
        cacheListeners?.forEach { listener -> listener.onAfterCacheRefresh() }
    }

    /**
     * Checks whether a refresh done by another thread while we were waiting for [refreshLock] already delivered the
     * data this caller needs, so refreshing again would only be a waste of resources.
     *
     * @param byTimeExpiry true, if the caller only wants to refresh the cache, because it is older than [expireTime].
     * @param requiredInvalidation Value of [invalidationCounter] seen by the caller before waiting for the lock.
     */
    private fun isUpToDate(byTimeExpiry: Boolean, requiredInvalidation: Long): Boolean {
        if (!initialized) {
            return false
        }
        if (byTimeExpiry) {
            // Any refresh completed in the meantime is fresh enough.
            return System.currentTimeMillis() - timeOfLastRefresh <= expireTime
        }
        return refreshedInvalidation >= requiredInvalidation
    }

    /**
     * Please implement this method refreshing the stored object _data. Do not forget to call checkRefresh in your cache
     * methods.
     *
     * @see .checkRefresh
     */
    protected abstract fun refresh()

    companion object {
        private val refreshScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        /**
         * Time a caller waits for a refresh running in another thread before giving up and working with the data
         * currently held by the cache. Only relevant for callers which need fresh data (expired cache), so it must
         * cover a normal refresh duration, but must never block a request thread for long (it may hold a DB
         * connection).
         */
        private const val MAX_WAIT_FOR_CONCURRENT_REFRESH_MS = 10_000L

        /**
         * Milliseconds.
         */
        const val TICKS_PER_SECOND: Long = 1000

        /**
         * Milliseconds.
         */
        const val TICKS_PER_MINUTE: Long = TICKS_PER_SECOND * 60

        /**
         * Milliseconds.
         */
        const val TICKS_PER_HOUR: Long = TICKS_PER_MINUTE * 60

        /**
         * Milliseconds.
         */
        const val TICKS_PER_DAY: Long = 24 * TICKS_PER_HOUR
    }
}
