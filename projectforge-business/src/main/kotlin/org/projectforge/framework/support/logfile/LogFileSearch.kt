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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.Constants
import org.projectforge.common.logging.LogLevel
import org.projectforge.framework.support.ErrorDigestCollector
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.util.zip.GZIPInputStream

private val log = KotlinLogging.logger {}

/**
 * What a problem of the log aggregation is recognized by in the log file.
 * @param location `Foo:42`, the caller of the logging or the root cause's first own frame.
 * @param byCode The problem is grouped by its code, so its messages differ.
 */
class LogFileProblem(
    val level: LogLevel,
    val location: String?,
    val exceptionClass: String?,
    val sampleMessage: String?,
    val byCode: Boolean,
    val firstSeen: Long,
    val lastSeen: Long,
)

/**
 * @param records The newest occurrences found, newest first.
 * @param truncated Older occurrences may exist: the limit of hits, time or size was reached.
 * @param formatWarning Lines of the files aren't readable, so occurrences may be missing.
 * @param pattern The format the log file is written in.
 */
class LogFileSearchResult(
    val records: List<LogFileRecord>,
    val searchedFiles: List<String>,
    val truncated: Boolean,
    val formatWarning: Boolean,
    val unparsedLines: Int,
    val pattern: String?,
    val recommendedPattern: String = LogFileParser.RECOMMENDED_PATTERN,
)

/** Searches the log files for the occurrences of a problem, see [LogFileSearchService]. */
class LogFileSearch(
    private val problem: LogFileProblem,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val maxHits: Int = MAX_HITS,
    private val maxChars: Long = MAX_CHARS,
    private val maxMillis: Long = MAX_MILLIS,
) {
    private val from = problem.firstSeen - Constants.MILLIS_PER_MINUTE
    private val until = problem.lastSeen + Constants.MILLIS_PER_MINUTE
    private val normalizedSample = problem.sampleMessage?.let { ErrorDigestCollector.normalize(it) }
    private val locationClass = problem.location?.substringBefore(':')?.substringBefore('.')?.substringBefore('$')
        ?.takeIf { it.isNotBlank() && it != "?" }
    private val locationFrame = problem.location?.substringAfter(':', "")?.takeIf { it.isNotBlank() }?.let { ":$it)" }

    fun search(files: LogFiles): LogFileSearchResult {
        val started = System.currentTimeMillis()
        val result = mutableListOf<LogFileRecord>()
        val searched = mutableListOf<String>()
        var chars = 0L
        var unparsed = 0
        var unreadable = false
        var truncated = false
        for (file in candidates(files)) {
            if (truncated) break
            searched.add(file.name)
            val hits = ArrayDeque<LogFileRecord>()
            val parser = LogFileParser(file.name, zone) { record ->
                if (matches(record)) {
                    hits.addLast(record)
                    if (hits.size > maxHits - result.size) {
                        hits.removeFirst()
                        truncated = true
                    }
                }
            }
            var lines = 0
            try {
                reader(file).useLines { sequence ->
                    for (line in sequence) {
                        parser.accept(line)
                        chars += line.length + 1
                        if (++lines % 1000 == 0 && (chars > maxChars || System.currentTimeMillis() - started > maxMillis)) {
                            truncated = true
                            break
                        }
                    }
                }
                parser.finish()
            } catch (ex: IOException) {
                log.warn { "Can't read log file '${file.absolutePath}': ${ex.message}" }
                unreadable = true
            }
            unparsed += parser.unparsedStarts
            if (lines > 0 && parser.records == 0) unreadable = true
            result.addAll(hits.reversed())
            if (result.size >= maxHits) truncated = true
        }
        return LogFileSearchResult(
            records = result,
            searchedFiles = searched,
            truncated = truncated,
            formatWarning = unreadable || unparsed > 0,
            unparsedLines = unparsed,
            pattern = files.pattern,
        )
    }

    /** The current file and the rolled ones of the problem's last days, newest first. */
    internal fun candidates(files: LogFiles): List<File> {
        val start = maxOf(from, until - MAX_DAYS * Constants.MILLIS_PER_DAY)
        val firstDay = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
        val lastDay = Instant.ofEpochMilli(until).atZone(zone).toLocalDate()
        val rolled = generateSequence(lastDay) { it.minusDays(1) }.takeWhile { !it.isBefore(firstDay) }
            .mapNotNull { files.rolled(it) }
        // The current file holds the events since its last roll, possibly of several days.
        val current = files.current?.takeIf { it.lastModified() >= start }
        return listOfNotNull(current) + rolled
    }

    internal fun matches(record: LogFileRecord): Boolean {
        if (record.timestampMillis !in from..until || !levelMatches(record.level)) {
            return false
        }
        if (normalizedSample != null && ErrorDigestCollector.normalize(record.message) == normalizedSample) {
            return true
        }
        val stackTrace = record.stackTrace
        val exceptionFound = problem.exceptionClass == null || stackTrace?.contains(problem.exceptionClass) == true
        // A request's exception: located by the root cause's frame, logged by the exception handler.
        if (problem.exceptionClass != null && exceptionFound && locationClass != null && locationFrame != null &&
            stackTrace!!.lineSequence().any { it.contains(locationFrame) && it.contains(locationClass) }
        ) {
            return true
        }
        // The message isn't comparable: it differs by the code or there is no sample anymore.
        return (problem.byCode || normalizedSample == null) && exceptionFound && loggerMatches(record.logger)
    }

    /** Logback has no FATAL, written as ERROR. */
    private fun levelMatches(level: LogLevel) =
        level == problem.level || (problem.level == LogLevel.FATAL && level == LogLevel.ERROR)

    /** The logger's class, `%-40.40logger{39}` cuts its beginning if longer than 40 characters. */
    private fun loggerMatches(logger: String): Boolean {
        val cls = locationClass ?: return false
        val simple = logger.substringAfterLast('.').substringBefore('$')
        return simple == cls || (logger.length >= 40 && simple.isNotEmpty() && cls.endsWith(simple))
    }

    private fun reader(file: File) =
        if (file.name.endsWith(".gz")) {
            GZIPInputStream(file.inputStream()).bufferedReader()
        } else {
            file.bufferedReader()
        }

    companion object {
        const val MAX_HITS = 50
        const val MAX_DAYS = 7L
        const val MAX_CHARS = 300_000_000L
        const val MAX_MILLIS = 8_000L
    }
}
