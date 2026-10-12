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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.common.extensions.formatMillis
import org.projectforge.common.scheduling.SchedulerActiveCheck
import org.projectforge.common.scheduling.SchedulerJobBody
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerRun
import org.projectforge.common.scheduling.SchedulerTrigger
import org.projectforge.framework.persistence.jpa.MaintenanceDbTimeouts
import org.projectforge.framework.utils.AbandonedThreads
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.scheduling.config.ScheduledTaskHolder
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

private val log = KotlinLogging.logger {}

/** Gets every run of a job (e.g. for storing it), see [SchedulerJobStatsService]. */
fun interface SchedulerRunSink {
    fun recordRun(state: SchedulerJobState, record: SchedulerRunRecord)
}

/**
 * The scheduled jobs and their runs since the start, in memory: runs the jobs (see [SchedulerJobRunner]), measures
 * them, prevents overlapping runs and catches and logs exceptions. Errors are logged by this class (log location
 * `SchedulerJobRegistry`), so the log aggregation shows them as problems of the subsystem scheduler.
 */
@Service
class SchedulerJobRegistry(private val environment: Environment) : SchedulerJobRunner {
    enum class RunNowStatus { STARTED, NOT_FOUND, INACTIVE, RUNNING }

    class RunNowResult(val status: RunNowStatus, val reason: String? = null)

    enum class TerminateStatus { TERMINATED, NOT_FOUND, NOT_RUNNING }

    private val states = ConcurrentHashMap<String, SchedulerJobState>()

    /** Set by the [SchedulerJobStatsService]. */
    @Volatile
    var sink: SchedulerRunSink? = null

    /** Since when the system is up: before, no job is overdue. */
    @Volatile
    var readySince: Long? = null
        internal set

    internal var clock: () -> Long = System::currentTimeMillis

    internal val minSlowMillis: Long by lazy {
        environment.getProperty("projectforge.scheduler.minSlowMillis", Long::class.javaObjectType, DEFAULT_MIN_SLOW_MILLIS)
    }

    internal val slowFactor: Double by lazy {
        environment.getProperty("projectforge.scheduler.slowFactor", Double::class.javaObjectType, DEFAULT_SLOW_FACTOR)
    }

    /** Set in the thread of "run now" and taken over by [runAsync] into the job's thread. */
    private val manualTrigger = ThreadLocal.withInitial { false }

    val jobs: List<SchedulerJobState>
        get() = states.values.sortedWith(compareBy({ it.definition.area }, { it.definition.id }))

    fun get(id: String): SchedulerJobState? = states[id]

    override fun register(definition: SchedulerJobDefinition, entry: Runnable, activeCheck: SchedulerActiveCheck?) {
        val state = stateOf(definition)
        state.entry = entry
        state.activeCheck = activeCheck
        if (state.schedule is ResolvedSchedule.Invalid) {
            log.warn { "Scheduler job ${definition.id}: schedule '${state.schedule.value}' not understood: ${state.schedule.error}" }
        }
    }

    override fun runAsync(definition: SchedulerJobDefinition, body: SchedulerJobBody) {
        val state = stateOf(definition)
        val trigger = currentTrigger()
        val runId = begin(state, trigger, async = true) ?: return
        try {
            thread(name = "pf-scheduler-${definition.id}") {
                execute(state, runId, trigger, body, async = true)
            }
        } catch (t: Throwable) {
            // Thread couldn't be started: release the lock, otherwise the job would never run again.
            state.runningSince = null
            state.running.set(false)
            log.error(t) { "Scheduler job ${definition.id} couldn't be started: ${t.message}" }
        }
    }

    override fun run(definition: SchedulerJobDefinition, body: SchedulerJobBody) {
        val state = stateOf(definition)
        val trigger = currentTrigger()
        val runId = begin(state, trigger, async = false) ?: return
        execute(state, runId, trigger, body, async = false)
    }

    /**
     * Starts the job at once (in a new thread), as if called by the scheduler.
     * @param user Only logged.
     */
    fun runNow(id: String, user: String?): RunNowResult {
        val state = states[id] ?: return RunNowResult(RunNowStatus.NOT_FOUND)
        val entry = state.entry ?: return RunNowResult(RunNowStatus.NOT_FOUND)
        inactiveReasonOf(state)?.let {
            state.inactiveReason = it
            return RunNowResult(RunNowStatus.INACTIVE, it)
        }
        if (state.running.get()) {
            return RunNowResult(RunNowStatus.RUNNING)
        }
        log.info { "Scheduler job $id started manually by $user." }
        thread(name = "pf-scheduler-manual-$id") {
            manualTrigger.set(true)
            try {
                entry.run()
            } catch (t: Throwable) {
                log.error(t) { "Scheduler job $id failed: ${t.message}" }
            } finally {
                manualTrigger.remove()
            }
        }
        return RunNowResult(RunNowStatus.STARTED)
    }

    /**
     * Terminates the running job (system dashboard): interrupts its thread and abandons the run, so the job may run
     * again at once instead of waiting for a run that hangs (e.g. on a database lock or a remote call). The run is
     * recorded as failed. A thread ignoring the interrupt can't be stopped (no [Thread.stop] on Java 20+): it runs on
     * until it returns by itself, but its result isn't recorded any more and it doesn't touch the state of newer runs
     * (see [execute]). It is counted in [SchedulerJobState.Snapshot.abandonedThreads] meanwhile.
     * @param user Only logged.
     */
    fun terminate(id: String, user: String?): TerminateStatus {
        val state = states[id] ?: return TerminateStatus.NOT_FOUND
        val now = clock()
        val record: SchedulerRunRecord
        synchronized(state) {
            val thread = state.runningThread
            if (!state.running.get() || thread == null) {
                return TerminateStatus.NOT_RUNNING
            }
            val start = state.runningSince ?: now
            val duration = now - start
            val stackTrace = AbandonedThreads.stackTraceOf(thread)
            log.error {
                "Scheduler job $id terminated by $user after ${duration.formatMillis()} (thread=${thread.name}, " +
                        "state=${thread.state}). Stack trace of the terminated thread:$stackTrace"
            }
            state.runId.incrementAndGet()
            state.runningThread = null
            state.abandonedThreads.add(thread)
            record = SchedulerRunRecord(
                jobId = id,
                start = start,
                durationMs = duration,
                status = SchedulerRunStatus.ERROR,
                trigger = state.runningTrigger ?: SchedulerTrigger.SCHEDULED,
                errorMessage = "Terminated by $user after ${duration.formatMillis()}.",
                stackExcerpt = "Stack trace of the terminated thread ${thread.name}:$stackTrace".take(SchedulerJobRunDO.MAX_STACK),
            )
            state.runningSince = null
            state.running.set(false)
            thread.interrupt()
        }
        record(state, record)
        return TerminateStatus.TERMINATED
    }

    /** Checks whether every `@Scheduled` method is tracked (a forgotten job would be missing on the dashboard). */
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady(event: ApplicationReadyEvent) {
        readySince = clock()
        try {
            val tracked = states.values.map { "${it.definition.owner.name}.${it.definition.method}" }.toSet()
            event.applicationContext.getBeansOfType(ScheduledTaskHolder::class.java).values
                .flatMap { it.scheduledTasks }
                .map { it.task.runnable.toString().replace(CGLIB_SUFFIX, "") }
                .distinct()
                .filter { it !in tracked }
                .forEach { log.warn { "Scheduled method $it isn't tracked by the scheduler dashboard (use SchedulerJobRunner)." } }
        } catch (t: Throwable) {
            log.warn { "Couldn't check the scheduled tasks: ${t.message}" }
        }
    }

    private fun stateOf(definition: SchedulerJobDefinition): SchedulerJobState =
        states.computeIfAbsent(definition.id) {
            SchedulerJobState(definition, ResolvedSchedule.resolve(definition.schedule, environment::resolveRequiredPlaceholders))
        }

    private fun currentTrigger() = if (manualTrigger.get()) SchedulerTrigger.MANUAL else SchedulerTrigger.SCHEDULED

    private fun inactiveReasonOf(state: SchedulerJobState): String? =
        try {
            state.activeCheck?.inactiveReason()
        } catch (t: Throwable) {
            "activity check failed: ${t.message}"
        }

    /** @return The id of the new run, or null, if the job is inactive or still running (skipped). */
    private fun begin(state: SchedulerJobState, trigger: SchedulerTrigger, async: Boolean): Long? {
        val now = clock()
        val reason = inactiveReasonOf(state)
        state.inactiveReason = reason
        if (trigger == SchedulerTrigger.SCHEDULED && (async || reason != null)) {
            state.lastScheduledReference = now
        }
        if (reason != null) {
            log.debug { "Scheduler job ${state.definition.id} inactive: $reason" }
            return null
        }
        if (!state.running.compareAndSet(false, true)) {
            if (trigger == SchedulerTrigger.SCHEDULED && !async) {
                state.lastScheduledReference = now
            }
            logOf(state) { "Scheduler job ${state.definition.id} skipped: the previous run (since ${state.runningSince?.let { (now - it).formatMillis() }}) is still running." }
            record(state, SchedulerRunRecord(state.definition.id, now, 0, SchedulerRunStatus.SKIPPED, trigger))
            return null
        }
        state.runningSince = now
        state.runningTrigger = trigger
        return state.runId.incrementAndGet()
    }

    /**
     * Runs the job and records the run. A run terminated meanwhile ([terminate], [runId] changed) is already recorded
     * and its running flag released, so it records nothing when it returns and leaves the state of a newer run alone.
     */
    private fun execute(
        state: SchedulerJobState,
        runId: Long,
        trigger: SchedulerTrigger,
        body: SchedulerJobBody,
        async: Boolean,
    ) {
        val id = state.definition.id
        val run = Run(trigger)
        val start = clock()
        synchronized(state) {
            if (state.runId.get() == runId) {
                state.runningThread = Thread.currentThread()
            }
        }
        try {
            // A statement blocked on a database lock fails after the timeout instead of hanging the job.
            MaintenanceDbTimeouts.run { body.run(run) }
        } catch (t: Throwable) {
            if (state.runId.get() == runId) {
                log.error(t) { "Scheduler job $id failed: ${t.message}" }
            }
            run.fail(t.message ?: t.javaClass.name, t)
        } finally {
            val current = synchronized(state) {
                (state.runId.get() == runId).also { if (it) state.runningThread = null }
            }
            if (!current) {
                log.warn {
                    "Terminated run of scheduler job $id returned after ${(clock() - start).formatMillis()}" +
                            (run.errorMessage?.let { ": $it" } ?: ".")
                }
                // Don't leave a pooled thread (run in the scheduler's thread) interrupted.
                Thread.interrupted()
                return
            }
            val end = clock()
            val duration = end - start
            val slow = duration >= SchedulerSlowDetector.thresholdMillis(state, minSlowMillis, slowFactor, end)
            val record = SchedulerRunRecord(
                jobId = id,
                start = start,
                durationMs = duration,
                status = if (run.errorMessage != null) SchedulerRunStatus.ERROR else SchedulerRunStatus.SUCCESS,
                trigger = trigger,
                slow = slow,
                errorMessage = run.errorMessage,
                stackExcerpt = run.stackExcerpt,
                notes = run.notes,
            )
            if (trigger == SchedulerTrigger.SCHEDULED && !async) {
                state.lastScheduledReference = end
            }
            try {
                record(state, record)
            } finally {
                state.runningSince = null
                state.running.set(false)
            }
            if (run.errorMessage == null) {
                logOf(state) { "Scheduler job $id finished after ${duration.formatMillis()}${if (slow) " (slow)" else ""}." }
            }
        }
    }

    private fun record(state: SchedulerJobState, record: SchedulerRunRecord) {
        state.record(record)
        try {
            sink?.recordRun(state, record)
        } catch (t: Throwable) {
            log.warn { "Couldn't record the run of scheduler job ${record.jobId}: ${t.message}" }
        }
    }

    /** Jobs running every few minutes only log on debug level. */
    private fun logOf(state: SchedulerJobState, msg: () -> String) {
        val schedule = state.schedule
        if (schedule is ResolvedSchedule.FixedDelay && schedule.delayMillis < QUIET_DELAY_MILLIS) {
            log.debug(msg)
        } else {
            log.info(msg)
        }
    }

    private class Run(override val trigger: SchedulerTrigger) : SchedulerRun {
        var errorMessage: String? = null
        var stackExcerpt: String? = null
        val notes = mutableListOf<String>()

        override fun fail(message: String, throwable: Throwable?) {
            if (errorMessage == null) {
                errorMessage = message
            } else if (!errorMessage!!.contains(message)) {
                errorMessage = "$errorMessage; $message"
            }
            if (stackExcerpt == null && throwable != null) {
                stackExcerpt = throwable.stackTraceToString().take(SchedulerJobRunDO.MAX_STACK)
            }
        }

        override fun note(text: String) {
            notes.add(text)
        }
    }

    companion object {
        const val DEFAULT_MIN_SLOW_MILLIS = 60_000L
        const val DEFAULT_SLOW_FACTOR = 3.0
        private const val QUIET_DELAY_MILLIS = 5 * 60_000L
        private val CGLIB_SUFFIX = Regex("[\$][\$]SpringCGLIB[\$][\$]\\d+")
    }
}
