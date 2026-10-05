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
import org.projectforge.common.i18n.UserException
import org.projectforge.common.logging.CommonLogEvents
import org.projectforge.common.logging.LogAudience
import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogEventRegistry
import org.projectforge.common.logging.LogLevel
import org.projectforge.common.logging.LogNotify
import org.projectforge.common.logging.LoggingEventData
import org.projectforge.framework.integration.SyncStats
import org.slf4j.event.KeyValuePair
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.time.Duration
import java.time.ZoneOffset

class SupportErrorDigestTest {
    private val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    private var eventId = 0L

    private fun event(
        level: Level,
        message: String,
        ex: Throwable? = null,
        logger: String = "org.projectforge.Foo",
        logEvent: LogEvent? = null,
    ) = LoggingEventData(
        LoggingEvent(javaClass.name, loggerContext.getLogger(logger), level, message, ex, null).apply {
            callerData = arrayOf(StackTraceElement("org.projectforge.Foo", "bar", "Foo.kt", 42))
            logEvent?.let { addKeyValuePair(KeyValuePair(LogEvent.KEY, it)) }
        },
        ++eventId,
    )

    @Test
    fun `log events are classified by heuristics and filtered`() {
        val connect = ErrorOccurrenceFactory.fromLogEvent(
            event(Level.ERROR, "LDAP failed", IllegalStateException("x", ConnectException("Connection refused")))
        )!!
        Assertions.assertSame(SupportLogEvents.EXTERNAL_UNREACHABLE, connect.event)
        Assertions.assertEquals(LogCategory.EXTERNAL, connect.category)
        Assertions.assertFalse(connect.groupByCode)
        Assertions.assertEquals("java.net.ConnectException", connect.exceptionClass)
        // A WARN without event counts only where it looks like an unreachable system.
        Assertions.assertSame(
            SupportLogEvents.EXTERNAL_UNREACHABLE,
            ErrorOccurrenceFactory.fromLogEvent(event(Level.WARN, "Gateway not reachable at https://gw"))?.event,
        )
        Assertions.assertNull(ErrorOccurrenceFactory.fromLogEvent(event(Level.WARN, "Slow page")))
        Assertions.assertSame(
            SupportLogEvents.LOGGED_ERROR,
            ErrorOccurrenceFactory.fromLogEvent(event(Level.ERROR, "boom", IllegalStateException("boom")))?.event,
        )
        Assertions.assertSame(SupportLogEvents.LOGGED_ERROR, ErrorOccurrenceFactory.fromLogEvent(event(Level.ERROR, "boom"))?.event)
        // Known messages of libraries are grouped by their code.
        val netty = ErrorOccurrenceFactory.fromLogEvent(
            event(
                Level.ERROR,
                "Unable to load io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider, fallback to system defaults.",
                logger = "io.netty.resolver.dns.DnsServerAddressStreamProviders",
            )
        )!!
        Assertions.assertSame(ThirdPartyLogEvents.NETTY_MACOS_DNS, netty.event)
        Assertions.assertTrue(netty.groupByCode)
        Assertions.assertNull(ThirdPartyLogEvents.find("org.projectforge.Foo", "MacOSDnsServerAddressStreamProvider"))
        // The REST handler reports itself, and the digest doesn't collect its own errors.
        Assertions.assertNull(
            ErrorOccurrenceFactory.fromLogEvent(event(Level.ERROR, "x", logger = "org.projectforge.rest.core.GlobalDefaultExceptionHandler"))
        )
        Assertions.assertNull(
            ErrorOccurrenceFactory.fromLogEvent(event(Level.ERROR, "x", logger = "org.projectforge.framework.support.SupportErrorDigest"))
        )
    }

    @Test
    fun `the event of the logging call wins over the exception's and the heuristics`() {
        // Explicit event: also a WARN is collected, and the text of an unreachable system doesn't matter.
        val explicit = ErrorOccurrenceFactory.fromLogEvent(event(Level.WARN, "Sipgate timeout", logEvent = SPIKE))!!
        Assertions.assertSame(SPIKE, explicit.event)
        Assertions.assertTrue(explicit.groupByCode)
        val explicitWithUserEx = ErrorOccurrenceFactory.fromLogEvent(
            event(Level.ERROR, "x", UserException("some.key"), logEvent = SPIKE)
        )!!
        Assertions.assertSame(SPIKE, explicitWithUserEx.event)
        // The exception's event.
        val user = ErrorOccurrenceFactory.fromLogEvent(event(Level.ERROR, "x", RuntimeException(UserException("some.key"))))!!
        Assertions.assertSame(CommonLogEvents.USER_ERROR, user.event)
        Assertions.assertEquals(LogNotify.NONE, user.event.notify)
    }

    @Test
    fun `request exceptions are classified`() {
        val ex = IllegalStateException("x")
        Assertions.assertSame(SupportLogEvents.REQUEST_ERROR, ErrorOccurrenceFactory.fromRequestException(ex, "kai", "GET /", false).event)
        Assertions.assertSame(SupportLogEvents.EXTERNAL_UNREACHABLE, ErrorOccurrenceFactory.fromRequestException(ex, "kai", "GET /", true).event)
        Assertions.assertSame(
            CommonLogEvents.USER_ERROR,
            ErrorOccurrenceFactory.fromRequestException(RuntimeException(UserException("k")), "kai", "GET /", true).event,
        )
        val known = ErrorOccurrenceFactory.fromRequestException(ex, "kai", "GET /", true, logEvent = SPIKE)
        Assertions.assertSame(SPIKE, known.event)
        Assertions.assertTrue(known.groupByCode)
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
    fun `equal errors with other ids fall into one group, sorted by category`() {
        val collector = ErrorDigestCollector()
        collector.add(occurrence(SupportLogEvents.LOGGED_ERROR, "Order #4711 failed", user = "a"))
        collector.add(occurrence(SupportLogEvents.LOGGED_ERROR, "Order #4712 failed", user = "b"))
        collector.add(occurrence(SupportLogEvents.LOGGED_ERROR, "NPE"))
        collector.add(occurrence(SupportLogEvents.EXTERNAL_UNREACHABLE, "LDAP down"))
        collector.add(occurrence(DATA_EVENT, "Inconsistent #1", location = "A:1"))
        collector.add(occurrence(DATA_EVENT, "Other text #2", location = "B:2")) // Same code: same group.
        val snapshot = collector.drain()
        Assertions.assertEquals(
            listOf(LogCategory.EXTERNAL, LogCategory.DATA, LogCategory.BUG, LogCategory.BUG),
            snapshot.groups.map { it.category },
        )
        Assertions.assertEquals(2, snapshot.groups[1].count)
        Assertions.assertEquals("test.data.inconsistent", snapshot.groups[1].key)
        val orders = snapshot.groups[2]
        Assertions.assertEquals(2, orders.count)
        Assertions.assertEquals(setOf("a", "b"), orders.users)
        Assertions.assertEquals(6, snapshot.occurrences)
        Assertions.assertTrue(collector.drain().groups.isEmpty(), "Drained: a new period starts.")
    }

    @Test
    fun `the collector stays bounded`() {
        val collector = ErrorDigestCollector(maxGroups = 3)
        repeat(5) { collector.add(occurrence(SupportLogEvents.LOGGED_ERROR, "error", location = "L$it")) }
        repeat(10) { collector.add(occurrence(SupportLogEvents.LOGGED_ERROR, "error", location = "L0")) }
        val snapshot = collector.drain()
        Assertions.assertEquals(3, snapshot.groups.size)
        Assertions.assertEquals(2, snapshot.dropped)
        Assertions.assertEquals(11, snapshot.groups.first().count)
        Assertions.assertEquals(ErrorGroup.MAX_SAMPLES, snapshot.groups.first().samples.size)
    }

    @Test
    fun `notify rules decide what is reported, not the level`() {
        val digest = SupportErrorDigest().also { it.active = true }
        digest.collect(occurrence(CommonLogEvents.USER_ERROR, "Validation failed"))
        digest.collect(occurrence(QUIET, "Expected error"))
        Assertions.assertTrue(digest.collector.drain().groups.isEmpty(), "NONE isn't collected for the digest.")
        Assertions.assertFalse(digest.immediatePending)
        digest.collect(occurrence(URGENT, "Data corrupted"))
        Assertions.assertTrue(digest.immediatePending, "IMMEDIATE sends the digest early.")
        Assertions.assertEquals(1, digest.collector.drain().groups.size)

        val filter = DigestNotifyFilter()
        fun period(spikes: Int, onceMessages: Int): ErrorDigestCollector.Snapshot {
            val collector = ErrorDigestCollector()
            repeat(spikes) { collector.add(occurrence(SPIKE, "Login failed")) }
            repeat(onceMessages) { collector.add(occurrence(ONCE, "API changed")) }
            collector.add(occurrence(SupportLogEvents.LOGGED_ERROR, "NPE"))
            return filter.apply(collector.drain())
        }
        period(spikes = 2, onceMessages = 1).let {
            Assertions.assertEquals(listOf("test.once", "support.loggedError"), it.groups.map { g -> g.event.code })
            Assertions.assertEquals(2, it.suppressed, "Below the threshold of 3.")
        }
        period(spikes = 3, onceMessages = 2).let {
            Assertions.assertEquals(listOf("test.spike", "support.loggedError"), it.groups.map { g -> g.event.code })
            Assertions.assertEquals(2, it.suppressed, "DIGEST_IF_NEW is reported once only.")
        }
    }

    @Test
    fun `new security and data problems are alarms`() {
        val digest = SupportErrorDigest().also { it.active = true }
        digest.collect(occurrence(SupportLogEvents.LOGGED_ERROR, "NPE"))
        digest.collect(occurrence(SupportLogEvents.EXTERNAL_UNREACHABLE, "LDAP down"))
        Assertions.assertFalse(digest.immediatePending, "Bugs and external failures wait for the interval.")
        digest.collect(occurrence(DATA_EVENT, "Inconsistent order"))
        Assertions.assertTrue(digest.immediatePending, "A new data problem.")

        val next = SupportErrorDigest().also { it.active = true }
        repeat(2) { next.collect(occurrence(SPIKE, "Login failed")) }
        Assertions.assertFalse(next.immediatePending, "Below the threshold of 3.")
        next.collect(occurrence(SPIKE, "Login failed"))
        Assertions.assertTrue(next.immediatePending)
    }

    @Test
    fun `log event codes are unique`() {
        Assertions.assertEquals(emptyList<String>(), LogEventRegistry.findProblems(*BusinessLogEventCatalog.HOLDERS))
        Assertions.assertTrue(BusinessLogEventCatalog.HOLDERS.all { LogEventRegistry.eventsOf(it).isNotEmpty() })
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
        collector.add(occurrence(SupportLogEvents.REQUEST_ERROR, "NPE in list", stackTrace = "java.lang.NullPointerException\n\tat x.Y(Y.kt:1)", request = "GET /rs/x"))
        collector.add(occurrence(SupportLogEvents.EXTERNAL_UNREACHABLE, "Sipgate not reachable"))
        collector.add(occurrence(DATA_EVENT, "Inconsistent order"))
        val snapshot = collector.drain()
        val stats = SyncStats("gateway-push").also { it.startRun().abort("Gateway not reachable") }
        val problems = SyncProblemTracker { listOf(stats) }.collect()
        val renderer = ErrorDigestRenderer("pf.example.org", ZoneOffset.UTC)
        val body = renderer.body(snapshot, problems, 0L, 3_600_000L, "error-digest.txt")
        val external = body.indexOf(LogCategory.EXTERNAL.title)
        val sync = body.indexOf("Sync runs with problems")
        val data = body.indexOf(LogCategory.DATA.title)
        val bugs = body.indexOf(LogCategory.BUG.title)
        Assertions.assertTrue(external in 0 until sync && sync < data && data < bugs, body)
        Assertions.assertTrue(body.contains("gateway-push: 1 aborted"), body)
        Assertions.assertTrue(body.contains("action: Repair the order."), body)
        Assertions.assertFalse(body.contains(SupportLogEvents.REQUEST_ERROR.action!!), "Not repeated for generic events: $body")
        Assertions.assertEquals("Error digest pf.example.org: 2 errors, 2 external failures", renderer.subject(snapshot, problems))
        val details = renderer.details(snapshot)
        Assertions.assertTrue(details.contains("GET /rs/x") && details.contains("at x.Y(Y.kt:1)"), details)
        Assertions.assertTrue(details.contains("Code: test.data.inconsistent"), details)
        val view = renderer.view(snapshot, problems, 0L, 3_600_000L, "error-digest.txt")
        Assertions.assertEquals(
            listOf(LogCategory.EXTERNAL.title, "Sync runs with problems", LogCategory.DATA.title, LogCategory.BUG.title),
            view.sections.map { it.title },
        )
        Assertions.assertEquals(2, view.errors)
        Assertions.assertEquals(2, view.external)
        val group = view.sections[3].groups.single()
        Assertions.assertEquals("IllegalStateException", group.exceptionClass)
        Assertions.assertEquals("badge-error", group.levelCss)
        Assertions.assertNull(group.code)
        Assertions.assertEquals("test.data.inconsistent", view.sections[2].groups.single().code)
        Assertions.assertEquals("1 aborted", view.sections[1].syncProblems.single().counts)
    }

    @Test
    fun `one mail per recipients of the audiences`() {
        val collector = ErrorDigestCollector()
        collector.add(occurrence(SupportLogEvents.EXTERNAL_UNREACHABLE, "Sipgate not reachable")) // ADMIN
        collector.add(occurrence(SPIKE, "Login failed")) // SECURITY
        collector.add(occurrence(DATA_EVENT, "Inconsistent order")) // DEVELOPER
        val snapshot = collector.drain().let { ErrorDigestCollector.Snapshot(it.groups, 1, 2, 3) }
        val stats = SyncStats("gateway-push").also { it.startRun().abort("Gateway not reachable") }
        val problems = SyncProblemTracker { listOf(stats) }.collect()
        val mails = SupportErrorDigest.splitByRecipients(snapshot, problems) {
            if (it == LogAudience.SECURITY) "security@acme.com" else "support@acme.com"
        }
        Assertions.assertEquals(listOf("support@acme.com", "security@acme.com"), mails.map { it.recipients })
        mails[0].let {
            Assertions.assertEquals(listOf(LogCategory.EXTERNAL, LogCategory.DATA), it.snapshot.groups.map { g -> g.category })
            Assertions.assertEquals(1, it.syncProblems.size, "Sync problems go to the admins.")
            Assertions.assertEquals(3, it.snapshot.muted)
        }
        Assertions.assertEquals(listOf(SPIKE.code), mails[1].snapshot.groups.map { it.event.code })
        Assertions.assertTrue(mails[1].syncProblems.isEmpty())

        val syncOnly = SupportErrorDigest.splitByRecipients(ErrorDigestCollector.Snapshot(emptyList(), 0), problems) {
            if (it == LogAudience.ADMIN) "ops@acme.com" else null
        }
        Assertions.assertEquals(listOf("ops@acme.com"), syncOnly.map { it.recipients })
        Assertions.assertTrue(
            SupportErrorDigest.splitByRecipients(ErrorDigestCollector.Snapshot(emptyList(), 0), emptyList()) { "a@b.c" }
                .isEmpty(),
            "Nothing reported, no mail.",
        )
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
        event: LogEvent,
        message: String,
        location: String = "Foo:1",
        user: String? = null,
        stackTrace: String? = null,
        request: String? = null,
    ) = ErrorOccurrence(
        timestampMillis = 1000L,
        level = LogLevel.ERROR,
        event = event,
        exceptionClass = "java.lang.IllegalStateException",
        message = message,
        location = location,
        stackTrace = stackTrace,
        user = user,
        request = request,
        groupByCode = event.code.startsWith("test."),
    )

    companion object {
        private val SPIKE = LogEvent("test.spike", LogCategory.SECURITY, threshold = 3)
        private val ONCE = LogEvent("test.once", LogCategory.EXTERNAL, notify = LogNotify.DIGEST_IF_NEW)
        private val QUIET = LogEvent("test.quiet", LogCategory.BUG, notify = LogNotify.NONE)
        private val URGENT = LogEvent("test.urgent", LogCategory.DATA, notify = LogNotify.IMMEDIATE)
        private val DATA_EVENT = LogEvent(
            "test.data.inconsistent", LogCategory.DATA,
            explanation = "An order is inconsistent.", action = "Repair the order.",
        )
    }
}
