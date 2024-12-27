/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.scripting

import mu.KotlinLogging
import org.projectforge.common.logging.LogLevel
import kotlin.script.experimental.api.ScriptDiagnostic

private val log = KotlinLogging.logger {}

/**
 * You may use logging functionality inside your scripts by using log.info(String) and log.error(String).
 */
class ScriptLogger {
    class Message(val message: String?, val level: LogLevel)

    val messages = mutableListOf<Message>()

    fun error(msg: Any?) {
        error { msg }
    }

    fun error(msg: () -> Any?) {
        val message = msg()?.toString()
        log.error { message }
        messages.add(Message(message, LogLevel.ERROR))
    }

    fun warn(msg: Any?) {
        warn { msg }
    }

    fun warn(msg: () -> Any?) {
        val message = msg()?.toString()
        log.info { message }
        messages.add(Message(message, LogLevel.WARN))
    }

    fun info(msg: Any?) {
        info { msg }
    }

    fun info(msg: () -> Any?) {
        val message = msg()?.toString()
        log.info { message }
        messages.add(Message(message, LogLevel.INFO))
    }

    fun debug(msg: Any?) {
        debug { msg }
    }

    fun debug(msg: () -> Any?) {
        val message = msg()?.toString()
        log.debug { message }
        messages.add(Message(message, LogLevel.DEBUG))
    }

    internal fun add(msg: String?, logLevel: ScriptDiagnostic.Severity) {
        when (logLevel) {
            ScriptDiagnostic.Severity.DEBUG -> {
                debug { msg }
                LogLevel.DEBUG
            }

            ScriptDiagnostic.Severity.INFO -> {
                info { msg }
                LogLevel.INFO
            }

            ScriptDiagnostic.Severity.WARNING -> {
                warn { msg }
                LogLevel.WARN
            }

            ScriptDiagnostic.Severity.ERROR, ScriptDiagnostic.Severity.FATAL -> {
                error { msg }
                LogLevel.ERROR
            }
        }
    }
}
