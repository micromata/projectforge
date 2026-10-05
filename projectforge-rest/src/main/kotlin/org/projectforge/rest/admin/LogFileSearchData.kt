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

package org.projectforge.rest.admin

import org.projectforge.common.logging.LogLevel
import org.projectforge.framework.support.logfile.LogFileRecord
import org.projectforge.framework.support.logfile.LogFileSearchResult
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** A record of the log file in the shape of [LogViewerEvent], so the frontend shows it with the same columns. */
class LogFileEvent(val id: Int, record: LogFileRecord) {
    /** UTC, as the admin log viewer. */
    val timestamp: String = TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(record.timestampMillis))
    val level: LogLevel = record.level
    val message = record.message
    val stackTrace = record.stackTrace
    val user = record.user
    val logger = record.logger

    /** Where it is found: `ProjectForge.2026-10-05.log.gz:1234`. */
    val source = "${record.file}:${record.line}"

    private companion object {
        val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
    }
}

/** `GET /rs/adminErrors/logFile`: the occurrences of a problem in the log files, see [LogFileSearchResult]. */
class LogFileSearchData(result: LogFileSearchResult) {
    val entries = result.records.mapIndexed { index, record -> LogFileEvent(index, record) }
    val searchedFiles = result.searchedFiles
    val truncated = result.truncated
    val formatWarning = result.formatWarning
    val unparsedLines = result.unparsedLines
    val pattern = result.pattern
    val recommendedPattern = result.recommendedPattern
}
