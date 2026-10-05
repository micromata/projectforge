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
