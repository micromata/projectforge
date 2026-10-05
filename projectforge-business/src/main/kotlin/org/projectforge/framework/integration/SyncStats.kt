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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.admin.SystemStatisticsData
import org.projectforge.business.admin.SystemsStatisticsBuilderInterface
import org.projectforge.common.extensions.formatMillis
import org.projectforge.common.logging.warn
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * Counters of one step of a sync (e.g. the users of the LDAP sync). Mutable, so the sync code can count
 * while working.
 */
class SyncCounts(
    var created: Int = 0,
    var updated: Int = 0,
    var deleted: Int = 0,
    var unchanged: Int = 0,
    var errors: Int = 0,
) {
    val total: Int
        get() = created + updated + deleted + unchanged + errors

    fun add(other: SyncCounts) {
        created += other.created
        updated += other.updated
        deleted += other.deleted
        unchanged += other.unchanged
        errors += other.errors
    }

    override fun toString(): String {
        return listOf("created" to created, "updated" to updated, "deleted" to deleted, "unchanged" to unchanged, "errors" to errors)
            .filter { it.second != 0 }
            .joinToString(", ") { "${it.first}=${it.second}" }
            .ifEmpty { "no changes" }
    }
}

/**
 * Timing and statistics of one kind of sync (LDAP, IdP, gateway, iCal subscriptions, ...), shown in the log
 * and on the system statistics page (group `sync`, see [SyncStatsRegistry]).
 *
 * Usage:
 * ```
 * private val syncStats = SyncStatsRegistry.get("ldap-master")
 * syncStats.execute { run ->
 *   run.step("users") { counts -> ... counts.updated++ ... }
 * }
 * ```
 */
class SyncStats(val type: String) {
    enum class Status { SUCCESS, ERRORS, ABORTED }

    /**
     * @param durationMs Duration of the step, or a negative value, if the step wasn't measured separately.
     */
    class StepResult(val name: String, val durationMs: Long, val counts: SyncCounts, val error: String? = null) {
        override fun toString(): String {
            val result = error?.let { "failed: $it" } ?: counts.toString()
            return if (durationMs >= 0) "$name [$result] ${durationMs.formatMillis()}" else "$name [$result]"
        }
    }

    inner class Run internal constructor(val info: String?) {
        val start = System.currentTimeMillis()
        val steps = mutableListOf<StepResult>()
        private var done = false

        /**
         * Measures the given step. An exception is recorded and rethrown.
         */
        fun <T> step(name: String, block: (SyncCounts) -> T): T {
            val stepStart = System.currentTimeMillis()
            val counts = SyncCounts()
            try {
                val result = block(counts)
                steps.add(StepResult(name, System.currentTimeMillis() - stepStart, counts))
                return result
            } catch (e: Exception) {
                steps.add(StepResult(name, System.currentTimeMillis() - stepStart, counts, e.message ?: e.javaClass.simpleName))
                throw e
            }
        }

        /**
         * Adds a step measured elsewhere.
         */
        fun addStep(name: String, durationMs: Long, counts: SyncCounts) {
            steps.add(StepResult(name, durationMs, counts))
        }

        /**
         * Adds the counts of a step without own timing (e.g. counted in the same loop as other steps).
         */
        fun addStep(name: String, counts: SyncCounts) {
            addStep(name, -1, counts)
        }

        /**
         * Finishes the run successfully (or with errors, if any step counted errors).
         */
        fun finish() {
            val errors = steps.sumOf { it.counts.errors }
            complete(if (errors > 0) Status.ERRORS else Status.SUCCESS, if (errors > 0) "$errors errors" else null)
        }

        /**
         * The remote system isn't reachable or answers too slowly: the run was stopped.
         */
        fun abort(reason: String, timeout: Boolean = false) {
            complete(Status.ABORTED, reason, timeout)
        }

        /**
         * The run had nothing to do (e.g. disabled or no changes). Doesn't count as a run.
         */
        fun skip(reason: String) {
            if (done) return
            done = true
            log.info { "Sync $type${infoString} skipped: $reason" }
        }

        val durationMs: Long
            get() = System.currentTimeMillis() - start

        private val infoString: String
            get() = info?.let { " ($it)" } ?: ""

        private fun complete(status: Status, message: String?, timeout: Boolean = false) {
            if (done) return
            done = true
            val duration = durationMs
            record(this, status, duration, message, timeout)
            // The type unquoted: the log aggregation normalizes a quoted value, which would make the failures of all
            // syncs one problem, but the dashboard attributes them to their subsystem (SubsystemProblemMatch).
            val summary = "Sync $type$infoString ${status.name.lowercase()} in ${duration.formatMillis()}" +
                    (message?.let { ": $it" } ?: "") +
                    (if (steps.isNotEmpty()) steps.joinToString(", ", prefix = " - ") else "")
            when (status) {
                Status.SUCCESS -> log.info { summary }
                else -> log.warn(IntegrationLogEvents.SYNC_RUN_FAILED) { summary }
            }
        }
    }

    var runs = 0
        private set
    var successfulRuns = 0
        private set
    var runsWithErrors = 0
        private set
    var abortedRuns = 0
        private set
    var timeouts = 0
        private set
    var lastRunStart: Date? = null
        private set
    var lastStatus: Status? = null
        private set
    var lastDurationMs: Long? = null
        private set
    var maxDurationMs = 0L
        private set
    var totalDurationMs = 0L
        private set
    var lastSteps: List<StepResult> = emptyList()
        private set
    var lastError: String? = null
        private set
    var lastErrorDate: Date? = null
        private set

    fun startRun(info: String? = null): Run {
        return Run(info)
    }

    /**
     * Runs the block, finishes the run afterwards, if not done by the block itself. An exception aborts the
     * run (and is rethrown).
     */
    fun <T> execute(info: String? = null, block: (Run) -> T): T {
        val run = startRun(info)
        try {
            val result = block(run)
            run.finish()
            return result
        } catch (e: Exception) {
            run.abort(e.message ?: e.javaClass.simpleName, IntegrationErrors.isTimeout(e))
            throw e
        }
    }

    @Synchronized
    private fun record(run: Run, status: Status, durationMs: Long, message: String?, timeout: Boolean) {
        runs++
        when (status) {
            Status.SUCCESS -> successfulRuns++
            Status.ERRORS -> runsWithErrors++
            Status.ABORTED -> abortedRuns++
        }
        if (timeout) {
            timeouts++
        }
        lastRunStart = Date(run.start)
        lastStatus = status
        lastDurationMs = durationMs
        maxDurationMs = maxOf(maxDurationMs, durationMs)
        totalDurationMs += durationMs
        lastSteps = run.steps.toList()
        if (status != Status.SUCCESS) {
            lastError = message
            lastErrorDate = Date()
        }
    }

    /**
     * One line for the system statistics page.
     */
    @Synchronized
    fun summary(): String {
        if (runs == 0) {
            return "no runs yet"
        }
        val sb = StringBuilder()
        sb.append("last: ").append(lastStatus?.name?.lowercase())
            .append(" at ").append(formatDate(lastRunStart))
            .append(" in ").append(lastDurationMs.formatMillis())
        if (lastSteps.isNotEmpty()) {
            sb.append(lastSteps.joinToString(", ", prefix = " (", postfix = ")"))
        }
        sb.append(" | runs=").append(runs)
            .append(", ok=").append(successfulRuns)
            .append(", withErrors=").append(runsWithErrors)
            .append(", aborted=").append(abortedRuns)
            .append(", timeouts=").append(timeouts)
            .append(", avg=").append((totalDurationMs / runs).formatMillis())
            .append(", max=").append(maxDurationMs.formatMillis())
        lastError?.let {
            sb.append(" | last problem at ").append(formatDate(lastErrorDate)).append(": ").append(it)
        }
        return sb.toString()
    }

    private fun formatDate(date: Date?): String {
        date ?: return "--"
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date)
    }
}

/**
 * All [SyncStats], shown on the system statistics page (group `sync`, admins only).
 */
object SyncStatsRegistry : SystemsStatisticsBuilderInterface {
    private val registry = ConcurrentHashMap<String, SyncStats>()

    @JvmStatic
    fun get(type: String): SyncStats {
        return registry.computeIfAbsent(type) { SyncStats(it) }
    }

    /** All registered sync types (e.g. for the support error digest). */
    @JvmStatic
    fun all(): List<SyncStats> = registry.values.sortedBy { it.type }

    override fun addStatisticsEntries(stats: SystemStatisticsData) {
        registry.values.sortedBy { it.type }.forEach {
            stats.add("sync-${it.type}", GROUP, "'${it.type}", it.summary())
        }
    }

    const val GROUP = "sync"
}

/**
 * Classifies exceptions of remote calls.
 */
object IntegrationErrors {
    /**
     * True for connect, read and response timeouts of all used clients (JDK, Apache, Netty, Reactor).
     */
    @JvmStatic
    fun isTimeout(e: Throwable): Boolean {
        return causes(e).any { it.javaClass.simpleName.contains("Timeout", ignoreCase = true) }
    }

    /**
     * True, if the remote system isn't reachable (connection refused, unknown host, timeout).
     */
    @JvmStatic
    fun isConnectionError(e: Throwable): Boolean {
        return causes(e).any { it is ConnectException || it is UnknownHostException || it is NoRouteToHostException } ||
                isTimeout(e)
    }

    private fun causes(e: Throwable): Sequence<Throwable> {
        return generateSequence(e) { cause -> cause.cause?.takeIf { it !== cause } }.take(20)
    }
}
