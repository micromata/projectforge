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
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.*

/**
 * [TimesheetDao.selectDistinct] selects the cost 2 ids instead of the sheets and checks the access per task and
 * user ([TimesheetDao.selectAccessProjection]): it must yield what the sheets [TimesheetDao.select] returns
 * hold, for every user, and so must the fallback over the loaded sheets (a custom result filter).
 */
class TimesheetSelectDistinctTest : AbstractTestBase() {
    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Test
    fun distinctKost2IdsMatchTheAccessibleSheets() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val costConfigured = configurationDao.getEntry(ConfigurationParam.COST_CONFIGURED)!!
            costConfigured.booleanValue = true
            configurationDao.update(costConfigured, checkAccess = false)
            val kunde = KundeDO().also {
                it.name = "Distinct-Kunde"
                it.id = 76
                kundeDao.insert(it)
            }
            val projekt = initTestDB.addProjekt(kunde, 76, "Distinct-Projekt", 0, 1)
            val projectTask = initTestDB.addTask("distinct-project", "root")
            val subTask = initTestDB.addTask("distinct-sub", "distinct-project")
            val otherTask = initTestDB.addTask("distinct-other", "root")
            projektDao.setTask(projekt, projectTask.id)
            projektDao.update(projekt)
            val kost2A = kost2Dao.getKost2(5, 76, 76, 0)!!
            val kost2B = kost2Dao.getKost2(5, 76, 76, 1)!!
            val testUser = getUser(TEST_USER)
            val foreignUser = initTestDB.addUser("distinct-foreign-user")
            var hour = 8
            fun book(task: TaskDO, kost2: Kost2DO?, user: PFUserDO) {
                timesheetDao.insert(TimesheetDO().also {
                    it.task = task
                    it.kost2 = kost2
                    it.user = user
                    it.startTime = date(hour++)
                    it.stopTime = date(hour)
                    it.description = "projection test"
                }, checkAccess = false)
            }
            book(projectTask, kost2A, testUser)
            book(subTask, kost2A, foreignUser)
            book(subTask, kost2B, foreignUser)
            book(otherTask, null, foreignUser)

            val taskIds = listOf(projectTask.id!!, subTask.id!!, otherTask.id!!)
            fun criteria() = QueryFilter().also {
                it.add(QueryFilter.isIn("task.id", taskIds))
                it.deleted = false
            }
            logon(TEST_FINANCE_USER)
            Assertions.assertEquals(setOf(kost2A.id, kost2B.id, null), distinct(criteria()))
            // Only the loaded sheets are capped by maxRows: proves the criteria query was answered by the projection.
            Assertions.assertEquals(3, distinct(criteria().also { it.maxRows = 1 }).size)
            Assertions.assertEquals(1, distinct(criteria().also { it.maxRows = 1 }, everySheet).size)
            listOf(TEST_FINANCE_USER, TEST_USER, TEST_USER2, TEST_PROJECT_MANAGER_USER, TEST_ADMIN_USER).forEach { user ->
                logon(user)
                Assertions.assertEquals(selected(criteria()), distinct(criteria()), user)
                Assertions.assertEquals(selected(criteria()), distinct(criteria(), everySheet), user)
            }
            null
        }
    }

    private fun distinct(filter: QueryFilter, resultFilter: CustomResultFilter<TimesheetDO>? = null) =
        timesheetDao.selectDistinct(filter, listOfNotNull(resultFilter), "kost2.id").toSet()

    /** Matches every sheet, but only on the loaded ones: forces the fallback. */
    private val everySheet = object : CustomResultFilter<TimesheetDO> {
        override fun match(list: MutableList<TimesheetDO>, element: TimesheetDO) = true
    }

    private fun selected(filter: QueryFilter) = timesheetDao.select(filter).map { it.kost2?.id }.toSet()

    private fun date(hour: Int): Date =
        Date.from(LocalDateTime.of(2026, Month.MARCH, 2, hour, 0).atZone(ZoneId.of("UTC")).toInstant())
}
