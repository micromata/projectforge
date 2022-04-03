/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.timesheet

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.task.TaskDao
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.time.DatePrecision
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTime.Companion.from
import org.projectforge.framework.time.PFDateTime.Companion.withDate
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.time.Month
import java.util.*

class TimesheetMassUpdateTest : AbstractTestBase() {
  // private static final Logger log = Logger.getLogger(TaskTest.class);
  @Autowired
  private val timesheetDao: TimesheetDao? = null

  @Autowired
  private val taskDao: TaskDao? = null

  @Autowired
  private val kundeDao: KundeDao? = null

  @Autowired
  private val kost2Dao: Kost2Dao? = null

  @Autowired
  private val projektDao: ProjektDao? = null
  private var date: PFDateTime? = null

  @BeforeEach
  fun setUp() {
    date = from(Date(), null, Locale.GERMAN).withPrecision(DatePrecision.MINUTE_15)
  }

  @Test
  fun massUpdate() {
    val prefix = "ts-mu1-"
    val list: MutableList<TimesheetDO> = ArrayList()
    initTestDB.addTask(prefix + "1", "root")
    initTestDB.addTask(prefix + "1.1", prefix + "1")
    initTestDB.addTask(prefix + "1.2", prefix + "1")
    initTestDB.addTask(prefix + "2", "root")
    initTestDB.addUser(prefix + "user1")
    logon(getUser(TEST_FINANCE_USER))
    list.add(
      createTimesheet(
        prefix,
        "1.1",
        "user1",
        2009,
        Month.NOVEMBER,
        21,
        3,
        0,
        3,
        15,
        "Office",
        "A lot of stuff done and more."
      )
    )
    list.add(
      createTimesheet(
        prefix,
        "1.2",
        "user1",
        2009,
        Month.NOVEMBER,
        21,
        3,
        15,
        3,
        30,
        "Office",
        "A lot of stuff done and more."
      )
    )
    val master = TimesheetDO()
    master.task = initTestDB.getTask(prefix + "2")
    master.location = "Headquarter"
    timesheetDao!!.massUpdate(list, master)
    assertAll(list, master)
  }

  @Test
  fun massUpdateWithKost2Transformation() {
    logon(getUser(TEST_FINANCE_USER))
    val prefix = "ts-mu50-"
    val list: MutableList<TimesheetDO> = ArrayList()
    val kunde = KundeDO()
    kunde.name = "ACME"
    kunde.id = 50
    kundeDao!!.save(kunde)
    val projekt1 = createProjekt(kunde, 1, "Webportal", 0, 1, 2)
    val projekt2 = createProjekt(kunde, 2, "iPhone App", 0, 1)
    val t1 = initTestDB.addTask(prefix + "1", "root")
    projektDao!!.setTask(projekt1, t1.id)
    projektDao.update(projekt1)
    initTestDB.addTask(prefix + "1.1", prefix + "1")
    initTestDB.addTask(prefix + "1.2", prefix + "1")
    val t2 = initTestDB.addTask(prefix + "2", "root")
    projektDao.setTask(projekt2, t2.id)
    projektDao.update(projekt2)
    initTestDB.addTask(prefix + "2.1", prefix + "2")
    initTestDB.addUser(prefix + "user1")
    logon(getUser(TEST_ADMIN_USER))
    list.add(
      createTimesheet(
        prefix, "1.1", "user1", 2009, Month.NOVEMBER, 21, 3, 0, 3, 15, "Office",
        "A lot of stuff done and more.", 5, 50, 1, 0
      )
    )
    list.add(
      createTimesheet(
        prefix, "1.2", "user1", 2009, Month.NOVEMBER, 21, 3, 15, 3, 30, "Office",
        "A lot of stuff done and more.", 5, 50, 1, 1
      )
    )
    list.add(
      createTimesheet(
        prefix, "1.2", "user1", 2009, Month.NOVEMBER, 21, 3, 30, 3, 45, "Office",
        "A lot of stuff done and more.", 5, 50, 1, 2
      )
    )
    val master = TimesheetDO()
    master.task = initTestDB.getTask(prefix + "2")
    master.location = "Headquarter"
    try {
      timesheetDao!!.massUpdate(list, master)
    } catch (e: UserException) {
      // ignore the exception here for testing
    }
    assertSheet(list[0], master)
    assertKost2(list[0], 5, 50, 2, 0) // Kost2 transformed.
    assertSheet(list[1], master)
    assertKost2(list[1], 5, 50, 2, 1) // Kost2 transformed.
    assertKost2(list[2], 5, 50, 1, 2) // Kost2 not transformed.
    Assertions.assertEquals(getTask(prefix + "1.2").id, list[2].taskId)
  }

  @Test
  fun massUpdateWithKost2() {
    logon(getUser(TEST_FINANCE_USER))
    val prefix = "ts-mu51-"
    val list: MutableList<TimesheetDO> = ArrayList()
    val kunde = KundeDO()
    kunde.name = "ACME ltd."
    kunde.id = 51
    kundeDao!!.save(kunde)
    val projekt1 = createProjekt(kunde, 1, "Webportal", 0, 1, 2)
    val projekt2 = createProjekt(kunde, 2, "iPhone App", 0, 1)
    val t1 = initTestDB.addTask(prefix + "1", "root")
    projektDao!!.setTask(projekt1, t1.id)
    projektDao.update(projekt1)
    initTestDB.addTask(prefix + "1.1", prefix + "1")
    initTestDB.addTask(prefix + "1.2", prefix + "1")
    val t2 = initTestDB.addTask(prefix + "2", "root")
    projektDao.setTask(projekt2, t2.id)
    projektDao.update(projekt2)
    initTestDB.addTask(prefix + "2.1", prefix + "2")
    initTestDB.addUser(prefix + "user1")
    logon(getUser(TEST_ADMIN_USER))
    list.add(
      createTimesheet(
        prefix, "1.1", "user1", 2009, Month.NOVEMBER, 21, 3, 0, 3, 15, "Office",
        "A lot of stuff done and more.", 5, 51, 1, 0
      )
    )
    list.add(
      createTimesheet(
        prefix, "1.2", "user1", 2009, Month.NOVEMBER, 21, 3, 15, 3, 30, "Office",
        "A lot of stuff done and more.", 5, 51, 1, 1
      )
    )
    list.add(
      createTimesheet(
        prefix, "1.2", "user1", 2009, Month.NOVEMBER, 21, 3, 30, 3, 45, "Office",
        "A lot of stuff done and more.", 5, 51, 1, 2
      )
    )
    val master = TimesheetDO()
    master.task = initTestDB.getTask(prefix + "2")
    master.location = "Headquarter"
    var kost2 = kost2Dao!!.getKost2(5, 51, 1, 0) // Kost2 is not supported by destination task.
    Assertions.assertNotNull(kost2)
    master.kost2 = kost2
    try {
      timesheetDao!!.massUpdate(list, master)
    } catch (e: UserException) {
      // ignore the exception here for testing
    }
    Assertions.assertEquals(getTask(prefix + "1.1").id, list[0].taskId) // Not moved.
    Assertions.assertEquals(getTask(prefix + "1.2").id, list[1].taskId) // Not moved.
    Assertions.assertEquals(getTask(prefix + "1.2").id, list[2].taskId) // Not moved.
    assertKost2(list[0], 5, 51, 1, 0) // Kost2 not transformed.
    assertKost2(list[1], 5, 51, 1, 1) // Kost2 not transformed.
    assertKost2(list[2], 5, 51, 1, 2) // Kost2 not transformed.
    kost2 = kost2Dao.getKost2(5, 51, 2, 0) // Kost2 supported by destination task.
    Assertions.assertNotNull(kost2)
    master.kost2 = kost2
    timesheetDao!!.massUpdate(list, master)
    assertAll(list, master) // All sheets moved.
    assertKost2(list[0], 5, 51, 2, 0) // Kost2 transformed.
    assertKost2(list[1], 5, 51, 2, 0) // Kost2 transformed.
    assertKost2(list[2], 5, 51, 2, 0) // Kost2 transformed.
  }

  @Test
  fun massUpdateMixedKost2() {
    logon(getUser(TEST_FINANCE_USER))
    val prefix = "ts-mu52-"
    val list: MutableList<TimesheetDO> = ArrayList()
    val kunde = KundeDO()
    kunde.name = "ACME International"
    kunde.id = 52
    kundeDao!!.save(kunde)
    val projekt1 = createProjekt(kunde, 1, "Webportal", 0, 1, 2)
    initTestDB.addTask(prefix + "1", "root")
    initTestDB.addTask(prefix + "1.1", prefix + "1")
    initTestDB.addTask(prefix + "1.2", prefix + "1")
    val t2 = initTestDB.addTask(prefix + "2", "root")
    projektDao!!.setTask(projekt1, t2.id)
    projektDao.update(projekt1)
    initTestDB.addTask(prefix + "2.1", prefix + "2")
    initTestDB.addUser(prefix + "user1")
    logon(getUser(TEST_ADMIN_USER))
    list.add(
      createTimesheet(
        prefix,
        "1.1",
        "user1",
        2009,
        Month.NOVEMBER,
        21,
        3,
        0,
        3,
        15,
        "Office",
        "A lot of stuff done and more."
      )
    )
    list.add(
      createTimesheet(
        prefix,
        "1.2",
        "user1",
        2009,
        Month.NOVEMBER,
        21,
        3,
        15,
        3,
        30,
        "Office",
        "A lot of stuff done and more."
      )
    )
    list.add(
      createTimesheet(
        prefix,
        "1.2",
        "user1",
        2009,
        Month.NOVEMBER,
        21,
        3,
        30,
        3,
        45,
        "Office",
        "A lot of stuff done and more."
      )
    )
    val master = TimesheetDO()
    master.task = initTestDB.getTask(prefix + "2")
    master.location = "Headquarter"
    try {
      timesheetDao!!.massUpdate(list, master)
    } catch (e: UserException) {
      // ignore the exception here for testing
    }
    Assertions.assertEquals(getTask(prefix + "1.1").id, list[0].taskId) // Not moved.
    Assertions.assertEquals(getTask(prefix + "1.2").id, list[1].taskId) // Not moved.
    Assertions.assertEquals(getTask(prefix + "1.2").id, list[2].taskId) // Not moved.
    Assertions.assertNull(list[0].kost2Id) // Kost2 not set.
    Assertions.assertNull(list[1].kost2Id) // Kost2 not set.
    Assertions.assertNull(list[2].kost2Id) // Kost2 not set.
    val kost2 = kost2Dao!!.getKost2(5, 52, 1, 0) // Kost2 supported by destination task.
    Assertions.assertNotNull(kost2)
    master.kost2 = kost2
    timesheetDao!!.massUpdate(list, master)
    assertAll(list, master) // All sheets moved.
    assertKost2(list[0], 5, 52, 1, 0) // Kost2 set.
    assertKost2(list[1], 5, 52, 1, 0) // Kost2 set.
    assertKost2(list[2], 5, 52, 1, 0) // Kost2 set.
  }

  @Test
  fun checkMassUpdateWithTimesheetProtection() {
    logon(getUser(TEST_FINANCE_USER))
    val prefix = "ts-mu53-"
    val list: MutableList<TimesheetDO> = ArrayList()
    val kunde = KundeDO()
    kunde.name = "ACME ltd."
    kunde.id = 53
    kundeDao!!.save(kunde)
    val t1 = initTestDB.addTask(prefix + "1", "root")
    val projekt1 = createProjekt(kunde, 1, "Webportal", 0, 1, 2)
    projekt1.task = t1
    projektDao!!.update(projekt1)
    val projekt2 = createProjekt(kunde, 2, "iPhone App", 0, 1)
    initTestDB.addTask(prefix + "1.1", prefix + "1")
    initTestDB.addTask(prefix + "1.2", prefix + "1")
    val t2 = initTestDB.addTask(prefix + "2", "root")
    projektDao.setTask(projekt2, t2.id)
    projektDao.update(projekt2)
    val dateTime = withDate(2009, Month.DECEMBER, 31)
    t2.protectTimesheetsUntil = dateTime.localDate
    taskDao!!.update(t2)
    initTestDB.addTask(prefix + "2.1", prefix + "2")
    initTestDB.addTask(prefix + "2.2", prefix + "2")
    initTestDB.addUser(prefix + "user")
    val ts1 = createTimesheet(
      prefix, "2.1", "user", 2009, Month.NOVEMBER, 21, 3, 30, 3, 45, "Office",
      "A lot of stuff done and more.",
      5, 53, 2, 0
    )
    list.add(ts1)
    val ts2 = createTimesheet(
      prefix, "1.1", "user", 2009, Month.NOVEMBER, 21, 3, 0, 3, 15, "Office",
      "A lot of stuff done and more.", 5,
      53, 1, 0
    )
    list.add(ts2)
    val ts3 = createTimesheet(
      prefix, "1.2", "user", 2009, Month.NOVEMBER, 21, 3, 15, 3, 30, "Office",
      "A lot of stuff done and more.",
      5, 53, 1, 1
    )
    list.add(ts3)
    logon(getUser(TEST_ADMIN_USER))
    val master = TimesheetDO()
    master.task = initTestDB.getTask(prefix + "2.2")
    try {
      timesheetDao!!.massUpdate(list, master)
    } catch (e: UserException) {
      // ignore the exception here for testing
    }
    assertSheet(list[0], master)
    assertKost2(list[0], 5, 53, 2, 0) // Kost2 unmodified.
    var ts = timesheetDao!!.getById(ts2.id)
    Assertions.assertEquals(getTask(prefix + "1.1").id, ts.taskId) // Not moved.
    assertKost2(ts, 5, 53, 1, 0) // Kost2 unmodified.
    ts = timesheetDao.getById(ts3.id)
    Assertions.assertEquals(getTask(prefix + "1.2").id, ts.taskId) // Not moved.
    assertKost2(ts, 5, 53, 1, 1) // Kost2 unmodified.
  }

  @Test
  fun checkMaxMassUpdateNumber() {
    val list: MutableList<TimesheetDO> = ArrayList()
    for (i in 0..BaseDao.MAX_MASS_UPDATE) {
      list.add(TimesheetDO())
    }
    try {
      timesheetDao!!.massUpdate(list, TimesheetDO())
      Assertions.fail<Any>("Maximum number of allowed mass updates exceeded. Not detected!")
    } catch (ex: UserException) {
      Assertions.assertEquals(BaseDao.MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N, ex.i18nKey)
      // OK.
    }
  }

  private fun createProjekt(
    kunde: KundeDO, projektNummer: Int, projektName: String,
    vararg kost2ArtIds: Int
  ): ProjektDO {
    return initTestDB.addProjekt(kunde, projektNummer, projektName, *kost2ArtIds.toTypedArray())
  }

  private fun assertAll(list: List<TimesheetDO>, master: TimesheetDO) {
    for (sheet in list) {
      assertSheet(sheet, master)
    }
  }

  private fun assertSheet(sheet: TimesheetDO, master: TimesheetDO) {
    if (master.taskId != null) {
      Assertions.assertEquals(master.taskId, sheet.taskId)
    }
    if (master.location != null) {
      Assertions.assertEquals(master.location, sheet.location)
    }
  }

  private fun assertKost2(
    sheet: TimesheetDO, nummernkreis: Int, bereich: Int, teilbereich: Int,
    art: Int
  ) {
    val kost2 = sheet.kost2
    Assertions.assertNotNull(kost2)
    Assertions.assertEquals(nummernkreis, kost2!!.nummernkreis)
    Assertions.assertEquals(bereich, kost2.bereich)
    Assertions.assertEquals(teilbereich, kost2.teilbereich)
    Assertions.assertEquals(art, kost2.kost2ArtId as Int)
  }

  private fun createTimesheet(
    prefix: String,
    taskName: String,
    userName: String,
    year: Int,
    month: Month,
    day: Int,
    fromHour: Int,
    fromMinute: Int,
    toHour: Int,
    toMinute: Int,
    location: String,
    description: String,
    kost2Nummernkreis: Int = 0,
    kost2Bereich: Int =
      0,
    kost2Teilbereich: Int = 0,
    kost2Art: Int = 0
  ): TimesheetDO {
    val ts = TimesheetDO()
    setTimeperiod(ts, year, month, day, fromHour, fromMinute, day, toHour, toMinute)
    ts.task = initTestDB.getTask(prefix + taskName)
    ts.user = getUser(prefix + userName)
    ts.location = location
    ts.description = description
    if (kost2Nummernkreis > 0) {
      val kost2 = kost2Dao!!.getKost2(kost2Nummernkreis, kost2Bereich, kost2Teilbereich, kost2Art)
      Assertions.assertNotNull(kost2)
      ts.kost2 = kost2
    }
    val id: Serializable = timesheetDao!!.internalSave(ts)
    return timesheetDao.getById(id)
  }

  private fun setTimeperiod(
    timesheet: TimesheetDO, year: Int, month: Month, fromDay: Int, fromHour: Int, fromMinute: Int,
    toDay: Int, toHour: Int, toMinute: Int
  ) {
    date = withDate(year, month, fromDay, fromHour, fromMinute, 0)
    timesheet.startTime = date!!.sqlTimestamp
    date = withDate(year, month, toDay, toHour, toMinute, 0)
    timesheet.stopTime = date!!.sqlTimestamp
  }
}
