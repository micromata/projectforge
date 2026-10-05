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
import org.junit.jupiter.api.assertThrows
import org.projectforge.Constants
import org.projectforge.business.admin.SystemStatisticsData
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogLevel
import org.projectforge.common.logging.LogNotify
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

class LogGroupAdminServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var logAggregationService: LogAggregationService

    @Autowired
    private lateinit var logGroupAdminService: LogGroupAdminService

    @Autowired
    private lateinit var logAggregationStatisticsBuilder: LogAggregationStatisticsBuilder

    @Test
    fun `list, detail and status changes`() {
        val now = System.currentTimeMillis()
        val hour = Constants.MILLIS_PER_HOUR
        val external = LogEvent("test.admin.external", LogCategory.EXTERNAL, notify = LogNotify.DIGEST_IF_NEW)
        val bug = LogEvent("test.admin.bug", LogCategory.BUG)
        repeat(3) { logAggregationService.add(occurrence(external, now - it * hour, user = "u$it")) }
        logAggregationService.add(occurrence(external, now - 3 * Constants.MILLIS_PER_DAY))
        logAggregationService.add(occurrence(bug, now - 10 * Constants.MILLIS_PER_DAY, stackTrace = "trace"))
        // Not written yet: the dashboard flushes itself.
        val list = logGroupAdminService.list(LogGroupFilter(search = "test.admin."), now)
        Assertions.assertTrue(list.enabled)
        Assertions.assertEquals(listOf(external.code), list.entries.map { it.code }, "The bug is older than 7 days.")
        val entry = list.entries.single()
        Assertions.assertEquals(4, entry.totalCount)
        Assertions.assertEquals(3, entry.count24h)
        Assertions.assertEquals(LogCategory.EXTERNAL.defaultNotify, entry.notify, "Not registered: the category's rule.")
        Assertions.assertEquals(28, entry.trend.size)
        Assertions.assertEquals(4, entry.trend.sum())
        Assertions.assertTrue(list.summary.externalProblems24h >= 1)
        Assertions.assertEquals(
            2, logGroupAdminService.list(LogGroupFilter(search = "test.admin.", days = 0), now).total,
        )
        Assertions.assertEquals(
            listOf(bug.code),
            logGroupAdminService.list(
                LogGroupFilter(search = "test.admin.", days = 0, category = LogCategory.BUG), now,
            ).entries.map { it.code },
        )

        val detail = logGroupAdminService.detail(entry.id, now)!!
        Assertions.assertFalse(detail.registered, "Not in the registry: a test event.")
        Assertions.assertEquals(168, detail.hourly.size)
        Assertions.assertEquals(4, detail.hourly.sum(), "7 days.")
        Assertions.assertEquals(3, detail.hourly.takeLast(24).sum())
        Assertions.assertEquals(4, detail.daily.sum())
        Assertions.assertEquals(3, detail.distinctUsers24h)
        Assertions.assertNull(logGroupAdminService.detail(-1, now))

        Assertions.assertEquals(
            1, logGroupAdminService.update(LogGroupUpdate(listOf(entry.id), LogGroupAction.ACKNOWLEDGE), now),
        )
        Assertions.assertEquals(LogGroupStatus.ACKNOWLEDGED, stateOf(external).status, "The digest sees it at once.")
        logGroupAdminService.update(LogGroupUpdate(listOf(entry.id), LogGroupAction.MUTE, muteDays = 7), now)
        logGroupAdminService.update(
            LogGroupUpdate(listOf(entry.id), LogGroupAction.SET_NOTIFY, notify = LogNotify.IMMEDIATE), now,
        )
        logGroupAdminService.list(LogGroupFilter(search = external.code), now).entries.single().let {
            Assertions.assertTrue(it.muted)
            Assertions.assertEquals(now + 7 * Constants.MILLIS_PER_DAY, it.mutedUntil)
            Assertions.assertEquals(LogNotify.IMMEDIATE, it.notify)
            Assertions.assertEquals(LogGroupStatus.ACKNOWLEDGED, it.status)
        }
        logGroupAdminService.update(LogGroupUpdate(listOf(entry.id), LogGroupAction.RESOLVE), now)
        Assertions.assertTrue(
            logGroupAdminService.list(LogGroupFilter(search = external.code), now).entries.isEmpty(),
            "Resolved problems aren't open.",
        )
        assertThrows<IllegalArgumentException> {
            logGroupAdminService.update(LogGroupUpdate(listOf(entry.id), LogGroupAction.MUTE, muteDays = 0), now)
        }
        assertThrows<IllegalArgumentException> { logGroupAdminService.update(LogGroupUpdate(listOf(entry.id)), now) }
    }

    @Test
    fun bins() {
        val start = 1_000 * Constants.MILLIS_PER_HOUR
        val buckets = listOf(-1, 0, 5, 6, 23, 24).map { LogBucketRow(1, Date(start + it * Constants.MILLIS_PER_HOUR), 1, 1) }
        Assertions.assertArrayEquals(
            intArrayOf(2, 1, 0, 1),
            LogGroupAdminService.bins(buckets, start, binHours = 6, count = 4),
            "Older and newer ones are skipped.",
        )
    }

    private fun stateOf(event: LogEvent) = logAggregationService.stateOf(ErrorDigestCollector.keyOf(occurrence(event, 0)))!!

    @Test
    fun `key figures in the system statistics, for admins only`() {
        logAggregationService.add(occurrence(LogEvent("test.admin.statistics", LogCategory.BUG), System.currentTimeMillis()))
        logon(TEST_USER)
        Assertions.assertTrue(statisticsOf().entries.isEmpty(), "Visible for all users, but not these.")
        logon(TEST_ADMIN_USER)
        val entries = statisticsOf().entries.associate { it.id to it.value }
        Assertions.assertEquals(
            listOf("errors24h", "errorsNew24h", "errorsRegressions", "errorsExternal24h", "errorsOpen"), entries.keys.toList(),
        )
        Assertions.assertNotEquals("0", entries["errorsNew24h"])
    }

    private fun statisticsOf() = SystemStatisticsData().also { logAggregationStatisticsBuilder.addStatisticsEntries(it) }

    private fun occurrence(event: LogEvent, millis: Long, user: String? = null, stackTrace: String? = null) =
        ErrorOccurrence(
            timestampMillis = millis,
            level = LogLevel.ERROR,
            event = event,
            exceptionClass = null,
            message = "Something failed",
            location = "Foo:1",
            stackTrace = stackTrace,
            user = user,
            request = null,
            groupByCode = true,
        )
}
