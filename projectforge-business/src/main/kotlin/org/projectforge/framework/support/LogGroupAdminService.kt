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

package org.projectforge.framework.support

import org.projectforge.Constants
import org.projectforge.common.logging.LogAudience
import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEventRegistry
import org.projectforge.common.logging.LogLevel
import org.projectforge.common.logging.LogNotify
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/** Which problems the dashboard lists. [OPEN]: new and acknowledged ones. */
enum class LogGroupStatusFilter(val statuses: Set<LogGroupStatus>?) {
    OPEN(setOf(LogGroupStatus.NEW, LogGroupStatus.ACKNOWLEDGED)),
    ALL(null),
    NEW(setOf(LogGroupStatus.NEW)),
    ACKNOWLEDGED(setOf(LogGroupStatus.ACKNOWLEDGED)),
    IGNORED(setOf(LogGroupStatus.IGNORED)),
    RESOLVED(setOf(LogGroupStatus.RESOLVED)),
}

/**
 * @param days Only problems occurred within the last days; null or 0 for all.
 * @param search Part of code, message, location or exception class, case-insensitive.
 */
class LogGroupFilter(
    var status: LogGroupStatusFilter = LogGroupStatusFilter.OPEN,
    var category: LogCategory? = null,
    var search: String? = null,
    var days: Int? = 7,
)

/**
 * A problem of the dashboard's list. Times as epoch millis.
 * @param notify The effective notify rule: the admin's override, else the one of the event.
 * @param regression A resolved problem that occurred again (and isn't looked at yet).
 * @param trend The occurrences of the last [LogGroupList.trendDays] days in bins of [LogGroupList.trendBinHours]
 * hours, oldest first.
 */
class LogGroupEntry(
    val id: Long,
    val code: String,
    val category: LogCategory,
    val level: LogLevel,
    val location: String?,
    val exceptionClass: String?,
    val message: String?,
    val firstSeen: Long,
    val lastSeen: Long,
    val totalCount: Long,
    val count24h: Int,
    val status: LogGroupStatus,
    val mutedUntil: Long?,
    val muted: Boolean,
    val notify: LogNotify,
    val overrideNotify: LogNotify?,
    val regression: Boolean,
    val trend: IntArray,
)

/** The key figures of all problems, whatever the filter. */
class LogGroupSummary(
    /** Occurrences of all problems within the last 24 hours. */
    val occurrences24h: Int,
    /** Problems first seen within the last 24 hours. */
    val newProblems24h: Int,
    /** Resolved problems that occurred again and are new again. */
    val regressions: Int,
    /** External systems (category EXTERNAL) with problems within the last 24 hours. */
    val externalProblems24h: Int,
    /** Problems with status NEW. */
    val open: Int,
)

/**
 * @param enabled False, if the log aggregation is disabled (`projectforge.support.logAggregation.enabled`).
 * @param total The number of matching problems; [entries] holds at most [LogGroupAdminService.MAX_ENTRIES].
 */
class LogGroupList(
    val enabled: Boolean,
    val entries: List<LogGroupEntry>,
    val total: Int,
    val summary: LogGroupSummary,
    val trendDays: Int = LogGroupAdminService.TREND_DAYS,
    val trendBinHours: Int = LogGroupAdminService.TREND_BIN_HOURS,
)

/**
 * A problem with everything known about it: the texts of its event and the sample of an occurrence.
 * @param registered The code is one of a known event ([LogEventRegistry]); otherwise explanation etc. are missing.
 * @param hourly The occurrences of the last 7 days per hour, oldest first, starting at [hourlyStart].
 * @param daily The occurrences of the last 30 days per 24 hours, oldest first, starting at [dailyStart].
 */
class LogGroupDetail(
    val entry: LogGroupEntry,
    val registered: Boolean,
    val explanation: String?,
    val action: String?,
    val eventNotify: LogNotify,
    val audience: LogAudience,
    val threshold: Int,
    val sampleMessage: String?,
    val sampleStackTrace: String?,
    val sampleRequest: String?,
    val lastNotified: Long?,
    val reopenedAt: Long?,
    val distinctUsers24h: Int,
    val hourly: IntArray,
    val hourlyStart: Long,
    val daily: IntArray,
    val dailyStart: Long,
)

enum class LogGroupAction {
    ACKNOWLEDGE,
    IGNORE,
    RESOLVE,

    /** Back to NEW. */
    REOPEN,

    /** Not reported for [LogGroupUpdate.muteDays] days. */
    MUTE,
    UNMUTE,

    /** Overrides the notify rule of the event by [LogGroupUpdate.notify], null for the event's one. */
    SET_NOTIFY,
}

class LogGroupUpdate(
    var ids: List<Long> = emptyList(),
    var action: LogGroupAction? = null,
    var muteDays: Int? = null,
    var notify: LogNotify? = null,
)

/**
 * The admin dashboard of the log aggregation (`next/adminErrors`): lists the problems ([LogGroupDO]) with their
 * trends ([LogBucketDO]) and changes their status. No access checks here, see `AdminErrorsRest`.
 */
@Service
class LogGroupAdminService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var logAggregationService: LogAggregationService

    fun list(filter: LogGroupFilter, now: Long = System.currentTimeMillis()): LogGroupList {
        // The counts of the last 30 seconds aren't written yet.
        logAggregationService.flush()
        val currentHour = PendingLogGroup.hourOf(now)
        val trendStart = currentHour - (TREND_DAYS * 24 - 1) * Constants.MILLIS_PER_HOUR
        val (rows, buckets) = persistenceService.runReadOnly { context ->
            context.executeNamedQuery(LogGroupDO.SELECT_ROWS, LogGroupRow::class.java) to
                    context.executeNamedQuery(LogBucketDO.SELECT_SINCE, LogBucketRow::class.java, "since" to Date(trendStart))
        }
        val bucketsByGroup = buckets.groupBy { it.groupId }
        val since24h = currentHour - 23 * Constants.MILLIS_PER_HOUR
        val summary = LogGroupSummary(
            occurrences24h = buckets.filter { it.bucketStart.time >= since24h }.sumOf { it.occurrences },
            newProblems24h = rows.count { it.firstSeen.time >= now - Constants.MILLIS_PER_DAY },
            regressions = rows.count { isRegression(it) },
            externalProblems24h = rows.count { it.category == LogCategory.EXTERNAL && it.lastSeen.time >= now - Constants.MILLIS_PER_DAY },
            open = rows.count { it.status == LogGroupStatus.NEW },
        )
        val matching = rows.filter { matches(it, filter, now) }
        val entries = matching.take(MAX_ENTRIES).map { row ->
            val groupBuckets = bucketsByGroup[row.id].orEmpty()
            entryOf(
                row, now,
                count24h = groupBuckets.filter { it.bucketStart.time >= since24h }.sumOf { it.occurrences },
                trend = bins(groupBuckets, trendStart, TREND_BIN_HOURS, TREND_DAYS * 24 / TREND_BIN_HOURS),
            )
        }
        return LogGroupList(logAggregationService.enabled, entries, matching.size, summary)
    }

    fun detail(id: Long, now: Long = System.currentTimeMillis()): LogGroupDetail? {
        logAggregationService.flush()
        val currentHour = PendingLogGroup.hourOf(now)
        val hourlyStart = currentHour - (7 * 24 - 1) * Constants.MILLIS_PER_HOUR
        val dailyStart = currentHour - (30 * 24 - 1) * Constants.MILLIS_PER_HOUR
        val (group, buckets) = persistenceService.runReadOnly { context ->
            val group = context.find(LogGroupDO::class.java, id) ?: return@runReadOnly null
            group to context.executeNamedQuery(LogBucketDO.FIND_BY_GROUP, LogBucketDO::class.java, "groupId" to id)
                .filter { it.bucketStart!!.time >= dailyStart }
                .map { LogBucketRow(id, it.bucketStart!!, it.occurrences, it.distinctUsers) }
        } ?: return null
        val since24h = currentHour - 23 * Constants.MILLIS_PER_HOUR
        val recent = buckets.filter { it.bucketStart.time >= since24h }
        val event = LogEventRegistry.get(group.code)
        val category = group.category!!
        return LogGroupDetail(
            entry = entryOf(rowOf(group), now, recent.sumOf { it.occurrences }, bins(buckets, hourlyStart, 6, 28)),
            registered = event != null,
            explanation = event?.explanation,
            action = event?.action,
            eventNotify = event?.notify ?: category.defaultNotify,
            audience = event?.audience ?: category.defaultAudience,
            threshold = event?.threshold ?: 1,
            sampleMessage = group.sampleMessage,
            sampleStackTrace = group.sampleStackTrace,
            sampleRequest = group.sampleRequest,
            lastNotified = group.lastNotified?.time,
            reopenedAt = group.reopenedAt?.time,
            // A user of several hours is counted per hour (see LogAggregationBuffer).
            distinctUsers24h = recent.sumOf { it.distinctUsers },
            hourly = bins(buckets, hourlyStart, 1, 7 * 24),
            hourlyStart = hourlyStart,
            daily = bins(buckets, dailyStart, 24, 30),
            dailyStart = dailyStart,
        )
    }

    /**
     * @return The number of changed problems.
     * @throws IllegalArgumentException if the action or its parameter is missing.
     */
    fun update(update: LogGroupUpdate, now: Long = System.currentTimeMillis()): Int {
        val action = requireNotNull(update.action) { "No action given." }
        val change: (LogGroupDO) -> Unit = when (action) {
            LogGroupAction.ACKNOWLEDGE -> { group -> group.status = LogGroupStatus.ACKNOWLEDGED }
            LogGroupAction.IGNORE -> { group -> group.status = LogGroupStatus.IGNORED }
            LogGroupAction.RESOLVE -> { group -> group.status = LogGroupStatus.RESOLVED }
            LogGroupAction.REOPEN -> { group -> group.status = LogGroupStatus.NEW }
            LogGroupAction.MUTE -> {
                val days = requireNotNull(update.muteDays?.takeIf { it in 1..MAX_MUTE_DAYS }) {
                    "muteDays must be 1..$MAX_MUTE_DAYS."
                }
                val until = Date(now + days * Constants.MILLIS_PER_DAY);
                { group -> group.mutedUntil = until }
            }

            LogGroupAction.UNMUTE -> { group -> group.mutedUntil = null }
            LogGroupAction.SET_NOTIFY -> { group -> group.overrideNotify = update.notify }
        }
        return logAggregationService.modifyGroups(update.ids, change)
    }

    private fun matches(row: LogGroupRow, filter: LogGroupFilter, now: Long): Boolean {
        if (filter.status.statuses?.contains(row.status) == false) {
            return false
        }
        if (filter.category != null && row.category != filter.category) {
            return false
        }
        filter.days?.takeIf { it > 0 }?.let { days ->
            if (row.lastSeen.time < now - days * Constants.MILLIS_PER_DAY) {
                return false
            }
        }
        val search = filter.search?.trim()?.takeIf { it.isNotEmpty() } ?: return true
        return listOf(row.code, row.message, row.location, row.exceptionClass)
            .any { it?.contains(search, ignoreCase = true) == true }
    }

    private fun entryOf(row: LogGroupRow, now: Long, count24h: Int, trend: IntArray): LogGroupEntry {
        val mutedUntil = row.mutedUntil?.time?.takeIf { it > now }
        return LogGroupEntry(
            id = row.id,
            code = row.code,
            category = row.category,
            level = row.level,
            location = row.location,
            exceptionClass = row.exceptionClass,
            message = row.message,
            firstSeen = row.firstSeen.time,
            lastSeen = row.lastSeen.time,
            totalCount = row.totalCount,
            count24h = count24h,
            status = row.status,
            mutedUntil = mutedUntil,
            muted = row.status == LogGroupStatus.IGNORED || mutedUntil != null,
            notify = row.overrideNotify ?: LogEventRegistry.get(row.code)?.notify ?: row.category.defaultNotify,
            overrideNotify = row.overrideNotify,
            regression = isRegression(row),
            trend = trend,
        )
    }

    private fun rowOf(group: LogGroupDO) = LogGroupRow(
        group.id!!, group.code!!, group.category!!, group.level!!, group.location, group.exceptionClass,
        group.sampleMessage?.take(300), group.firstSeen!!, group.lastSeen!!, group.totalCount, group.status,
        group.mutedUntil, group.overrideNotify, group.reopenedAt,
    )

    companion object {
        const val MAX_ENTRIES = 500
        const val TREND_DAYS = 7
        const val TREND_BIN_HOURS = 6
        const val MAX_MUTE_DAYS = 365

        internal fun isRegression(row: LogGroupRow) = row.status == LogGroupStatus.NEW && row.reopenedAt != null

        /** The occurrences of the buckets in [count] bins of [binHours] hours from [start] on; older or newer ones are skipped. */
        internal fun bins(buckets: List<LogBucketRow>, start: Long, binHours: Int, count: Int): IntArray {
            val result = IntArray(count)
            val binMillis = binHours * Constants.MILLIS_PER_HOUR
            buckets.forEach { bucket ->
                val offset = bucket.bucketStart.time - start
                if (offset >= 0) {
                    val index = (offset / binMillis).toInt()
                    if (index < count) {
                        result[index] += bucket.occurrences
                    }
                }
            }
            return result
        }
    }
}
