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

import org.projectforge.framework.persistence.jpa.PfPersistenceContext

/**
 * Merges the problems ([LogGroupDO]) whose group key changed with a refined [ErrorDigestCollector.normalize], e.g.
 * one problem per user name before quoted values were normalized: their occurrences would otherwise stay apart
 * from the ones counted from now on, until the cleanup removes them.
 *
 * The key is rebuilt from the stored code, exception, location and sample message. Not rebuilt are problems
 * grouped by their code alone (their fingerprint is the code's) and problems without sample (cleared by the
 * cleanup).
 *
 * Counts and hourly buckets are summed up into one problem: the one already having the new fingerprint, else the
 * oldest. The admin's settings (status, muted, notify rule) are kept if all merged problems agree, else the
 * problem is new again, to be looked at once more. When it was reported last is kept, so the digest doesn't report
 * it as new.
 */
internal object LogGroupMerger {
    /** @return The number of problems merged into others (removed). */
    fun merge(context: PfPersistenceContext): Int {
        val rows = context.executeNamedQuery(LogGroupDO.SELECT_KEYS, LogGroupKeyRow::class.java)
        val byFingerprint = rows.associateBy { it.fingerprint }
        val moves = rows.mapNotNull { row ->
            row.target()?.takeIf { it != row.fingerprint }?.let { row to it }
        }
        var removed = 0
        moves.groupBy({ it.second }, { it.first }).forEach { (target, movedRows) ->
            val ids = (listOfNotNull(byFingerprint[target]) + movedRows).map { it.id }.distinct()
            removed += mergeGroups(context, target, ids)
        }
        return removed
    }

    /** The fingerprint by the current normalization, null if not to be rebuilt. */
    private fun LogGroupKeyRow.target(): String? {
        if (message == null || fingerprint == LogAggregationService.fingerprintOf(code)) {
            return null
        }
        return LogAggregationService.fingerprintOf(
            "$code|$exceptionClass|$location|${ErrorDigestCollector.normalize(message)}"
        )
    }

    private fun mergeGroups(context: PfPersistenceContext, target: String, ids: List<Long>): Int {
        val groups = ids.mapNotNull { context.find(LogGroupDO::class.java, it, attached = true) }
        if (groups.isEmpty()) {
            return 0
        }
        val survivor = groups.find { it.fingerprint == target } ?: groups.minBy { it.firstSeen!! }
        val others = groups - survivor
        others.forEach { group ->
            survivor.totalCount += group.totalCount
            if (group.firstSeen!!.before(survivor.firstSeen)) survivor.firstSeen = group.firstSeen
            if (group.level!!.ordinal < survivor.level!!.ordinal) survivor.level = group.level
        }
        val latest = groups.filter { it.sampleMessage != null }.maxByOrNull { it.lastSeen!! }
        if (latest != null && latest !== survivor) {
            survivor.sampleMessage = latest.sampleMessage
            survivor.sampleStackTrace = latest.sampleStackTrace
            survivor.sampleRequest = latest.sampleRequest
        }
        survivor.lastSeen = groups.maxOf { it.lastSeen!! }
        survivor.lastNotified = groups.mapNotNull { it.lastNotified }.maxOrNull()
        survivor.reopenedAt = groups.mapNotNull { it.reopenedAt }.maxOrNull()
        survivor.status = groups.map { it.status }.distinct().singleOrNull() ?: LogGroupStatus.NEW
        survivor.overrideNotify = groups.map { it.overrideNotify }.distinct().singleOrNull()
        survivor.mutedUntil = if (groups.all { it.mutedUntil != null }) groups.minOf { it.mutedUntil!! } else null
        survivor.fingerprint = target
        mergeBuckets(context, survivor.id!!, others.map { it.id!! })
        others.forEach { context.delete(it) }
        return others.size
    }

    /** Moves the buckets to the survivor, adding up the ones of the same hour. */
    private fun mergeBuckets(context: PfPersistenceContext, survivorId: Long, otherIds: List<Long>) {
        val byStart = buckets(context, survivorId).associateByTo(mutableMapOf()) { it.bucketStart!! }
        otherIds.flatMap { buckets(context, it) }.forEach { bucket ->
            val existing = byStart[bucket.bucketStart!!]
            if (existing == null) {
                bucket.groupId = survivorId
                byStart[bucket.bucketStart!!] = bucket
            } else {
                existing.occurrences += bucket.occurrences
                // The users of both may overlap: at least the larger count.
                existing.distinctUsers = maxOf(existing.distinctUsers, bucket.distinctUsers)
                context.delete(bucket)
            }
        }
    }

    private fun buckets(context: PfPersistenceContext, groupId: Long): List<LogBucketDO> = context.executeNamedQuery(
        LogBucketDO.FIND_BY_GROUP, LogBucketDO::class.java, "groupId" to groupId, attached = true,
    )
}

/** What a problem's group key is rebuilt from, see [LogGroupMerger]. */
class LogGroupKeyRow(
    val id: Long,
    val fingerprint: String,
    val code: String,
    val exceptionClass: String?,
    val location: String?,
    val message: String?,
)
