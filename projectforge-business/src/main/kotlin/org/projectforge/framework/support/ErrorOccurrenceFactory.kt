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
import org.projectforge.common.logging.LoggingEventData
import org.projectforge.framework.integration.IntegrationErrors
import org.projectforge.framework.utils.ExceptionStackTracePrinter

/**
 * Turns log events and request exceptions into [ErrorOccurrence]s, or drops them where they don't belong into
 * the support error digest.
 */
object ErrorOccurrenceFactory {
    /**
     * Loggers whose events are not taken from the log: the REST exception handler reports its errors itself
     * (with the request, and only the unexpected ones), and the digest must not collect its own errors.
     */
    private val IGNORED_LOGGERS = listOf(
        "org.projectforge.rest.core.GlobalDefaultExceptionHandler",
        "org.projectforge.framework.support.",
    )

    /**
     * Text patterns of an unreachable remote system, for log events (only a stack trace text, no exception):
     * the classes [IntegrationErrors.isConnectionError] checks, and the messages of the clients that only log.
     */
    private val CONNECTION_PATTERNS = listOf(
        "ConnectException", "UnknownHostException", "NoRouteToHostException", "SocketTimeoutException",
        "ConnectTimeoutException", "TimeoutException", "HttpTimeoutException", "ReadTimeoutException",
        "not reachable", "unreachable", "connection refused", "timed out", "timeout",
    )

    /**
     * Null if the event isn't collected: ERROR is, WARN only where it looks like an unreachable system.
     */
    fun fromLogEvent(event: LoggingEventData): ErrorOccurrence? {
        val loggerName = event.loggerName ?: ""
        if (IGNORED_LOGGERS.any { loggerName.startsWith(it) }) {
            return null
        }
        val stackTrace = event.stackTrace
        val connectionError = isConnectionText(event.message) || isConnectionText(stackTrace?.let { exceptionLines(it) })
        val category = when {
            connectionError -> ErrorCategory.EXTERNAL_UNREACHABLE
            !event.level.matches(LogLevel.ERROR) -> return null
            stackTrace != null -> ErrorCategory.ERROR
            else -> ErrorCategory.ERROR_NO_TRACE
        }
        return ErrorOccurrence(
            timestampMillis = event.timestampMillis,
            level = event.level,
            category = category,
            exceptionClass = stackTrace?.let { rootExceptionClass(it) },
            message = event.message,
            location = "${event.javaClassSimpleName ?: event.javaClass}:${event.lineNumber}",
            stackTrace = stackTrace,
            user = event.user,
        )
    }

    /**
     * An exception of a request, already found worth reporting by the caller.
     * @param external True for a remote system that isn't reachable (connection error, timeout).
     */
    fun fromRequestException(
        ex: Throwable,
        user: String?,
        request: String?,
        external: Boolean = IntegrationErrors.isConnectionError(ex),
        timestampMillis: Long = System.currentTimeMillis(),
    ): ErrorOccurrence {
        val rootCause = generateSequence(ex) { it.cause.takeIf { cause -> cause !== it } }.take(20).last()
        val frame = rootCause.stackTrace.firstOrNull { it.className.startsWith("org.projectforge.") }
            ?: rootCause.stackTrace.firstOrNull()
        return ErrorOccurrence(
            timestampMillis = timestampMillis,
            level = LogLevel.ERROR,
            category = if (external) ErrorCategory.EXTERNAL_UNREACHABLE else ErrorCategory.REQUEST_ERROR,
            exceptionClass = rootCause::class.java.name,
            message = rootCause.message ?: ex.message,
            location = frame?.let { "${it.className.substringAfterLast('.')}:${it.lineNumber}" } ?: "?",
            stackTrace = ExceptionStackTracePrinter.toString(ex, stopBeforeForeignPackages = false, depth = 40),
            user = user,
            request = request,
        )
    }

    internal fun isConnectionText(text: String?): Boolean {
        text ?: return false
        return CONNECTION_PATTERNS.any { text.contains(it, ignoreCase = true) }
    }

    /**
     * The exception lines of a stack trace text (`Foo: message`, `Caused by: ...`), without the frames: a frame
     * like `withTimeout` says nothing about the error.
     */
    internal fun exceptionLines(stackTrace: String): String =
        stackTrace.lineSequence().map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("at ") && !it.startsWith("...") && !it.startsWith("Suppressed:") }
            .joinToString("\n")

    /**
     * The class of the innermost exception in a stack trace text: the last `Caused by:` line, else the first line.
     */
    internal fun rootExceptionClass(stackTrace: String): String? {
        val lines = stackTrace.lineSequence().map { it.trim() }
        val line = lines.lastOrNull { it.startsWith("Caused by: ") }?.removePrefix("Caused by: ")
            ?: lines.firstOrNull { it.isNotBlank() }
            ?: return null
        return line.substringBefore(':').trim().takeIf { it.isNotEmpty() && !it.contains(' ') }
    }
}
