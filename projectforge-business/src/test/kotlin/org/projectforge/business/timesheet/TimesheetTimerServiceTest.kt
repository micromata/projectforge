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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.UserPrefCache
import org.springframework.beans.factory.annotation.Autowired

class TimesheetTimerServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var timerService: TimesheetTimerService

    @Autowired
    private lateinit var gridService: TimesheetTimeGridService

    @Autowired
    private lateinit var userPrefCache: UserPrefCache

    @Test
    fun `timer belongs to its user and survives the cache`() {
        logon(TEST_USER)
        assertNull(timerService.get())
        val before = System.currentTimeMillis()
        timerService.start(TimesheetTimerPref(startedAt = 1, taskId = 4711, description = "Work", tag = "t"))
        val timer = timerService.get()!!
        assertEquals(4711L, timer.taskId)
        assertEquals("Work", timer.description)
        // The server's clock, never the one of the request.
        assertNotNull(timer.startedAt)
        assert(timer.startedAt!! >= before)
        logon(TEST_USER2)
        assertNull(timerService.get())
        logon(TEST_USER)
        userPrefCache.flushToDB(getUserId(TEST_USER))
        userPrefCache.setExpired()
        assertEquals("Work", timerService.get()!!.description)
        timerService.clear()
        assertNull(timerService.get())
    }

    @Test
    fun `grid is per user, defaults to 15 and takes only allowed values`() {
        logon(TEST_USER)
        assertEquals(15, gridService.get())
        assertEquals(30, gridService.set(30))
        assertEquals(15, gridService.set(7))
        gridService.set(10)
        assertEquals(10, gridService.get())
        logon(TEST_USER2)
        assertEquals(15, gridService.get())
    }
}
