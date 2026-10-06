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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogLevel
import org.projectforge.framework.integration.SyncStats
import org.projectforge.mail.Mail
import org.projectforge.mail.SendMail
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.time.ZoneOffset

class ErrorDigestMailTest : AbstractTestBase() {
    @Autowired
    private lateinit var sendMail: SendMail

    @Test
    fun `html digest is rendered, escaped and sorted by severity`() {
        val collector = ErrorDigestCollector()
        collector.add(occurrence(DATA_EVENT, "Message with <script>alert(1)</script> and \$x", "UserPrefDO:175", LogLevel.WARN))
        collector.add(occurrence(SupportLogEvents.REQUEST_ERROR, "NPE in list", "Foo:1", LogLevel.ERROR, user = "kai"))
        collector.add(occurrence(SupportLogEvents.EXTERNAL_UNREACHABLE, "Sipgate not reachable", "Sipgate:2", LogLevel.ERROR))
        val snapshot = collector.drain()
        val stats = SyncStats("gateway-push").also { it.startRun().abort("Gateway not reachable") }
        val problems = SyncProblemTracker { listOf(stats) }.collect()
        val renderer = ErrorDigestRenderer("https://pf.example.org", ZoneOffset.UTC)
        val html = sendMail.renderGroovyTemplate(
            Mail(), "mail/errorDigestMail.html",
            renderer.htmlData(snapshot, problems, 0L, 3_600_000L, "error-digest.txt"), "Error digest", null,
        )
        File("build/tmp").mkdirs()
        File("build/tmp/errorDigestMail.html").writeText(html) // For a visual check in the browser.
        Assertions.assertFalse(html.contains("not found!"), html)
        Assertions.assertFalse(html.contains("<script>"), html)
        Assertions.assertTrue(html.contains("&lt;script&gt;"), html)
        Assertions.assertTrue(html.contains("\$x"), html)
        val external = html.indexOf(LogCategory.EXTERNAL.title)
        val sync = html.indexOf("Sync runs with problems")
        val data = html.indexOf(LogCategory.DATA.title)
        val bugs = html.indexOf(LogCategory.BUG.title)
        Assertions.assertTrue(external in 0 until sync && sync < data && data < bugs, html)
        Assertions.assertTrue(html.contains("Repair the preferences.") && html.contains("test.data.userPref"), html)
        Assertions.assertTrue(html.contains("badge-warn") && html.contains("badge-error"), html)
        Assertions.assertTrue(html.contains("users: kai"), html)
        Assertions.assertTrue(html.contains("error-digest.txt"), html)
    }

    @Test
    fun `new problems, regressions and spikes first, with links into the dashboard`() {
        val collector = ErrorDigestCollector()
        collector.add(occurrence(SupportLogEvents.EXTERNAL_UNREACHABLE, "Sipgate not reachable", "Sipgate:2", LogLevel.ERROR))
        collector.add(occurrence(DATA_EVENT, "Broken pref", "UserPrefDO:175", LogLevel.WARN))
        repeat(12) { collector.add(occurrence(SupportLogEvents.REQUEST_ERROR, "NPE $it", "Foo:1", LogLevel.ERROR)) }
        val snapshot = collector.drain().let { ErrorDigestCollector.Snapshot(it.groups, 0, suppressed = 4, muted = 2) }
        val (external, data, bug) = listOf(LogCategory.EXTERNAL, LogCategory.DATA, LogCategory.BUG)
            .map { category -> snapshot.groups.single { it.category == category } }
        data.novelty = DigestNovelty.NEW
        data.problemId = 42
        bug.novelty = DigestNovelty.SPIKE
        bug.hourlyMean = 0.25
        val renderer = ErrorDigestRenderer("https://pf.example.org", ZoneOffset.UTC, "https://pf.example.org/next/problemDashboard")
        Assertions.assertEquals(
            "Error digest https://pf.example.org: 2 problems, 1 external failures (1 new, 1 spikes)",
            renderer.subject(snapshot, emptyList()),
        )
        val html = sendMail.renderGroovyTemplate(
            Mail(), "mail/errorDigestMail.html",
            renderer.htmlData(snapshot, emptyList(), 0L, 3_600_000L, null), "Error digest", null,
        )
        Assertions.assertFalse(html.contains("not found!"), html)
        val newSection = html.indexOf("New problems")
        val spikes = html.indexOf("Spikes:")
        val known = html.indexOf(LogCategory.EXTERNAL.title)
        Assertions.assertTrue(newSection in 0 until spikes && spikes < known, html)
        Assertions.assertTrue(html.contains("href=\"https://pf.example.org/next/problemDashboard?id=42\""), html)
        Assertions.assertTrue(html.contains("usually 0.3 per hour"), html)
        Assertions.assertTrue(html.contains("ignored or muted problems"), html)
        Assertions.assertTrue(html.contains("13 occurrences"), "The occurrences below the problems: $html")
        Assertions.assertFalse(html.contains(">groups<"), html)
        Assertions.assertEquals(DigestNovelty.KNOWN, external.novelty)
        val body = renderer.body(snapshot, emptyList(), 0L, 3_600_000L, null)
        Assertions.assertTrue(body.indexOf("== New problems (1) ==") < body.indexOf("== ${LogCategory.EXTERNAL.title} (1) =="), body)
        Assertions.assertTrue(body.contains("category: ${LogCategory.DATA.title}") && body.contains("problemDashboard?id=42"), body)
        Assertions.assertTrue(body.contains("2 ignored or muted problems not listed"), body)
    }

    @Test
    fun `counts are formatted with thousands separators`() {
        Assertions.assertEquals("159,098", ErrorDigestRenderer.formatCount(159_098))
        Assertions.assertEquals("999", ErrorDigestRenderer.formatCount(999))
        val collector = ErrorDigestCollector()
        collector.add(occurrence(SupportLogEvents.REQUEST_ERROR, "NPE", "Foo:1", LogLevel.ERROR))
        val snapshot = collector.drain().let { ErrorDigestCollector.Snapshot(it.groups, 0, suppressed = 1_149_892) }
        val renderer = ErrorDigestRenderer("https://pf.example.org", ZoneOffset.UTC)
        val html = sendMail.renderGroovyTemplate(
            Mail(), "mail/errorDigestMail.html",
            renderer.htmlData(snapshot, emptyList(), 0L, 3_600_000L, null), "Error digest", null,
        )
        Assertions.assertTrue(html.contains(">1,149,892</span>"), html)
        val body = renderer.body(snapshot, emptyList(), 0L, 3_600_000L, null)
        Assertions.assertTrue(body.contains("1,149,892 occurrences not listed"), body)
    }

    @Test
    fun `only the most frequent known problems are listed`() {
        val collector = ErrorDigestCollector()
        repeat(ErrorDigestRenderer.MAX_KNOWN_GROUPS + 3) {
            collector.add(occurrence(LogEvent("test.known.$it", LogCategory.BUG), "Failed", "Foo:$it", LogLevel.ERROR))
        }
        val view = ErrorDigestRenderer(null, ZoneOffset.UTC).view(collector.drain(), emptyList(), 0L, 1L, null)
        Assertions.assertEquals(ErrorDigestRenderer.MAX_KNOWN_GROUPS, view.sections.sumOf { it.groups.size })
        Assertions.assertEquals(3, view.omitted)
    }

    private fun occurrence(
        event: LogEvent,
        message: String,
        location: String,
        level: LogLevel,
        user: String? = null,
    ) = ErrorOccurrence(
        timestampMillis = 1000L,
        level = level,
        event = event,
        exceptionClass = null,
        message = message,
        location = location,
        user = user,
        groupByCode = event === DATA_EVENT,
    )

    companion object {
        private val DATA_EVENT = LogEvent(
            "test.data.userPref", LogCategory.DATA,
            explanation = "A user preference is broken.", action = "Repair the preferences.",
        )
    }
}
