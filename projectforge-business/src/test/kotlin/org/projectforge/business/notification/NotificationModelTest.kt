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


package org.projectforge.business.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.business.test.TestSetup
import org.projectforge.business.timesheet.UnbookedDaysCalculator
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.business.vacation.model.VacationStatus
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.utils.HtmlSanitizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date

class NotificationModelTest {
    @Test
    fun sendDayTest() {
        // November 2025: Saturday 1st, Friday 28th is the last working day, Sunday 30th.
        val november = YearMonth.of(2025, 11)
        val beforeMonthEnd = { day: Int -> NotificationSchedule(NotificationSchedule.Mode.WORKING_DAYS_BEFORE_MONTH_END, day) }
        assertEquals(LocalDate.of(2025, 11, 28), beforeMonthEnd(0).sendDay(november))
        assertEquals(LocalDate.of(2025, 11, 27), beforeMonthEnd(1).sendDay(november))
        assertEquals(LocalDate.of(2025, 11, 21), beforeMonthEnd(5).sendDay(november))
        val dayOfMonth = { day: Int -> NotificationSchedule(NotificationSchedule.Mode.DAY_OF_MONTH, day) }
        assertEquals(LocalDate.of(2025, 11, 3), dayOfMonth(1).sendDay(november), "Saturday -> Monday")
        assertEquals(LocalDate.of(2025, 11, 12), dayOfMonth(12).sendDay(november))
        assertEquals(LocalDate.of(2025, 11, 28), dayOfMonth(29).sendDay(november), "no working day left -> last one")
        assertEquals(LocalDate.of(2025, 11, 28), dayOfMonth(31).sendDay(november), "beyond the month -> last one")
        // December 2025: 25th and 26th are holidays (Thursday, Friday), the 29th is the next working day.
        assertEquals(LocalDate.of(2025, 12, 29), dayOfMonth(25).sendDay(YearMonth.of(2025, 12)))
        assertTrue(dayOfMonth(12).isDue(LocalDate.of(2025, 11, 12)))
        assertFalse(dayOfMonth(12).isDue(LocalDate.of(2025, 11, 13)))
    }

    @Test
    fun dailyAndWeeklyTest() {
        val daily = NotificationSchedule(NotificationSchedule.Mode.DAILY)
        assertTrue(daily.isDue(LocalDate.of(2025, 11, 28)))
        assertFalse(daily.isDue(LocalDate.of(2025, 11, 29)), "Saturday")
        assertFalse(daily.isDue(LocalDate.of(2025, 12, 25)), "Christmas")
        val thursday = NotificationSchedule(NotificationSchedule.Mode.WEEKLY, dayOfWeek = DayOfWeek.THURSDAY)
        assertTrue(thursday.isDue(LocalDate.of(2025, 11, 27)))
        assertFalse(thursday.isDue(LocalDate.of(2025, 11, 28)))
        // Christmas week 2025: Thursday 25th and Friday 26th are holidays, no working day left in the week.
        (25..28).forEach { assertFalse(thursday.isDue(LocalDate.of(2025, 12, it)), "December $it") }
        // Easter Monday 2026 is April 6th: a weekly Monday rule moves to Tuesday.
        val monday = NotificationSchedule(NotificationSchedule.Mode.WEEKLY, dayOfWeek = DayOfWeek.MONDAY)
        assertFalse(monday.isDue(LocalDate.of(2026, 4, 6)))
        assertTrue(monday.isDue(LocalDate.of(2026, 4, 7)))
        assertFalse(NotificationSchedule().isDue(LocalDate.of(2026, 4, 7)), "NONE")
    }

    @Test
    fun validateTest() {
        assertTrue(NotificationSchedule().validate().isEmpty())
        assertEquals(
            listOf("schedule.day"),
            NotificationSchedule(NotificationSchedule.Mode.WORKING_DAYS_BEFORE_MONTH_END, 16).validate().map { it.first },
        )
        assertEquals(
            listOf("schedule.day"),
            NotificationSchedule(NotificationSchedule.Mode.DAY_OF_MONTH, 0).validate().map { it.first },
        )
        assertEquals(
            listOf("schedule.dayOfWeek"),
            NotificationSchedule(NotificationSchedule.Mode.WEEKLY).validate().map { it.first },
        )
    }

    @Test
    fun deliveryStateTest() {
        val created = Date(1_000_000L)
        val delivery = NotificationDelivery(
            listOf(
                NotificationDeliveryStep(NotificationChannel.IN_APP, 0),
                NotificationDeliveryStep(NotificationChannel.MAIL, 2880, onlyIfUnacknowledged = true),
            )
        )
        val state = NotificationDeliveryState.of(delivery, created)
        assertEquals(created, state.nextDueAt())
        state.steps[0].sentAt = created
        assertEquals(Date(created.time + 2880 * 60_000L), state.nextDueAt())
        state.steps[1].skipped = true
        assertNull(state.nextDueAt())
        // Written and read again:
        val reread = JsonUtils.fromJson(JsonUtils.toJson(state), NotificationDeliveryState::class.java)!!
        assertEquals(2, reread.steps.size)
        assertEquals(NotificationChannel.MAIL, reread.steps[1].channel)
        assertTrue(reread.steps[1].skipped)
    }

    @Test
    fun variablesTest() {
        val variables = mapOf("firstName" to "<Kai>", "month" to "Oktober 2026")
        assertEquals(
            "Hallo &lt;Kai&gt;, {{unknown}} Oktober 2026",
            NotificationTemplate.replaceVariables("Hallo {{firstName}}, {{unknown}} {{ month }}", variables, true),
        )
        assertEquals("Hallo <Kai>", NotificationTemplate.renderSubject("Hallo {{firstName}} ", variables))
        assertEquals(
            "<p>Hallo &lt;Kai&gt;</p>",
            NotificationTemplate.renderText("<p onclick=\"x()\">Hallo {{firstName}}</p><script>alert(1)</script>", variables),
        )
    }

    @Test
    fun sanitizeTest() {
        assertEquals("", HtmlSanitizer.sanitize(null))
        assertEquals(
            "<p>Hi <strong>you</strong>, <span style=\"color: #e03e2d\">red</span></p>",
            HtmlSanitizer.sanitize("<p onclick=\"x()\">Hi <strong>you</strong>, <span style=\"color: #e03e2d; font-size: 80px\">red</span></p><script>alert(1)</script>"),
        )
        assertEquals(
            "<a href=\"https://example.org\" target=\"_blank\" rel=\"noopener\">link</a> bad",
            HtmlSanitizer.sanitize("<a href=\"https://example.org\">link</a> <a href=\"javascript:alert(1)\">bad</a>"),
        )
        assertEquals("<span>x</span>", HtmlSanitizer.sanitize("<span style=\"background: url(x)\">x</span>"))
        assertEquals("Line 1<br>\nLine &lt;2&gt;", HtmlSanitizer.sanitize("Line 1\nLine <2>"))
    }

    @Test
    fun unbookedDaysTest() {
        val november = YearMonth.of(2025, 11)
        // 20 working days in November 2025 (no holiday).
        assertEquals(20, UnbookedDaysCalculator.unbookedDays(november, emptySet(), emptySet()).size)
        val days = UnbookedDaysCalculator.unbookedDays(november, setOf(3, 4, 5), setOf(6, 7))
        assertEquals(15, days.size)
        assertEquals(10, days.first())
        assertEquals("10.11., 11.11.", UnbookedDaysCalculator.format(november, days.take(2)))
        assertNull(UnbookedDaysCalculator.format(november, emptyList()))
        // December 2025: the holidays are no working days.
        val december = UnbookedDaysCalculator.unbookedDays(YearMonth.of(2025, 12), emptySet(), emptySet())
        assertFalse(december.contains(25))
        assertFalse(december.contains(26))
        // Only the days up to the run count:
        assertEquals(listOf(10), TimesheetsMissingRuleHandler.cut(days.take(2), november, LocalDate.of(2025, 11, 10)))
    }

    @Test
    fun vacationDaysTest() {
        val november = YearMonth.of(2025, 11)
        val vacations = listOf(
            vacation(LocalDate.of(2025, 10, 30), LocalDate.of(2025, 11, 4)), // Starts in October.
            vacation(LocalDate.of(2025, 11, 12), LocalDate.of(2025, 11, 12)).also { it.halfDayBegin = true },
            vacation(LocalDate.of(2025, 11, 17), LocalDate.of(2025, 11, 18), VacationStatus.IN_PROGRESS),
            vacation(LocalDate.of(2025, 11, 19), LocalDate.of(2025, 11, 19), VacationStatus.REJECTED),
            vacation(LocalDate.of(2025, 11, 20), LocalDate.of(2025, 11, 20)).also { it.deleted = true },
        )
        assertEquals(setOf(1, 2, 3, 4, 12), UnbookedDaysCalculator.vacationDays(november, vacations))
    }

    private fun vacation(start: LocalDate, end: LocalDate, status: VacationStatus = VacationStatus.APPROVED) =
        VacationDO().also {
            it.startDate = start
            it.endDate = end
            it.status = status
        }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            TestSetup.init()
        }
    }
}
