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

package org.projectforge.business.employee

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeDao
import org.projectforge.business.fibu.GenderConverter
import org.projectforge.business.fibu.IsoGender
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

    private lateinit var employeeList: List<EmployeeDO>

    @BeforeEach
    fun init() {
        logon(TEST_FULL_ACCESS_USER)
        employeeList = employeeDao.internalLoadAll()
        Assertions.assertTrue(employeeList.isNotEmpty(), "Keine Mitarbeiter in der Test DB!")
    }

    @AfterEach
    fun clean() {
        //Get initial infos
        baseLog.info("Cleanup deleted employess -> undelete")
        baseLog.info("Count employees: " + employeeList.size)
        Assertions.assertTrue(employeeList.isNotEmpty())
        for (e in employeeList) {
            baseLog.info("Employee: $e")
            if (e.isDeleted) {
                //Undelete
                employeeDao.internalUndelete(e)
            }
        }
    }

    @Test
    fun testBirthday() {
        //Get initial infos
        baseLog.info("Count employees: " + employeeList.size)
        Assertions.assertTrue(employeeList.isNotEmpty())
        val e = employeeList[0]
        baseLog.info("Employee: $e")
        val historyEntries = employeeDao.getDisplayHistoryEntries(e)
        baseLog.info("Employee history entry size: " + historyEntries.size)

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
        baseLog.info("Count employees: " + employeeList.size)
        Assertions.assertTrue(employeeList.isNotEmpty())
        val e = employeeList[0]
        baseLog.info("Employee: $e")

        //Mark as deleted
        employeeDao.markAsDeleted(e)

        //Check updates
        val updatdEmployee = employeeDao.getById(e.id)
        Assertions.assertTrue(updatdEmployee.isDeleted)
        employeeDao.update(e)
    }

    @Test
    fun testGender() {
        //Get initial infos
        baseLog.info("Count employees: " + employeeList.size)
        Assertions.assertTrue(employeeList.isNotEmpty())
        val e = employeeList[0]
        baseLog.info("Employee: $e")
        val historyEntriesBefore = employeeDao.getDisplayHistoryEntries(e)
        e.gender = IsoGender.valueOf("NOT_KNOWN")
        Assertions.assertEquals(e.gender, IsoGender.NOT_KNOWN)
        Assertions.assertEquals(e.gender!!.i18nKey, IsoGender.NOT_KNOWN.i18nKey)
        Assertions.assertEquals(e.gender!!.ordinal, IsoGender.NOT_KNOWN.ordinal)
        Assertions.assertEquals(e.gender!!.isoCode, IsoGender.NOT_KNOWN.isoCode)
        e.gender = IsoGender.MALE
        Assertions.assertEquals(e.gender, IsoGender.MALE)
        Assertions.assertEquals(e.gender!!.i18nKey, IsoGender.MALE.i18nKey)
        Assertions.assertEquals(e.gender!!.ordinal, IsoGender.MALE.ordinal)
        Assertions.assertEquals(e.gender!!.isoCode, IsoGender.MALE.isoCode)
        e.gender = IsoGender.FEMALE
        Assertions.assertEquals(e.gender, IsoGender.FEMALE)
        Assertions.assertEquals(e.gender!!.i18nKey, IsoGender.FEMALE.i18nKey)
        Assertions.assertEquals(e.gender!!.ordinal, IsoGender.FEMALE.ordinal)
        Assertions.assertEquals(e.gender!!.isoCode, IsoGender.FEMALE.isoCode)
        e.gender = IsoGender.NOT_APPLICABLE
        Assertions.assertEquals(e.gender, IsoGender.NOT_APPLICABLE)
        Assertions.assertEquals(e.gender!!.i18nKey, IsoGender.NOT_APPLICABLE.i18nKey)
        Assertions.assertEquals(e.gender!!.ordinal, IsoGender.NOT_APPLICABLE.ordinal)
        Assertions.assertEquals(e.gender!!.isoCode, IsoGender.NOT_APPLICABLE.isoCode)

        // test history
        employeeDao.update(e)
        val historyEntriesAfter = employeeDao.getDisplayHistoryEntries(e)
        Assertions.assertEquals(historyEntriesBefore.size + 1, historyEntriesAfter.size)
        val genderHistoryEntry = historyEntriesAfter[0]
        Assertions.assertEquals("Gender", genderHistoryEntry.propertyName)
        Assertions.assertEquals("not applicable", genderHistoryEntry.newValue)
    }

    @Test
    fun testGenderConverter() {
        val genderConverter = GenderConverter()
        Assertions.assertEquals(genderConverter.convertToDatabaseColumn(null).toInt(), IsoGender.NOT_KNOWN.isoCode)
        Assertions.assertEquals(genderConverter.convertToDatabaseColumn(IsoGender.NOT_KNOWN).toInt(), IsoGender.NOT_KNOWN.isoCode)
        Assertions.assertEquals(genderConverter.convertToDatabaseColumn(IsoGender.MALE).toInt(), IsoGender.MALE.isoCode)
        Assertions.assertEquals(genderConverter.convertToDatabaseColumn(IsoGender.FEMALE).toInt(), IsoGender.FEMALE.isoCode)
        Assertions.assertEquals(genderConverter.convertToDatabaseColumn(IsoGender.NOT_APPLICABLE).toInt(), IsoGender.NOT_APPLICABLE.isoCode)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(null), IsoGender.NOT_KNOWN)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(Int.MAX_VALUE), IsoGender.NOT_KNOWN)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(Int.MIN_VALUE), IsoGender.NOT_KNOWN)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(0), IsoGender.NOT_KNOWN)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(1), IsoGender.MALE)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(2), IsoGender.FEMALE)
        Assertions.assertEquals(genderConverter.convertToEntityAttribute(9), IsoGender.NOT_APPLICABLE)
    }

    @Test
    fun testBanking() {
        baseLog.info("Count employees: " + employeeList.size)
        Assertions.assertTrue(employeeList.isNotEmpty())
        val e = employeeList[0]
        baseLog.info("Employee: $e")
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
        assertHistoryEntry(historyEntriesAfter[0], "Account holder", accountHolder)
        assertHistoryEntry(historyEntriesAfter[1], "BIC", bic)
        assertHistoryEntry(historyEntriesAfter[2], "IBAN", iban)
    }

    @Test
    fun testAddress() {
        Assertions.assertTrue(employeeList.isNotEmpty())
        val e = employeeList[0]
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
        assertHistoryEntry(historyEntriesAfter[0], "State", state)
        assertHistoryEntry(historyEntriesAfter[1], "Country", country)
        assertHistoryEntry(historyEntriesAfter[2], "City", city)
        assertHistoryEntry(historyEntriesAfter[3], "Zip code", zipCode)
        assertHistoryEntry(historyEntriesAfter[4], "Street", street)
    }

    @Test
    fun testStaffNumber() {
        Assertions.assertTrue(employeeList.isNotEmpty())
        val e = employeeList[0]
        val historyEntriesBefore = employeeDao.getDisplayHistoryEntries(e)
        val staffNumber = "123abc456def"
        e.staffNumber = staffNumber
        Assertions.assertEquals(e.staffNumber, staffNumber)
        employeeDao.update(e)

        // test history
        val historyEntriesAfter = employeeDao.getDisplayHistoryEntries(e)
        Assertions.assertEquals(historyEntriesBefore.size + 1, historyEntriesAfter.size)
        assertHistoryEntry(historyEntriesAfter[0], "Staff number", staffNumber)
    }

    private fun assertHistoryEntry(historyEntry: DisplayHistoryEntry, propertyName: String, newValue: String) {
        Assertions.assertEquals(historyEntry.propertyName, propertyName)
        Assertions.assertEquals(historyEntry.newValue, newValue)
    }

    companion object {
        /**
         * @param email: Optional mail address of the user.
         */
        fun createEmployee(employeeService: EmployeeService, employeeDao: EmployeeDao, test: AbstractTestBase, name: String,
                           hrAccess: Boolean = false,
                           groupDao: GroupDao? = null,
                           email: String? = null): EmployeeDO {
            val loggedInUser = ThreadLocalUserContext.user
            test.logon(TEST_ADMIN_USER)
            val user = PFUserDO()
            val useName = "${test.javaClass.simpleName}.$name"
            user.firstname = "$name.firstname"
            user.lastname = "$name.lastname"
            user.username = "$useName.username"
            user.email = email
            if (hrAccess) {
                user.addRight(UserRightDO(UserRightId.HR_VACATION, UserRightValue.READWRITE))
            }
            test.initTestDB.addUser(user)
            if (hrAccess) {
                val group = test.getGroup(ProjectForgeGroup.HR_GROUP.toString())
                group.assignedUsers!!.add(user)
                groupDao!!.update(group)
            }
            val employee = EmployeeDO()
            employee.user = user
            employeeService.addNewAnnualLeaveDays(employee, LocalDate.now().minusYears(2), BigDecimal(30))
            employeeDao.internalSave(employee)
            test.logon(loggedInUser)
            return employee
        }
    }
}
