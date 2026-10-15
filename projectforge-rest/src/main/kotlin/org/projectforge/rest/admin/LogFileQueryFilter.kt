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
import org.projectforge.framework.support.logfile.LogFileContextScope

/**
 * The free search of the admin log viewer over the log files ([AdminLogViewerRest.queryLogFile]), also stored as
 * user pref, so the class must keep its name and package (the pref is stored with its class name).
 * @param from Epoch millis, null for the beginning of today.
 * @param until Epoch millis, null for now.
 * @param period The period chosen by the frontend (`1h`, `today`, `custom` …), only kept for the next visit.
 * @param before The default number of records before a hit shown as its context.
 * @param after The default number of records after a hit shown as its context.
 */
class LogFileQueryFilter(
    var search: String? = null,
    var threshold: LogLevel = LogLevel.WARN,
    var from: Long? = null,
    var until: Long? = null,
    var period: String? = null,
    var includeLoggers: List<String> = emptyList(),
    var excludeLoggers: List<String> = emptyList(),
    var before: Int = 10,
    var after: Int = 10,
)

/**
 * The context of a hit of the log file search ([AdminLogViewerRest.logFileContext]).
 * @param file The file's name, as answered by the search ([LogFileEvent.file]), resolved against the configured
 * log files only.
 * @param line The first line of the hit ([LogFileEvent.line]).
 * @param timestampMillis The hit's timestamp, to tell whether the record at the line is still the hit.
 * @param user The hit's `user@ip`, as answered by the search.
 * @param logger The hit's logger, as answered by the search.
 */
class LogFileContextRequest(
    val file: String = "",
    val line: Int = 0,
    val timestampMillis: Long = 0,
    val user: String = "",
    val logger: String = "",
    val before: Int = 10,
    val after: Int = 10,
    val scope: LogFileContextScope = LogFileContextScope.ALL,
    val excludeLoggers: List<String> = emptyList(),
)

/**
 * The context of a log event in memory ([AdminLogViewerRest.memoryContext]), found by its [id] (the order
 * number of [LogViewerEvent.id]).
 */
class LogMemoryContextRequest(
    val id: Long = 0,
    val before: Int = 10,
    val after: Int = 10,
    val scope: LogFileContextScope = LogFileContextScope.ALL,
    val excludeLoggers: List<String> = emptyList(),
)
