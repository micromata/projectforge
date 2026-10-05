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

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate

/**
 * The log files of a day.
 * @param current The file written to now.
 * @param rolledPattern The name of a rolled file with `%d{…}` for its day (`…/ProjectForge.%d{yyyy-MM-dd}.log.gz`).
 * @param pattern The format the log events are written in, as logback has it (placeholders resolved).
 */
class LogFiles(val current: File?, val rolledPattern: String?, val pattern: String? = null) {
    /** The rolled file of the day, if it exists. Only a daily pattern of ISO dates is supported. */
    fun rolled(day: LocalDate): File? {
        val pattern = rolledPattern ?: return null
        return File(pattern.replace(DATE_TOKEN, day.toString())).takeIf { it.isFile }
    }

    private companion object {
        val DATE_TOKEN = Regex("%d(\\{[^}]*})?")
    }
}

/**
 * Finds the log file of all log events (`ROLLING-FILE-ALL` of `logback-spring.xml`) as logback writes it, so
 * wherever `logging.file.path` points to. No path comes from outside.
 */
object LogFileLocator {
    const val APPENDER_NAME = "ROLLING-FILE-ALL"

    fun locate(): LogFiles {
        val context = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return LogFiles(null, null)
        val appender = context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender(APPENDER_NAME)
                as? RollingFileAppender<*> ?: return LogFiles(null, null)
        val policy = appender.rollingPolicy as? TimeBasedRollingPolicy<*>
        return LogFiles(
            current = appender.file?.let { File(it) }?.takeIf { it.isFile },
            rolledPattern = policy?.fileNamePattern,
            pattern = (appender.encoder as? PatternLayoutEncoder)?.pattern,
        )
    }
}
