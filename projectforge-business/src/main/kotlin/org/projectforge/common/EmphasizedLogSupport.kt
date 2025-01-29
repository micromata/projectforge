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

package org.projectforge.common

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger

/**
 * Helper for logging very important information and warnings.
 */
class EmphasizedLogSupport @JvmOverloads constructor(
    private val log: Logger,
    val priority: Priority = Priority.IMPORTANT,
    private val alignment: Alignment = Alignment.CENTER
) {
    private var number = 0
    private val innerLength: Int
    private var logLevel = LogLevel.INFO
    private var started = false

    constructor(log: Logger, alignment: Alignment) : this(log, Priority.IMPORTANT, alignment)

    enum class Priority {
        NORMAL, IMPORTANT, VERY_IMPORTANT
    }

    enum class Alignment {
        CENTER, LEFT
    }

    enum class LogLevel {
        ERROR, WARN, INFO
    }

    init {
        when (priority) {
            Priority.NORMAL -> this.number = 1
            Priority.VERY_IMPORTANT -> {
                this.number = 5
                this.logLevel = LogLevel.WARN
            }

            else -> this.number = 2
        }
        innerLength = CONSOLE_LENGTH - 2 * number
    }

    private fun ensureStart() {
        if (!started) {
            started = true
            logStartSeparator()
        }
    }

    /**
     * @return this for chaining.
     */
    fun setLogLevel(logLevel: LogLevel): EmphasizedLogSupport {
        this.logLevel = logLevel
        return this
    }

    /**
     * @return this for chaining.
     */
    private fun logStartSeparator(): EmphasizedLogSupport {
        for (i in 0..<number) {
            logSeparatorLine()
        }
        return log("")
    }

    /**
     * @return this for chaining.
     */
    fun logEnd(): EmphasizedLogSupport {
        ensureStart()
        log("")
        for (i in 0..<number) {
            logSeparatorLine()
        }
        return this
    }

    private fun logSeparatorLine() {
        logLine(StringUtils.rightPad("", innerLength, "*") + asterisks(number * 2 + 2))
    }

    fun log(text: String): EmphasizedLogSupport {
        ensureStart()
        if (StringUtils.contains(text, "\n")) {
            for (line in StringUtils.splitPreserveAllTokens(text, '\n')) {
                logLineText(line)
            }
        } else {
            logLineText(text)
        }
        return this
    }

    private fun logLineText(line: String) {
        val padText = if (alignment == Alignment.LEFT)
            StringUtils.rightPad(line, innerLength)
        else
            StringUtils.center(line, innerLength)
        val text = when (this.priority) {
            Priority.VERY_IMPORTANT -> ConsoleUtils.getText(padText, ConsoleUtils.AnsiColor.RED)
            Priority.IMPORTANT -> ConsoleUtils.getText(padText, ConsoleUtils.AnsiColor.MAGENTA)
            Priority.NORMAL -> ConsoleUtils.getText(padText, ConsoleUtils.AnsiColor.BLUE)
        }
        logLine(asterisks(number) + " " + text + " " + asterisks(number))
    }

    private fun logLine(msg: String) {
        if (logLevel == LogLevel.ERROR) log.error(msg)
        else if (logLevel == LogLevel.WARN) log.warn(msg)
        else log.info(msg)
    }

    companion object {
        private const val CONSOLE_LENGTH = 120

        private fun asterisks(number: Int): String {
            return StringUtils.rightPad("*", number, '*')
        }
    }
}
