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

package org.projectforge.business.user

import org.apache.commons.collections.CollectionUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.persistence.user.api.UserPrefArea
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserPrefDO
import org.projectforge.framework.persistence.user.entities.UserPrefEntryDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDateTimeUtils.Companion.parseAndCreateDateTime
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.util.*

@Suppress("deprecation")
class UserPrefTest : AbstractTestBase() {
    @Autowired
    private lateinit var userPrefDao: UserPrefDao

    internal class User {
        var firstname: String? = null
        var locale: Locale? = null
        var timeZone: TimeZone? = null
        var lastPasswordChange: Date? = null
        var lastLogin: Date? = null

        companion object {
            fun createTestUser(): User {
                val user = User()
                user.firstname = "Kai"
                user.locale = Locale.GERMAN
                user.timeZone = DateHelper.EUROPE_BERLIN
                val date = parseAndCreateDateTime("2019-06-26 08:33")
                user.lastPasswordChange = date!!.utilDate
                user.lastLogin = date.sqlTimestamp
                return user
            }
        }
    }

    @Test
    fun jsonTest() {
        val loggedInUser = getUser(TEST_USER)
        logon(loggedInUser)
        val user = User.createTestUser()
        var userPref: UserPrefDO? = UserPrefDO()
        userPref!!.user = loggedInUser
        userPref.valueObject = user
        userPref.area = "TEST_AREA"
        userPref.name = ""
        val id = userPrefDao.saveInTrans(userPref)
        userPref = userPrefDao.internalGetById(id)
        val user2 = userPrefDao.deserizalizeValueObject(userPref) as User
        Assertions.assertEquals(User::class.java.name, userPref!!.valueTypeString)
        Assertions.assertEquals(User::class.java, userPref.valueType)
        Assertions.assertEquals(user.firstname, user2.firstname)
        Assertions.assertEquals(user.locale, user2.locale)
        Assertions.assertEquals(user.timeZone, user2.timeZone)
        Assertions.assertEquals(user.lastPasswordChange!!.time, user2.lastPasswordChange!!.time)
        Assertions.assertEquals(user.lastLogin!!.time, user2.lastLogin!!.time)
    }

    @Test
    fun saveAndUpdateTest() {
        val loggedInUser = getUser(TEST_USER)
        logon(loggedInUser)
        var lastStats = countHistoryEntries()
        persistenceService.runInTransaction { context ->
            saveAndUpdateTest("", loggedInUser)
            saveAndUpdateTest("test", loggedInUser)

            var userPref = UserPrefDO()
            userPref.user = loggedInUser
            userPref.area = "TEST_AREA3"
            userPref.name = ""
            addEntry(userPref, "param1", "value1")
            addEntry(userPref, "param2", "value2")
            lastStats = countHistoryEntries()
            userPrefDao.internalSaveOrUpdate(userPref, context)
            lastStats = assertNumberOfNewHistoryEntries(lastStats, 0, 0)
            userPref = userPrefDao.internalQuery(loggedInUser.id!!, "TEST_AREA3", "", context)!!
            Assertions.assertEquals(2, userPref.userPrefEntries!!.size)
            Assertions.assertEquals("value1", userPref.getUserPrefEntryAsString("param1"))
            Assertions.assertEquals("value2", userPref.getUserPrefEntryAsString("param2"))
        }
        persistenceService.runInTransaction { context ->
            val userPref = UserPrefDO()
            userPref.user = loggedInUser
            userPref.area = "TEST_AREA3"
            userPref.name = ""
            addEntry(userPref, "param1", "value1b")
            addEntry(userPref, "param3", "value3")
            userPrefDao.internalSaveOrUpdate(userPref, context)
        }
        lastStats = assertNumberOfNewHistoryEntries(lastStats, 0, 0)
        persistenceService.runReadOnly { context ->
            val userPref = userPrefDao.internalQuery(loggedInUser.id!!, "TEST_AREA3", "", context)!!
            Assertions.assertEquals(2, userPref.userPrefEntries!!.size)
            Assertions.assertEquals("value1b", userPref.getUserPrefEntryAsString("param1"))
            Assertions.assertEquals("value3", userPref.getUserPrefEntryAsString("param3"))
        }
        persistenceService.runInTransaction { context ->
            var userPref = UserPrefDO()
            userPref.user = loggedInUser
            userPref.area = "TEST_AREA3"
            userPref.name = ""
            userPrefDao.internalSaveOrUpdate(userPref, context)
            lastStats = assertNumberOfNewHistoryEntries(lastStats, 0, 0)
            userPref = userPrefDao.internalQuery(loggedInUser.id!!, "TEST_AREA3", "", context)!!
            Assertions.assertTrue(CollectionUtils.isEmpty(userPref.userPrefEntries))

            userPref = UserPrefDO()
            userPref.user = loggedInUser
            userPref.area = "TEST_AREA4"
            userPref.name = ""
            userPrefDao.internalSaveOrUpdate(userPref, context)
            lastStats = assertNumberOfNewHistoryEntries(lastStats, 0, 0)
            userPref = userPrefDao.internalQuery(loggedInUser.id!!, "TEST_AREA4", "", context)!!
            Assertions.assertTrue(CollectionUtils.isEmpty(userPref.userPrefEntries))

            userPref = UserPrefDO()
            userPref.user = loggedInUser
            userPref.area = "TEST_AREA4"
            userPref.name = ""
            addEntry(userPref, "param1", "value1")
            addEntry(userPref, "param2", "value2")
            userPrefDao.internalSaveOrUpdate(userPref, context)
            lastStats = assertNumberOfNewHistoryEntries(lastStats, 0, 0)
            userPref = userPrefDao.internalQuery(loggedInUser.id!!, "TEST_AREA4", "", context)!!
            Assertions.assertEquals(2, userPref.userPrefEntries!!.size)
            Assertions.assertEquals("value1", userPref.getUserPrefEntryAsString("param1"))
            Assertions.assertEquals("value2", userPref.getUserPrefEntryAsString("param2"))
            null
        }
        assertNumberOfNewHistoryEntries(lastStats, 0, 0)
    }

    private fun addEntry(userPref: UserPrefDO, parameter: String, value: String) {
        val entry = UserPrefEntryDO()
        entry.parameter = parameter
        entry.value = value
        userPref.addOrUpdateUserPrefEntry(entry)
    }


    private fun saveAndUpdateTest(name: String, loggedInUser: PFUserDO) {
        val user = User.createTestUser()
        val lastStats = countHistoryEntries()
        var userPref = UserPrefDO()
        userPref.user = loggedInUser
        userPref.valueObject = user
        userPref.area = "TEST_AREA2"
        userPref.name = name
        userPrefDao.internalSaveOrUpdateInTrans(userPref)
        assertNumberOfNewHistoryEntries(lastStats, 0, 0)
        val id = userPref.id
        userPref = UserPrefDO()
        userPref.user = loggedInUser
        userPref.valueObject = user
        userPref.area = "TEST_AREA2"
        userPref.name = name
        userPrefDao.internalSaveOrUpdateInTrans(userPref)
        assertNumberOfNewHistoryEntries(lastStats, 0, 0)
        Assertions.assertEquals(id, userPref.id, "Object should be updated not inserted.")
    }

    @Test
    fun convertPrefParameters() {
        val user = getUser(TEST_USER)
        logon(user)
        persistenceService.runInTransaction { context ->
            val user2 = getUser(TEST_USER2)
            val task = initTestDB.addTask("UserPrefTest", "root", context)
            var userPref = createUserPref(user, UserPrefArea.TIMESHEET_TEMPLATE, "test")
            var timesheet = createTimesheet(user2, task, "Micromata", "Wrote a test case...")
            userPrefDao.addUserPrefParameters(userPref, timesheet)
            Assertions.assertFalse(
                userPrefDao.doesParameterNameAlreadyExist(
                    null,
                    user,
                    UserPrefArea.TIMESHEET_TEMPLATE,
                    "test"
                )
            )
            val id: Serializable = userPrefDao.save(userPref, context)
            Assertions.assertTrue(
                userPrefDao.doesParameterNameAlreadyExist(
                    null,
                    user,
                    UserPrefArea.TIMESHEET_TEMPLATE,
                    "test"
                )
            )
            Assertions.assertFalse(
                userPrefDao.doesParameterNameAlreadyExist(
                    id as Long,
                    user,
                    UserPrefArea.TIMESHEET_TEMPLATE,
                    "test"
                )
            )
            userPref = userPrefDao.getById(id, context)!!
            Assertions.assertEquals(5, userPref.userPrefEntries!!.size) // user, task, kost2, location, description.
            run {
                val it = userPref.sortedUserPrefEntries.iterator()
                var entry = it.next()
                assertUserPrefEntry(entry, "user", PFUserDO::class.java, user2.id.toString(), "user", null, "1")
                userPrefDao.updateParameterValueObject(entry)
                Assertions.assertEquals(user2.id, (entry.valueAsObject as PFUserDO).id)
                entry = it.next()
                assertUserPrefEntry(entry, "task", TaskDO::class.java, task.id.toString(), "task", null, "2")
                userPrefDao.updateParameterValueObject(entry)
                Assertions.assertEquals(task.id, (entry.valueAsObject as TaskDO).id)
                entry = it.next()
                assertUserPrefEntry(entry, "kost2", Kost2DO::class.java, null, "fibu.kost2", null, "3")
                entry = it.next()
                assertUserPrefEntry(
                    entry,
                    "location",
                    String::class.java,
                    "Micromata",
                    "timesheet.location",
                    100,
                    "ZZZ00"
                )
                entry = it.next()
                assertUserPrefEntry(
                    entry, "description",
                    String::class.java, "Wrote a test case...", "description", 4000, "ZZZ01"
                )
            }
            timesheet = TimesheetDO()
            userPrefDao.fillFromUserPrefParameters(userPref, timesheet)
            Assertions.assertEquals(user2.id, timesheet.userId)
            Assertions.assertEquals(task.id, timesheet.taskId)
            Assertions.assertNull(timesheet.kost2Id)
            Assertions.assertEquals("Micromata", timesheet.location)
            Assertions.assertEquals("Wrote a test case...", timesheet.description)
            userPref.getUserPrefEntry("location")!!.value = "At home"
            userPrefDao.update(userPref, context)
            val names = userPrefDao.getPrefNames(UserPrefArea.TIMESHEET_TEMPLATE)
            Assertions.assertEquals(1, names.size)
            Assertions.assertEquals("test", names[0])
            var dependents = userPref
                .getDependentUserPrefEntries(userPref.getUserPrefEntry("user")!!.parameter!!)
            Assertions.assertNull(dependents)
            dependents = userPref.getDependentUserPrefEntries(userPref.getUserPrefEntry("task")!!.parameter!!)
            Assertions.assertEquals(1, dependents!!.size)
            Assertions.assertEquals("kost2", dependents[0].parameter)
            null
        }
    }

    private fun assertUserPrefEntry(
        userPrefEntry: UserPrefEntryDO, parameter: String, type: Class<*>,
        valueAsString: String?, i18nKey: String, maxLength: Int?, orderString: String
    ) {
        Assertions.assertEquals(parameter, userPrefEntry.parameter)
        Assertions.assertEquals(type, userPrefEntry.type)
        Assertions.assertEquals(i18nKey, userPrefEntry.i18nKey)
        Assertions.assertEquals(maxLength, userPrefEntry.maxLength)
        Assertions.assertEquals(orderString, userPrefEntry.orderString)
        Assertions.assertEquals(valueAsString, userPrefEntry.value)
    }

    private fun createTimesheet(
        user: PFUserDO, task: TaskDO, location: String,
        description: String
    ): TimesheetDO {
        val timesheet = TimesheetDO()
        timesheet.user = user
        timesheet.task = task
        timesheet.location = location
        timesheet.description = description
        return timesheet
    }

    private fun createUserPref(user: PFUserDO, area: UserPrefArea, name: String): UserPrefDO {
        val userPref = UserPrefDO()
        userPref.user = user
        userPref.areaObject = area
        userPref.name = name
        return userPref
    }
}
