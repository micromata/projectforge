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
import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.common.i18n.UserException
import org.slf4j.LoggerFactory

class LogEventTest {
    private val loggerName = "org.projectforge.common.logging.LogEventTestLogger"
    private val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    private val logbackLogger = loggerContext.getLogger(loggerName)
    private lateinit var appender: LoggerMemoryAppender
    private val received = mutableListOf<LoggingEventData>()

    @BeforeEach
    fun setUp() {
        appender = LoggerMemoryAppender().apply { context = loggerContext; start() }
        appender.addListener { received.add(it) }
        logbackLogger.addAppender(appender)
        logbackLogger.level = Level.INFO
    }

    @AfterEach
    fun tearDown() {
        logbackLogger.detachAppender(appender)
    }

    @Test
    fun `kotlin calls carry the event, the caller is the location`() {
        val log = KotlinLogging.logger(loggerName)
        val ex = IllegalStateException("boom")
        log.error(EVENT, ex) { "Sync failed" }
        log.warn(EVENT) { "Sync warning" }
        log.info(EVENT) { "Only info" } // Not passed to the listener (WARN and above only).
        log.error(EVENT) { throw IllegalArgumentException("broken message") }
        Assertions.assertEquals(3, received.size)
        received[0].let {
            Assertions.assertSame(EVENT, it.logEvent)
            Assertions.assertEquals(LogLevel.ERROR, it.level)
            Assertions.assertEquals("Sync failed", it.message)
            Assertions.assertTrue(it.stackTrace!!.contains("boom"))
            Assertions.assertEquals("LogEventTest", it.javaClassSimpleName, "Location must be the caller.")
            Assertions.assertTrue(it.methodName.startsWith("kotlin calls carry the event"), it.methodName)
        }
        Assertions.assertSame(EVENT, received[1].logEvent)
        Assertions.assertEquals(LogLevel.WARN, received[1].level)
        Assertions.assertTrue(received[2].message!!.startsWith("Log message invocation failed"))
    }

    @Test
    fun `java style calls carry the event, the caller is the location`() {
        val log = LoggerFactory.getLogger(loggerName)
        PfLog.error(log, EVENT, "Sync failed", IllegalStateException("boom"))
        PfLog.warn(log, EVENT, "Sync warning")
        Assertions.assertEquals(2, received.size)
        received.forEach {
            Assertions.assertSame(EVENT, it.logEvent)
            Assertions.assertEquals("LogEventTest", it.javaClassSimpleName, "Location must be the caller.")
        }
        Assertions.assertNotNull(received[0].stackTrace)
    }

    @Test
    fun `exceptions carry their event, the logging call wins`() {
        val log = KotlinLogging.logger(loggerName)
        log.error(RuntimeException("wrapped", UserException("some.i18n.key"))) { "User error" }
        log.error(IllegalStateException("plain")) { "Plain error" }
        log.error(EVENT, UserException("some.i18n.key")) { "Explicit event" }
        Assertions.assertSame(CommonLogEvents.USER_ERROR, received[0].logEvent)
        Assertions.assertNull(received[1].logEvent)
        Assertions.assertSame(EVENT, received[2].logEvent)
    }

    @Test
    fun `defaults and validation`() {
        Assertions.assertEquals(LogNotify.DIGEST, EVENT.notify)
        Assertions.assertEquals(LogAudience.ADMIN, EVENT.audience)
        Assertions.assertEquals("test.sync.failed", EVENT.toString())
        assertThrows<IllegalArgumentException> { LogEvent("noDot", LogCategory.BUG) }
        assertThrows<IllegalArgumentException> { LogEvent("Upper.case", LogCategory.BUG) }
        assertThrows<IllegalArgumentException> { LogEvent("test.threshold", LogCategory.BUG, threshold = 0) }
    }

    @Test
    fun `registry collects the events of holders and finds duplicate codes`() {
        Assertions.assertEquals(listOf(Holder.A, Holder.B), LogEventRegistry.eventsOf(Holder).sortedBy { it.code })
        LogEventRegistry.register(Holder, CommonLogEvents)
        Assertions.assertSame(Holder.A, LogEventRegistry.get("test.holder.a"))
        Assertions.assertSame(CommonLogEvents.USER_ERROR, LogEventRegistry.get(CommonLogEvents.USER_ERROR.code))
        LogEventRegistry.register(DuplicateHolder) // Logged as error, the first one stays.
        Assertions.assertSame(Holder.A, LogEventRegistry.get("test.holder.a"))
        Assertions.assertTrue(LogEventRegistry.findProblems(Holder, CommonLogEvents).isEmpty())
        Assertions.assertEquals(1, LogEventRegistry.findProblems(Holder, DuplicateHolder).size)
    }

    private object Holder {
        @JvmField
        val A = LogEvent("test.holder.a", LogCategory.BUG)
        val B = LogEvent("test.holder.b", LogCategory.DATA)
    }

    private object DuplicateHolder {
        val A = LogEvent("test.holder.a", LogCategory.CONFIG)
    }

    companion object {
        private val EVENT = LogEvent(
            "test.sync.failed", LogCategory.EXTERNAL,
            explanation = "The test system is not reachable.",
            action = "Nothing to do, it's a test.",
        )
    }
}
