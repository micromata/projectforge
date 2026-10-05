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

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogLevel
import org.projectforge.common.logging.LogNotify

/** One collected error. */
class ErrorOccurrence(
    val timestampMillis: Long,
    val level: LogLevel,
    /** The classification: given by the logging call or the exception, else found by [ErrorOccurrenceFactory]. */
    val event: LogEvent,
    /** The root cause's class name, if there is an exception. */
    val exceptionClass: String?,
    val message: String?,
    /** Where it happened: the logging call site or the first ProjectForge frame, e.g. `LdapConnector:133`. */
    val location: String,
    val stackTrace: String? = null,
    val user: String? = null,
    /** Method and uri of the request, if any. */
    val request: String? = null,
    /**
     * True if the [event] is specific to the problem (given by the logging call or the exception): its code alone
     * identifies the group. False for the generic events of [SupportLogEvents].
     */
    val groupByCode: Boolean = false,
) {
    val category: LogCategory get() = event.category
}

/** All occurrences of one problem: the same specific event, or only differing in ids, numbers and the like. */
class ErrorGroup(val key: String, first: ErrorOccurrence) {
    val event = first.event
    val category = first.category
    val groupByCode = first.groupByCode
    val exceptionClass = first.exceptionClass
    val location = first.location
    val message = first.message
    var level = first.level
        private set
    var count = 0
        private set
    var firstMillis = first.timestampMillis
        private set
    var lastMillis = first.timestampMillis
        private set
    val users = linkedSetOf<String>()
    val samples = mutableListOf<ErrorOccurrence>()

    internal fun add(occurrence: ErrorOccurrence) {
        ++count
        if (occurrence.level.ordinal < level.ordinal) level = occurrence.level
        firstMillis = minOf(firstMillis, occurrence.timestampMillis)
        lastMillis = maxOf(lastMillis, occurrence.timestampMillis)
        occurrence.user?.takeIf { it.isNotBlank() && users.size < MAX_USERS }?.let { users.add(it) }
        if (samples.size < MAX_SAMPLES) samples.add(occurrence)
    }

    companion object {
        const val MAX_SAMPLES = 5
        const val MAX_USERS = 20
    }
}

/**
 * The errors of one digest period, grouped and bounded: at most [maxGroups] groups (further new ones are only
 * counted as dropped), each with at most [ErrorGroup.MAX_SAMPLES] samples. Thread-safe.
 */
class ErrorDigestCollector(private val maxGroups: Int = MAX_GROUPS) {
    /**
     * @param dropped Occurrences not collected because of the group limit.
     * @param suppressed Occurrences of groups not reported (below their threshold or already reported).
     */
    class Snapshot(val groups: List<ErrorGroup>, val dropped: Int, val suppressed: Int = 0) {
        val occurrences: Int get() = groups.sumOf { it.count }
        fun count(category: LogCategory) = groups.filter { it.category == category }.sumOf { it.count }
    }

    private var groups = LinkedHashMap<String, ErrorGroup>()
    private var dropped = 0

    fun add(occurrence: ErrorOccurrence) {
        val key = keyOf(occurrence)
        synchronized(this) {
            val group = groups[key] ?: if (groups.size >= maxGroups) {
                ++dropped
                return
            } else {
                ErrorGroup(key, occurrence).also { groups[key] = it }
            }
            group.add(occurrence)
        }
    }

    /** Returns everything collected, sorted by severity (category, then count), and starts a new period. */
    fun drain(): Snapshot {
        val (taken, takenDropped) = synchronized(this) {
            val result = groups to dropped
            groups = LinkedHashMap()
            dropped = 0
            result
        }
        val sorted = taken.values.sortedWith(compareBy<ErrorGroup> { it.category.ordinal }.thenByDescending { it.count })
        return Snapshot(sorted, takenDropped)
    }

    companion object {
        const val MAX_GROUPS = 500

        private val UUID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        private val HEX = Regex("\\b(?=[0-9a-fA-F]*\\d)[0-9a-fA-F]{8,}\\b")
        private val NUMBER = Regex("\\d+")

        /** Ids, numbers, uuids and hashes replaced, so the same error with other data falls into one group. */
        internal fun normalize(message: String?): String {
            message ?: return ""
            return message.replace(UUID, "#").replace(HEX, "#").replace(NUMBER, "#").take(300)
        }

        /** The fingerprint: the event's code if it is specific, else code, exception, location and message. */
        internal fun keyOf(o: ErrorOccurrence): String =
            if (o.groupByCode) {
                o.event.code
            } else {
                "${o.event.code}|${o.exceptionClass}|${o.location}|${normalize(o.message)}"
            }
    }
}


/**
 * Applies the notify rules to a drained snapshot. A group is not reported if
 * - its problem is [ignored or muted][LogGroupState.muted] by an admin,
 * - its notify rule is [LogNotify.NONE]: the admin's [override][LogGroupState.overrideNotify], else the event's,
 * - it is below its event's [threshold][LogEvent.threshold],
 * - its rule is [LogNotify.DIGEST_IF_NEW] and it was reported before, unless it reoccurred after it was resolved.
 *
 * "Reported before" is taken from the database ([states], it survives restarts); for a problem not yet written there
 * (or without database) from memory, since start-up and for at most [maxRemembered] groups.
 */
class DigestNotifyFilter(
    private val states: LogGroupStates? = null,
    private val maxRemembered: Int = 10_000,
) {
    private val reported = object : LinkedHashMap<String, Boolean>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) = size > maxRemembered
    }

    @Synchronized
    fun apply(snapshot: ErrorDigestCollector.Snapshot, now: Long = System.currentTimeMillis()): ErrorDigestCollector.Snapshot {
        val (reportable, suppressed) = snapshot.groups.partition { group ->
            val state = states?.stateOf(group.key)
            val notify = state?.overrideNotify ?: group.event.notify
            when {
                state?.muted(now) == true || notify == LogNotify.NONE -> false
                group.count < group.event.threshold -> false
                notify != LogNotify.DIGEST_IF_NEW -> true
                state != null -> state.unreported
                else -> reported[group.key] == null
            }
        }
        reportable.forEach { reported[it.key] = true }
        states?.markNotified(reportable.map { it.key })
        return ErrorDigestCollector.Snapshot(reportable, snapshot.dropped, snapshot.suppressed + suppressed.sumOf { it.count })
    }

    companion object {
        /** The notify rule of an occurrence: the admin's override of its problem, else the one of its event. */
        fun notifyOf(occurrence: ErrorOccurrence, state: LogGroupState?): LogNotify =
            state?.overrideNotify ?: occurrence.event.notify
    }
}
