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

import org.projectforge.common.scheduling.SchedulerTrigger
import java.util.Date

enum class SchedulerRunStatus { SUCCESS, ERROR, SKIPPED }

/**
 * A run of a scheduled job (times as epoch millis).
 * @param slow Took longer than the threshold of [SchedulerSlowDetector].
 */
class SchedulerRunRecord(
    val jobId: String,
    val start: Long,
    val durationMs: Long,
    val status: SchedulerRunStatus,
    val trigger: SchedulerTrigger,
    val slow: Boolean = false,
    val errorMessage: String? = null,
    val stackExcerpt: String? = null,
    val notes: List<String> = emptyList(),
) {
    /** Failed and slow runs are stored as single runs ([SchedulerJobRunDO]). */
    val storable: Boolean
        get() = status == SchedulerRunStatus.ERROR || slow && status == SchedulerRunStatus.SUCCESS
}

/**
 * The runs of a job in a period, in memory (the runs since the last flush) or loaded from a [SchedulerJobStatsDO].
 * Merged additively, so a day can be written in several flushes and the days of a month merged into one.
 */
class SchedulerStats {
    var runCount = 0
    var successCount = 0
    var errorCount = 0
    var skippedCount = 0
    var manualCount = 0
    var slowCount = 0
    var minDurationMs: Long? = null
    var maxDurationMs: Long? = null
    var sumDurationMs = 0L
    var lastRunStart: Long? = null
    var lastStatus: SchedulerRunStatus? = null
    var lastDurationMs: Long? = null
    var lastErrorMessage: String? = null
    var lastErrorTime: Long? = null

    val avgDurationMs: Long?
        get() = if (runCount > 0) sumDurationMs / runCount else null

    fun add(record: SchedulerRunRecord): SchedulerStats {
        if (record.status == SchedulerRunStatus.SKIPPED) {
            skippedCount++
            return this
        }
        runCount++
        if (record.status == SchedulerRunStatus.ERROR) errorCount++ else successCount++
        if (record.trigger == SchedulerTrigger.MANUAL) manualCount++
        if (record.slow) slowCount++
        minDurationMs = minOf(minDurationMs ?: record.durationMs, record.durationMs)
        maxDurationMs = maxOf(maxDurationMs ?: record.durationMs, record.durationMs)
        sumDurationMs += record.durationMs
        if (lastRunStart == null || record.start >= lastRunStart!!) {
            lastRunStart = record.start
            lastStatus = record.status
            lastDurationMs = record.durationMs
        }
        if (record.status == SchedulerRunStatus.ERROR && (lastErrorTime == null || record.start >= lastErrorTime!!)) {
            lastErrorTime = record.start
            lastErrorMessage = record.errorMessage
        }
        return this
    }

    fun merge(other: SchedulerStats): SchedulerStats {
        runCount += other.runCount
        successCount += other.successCount
        errorCount += other.errorCount
        skippedCount += other.skippedCount
        manualCount += other.manualCount
        slowCount += other.slowCount
        minDurationMs = listOfNotNull(minDurationMs, other.minDurationMs).minOrNull()
        maxDurationMs = listOfNotNull(maxDurationMs, other.maxDurationMs).maxOrNull()
        sumDurationMs += other.sumDurationMs
        val otherLast = other.lastRunStart
        if (otherLast != null && (lastRunStart == null || otherLast >= lastRunStart!!)) {
            lastRunStart = otherLast
            lastStatus = other.lastStatus
            lastDurationMs = other.lastDurationMs
        }
        val otherError = other.lastErrorTime
        if (otherError != null && (lastErrorTime == null || otherError >= lastErrorTime!!)) {
            lastErrorTime = otherError
            lastErrorMessage = other.lastErrorMessage
        }
        return this
    }

    fun copyTo(dbo: SchedulerJobStatsDO) {
        dbo.runCount = runCount
        dbo.successCount = successCount
        dbo.errorCount = errorCount
        dbo.skippedCount = skippedCount
        dbo.manualCount = manualCount
        dbo.slowCount = slowCount
        dbo.minDurationMs = minDurationMs
        dbo.maxDurationMs = maxDurationMs
        dbo.sumDurationMs = sumDurationMs
        dbo.lastRunStart = lastRunStart?.let { Date(it) }
        dbo.lastStatus = lastStatus
        dbo.lastDurationMs = lastDurationMs
        dbo.lastErrorMessage = lastErrorMessage?.take(SchedulerJobStatsDO.MAX_MESSAGE)
        dbo.lastErrorTime = lastErrorTime?.let { Date(it) }
    }

    companion object {
        fun of(dbo: SchedulerJobStatsDO): SchedulerStats = SchedulerStats().also {
            it.runCount = dbo.runCount
            it.successCount = dbo.successCount
            it.errorCount = dbo.errorCount
            it.skippedCount = dbo.skippedCount
            it.manualCount = dbo.manualCount
            it.slowCount = dbo.slowCount
            it.minDurationMs = dbo.minDurationMs
            it.maxDurationMs = dbo.maxDurationMs
            it.sumDurationMs = dbo.sumDurationMs
            it.lastRunStart = dbo.lastRunStart?.time
            it.lastStatus = dbo.lastStatus
            it.lastDurationMs = dbo.lastDurationMs
            it.lastErrorMessage = dbo.lastErrorMessage
            it.lastErrorTime = dbo.lastErrorTime?.time
        }

        /** Merges [other] into the row (new or loaded). */
        fun mergeInto(dbo: SchedulerJobStatsDO, other: SchedulerStats) {
            of(dbo).merge(other).copyTo(dbo)
        }
    }
}
