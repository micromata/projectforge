/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
package org.projectforge.business.employee

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.Gender
import org.projectforge.business.fibu.GenderConverter
import org.projectforge.business.fibu.api.EmployeeService
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class EmployeeTest : AbstractTestBase() {
    @Autowired
    private lateinit var employeeDao: EmployeeDao

    private var employeeList: List<EmployeeDO>? = null

    @BeforeEach
    fun init() {
        logon(TEST_FULL_ACCESS_USER)
        employeeList = employeeDao.internalLoadAll()
        Assertions.assertTrue(employeeList!!.size > 0, "Keine Mitarbeiter in der Test DB!")
    }

    @AfterEach
    fun clean() {
        //Get initial infos
        log.info("Cleanup deleted employess -> undelete")
        log.info("Count employees: " + employeeList!!.size)
        Assertions.assertTrue(employeeList!!.size > 0)
        for (e in employeeList!!) {
            log.info("Employee: $e")
            if (e.isDeleted) {
                //Undelete
                employeeDao.internalUndelete(e)
            }
        }
    }

    @Test
    fun testBirthday() {
        //Get initial infos
        log.info("Count employees: " + employeeList!!.size)
        Assertions.assertTrue(employeeList!!.size > 0)
        val e = employeeList!![0]
        log.info("Employee: $e")
        val historyEntries = employeeDao.getDisplayHistoryEntries(e)
        log.info("Employee history entry size: " + historyEntries.size)

        //Update employee
        val birthday = LocalDate.of(1985, Month.DECEMBER, 17)
        e.birthday = birthday
        employeeDao.update(e)

        //Check updates
        val updatdEmployee = employeeDao.getById(e.id)
        Assertions.assertEquals(updatdEmployee.birthday, e.birthday)

        // test history
        val updatedHistoryEntries = employeeDao.getDisplayHistoryEntries(e)
        Assertions.assertEquals(updatedHistoryEntries.size, historyEntries.size + 1)

        //Remove data and check
        e.birthday = null
        employeeDao.update(e)

        //Check updates
        val updatdEmployeeRemove = employeeDao.getById(e.id)
        Assertions.assertNull(updatdEmployeeRemove.birthday)

        // test history
        val updatedHistoryRemoveEntries = employeeDao.getDisplayHistoryEntries(e)
        Assertions.assertEquals(updatedHistoryRemoveEntries.size, historyEntries.size + 2)
    }

    @Test
    fun testMarkAsDeleted() {
        //Get initial infos
        log.info("Count employees: " + employeeList!!.size)
        Assertions.assertTrue(employeeList!!.size > 0)
        val e = employeeList!![0]
        log.info("Employee: $e")

        //Mark as deleted
        employeeDao!!.markAsDeleted(e)

        //Check updates
        val updatdEmployee = employeeDao.getById(e.id)
        Assertions.assertTrue(updatdEmployee.isDeleted)
        employeeDao.update(e)
    }

    @Test
    fun testGender() {
        //Get initial infos
        log.info("Count employees: " + employeeList!!.size)
        Assertions.assertTrue(employeeList!!.size > 0)
        val e = employeeList!![0]
        log.info("Employee: $e")
        val historyEntriesBefore = employeeDao.getDisplayHistoryEntries(e)
        e.gender = Gender.valueOf("NOT_KNOWN")
        Assertions.assertEquals(e.gender, Gender.NOT_KNOWN)
        Assertions.assertEquals(e.gender!!.i18nKey, Gender.NOT_KNOWN.i18nKey)
        Assertions.assertEquals(e.gender!!.ordinal, Gender.NOT_KNOWN.ordinal)
        Assertions.assertEquals(e.gender!!.isoCode, Gender.NOT_KNOWN.isoCode)
        e.gender = Gender.MALE
        Assertions.assertEquals(e.gender, Gender.MALE)
        Assertions.assertEquals(e.gender!!.i18nKey, Gender.MALE.i18nKey)
        Assertions.assertEquals(e.gender!!.ordinal, Gender.MALE.ordinal)
        Assertions.assertEquals(e.gender!!.isoCode, Gender.MALE.isoCode)
        e.gender = Gender.FEMALE
        Assertions.assertEquals(e.gender, Gender.FEMALE)
        Assertions.assertEquals(e.gender!!.i18nKey, Gender.FEMALE.i18nKey)
        Assertions.assertEquals(e.gender!!.ordinal, Gender.FEMALE.ordinal)
        Assertions.assertEquals(e.gender!!.isoCode, Gender.FEMALE.isoCode)
        e.gender = Gender.NOT_APPLICABLE
        Assertions.assertEquals(e.gender, Gender.NOT_APPLICABLE)
        Assertions.assertEquals(e.gender!!.i18nKey, Gender.NOT_APPLICABLE.i18nKey)
        Assertions.assertEquals(e.gender!!.ordinal, Gender.NOT_APPLICABLE.ordinal)
        Assertions.assertEquals(e.gender!!.isoCode, Gender.NOT_APPLICABLE.isoCode)

        // test history
        employeeDao.update(e)
        val historyEntriesAfter = employeeDao.getDisplayHistoryEntries(e)
        Assertions.assertEquals(historyEntriesBefore.size + 1, historyEntriesAfter.size)
        val genderHistoryEntry = historyEntriesAfter[0]
        Assertions.assertEquals(genderHistoryEntry.propertyName, "gender")
        Assertions.assertEquals(genderHistoryEntry.newValue, "[" + Gender.NOT_APPLICABLE.toString() + "]")
    }

    @Test
    fun testGenderConverter() {
        val genderConverter = GenderConverter()
        Assertions.assertEquals(genderConverter.convertToDatabaseColumn(null).toInt(), Gender.NOT_KNOWN.isoCode)
        Assertions.assertEquals(genderConverter.convertToDatabaseColumn(Gender.NOT_KNOWN).toInt(), Gender.NOT_KNOWN.isoCode)
        Assertions.assertEquals(genderConverter.convertToDatabaseColumn(Gender.MALE).toInt(), Gender.MALE.isoCode)
        Assertions.assertEquals(genderConverter.convertToDatabaseColumn(Gender.FEMALE).toInt(), Gender.FEMALE.isoCode)
        Assertions.assertEquals(genderConverter.convertToDatabaseColumn(Gender.NOT_APPLICABLE).toInt(), Gender.NOT_APPLICABLE.isoCode)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(null), Gender.NOT_KNOWN)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(Int.MAX_VALUE), Gender.NOT_KNOWN)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(Int.MIN_VALUE), Gender.NOT_KNOWN)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(0), Gender.NOT_KNOWN)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(1), Gender.MALE)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(2), Gender.FEMALE)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(9), Gender.NOT_APPLICABLE)
    }

    @Test
    fun testBanking() {
        log.info("Count employees: " + employeeList!!.size)
        Assertions.assertTrue(employeeList!!.size > 0)
        val e = employeeList!![0]
        log.info("Employee: $e")
        val historyEntriesBefore = employeeDao.getDisplayHistoryEntries(e)
        val iban = "/*/*/*/*/*///*/*//*//*/*/*/*/*/*/*/*/*/*/*/*/*////"
        e.iban = iban
        Assertions.assertEquals(e.iban, iban)
        employeeDao.update(e) // for history test
        val bic = "ööäööäööäöä"
        e.bic = bic
        Assertions.assertEquals(e.bic, bic)
        employeeDao.update(e) // for history test
        val accountHolder = "Mr. X"
        e.accountHolder = accountHolder
        Assertions.assertEquals(e.accountHolder, accountHolder)
        employeeDao.update(e) // for history test

        // test history
        val historyEntriesAfter = employeeDao.getDisplayHistoryEntries(e)
        Assertions.assertEquals(historyEntriesBefore.size + 3, historyEntriesAfter.size)
        assertHistoryEntry(historyEntriesAfter[0], "accountHolder", accountHolder)
        assertHistoryEntry(historyEntriesAfter[1], "bic", bic)
        assertHistoryEntry(historyEntriesAfter[2], "iban", iban)
    }

    @Test
    fun testAddress() {
        Assertions.assertTrue(employeeList!!.size > 0)
        val e = employeeList!![0]
        val historyEntriesBefore = employeeDao.getDisplayHistoryEntries(e)
        val street = "Some street"
        e.street = street
        Assertions.assertEquals(e.street, street)
        employeeDao.update(e)
        val zipCode = "12345"
        e.zipCode = zipCode
        Assertions.assertEquals(e.zipCode, zipCode)
        employeeDao.update(e)
        val city = "Kassel"
        e.city = city
        Assertions.assertEquals(e.city, city)
        employeeDao.update(e)
        val country = "Deutschland"
        e.country = country
        Assertions.assertEquals(e.country, country)
        employeeDao.update(e)
        val state = "Hessen"
        e.state = state
        Assertions.assertEquals(e.state, state)
        employeeDao.update(e)

        // test history
        val historyEntriesAfter = employeeDao.getDisplayHistoryEntries(e)
        Assertions.assertEquals(historyEntriesBefore.size + 5, historyEntriesAfter.size)
        assertHistoryEntry(historyEntriesAfter[0], "state", state)
        assertHistoryEntry(historyEntriesAfter[1], "country", country)
        assertHistoryEntry(historyEntriesAfter[2], "city", city)
        assertHistoryEntry(historyEntriesAfter[3], "zipCode", zipCode)
        assertHistoryEntry(historyEntriesAfter[4], "street", street)
    }

    @Test
    fun testStaffNumber() {
        Assertions.assertTrue(employeeList!!.size > 0)
        val e = employeeList!![0]
        val historyEntriesBefore = employeeDao.getDisplayHistoryEntries(e)
        val staffNumber = "123abc456def"
        e.staffNumber = staffNumber
        Assertions.assertEquals(e.staffNumber, staffNumber)
        employeeDao.update(e)

        // test history
        val historyEntriesAfter = employeeDao.getDisplayHistoryEntries(e)
        Assertions.assertEquals(historyEntriesBefore.size + 1, historyEntriesAfter.size)
        assertHistoryEntry(historyEntriesAfter[0], "staffNumber", staffNumber)
    }

    private fun assertHistoryEntry(historyEntry: DisplayHistoryEntry, propertyName: String, newValue: String) {
        Assertions.assertEquals(historyEntry.propertyName, propertyName)
        Assertions.assertEquals(historyEntry.newValue, newValue)
    }

    companion object {
        fun createEmployee(employeeService: EmployeeService, employeeDao: EmployeeDao, test: AbstractTestBase, name: String, hrAccess: Boolean = false, groupDao: GroupDao? = null): EmployeeDO {
            val loggedInUser = ThreadLocalUserContext.getUser()
            test.logon(TEST_ADMIN_USER)
            val user = PFUserDO()
            val useName = "${test.javaClass.simpleName}.$name"
            user.firstname = useName
            user.lastname = useName
            user.username = useName
            if (hrAccess) {
                user.addRight(UserRightDO(UserRightId.HR_VACATION, UserRightValue.READWRITE))
            }
            test.initTestDB.addUser(user);
            if (hrAccess) {
                val group = test.getGroup(ProjectForgeGroup.HR_GROUP.toString())
                group.assignedUsers!!.add(user)
                groupDao!!.update(group)
            }
            val employee = EmployeeDO()
            employee.user = user
            employeeService.addNewAnnualLeaveDays(employee, LocalDate.now().minusYears(2), BigDecimal(30));
            employeeDao.internalSave(employee)
            test.logon(loggedInUser)
            return employee
        }
    }
}
