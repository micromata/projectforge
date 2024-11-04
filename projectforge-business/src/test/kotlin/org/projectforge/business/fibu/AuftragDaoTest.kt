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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserRightDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.framework.time.PFDay.Companion.now
import org.projectforge.framework.time.PFDay.Companion.withDate
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class AuftragDaoTest : AbstractTestBase() {
    override fun beforeAll() {
        recreateDataBase() // Remove any orders created by other tests before.
        dbNumber = auftragDao.nextNumber
    }

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var userRightDao: UserRightDao

    private val random = Random()

    @Test
    fun getNextNumber() {
        logon(TEST_FINANCE_USER)
        var auftrag = AuftragDO()
        auftrag.nummer = auftragDao.getNextNumber(auftrag)
        auftrag.addPosition(AuftragsPositionDO())
        auftragDao.insert(auftrag)
        Assertions.assertEquals(dbNumber++, auftrag.nummer)
        auftrag = AuftragDO()
        auftrag.nummer = auftragDao.getNextNumber(auftrag)
        auftrag.addPosition(AuftragsPositionDO())
        auftragDao.insert(auftrag)
        Assertions.assertEquals(dbNumber++, auftrag.nummer)
    }

    @Test
    fun checkAccess() {
        logon(TEST_FINANCE_USER)
        lateinit var id1: Serializable
        lateinit var id2: Serializable
        lateinit var id3: Serializable
        persistenceService.runInTransaction { _ ->
            var auftrag1 = AuftragDO()
            auftrag1.nummer = auftragDao.getNextNumber(auftrag1)
            auftragDao.setContactPerson(auftrag1, getUserId(TEST_FINANCE_USER))
            try {
                suppressErrorLogs {
                    id1 = auftragDao.insert(auftrag1)
                }
                Assertions.fail { "UserException expected: Order should have positions." }
            } catch (ex: UserException) {
                Assertions.assertEquals("fibu.auftrag.error.auftragHatKeinePositionen", ex.i18nKey)
            }
            auftrag1.addPosition(AuftragsPositionDO())
            id1 = auftragDao.insert(auftrag1)
            dbNumber++ // Needed for getNextNumber test;
            auftrag1 = auftragDao.find(id1, attached = true)!! // Attached is important, otherwise deadlock.

            val auftrag2 = AuftragDO()
            auftrag2.nummer = auftragDao.getNextNumber(auftrag2)
            auftragDao.setContactPerson(auftrag2, getUserId(TEST_PROJECT_MANAGER_USER))
            auftrag2.addPosition(AuftragsPositionDO())
            id2 = auftragDao.insert(auftrag2)
            dbNumber++ // Needed for getNextNumber test;

            val auftrag3 = AuftragDO()
            auftrag3.nummer = auftragDao.getNextNumber(auftrag3)
            auftragDao.setContactPerson(auftrag3, getUserId(TEST_PROJECT_MANAGER_USER))
            val dateTime = now().minusYears(6) // 6 years old.
            auftrag3.angebotsDatum = dateTime.localDate
            auftrag3.auftragsStatus = AuftragsStatus.ABGESCHLOSSEN
            val position = AuftragsPositionDO()
            position.vollstaendigFakturiert = true
            position.status = AuftragsPositionsStatus.ABGESCHLOSSEN
            auftrag3.addPosition(position)
            id3 = auftragDao.insert(auftrag3)
            dbNumber++ // Needed for getNextNumber test;
            logon(TEST_PROJECT_MANAGER_USER)
            try {
                suppressErrorLogs {
                    auftragDao.find(id1, attached = true) // Attached is important, otherwise deadlock.
                }
                Assertions.fail { "AccessException expected: Projectmanager should not have access to foreign orders." }
            } catch (ex: AccessException) {
                // OK
            }
        }
        auftragDao.find(id2, attached = true) // Attached is important, otherwise deadlock.
        try {
            suppressErrorLogs {
                auftragDao.find(id3, attached = true) // Attached is important, otherwise deadlock.
            }
            Assertions.fail { "AccessException expected: Projectmanager should not have access to older orders than ${AuftragRight.MAX_DAYS_OF_VISIBILITY_4_PROJECT_MANGER} days." }
        } catch (ex: AccessException) {
            // OK
        }
        persistenceService.runInTransaction {
            val useId = id1
            logon(TEST_CONTROLLING_USER)
            val order = auftragDao.find(useId, attached = true)!! // Attached is important, otherwise deadlock.
            checkNoWriteAccess(order, "Controller")

            logon(TEST_USER)
            checkNoAccess(useId, order, "Other")

            logon(TEST_ADMIN_USER)
            checkNoAccess(useId, order, "Admin ")
        }
    }

    @Test
    fun checkAccess2() {
        lateinit var auftrag1: AuftragDO
        lateinit var auftrag2: AuftragDO
        lateinit var id: Serializable
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val group1 = initTestDB.addGroup("AuftragDaoTest.ProjectManagers1", TEST_PROJECT_ASSISTANT_USER)
            val group2 = initTestDB.addGroup("AuftragDaoTest.ProjectManagers2", TEST_PROJECT_MANAGER_USER)
            var projekt1 = ProjektDO()
            projekt1.name = "ACME - Webportal 1"
            projekt1.projektManagerGroup = group1
            id = projektDao.insert(projekt1)
            projekt1 = projektDao.find(id, attached = true)!! // Attached is important, otherwise deadlock.
            auftrag1 = AuftragDO()
            auftrag1.nummer = auftragDao.getNextNumber(auftrag1)
            auftrag1.projekt = projekt1
            auftrag1.addPosition(AuftragsPositionDO())
            id = auftragDao.insert(auftrag1)
            dbNumber++ // Needed for getNextNumber test;
            auftrag1 = auftragDao.find(id, attached = true)!! // Attached is important, otherwise deadlock.

            var projekt2 = ProjektDO()
            projekt2.name = "ACME - Webportal 2"
            projekt2.projektManagerGroup = group2
            id = projektDao.insert(projekt2)
            projekt2 = projektDao.find(id, attached = true)!! // Attached is important, otherwise deadlock.
            auftrag2 = AuftragDO()
            auftrag2.nummer = auftragDao.getNextNumber(auftrag2)
            auftrag2.projekt = projekt2
            auftrag2.addPosition(AuftragsPositionDO())
            id = auftragDao.insert(auftrag2)
            dbNumber++ // Needed for getNextNumber test;
            auftrag2 = auftragDao.find(id, attached = true)!! // Attached is important, otherwise deadlock.

            logon(TEST_CONTROLLING_USER)
            checkNoWriteAccess(auftrag1, "Controlling")

            logon(TEST_USER)
            checkNoAccess(id, auftrag1, "Other")
        }
        persistenceService.runInTransaction { _ ->
            logon(TEST_PROJECT_MANAGER_USER)
            projektDao.select(ProjektFilter())
            checkNoAccess(auftrag1.id, "Project manager")
            checkNoWriteAccess(auftrag1, "Project manager")
            checkHasUpdateAccess(auftrag2.id)

            logon(TEST_PROJECT_ASSISTANT_USER)
            projektDao.select(ProjektFilter())
            checkHasUpdateAccess(auftrag1.id)
            checkNoAccess(auftrag2.id, "Project assistant")
            checkNoWriteAccess(auftrag2, "Project assistant")

            logon(TEST_ADMIN_USER)
            checkNoAccess(id, auftrag1, "Admin ")
        }
    }

    @Test
    fun checkPartlyReadwriteAccess() {
        var logonUser: PFUserDO? = null
        var auftragId: Long? = null
        var group: GroupDO? = null
        lateinit var id: Serializable
        lateinit var user: PFUserDO
        persistenceService.runInTransaction { _ ->
            logon(TEST_ADMIN_USER)
            user = initTestDB.addUser("AuftragDaoCheckPartlyReadWriteAccess")
            val financeGroup = getGroup(FINANCE_GROUP)
            financeGroup.addUser(user)
            groupDao.update(financeGroup)
            val projectAssistants = getGroup(PROJECT_ASSISTANT)
            projectAssistants.addUser(user)
            groupDao.update(projectAssistants)

            group = initTestDB.addGroup("AuftragDaoTest.checkPartlyReadwriteAccess")
            logon(TEST_FINANCE_USER)
            var projekt = ProjektDO()
            projekt.name = "ACME - Webportal checkPartlyReadwriteAccess"
            projekt.projektManagerGroup = group
            id = projektDao.insert(projekt)
            projekt = projektDao.find(id, attached = true)!! // Attached is important, otherwise deadlock.

            var auftrag = AuftragDO()
            auftrag.nummer = auftragDao.getNextNumber(auftrag)
            auftrag.projekt = projekt
            auftrag.addPosition(AuftragsPositionDO())
            id = auftragDao.insert(auftrag)
            auftragId = id
            dbNumber++ // Needed for getNextNumber test;
        }
        persistenceService.runInTransaction {
            var auftrag = auftragDao.find(id, attached = true)!! // Attached is important, otherwise deadlock.

            logon(user)
            try {
                suppressErrorLogs {
                    auftrag = auftragDao.find(id, attached = true)!! // Attached is important, otherwise deadlock.
                }
                Assertions.fail { "Access exception expected." }
            } catch (ex: AccessException) {
                Assertions.assertEquals("access.exception.userHasNotRight", ex.i18nKey)
            }
            logon(TEST_ADMIN_USER)
            user.addRight(UserRightDO(UserRightId.PM_ORDER_BOOK, UserRightValue.PARTLYREADWRITE)) //
            userRightDao.insert(user.rights!!.toList())
            userService.update(user)
            user = userService.find(user.id, false)
            logonUser = user
            logon(user)
            try {
                suppressErrorLogs {
                    auftrag = auftragDao.find(id, attached = true)!! // Attached is important, otherwise deadlock.
                }
                Assertions.fail { "Access exception expected." }
            } catch (ex: AccessException) {
                Assertions.assertEquals("access.exception.userHasNotRight", ex.i18nKey)
            }
        }
        persistenceService.runInTransaction { _ ->
            val user = logonUser!!
            logon(TEST_ADMIN_USER)
            val right = user.getRight(UserRightId.PM_ORDER_BOOK)!!
            right.value = UserRightValue.READWRITE // Full access
            userRightDao.update(right)
            logon(user)
            auftragDao.find(auftragId, attached = true) // Attached is important, otherwise deadlock.
            logon(TEST_ADMIN_USER)
            right.value = UserRightValue.PARTLYREADWRITE
            userRightDao.update(right)
            group!!.assignedUsers!!.add(user)
            groupDao.update(group!!) // User is now in project manager group.
            logon(user)
            auftragDao.find(auftragId, attached = true)!! // Attached is important, otherwise deadlock.
        }
    }

    private fun checkHasUpdateAccess(auftragsId: Serializable?) {
        var auftrag = auftragDao.find(auftragsId, attached = true)!! // Attached is important, otherwise deadlock.
        val value = random.nextLong().toString()
        auftrag.bemerkung = value
        auftragDao.update(auftrag)
        auftrag = auftragDao.find(auftragsId, attached = true)!! // Attached is important, otherwise deadlock.
        Assertions.assertEquals(value, auftrag.bemerkung)
    }

    private fun checkNoAccess(who: String) {
        try {
            val filter = AuftragFilter()
            suppressErrorLogs {
                auftragDao.select(filter)
            }
            Assertions.fail { "AccessException expected: $who users should not have select list access to orders." }
        } catch (ex: AccessException) {
            // OK
        }
    }

    private fun checkNoAccess(auftragsId: Serializable?, who: String) {
        try {
            suppressErrorLogs {
                auftragDao.find(auftragsId, attached = true) // Attached is important, otherwise deadlock.
            }
            Assertions.fail { "AccessException expected: $who users should not have select access to orders." }
        } catch (ex: AccessException) {
            // OK
        }
    }

    private fun checkNoAccess(id: Serializable, auftrag: AuftragDO, who: String) {
        checkNoAccess(who)
        checkNoAccess(id, who)
        checkNoWriteAccess(auftrag, who)
    }

    private fun checkNoWriteAccess(auftrag: AuftragDO, who: String) {
        try {
            val auf = AuftragDO()
            val number = auftragDao.getNextNumber(auf)
            auf.nummer = number
            suppressErrorLogs {
                auftragDao.insert(auf)
            }
            Assertions.fail { "AccessException expected: $who users should not have save access to orders." }
        } catch (ex: AccessException) {
            // OK
        }
        try {
            auftrag.bemerkung = who
            suppressErrorLogs {
                auftragDao.update(auftrag)
            }
            Assertions.fail { "AccessException expected: $who users should not have update access to orders." }
        } catch (ex: AccessException) {
            // OK
        }
    }

    @Test
    fun checkVollstaendigFakturiert() {
        lateinit var auftrag1: AuftragDO
        lateinit var id1: Serializable
        lateinit var position: AuftragsPositionDO
        logon(TEST_FINANCE_USER)
        auftrag1 = AuftragDO()
        auftrag1.nummer = auftragDao.getNextNumber(auftrag1)
        auftragDao.setContactPerson(auftrag1, getUserId(TEST_PROJECT_MANAGER_USER))
        auftrag1.addPosition(AuftragsPositionDO())
        id1 = auftragDao.insert(auftrag1)
        persistenceService.runInTransaction { _ ->
            dbNumber++ // Needed for getNextNumber test;
            auftrag1 = auftragDao.find(id1)!! // Attached is important, otherwise deadlock.

            position = auftrag1.positionenIncludingDeleted!![0]
            position.vollstaendigFakturiert = true
            try {
                suppressErrorLogs {
                    auftragDao.update(auftrag1)
                }
                Assertions.fail { "UserException expected: Only orders with state ABGESCHLOSSEN should be set as fully invoiced." }
            } catch (ex: UserException) {
                Assertions.assertEquals(
                    "fibu.auftrag.error.nurAbgeschlosseneAuftragsPositionenKoennenVollstaendigFakturiertSein",
                    ex.i18nKey
                )
            }
        }
        persistenceService.runInTransaction { _ ->
            auftrag1 = auftragDao.find(id1, attached = true)!! // Attached is important, otherwise deadlock.
            auftrag1.auftragsStatus = AuftragsStatus.ABGESCHLOSSEN
            auftragDao.update(auftrag1)
        }

        persistenceService.runInTransaction { _ ->
            auftrag1 = auftragDao.find(id1)!! // Attached is important, otherwise deadlock.
            logon(TEST_PROJECT_MANAGER_USER)
            position = auftrag1.positionenIncludingDeleted!![0]
            position.status = AuftragsPositionsStatus.ABGESCHLOSSEN
            position.vollstaendigFakturiert = true
            try {
                suppressErrorLogs {
                    auftragDao.update(auftrag1)
                }
                Assertions.fail { "AccessException expected: Projectmanager should not able to set order as fully invoiced." }
            } catch (ex: AccessException) {
                // OK
                Assertions.assertEquals("fibu.auftrag.error.vollstaendigFakturiertProtection", ex.i18nKey)
            }
        }
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            position = auftrag1.positionenIncludingDeleted!![0]
            position.status = AuftragsPositionsStatus.ABGESCHLOSSEN
            position.vollstaendigFakturiert = true
            auftragDao.update(auftrag1)
        }
    }

    @Test
    fun checkEmptyAuftragsPositionen() {
        logon(TEST_FINANCE_USER)
        lateinit var id: Serializable
        persistenceService.runInTransaction { _ ->
            var auftrag = AuftragDO()
            auftrag.nummer = auftragDao.getNextNumber(auftrag)
            auftrag.addPosition(AuftragsPositionDO())
            auftrag.addPosition(AuftragsPositionDO())
            auftrag.addPosition(AuftragsPositionDO())
            auftrag.addPosition(AuftragsPositionDO())
            id = auftragDao.insert(auftrag)
        }
        persistenceService.runInTransaction { _ ->
            dbNumber++ // Needed for getNextNumber test;
            var auftrag = auftragDao.find(id, attached = true)!!
            Assertions.assertEquals(1, auftrag.positionenIncludingDeleted!!.size)
            auftrag = AuftragDO()
            auftrag.nummer = auftragDao.getNextNumber(auftrag)
            auftrag.addPosition(AuftragsPositionDO())
            auftrag.addPosition(AuftragsPositionDO())
            val position = AuftragsPositionDO()
            position.titel = "Hurzel"
            auftrag.addPosition(position)
            auftrag.addPosition(AuftragsPositionDO())
            id = auftragDao.insert(auftrag)
            dbNumber++ // Needed for getNextNumber test;
            auftrag = auftragDao.find(id, attached = true)!!
            Assertions.assertEquals(3, auftrag.positionenIncludingDeleted!!.size)
            auftrag.positionenIncludingDeleted!![2].titel = null
            auftragDao.update(auftrag)
            auftrag = auftragDao.find(id, attached = true)!! // Attached is important, otherwise deadlock.
            Assertions.assertEquals(3, auftrag.positionenIncludingDeleted!!.size)
        }
    }

    @Test
    fun validateDatesInPaymentScheduleWithinPeriodOfPerformanceOfPosition() {
        persistenceService.runInTransaction { _ ->
            val auftrag = AuftragDO()
            val auftragsPositions = auftrag.ensureAndGetPositionen()
            val paymentSchedules = auftrag.ensureAndGetPaymentSchedules()

            auftrag.periodOfPerformanceBegin = LocalDate.of(2017, 5, 1)
            auftrag.periodOfPerformanceEnd = LocalDate.of(2017, 6, 30)

            val pos1 = AuftragsPositionDO()
            pos1.number = 1.toShort()

            val pos2 = AuftragsPositionDO()
            pos2.number = 2.toShort()
            pos2.periodOfPerformanceType = PeriodOfPerformanceType.OWN
            pos2.periodOfPerformanceBegin = LocalDate.of(2017, 5, 24)
            pos2.periodOfPerformanceEnd = LocalDate.of(2017, 5, 25)

            auftragsPositions.add(pos1)
            auftragsPositions.add(pos2)

            var paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 1.toShort()
            paymentSchedule.scheduleDate = LocalDate.of(2017, 5, 1)

            paymentSchedules.add(paymentSchedule)

            paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 1.toShort()
            paymentSchedule.scheduleDate = LocalDate.of(2017, 5, 20)

            paymentSchedules.add(paymentSchedule)

            paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 1.toShort()
            paymentSchedule.scheduleDate = LocalDate.of(2017, 6, 30)

            paymentSchedules.add(paymentSchedule)

            paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 2.toShort()
            paymentSchedule.scheduleDate = LocalDate.of(2017, 5, 24)

            paymentSchedules.add(paymentSchedule)

            paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 2.toShort()
            paymentSchedule.scheduleDate = LocalDate.of(2017, 5, 25)

            paymentSchedules.add(paymentSchedule)

            var exceptionThrown = false
            try {
                auftragDao.validateDatesInPaymentScheduleWithinPeriodOfPerformanceOfPosition(auftrag)
            } catch (e: UserException) {
                exceptionThrown = true
            }
            Assertions.assertFalse(exceptionThrown)

            paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 1.toShort()
            paymentSchedule.scheduleDate = LocalDate.of(2017, 4, 30)

            paymentSchedules.add(paymentSchedule)

            paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 2.toShort()
            // Later than end of performance plus 3 months:
            paymentSchedule.scheduleDate = LocalDate.of(2017, 8, 26)

            paymentSchedules.add(paymentSchedule)

            try {
                auftragDao.validateDatesInPaymentScheduleWithinPeriodOfPerformanceOfPosition(auftrag)
            } catch (e: UserException) {
                exceptionThrown = true
                Assertions.assertEquals(e.params!!.size, 1)
                Assertions.assertEquals(e.params!![0], "1, 2")
            }
            Assertions.assertTrue(exceptionThrown)
        }
    }

    @Test
    fun validateAmountsInPaymentScheduleNotGreaterThanNetSumOfPosition() {
        persistenceService.runInTransaction { _ ->
            val auftrag = AuftragDO()
            val auftragsPositions = auftrag.ensureAndGetPositionen()
            val paymentSchedules = auftrag.ensureAndGetPaymentSchedules()

            val pos1 = AuftragsPositionDO()
            pos1.number = 1.toShort()
            pos1.nettoSumme = BigDecimal(2000)

            val pos2 = AuftragsPositionDO()
            pos2.number = 2.toShort()
            pos2.nettoSumme = BigDecimal(5000)

            auftragsPositions.add(pos1)
            auftragsPositions.add(pos2)

            var paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 1.toShort()
            paymentSchedule.amount = BigDecimal(1000)

            paymentSchedules.add(paymentSchedule)

            paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 1.toShort()
            paymentSchedule.amount = null // should not cause a NPE

            paymentSchedules.add(paymentSchedule)

            paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 1.toShort()
            paymentSchedule.amount = BigDecimal(1000)

            paymentSchedules.add(paymentSchedule)

            paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 2.toShort()
            paymentSchedule.amount = BigDecimal(2000)

            paymentSchedules.add(paymentSchedule)

            paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 2.toShort()
            paymentSchedule.amount = BigDecimal(2999)

            paymentSchedules.add(paymentSchedule)

            var exceptionThrown = false
            try {
                auftragDao.validateAmountsInPaymentScheduleNotGreaterThanNetSumOfPosition(auftrag)
            } catch (e: UserException) {
                exceptionThrown = true
            }
            Assertions.assertFalse(exceptionThrown)

            // amounts of position 1 (2001) will now be greater than netto summe (2000) -> should throw exception
            paymentSchedule = PaymentScheduleDO()
            paymentSchedule.positionNumber = 1.toShort()
            paymentSchedule.amount = BigDecimal(1)

            paymentSchedules.add(paymentSchedule)


            try {
                auftragDao.validateAmountsInPaymentScheduleNotGreaterThanNetSumOfPosition(auftrag)
            } catch (e: UserException) {
                exceptionThrown = true
            }
            Assertions.assertTrue(exceptionThrown)
        }
    }

    @Test
    fun testPeriodOfPerformanceFilter() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            auftragDao.insert(createAuftragWithPeriodOfPerformance(2017, 4, 1, 2017, 4, 30))
            auftragDao.insert(createAuftragWithPeriodOfPerformance(2017, 4, 3, 2017, 4, 5))
            auftragDao.insert(createAuftragWithPeriodOfPerformance(2017, 3, 31, 2017, 5, 1))
            auftragDao.insert(createAuftragWithPeriodOfPerformance(2017, 3, 31, 2017, 4, 5))
            auftragDao.insert(createAuftragWithPeriodOfPerformance(2017, 3, 31, 2017, 5, 1))
            auftragDao.insert(createAuftragWithPeriodOfPerformance(2010, 1, 1, 2020, 12, 31))

            val auftragFilter = AuftragFilter()

            setPeriodOfPerformanceStartDateAndEndDate(auftragFilter, 2017, 4, 1, 2017, 4, 30)
            Assertions.assertEquals(6, auftragDao.select(auftragFilter).size)

            setPeriodOfPerformanceStartDateAndEndDate(auftragFilter, 2017, 4, 1, 2017, 4, 1)
            Assertions.assertEquals(5, auftragDao.select(auftragFilter).size)

            auftragFilter.periodOfPerformanceStartDate = null
            Assertions.assertEquals(5, auftragDao.select(auftragFilter).size)

            setPeriodOfPerformanceStartDateAndEndDate(auftragFilter, 2017, 4, 6, 2017, 4, 6)
            Assertions.assertEquals(4, auftragDao.select(auftragFilter).size)

            auftragFilter.periodOfPerformanceStartDate = null
            Assertions.assertEquals(6, auftragDao.select(auftragFilter).size)

            setPeriodOfPerformanceStartDateAndEndDate(auftragFilter, 2016, 1, 1, 2016, 1, 1)
            Assertions.assertEquals(1, auftragDao.select(auftragFilter).size)

            auftragFilter.periodOfPerformanceEndDate = null
            Assertions.assertEquals(6, auftragDao.select(auftragFilter).size)
        }
    }

    private fun setPeriodOfPerformanceStartDateAndEndDate(
        auftragFilter: AuftragFilter, startYear: Int, startMonth: Int, startDay: Int,
        endYear: Int, endMonth: Int, endDay: Int
    ) {
        auftragFilter.periodOfPerformanceStartDate = withDate(startYear, startMonth, startDay).localDate
        auftragFilter.periodOfPerformanceEndDate = withDate(endYear, endMonth, endDay).localDate
    }

    private fun createAuftragWithPeriodOfPerformance(
        beginYear: Int, beginMonth: Int, beginDay: Int, endYear: Int, endMonth: Int,
        endDay: Int
    ): AuftragDO {
        val auftrag = AuftragDO()
        auftrag.nummer = auftragDao.getNextNumber(auftrag)
        dbNumber++
        auftrag.addPosition(AuftragsPositionDO())
        auftrag.periodOfPerformanceBegin = LocalDate.of(beginYear, beginMonth, beginDay)
        auftrag.periodOfPerformanceEnd = LocalDate.of(endYear, endMonth, endDay)
        return auftrag
    }

    companion object {
        private var dbNumber = 0
    }
}
