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

package org.projectforge.common.logging

/**
 * Who has to act on a [LogEvent]: the recipients of its notifications are configured per audience.
 */
enum class LogAudience {
    /** A bug or a data problem: the developers have to look at the code. */
    DEVELOPER,

    /** Operations: configuration, external systems, infrastructure. */
    ADMIN,

    /** Suspicious access, failed authentications. */
    SECURITY,
}

/**
 * Whether and when a [LogEvent] is reported to its [LogAudience]. Independent of the log level: the level says how
 * severe a problem is, this says whether somebody has to be told.
 */
enum class LogNotify {
    /** Counted only (dashboard, log viewer), never mailed. */
    NONE,

    /** Part of every error digest of the period it occurs in. */
    DIGEST,

    /** Part of the error digest only the first time it occurs (since start-up, until it's persisted). */
    DIGEST_IF_NEW,

    /** Reported as soon as possible: sends the error digest early (rate-limited). */
    IMMEDIATE,
}

/**
 * What kind of problem a [LogEvent] is. The order is the order of the error digest's sections.
 */
enum class LogCategory(
    val title: String,
    val defaultNotify: LogNotify,
    val defaultAudience: LogAudience,
) {
    /** A remote system (LDAP, IdP, Sipgate, SMTP, d.velop, ...) isn't reachable or fails. */
    EXTERNAL("External systems not reachable or failing", LogNotify.DIGEST, LogAudience.ADMIN),

    /** Suspicious requests, failed authentications, locked accounts. */
    SECURITY("Security", LogNotify.DIGEST, LogAudience.SECURITY),

    /** Inconsistent or corrupt data in the database or in files. */
    DATA("Data problems", LogNotify.DIGEST, LogAudience.DEVELOPER),

    /** Unexpected errors: a bug. */
    BUG("Unexpected errors", LogNotify.DIGEST, LogAudience.DEVELOPER),

    /** A missing or wrong configuration of this installation. */
    CONFIG("Configuration problems", LogNotify.DIGEST, LogAudience.ADMIN),

    /** A client got something wrong (bad request, validation error, expired session): nothing to do on the server. */
    CLIENT("Client errors", LogNotify.NONE, LogAudience.DEVELOPER),

    /** Not classified at all (e.g. a plain WARN). */
    UNCLASSIFIED("Unclassified", LogNotify.NONE, LogAudience.DEVELOPER),
}

/**
 * The classification of a log message, given where it is logged: what it is ([category]), whether somebody has to be
 * told ([notify], [audience], [threshold]), what it means ([explanation]) and what to do about it ([action]).
 *
 * The events of an area are defined in one `object` of its package (e.g. `MailLogEvents`, with `@JvmField` for Java)
 * and logged with `log.error(MailLogEvents.SEND_FAILED, ex) { "..." }` (Kotlin, see `LogEventLogging.kt`) or
 * `PfLog.error(log, MailLogEvents.SEND_FAILED, "...", ex)` (Java). The event travels as the SLF4J key-value [KEY], so
 * the log message itself stays short; exceptions may carry their event themselves ([LogEventAware]).
 *
 * Explanation and action are English texts for the support team, not translated.
 */
class LogEvent @JvmOverloads constructor(
    /** Stable id, e.g. `sipgate.sync.failed`: the fingerprint of the error group, the key of admin overrides. */
    val code: String,
    val category: LogCategory,
    val notify: LogNotify = category.defaultNotify,
    val audience: LogAudience = category.defaultAudience,
    /** What does it mean? */
    val explanation: String? = null,
    /** What has to be done? */
    val action: String? = null,
    /** Reported only from this number of occurrences within one digest period on (spikes, e.g. failed logins). */
    val threshold: Int = 1,
) {
    init {
        require(CODE_REGEX.matches(code)) { "Invalid log event code '$code', expected e.g. 'sipgate.sync.failed'." }
        require(threshold >= 1) { "Threshold of log event '$code' must be at least 1: $threshold" }
    }

    /** The code: the value of [KEY] in log patterns with `%kvp`. */
    override fun toString(): String = code

    companion object {
        /** The SLF4J key of the [LogEvent] of a logging call. */
        const val KEY = "pf.event"

        private val CODE_REGEX = Regex("[a-z][a-zA-Z0-9]*(\\.[a-zA-Z0-9_-]+)+")
    }
}
