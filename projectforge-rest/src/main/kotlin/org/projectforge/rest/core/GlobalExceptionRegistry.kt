/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.core

import org.projectforge.common.logging.LogLevel
import org.projectforge.framework.i18n.InternalErrorException
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException

/**
 * Registry for known exceptions. For known exceptions, the log level, status and message can be defined. You may
 * also define a match function to match the exception as well as if an email should be sent to the developers.
 * This class is thread-safe.
 */
object GlobalExceptionRegistry {
    open class ExInfo(
        val message: String? = null,
        val logLevel: LogLevel = LogLevel.ERROR,
        val status: HttpStatus = HttpStatus.BAD_REQUEST,
        val knownException: Class<*>? = null,
        val knownExceptionName: String? = null,
        val knownExceptionMessagePart: String? = null,
        val sendMailToDevelopers: Boolean = false,
        val match: ((ex: Throwable) -> Boolean)? = null
    ) {
        /**
         * @param ex The exception to check.
         * @return True if the exception matches this ExInfo (all given values: exception, exceptionName and messagePart, if given).
         */
        open fun match(ex: Throwable): Boolean {
            return match?.invoke(ex)
                ?: (matchKnownException(ex) && matchKnownExceptionName(ex) && matchKnownExceptionMessagePart(ex))
        }

        private fun matchKnownException(ex: Throwable): Boolean {
            return knownException == null || knownException.isAssignableFrom(ex::class.java)
        }

        private fun matchKnownExceptionName(ex: Throwable): Boolean {
            return knownExceptionName == null || ex::class.qualifiedName?.contains(knownExceptionName) == true
        }

        private fun matchKnownExceptionMessagePart(ex: Throwable): Boolean {
            return knownExceptionMessagePart == null || ex.message?.contains(
                knownExceptionMessagePart,
                ignoreCase = true
            ) == true
        }
    }

    private val knownExceptions = mutableListOf<ExInfo>()

    @JvmStatic
    @JvmOverloads
    fun registerExInfo(
        knownException: Class<*>,
        message: String? = null,
        logLevel: LogLevel = LogLevel.ERROR,
        status: HttpStatus = HttpStatus.BAD_REQUEST,
        knownExceptionMessagePart: String? = null,
        match: ((ex: Throwable) -> Boolean)? = null,
    ) {
        registerExInfo(
            ExInfo(
                message = message,
                logLevel = logLevel,
                status = status,
                knownException = knownException,
                knownExceptionMessagePart = knownExceptionMessagePart,
                match = match,
            )
        )
    }

    fun registerExInfo(exInfo: ExInfo) {
        synchronized(knownExceptions) {
            knownExceptions.add(exInfo)
        }
    }

    init {
        registerExInfo(ConnectException::class.java, "Connection exception")
        registerExInfo(NoRouteToHostException::class.java, "No route to host")
        registerExInfo(AsyncRequestNotUsableException::class.java, "Asyn client connection lost (OK)", LogLevel.INFO)
        registerExInfo(AsyncRequestTimeoutException::class.java, "Asyn client connection lost (OK)", LogLevel.INFO)
        registerExInfo(java.lang.IllegalStateException::class.java, knownExceptionMessagePart = "Cannot start async")
        registerExInfo(NoResourceFoundException::class.java, "Resource not found", status = HttpStatus.NOT_FOUND)
        registerExInfo(
            InternalErrorException::class.java,
            "Csrf error",
            LogLevel.WARN,
            knownExceptionMessagePart = "csrf"
        )
        registerExInfo(IOException::class.java, "Connection lost", LogLevel.INFO, match = { ex ->
            val message = ex.message?.lowercase()
            message?.contains("broken pipe") == true || message?.contains("connection reset by peer") == true
                    // Die Verbindung wurde vom Kommunikationspartner zurückgesetzt
                    || message?.contains("verbindung wurde vom kommunikationspartner") == true
        })
    }

    @JvmStatic
    fun sendMailToDevelopers(ex: Throwable): Boolean {
        val known = findExInfo(ex) ?: return true
        return known.sendMailToDevelopers
    }

    @JvmStatic
    fun findExInfo(ex: Throwable): ExInfo? {
        synchronized(knownExceptions) {
            return knownExceptions.find { it.match(ex) }
        }
    }
}
