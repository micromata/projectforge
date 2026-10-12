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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests the refresh contract of [AbstractCache]: a cache must never be refreshed by two threads at the same time
 * (otherwise the slower refresh overwrites the result of the faster one with older data), nested access from inside a
 * refresh must not recurse, and a refresh caused only by expiration time must not block the calling thread.
 *
 * @author Kai Reinhard
 */
class AbstractCacheTest {
    /**
     * @param expireTime in milliseconds.
     */
    private class TestCache(expireTime: Long = 60 * TICKS_PER_MINUTE) : AbstractCache(expireTime) {
        val refreshCount = AtomicInteger(0)

        /** Highest number of threads seen inside [refresh] at the same time. Must never exceed 1. */
        val maxConcurrentRefreshes = AtomicInteger(0)

        private val currentRefreshes = AtomicInteger(0)

        /** Duration of a single [refresh] in milliseconds. */
        var refreshDurationMs: Long = 0

        /** Executed inside [refresh] (e. g. for accessing this cache while it is refreshing). */
        var refreshAction: (() -> Unit)? = null

        var throwExceptionOnRefresh = false

        /** Overrides [maxRefreshDurationMs] for the stuck-refresh test; null keeps the production default. */
        var maxRefreshDurationMsOverride: Long? = null

        override val maxRefreshDurationMs: Long
            get() = maxRefreshDurationMsOverride ?: super.maxRefreshDurationMs

        val events: MutableList<String> = Collections.synchronizedList(mutableListOf())

        override fun refresh() {
            events.add("refresh")
            val concurrent = currentRefreshes.incrementAndGet()
            maxConcurrentRefreshes.updateAndGet { max -> maxOf(max, concurrent) }
            try {
                refreshCount.incrementAndGet()
                refreshAction?.invoke()
                if (refreshDurationMs > 0) {
                    Thread.sleep(refreshDurationMs)
                }
                if (throwExceptionOnRefresh) {
                    throw IllegalStateException("Exception for test purposes.")
                }
            } finally {
                currentRefreshes.decrementAndGet()
            }
        }

        /** [checkRefresh] is protected, so the tests need this bridge. */
        fun accessData(waitForRefresh: Boolean = false) {
            checkRefresh(waitForRefresh)
        }
    }

    @Test
    fun `cache is refreshed on first access only`() {
        val cache = TestCache()
        Assertions.assertFalse(cache.initialized, "Cache isn't initialized before first access.")
        cache.accessData()
        Assertions.assertTrue(cache.initialized)
        Assertions.assertEquals(1, cache.refreshCount.get(), "First access refreshes the cache.")
        cache.accessData()
        cache.accessData()
        Assertions.assertEquals(1, cache.refreshCount.get(), "Cache isn't expired, so no further refresh.")
        cache.setExpired()
        cache.accessData()
        Assertions.assertEquals(2, cache.refreshCount.get(), "Expired cache is refreshed on next access.")
        cache.forceReload()
        Assertions.assertEquals(3, cache.refreshCount.get(), "forceReload refreshes the cache synchronously.")
    }

    @Test
    @Timeout(30)
    fun `concurrent refreshes of the same cache are serialized`() {
        val cache = TestCache()
        cache.refreshDurationMs = 50
        val numberOfThreads = 8
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(numberOfThreads)
        try {
            val futures = (1..numberOfThreads).map {
                executor.submit {
                    start.await()
                    cache.forceReload()
                }
            }
            start.countDown()
            futures.forEach { it.get(20, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
        // This is the regression: two threads inside refresh() at the same time both build their own data and the
        // slower one overwrites the newer result of the faster one (last writer wins).
        Assertions.assertEquals(
            1, cache.maxConcurrentRefreshes.get(),
            "Only one thread at a time may refresh the cache."
        )
        Assertions.assertTrue(cache.refreshCount.get() >= 1, "At least one refresh was done.")
        Assertions.assertTrue(
            cache.refreshCount.get() <= numberOfThreads,
            "No refresh in addition to those requested: ${cache.refreshCount.get()}"
        )
        Assertions.assertFalse(cache.isRefreshInProgress, "No refresh is running anymore.")
    }

    @Test
    @Timeout(30)
    fun `refresh accessing its own cache doesn't start a nested refresh`() {
        // Some caches use other caches, which use them again (AuftragsCache <-> AuftragsRechnungCache). Such a nested
        // access must not refresh the cache again (endless recursion), the outer refresh is doing the work.
        val cache = TestCache()
        cache.refreshAction = {
            Assertions.assertTrue(cache.isRefreshInProgress, "Refresh is running.")
            cache.accessData()  // Nested access of the cache currently refreshing.
            cache.setExpired()
            cache.accessData()
            cache.forceReload()
        }
        cache.accessData()
        Assertions.assertEquals(1, cache.refreshCount.get(), "Nested accesses don't refresh the cache again.")
        Assertions.assertFalse(cache.isRefreshInProgress)
    }

    @Test
    @Timeout(30)
    fun `refresh caused by expire time doesn't block the caller`() {
        val cache = TestCache(expireTime = 100)
        cache.accessData() // Initial (synchronous) refresh.
        Assertions.assertEquals(1, cache.refreshCount.get())
        cache.refreshDurationMs = 2000
        Thread.sleep(200) // Now the cache is expired by time (but not invalidated), so stale data may be used.
        val begin = System.currentTimeMillis()
        cache.accessData()
        val duration = System.currentTimeMillis() - begin
        Assertions.assertTrue(
            duration < 1000,
            "Refresh by expire time is done asynchronously, the caller must not wait for it (waited ${duration}ms)."
        )
        awaitRefreshCount(cache, 2)
        Assertions.assertEquals(1, cache.maxConcurrentRefreshes.get())
    }

    @Test
    @Timeout(30)
    fun `listeners are called before and after the refresh`() {
        val cache = TestCache()
        cache.register(object : CacheListener {
            override fun onBeforeCacheRefresh() {
                cache.events.add("before")
                Assertions.assertFalse(cache.isRefreshInProgress, "Refresh not yet started.")
            }

            override fun onAfterCacheRefresh() {
                cache.events.add("after")
                Assertions.assertFalse(cache.isRefreshInProgress, "Refresh already done.")
            }
        })
        cache.accessData()
        Assertions.assertEquals(listOf("before", "refresh", "after"), cache.events)
    }

    @Test
    @Timeout(30)
    fun `listener may reload the cache after a refresh`() {
        // AuftragsRechnungCache does this: after AuftragsCache is refreshed, the order sums have to be re-calculated on
        // top of the invoice data. onAfterCacheRefresh is therefore called with the refresh lock released.
        val cache = TestCache()
        val reloads = AtomicInteger(0)
        cache.register(object : CacheListener {
            override fun onAfterCacheRefresh() {
                if (reloads.incrementAndGet() > 1) {
                    return // Only one coupled reload, otherwise this would be an endless ping-pong.
                }
                cache.forceReload()
            }
        })
        cache.accessData()
        Assertions.assertEquals(2, cache.refreshCount.get(), "Listener triggered a second refresh.")
        Assertions.assertEquals(1, cache.maxConcurrentRefreshes.get())
    }

    @Test
    @Timeout(30)
    fun `listener refreshing the cache during a refresh doesn't start a nested refresh`() {
        // onBeforeCacheRefresh is called with the lock held (by the refreshing thread), so a listener trying to reload
        // this cache must not start a second refresh (see AuftragsRechnungCache.auftragsCacheListener).
        val cache = TestCache()
        cache.register(object : CacheListener {
            override fun onBeforeCacheRefresh() {
                cache.forceReload()
            }
        })
        cache.accessData()
        Assertions.assertEquals(1, cache.refreshCount.get())
    }

    @Test
    @Timeout(30)
    fun `exceptions while refreshing don't escape and don't break the cache`() {
        val cache = TestCache()
        cache.throwExceptionOnRefresh = true
        cache.accessData() // Exception is only logged.
        Assertions.assertEquals(1, cache.refreshCount.get())
        Assertions.assertFalse(cache.isRefreshInProgress, "Flag is reset, even if refresh failed.")
        cache.throwExceptionOnRefresh = false
        cache.forceReload()
        Assertions.assertEquals(2, cache.refreshCount.get(), "Cache is still usable after a failed refresh.")
    }

    @Test
    @Timeout(30)
    fun `reader of an invalidated cache waits for the running refresh instead of refreshing again`() {
        val cache = TestCache()
        cache.accessData()
        cache.refreshDurationMs = 500
        val refreshThread = Thread { cache.forceReload() }
        refreshThread.start()
        while (!cache.isRefreshInProgress) {
            Thread.sleep(10) // Wait for the refresh to start.
        }
        cache.accessData() // The cache is invalidated, so this caller needs the fresh data of the running refresh.
        Assertions.assertFalse(cache.isRefreshInProgress, "Reader returns not until the refresh is done.")
        Assertions.assertEquals(2, cache.refreshCount.get(), "The reader doesn't refresh the cache a second time.")
        refreshThread.join(20_000)
        Assertions.assertEquals(1, cache.maxConcurrentRefreshes.get())
    }

    @Test
    @Timeout(30)
    fun `mutually dependent caches don't deadlock a caller`() {
        // Reproduces the AuftragsCache <-> AuftragsRechnungCache lock inversion: two threads each refresh one cache,
        // and each refresh accesses the other cache. A refresh holding one cache's lock must not wait for the other's
        // lock (it uses a non-blocking tryLock while nested, see AbstractCache.performRefresh), so neither thread
        // deadlocks - it works with the current data instead.
        val cacheA = TestCache()
        val cacheB = TestCache()
        val bothRefreshing = CountDownLatch(2)
        cacheA.refreshAction = {
            bothRefreshing.countDown()
            bothRefreshing.await(20, TimeUnit.SECONDS) // Both locks are held now: the deadlock window.
            cacheB.accessData()
        }
        cacheB.refreshAction = {
            bothRefreshing.countDown()
            bothRefreshing.await(20, TimeUnit.SECONDS)
            cacheA.accessData()
        }
        val threadA = Thread { cacheA.forceReload() }
        val threadB = Thread { cacheB.forceReload() }
        val begin = System.currentTimeMillis()
        threadA.start()
        threadB.start()
        threadA.join(20_000)
        threadB.join(20_000)
        val duration = System.currentTimeMillis() - begin
        Assertions.assertFalse(threadA.isAlive || threadB.isAlive, "The mutually dependent refreshes didn't deadlock.")
        // With the bug the cross-cache access blocks and only recovers after the 10s lock timeout. Non-blocking nested
        // access must return at once, so the whole thing finishes well below that timeout.
        Assertions.assertTrue(duration < 5_000, "No blocking cross-lock wait occurred (took ${duration}ms).")
    }

    @Test
    @Timeout(30)
    fun `mutually dependent caches don't deadlock via onBeforeCacheRefresh listeners`() {
        // The AuftragsCache.auftragsCacheListener reloads AuftragsRechnungCache from onBeforeCacheRefresh, i.e. while
        // the refreshLock is already held. That cross-cache access must count as nested (non-blocking) too, otherwise
        // two threads can deadlock in each other's onBeforeCacheRefresh. This reproduces exactly that window.
        val cacheA = TestCache()
        val cacheB = TestCache()
        val bothInBeforeRefresh = CountDownLatch(2)
        cacheA.register(object : CacheListener {
            override fun onBeforeCacheRefresh() {
                bothInBeforeRefresh.countDown()
                bothInBeforeRefresh.await(20, TimeUnit.SECONDS) // Both hold their own lock now: the deadlock window.
                cacheB.accessData()
            }
        })
        cacheB.register(object : CacheListener {
            override fun onBeforeCacheRefresh() {
                bothInBeforeRefresh.countDown()
                bothInBeforeRefresh.await(20, TimeUnit.SECONDS)
                cacheA.accessData()
            }
        })
        val threadA = Thread { cacheA.forceReload() }
        val threadB = Thread { cacheB.forceReload() }
        val begin = System.currentTimeMillis()
        threadA.start()
        threadB.start()
        threadA.join(20_000)
        threadB.join(20_000)
        val duration = System.currentTimeMillis() - begin
        Assertions.assertFalse(
            threadA.isAlive || threadB.isAlive,
            "onBeforeCacheRefresh cross-cache access must not deadlock."
        )
        Assertions.assertTrue(duration < 5_000, "No blocking cross-lock wait occurred (took ${duration}ms).")
    }

    @Test
    @Timeout(30)
    fun `a stuck refresh is interrupted and the cache heals itself`() {
        val cache = TestCache()
        cache.accessData() // Initial refresh.
        Assertions.assertEquals(1, cache.refreshCount.get())
        cache.maxRefreshDurationMsOverride = 200
        cache.refreshDurationMs = 60_000 // The next refresh blocks, simulating a wedged JDBC call.
        cache.setExpired()
        val stuckThread = Thread { cache.accessData() } // Blocks in the stuck refresh.
        stuckThread.start()
        while (!cache.isRefreshInProgress) {
            Thread.sleep(10)
        }
        Thread.sleep(400) // Let the refresh exceed maxRefreshDurationMs.
        cache.refreshDurationMs = 0 // The healing refresh is fast.
        // This access detects the stuck refresh, interrupts it, then refreshes the cache (which heals itself, because
        // the interrupted refresh left the cache expired).
        cache.accessData()
        stuckThread.join(20_000)
        Assertions.assertFalse(stuckThread.isAlive, "The stuck refresh was interrupted.")
        Assertions.assertFalse(cache.isRefreshInProgress, "The cache refreshed again after the stuck refresh.")
        Assertions.assertEquals(3, cache.refreshCount.get(), "1 initial + interrupted + healed refresh.")
        Assertions.assertEquals(1, cache.maxConcurrentRefreshes.get(), "The stuck and healing refresh didn't overlap.")
    }

    @Test
    @Timeout(30)
    fun `a caller may wait synchronously for a refresh caused by expire time`() {
        // Opt-in synchronous refresh (checkRefresh(waitForRefresh = true)): unlike the default, the caller must not
        // get stale data but block until the fresh data is there. For tests and the rare read-after-write caller.
        val cache = TestCache(expireTime = 100)
        cache.accessData() // Initial refresh.
        Assertions.assertEquals(1, cache.refreshCount.get())
        cache.refreshDurationMs = 300
        Thread.sleep(200) // Expired by time only (not invalidated), so the default would serve stale data.
        val begin = System.currentTimeMillis()
        cache.accessData(waitForRefresh = true)
        val duration = System.currentTimeMillis() - begin
        Assertions.assertEquals(2, cache.refreshCount.get(), "The caller waited for the synchronous refresh.")
        Assertions.assertFalse(cache.isRefreshInProgress, "The refresh completed before the caller returned.")
        Assertions.assertTrue(
            duration >= 300,
            "The caller really waited for the ${300}ms refresh (waited ${duration}ms)."
        )
        Assertions.assertEquals(1, cache.maxConcurrentRefreshes.get())
    }

    @Test
    @Timeout(30)
    fun `synchronous wait for a refresh doesn't deadlock mutually dependent caches`() {
        // The synchronous wait (waitForRefresh = true) must stay deadlock-safe: a refresh reaching into the other,
        // mutually dependent cache while waiting synchronously must still use the non-blocking nested tryLock.
        val cacheA = TestCache(expireTime = 100)
        val cacheB = TestCache(expireTime = 100)
        val bothRefreshing = CountDownLatch(2)
        cacheA.refreshAction = {
            bothRefreshing.countDown()
            bothRefreshing.await(20, TimeUnit.SECONDS)
            cacheB.accessData(waitForRefresh = true)
        }
        cacheB.refreshAction = {
            bothRefreshing.countDown()
            bothRefreshing.await(20, TimeUnit.SECONDS)
            cacheA.accessData(waitForRefresh = true)
        }
        val threadA = Thread { cacheA.forceReload() }
        val threadB = Thread { cacheB.forceReload() }
        val begin = System.currentTimeMillis()
        threadA.start()
        threadB.start()
        threadA.join(20_000)
        threadB.join(20_000)
        val duration = System.currentTimeMillis() - begin
        Assertions.assertFalse(threadA.isAlive || threadB.isAlive, "The synchronous cross-cache waits didn't deadlock.")
        Assertions.assertTrue(duration < 5_000, "No blocking cross-lock wait occurred (took ${duration}ms).")
    }

    @Test
    @Timeout(30)
    fun `readers of a valid cache aren't blocked by a refresh running by expire time`() {
        val cache = TestCache(expireTime = 100)
        cache.accessData()
        cache.refreshDurationMs = 1000
        Thread.sleep(200) // Expired by time only: the async refresh returns stale data to the callers.
        cache.accessData() // Triggers the asynchronous refresh.
        while (!cache.isRefreshInProgress) {
            Thread.sleep(10)
        }
        val begin = System.currentTimeMillis()
        cache.accessData()
        val duration = System.currentTimeMillis() - begin
        Assertions.assertTrue(duration < 500, "Reader wasn't blocked by the running refresh (waited ${duration}ms).")
        awaitRefreshCount(cache, 2)
        Assertions.assertEquals(1, cache.maxConcurrentRefreshes.get())
    }

    @Test
    @Timeout(30)
    fun `status reports successful and failed refreshes`() {
        val cache = TestCache()
        Assertions.assertTrue(AbstractCache.allCaches().contains(cache), "Every cache registers itself.")
        cache.status().let {
            Assertions.assertEquals("TestCache", it.name)
            Assertions.assertFalse(it.everFilled)
            Assertions.assertNull(it.lastRefreshStart)
            Assertions.assertEquals(0, it.refreshCount)
        }
        cache.accessData()
        cache.status().let {
            Assertions.assertTrue(it.everFilled)
            Assertions.assertFalse(it.expired)
            Assertions.assertNotNull(it.lastRefreshStart)
            Assertions.assertNotNull(it.lastSuccess)
            Assertions.assertNotNull(it.lastRefreshDurationMs)
            Assertions.assertNull(it.lastRefreshError)
            Assertions.assertEquals(1, it.refreshCount)
            Assertions.assertEquals(CacheState.OK, CacheAdminService.stateOf(it))
        }
        cache.throwExceptionOnRefresh = true
        cache.forceReload()
        cache.status().let {
            Assertions.assertEquals("Exception for test purposes.", it.lastRefreshError)
            Assertions.assertTrue(it.expired, "A failed refresh leaves the cache expired.")
            Assertions.assertEquals(2, it.refreshCount)
            Assertions.assertEquals(1, it.failureCount)
            Assertions.assertEquals(CacheState.FAILED, CacheAdminService.stateOf(it))
        }
        cache.throwExceptionOnRefresh = false
        cache.forceReload()
        Assertions.assertNull(cache.status().lastRefreshError, "A successful refresh clears the error.")
    }

    @Test
    @Timeout(30)
    fun `the watchdog interrupts a stuck refresh without accessing the cache`() {
        val cache = TestCache()
        cache.accessData() // Initial refresh.
        cache.maxRefreshDurationMsOverride = 200
        cache.refreshDurationMs = 60_000 // The next refresh blocks, simulating a wedged JDBC call.
        cache.forceReloadAsync()
        while (!cache.isRefreshInProgress) {
            Thread.sleep(10)
        }
        Assertions.assertEquals(CacheState.REFRESHING, CacheAdminService.stateOf(cache.status()))
        Thread.sleep(400) // Let the refresh exceed maxRefreshDurationMs.
        cache.status().let {
            Assertions.assertTrue(it.stuck)
            Assertions.assertNotNull(it.refreshRunningSince)
            Assertions.assertEquals(CacheState.STUCK, CacheAdminService.stateOf(it))
        }
        cache.checkStuck()
        while (cache.isRefreshInProgress) {
            Thread.sleep(10)
        }
        cache.status().let {
            Assertions.assertFalse(it.stuck)
            Assertions.assertTrue(it.expired, "The interrupted refresh left the cache expired, it heals on next access.")
            Assertions.assertEquals(1, it.failureCount)
        }
    }

    @Test
    @Timeout(30)
    fun `a terminated refresh ignoring the interrupt is abandoned and the cache refreshes again at once`() {
        val cache = TestCache()
        cache.accessData() // Initial refresh.
        Assertions.assertFalse(cache.terminateRefresh("admin"), "No refresh running.")
        val block = AtomicBoolean(true)
        val release = AtomicBoolean(false)
        cache.refreshAction = {
            if (block.getAndSet(false)) {
                // Simulates a JDBC call not reacting to the interrupt.
                while (!release.get()) {
                    try {
                        Thread.sleep(10)
                    } catch (_: InterruptedException) {
                        // Ignored.
                    }
                }
            }
        }
        // A pooled thread (as request and coroutine threads are): it stays alive after the abandoned refresh.
        val pool = Executors.newSingleThreadExecutor()
        val zombie = pool.submit { cache.forceReload() }
        while (!cache.isRefreshInProgress) {
            Thread.sleep(10)
        }
        Assertions.assertTrue(cache.terminateRefresh("admin"))
        cache.status().let {
            Assertions.assertFalse(it.refreshInProgress)
            Assertions.assertTrue(it.expired, "The next access refreshes the cache.")
            Assertions.assertEquals(1, it.abandonedThreads)
            Assertions.assertEquals(1, it.failureCount)
            Assertions.assertTrue(it.lastRefreshError!!.contains("Terminated by admin"))
        }
        // A new refresh doesn't wait for the hanging one: the lock was replaced.
        val begin = System.currentTimeMillis()
        cache.forceReload()
        Assertions.assertTrue(System.currentTimeMillis() - begin < 5_000, "Didn't wait for the abandoned refresh.")
        Assertions.assertEquals(3, cache.refreshCount.get(), "1 initial + abandoned + new refresh.")
        cache.status().let {
            Assertions.assertFalse(it.expired)
            Assertions.assertNull(it.lastRefreshError)
            Assertions.assertFalse(it.refreshInProgress)
        }
        // The abandoned refresh returns at last: it doesn't touch the state, but expires the cache again, as its data
        // may be older than the new refresh's.
        release.set(true)
        zombie.get(20, TimeUnit.SECONDS)
        cache.status().let {
            Assertions.assertTrue(it.expired)
            Assertions.assertFalse(it.refreshInProgress)
            Assertions.assertEquals(0, it.abandonedThreads, "The abandoned refresh is over, the pooled thread lives on.")
            Assertions.assertEquals(3L, it.refreshCount, "The abandoned refresh wasn't counted twice.")
        }
        // The pooled thread of the abandoned refresh may refresh the cache again.
        pool.submit { cache.accessData() }.get(20, TimeUnit.SECONDS)
        Assertions.assertEquals(4, cache.refreshCount.get(), "The cache heals on the next access.")
        Assertions.assertFalse(cache.status().expired)
        pool.shutdown()
    }

    @Test
    @Timeout(30)
    fun `a waiter on the replaced lock doesn't refresh beside the new refresh`() {
        val cache = TestCache()
        cache.accessData() // Initial refresh.
        val refreshes = TerminateScenario(cache)
        val pool = Executors.newFixedThreadPool(3)
        val zombie = pool.submit { cache.forceReload() }
        while (!cache.isRefreshInProgress) {
            Thread.sleep(10)
        }
        // Waits for the lock of the hanging refresh (the lock replaced by terminateRefresh).
        val waiter = pool.submit { cache.forceReload() }
        Thread.sleep(200)
        Assertions.assertTrue(cache.terminateRefresh("admin"))
        val newRefresh = pool.submit { cache.forceReload() }
        Assertions.assertTrue(refreshes.newRefreshStarted.await(10, TimeUnit.SECONDS))
        // The terminated refresh returns and releases the old lock: the waiter gets it.
        refreshes.releaseZombie.set(true)
        zombie.get(10, TimeUnit.SECONDS)
        Thread.sleep(300)
        Assertions.assertEquals(
            3, cache.refreshCount.get(),
            "1 initial + terminated + new refresh: the waiter didn't refresh beside the new refresh.",
        )
        refreshes.releaseNewRefresh.countDown()
        newRefresh.get(10, TimeUnit.SECONDS)
        waiter.get(20, TimeUnit.SECONDS)
        pool.shutdown()
    }

    @Test
    @Timeout(30)
    fun `the expiry by a returning terminated refresh isn't lost by a refresh running meanwhile`() {
        val cache = TestCache()
        cache.accessData() // Initial refresh.
        val refreshes = TerminateScenario(cache)
        val pool = Executors.newFixedThreadPool(2)
        val zombie = pool.submit { cache.forceReload() }
        while (!cache.isRefreshInProgress) {
            Thread.sleep(10)
        }
        Assertions.assertTrue(cache.terminateRefresh("admin"))
        val newRefresh = pool.submit { cache.forceReload() }
        Assertions.assertTrue(refreshes.newRefreshStarted.await(10, TimeUnit.SECONDS))
        // The terminated refresh returns while the new one runs: its (older) data may have overwritten the new data.
        refreshes.releaseZombie.set(true)
        zombie.get(10, TimeUnit.SECONDS)
        refreshes.releaseNewRefresh.countDown()
        newRefresh.get(10, TimeUnit.SECONDS)
        Assertions.assertTrue(cache.status().expired, "The new refresh didn't mark the cache up-to-date.")
        cache.accessData()
        Assertions.assertEquals(4, cache.refreshCount.get(), "The cache heals on the next access.")
        Assertions.assertFalse(cache.status().expired)
        pool.shutdown()
    }

    /**
     * The 2nd refresh of the cache hangs ignoring the interrupt until [releaseZombie] (to be terminated), the 3rd (the
     * new one after the termination) blocks until [releaseNewRefresh].
     */
    private class TerminateScenario(cache: TestCache) {
        val releaseZombie = AtomicBoolean(false)
        val newRefreshStarted = CountDownLatch(1)
        val releaseNewRefresh = CountDownLatch(1)

        init {
            cache.refreshAction = {
                when (cache.refreshCount.get()) {
                    2 -> while (!releaseZombie.get()) {
                        try {
                            Thread.sleep(10)
                        } catch (_: InterruptedException) {
                            // Ignored, simulates a JDBC call not reacting to the interrupt.
                        }
                    }

                    3 -> {
                        newRefreshStarted.countDown()
                        releaseNewRefresh.await(20, TimeUnit.SECONDS)
                    }
                }
            }
        }
    }

    private fun awaitRefreshCount(cache: TestCache, expected: Int) {
        val timeout = System.currentTimeMillis() + 20_000
        while (cache.refreshCount.get() < expected && System.currentTimeMillis() < timeout) {
            Thread.sleep(20)
        }
        Assertions.assertEquals(expected, cache.refreshCount.get(), "Asynchronous refresh was done.")
    }
}
