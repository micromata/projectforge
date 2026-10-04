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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class LogEventListenerTest {
    private val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

    private fun event(level: Level, message: String) =
        LoggingEvent(javaClass.name, loggerContext.getLogger("org.projectforge.Foo"), level, message, null, null)
            .apply { callerData = arrayOf(StackTraceElement("org.projectforge.Foo", "bar", "Foo.kt", 42)) }

    @Test
    fun `listeners get WARN and above, a failing one doesn't break logging`() {
        val appender = LoggerMemoryAppender().apply { context = loggerContext; start() }
        val received = mutableListOf<String>()
        appender.addListener { throw IllegalStateException("broken listener") }
        val listener = LogEventListener { received.add(it.message ?: "") }
        appender.addListener(listener)
        appender.addListener(listener) // Registered once only.
        appender.doAppend(event(Level.INFO, "info"))
        appender.doAppend(event(Level.WARN, "warn"))
        appender.doAppend(event(Level.ERROR, "error"))
        Assertions.assertEquals(listOf("warn", "error"), received)
        Assertions.assertEquals(3, appender.query(LogFilter()).size, "All events still reach the queue.")
        appender.removeListener(listener)
        appender.doAppend(event(Level.ERROR, "after"))
        Assertions.assertEquals(2, received.size)
    }
}
