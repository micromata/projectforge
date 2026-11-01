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

package org.projectforge.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.framework.time.PFDateTime.Companion.withDate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.server.ResponseStatusException
import java.time.Month

class TimesheetAppListTest : AbstractTestBase() {
    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var timesheetEntityRest: TimesheetEntityRest

    private fun book(user: String, day: Int, fromHour: Int, toHour: Int): Long {
        val sheet = TimesheetDO()
        sheet.startTime = withDate(2031, Month.MARCH, day, fromHour, 0, 0).sqlTimestamp
        sheet.stopTime = withDate(2031, Month.MARCH, day, toHour, 0, 0).sqlTimestamp
        sheet.task = initTestDB.getTask("applist-task")
        sheet.user = getUser(user)
        return timesheetDao.insert(sheet, checkAccess = false)
    }

    private fun millis(day: Int) = withDate(2031, Month.MARCH, day, 0, 0, 0).epochMilli

    @Test
    fun `the app list holds the own time sheets of the period only, ordered by start`() {
        logon(getUser(TEST_FINANCE_USER))
        var afternoon = 0L
        var morning = 0L
        persistenceService.runInTransaction { _ ->
            initTestDB.addTask("applist-task", "root")
            afternoon = book(TEST_FINANCE_USER, 10, 13, 15)
            morning = book(TEST_FINANCE_USER, 10, 8, 9)
            book(TEST_FINANCE_USER, 11, 8, 9)
            book(TEST_USER, 10, 8, 9)
        }
        val day = timesheetEntityRest.getAppList(millis(10), millis(11))
        assertEquals(listOf(morning, afternoon), day.timesheets.map { it.id })
        assertEquals("applist-task", day.timesheets.first().task?.title)
        val week = timesheetEntityRest.getAppList(millis(10), millis(12))
        assertEquals(3, week.timesheets.size)
    }

    @Test
    fun `the period is limited`() {
        logon(getUser(TEST_FINANCE_USER))
        assertThrows(ResponseStatusException::class.java) {
            timesheetEntityRest.getAppList(millis(10), millis(10))
        }
        assertThrows(ResponseStatusException::class.java) {
            timesheetEntityRest.getAppList(millis(1), millis(1) + 63 * 24 * 3600 * 1000L)
        }
    }
}
