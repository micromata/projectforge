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

package org.projectforge.framework.integration

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class IntegrationTest {
    @Test
    fun `settings are resolved by id, built-in defaults and general default`() {
        val default = IntegrationSettings(connectTimeoutMs = 1000, responseTimeoutMs = 2000)
        val custom = IntegrationSettings(responseTimeoutMs = 3000)
        IntegrationConfig.resolve("ldap", default, custom).let {
            assertEquals(1000, it.connectTimeoutMs)
            assertEquals(3000, it.responseTimeoutMs)
            assertEquals(IntegrationConfig.FALLBACK.connectionRequestTimeoutMs, it.connectionRequestTimeoutMs)
        }
        // Built-in default of the gateway wins over the general default:
        assertEquals(120_000, IntegrationConfig.resolve(IntegrationConfig.GATEWAY, default, null).responseTimeoutMs)
        // But not over the configuration of the id:
        assertEquals(3000, IntegrationConfig.resolve(IntegrationConfig.GATEWAY, default, custom).responseTimeoutMs)
        assertEquals(IntegrationConfig.FALLBACK, IntegrationConfig.resolve("unknown", null, null))
        assertEquals(10L * 1024 * 1024, IntegrationConfig.get(IntegrationConfig.ICAL).maxResponseBytes)
    }

    @Test
    fun `read limited stream`() {
        val data = ByteArray(20_000) { it.toByte() }
        assertArrayEquals(data, PooledHttpClients.readLimited(ByteArrayInputStream(data), 20_000))
        assertArrayEquals(data, PooledHttpClients.readLimited(ByteArrayInputStream(data), 0))
        assertThrows<PooledHttpClients.ResponseTooLargeException> {
            PooledHttpClients.readLimited(ByteArrayInputStream(data), 19_999)
        }
    }

    @Test
    fun `single run executor coalesces triggers`() {
        val executor = SingleRunExecutor("test-sync")
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val counter = AtomicInteger()
        assertTrue(executor.trigger {
            started.countDown()
            release.await(5, TimeUnit.SECONDS)
            counter.incrementAndGet()
        })
        assertTrue(started.await(5, TimeUnit.SECONDS))
        // First one is running, the second one waits, the others are dropped:
        assertTrue(executor.trigger { counter.incrementAndGet() })
        assertFalse(executor.trigger { counter.incrementAndGet() })
        assertFalse(executor.trigger { counter.incrementAndGet() })
        assertTrue(executor.busy)
        release.countDown()
        val waitUntil = System.currentTimeMillis() + 5000
        while (executor.busy && System.currentTimeMillis() < waitUntil) {
            Thread.sleep(10)
        }
        assertEquals(2, counter.get())
        // Exceptions don't kill the executor:
        executor.trigger { throw IllegalStateException("test") }
        val done = CountDownLatch(1)
        val waitUntil2 = System.currentTimeMillis() + 5000
        while (!executor.trigger { done.countDown() } && System.currentTimeMillis() < waitUntil2) {
            Thread.sleep(10)
        }
        assertTrue(done.await(5, TimeUnit.SECONDS))
        executor.shutdown()
    }

    @Test
    fun `sync stats record runs and steps`() {
        val stats = SyncStats("test")
        assertEquals("no runs yet", stats.summary())
        stats.execute("full") { run ->
            run.step("users") { it.updated = 3; it.created = 1 }
            run.step("groups") { it.unchanged = 2 }
        }
        assertEquals(1, stats.successfulRuns)
        assertEquals(SyncStats.Status.SUCCESS, stats.lastStatus)
        assertEquals(listOf("users", "groups"), stats.lastSteps.map { it.name })
        assertEquals("created=1, updated=3", stats.lastSteps[0].counts.toString())

        stats.execute { run -> run.step("users") { it.errors = 2 } }
        assertEquals(1, stats.runsWithErrors)
        assertEquals("2 errors", stats.lastError)

        assertThrows<SocketTimeoutException> {
            stats.execute { run -> run.step("users") { throw SocketTimeoutException("Read timed out") } }
        }
        assertEquals(1, stats.abortedRuns)
        assertEquals(1, stats.timeouts)
        assertEquals(3, stats.runs)
        assertTrue(stats.summary().contains("timeouts=1"))

        stats.startRun().skip("disabled")
        assertEquals(3, stats.runs)
    }

    @Test
    fun `classify errors`() {
        assertTrue(IntegrationErrors.isTimeout(RuntimeException(SocketTimeoutException())))
        assertFalse(IntegrationErrors.isTimeout(RuntimeException(ConnectException())))
        assertTrue(IntegrationErrors.isConnectionError(RuntimeException(ConnectException())))
        assertFalse(IntegrationErrors.isConnectionError(IllegalStateException()))
    }
}
