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
import java.io.IOException
import java.time.ZoneId

private val log = KotlinLogging.logger {}

/** Which of the neighbours of a hit are its context. */
enum class LogFileContextScope {
    /** All records around it (but those of the excluded loggers). */
    ALL,

    /** Those of the same `user@ip`, so the requests of other users running at the same time are left out. */
    USER,

    /** Those of the same logger. */
    LOGGER,
}

/**
 * A hit with its context in the log file.
 * @param hit null, if the record isn't found at its position (anymore), e.g. the current file was rolled since.
 * @param before The records before the hit, oldest first.
 * @param after The records after the hit, oldest first.
 * @param moreBefore There are more records of the scope before, in the same file.
 * @param moreAfter There are more records of the scope after, in the same file.
 * @param truncated The time or size limit was reached before all context was read.
 */
class LogFileContextResult(
    val hit: LogFileRecord?,
    val before: List<LogFileRecord> = emptyList(),
    val after: List<LogFileRecord> = emptyList(),
    val moreBefore: Boolean = false,
    val moreAfter: Boolean = false,
    val truncated: Boolean = false,
)

/**
 * Reads the context of a hit of a [LogFileSearch]: the [before] records before and the [after] records after
 * the record starting at [line] of the log file, of the [scope] and not of the [excludeLoggers]. The hit is
 * identified by its file, line and timestamp, so a file rolled meanwhile isn't mistaken for the one searched.
 * Its [user] and [logger] (as found by the search) are needed beforehand, to keep only the records of the scope
 * while reading the file up to the hit.
 * Only within the hit's file: the context doesn't continue in the file of the day before or after.
 */
class LogFileContext(
    private val line: Int,
    private val timestampMillis: Long,
    private val user: String,
    private val logger: String,
    before: Int,
    after: Int,
    private val scope: LogFileContextScope = LogFileContextScope.ALL,
    excludeLoggers: List<String> = emptyList(),
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val maxChars: Long = LogFileSearch.MAX_CHARS,
    private val maxMillis: Long = LogFileSearch.MAX_MILLIS,
) {
    private val before = before.coerceIn(0, MAX_RECORDS)
    private val after = after.coerceIn(0, MAX_RECORDS)
    private val exclude = excludeLoggers.mapNotNull { LoggerPattern.of(it) }

    /** @return null, if there is no such file. */
    fun read(files: LogFiles, fileName: String): LogFileContextResult? {
        val file = files.byName(fileName) ?: return null
        val started = System.currentTimeMillis()
        // One more than asked for, to know whether there are more.
        val previous = ArrayDeque<LogFileRecord>()
        val following = mutableListOf<LogFileRecord>()
        var hit: LogFileRecord? = null
        var done = false
        val parser = LogFileParser(file.name, zone) { record ->
            val found = hit
            when {
                done -> {}
                found == null && record.line == line -> {
                    if (record.timestampMillis == timestampMillis) hit = record else done = true
                }
                found == null && record.line > line -> done = true // No record starts at the line.
                found == null -> if (inScope(record)) {
                    previous.addLast(record)
                    if (previous.size > before + 1) previous.removeFirst()
                }
                inScope(record) -> {
                    following.add(record)
                    if (following.size > after) done = true
                }
            }
        }
        var chars = 0L
        var lines = 0
        var truncated = false
        try {
            LogFileSearch.reader(file).useLines { sequence ->
                for (text in sequence) {
                    parser.accept(text)
                    if (done) break
                    chars += text.length + 1
                    if (++lines % 1000 == 0 && (chars > maxChars || System.currentTimeMillis() - started > maxMillis)) {
                        truncated = true
                        break
                    }
                }
            }
            if (!done && !truncated) parser.finish()
        } catch (ex: IOException) {
            log.warn { "Can't read log file '${file.absolutePath}': ${ex.message}" }
            return LogFileContextResult(hit = null)
        }
        val found = hit ?: return LogFileContextResult(hit = null, truncated = truncated)
        return LogFileContextResult(
            hit = found,
            before = previous.toList().takeLast(before),
            after = following.take(after),
            moreBefore = previous.size > before,
            moreAfter = following.size > after,
            truncated = truncated,
        )
    }

    private fun inScope(record: LogFileRecord): Boolean {
        if (exclude.any { it.matches(record.logger) }) {
            return false
        }
        return when (scope) {
            LogFileContextScope.ALL -> true
            LogFileContextScope.USER -> userOf(record.user) == userOf(user)
            LogFileContextScope.LOGGER -> record.logger == logger
        }
    }

    /** `user@ip` without the sessions of the request. */
    private fun userOf(user: String) = user.substringBefore(' ')

    companion object {
        const val MAX_RECORDS = 1000
    }
}
