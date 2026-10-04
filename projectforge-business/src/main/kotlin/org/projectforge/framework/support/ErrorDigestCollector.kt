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

import org.projectforge.common.logging.LogLevel

/**
 * What the support error digest groups by, most severe first (the order of the digest's sections).
 */
enum class ErrorCategory(val title: String) {
    /** A connection error or timeout: an external system (LDAP, IdP, gateway, Sipgate, SMTP, ...) is down. */
    EXTERNAL_UNREACHABLE("External systems not reachable"),

    /** An unexpected exception in a request of a logged-in user: a bug somebody hit. */
    REQUEST_ERROR("Unexpected errors in requests"),

    /** Logged errors with stack trace (background jobs, caught exceptions). */
    ERROR("Logged errors with stack trace"),

    /** Logged errors without stack trace. */
    ERROR_NO_TRACE("Logged errors without stack trace"),
}

/** One collected error. */
class ErrorOccurrence(
    val timestampMillis: Long,
    val level: LogLevel,
    val category: ErrorCategory,
    /** The root cause's class name, if there is an exception. */
    val exceptionClass: String?,
    val message: String?,
    /** Where it happened: the logging call site or the first ProjectForge frame, e.g. `LdapConnector:133`. */
    val location: String,
    val stackTrace: String? = null,
    val user: String? = null,
    /** Method and uri of the request, if any. */
    val request: String? = null,
)

/** All occurrences that only differ in ids, numbers and the like. */
class ErrorGroup(val key: String, first: ErrorOccurrence) {
    val category = first.category
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
    class Snapshot(val groups: List<ErrorGroup>, val dropped: Int) {
        val occurrences: Int get() = groups.sumOf { it.count }
        fun count(category: ErrorCategory) = groups.filter { it.category == category }.sumOf { it.count }
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

        internal fun keyOf(o: ErrorOccurrence): String =
            "${o.category}|${o.exceptionClass}|${o.location}|${normalize(o.message)}"
    }
}
