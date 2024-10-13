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

package org.projectforge.rest

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.task.TaskDao
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.time.DatePrecision
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTime.Companion.from
import org.projectforge.framework.time.PFDateTime.Companion.withDate
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.time.Month
import java.util.*

class TimesheetMassUpdateTest : AbstractTestBase() {
    // private static final Logger log = Logger.getLogger(TaskTest.class);
    @Autowired
    private lateinit var configurationDao: ConfigurationDao

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var timesheetMultiSelectedPageRest: TimesheetMultiSelectedPageRest

    private var date: PFDateTime? = null

    @BeforeEach
    fun setUp() {
        date = from(Date(), null, Locale.GERMAN).withPrecision(DatePrecision.MINUTE_15)
        Configuration.instance.isCostConfigured
        val costConfigured = configurationDao.getEntry(ConfigurationParam.COST_CONFIGURED)
        costConfigured.booleanValue = true
        configurationDao.internalUpdate(costConfigured)
    }

    @Test
    fun massUpdate() {
        val prefix = "ts-mu1-"
        val list = mutableListOf<TimesheetDO>()
        persistenceService.runInTransaction { _ ->
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
                    "A lot of stuff done and more.",
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
                    "A lot of stuff done and more.",
                )
            )
        }
        val master = TimesheetDO()
        master.task = initTestDB.getTask(prefix + "2")
        master.location = "Headquarter"
        val dbList = massUpdate(list, master)
        assertAll(dbList, master)
    }

    @Test
    fun massUpdateWithKost2Transformation() {
        val prefix = "ts-mu50-"
        val list = mutableListOf<TimesheetDO>()
        persistenceService.runInTransaction { _ ->
            logon(getUser(TEST_FINANCE_USER))
            val kunde = KundeDO()
            kunde.name = "ACME"
            kunde.id = 50
            kundeDao.save(kunde)
            val projekt1 = createProjekt(kunde, 1, "Webportal", 0, 1, 2)
            val projekt2 = createProjekt(kunde, 2, "iPhone App", 0, 1)
            val t1 = initTestDB.addTask(prefix + "1", "root")
            projektDao.setTask(projekt1, t1.id)
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
                    "TS#0", 5, 50, 1, 0,
                )
            )
            list.add(
                createTimesheet(
                    prefix, "1.2", "user1", 2009, Month.NOVEMBER, 21, 3, 15, 3, 30, "Office",
                    "TS#1", 5, 50, 1, 1,
                )
            )
            list.add(
                createTimesheet(
                    prefix, "1.2", "user1", 2009, Month.NOVEMBER, 21, 3, 30, 3, 45, "Office",
                    "TS#2", 5, 50, 1, 2,
                )
            )
        }
        val master = TimesheetDO()
        master.task = initTestDB.getTask(prefix + "2")
        master.location = "Headquarter"

        val dbList = massUpdate(list, master)
        assertSheet(dbList.find { it.description == "TS#0" }!!, master)
        assertKost2(dbList.find { it.description == "TS#0" }!!, 5, 50, 2, 0) // Kost2 transformed.
        assertSheet(dbList.find { it.description == "TS#1" }!!, master)
        assertKost2(dbList.find { it.description == "TS#1" }!!, 5, 50, 2, 1) // Kost2 transformed.
        assertKost2(dbList.find { it.description == "TS#2" }!!, 5, 50, 1, 2) // Kost2 not transformed.
        Assertions.assertEquals(getTask(prefix + "1.2").id, dbList.find { it.description == "TS#2" }!!.taskId)
    }

    @Test
    fun massUpdateWithKost2() {
        val prefix = "ts-mu51-"
        val list = mutableListOf<TimesheetDO>()
        persistenceService.runInTransaction { _ ->
            logon(getUser(TEST_FINANCE_USER))
            val kunde = KundeDO()
            kunde.name = "ACME ltd."
            kunde.id = 51
            kundeDao.save(kunde)
            val projekt1 = createProjekt(kunde, 1, "Webportal", 0, 1, 2)
            val projekt2 = createProjekt(kunde, 2, "iPhone App", 0, 1)
            val t1 = initTestDB.addTask(prefix + "1", "root")
            projektDao.setTask(projekt1, t1.id)
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
                    "TS#0", 5, 51, 1, 0
                )
            )
            list.add(
                createTimesheet(
                    prefix, "1.2", "user1", 2009, Month.NOVEMBER, 21, 3, 15, 3, 30, "Office",
                    "TS#1", 5, 51, 1, 1
                )
            )
            list.add(
                createTimesheet(
                    prefix, "1.2", "user1", 2009, Month.NOVEMBER, 21, 3, 30, 3, 45, "Office",
                    "TS#2", 5, 51, 1, 2
                )
            )
        }
        val master = TimesheetDO()
        master.task = initTestDB.getTask(prefix + "2")
        master.location = "Headquarter"
        var kost2 = kost2Dao.getKost2(5, 51, 1, 0) // Kost2 is not supported by destination task.
        Assertions.assertNotNull(kost2)
        master.kost2 = kost2

        var dbList = massUpdate(list, master)
        Assertions.assertEquals(
            getTask(prefix + "1.1").id,
            dbList.find { it.description == "TS#0" }?.taskId
        ) // Not moved.
        Assertions.assertEquals(
            getTask(prefix + "1.2").id,
            dbList.find { it.description == "TS#1" }?.taskId
        ) // Not moved.
        Assertions.assertEquals(
            getTask(prefix + "1.2").id,
            dbList.find { it.description == "TS#2" }?.taskId
        ) // Not moved.
        assertKost2(dbList[0], 5, 51, 1, 0) // Kost2 not transformed.
        assertKost2(dbList[1], 5, 51, 1, 1) // Kost2 not transformed.
        assertKost2(dbList[2], 5, 51, 1, 2) // Kost2 not transformed.
        kost2 = kost2Dao.getKost2(5, 51, 2, 0) // Kost2 supported by destination task.
        kost2 = kost2Dao.getKost2(5, 51, 2, 0) // Kost2 supported by destination task.
        Assertions.assertNotNull(kost2)
        master.kost2 = kost2
        dbList = massUpdate(dbList, master)
        assertAll(dbList, master) // All sheets moved.
        assertKost2(dbList[0], 5, 51, 2, 0) // Kost2 transformed.
        assertKost2(dbList[1], 5, 51, 2, 0) // Kost2 transformed.
        assertKost2(dbList[2], 5, 51, 2, 0) // Kost2 transformed.
    }

    @Test
    fun massUpdateMixedKost2() {
        val list = mutableListOf<TimesheetDO>()
        val prefix = "ts-mu52-"
        persistenceService.runInTransaction { _ ->
            logon(getUser(TEST_FINANCE_USER))
            val kunde = KundeDO()
            kunde.name = "ACME International"
            kunde.id = 52
            kundeDao.save(kunde)
            val projekt1 = createProjekt(kunde, 1, "Webportal", 0, 1, 2)
            initTestDB.addTask(prefix + "1", "root")
            initTestDB.addTask(prefix + "1.1", prefix + "1")
            initTestDB.addTask(prefix + "1.2", prefix + "1")
            val t2 = initTestDB.addTask(prefix + "2", "root")
            projektDao.setTask(projekt1, t2.id)
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
        }
        val master = TimesheetDO()
        master.task = initTestDB.getTask(prefix + "2")
        master.location = "Headquarter"

        var dbList = massUpdate(list, master)

        Assertions.assertEquals(getTask(prefix + "1.1").id, dbList[0].taskId) // Not moved.
        Assertions.assertEquals(getTask(prefix + "1.2").id, dbList[1].taskId) // Not moved.
        Assertions.assertEquals(getTask(prefix + "1.2").id, dbList[2].taskId) // Not moved.
        Assertions.assertNull(dbList[0].kost2Id) // Kost2 not set.
        Assertions.assertNull(dbList[1].kost2Id) // Kost2 not set.
        Assertions.assertNull(dbList[2].kost2Id) // Kost2 not set.
        val kost2 = kost2Dao.getKost2(5, 52, 1, 0) // Kost2 supported by destination task.
        Assertions.assertNotNull(kost2)
        master.kost2 = kost2
        dbList = massUpdate(list, master)
        assertAll(dbList, master) // All sheets moved.
        assertKost2(dbList[0], 5, 52, 1, 0) // Kost2 set.
        assertKost2(dbList[1], 5, 52, 1, 0) // Kost2 set.
        assertKost2(dbList[2], 5, 52, 1, 0) // Kost2 set.
    }

    @Test
    fun checkMassUpdateWithTimesheetProtection() {
        val prefix = "ts-mu53-"
        val list = mutableListOf<TimesheetDO>()
        persistenceService.runInTransaction { _ ->
            logon(getUser(TEST_FINANCE_USER))
            val kunde = KundeDO()
            kunde.name = "ACME ltd."
            kunde.id = 53
            kundeDao.save(kunde)
            val t1 = initTestDB.addTask(prefix + "1", "root")
            val projekt1 = createProjekt(kunde, 1, "Webportal", 0, 1, 2)
            projekt1.task = t1
            projektDao.update(projekt1)
            val projekt2 = createProjekt(kunde, 2, "iPhone App", 0, 1)
            initTestDB.addTask(prefix + "1.1", prefix + "1")
            initTestDB.addTask(prefix + "1.2", prefix + "1")
            val t2 = initTestDB.addTask(prefix + "2", "root")
            projektDao.setTask(projekt2, t2.id)
            projektDao.update(projekt2)
            val dateTime = withDate(2009, Month.DECEMBER, 31)
            t2.protectTimesheetsUntil = dateTime.localDate
            taskDao.update(t2)
            initTestDB.addTask(prefix + "2.1", prefix + "2")
            initTestDB.addTask(prefix + "2.2", prefix + "2")
            initTestDB.addUser(prefix + "user")
            list.add(
                createTimesheet(
                    prefix, "2.1", "user", 2009, Month.NOVEMBER, 21, 3, 30, 3, 45, "Office",
                    "TS#1",
                    5, 53, 2, 0
                )
            )
            list.add(
                createTimesheet(
                    prefix, "1.1", "user", 2009, Month.NOVEMBER, 21, 3, 0, 3, 15, "Office",
                    "TS#2", 5,
                    53, 1, 0
                )
            )
            list.add(
                createTimesheet(
                    prefix, "1.2", "user", 2009, Month.NOVEMBER, 21, 3, 15, 3, 30, "Office",
                    "TS#3",
                    5, 53, 1, 1
                )
            )
        }
        logon(getUser(TEST_ADMIN_USER))
        val master = TimesheetDO()
        master.task = initTestDB.getTask(prefix + "2.2")

        val dbList = massUpdate(list, master)
        var ts = dbList.find { it.description == "TS#1" }!!
        assertSheet(ts, list[0], "Task not changed due to protectionUntil")
        assertKost2(ts, 5, 53, 2, 0) // Kost2 unmodified.
        ts = timesheetDao.getById(list[1].id)!!
        Assertions.assertEquals(getTask(prefix + "1.1").id, ts.taskId) // Not moved.
        assertKost2(ts, 5, 53, 1, 0) // Kost2 unmodified.
        ts = timesheetDao.getById(list[2].id)!!
        Assertions.assertEquals(getTask(prefix + "1.2").id, ts.taskId) // Not moved.
        assertKost2(ts, 5, 53, 1, 1) // Kost2 unmodified.
    }

    @Test
    fun checkMaxMassUpdateNumber() {
        val list = mutableListOf<TimesheetDO>()
        for (i in 0L..BaseDao.MAX_MASS_UPDATE) {
            val ts = TimesheetDO()
            ts.id = i
            list.add(ts)
        }
        try {
            val master = TimesheetDO()
            master.location = "test"
            massUpdate(list, master)
            Assertions.fail<Any>("Maximum number of allowed mass updates exceeded. Not detected!")
        } catch (ex: UserException) {
            Assertions.assertEquals(BaseDao.MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N, ex.i18nKey)
            // OK.
        }
    }

    private fun createProjekt(
        kunde: KundeDO, projektNummer: Int, projektName: String,
        vararg kost2ArtIds: Long,
    ): ProjektDO {
        return initTestDB.addProjekt(kunde, projektNummer, projektName, *kost2ArtIds)
    }

    private fun assertAll(list: List<TimesheetDO>, master: TimesheetDO) {
        for (sheet in list) {
            assertSheet(sheet, master)
        }
    }

    private fun assertSheet(sheet: TimesheetDO, master: TimesheetDO, message: String? = null) {
        if (master.taskId != null) {
            Assertions.assertEquals(master.taskId, sheet.taskId, message)
        }
        if (master.location != null) {
            Assertions.assertEquals(master.location, sheet.location, message)
        }
    }

    private fun assertKost2(
        sheet: TimesheetDO, nummernkreis: Int, bereich: Int, teilbereich: Int,
        art: Long
    ) {
        val kost2 = sheet.kost2
        Assertions.assertNotNull(kost2)
        Assertions.assertEquals(nummernkreis, kost2!!.nummernkreis)
        Assertions.assertEquals(bereich, kost2.bereich)
        Assertions.assertEquals(teilbereich, kost2.teilbereich)
        Assertions.assertEquals(art, kost2.kost2ArtId as Long)
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
        kost2Art: Int = 0,
    ): TimesheetDO {
        val ts = TimesheetDO()
        setTimeperiod(ts, year, month, day, fromHour, fromMinute, day, toHour, toMinute)
        ts.task = initTestDB.getTask(prefix + taskName)
        ts.user = getUser(prefix + userName)
        ts.location = location
        ts.description = description
        if (kost2Nummernkreis > 0) {
            val kost2 = kost2Dao.getKost2(kost2Nummernkreis, kost2Bereich, kost2Teilbereich, kost2Art.toLong())
            Assertions.assertNotNull(kost2)
            ts.kost2 = kost2
        }
        val id: Serializable = timesheetDao.internalSave(ts)!!
        return timesheetDao.getById(id)!!
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

    private fun massUpdate(
        list: List<TimesheetDO>,
        master: TimesheetDO,
    ): List<TimesheetDO> {
        val massUpdateData = mutableMapOf<String, MassUpdateParameter>()
        if (master.location != null) {
            MassUpdateParameter().let { param ->
                massUpdateData["location"] = param
                param.textValue = master.location
            }
        }
        if (master.taskId != null || master.kost2Id != null) {
            MassUpdateParameter().let { param ->
                massUpdateData["taskAndKost2"] = param
                param.change = true
            }
            if (master.taskId != null) {
                MassUpdateParameter().let { param ->
                    massUpdateData["task"] = param
                    param.id = master.taskId
                }
            }
            if (master.kost2Id != null) {
                MassUpdateParameter().let { param ->
                    massUpdateData["kost2"] = param
                    param.id = master.kost2Id
                }
            }
        }
        val massUpdateContext = object : MassUpdateContext<TimesheetDO>(massUpdateData) {
            override fun getId(obj: TimesheetDO): Long {
                return obj.id!!
            }
        }
        val selectedIds = list.map { it.id!! }
        val request = Mockito.mock(HttpServletRequest::class.java)
        timesheetMultiSelectedPageRest.massUpdate(request, selectedIds, massUpdateContext)?.let {
            throw UserException(BaseDao.MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N)
        }

        val dbList = timesheetDao.getListByIds(selectedIds)
        return dbList!!
    }
}
