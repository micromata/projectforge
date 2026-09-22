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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.Constants
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.*

/**
 * Verifies that the monthly report (attendance view) splits overlapping time of shared cost elements proportionally:
 * 1h booked on project A overlapping 1h booked on project B counts 0.5h for each project, so the net total stays 1h
 * (overlapping time counted once). See [org.projectforge.business.timesheet.TimesheetOverlapUtils].
 */
class MonthlyEmployeeReportOverlapTest : AbstractTestBase() {
    @Autowired
    private lateinit var monthlyEmployeeReportDao: MonthlyEmployeeReportDao

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
    private lateinit var configurationDao: ConfigurationDao

    @BeforeEach
    fun setUp() {
        Configuration.instance.isCostConfigured
        val costConfigured = configurationDao.getEntry(ConfigurationParam.COST_CONFIGURED)!!
        costConfigured.booleanValue = true
        configurationDao.update(costConfigured, checkAccess = false)
    }

    @Test
    fun overlappingTimeIsSplitProportionally() {
        persistenceService.runInTransaction { _ ->
            logon(AbstractTestBase.TEST_FINANCE_USER)
            val kunde = KundeDO().also {
                it.name = "Report-Overlap-Kunde"
                it.id = 72
                kundeDao.insert(it)
            }
            // Two projects, each on its own released task, each with a single cost 2 (art 0, working fraction 1.0).
            val projektA = initTestDB.addProjekt(kunde, 72, "Report-Overlap-A", 0)
            val projektB = initTestDB.addProjekt(kunde, 73, "Report-Overlap-B", 0)
            val taskA = initTestDB.addTask("rep-ovl-A", "root")
            taskA.allowTimeOverlap = true
            taskDao.update(taskA, checkAccess = false)
            projektDao.setTask(projektA, taskA.id)
            projektDao.update(projektA)
            val taskB = initTestDB.addTask("rep-ovl-B", "root")
            taskB.allowTimeOverlap = true
            taskDao.update(taskB, checkAccess = false)
            projektDao.setTask(projektB, taskB.id)
            projektDao.update(projektB)

            val kost2A = kost2Dao.getKost2(5, 72, 72, 0)!!
            val kost2B = kost2Dao.getKost2(5, 72, 73, 0)!!

            val user = initTestDB.addUser("rep-ovl-user")

            // 1h on A fully overlapping 1h on B (different released projects => overlap allowed).
            insert(taskA, kost2A, user, date(8), date(9))
            insert(taskB, kost2B, user, date(8), date(9))

            val report = monthlyEmployeeReportDao.getReport(2031, 3, user)!!

            Assertions.assertEquals(2, report.kost2Durations.size, "Both cost 2 rows expected.")
            Assertions.assertEquals(
                HOUR / 2, report.kost2Durations[kost2A.id]!!.millis,
                "Project A gets half of the overlapping hour."
            )
            Assertions.assertEquals(
                HOUR / 2, report.kost2Durations[kost2B.id]!!.millis,
                "Project B gets half of the overlapping hour."
            )
            // The union of the two overlapping hours is one hour, so the net total must be 1h, not 2h.
            Assertions.assertEquals(HOUR, report.totalNetDuration, "Net total is the union (1h), not the sum (2h).")
            null
        }
    }

    private fun insert(task: TaskDO, kost2: Kost2DO, user: PFUserDO, start: Date, stop: Date) {
        val ts = TimesheetDO()
        ts.task = task
        ts.kost2 = kost2
        ts.user = user
        ts.startTime = start
        ts.stopTime = stop
        timesheetDao.insert(ts, checkAccess = false)
    }

    private fun date(hour: Int, minute: Int = 0): Date =
        Date.from(LocalDateTime.of(2031, Month.MARCH, 10, hour, minute).atZone(ZoneId.of("UTC")).toInstant())

    companion object {
        private const val HOUR = Constants.MILLIS_PER_HOUR
    }
}
