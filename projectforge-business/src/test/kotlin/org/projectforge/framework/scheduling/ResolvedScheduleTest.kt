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


package org.projectforge.framework.scheduling

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.common.scheduling.SchedulerSchedule
import java.time.ZoneId
import java.time.ZonedDateTime

class ResolvedScheduleTest {
    private val properties = mapOf("cron" to "0 30 2 * * *", "delay" to "60000", "off" to "-")

    /** Replaces `${key}` and `${key:default}` as Spring's environment does. */
    private val resolver: (String) -> String = { value ->
        Regex("[\$]\\{([^:}]+)(:([^}]*))?}").replace(value) { match ->
            properties[match.groupValues[1]] ?: match.groups[3]?.value
            ?: throw IllegalArgumentException("Could not resolve placeholder '${match.groupValues[1]}'")
        }
    }

    @Test
    fun `placeholders and defaults are resolved`() {
        val cron = ResolvedSchedule.resolve(SchedulerSchedule.Cron("\${cron}"), resolver, UTC)
        Assertions.assertEquals("0 30 2 * * *", (cron as ResolvedSchedule.Cron).expression)
        val delay = ResolvedSchedule.resolve(SchedulerSchedule.FixedDelay("\${delay}", "\${initial:1000}"), resolver, UTC)
        delay as ResolvedSchedule.FixedDelay
        Assertions.assertEquals(60_000L, delay.delayMillis)
        Assertions.assertEquals(1_000L, delay.initialDelayMillis)
        Assertions.assertEquals(0L, (ResolvedSchedule.resolve(SchedulerSchedule.FixedDelay(5_000L), resolver, UTC)
                as ResolvedSchedule.FixedDelay).initialDelayMillis)
    }

    @Test
    fun `a dash disables, unknown values are invalid`() {
        Assertions.assertSame(ResolvedSchedule.Disabled, ResolvedSchedule.resolve(SchedulerSchedule.Cron("\${off}"), resolver, UTC))
        Assertions.assertTrue(ResolvedSchedule.resolve(SchedulerSchedule.Cron("\${unknown}"), resolver, UTC) is ResolvedSchedule.Invalid)
        Assertions.assertTrue(ResolvedSchedule.resolve(SchedulerSchedule.Cron("no cron"), resolver, UTC) is ResolvedSchedule.Invalid)
        Assertions.assertTrue(ResolvedSchedule.resolve(SchedulerSchedule.FixedDelay("abc"), resolver, UTC) is ResolvedSchedule.Invalid)
    }

    @Test
    fun `next run and period of a cron job`() {
        val cron = ResolvedSchedule.resolve(SchedulerSchedule.Cron("0 30 2 * * *"), resolver, UTC)
        val from = millis(2026, 10, 10, 3, 0)
        Assertions.assertEquals(millis(2026, 10, 11, 2, 30), cron.nextAfter(from))
        Assertions.assertEquals(24 * 3600_000L, cron.periodAround(from))
    }

    @Test
    fun `birthday butler's cron (two days before the end of the month) is understood`() {
        val cron = ResolvedSchedule.resolve(SchedulerSchedule.Cron("0 0 8 L-2 * ?"), resolver, UTC)
        Assertions.assertTrue(cron is ResolvedSchedule.Cron)
        Assertions.assertEquals(millis(2026, 2, 26, 8, 0), cron.nextAfter(millis(2026, 2, 1, 0, 0)))
        Assertions.assertEquals(millis(2026, 3, 29, 8, 0), cron.nextAfter(millis(2026, 2, 27, 0, 0)))
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, UTC).toInstant().toEpochMilli()

    companion object {
        private val UTC = ZoneId.of("UTC")
    }
}
