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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.projectforge.common.logging.LogLevel
import org.projectforge.common.logging.LoggingEventData
import org.projectforge.framework.integration.SyncStats
import java.net.ConnectException
import java.time.Duration
import java.time.ZoneOffset

class SupportErrorDigestTest {
    private val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    private var eventId = 0L

    private fun event(level: Level, message: String, ex: Throwable? = null, logger: String = "org.projectforge.Foo") =
        LoggingEventData(
            LoggingEvent(javaClass.name, loggerContext.getLogger(logger), level, message, ex, null)
                .apply { callerData = arrayOf(StackTraceElement("org.projectforge.Foo", "bar", "Foo.kt", 42)) },
            ++eventId,
        )

    @Test
    fun `log events are categorized and filtered`() {
        val connect = ErrorOccurrenceFactory.fromLogEvent(
            event(Level.ERROR, "LDAP failed", IllegalStateException("x", ConnectException("Connection refused")))
        )!!
        Assertions.assertEquals(ErrorCategory.EXTERNAL_UNREACHABLE, connect.category)
        Assertions.assertEquals("java.net.ConnectException", connect.exceptionClass)
        // A WARN counts only where it looks like an unreachable system.
        Assertions.assertEquals(
            ErrorCategory.EXTERNAL_UNREACHABLE,
            ErrorOccurrenceFactory.fromLogEvent(event(Level.WARN, "Gateway not reachable at https://gw"))?.category,
        )
        Assertions.assertNull(ErrorOccurrenceFactory.fromLogEvent(event(Level.WARN, "Slow page")))
        Assertions.assertEquals(
            ErrorCategory.ERROR,
            ErrorOccurrenceFactory.fromLogEvent(event(Level.ERROR, "boom", IllegalStateException("boom")))?.category,
        )
        Assertions.assertEquals(
            ErrorCategory.ERROR_NO_TRACE,
            ErrorOccurrenceFactory.fromLogEvent(event(Level.ERROR, "boom"))?.category,
        )
        // The REST handler reports itself, and the digest doesn't collect its own errors.
        Assertions.assertNull(
            ErrorOccurrenceFactory.fromLogEvent(event(Level.ERROR, "x", logger = "org.projectforge.rest.core.GlobalDefaultExceptionHandler"))
        )
        Assertions.assertNull(
            ErrorOccurrenceFactory.fromLogEvent(event(Level.ERROR, "x", logger = "org.projectforge.framework.support.SupportErrorDigest"))
        )
    }

    @Test
    fun `a frame named like a timeout doesn't make an error external`() {
        val trace = "java.lang.IllegalStateException: boom\n\tat kotlinx.coroutines.TimeoutKt.withTimeout(Timeout.kt:1)\n"
        Assertions.assertFalse(ErrorOccurrenceFactory.isConnectionText(ErrorOccurrenceFactory.exceptionLines(trace)))
        Assertions.assertEquals(
            "java.net.ConnectException",
            ErrorOccurrenceFactory.rootExceptionClass(
                "java.lang.IllegalStateException: x\n\tat a.b(C.kt:1)\nCaused by: java.net.ConnectException: Connection refused\n"
            ),
        )
    }

    @Test
    fun `equal errors with other ids fall into one group, sorted by severity`() {
        val collector = ErrorDigestCollector()
        collector.add(occurrence(ErrorCategory.ERROR, "Order #4711 failed", user = "a"))
        collector.add(occurrence(ErrorCategory.ERROR, "Order #4712 failed", user = "b"))
        collector.add(occurrence(ErrorCategory.REQUEST_ERROR, "NPE"))
        collector.add(occurrence(ErrorCategory.EXTERNAL_UNREACHABLE, "LDAP down"))
        val snapshot = collector.drain()
        Assertions.assertEquals(
            listOf(ErrorCategory.EXTERNAL_UNREACHABLE, ErrorCategory.REQUEST_ERROR, ErrorCategory.ERROR),
            snapshot.groups.map { it.category },
        )
        val orders = snapshot.groups.last()
        Assertions.assertEquals(2, orders.count)
        Assertions.assertEquals(setOf("a", "b"), orders.users)
        Assertions.assertEquals(4, snapshot.occurrences)
        Assertions.assertTrue(collector.drain().groups.isEmpty(), "Drained: a new period starts.")
    }

    @Test
    fun `the collector stays bounded`() {
        val collector = ErrorDigestCollector(maxGroups = 3)
        repeat(5) { collector.add(occurrence(ErrorCategory.ERROR, "error", location = "L$it")) }
        repeat(10) { collector.add(occurrence(ErrorCategory.ERROR, "error", location = "L0")) }
        val snapshot = collector.drain()
        Assertions.assertEquals(3, snapshot.groups.size)
        Assertions.assertEquals(2, snapshot.dropped)
        Assertions.assertEquals(11, snapshot.groups.first().count)
        Assertions.assertEquals(ErrorGroup.MAX_SAMPLES, snapshot.groups.first().samples.size)
    }

    @Test
    fun `sync problems are reported once per new failure`() {
        val stats = SyncStats("ldap-master")
        val tracker = SyncProblemTracker { listOf(stats) }
        Assertions.assertTrue(tracker.collect().isEmpty())
        stats.startRun().abort("LDAP not reachable", timeout = true)
        val problems = tracker.collect()
        Assertions.assertEquals(1, problems.size)
        Assertions.assertEquals(1, problems[0].abortedRuns)
        Assertions.assertEquals(1, problems[0].timeouts)
        Assertions.assertTrue(tracker.collect().isEmpty(), "Nothing new since the last digest.")
    }

    @Test
    fun `body and attachment`() {
        val collector = ErrorDigestCollector()
        collector.add(occurrence(ErrorCategory.REQUEST_ERROR, "NPE in list", stackTrace = "java.lang.NullPointerException\n\tat x.Y(Y.kt:1)", request = "GET /rs/x"))
        collector.add(occurrence(ErrorCategory.EXTERNAL_UNREACHABLE, "Sipgate not reachable"))
        val snapshot = collector.drain()
        val stats = SyncStats("gateway-push").also { it.startRun().abort("Gateway not reachable") }
        val problems = SyncProblemTracker { listOf(stats) }.collect()
        val renderer = ErrorDigestRenderer("pf.example.org", ZoneOffset.UTC)
        val body = renderer.body(snapshot, problems, 0L, 3_600_000L, "error-digest.txt")
        val external = body.indexOf("External systems not reachable")
        val sync = body.indexOf("Sync runs with problems")
        val requests = body.indexOf("Unexpected errors in requests")
        Assertions.assertTrue(external in 0 until sync && sync < requests, body)
        Assertions.assertTrue(body.contains("gateway-push: 1 aborted"), body)
        Assertions.assertEquals("Error digest pf.example.org: 1 errors, 2 external failures", renderer.subject(snapshot, problems))
        val details = renderer.details(snapshot)
        Assertions.assertTrue(details.contains("GET /rs/x") && details.contains("at x.Y(Y.kt:1)"), details)
    }

    @Test
    fun `interval property`() {
        Assertions.assertEquals(Duration.ofHours(1), SupportErrorDigest.parseInterval(null))
        Assertions.assertEquals(Duration.ofMinutes(15), SupportErrorDigest.parseInterval("15m"))
        Assertions.assertEquals(Duration.ofDays(1), SupportErrorDigest.parseInterval("1d"))
        Assertions.assertEquals(Duration.ofHours(2), SupportErrorDigest.parseInterval("PT2H"))
        Assertions.assertTrue(SupportErrorDigest.parseInterval("0").isZero, "0 disables the digest.")
        Assertions.assertEquals(Duration.ofHours(1), SupportErrorDigest.parseInterval("often"))
    }

    private fun occurrence(
        category: ErrorCategory,
        message: String,
        location: String = "Foo:1",
        user: String? = null,
        stackTrace: String? = null,
        request: String? = null,
    ) = ErrorOccurrence(
        timestampMillis = 1000L,
        level = LogLevel.ERROR,
        category = category,
        exceptionClass = "java.lang.IllegalStateException",
        message = message,
        location = location,
        stackTrace = stackTrace,
        user = user,
        request = request,
    )
}
