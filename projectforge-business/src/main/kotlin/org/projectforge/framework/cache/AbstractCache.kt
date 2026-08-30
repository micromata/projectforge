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
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

private val log = KotlinLogging.logger {}

/**
 * This class is useful, if the stored object of derived classes has to be cached. After reaching expireTime during a
 * request, the method refresh will be called.
 *
 * @author Kai Reinhard
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
     * [System.currentTimeMillis] when the currently running [refresh] started, or -1 if none is running. Used by
     * [checkStuckRefresh] to detect a refresh that never returns (e.g. a JDBC call blocked on a DB lock).
     */
    @Transient
    @Volatile
    private var refreshStartMillis: Long = -1

    /**
     * The thread that is currently executing [refresh], so [checkStuckRefresh] can interrupt it if it got wedged.
     */
    @Transient
    @Volatile
    private var refreshingThread: Thread? = null

    /**
     * Guards [checkStuckRefresh] so a wedged refresh is reported (and interrupted) only once per refresh, not on
     * every request that observes it.
     */
    @Transient
    @Volatile
    private var stuckRefreshReported = false

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
     * Note: this becomes true as soon as a refresh _starts_ (see [performRefresh], which sets [timeOfLastRefresh]
     * before calling [refresh]). It is therefore NOT a reliable "the cache holds data" signal - use [everFilled]
     * for that.
     */
    val initialized: Boolean
        get() = timeOfLastRefresh != -1L

    /**
     * @return true once at least one [refresh] has _completed successfully_, i.e. the cache actually holds data.
     * Unlike [initialized] this only flips after [refresh] returned without throwing (see [performRefresh]).
     */
    private val everFilled: Boolean
        get() = refreshedInvalidation != -1L

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
     *
     * A synchronous refresh triggered here from _within_ another cache's refresh (mutually dependent caches, e.g.
     * AuftragsCache <-> AuftragsRechnungCache) never blocks on the other cache's lock, see [performRefresh]: that
     * cross-lock wait was the cause of a production deadlock that froze the order book and invoices until a restart.
     *
     * @param waitForRefresh If true, a refresh caused only by time expiry is also done _synchronously_ (the caller
     * waits for it, up to [MAX_WAIT_FOR_CONCURRENT_REFRESH_MS], instead of getting stale data). Intended for tests and
     * the rare caller that needs guaranteed fresh data even without an explicit invalidation. Still deadlock-safe: like
     * every refresh it honors the nested-refresh guard, so a nested cross-cache call never blocks on the other lock.
     * The invalidation path is synchronous regardless of this flag.
     */
    @JvmOverloads
    protected fun checkRefresh(waitForRefresh: Boolean = false) {
        checkStuckRefresh()
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

        // Cache is initialized and expired only by time.
        if (waitForRefresh) {
            // Caller wants guaranteed fresh data: refresh synchronously (deadlock-safe via the nested guard).
            performRefresh(byTimeExpiry = true)
        } else {
            // Normal case: trigger async refresh, return stale data, don't block the (possibly DB-holding) caller.
            triggerAsyncRefresh(byTimeExpiry = true)
        }
    }

    /**
     * "Wait until initially filled, otherwise consume the current data directly - even while a refresh is running."
     *
     * Semantics:
     * - Not yet filled (no [refresh] has ever completed, i.e. the startup window): waits until the first refresh
     *   succeeds - triggering it itself if necessary - so the caller reads a populated cache instead of an empty one.
     *   The wait is bounded by [maxRefreshDurationMs] so a permanently failing refresh (e.g. database down) can never
     *   block the request thread forever; after the bound it returns and the caller works with whatever is there.
     * - Already filled at least once: returns immediately and lets the caller consume the current data as is, even
     *   while a time-/invalidation-triggered refresh is running. A due refresh is only kicked off asynchronously
     *   (never blocking here); its result is picked up on a later call.
     *
     * Use this instead of [checkRefresh] on a read path whose cache-miss fallback is expensive per element - e.g.
     * [org.projectforge.business.fibu.AbstractRechnungCache.ensureRechnungInfo], called once per row while building an
     * invoice list: before the cache is filled, every miss would lazily load that invoice's positions and cost
     * assignments (an N+1 storm). Waiting once for the single bulk refresh is far cheaper, and after the initial fill
     * serving complete but possibly slightly stale data is fine - the running/next refresh reconciles it.
     *
     * Deadlock-safe: from within another cache's refresh (nested, [refreshDepthOnThisThread] > 0) it never waits on
     * this cache's lock (exactly like [checkRefresh]/[performRefresh]); it tries a refresh once and returns.
     */
    protected fun waitForInitialization() {
        checkStuckRefresh()
        if (everFilled) {
            // Filled at least once: never block. Only kick off a due refresh asynchronously and serve current data.
            if (isExpired) {
                triggerAsyncRefresh(byTimeExpiry = false)
            } else if (System.currentTimeMillis() - timeOfLastRefresh > expireTime) {
                triggerAsyncRefresh(byTimeExpiry = true)
            }
            return
        }
        if (refreshDepthOnThisThread.get() > 0) {
            // Nested cross-cache refresh: must not wait on this cache's lock. Try once, then serve whatever is there.
            performRefresh()
            return
        }
        // Not filled yet (startup): wait for the initial refresh (ours or a concurrent one) to complete, bounded so a
        // permanently failing refresh can't block the request thread forever.
        val deadline = System.currentTimeMillis() + maxRefreshDurationMs
        while (!everFilled) {
            performRefresh()
            if (everFilled || System.currentTimeMillis() >= deadline) {
                return
            }
            // A concurrent refresh is running (performRefresh returned without acquiring the lock, or its refresh()
            // failed): back off briefly instead of spinning, then re-check whether it filled the cache.
            try {
                Thread.sleep(INITIAL_FILL_RETRY_INTERVAL_MS)
            } catch (ex: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
        }
    }

    /**
     * Schedules an asynchronous [performRefresh] unless a refresh is already running (the running one will deliver
     * the fresh data). Never blocks the calling thread.
     */
    private fun triggerAsyncRefresh(byTimeExpiry: Boolean) {
        if (!refreshLock.isLocked) {
            refreshScope.launch {
                performRefresh(byTimeExpiry = byTimeExpiry)
            }
        }
    }

    /**
     * Detects a refresh that has been running abnormally long (longer than [MAX_REFRESH_DURATION_MS]) - e.g. a JDBC
     * call blocked on a database lock - which would otherwise hold [refreshLock] forever and keep the cache stale
     * until a restart. Reports it once and interrupts the refreshing thread to abort the blocked I/O: [refresh] catches
     * the resulting exception, releases the lock and leaves the cache expired, so the next [checkRefresh] schedules a
     * fresh refresh and the cache heals itself. Request threads keep serving stale data meanwhile.
     */
    private fun checkStuckRefresh() {
        if (!isRefreshInProgress || stuckRefreshReported) {
            return
        }
        val startMillis = refreshStartMillis
        if (startMillis < 0 || System.currentTimeMillis() - startMillis <= maxRefreshDurationMs) {
            return
        }
        stuckRefreshReported = true
        val thread = refreshingThread
        val runningSeconds = (System.currentTimeMillis() - startMillis) / 1000
        // Dump where the refresh is wedged (almost always a JDBC call blocked on a database lock - e.g. a cache
        // refresh that opened a second connection from within a still-open write transaction) so the production log
        // pinpoints the exact stack instead of only telling that "something" is stuck. Then interrupt it: refresh()
        // catches the resulting exception, releases the lock and leaves the cache expired, so it heals on the next
        // access without a restart.
        val stackTrace = thread?.stackTrace?.joinToString(separator = "") { "\n\tat $it" } ?: " <thread already gone>"
        log.error {
            "Refresh of cache ${this::class.simpleName} has been running for ${runningSeconds}s and seems to be " +
                    "stuck (thread=${thread?.name}, state=${thread?.state}). This is usually a JDBC call blocked on a " +
                    "database lock (e.g. a cache refresh opening a second connection from within an open write " +
                    "transaction). Interrupting it so the cache can refresh again without a restart. Stack trace of " +
                    "the stuck thread:$stackTrace"
        }
        thread?.interrupt()
    }

    /**
     * A [refresh] running longer than this (milliseconds) is considered stuck and gets interrupted, see
     * [checkStuckRefresh]. Overridable for tests; defaults to [MAX_REFRESH_DURATION_MS].
     */
    protected open val maxRefreshDurationMs: Long
        get() = MAX_REFRESH_DURATION_MS

    /**
     * Refreshes the cache, guarded by [refreshLock]: only one thread at a time may refresh this cache. All other
     * callers return immediately instead of starting a competing refresh - two concurrent refreshes would both build
     * their own data and the slower one would overwrite the newer result of the faster one (last writer wins).
     *
     * A nested call from the refresh thread itself (a refresh indirectly accessing this same cache) also returns
     * immediately: the outer refresh is already underway and re-entering it would recurse endlessly.
     *
     * A nested refresh of a _different_ cache (mutually dependent caches: AuftragsCache <-> AuftragsRechnungCache
     * access each other while refreshing) only ever tries the lock without waiting: if another thread already holds
     * it, this refresh would have to wait for it while itself holding a lock the other thread may be waiting for - a
     * lock-ordering deadlock. This exact cross-lock wait froze the order book and invoices in production until a
     * restart. Serving the current data instead is safe: the coupled-refresh listeners and expiry reconcile the
     * dependent caches afterwards. Without contention (the normal and single-threaded case) the lock is free and the
     * refresh runs synchronously as before.
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
        val nested = refreshDepthOnThisThread.get() > 0
        val locked = if (nested) {
            // Inside another cache's refresh (holding its lock): never wait for this cache's lock, that could deadlock.
            refreshLock.tryLock()
        } else {
            refreshLock.tryLock(MAX_WAIT_FOR_CONCURRENT_REFRESH_MS, TimeUnit.MILLISECONDS)
        }
        if (!locked) {
            log.warn { "Refresh of cache ${this::class.simpleName} is already in progress, using current data." }
            return
        }
        // Invariant for deadlock-freedom: from here until unlock this thread holds refreshLock, so it must count as
        // "refreshing" (depth > 0) for the whole region - including the onBeforeCacheRefresh listeners, which may
        // (indirectly) refresh a mutually dependent cache. While depth > 0, such a refresh never waits for the other
        // cache's lock (see the tryLock choice above), so a thread holding a lock never blocks on another lock and no
        // lock-ordering cycle can form.
        refreshDepthOnThisThread.set(refreshDepthOnThisThread.get() + 1)
        try {
            if (isUpToDate(byTimeExpiry, requiredInvalidation)) {
                // Another thread already refreshed while we waited for the lock.
                return
            }
            cacheListeners?.forEach { listener -> listener.onBeforeCacheRefresh() }
            try {
                isRefreshInProgress = true
                stuckRefreshReported = false
                refreshStartMillis = System.currentTimeMillis()
                refreshingThread = Thread.currentThread()
                val invalidation = invalidationCounter.get()
                this.timeOfLastRefresh = System.currentTimeMillis()
                try {
                    this.refresh()
                    // Mark the cache up-to-date only after a successful refresh. A failed or interrupted refresh (see
                    // checkStuckRefresh) leaves it expired, so the next checkRefresh triggers a new refresh and the
                    // cache heals itself without a restart.
                    this.refreshedInvalidation = invalidation
                    this.isExpired = false
                } catch (ex: Throwable) {
                    log.error(ex.message, ex)
                }
            } finally {
                isRefreshInProgress = false
                refreshingThread = null
                refreshStartMillis = -1
                // Clear a pending interrupt so the (pooled/coroutine) thread is not left interrupted, in case
                // checkStuckRefresh interrupted us but refresh() finished before observing it.
                Thread.interrupted()
            }
        } finally {
            refreshDepthOnThisThread.set(refreshDepthOnThisThread.get() - 1)
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
     * Runs read-only database work needed to keep this cache up to date (a [refresh], or an incremental update from a
     * [org.projectforge.framework.persistence.api.BaseDOModifiedListener] callback).
     *
     * If the current thread is already inside a write transaction, the block runs on that transaction's own
     * connection instead of a new, isolated one: opening a second connection here and querying from it would block
     * on the row/table locks the still-open transaction holds, while that transaction can never commit because this
     * same thread is parked waiting for the second connection - a single-thread self-deadlock across two connections.
     * It froze VacationDaoTest (VacationCache.afterInsertOrModify -> loadOtherReplacementIds) and the invoice caches,
     * and would freeze the same paths in production (only broken out of by the [checkStuckRefresh] watchdog after
     * [MAX_REFRESH_DURATION_MS], and not at all on a single-threaded path that never observes itself). Reusing the
     * transaction's connection sees its still-uncommitted changes - exactly what a cache update right after an
     * insert/update wants - and cannot deadlock.
     *
     * Outside a write transaction the work runs in its own isolated read-only context, exactly as a cache refresh
     * does normally (see [PfPersistenceService.runIsolatedReadOnly]). Note: [PfPersistenceService.runIsolatedReadOnly]
     * must NOT be changed to reuse the transaction globally - it is contractually isolated even inside a transaction
     * (see PfPersistenceServiceTest), so the safe choice is made here, where cache maintenance is the caller.
     */
    protected fun <T> runReadOnlyForCacheMaintenance(block: (context: PfPersistenceContext) -> T): T {
        val persistenceService = PfPersistenceService.instance
        return if (persistenceService.isTransactionActive()) {
            log.debug {
                "runReadOnlyForCacheMaintenance (${this::class.simpleName}): inside a write transaction, reusing its " +
                        "connection instead of opening a second one, to avoid a cross-connection self-deadlock."
            }
            persistenceService.runReadOnly(block)
        } else {
            persistenceService.runIsolatedReadOnly(block = block)
        }
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
         * How many cache refreshes the current thread is nested in (across all [AbstractCache] instances). Greater
         * than 0 means the thread is already refreshing some cache and holds its [refreshLock]; a refresh of another,
         * mutually dependent cache must then not wait for that cache's lock (deadlock), see [performRefresh].
         */
        private val refreshDepthOnThisThread = ThreadLocal.withInitial { 0 }

        /**
         * Time a caller waits for a refresh running in another thread before giving up and working with the data
         * currently held by the cache. Only relevant for callers which need fresh data (expired cache), so it must
         * cover a normal refresh duration, but must never block a request thread for long (it may hold a DB
         * connection).
         */
        private const val MAX_WAIT_FOR_CONCURRENT_REFRESH_MS = 10_000L

        /**
         * A [refresh] running longer than this is considered stuck (e.g. blocked on a database lock) and gets
         * interrupted by [checkStuckRefresh] so the cache can recover without a restart. Must be well above a normal
         * refresh duration to avoid interrupting a legitimately slow refresh.
         */
        private const val MAX_REFRESH_DURATION_MS = 120_000L

        /**
         * Back-off between attempts in [waitForInitialization] while another thread's initial refresh is still
         * running: short enough to pick up the fresh data promptly, long enough not to busy-spin.
         */
        private const val INITIAL_FILL_RETRY_INTERVAL_MS = 50L

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
