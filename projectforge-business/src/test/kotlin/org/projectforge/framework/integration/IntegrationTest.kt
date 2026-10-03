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
    fun `single run executor runs the newest waiting job`() {
        val executor = SingleRunExecutor("test-sync")
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executed = java.util.Collections.synchronizedList(mutableListOf<Int>())
        assertTrue(executor.trigger {
            started.countDown()
            release.await(5, TimeUnit.SECONDS)
            executed.add(1)
        })
        assertTrue(started.await(5, TimeUnit.SECONDS))
        // First one is running, the second one waits and is replaced by the newer ones:
        assertTrue(executor.trigger { executed.add(2) })
        assertFalse(executor.trigger { executed.add(3) })
        assertFalse(executor.trigger { executed.add(4) })
        assertTrue(executor.busy)
        release.countDown()
        waitUntilIdle(executor)
        assertEquals(listOf(1, 4), executed)
        // Exceptions don't kill the executor:
        executor.trigger { throw IllegalStateException("test") }
        executor.trigger { executed.add(5) }
        waitUntilIdle(executor)
        assertEquals(5, executed.last())
        executor.shutdown()
    }

    private fun waitUntilIdle(executor: SingleRunExecutor) {
        val waitUntil = System.currentTimeMillis() + 5000
        while (executor.busy && System.currentTimeMillis() < waitUntil) {
            Thread.sleep(10)
        }
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
    fun `retry backoff gets less frequent`() {
        val min = 60_000L
        val hour = 60 * min
        val day = 24 * hour
        val backoff = RetryBackoff(
            listOf(RetryBackoff.Phase(15 * min, 45 * min), RetryBackoff.Phase(hour, day)), day,
        )
        val start = 1_000_000L
        // Follow the schedule from the first failure on:
        val attempts = generateSequence(start) { backoff.nextAttempt(start, it) }.take(30).map { (it - start) / min }.toList()
        // 3 times every 15 minutes, then hourly:
        assertEquals(listOf(0L, 15, 30, 45, 105, 165, 225), attempts.take(7))
        assertEquals(45 + 23 * 60L, attempts[26]) // Last hourly retry within 24 h after the first failure,
        assertEquals(listOf(attempts[26] + 24 * 60, attempts[26] + 48 * 60), attempts.subList(27, 29)) // then daily.
        // A late attempt continues with the next slot:
        assertEquals(start + 105 * min, backoff.nextAttempt(start, start + 61 * min))
        val lastHourly = start + (45 + 23 * 60) * min
        assertEquals(lastHourly + 4 * day, backoff.nextAttempt(start, start + 4 * day + 3 * hour))
        // Unknown last attempt (restart): no immediate retry, but the next slot.
        assertFalse(backoff.isDue(start, null, start + 10 * day + 5 * hour))
        assertEquals(lastHourly + 10 * day, backoff.nextAttempt(start, null, start + 10 * day + 5 * hour))
        assertTrue(backoff.isDue(start, start + 10 * day + 5 * hour, lastHourly + 10 * day))
        // The tolerance catches slots slightly after the tick of a periodic job:
        val tolerant = RetryBackoff(listOf(RetryBackoff.Phase(15 * min, 45 * min)), day, 5 * min)
        assertFalse(backoff.isDue(start, start + 3000, start + 15 * min - 3000))
        assertTrue(tolerant.isDue(start, start + 3000, start + 15 * min - 3000))
    }

    @Test
    fun `classify errors`() {
        assertTrue(IntegrationErrors.isTimeout(RuntimeException(SocketTimeoutException())))
        assertFalse(IntegrationErrors.isTimeout(RuntimeException(ConnectException())))
        assertTrue(IntegrationErrors.isConnectionError(RuntimeException(ConnectException())))
        assertFalse(IntegrationErrors.isConnectionError(IllegalStateException()))
    }
}
