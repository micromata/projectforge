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

import io.github.oshai.kotlinlogging.KLogger
import org.slf4j.Logger
import org.slf4j.spi.CallerBoundaryAware
import org.slf4j.spi.LoggingEventBuilder

// Logging calls with a [LogEvent], passed as SLF4J key-value [LogEvent.KEY].
// Inline, so the caller (not this file) is the location of the log message.

inline fun KLogger.error(event: LogEvent, throwable: Throwable? = null, crossinline message: () -> Any?) =
    atError {
        this.message = logMessage(message)
        cause = throwable
        payload = mapOf(LogEvent.KEY to event)
    }

inline fun KLogger.warn(event: LogEvent, throwable: Throwable? = null, crossinline message: () -> Any?) =
    atWarn {
        this.message = logMessage(message)
        cause = throwable
        payload = mapOf(LogEvent.KEY to event)
    }

inline fun KLogger.info(event: LogEvent, throwable: Throwable? = null, crossinline message: () -> Any?) =
    atInfo {
        this.message = logMessage(message)
        cause = throwable
        payload = mapOf(LogEvent.KEY to event)
    }

/** A failing message lambda must not break the logging call (as with kotlin-logging's own calls). */
@PublishedApi
internal inline fun logMessage(message: () -> Any?): String? =
    try {
        message()?.toString()
    } catch (ex: Exception) {
        "Log message invocation failed: $ex"
    }

/**
 * Logging calls with a [LogEvent] for Java (and other users of a plain SLF4J [Logger]):
 * `PfLog.error(log, MailLogEvents.SEND_FAILED, "Sending failed", ex)`.
 */
object PfLog {
    @JvmStatic
    @JvmOverloads
    fun error(log: Logger, event: LogEvent, message: String?, throwable: Throwable? = null) =
        log(log.atError(), event, message, throwable)

    @JvmStatic
    @JvmOverloads
    fun warn(log: Logger, event: LogEvent, message: String?, throwable: Throwable? = null) =
        log(log.atWarn(), event, message, throwable)

    @JvmStatic
    @JvmOverloads
    fun info(log: Logger, event: LogEvent, message: String?, throwable: Throwable? = null) =
        log(log.atInfo(), event, message, throwable)

    private fun log(builder: LoggingEventBuilder, event: LogEvent, message: String?, throwable: Throwable?) {
        // The location of the message is the caller of PfLog.
        (builder as? CallerBoundaryAware)?.setCallerBoundary(PfLog::class.java.name)
        builder.addKeyValue(LogEvent.KEY, event).setCause(throwable).log(message)
    }
}
