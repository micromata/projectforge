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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.ProjektStatus
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.business.task.TaskDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.*

/**
 * A deactivated (non-active) cost 2 unit can't be booked any more, but the time sheets already booked on it
 * stay editable and deletable: [TimesheetDao] checks the cost 2 unit of a new time sheet and of a changed task
 * or cost 2 unit only. A deleted cost 2 unit isn't bookable either ([KostCache.getActiveKost2]). The same
 * holds for an ended project, whose task takes no new time sheet at all, not even one without cost 2 unit.
 */
class TimesheetKost2StatusTest : AbstractTestBase() {
    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @BeforeEach
    fun setUp() {
        val costConfigured = configurationDao.getEntry(ConfigurationParam.COST_CONFIGURED)!!
        costConfigured.booleanValue = true
        configurationDao.update(costConfigured, checkAccess = false)
    }

    @Test
    fun deactivatedKost2KeepsItsBookings() {
        persistenceService.runInTransaction { _ ->
            logon(AbstractTestBase.TEST_FINANCE_USER)
            val kunde = KundeDO().also {
                it.name = "Kost2-Status-Kunde"
                it.id = 74
                kundeDao.insert(it)
            }
            val projekt = initTestDB.addProjekt(kunde, 74, "Kost2-Status-Projekt", 0, 1)
            val task = initTestDB.addTask("kost2-status", "root")
            projektDao.setTask(projekt, task.id)
            projektDao.update(projekt)
            val kost2A = kost2Dao.getKost2(5, 74, 74, 0)!!
            val kost2B = kost2Dao.getKost2(5, 74, 74, 1)!!
            val user = initTestDB.addUser("kost2-status-user")

            val sheet = TimesheetDO().also {
                it.task = task
                it.kost2 = kost2A
                it.user = user
                it.startTime = date(8)
                it.stopTime = date(9)
                it.description = "booked while active"
            }
            timesheetDao.insert(sheet, checkAccess = false)

            kost2A.kostentraegerStatus = KostentraegerStatus.NONACTIVE
            kost2Dao.update(kost2A)

            // The stored booking stays editable.
            sheet.description = "edited after deactivation"
            timesheetDao.update(sheet, checkAccess = false)

            // But no new booking on it, neither a new sheet nor a switch to it.
            val other = TimesheetDO().also {
                it.task = task
                it.kost2 = kost2B
                it.user = user
                it.startTime = date(10)
                it.stopTime = date(11)
            }
            timesheetDao.insert(other, checkAccess = false)
            // A detached copy, as a request would post it (the inserted one is the stored one in this transaction).
            val switched = timesheetDao.find(other.id, checkAccess = false)!!
            switched.kost2 = kost2A
            assertInvalidKost2 { timesheetDao.update(switched, checkAccess = false) }
            val newSheet = TimesheetDO().also {
                it.task = task
                it.kost2 = kost2A
                it.user = user
                it.startTime = date(12)
                it.stopTime = date(13)
            }
            assertInvalidKost2 { timesheetDao.insert(newSheet, checkAccess = false) }

            // The stored booking can be deleted and undeleted.
            timesheetDao.markAsDeleted(sheet, checkAccess = false)
            timesheetDao.undelete(sheet, checkAccess = false)

            // A deleted cost 2 unit is no longer bookable.
            Assertions.assertTrue(kostCache.getActiveKost2(5, 74, 74).any { it.id == kost2B.id })
            kost2Dao.markAsDeleted(kost2B)
            Assertions.assertFalse(kostCache.getActiveKost2(5, 74, 74).any { it.id == kost2B.id })
            null
        }
    }

    @Test
    fun endedProjectTakesNoNewBookings() {
        persistenceService.runInTransaction { _ ->
            logon(AbstractTestBase.TEST_FINANCE_USER)
            val kunde = KundeDO().also {
                it.name = "Kost2-Ended-Kunde"
                it.id = 75
                kundeDao.insert(it)
            }
            val projekt = initTestDB.addProjekt(kunde, 75, "Kost2-Ended-Projekt", 0)
            val task = initTestDB.addTask("kost2-ended", "root")
            projektDao.setTask(projekt, task.id)
            projektDao.update(projekt)
            val kost2 = kost2Dao.getKost2(5, 75, 75, 0)!!
            val user = initTestDB.addUser("kost2-ended-user")

            val sheet = TimesheetDO().also {
                it.task = task
                it.kost2 = kost2
                it.user = user
                it.startTime = date(8)
                it.stopTime = date(9)
                it.description = "booked while running"
            }
            timesheetDao.insert(sheet, checkAccess = false)

            val ended = projektDao.find(projekt.id)!!
            ended.status = ProjektStatus.ENDED
            projektDao.update(ended)

            // The stored booking stays editable and deletable.
            sheet.description = "edited after the project ended"
            timesheetDao.update(sheet, checkAccess = false)
            timesheetDao.markAsDeleted(sheet, checkAccess = false)
            timesheetDao.undelete(sheet, checkAccess = false)

            // No new booking, neither on its cost 2 unit nor without one (the task has no bookable one left).
            listOf(kost2, null).forEachIndexed { index, newKost2 ->
                val newSheet = TimesheetDO().also {
                    it.task = task
                    it.kost2 = newKost2
                    it.user = user
                    it.startTime = date(10 + 2 * index)
                    it.stopTime = date(11 + 2 * index)
                }
                val ex = assertThrows<UserException> { timesheetDao.insert(newSheet, checkAccess = false) }
                Assertions.assertEquals("timesheet.error.projectEnded", ex.i18nKey)
            }
            null
        }
    }

    private fun assertInvalidKost2(block: () -> Unit) {
        val ex = assertThrows<UserException> { block() }
        Assertions.assertEquals("timesheet.error.invalidKost2", ex.i18nKey)
    }

    private fun date(hour: Int): Date =
        Date.from(LocalDateTime.of(2031, Month.APRIL, 7, hour, 0).atZone(ZoneId.of("UTC")).toInstant())
}
