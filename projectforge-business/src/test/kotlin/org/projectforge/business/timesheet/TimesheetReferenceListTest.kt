/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.time.PFDateTime.Companion.withDate
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.time.Month

class TimesheetReferenceListTest : AbstractTestBase() {
  @Autowired
  private lateinit var timesheetDao: TimesheetDao

  @Test
  fun testTimesheetReferenceLists() {
    logon(ADMIN)
    initTestDB.addUser(user)
    initTestDB.addTask(prefix, "root")
    initTestDB.addTask("$prefix.1", prefix)
    initTestDB.addTask("$prefix.1.1", "$prefix.1")
    initTestDB.addTask("$prefix.1.1.1", "$prefix.1.1")
    initTestDB.addTask("$prefix.1.1.2", "$prefix.1.1")
    initTestDB.addTask("$prefix.1.2", "$prefix.1")
    initTestDB.addTask("$prefix.2", prefix)

    Assertions.assertEquals(0, timesheetDao.getUsedReferences(getTaskId("1")).size)

    var day = 1
    createTimesheet("1", day++, "Reference 1")
    createTimesheet("1", day++, "Reference 1")

    Assertions.assertEquals(1, timesheetDao.getUsedReferences(getTaskId("1")).size)

    createTimesheet("1", day++, "Reference 1a")
    createTimesheet("1", day++, "Reference 1b")
    createTimesheet("1.1", day++, "Reference 1.1a")
    createTimesheet("1.1", day++, "Reference 1.1b")
    createTimesheet("1.2", day++, "Reference 1.2a")
    createTimesheet("1.2", day, "Reference 1.2b")

    //println(timesheetDao.getUsedReferences(getTaskId("1.1")).joinToString { it })
    Assertions.assertEquals(7, timesheetDao.getUsedReferences(getTaskId("1")).size)
    Assertions.assertEquals(5, timesheetDao.getUsedReferences(getTaskId("1.1")).size)
    Assertions.assertEquals(5, timesheetDao.getUsedReferences(getTaskId("1.1.1")).size)
  }

  private fun getTaskId(name: String): Int {
    return initTestDB.getTask("$prefix.$name").id
  }

  private fun createTimesheet(
    taskName: String,
    day: Int,
    reference: String
  ) {
    val ts = TimesheetDO()
    ts.startTime = withDate(2021, Month.MARCH, day, 8, 0, 0).utilDate
    ts.stopTime = withDate(2021, Month.MARCH, day, 9, 0, 0).utilDate
    ts.task = initTestDB.getTask("$prefix.$taskName")
    Assertions.assertNotNull(ts.task, "Task $prefix.$taskName not found.")
    ts.user = getUser(user)
    ts.reference = reference
    timesheetDao.internalSave(ts)
  }

  companion object {
    const val prefix = "trt"
    const val user = "$prefix-user"
  }
}
