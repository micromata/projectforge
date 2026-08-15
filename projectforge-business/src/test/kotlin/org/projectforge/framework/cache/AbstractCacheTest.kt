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
        fun accessData() {
            checkRefresh()
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

    private fun awaitRefreshCount(cache: TestCache, expected: Int) {
        val timeout = System.currentTimeMillis() + 20_000
        while (cache.refreshCount.get() < expected && System.currentTimeMillis() < timeout) {
            Thread.sleep(20)
        }
        Assertions.assertEquals(expected, cache.refreshCount.get(), "Asynchronous refresh was done.")
    }
}
