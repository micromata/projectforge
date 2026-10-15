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
import org.projectforge.common.logging.LogLevel

/**
 * The occurrences of one problem since the last flush of [LogAggregationBuffer].
 * @param key The group key of [ErrorDigestCollector.keyOf].
 */
class PendingLogGroup internal constructor(val key: String, first: ErrorOccurrence) {
    val event = first.event
    val exceptionClass = first.exceptionClass
    val location = first.location

    var level: LogLevel = first.level
        private set
    var count = 0L
        private set
    var firstMillis = first.timestampMillis
        private set
    var lastMillis = first.timestampMillis
        private set

    /** The latest occurrence, but one with a stack trace is preferred. */
    var sample: ErrorOccurrence = first
        private set

    /** Occurrences per hour (start of the hour in millis). */
    val hours = sortedMapOf<Long, Int>()

    /** Distinct users per hour, as far as known: set by [LogAggregationBuffer.drain]. */
    var distinctUsers: Map<Long, Int> = emptyMap()
        internal set

    internal fun add(occurrence: ErrorOccurrence) {
        ++count
        if (occurrence.level.ordinal < level.ordinal) level = occurrence.level
        firstMillis = minOf(firstMillis, occurrence.timestampMillis)
        lastMillis = maxOf(lastMillis, occurrence.timestampMillis)
        if (occurrence.stackTrace != null || sample.stackTrace == null) sample = occurrence
        hours.merge(hourOf(occurrence.timestampMillis), 1, Int::plus)
    }

    /** Adds the counts of a pending group not written to the database (see [LogAggregationBuffer.restore]). */
    internal fun merge(other: PendingLogGroup) {
        count += other.count
        if (other.level.ordinal < level.ordinal) level = other.level
        firstMillis = minOf(firstMillis, other.firstMillis)
        lastMillis = maxOf(lastMillis, other.lastMillis)
        if (sample.stackTrace == null && other.sample.stackTrace != null) sample = other.sample
        other.hours.forEach { (hour, n) -> hours.merge(hour, n, Int::plus) }
    }

    companion object {
        fun hourOf(millis: Long): Long = millis - Math.floorMod(millis, Constants.MILLIS_PER_HOUR)
    }
}

/**
 * Collects the occurrences between two database writes of [LogAggregationService], grouped by
 * [ErrorDigestCollector.keyOf] and bounded: at most [maxGroups] groups, further new ones are only counted as
 * [dropped]. Thread-safe.
 *
 * The distinct users of an hour are counted over all flushes of the hour (the database only gets the number, no
 * names): for the current and the previous hour, at most [maxUsersPerHour] each.
 */
class LogAggregationBuffer(
    private val maxGroups: Int = MAX_GROUPS,
    private val maxUsersPerHour: Int = MAX_USERS_PER_HOUR,
) {
    private var pending = LinkedHashMap<String, PendingLogGroup>()

    /** Key -> hour -> users. */
    private val users = HashMap<String, MutableMap<Long, MutableSet<String>>>()

    /** Occurrences lost because of the group limit, since start-up. */
    var dropped = 0L
        private set

    val size: Int
        @Synchronized get() = pending.size

    @Synchronized
    fun add(occurrence: ErrorOccurrence, key: String = ErrorDigestCollector.keyOf(occurrence)) {
        val group = pending[key] ?: if (pending.size >= maxGroups) {
            ++dropped
            return
        } else {
            PendingLogGroup(key, occurrence).also { pending[key] = it }
        }
        group.add(occurrence)
        occurrence.user?.takeIf { it.isNotBlank() }?.let { user ->
            val set = users.getOrPut(key) { HashMap() }
                .getOrPut(PendingLogGroup.hourOf(occurrence.timestampMillis)) { HashSet() }
            if (set.size < maxUsersPerHour) set.add(user)
        }
    }

    /** Returns everything since the last call, with the distinct users per hour, and starts anew. */
    @Synchronized
    fun drain(now: Long = System.currentTimeMillis()): List<PendingLogGroup> {
        val result = pending.values.toList()
        pending = LinkedHashMap()
        result.forEach { group ->
            val hourUsers = users[group.key]
            group.distinctUsers = group.hours.keys.associateWith { hourUsers?.get(it)?.size ?: 0 }
        }
        // Users of older hours aren't needed any more: their buckets are complete.
        val previousHour = PendingLogGroup.hourOf(now) - Constants.MILLIS_PER_HOUR
        users.values.forEach { byHour -> byHour.keys.removeIf { it < previousHour } }
        users.values.removeIf { it.isEmpty() }
        return result
    }

    /** Puts back what couldn't be written, as far as there is room: the next flush tries again. */
    @Synchronized
    fun restore(groups: List<PendingLogGroup>) {
        groups.forEach { group ->
            val current = pending[group.key]
            if (current != null) {
                current.merge(group)
            } else if (pending.size < maxGroups) {
                pending[group.key] = group
            } else {
                dropped += group.count
            }
        }
    }

    companion object {
        const val MAX_GROUPS = 1000
        const val MAX_USERS_PER_HOUR = 100
    }
}
