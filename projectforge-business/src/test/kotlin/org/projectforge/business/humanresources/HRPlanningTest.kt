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

package org.projectforge.business.humanresources

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.humanresources.HRPlanningDO.Companion.getFirstDayOfWeek
import org.projectforge.business.user.*
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.UserRightService
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.framework.time.PFDateTime.Companion.withDate
import org.projectforge.framework.time.PFDay
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.*

class HRPlanningTest : AbstractTestBase() {
    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var hrPlanningDao: HRPlanningDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var userRightDao: UserRightDao

    @Autowired
    var userRights: UserRightService? = null

    override fun beforeAll() {
        persistenceService.runInTransaction { context ->
            logon(TEST_FINANCE_USER)
            val kunde = KundeDO()
            kunde.name = "ACME ltd."
            kunde.id = 59L
            kundeDao.save(kunde, context)
            projekt1 = initTestDB.addProjekt(kunde, 0, "Web portal", context)
            projekt2 = initTestDB.addProjekt(kunde, 1, "Order management", context)
        }
    }

    @Test
    fun testUserRights() {
        lateinit var user1: PFUserDO
        lateinit var planning: HRPlanningDO
        persistenceService.runInTransaction { context ->
            user1 = initTestDB.addUser("HRPlanningTestUser1", context)
            val right = userRights!!.getRight(UserRightId.PM_HR_PLANNING) as HRPlanningRight
            Assertions.assertFalse(right.isAvailable(user1, userGroupCache.getUserGroupDOs(user1)))
            planning = HRPlanningDO()
            planning.user = getUser(TEST_USER)
            logon(user1)
            Assertions.assertFalse(hrPlanningDao.hasLoggedInUserAccess(planning, null, OperationType.SELECT, false))
            try {
                hrPlanningDao.hasLoggedInUserAccess(planning, null, OperationType.SELECT, true)
                Assertions.fail<Any>("AccessException excepted.")
            } catch (ex: AccessException) {
                // OK
            }
            logon(TEST_ADMIN_USER)
            val group = initTestDB.getGroup(ORGA_GROUP)
            group!!.assignedUsers!!.add(user1)
            groupDao.update(group, context)
            Assertions.assertTrue(right.isAvailable(user1, userGroupCache.getUserGroupDOs(user1)))
            logon(user1)
            Assertions.assertFalse(hrPlanningDao.hasLoggedInUserAccess(planning, null, OperationType.SELECT, false))
            Assertions.assertTrue(accessChecker.hasLoggedInUserSelectAccess(UserRightId.PM_HR_PLANNING, false))
            Assertions.assertFalse(
                accessChecker.hasLoggedInUserSelectAccess(
                    UserRightId.PM_HR_PLANNING,
                    planning,
                    false
                )
            )
            Assertions.assertFalse(
                accessChecker.hasLoggedInUserHistoryAccess(
                    UserRightId.PM_HR_PLANNING,
                    planning,
                    false
                )
            )
            Assertions.assertFalse(
                accessChecker.hasLoggedInUserInsertAccess(
                    UserRightId.PM_HR_PLANNING,
                    planning,
                    false
                )
            )
            logon(TEST_ADMIN_USER)
            user1.addRight(UserRightDO(user1, UserRightId.PM_HR_PLANNING, UserRightValue.READONLY))
            userRightDao.save(ArrayList(user1.rights), context)
            userService.updateInTrans(user1)
            logon(user1)
            Assertions.assertTrue(hrPlanningDao.hasLoggedInUserAccess(planning, null, OperationType.SELECT, false))
            Assertions.assertTrue(
                accessChecker.hasLoggedInUserSelectAccess(
                    UserRightId.PM_HR_PLANNING,
                    planning,
                    false
                )
            )
            Assertions.assertTrue(
                accessChecker.hasLoggedInUserHistoryAccess(
                    UserRightId.PM_HR_PLANNING,
                    planning,
                    false
                )
            )
            Assertions.assertFalse(
                accessChecker.hasLoggedInUserInsertAccess(
                    UserRightId.PM_HR_PLANNING,
                    planning,
                    false
                )
            )
        }
        persistenceService.runInTransaction { context ->
            logon(TEST_ADMIN_USER)
            // split
            user1 = userService.getById(user1.id)
            val userRight = user1.getRight(UserRightId.PM_HR_PLANNING)
            userRight!!.value = UserRightValue.READWRITE
            userRightDao.update(userRight, context)
            logon(user1)
            Assertions.assertTrue(hrPlanningDao.hasLoggedInUserAccess(planning, null, OperationType.SELECT, false))
            Assertions.assertTrue(
                accessChecker.hasLoggedInUserSelectAccess(
                    UserRightId.PM_HR_PLANNING,
                    planning,
                    false
                )
            )
            Assertions.assertTrue(
                accessChecker.hasLoggedInUserHistoryAccess(
                    UserRightId.PM_HR_PLANNING,
                    planning,
                    false
                )
            )
            Assertions.assertTrue(
                accessChecker.hasLoggedInUserInsertAccess(
                    UserRightId.PM_HR_PLANNING,
                    planning,
                    false
                )
            )
        }
    }

    @Test
    fun testFirstDayOfWeek() {
        val date = LocalDate.of(2010, Month.JANUARY, 9)
        Assertions.assertEquals(
            "2010-01-04",
            getFirstDayOfWeek(date).toString()
        )
    }

    @Test
    fun testBeginOfWeek() {
        persistenceService.runInTransaction { context ->
            logon(TEST_FINANCE_USER)
            var planning = HRPlanningDO()
            val date = LocalDate.of(2010, Month.JANUARY, 9)
            planning.setFirstDayOfWeek(date)
            Assertions.assertEquals("2010-01-04", planning.week.toString())
            planning.week = date
            planning.user = getUser(TEST_USER)
            Assertions.assertEquals("2010-01-09", planning.week.toString())
            val id: Serializable = hrPlanningDao.save(planning, context)
            planning = hrPlanningDao.getById(id, context)!!
            Assertions.assertEquals("2010-01-04", planning.week.toString())
        }
    }

    @Test
    fun overwriteDeletedEntries() {
        persistenceService.runInTransaction { context: PfPersistenceContext? ->
            logon(TEST_FINANCE_USER)
            // Create planning:
            var planning = HRPlanningDO()
            planning.user = getUser(TEST_USER)
            planning.week = LocalDate.of(2010, Month.JANUARY, 11)
            assertLocalDate(planning.week!!, 2010, Month.JANUARY, 11)
            var entry = HRPlanningEntryDO()
            setHours(entry, 1, 2, 3, 4, 5, 6)
            entry.projekt = projekt1
            planning.addEntry(entry)
            entry = HRPlanningEntryDO()
            setHours(entry, 2, 4, 6, 8, 10, 12)
            entry.status = HRPlanningEntryStatus.OTHER
            planning.addEntry(entry)
            entry = HRPlanningEntryDO()
            setHours(entry, 6, 5, 4, 3, 2, 1)
            entry.projekt = projekt2
            planning.addEntry(entry)
            val id: Serializable = hrPlanningDao.save(planning, context!!)
            // Check saved planning
            planning = hrPlanningDao.getById(id, context)!!
            val day = PFDay(planning.week!!)
            assertLocalDate(day.localDate, 2010, Month.JANUARY, 11)
            Assertions.assertEquals(3, planning.entries!!.size)
            assertHours(planning.getProjectEntry(projekt1!!)!!, 1, 2, 3, 4, 5, 6)
            assertHours(planning.getProjectEntry(projekt2!!)!!, 6, 5, 4, 3, 2, 1)
            assertHours(planning.getStatusEntry(HRPlanningEntryStatus.OTHER)!!, 2, 4, 6, 8, 10, 12)
            // Delete entry
            planning.getProjectEntry(projekt1!!)!!.deleted = true
            hrPlanningDao.update(planning, context)
            // Check deleted entry and re-adding it
            planning = hrPlanningDao.getById(id, context)!!
            Assertions.assertTrue(planning.getProjectEntry(projekt1!!)!!.deleted)
            entry = HRPlanningEntryDO()
            setHours(entry, 7, 9, 11, 1, 3, 5)
            entry.projekt = projekt1
            planning.addEntry(entry)
            hrPlanningDao.update(planning, context)
            null
        }
    }

    private fun setHours(
        entry: HRPlanningEntryDO, monday: Int, tuesday: Int, wednesday: Int,
        thursday: Int,
        friday: Int, weekend: Int
    ) {
        entry.mondayHours = BigDecimal(monday)
        entry.tuesdayHours = BigDecimal(tuesday)
        entry.wednesdayHours = BigDecimal(wednesday)
        entry.thursdayHours = BigDecimal(thursday)
        entry.fridayHours = BigDecimal(friday)
        entry.weekendHours = BigDecimal(weekend)
    }

    private fun assertHours(
        entry: HRPlanningEntryDO, monday: Int, tuesday: Int, wednesday: Int,
        thursday: Int,
        friday: Int, weekend: Int
    ) {
        assertBigDecimal(monday, entry.mondayHours!!)
        assertBigDecimal(tuesday, entry.tuesdayHours!!)
        assertBigDecimal(wednesday, entry.wednesdayHours!!)
        assertBigDecimal(thursday, entry.thursdayHours!!)
        assertBigDecimal(friday, entry.fridayHours!!)
        assertBigDecimal(weekend, entry.weekendHours!!)
    }

    private fun createDate(
        year: Int, month: Month, day: Int, hour: Int, minute: Int,
        second: Int, millisecond: Int
    ): LocalDate {
        return withDate(year, month, day, hour, minute, second, millisecond, ZoneId.of("UTC"), Locale.GERMAN).localDate
    }

    companion object {
        private var projekt1: ProjektDO? = null
        private var projekt2: ProjektDO? = null
    }
}
