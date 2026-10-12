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

/**
 * The free search of the admin log viewer over the log files: the records of a time window of [threshold] or
 * higher, matching all [search] terms and of the included, not of the excluded loggers (see [LoggerPattern]).
 */
class LogFileQuery(
    search: String?,
    val threshold: LogLevel,
    override val from: Long,
    override val until: Long,
    includeLoggers: List<String> = emptyList(),
    excludeLoggers: List<String> = emptyList(),
) : LogFileMatcher {
    private val terms = SearchTerms.parse(search)
    private val include = includeLoggers.mapNotNull { LoggerPattern.of(it) }
    private val exclude = excludeLoggers.mapNotNull { LoggerPattern.of(it) }

    override val maxDays: Long
        get() = MAX_DAYS

    override fun matches(record: LogFileRecord): Boolean {
        // Logback has no FATAL, written as ERROR.
        val threshold = if (threshold == LogLevel.FATAL) LogLevel.ERROR else threshold
        return record.level.matches(threshold) &&
                (include.isEmpty() || include.any { it.matches(record.logger) }) &&
                exclude.none { it.matches(record.logger) } &&
                terms.matches(record)
    }

    companion object {
        /** A free search reaches further back than the one of a problem, the time budget limits it anyway. */
        const val MAX_DAYS = 31L

        const val MAX_HITS = 200
    }
}

/**
 * The terms of a search: all of them must be found (case-insensitive) in the message, logger, user or stack trace
 * of a record, those with a leading `-` must not. `"…"` quotes a phrase with blanks.
 */
class SearchTerms private constructor(val required: List<String>, val excluded: List<String>) {
    fun matches(record: LogFileRecord): Boolean =
        required.all { contains(record, it) } && excluded.none { contains(record, it) }

    private fun contains(record: LogFileRecord, term: String) =
        record.message.contains(term, ignoreCase = true) ||
                record.logger.contains(term, ignoreCase = true) ||
                record.user.contains(term, ignoreCase = true) ||
                record.stackTrace?.contains(term, ignoreCase = true) == true

    companion object {
        private val TOKEN = Regex("(-?)(?:\"([^\"]*)\"?|(\\S+))")

        fun parse(search: String?): SearchTerms {
            val required = mutableListOf<String>()
            val excluded = mutableListOf<String>()
            TOKEN.findAll(search ?: "").forEach { match ->
                val (minus, phrase, word) = match.destructured
                val term = phrase.ifEmpty { word }
                when {
                    term.isEmpty() || term == "-" -> {}
                    minus.isNotEmpty() -> excluded.add(term)
                    else -> required.add(term)
                }
            }
            return SearchTerms(required, excluded)
        }
    }
}

/**
 * A logger category as entered, e.g. `org.projectforge.business.fibu` or `org.hibernate`, compared with the
 * loggers as written into the log file: `%-40.40logger{39}` abbreviates their leading packages to one letter
 * (`o.p.b.fibu.RechnungDao`), so a segment matches its abbreviation, either way. With dots, the pattern's segments
 * must start the logger's (a package and all below it, or a class); a single word matches a segment of the
 * logger or a part of its class name (`hibernate`, `RechnungDao`). Case-insensitive.
 */
class LoggerPattern private constructor(private val segments: List<String>) {
    fun matches(logger: String): Boolean {
        val loggerSegments = logger.split('.')
        if (segments.size == 1) {
            val word = segments[0]
            return loggerSegments.dropLast(1).any { it.equals(word, ignoreCase = true) } ||
                    loggerSegments.last().contains(word, ignoreCase = true)
        }
        if (segments.size > loggerSegments.size) {
            return false
        }
        return segments.indices.all { segmentMatches(segments[it], loggerSegments[it]) }
    }

    private fun segmentMatches(pattern: String, segment: String) =
        pattern.equals(segment, ignoreCase = true) ||
                (segment.length == 1 && pattern.startsWith(segment, ignoreCase = true)) ||
                (pattern.length == 1 && segment.startsWith(pattern, ignoreCase = true))

    companion object {
        /** @return null for a blank pattern. */
        fun of(pattern: String): LoggerPattern? {
            val segments = pattern.trim().trim('.').split('.').filter { it.isNotBlank() }
            return if (segments.isEmpty()) null else LoggerPattern(segments)
        }
    }
}
