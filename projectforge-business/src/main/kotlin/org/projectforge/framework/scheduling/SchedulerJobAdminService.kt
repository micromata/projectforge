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
import org.projectforge.common.scheduling.SchedulerTrigger
import org.projectforge.framework.i18n.TimeAgo
import org.projectforge.framework.i18n.translate
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

private val log = KotlinLogging.logger {}

/** The status of a job on the dashboard, see [SchedulerJobAdminService.statusOf]. */
enum class SchedulerJobStatus {
    RUNNING,
    OK,
    FAILED,
    OVERDUE,

    /** The last call was skipped, as the job was still running. */
    SKIPPED,

    /** Not run, e.g. not configured or its plugin not activated (see [SchedulerJobEntry.inactiveReason]). */
    INACTIVE,

    /** Switched off by its schedule (cron "-") or the schedule is invalid. */
    DISABLED,

    /** Not yet run since the start (and before). */
    PENDING,
}

/**
 * A job of the dashboard's list. Times as epoch millis.
 * @param cron The resolved cron expression (placeholders replaced), if a cron job.
 * @param runs7d The runs of the last 7 days (today included), stored and not yet written ones.
 */
class SchedulerJobEntry(
    val id: String,
    val title: String,
    val description: String?,
    val area: String,
    val areaTitle: String,
    val scheduleType: String,
    val cron: String?,
    val delayMillis: Long?,
    val initialDelayMillis: Long?,
    val zone: String,
    val status: SchedulerJobStatus,
    val inactiveReason: String?,
    val runningSince: Long?,
    val lastRun: Long?,
    /** The last run as "5 minutes ago" in the user's locale. */
    val lastRunTimeAgo: String? = null,
    /** Set (translated) if the last run was before the start of the system, so it doesn't count for overdue jobs. */
    val lastRunBeforeStart: String? = null,
    val lastDurationMs: Long?,
    val lastStatus: SchedulerRunStatus?,
    val lastError: String?,
    val lastErrorTime: Long?,
    val nextRun: Long?,
    val overdue: Boolean,
    val runs7d: Int,
    val errors7d: Int,
    val skipped7d: Int,
    val slow7d: Int,
    val avgDurationMs7d: Long?,
    val maxDurationMs7d: Long?,
    val runNowAllowed: Boolean,
)

class SchedulerJobList(
    val jobs: List<SchedulerJobEntry>,
    /** Since when the system is up: runs before don't count for overdue jobs. */
    val readySince: Long?,
)

/** The runs of a job on one day or in one month. */
class SchedulerPeriodEntry(
    val periodStart: LocalDate,
    val runCount: Int,
    val successCount: Int,
    val errorCount: Int,
    val skippedCount: Int,
    val manualCount: Int,
    val slowCount: Int,
    val minDurationMs: Long?,
    val maxDurationMs: Long?,
    val avgDurationMs: Long?,
    val lastErrorMessage: String?,
)

class SchedulerRunEntry(
    val start: Long,
    val durationMs: Long,
    val status: SchedulerRunStatus,
    val trigger: SchedulerTrigger,
    val slow: Boolean,
    val errorMessage: String?,
    val stackExcerpt: String?,
    val notes: List<String>,
)

/**
 * @param recentRuns The last runs since the start (in memory), newest first.
 * @param storedRuns The failed and slow runs of the database, newest first.
 */
class SchedulerJobDetail(
    val job: SchedulerJobEntry,
    val days: List<SchedulerPeriodEntry>,
    val months: List<SchedulerPeriodEntry>,
    val recentRuns: List<SchedulerRunEntry>,
    val storedRuns: List<SchedulerRunEntry>,
    val dailyRetentionDays: Int,
)

/**
 * The scheduled jobs for the admin dashboard (tab scheduler): their state ([SchedulerJobRegistry]) and runs
 * ([SchedulerJobStatsService]). Access is checked by the caller (admins only).
 */
@Service
class SchedulerJobAdminService(
    private val registry: SchedulerJobRegistry,
    private val statsService: SchedulerJobStatsService,
) {
    internal var clock: () -> Long = System::currentTimeMillis

    fun list(): SchedulerJobList {
        val now = clock()
        val stats = statsService.statsSince(today(now).minusDays(STATS_DAYS - 1))
        return SchedulerJobList(registry.jobs.map { entryOf(it, stats[it.definition.id], now) }, registry.readySince)
    }

    fun detail(id: String): SchedulerJobDetail? {
        val state = registry.get(id) ?: return null
        val now = clock()
        val today = today(now)
        val stats = statsService.statsSince(today.minusDays(STATS_DAYS - 1), id)[id]
        val snapshot = state.snapshot()
        return SchedulerJobDetail(
            job = entryOf(state, stats, now, snapshot),
            days = statsService.days(id, today.minusDays(statsService.dailyRetentionDays.toLong()))
                .map { (day, s) -> periodOf(day, s) },
            months = statsService.months(id).map { (month, s) -> periodOf(month, s) },
            recentRuns = snapshot.recentRuns.map { runOf(it) },
            storedRuns = statsService.storedRuns(id, MAX_STORED_RUNS).map { runOf(it) },
            dailyRetentionDays = statsService.dailyRetentionDays,
        )
    }

    /** @param user Only logged. */
    fun runNow(id: String, user: String?): SchedulerJobRegistry.RunNowResult = registry.runNow(id, user)

    /** The jobs with their status, for the tile of the overview ([SchedulerSubsystemStatusProvider]). */
    internal fun overview(): List<Pair<SchedulerJobState, SchedulerJobEntry>> {
        val now = clock()
        return registry.jobs.map { it to entryOf(it, null, now) }
    }

    private fun entryOf(
        state: SchedulerJobState,
        stats: SchedulerStats?,
        now: Long,
        snapshot: SchedulerJobState.Snapshot = state.snapshot(),
    ): SchedulerJobEntry {
        val definition = state.definition
        val schedule = state.schedule
        // Checked again, as the reason is otherwise only known after the first call (e.g. nightly jobs).
        val inactiveReason = when (schedule) {
            is ResolvedSchedule.Invalid -> "Invalid schedule '${schedule.value}': ${schedule.error}"
            else -> currentInactiveReason(state)
        }
        val disabled = schedule is ResolvedSchedule.Disabled || schedule is ResolvedSchedule.Invalid
        val overdue = SchedulerOverdueCalculator.calculate(
            schedule = schedule,
            lastScheduledReference = snapshot.lastScheduledReference,
            running = snapshot.running,
            inactive = inactiveReason != null,
            readySince = registry.readySince,
            now = now,
        )
        return SchedulerJobEntry(
            id = definition.id,
            title = translateOr(definition.titleKey, definition.id)!!,
            description = translateOr(definition.descriptionKey, null),
            area = definition.area.name,
            areaTitle = translateOr(definition.area.i18nKey, definition.area.name)!!,
            scheduleType = when (schedule) {
                is ResolvedSchedule.Cron -> "CRON"
                is ResolvedSchedule.FixedDelay -> "FIXED_DELAY"
                is ResolvedSchedule.Disabled -> "DISABLED"
                is ResolvedSchedule.Invalid -> "INVALID"
            },
            cron = (schedule as? ResolvedSchedule.Cron)?.expression,
            delayMillis = (schedule as? ResolvedSchedule.FixedDelay)?.delayMillis,
            initialDelayMillis = (schedule as? ResolvedSchedule.FixedDelay)?.initialDelayMillis,
            zone = ZoneId.systemDefault().id,
            status = statusOf(snapshot, disabled = disabled, inactive = inactiveReason != null, overdue.overdue),
            inactiveReason = if (schedule is ResolvedSchedule.Disabled) null else inactiveReason,
            runningSince = snapshot.runningSince.takeIf { snapshot.running },
            lastRun = snapshot.lastStart,
            lastRunTimeAgo = snapshot.lastStart?.let { TimeAgo.getMessage(Date(it)) },
            lastRunBeforeStart = snapshot.lastStart?.takeIf { start -> registry.readySince.let { it != null && start < it } }
                ?.let { translate("system.scheduler.hint.beforeRestart") },
            lastDurationMs = snapshot.lastDurationMs,
            lastStatus = snapshot.lastStatus,
            lastError = snapshot.lastError,
            lastErrorTime = snapshot.lastErrorTime,
            nextRun = overdue.nextRun.takeIf { !disabled && inactiveReason == null },
            overdue = overdue.overdue,
            runs7d = stats?.runCount ?: 0,
            errors7d = stats?.errorCount ?: 0,
            skipped7d = stats?.skippedCount ?: 0,
            slow7d = stats?.slowCount ?: 0,
            avgDurationMs7d = stats?.avgDurationMs,
            maxDurationMs7d = stats?.maxDurationMs,
            // A disabled job may be run manually (e.g. the JCR sanity check, run nightly by the sanity checks).
            runNowAllowed = state.entry != null && inactiveReason == null && !snapshot.running &&
                    schedule !is ResolvedSchedule.Invalid,
        )
    }

    private fun currentInactiveReason(state: SchedulerJobState): String? {
        val check = state.activeCheck ?: return null
        return try {
            check.inactiveReason()
        } catch (ex: Exception) {
            log.warn(ex) { "Active check of scheduler job ${state.definition.id} failed: ${ex.message}" }
            state.inactiveReason
        }
    }

    private fun today(now: Long): LocalDate =
        LocalDate.ofInstant(Instant.ofEpochMilli(now), statsService.zone)

    companion object {
        private const val STATS_DAYS = 7L
        private const val MAX_STORED_RUNS = 100

        /** The worst first: running before all, as a running job is neither overdue nor failed (yet). */
        internal fun statusOf(
            snapshot: SchedulerJobState.Snapshot,
            disabled: Boolean,
            inactive: Boolean,
            overdue: Boolean,
        ): SchedulerJobStatus = when {
            snapshot.running -> SchedulerJobStatus.RUNNING
            inactive -> SchedulerJobStatus.INACTIVE
            overdue -> SchedulerJobStatus.OVERDUE
            snapshot.recentRuns.firstOrNull()?.status == SchedulerRunStatus.SKIPPED -> SchedulerJobStatus.SKIPPED
            snapshot.lastStatus == SchedulerRunStatus.ERROR -> SchedulerJobStatus.FAILED
            snapshot.lastStatus == SchedulerRunStatus.SUCCESS -> if (disabled) SchedulerJobStatus.DISABLED else SchedulerJobStatus.OK
            disabled -> SchedulerJobStatus.DISABLED
            else -> SchedulerJobStatus.PENDING
        }

        private fun translateOr(key: String, default: String?): String? =
            translate(key).takeUnless { it.startsWith("???") } ?: default

        private fun periodOf(start: LocalDate, stats: SchedulerStats) = SchedulerPeriodEntry(
            periodStart = start,
            runCount = stats.runCount,
            successCount = stats.successCount,
            errorCount = stats.errorCount,
            skippedCount = stats.skippedCount,
            manualCount = stats.manualCount,
            slowCount = stats.slowCount,
            minDurationMs = stats.minDurationMs,
            maxDurationMs = stats.maxDurationMs,
            avgDurationMs = stats.avgDurationMs,
            lastErrorMessage = stats.lastErrorMessage,
        )

        private fun runOf(record: SchedulerRunRecord) = SchedulerRunEntry(
            start = record.start,
            durationMs = record.durationMs,
            status = record.status,
            trigger = record.trigger,
            slow = record.slow,
            errorMessage = record.errorMessage,
            stackExcerpt = record.stackExcerpt,
            notes = record.notes,
        )
    }
}
