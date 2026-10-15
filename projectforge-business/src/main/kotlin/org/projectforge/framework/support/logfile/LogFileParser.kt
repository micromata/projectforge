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

package org.projectforge.framework.support.logfile

import org.projectforge.common.logging.LogLevel
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A log event read from the log file.
 * @param timestampMillis Read in the time zone of the JVM, the one it was written in.
 * @param user `user@ip` with the log sessions, as written.
 * @param line The number of its first line in the file, from 1.
 */
class LogFileRecord(
    val timestampMillis: Long,
    val level: LogLevel,
    val user: String,
    val logger: String,
    val message: String,
    val stackTrace: String?,
    val file: String,
    val line: Int,
)

/**
 * Reads the records of `ProjectForge.log` written with [RECOMMENDED_PATTERN] or the [LEGACY_PATTERN] of all
 * versions until 2026, which is the same with stray dashes: before the date, before the level and on a line of its
 * own before a stack trace (Spring resolves `${LOG_LEVEL_PATTERN:-%5p}` by `:` to the default `-%5p`).
 *
 * A record starts with a line of date, level, user and logger; all following lines up to the next start are its
 * message, and its stack trace from the first line that looks like an exception on.
 *
 * Not thread-safe: one parser per file.
 */
class LogFileParser(
    private val file: String,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val consumer: (LogFileRecord) -> Unit,
) {
    /** Lines looking like the start of a record (a date first) but not matching the format. */
    var unparsedStarts = 0
        private set

    /** Records read. */
    var records = 0
        private set

    private var lineNumber = 0
    private var current: Pending? = null

    fun accept(line: String) {
        ++lineNumber
        val match = START.matchEntire(line)
        if (match != null) {
            flush()
            val (date, level, user, logger, message) = match.destructured
            current = Pending(
                timestampMillis = LocalDateTime.parse(date, DATE_FORMAT).atZone(zone).toInstant().toEpochMilli(),
                level = LogLevel.valueOf(level.trim()),
                user = user,
                logger = logger.trim(),
                message = StringBuilder(message),
                line = lineNumber,
            )
            return
        }
        if (DATE_FIRST.containsMatchIn(line)) {
            ++unparsedStarts
        }
        val pending = current ?: return // Before the first record (e.g. a file cut by its size).
        pending.add(line)
    }

    /** To be called after the last line. */
    fun finish() = flush()

    private fun flush() {
        val pending = current ?: return
        current = null
        ++records
        consumer(pending.toRecord(file))
    }

    private class Pending(
        val timestampMillis: Long,
        val level: LogLevel,
        val user: String,
        val logger: String,
        val message: StringBuilder,
        val line: Int,
    ) {
        private var stackTrace: StringBuilder? = null
        private var lines = 0

        fun add(line: String) {
            if (++lines > MAX_LINES) {
                return
            }
            val trace = stackTrace
            if (trace != null) {
                if (trace.length < MAX_CHARS) trace.append('\n').append(line)
                return
            }
            if (line == "-" && lines == 1) {
                return // Legacy: the line before a stack trace.
            }
            if (EXCEPTION.containsMatchIn(line)) {
                stackTrace = StringBuilder(line)
            } else if (message.length < MAX_CHARS) {
                message.append('\n').append(line)
            }
        }

        fun toRecord(file: String): LogFileRecord {
            // A record without stack trace ends with the empty line of `%wEx`, if any.
            val text = message.toString().trimEnd()
            return LogFileRecord(timestampMillis, level, user, logger, text, stackTrace?.toString()?.trimEnd(), file, line)
        }
    }

    companion object {
        /** The format of the log file, as recommended in `application.properties`. */
        const val RECOMMENDED_PATTERN =
            "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p %X{user}@%X{ip}%X{logSessions} %-40.40logger{39} : %m%n%wEx"

        /** The format of the versions until 2026 (see the class comment), also read. */
        const val LEGACY_PATTERN =
            "-%d{\${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}} \${LOG_LEVEL_PATTERN:-%5p} %X{user}@%X{ip}%X{logSessions} %-40.40logger{39} : %m%n\${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"

        private const val MAX_LINES = 300
        private const val MAX_CHARS = 30_000

        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        /**
         * `[---]2026-10-05 00:47:30.182 [-]ERROR kai@127.0.0.1 o.p.jcr.JCRCheckSanityCheckJob    : message`. The
         * logger is padded to 40 characters, so it is followed by at least one blank; user and ip may be empty, and
         * a request's sessions follow them as ` sessions=[http=…]` (`LoggingFilter`).
         */
        private val START = Regex(
            "-{0,3}(\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d\\.\\d{3}) -? ?(FATAL|ERROR| ?WARN| ?INFO|DEBUG|TRACE) " +
                    "(\\S*@\\S*(?: sessions=\\[[^]]*])?) (\\S.{0,39}?) +: (.*)"
        )

        private val DATE_FIRST = Regex("^-*\\d{4}-\\d\\d-\\d\\d[ T]\\d\\d:")

        /** `java.lang.IllegalStateException: …`, `Caused by: …`, `org.Foo$BarException` or a frame. */
        private val EXCEPTION =
            Regex("^(Caused by: |\\tat |\\s+at )|^[a-zA-Z_$][\\w$]*(\\.[\\w$]+)*\\.[\\w$]*(Exception|Error|Throwable)\\b")
    }
}
