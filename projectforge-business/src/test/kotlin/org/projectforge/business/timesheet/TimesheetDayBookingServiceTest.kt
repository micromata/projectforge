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

package org.projectforge.business.timesheet

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.projectforge.business.timesheet.TimesheetDayBookingService.Status

class TimesheetDayBookingServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var service: TimesheetDayBookingService

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Test
    fun planSkipsWeekendsHolidaysAndOverlapsAndBooksHalfDaysProRata() {
        logon(TEST_FINANCE_USER)
        val task = initTestDB.addTask("bookDays-plan", "root")
        val user = initTestDB.addUser("bookDays-plan-user")
        insertSheet(user, task, LocalDate.of(2026, 12, 22), 10, 11) // Overlaps 9:00–17:00.
        insertSheet(user, task, LocalDate.of(2026, 12, 23), 7, 9) // Ends when the new one starts: no overlap.

        // Mon 21.12.2026 (half vacation day) until Fri 01.01.2027.
        val days = service.planDays(request(user, task, LocalDate.of(2026, 12, 21), LocalDate.of(2027, 1, 1), halfDayBegin = true))

        val expected = listOf(
            Status.PARTIAL to 240, // Half vacation day at the begin.
            Status.OVERLAP to 0,
            Status.BOOK to 480,
            Status.PARTIAL to 240, // Christmas Eve: a half working day.
            Status.HOLIDAY to 0,
            Status.WEEKEND to 0,
            Status.WEEKEND to 0,
            Status.BOOK to 480,
            Status.BOOK to 480,
            Status.BOOK to 480,
            Status.PARTIAL to 240, // New Year's Eve.
            Status.HOLIDAY to 0,
        )
        Assertions.assertEquals(expected, days.map { it.status to it.minutes })
        Assertions.assertFalse(days[3].note.isNullOrBlank(), "The half working day is named.")
        Assertions.assertTrue(days[1].note!!.contains("10:00"), "The overlapped time sheet is shown: ${days[1].note}")
        Assertions.assertNull(days[2].sharedOverlap)
        Assertions.assertEquals(2, sheetsOf(user).size, "A dry run books nothing.")

        // Booked, the day with the earlier time sheet has two then.
        service.book(request(user, task, LocalDate.of(2026, 12, 22), LocalDate.of(2026, 12, 23)))
        val zoneId = ThreadLocalUserContext.zoneId
        val perDay = sheetsOf(user).groupBy { PFDateTime.from(it.startTime!!).withZoneSameInstant(zoneId).localDate }
        Assertions.assertEquals(1, perDay[LocalDate.of(2026, 12, 22)]?.size, "The overlapping day is skipped.")
        Assertions.assertEquals(2, perDay[LocalDate.of(2026, 12, 23)]?.size)
    }

    @Test
    fun overlapsFollowTheSharedCostRule() {
        logon(TEST_FINANCE_USER)
        // Released (shared cost element) without project: an overlap with it is allowed, see TimesheetOverlapRuleTest.
        val released = initTestDB.addTask("bookDays-shared-released", "root")
        released.allowTimeOverlap = true
        taskDao.update(released, checkAccess = false)
        val plain = initTestDB.addTask("bookDays-shared-plain", "root")
        val user = initTestDB.addUser("bookDays-shared-user")
        val day = LocalDate.of(2026, 11, 2)
        insertSheet(user, released, day, 10, 11)
        insertSheet(user, plain, day.plusDays(1), 10, 11)

        val days = service.planDays(request(user, plain, day, day.plusDays(1)))
        Assertions.assertEquals(listOf(Status.BOOK, Status.OVERLAP), days.map { it.status })
        Assertions.assertTrue(days[0].sharedOverlap!!.contains("10:00"), "The allowed overlap is noted.")
        Assertions.assertEquals(1, service.book(request(user, plain, day, day.plusDays(1))).count { it.booking })
        Assertions.assertEquals(3, sheetsOf(user).size)

        // Without a task, the rule can't be applied: every overlap is unknown and nothing would be booked.
        val unknown = service.planDays(request(user, plain, day.plusDays(1), day.plusDays(1)).also { it.taskId = null })
        Assertions.assertEquals(listOf(Status.OVERLAP_UNKNOWN to 0), unknown.map { it.status to it.minutes })
    }

    @Test
    fun bookInsertsOneSheetPerDay() {
        logon(TEST_FINANCE_USER)
        val task = initTestDB.addTask("bookDays-book", "root")
        val user = initTestDB.addUser("bookDays-book-user")
        // Mon 02.11. until Fri 06.11.2026, 7.5 hours from 8:00.
        val booked = service.book(
            request(user, task, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6), hours = "7.5", start = LocalTime.of(8, 0))
        )
        Assertions.assertEquals(5, booked.count { it.booking })
        val sheets = sheetsOf(user).sortedBy { it.startTime }
        Assertions.assertEquals(5, sheets.size)
        val zoneId = ThreadLocalUserContext.zoneId
        sheets.forEachIndexed { index, sheet ->
            val start = PFDateTime.from(sheet.startTime!!).withZoneSameInstant(zoneId)
            Assertions.assertEquals(LocalDateTime.of(2026, 11, 2 + index, 8, 0), start.localDateTime)
            Assertions.assertEquals(450L * 60_000, sheet.duration)
            Assertions.assertEquals(task.id, sheet.taskId)
        }
        // The days overlap the booked time sheets now: all are skipped, nothing is left to book.
        assertThrows<UserException> {
            service.book(request(user, task, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6)))
        }
    }

    @Test
    fun protectedPeriodBooksNothing() {
        logon(TEST_FINANCE_USER)
        val task = initTestDB.addTask("bookDays-protected", "root")
        task.protectTimesheetsUntil = LocalDate.of(2026, 11, 4)
        taskDao.update(task, checkAccess = false)
        // Finance members may override the protection for others, but not for their own time sheets.
        val user = getUser(TEST_FINANCE_USER)
        assertThrows<UserException> {
            service.book(request(user, task, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6)))
        }
        Assertions.assertTrue(
            sheetsOf(user).none { it.taskId == task.id },
            "All or nothing: no day is booked, not even those after the protected period."
        )
    }

    @Test
    fun invalidRequestsAndOtherUsers() {
        logon(TEST_FINANCE_USER)
        val task = initTestDB.addTask("bookDays-invalid", "root")
        val user = initTestDB.addUser("bookDays-invalid-user")
        val day = LocalDate.of(2026, 11, 2)
        fun refused(key: String, request: TimesheetDayBookingService.Request) {
            val ex = assertThrows<UserException> { service.planDays(request) }
            Assertions.assertEquals(key, ex.i18nKey)
        }
        refused("timesheet.bookDays.error.period", request(user, task, day, day.minusDays(1)))
        refused("timesheet.bookDays.error.tooManyDays", request(user, task, day, LocalDate.of(2026, 12, 2)))
        // A month at most: 02.11. until 01.12. is fine.
        Assertions.assertEquals(30, service.planDays(request(user, task, day, LocalDate.of(2026, 12, 1))).size)
        refused("timesheet.bookDays.error.hours", request(user, task, day, day, hours = "15"))
        refused("timesheet.bookDays.error.nextDay", request(user, task, day, day, start = LocalTime.of(20, 0)))

        // Someone who may not see the time sheets of others may not plan theirs either.
        logon(TEST_USER)
        refused("timesheet.bookDays.error.otherUser", request(user, task, day, day))
    }

    private fun request(
        user: PFUserDO,
        task: TaskDO,
        from: LocalDate,
        until: LocalDate,
        hours: String = "8",
        start: LocalTime = LocalTime.of(9, 0),
        halfDayBegin: Boolean = false,
    ) = TimesheetDayBookingService.Request(
        userId = user.id,
        taskId = task.id,
        description = "Vacation",
        startDate = from,
        endDate = until,
        startTime = start,
        hoursPerDay = BigDecimal(hours),
        halfDayBegin = halfDayBegin,
    )

    private fun insertSheet(user: PFUserDO, task: TaskDO, day: LocalDate, fromHour: Int, toHour: Int) {
        val zoneId = ThreadLocalUserContext.zoneId
        timesheetDao.insert(TimesheetDO().also {
            it.user = user
            it.task = task
            it.description = "existing"
            it.startTime = PFDateTime.from(LocalDateTime.of(day, LocalTime.of(fromHour, 0)), zoneId).utilDate
            it.stopTime = PFDateTime.from(LocalDateTime.of(day, LocalTime.of(toHour, 0)), zoneId).utilDate
        }, checkAccess = false)
    }

    private fun sheetsOf(user: PFUserDO): List<TimesheetDO> {
        val filter = TimesheetFilter()
        filter.userId = user.id
        return timesheetDao.internalGetList(filter, checkAccess = false)
    }
}
