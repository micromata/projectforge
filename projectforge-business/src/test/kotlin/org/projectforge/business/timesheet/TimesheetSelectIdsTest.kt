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
import org.projectforge.business.task.TaskDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.*

/**
 * [TimesheetDao.selectIds] selects the ids instead of the sheets and checks the access per task and user
 * ([TimesheetDao.selectAccessProjection]): it must yield the ids [TimesheetDao.select] returns, in the same order,
 * for every user, with the same cap, and so must the fallback over the loaded sheets (a custom result filter).
 */
class TimesheetSelectIdsTest : AbstractTestBase() {
    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Test
    fun idsMatchTheAccessibleSheetsInTheirOrder() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val projectTask = initTestDB.addTask("ids-project", "root")
            val subTask = initTestDB.addTask("ids-sub", "ids-project")
            val otherTask = initTestDB.addTask("ids-other", "root")
            val testUser = getUser(TEST_USER)
            val foreignUser = initTestDB.addUser("ids-foreign-user")
            var day = 1
            fun book(task: TaskDO, user: org.projectforge.framework.persistence.user.entities.PFUserDO) {
                timesheetDao.insert(TimesheetDO().also {
                    it.task = task
                    it.user = user
                    it.startTime = date(day++, 8)
                    it.stopTime = date(day - 1, 9)
                    it.description = "ids test"
                }, checkAccess = false)
            }
            book(projectTask, testUser)
            book(subTask, foreignUser)
            book(subTask, testUser)
            book(otherTask, foreignUser)
            book(otherTask, testUser)

            val taskIds = listOf(projectTask.id!!, subTask.id!!, otherTask.id!!)
            fun criteria(descending: Boolean = true) = QueryFilter().also {
                it.add(QueryFilter.isIn("task.id", taskIds))
                it.deleted = false
                it.addOrder(if (descending) SortProperty.desc("startTime") else SortProperty.asc("startTime"))
            }
            listOf(TEST_FINANCE_USER, TEST_USER, TEST_USER2, TEST_PROJECT_MANAGER_USER, TEST_ADMIN_USER).forEach { user ->
                logon(user)
                listOf(true, false).forEach { descending ->
                    val expected = timesheetDao.select(criteria(descending)).map { it.id!! }
                    Assertions.assertEquals(expected, ids(criteria(descending)), "$user, descending=$descending")
                    Assertions.assertEquals(expected, ids(criteria(descending), everySheet), "$user, fallback")
                }
            }
            logon(TEST_FINANCE_USER)
            val all = timesheetDao.select(criteria()).map { it.id!! }
            Assertions.assertTrue(all.size >= 5)
            // The cap counts the rows granted, as the loaded path does.
            val capped = timesheetDao.selectIds(criteria().also { it.maxRows = 2 }, null)
            Assertions.assertEquals(all.take(2), capped.ids.toList())
            Assertions.assertTrue(capped.truncated)
            null
        }
    }

    private fun ids(filter: QueryFilter, resultFilter: CustomResultFilter<TimesheetDO>? = null) =
        timesheetDao.selectIds(filter, listOfNotNull(resultFilter)).ids.toList()

    /** Matches every sheet, but only on the loaded ones: forces the fallback. */
    private val everySheet = object : CustomResultFilter<TimesheetDO> {
        override fun match(list: MutableList<TimesheetDO>, element: TimesheetDO) = true
    }

    private fun date(day: Int, hour: Int): Date =
        Date.from(LocalDateTime.of(2026, Month.MARCH, day, hour, 0).atZone(ZoneId.of("UTC")).toInstant())
}
