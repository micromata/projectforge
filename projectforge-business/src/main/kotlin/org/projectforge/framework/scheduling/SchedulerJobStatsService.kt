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
import jakarta.annotation.PostConstruct
import org.projectforge.Constants
import org.projectforge.ShutdownListener
import org.projectforge.ShutdownService
import org.projectforge.business.privacyprotection.CronPrivacyProtectionJob
import org.projectforge.business.privacyprotection.IPrivacyProtectionJob
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerSchedule
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {}

/**
 * Stores the runs of the scheduled jobs ([SchedulerJobRegistry]) in the database:
 * - per job and day ([SchedulerJobStatsDO]): counted in memory and written every 5 minutes ([flush]) and on
 *   shutdown, so even jobs running every 30 seconds cause only one write per job and flush,
 * - failed and slow runs as single runs ([SchedulerJobRunDO]): written at once (in a thread of its own, not by the
 *   job), at most [maxStoredRunsPerJobAndDay] per job and day.
 *
 * If the database fails, the data is kept in memory for the next try (the single runs bounded).
 *
 * Cleaned up by the daily privacy protection job ([cleanup]): the days of complete months older than
 * [dailyRetentionDays] are aggregated into one row per job and month, old months and single runs are deleted.
 */
@Service
class SchedulerJobStatsService : SchedulerRunSink, IPrivacyProtectionJob, ShutdownListener {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var registry: SchedulerJobRegistry

    @Autowired
    private lateinit var cronPrivacyProtectionJob: CronPrivacyProtectionJob

    @Autowired
    private lateinit var shutdownService: ShutdownService

    @Value("\${projectforge.scheduler.stats.dailyRetentionDays:90}")
    internal var dailyRetentionDays: Int = 90

    @Value("\${projectforge.scheduler.stats.monthlyRetentionMonths:60}")
    internal var monthlyRetentionMonths: Int = 60

    @Value("\${projectforge.scheduler.stats.runRetentionDays:90}")
    internal var runRetentionDays: Int = 90

    @Value("\${projectforge.scheduler.stats.maxStoredRunsPerJobAndDay:100}")
    internal var maxStoredRunsPerJobAndDay: Int = 100

    internal var zone: ZoneId = ZoneId.systemDefault()

    internal var clock: () -> Long = System::currentTimeMillis

    /** The runs per job and day not yet written. */
    private val pending = ConcurrentHashMap<DayKey, SchedulerStats>()

    /** Failed and slow runs not yet written. */
    private val pendingRuns = ConcurrentLinkedQueue<SchedulerRunRecord>()

    /** The single runs stored per job and day (since the start), for the cap. */
    private val storedRunsPerDay = ConcurrentHashMap<DayKey, AtomicInteger>()

    private val runWriter = Executors.newSingleThreadExecutor { Thread(it, "pf-scheduler-stats").apply { isDaemon = true } }

    private val runWriteScheduled = AtomicBoolean(false)

    /** Only the first failure is logged, not every 5 minutes. */
    @Volatile
    private var lastWriteFailed = false

    /** Set on shutdown: the database may be closed, nothing is written anymore. */
    @Volatile
    private var closed = false

    internal data class DayKey(val jobId: String, val day: LocalDate)

    @PostConstruct
    internal fun init() {
        registry.sink = this
        registry.register(FLUSH_JOB, ::scheduledFlush)
        cronPrivacyProtectionJob.register(this)
        shutdownService.registerListener(this)
    }

    /** Only counts in memory (single runs to store are written soon by another thread). */
    override fun recordRun(state: SchedulerJobState, record: SchedulerRunRecord) {
        val key = DayKey(record.jobId, dayOf(record.start))
        pending.compute(key) { _, stats -> (stats ?: SchedulerStats()).add(record) }
        if (record.storable) {
            val count = storedRunsPerDay.computeIfAbsent(key) { AtomicInteger() }.incrementAndGet()
            if (count <= maxStoredRunsPerJobAndDay) {
                enqueueRuns(listOf(record))
                writeRunsSoon()
            }
        }
    }

    @Scheduled(fixedDelayString = FLUSH_DELAY, initialDelayString = FLUSH_DELAY)
    fun scheduledFlush() {
        registry.run(FLUSH_JOB) { run ->
            if (!flush()) {
                run.fail("Scheduler statistics couldn't be written (kept in memory).")
            }
        }
    }

    /**
     * Writes everything counted since the last call.
     * @return false, if the database failed (the data is kept for the next try).
     */
    @Synchronized
    fun flush(): Boolean {
        if (closed) {
            return true
        }
        val days = drainDays()
        val runs = drainRuns()
        if (days.isEmpty() && runs.isEmpty()) {
            // Nothing to write: no db access (e.g. on shutdown, when the db may already be closed).
            return true
        }
        val written = guarded("write the statistics") {
            persistenceService.runInNewTransaction { context ->
                days.forEach { (key, stats) -> write(context, key.jobId, SchedulerStatsPeriod.DAY, key.day, stats) }
                runs.forEach { context.insert(toDO(it)) }
            }
        }
        if (written == null) {
            days.forEach { (key, stats) -> restoreDay(key, stats) }
            enqueueRuns(runs)
            return false
        }
        val yesterday = today().minusDays(1)
        storedRunsPerDay.keys.removeIf { it.day < yesterday }
        refreshBaselines()
        return true
    }

    /** Writes the failed and slow runs not yet written. */
    @Synchronized
    internal fun writeRuns() {
        if (closed) {
            return
        }
        val runs = drainRuns()
        if (runs.isEmpty()) {
            return
        }
        guarded("write ${runs.size} failed or slow runs") {
            persistenceService.runInNewTransaction { context -> runs.forEach { context.insert(toDO(it)) } }
        } ?: enqueueRuns(runs)
    }

    /** Called on shutdown by [ShutdownService] before the database is closed. */
    override fun shutdown() {
        flush()
        closed = true
        runWriter.shutdown()
    }

    /** The daily cleanup (privacy protection job), see [cleanup]. Failures are reported by that job. */
    override fun execute() {
        cleanup()
    }

    class CleanupResult(val aggregatedDays: Int, val months: Int, val deletedMonths: Int, val deletedRuns: Int)

    /**
     * Aggregates the days of the complete months before [dailyRetentionDays] into one row per job and month (in the
     * same transaction, as they are deleted, so a second call doesn't count them twice), deletes the months older
     * than [monthlyRetentionMonths] and the single runs older than [runRetentionDays].
     */
    @Synchronized
    internal fun cleanup(now: Long = clock()): CleanupResult {
        flush()
        val today = dayOf(now)
        val cutoff = today.minusDays(dailyRetentionDays.toLong()).withDayOfMonth(1)
        val result = persistenceService.runInNewTransaction { context ->
            val days = context.executeNamedQuery(
                SchedulerJobStatsDO.SELECT_BEFORE, SchedulerJobStatsDO::class.java,
                "periodType" to SchedulerStatsPeriod.DAY, "before" to cutoff,
            )
            val months = days.groupBy { DayKey(it.jobId!!, it.periodStart!!.withDayOfMonth(1)) }
            months.forEach { (key, rows) ->
                val stats = SchedulerStats()
                rows.forEach { stats.merge(SchedulerStats.of(it)) }
                write(context, key.jobId, SchedulerStatsPeriod.MONTH, key.day, stats)
            }
            context.executeNamedUpdate(
                SchedulerJobStatsDO.DELETE_BEFORE, "periodType" to SchedulerStatsPeriod.DAY, "before" to cutoff,
            )
            val deletedMonths = context.executeNamedUpdate(
                SchedulerJobStatsDO.DELETE_BEFORE, "periodType" to SchedulerStatsPeriod.MONTH,
                "before" to today.withDayOfMonth(1).minusMonths(monthlyRetentionMonths.toLong()),
            )
            val deletedRuns = context.executeNamedUpdate(
                SchedulerJobRunDO.DELETE_BEFORE, "before" to Date(now - runRetentionDays * Constants.MILLIS_PER_DAY),
            )
            CleanupResult(days.size, months.size, deletedMonths, deletedRuns)
        }
        log.info {
            "Scheduler statistics cleaned up: ${result.aggregatedDays} days aggregated into ${result.months} months, " +
                    "${result.deletedMonths} months and ${result.deletedRuns} single runs deleted."
        }
        return result
    }

    /** After a restart, the last runs are shown, not "never run" (nightly and monthly jobs). */
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        guarded("load the last runs") {
            val rows = persistenceService.runReadOnly { context ->
                context.executeNamedQuery(
                    SchedulerJobStatsDO.SELECT_SINCE, SchedulerJobStatsDO::class.java,
                    "periodType" to SchedulerStatsPeriod.DAY, "since" to today().minusDays(LAST_RUN_SEARCH_DAYS),
                )
            }
            rows.groupBy { it.jobId }.forEach { (jobId, list) ->
                val stats = SchedulerStats()
                list.forEach { stats.merge(SchedulerStats.of(it)) }
                registry.get(jobId!!)?.restore(stats)
            }
        }
        refreshBaselines()
    }

    /** The stats per job since the given day (stored and not yet written). */
    fun statsSince(since: LocalDate, jobId: String? = null): Map<String, SchedulerStats> {
        val result = mutableMapOf<String, SchedulerStats>()
        daysSince(since, jobId).forEach { (key, stats) ->
            result.getOrPut(key.jobId) { SchedulerStats() }.merge(stats)
        }
        return result
    }

    /** The days of a job since the given day, today's runs not yet written included. */
    fun days(jobId: String, since: LocalDate): List<Pair<LocalDate, SchedulerStats>> =
        daysSince(since, jobId).map { it.key.day to it.value }.sortedBy { it.first }

    fun months(jobId: String): List<Pair<LocalDate, SchedulerStats>> =
        persistenceService.runReadOnly { context ->
            context.executeNamedQuery(
                SchedulerJobStatsDO.SELECT_BY_JOB_SINCE, SchedulerJobStatsDO::class.java,
                "jobId" to jobId, "periodType" to SchedulerStatsPeriod.MONTH,
                "since" to today().withDayOfMonth(1).minusMonths(monthlyRetentionMonths.toLong()),
            )
        }.map { it.periodStart!! to SchedulerStats.of(it) }

    /** The stored failed and slow runs of a job, newest first, the ones not yet written included. */
    fun storedRuns(jobId: String, maxResults: Int): List<SchedulerRunRecord> {
        val stored = persistenceService.runReadOnly { context ->
            context.executeNamedQuery(
                SchedulerJobRunDO.SELECT_BY_JOB, SchedulerJobRunDO::class.java, "jobId" to jobId,
                maxResults = maxResults,
            )
        }.map {
            SchedulerRunRecord(
                jobId = it.jobId!!,
                start = it.startTime!!.time,
                durationMs = it.durationMs,
                status = it.status!!,
                trigger = it.trigger!!,
                slow = it.slow,
                errorMessage = it.errorMessage,
                stackExcerpt = it.stackExcerpt,
            )
        }
        return (pendingRuns.filter { it.jobId == jobId } + stored).sortedByDescending { it.start }.take(maxResults)
    }

    private fun daysSince(since: LocalDate, jobId: String?): Map<DayKey, SchedulerStats> {
        val result = mutableMapOf<DayKey, SchedulerStats>()
        persistenceService.runReadOnly { context ->
            if (jobId == null) {
                context.executeNamedQuery(
                    SchedulerJobStatsDO.SELECT_SINCE, SchedulerJobStatsDO::class.java,
                    "periodType" to SchedulerStatsPeriod.DAY, "since" to since,
                )
            } else {
                context.executeNamedQuery(
                    SchedulerJobStatsDO.SELECT_BY_JOB_SINCE, SchedulerJobStatsDO::class.java,
                    "jobId" to jobId, "periodType" to SchedulerStatsPeriod.DAY, "since" to since,
                )
            }
        }.forEach { result[DayKey(it.jobId!!, it.periodStart!!)] = SchedulerStats.of(it) }
        // The deltas not yet written are added: the rows contain only the flushed ones.
        pending.keys.filter { it.day >= since && (jobId == null || it.jobId == jobId) }.forEach { key ->
            // Copied under the lock of the map entry, as recordRun modifies it there.
            pending.computeIfPresent(key) { _, stats ->
                result.getOrPut(key) { SchedulerStats() }.merge(stats)
                stats
            }
        }
        return result
    }

    /** The average duration of the last days, for detecting slow runs ([SchedulerSlowDetector]). */
    private fun refreshBaselines() {
        val stats = guarded("read the average durations") {
            statsSince(today().minusDays(BASELINE_DAYS))
        } ?: return
        registry.jobs.forEach { state ->
            val jobStats = stats[state.definition.id]
            state.baselineAvgMs = jobStats?.takeIf { it.runCount >= MIN_BASELINE_RUNS }?.avgDurationMs
        }
    }

    private fun write(
        context: PfPersistenceContext,
        jobId: String,
        periodType: SchedulerStatsPeriod,
        periodStart: LocalDate,
        stats: SchedulerStats,
    ) {
        val dbo = context.selectNamedSingleResult(
            SchedulerJobStatsDO.FIND_BY_JOB_PERIOD, SchedulerJobStatsDO::class.java,
            "jobId" to jobId, "periodType" to periodType, "periodStart" to periodStart, attached = true,
        ) ?: SchedulerJobStatsDO().also {
            it.jobId = jobId
            it.periodType = periodType
            it.periodStart = periodStart
        }
        SchedulerStats.mergeInto(dbo, stats)
        if (dbo.id == null) {
            context.insert(dbo)
        }
    }

    private fun toDO(record: SchedulerRunRecord) = SchedulerJobRunDO().also {
        it.jobId = record.jobId
        it.startTime = Date(record.start)
        it.durationMs = record.durationMs
        it.status = record.status
        it.slow = record.slow
        it.trigger = record.trigger
        it.errorMessage = record.errorMessage?.take(SchedulerJobStatsDO.MAX_MESSAGE)
        it.stackExcerpt = record.stackExcerpt?.take(SchedulerJobRunDO.MAX_STACK)
    }

    private fun drainDays(): List<Pair<DayKey, SchedulerStats>> =
        pending.keys.toList().mapNotNull { key -> pending.remove(key)?.let { key to it } }

    private fun restoreDay(key: DayKey, stats: SchedulerStats) {
        pending.compute(key) { _, current -> current?.merge(stats) ?: stats }
    }

    private fun drainRuns(): List<SchedulerRunRecord> = generateSequence { pendingRuns.poll() }.toList()

    private fun enqueueRuns(runs: List<SchedulerRunRecord>) {
        pendingRuns.addAll(runs)
        while (pendingRuns.size > MAX_PENDING_RUNS) {
            pendingRuns.poll()
        }
    }

    private fun writeRunsSoon() {
        if (closed || !runWriteScheduled.compareAndSet(false, true)) {
            return
        }
        try {
            runWriter.execute {
                runWriteScheduled.set(false)
                writeRuns()
            }
        } catch (t: Throwable) {
            // Executor shut down: written by the next flush (if any).
            runWriteScheduled.set(false)
        }
    }

    private fun today(): LocalDate = dayOf(clock())

    private fun dayOf(millis: Long): LocalDate = LocalDate.ofInstant(Instant.ofEpochMilli(millis), zone)

    /** Runs the block; null on failure, which is logged once until it works again. */
    private fun <T> guarded(what: String, block: () -> T): T? {
        try {
            return block().also {
                if (lastWriteFailed) {
                    lastWriteFailed = false
                    log.info { "Scheduler statistics work again." }
                }
            }
        } catch (t: Throwable) {
            if (!lastWriteFailed) {
                lastWriteFailed = true
                log.warn(t) { "Scheduler statistics couldn't $what (kept in memory, retried): ${t.message}" }
            }
            return null
        }
    }

    companion object {
        private const val FLUSH_DELAY = "\${projectforge.scheduler.stats.flushIntervalMillis:300000}"

        val FLUSH_JOB = SchedulerJobDefinition(
            "scheduler.statsFlush", SchedulerJobArea.SYSTEM, SchedulerJobStatsService::class.java, "scheduledFlush",
            SchedulerSchedule.FixedDelay(FLUSH_DELAY, FLUSH_DELAY),
        )

        private const val MAX_PENDING_RUNS = 1000
        private const val LAST_RUN_SEARCH_DAYS = 62L
        private const val BASELINE_DAYS = 7L
        private const val MIN_BASELINE_RUNS = 10
    }
}
