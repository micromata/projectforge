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


package org.projectforge.framework.scheduling

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerSchedule
import org.projectforge.common.scheduling.SchedulerTrigger
import org.springframework.core.env.StandardEnvironment
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SchedulerJobRegistryTest {
    private val registry = SchedulerJobRegistry(StandardEnvironment())
    private val sunk = CopyOnWriteArrayList<SchedulerRunRecord>()

    init {
        registry.sink = SchedulerRunSink { _, record -> sunk.add(record) }
    }

    @Test
    fun `runs are measured, exceptions caught and failures recorded`() {
        val job = definition("test.run")
        var time = 1_000L
        registry.clock = { time }
        registry.run(job) { time += 500 }
        registry.run(job) { throw IllegalStateException("broken") }
        registry.run(job) { run ->
            run.note("a note")
            run.fail("failed")
            run.fail("failed") // Not repeated.
            run.fail("again")
        }
        val runs = registry.get(job.id)!!.snapshot().recentRuns
        Assertions.assertEquals(3, runs.size)
        runs[2].let {
            Assertions.assertEquals(SchedulerRunStatus.SUCCESS, it.status)
            Assertions.assertEquals(1_000L, it.start)
            Assertions.assertEquals(500L, it.durationMs)
            Assertions.assertEquals(SchedulerTrigger.SCHEDULED, it.trigger)
        }
        runs[1].let {
            Assertions.assertEquals(SchedulerRunStatus.ERROR, it.status)
            Assertions.assertEquals("broken", it.errorMessage)
            Assertions.assertTrue(it.stackExcerpt!!.contains("IllegalStateException"))
        }
        runs[0].let {
            Assertions.assertEquals(SchedulerRunStatus.ERROR, it.status)
            Assertions.assertEquals("failed; again", it.errorMessage)
            Assertions.assertNull(it.stackExcerpt)
            Assertions.assertEquals(listOf("a note"), it.notes)
        }
        val snapshot = registry.get(job.id)!!.snapshot()
        Assertions.assertEquals(3, snapshot.runs)
        Assertions.assertEquals(2, snapshot.errors)
        Assertions.assertEquals("failed; again", snapshot.lastError)
        Assertions.assertFalse(snapshot.running)
        Assertions.assertEquals(runs.reversed().map { it.start to it.status }, sunk.map { it.start to it.status })
        Assertions.assertEquals(1_500L, snapshot.lastScheduledReference, "End of the last run in the pool thread.")
    }

    @Test
    fun `an overlapping call is skipped`() {
        val job = definition("test.overlap")
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val done = CountDownLatch(1)
        registry.runAsync(job) {
            started.countDown()
            release.await(10, TimeUnit.SECONDS)
            done.countDown()
        }
        Assertions.assertTrue(started.await(10, TimeUnit.SECONDS))
        Assertions.assertTrue(registry.get(job.id)!!.snapshot().running)
        var called = false
        registry.run(job) { called = true }
        registry.runAsync(job) { called = true }
        Assertions.assertFalse(called)
        release.countDown()
        Assertions.assertTrue(done.await(10, TimeUnit.SECONDS))
        waitUntil { !registry.get(job.id)!!.snapshot().running }
        val snapshot = registry.get(job.id)!!.snapshot()
        Assertions.assertEquals(2, snapshot.skipped)
        Assertions.assertEquals(1, snapshot.runs)
        Assertions.assertEquals(
            listOf(SchedulerRunStatus.SUCCESS, SchedulerRunStatus.SKIPPED, SchedulerRunStatus.SKIPPED),
            snapshot.recentRuns.map { it.status },
        )
        registry.run(job) { called = true }
        Assertions.assertTrue(called, "Runs again after the end of the previous run.")
    }

    @Test
    fun `an inactive job isn't run and not counted`() {
        val job = definition("test.inactive")
        var reason: String? = "not configured"
        registry.register(job, {}) { reason }
        var calls = 0
        registry.run(job) { calls++ }
        Assertions.assertEquals(0, calls)
        val snapshot = registry.get(job.id)!!.snapshot()
        Assertions.assertEquals("not configured", snapshot.inactiveReason)
        Assertions.assertEquals(0, snapshot.runs + snapshot.skipped)
        Assertions.assertTrue(sunk.isEmpty())
        Assertions.assertEquals(SchedulerJobRegistry.RunNowStatus.INACTIVE, registry.runNow(job.id, "admin").status)
        reason = null
        registry.run(job) { calls++ }
        Assertions.assertEquals(1, calls)
        Assertions.assertNull(registry.get(job.id)!!.snapshot().inactiveReason)
    }

    @Test
    fun `only the last runs are kept in memory`() {
        val job = definition("test.ring")
        var time = 0L
        registry.clock = { time++ }
        repeat(25) { registry.run(job) {} }
        val snapshot = registry.get(job.id)!!.snapshot()
        Assertions.assertEquals(SchedulerJobState.MAX_RECENT_RUNS, snapshot.recentRuns.size)
        Assertions.assertEquals(25, snapshot.runs)
        Assertions.assertTrue(snapshot.recentRuns.zipWithNext().all { (a, b) -> a.start > b.start }, "Newest first.")
        Assertions.assertEquals(25, sunk.size)
    }

    @Test
    fun `run now starts the registered method, manual runs are marked`() {
        val job = definition("test.runNow")
        val triggers = CopyOnWriteArrayList<SchedulerTrigger>()
        val release = CountDownLatch(1)
        val started = CountDownLatch(1)
        registry.register(job, {
            registry.runAsync(job) { run ->
                triggers.add(run.trigger)
                started.countDown()
                release.await(10, TimeUnit.SECONDS)
            }
        })
        Assertions.assertEquals(SchedulerJobRegistry.RunNowStatus.NOT_FOUND, registry.runNow("test.unknown", "admin").status)
        Assertions.assertEquals(SchedulerJobRegistry.RunNowStatus.STARTED, registry.runNow(job.id, "admin").status)
        Assertions.assertTrue(started.await(10, TimeUnit.SECONDS))
        Assertions.assertEquals(SchedulerJobRegistry.RunNowStatus.RUNNING, registry.runNow(job.id, "admin").status)
        release.countDown()
        waitUntil { registry.get(job.id)!!.snapshot().runs == 1 && !registry.get(job.id)!!.snapshot().running }
        Assertions.assertEquals(listOf(SchedulerTrigger.MANUAL), triggers)
        val snapshot = registry.get(job.id)!!.snapshot()
        Assertions.assertEquals(SchedulerTrigger.MANUAL, snapshot.recentRuns.first().trigger)
        Assertions.assertNull(snapshot.lastScheduledReference, "Manual runs don't move the schedule.")

        // A scheduled call (by the scheduler's thread) isn't manual.
        val done = CountDownLatch(1)
        registry.runAsync(job) { run ->
            triggers.add(run.trigger)
            done.countDown()
        }
        Assertions.assertTrue(done.await(10, TimeUnit.SECONDS))
        Assertions.assertEquals(listOf(SchedulerTrigger.MANUAL, SchedulerTrigger.SCHEDULED), triggers)
    }

    @Test
    fun `an unregistered job can't be run now, a failing sink doesn't break the job`() {
        val job = definition("test.sink")
        registry.sink = SchedulerRunSink { _, _ -> throw IllegalStateException("db down") }
        registry.run(job) {}
        Assertions.assertEquals(1, registry.get(job.id)!!.snapshot().runs)
        Assertions.assertFalse(registry.get(job.id)!!.snapshot().running)
        Assertions.assertEquals(SchedulerJobRegistry.RunNowStatus.NOT_FOUND, registry.runNow(job.id, "admin").status)
    }

    private fun definition(id: String) = SchedulerJobDefinition(
        id, SchedulerJobArea.SYSTEM, javaClass, "run", SchedulerSchedule.FixedDelay(3_600_000L),
    )

    private fun waitUntil(condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 10_000
        while (!condition()) {
            Assertions.assertTrue(System.currentTimeMillis() < until, "Timeout.")
            Thread.sleep(10)
        }
    }
}
