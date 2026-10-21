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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.time.PFDateTime.Companion.withDate
import org.springframework.beans.factory.annotation.Autowired
import java.time.Month

/**
 * The project checklist of the time sheet list ([TimesheetEntityRest.projectFilterValues]): without any other
 * criterion ("choose from all") it offers the projects of every sheet the user may read, rather than nothing
 * because of the period-or-task guard ([TimesheetEntityRest.needsMoreFilter]).
 */
class TimesheetChecklistValuesTest : AbstractTestBase() {
    @Autowired
    private lateinit var timesheetEntityRest: TimesheetEntityRest

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Test
    fun `without other criteria the projects of all readable sheets are offered`() {
        val prefix = "ts-checklist-"
        var projektId: Long? = null
        persistenceService.runInTransaction { _ ->
            logon(getUser(TEST_FINANCE_USER))
            val kunde = KundeDO().also {
                it.name = "Checklist customer"
                it.id = 63
            }
            kundeDao.insert(kunde)
            projektId = initTestDB.addProjekt(kunde, 1, "Checklist project", 0).id
            initTestDB.addTask(prefix + "1", "root")
            initTestDB.addUser(prefix + "user1")
            val ts = TimesheetDO()
            ts.startTime = withDate(2011, Month.MARCH, 7, 9, 0, 0).sqlTimestamp
            ts.stopTime = withDate(2011, Month.MARCH, 7, 10, 0, 0).sqlTimestamp
            ts.task = initTestDB.getTask(prefix + "1")
            ts.user = getUser(prefix + "user1")
            ts.kost2 = kost2Dao.getKost2(5, 63, 1, 0L)
            timesheetDao.insert(ts, checkAccess = false)
        }
        val key = projektId.toString()

        logon(getUser(TEST_ADMIN_USER))
        assertTrue(timesheetEntityRest.projectFilterValues(MagicFilter()).any { it.id == key })
        // Criteria that don't pass the guard still offer nothing, as the list shows nothing.
        val userOnly = MagicFilter(entries = mutableListOf(MagicFilterEntry("user").also { it.value.id = 42 }))
        assertTrue(timesheetEntityRest.projectFilterValues(userOnly).isEmpty())

        // Nobody is offered the projects of sheets they may not read.
        logon(getUser(TEST_USER))
        assertFalse(timesheetEntityRest.projectFilterValues(MagicFilter()).any { it.id == key })
    }
}
