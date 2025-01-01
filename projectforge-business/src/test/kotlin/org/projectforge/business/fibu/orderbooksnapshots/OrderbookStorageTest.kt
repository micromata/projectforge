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

package org.projectforge.business.fibu.orderbooksnapshots

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.*
import org.projectforge.business.task.TaskDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.time.PFDateTimeUtils
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

class OrderbookStorageTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var orderbookStorageService: OrderbookSnapshotsService

    @Autowired
    private lateinit var projektDao: ProjektDao

    private lateinit var kunde: KundeDO
    private lateinit var project: ProjektDO
    private lateinit var task: TaskDO


    @Test
    fun `test serialization and deserialization of order book`() {
        recreateDataBase() // Remove any orders created by other tests before.
        task = initTestDB.addTask("OrderbookStorageTest", "root")
        kunde = KundeDO().also {
            it.name = "ACME"
            it.id = 984232
            kundeDao.insert(it, checkAccess = false)
        }
        project = ProjektDO().also {
            it.name = "Project"
            projektDao.insert(it, checkAccess = false)
        }
        createOrder("1", LocalDate.of(2024, Month.DECEMBER, 1))
        createOrder("2", LocalDate.of(2024, Month.DECEMBER, 1))
        createOrder("3", LocalDate.of(2024, Month.DECEMBER, 1))
        val order4 = createOrder("4")
        val order5 = createOrder("5")
        val order6 = createOrder("6")
        val stats = orderbookStorageService.storeOrderbookSnapshot(date = LocalDate.of(2024, Month.DECEMBER, 15))
        // Previous entry should be overwritten. No exception expected:
        orderbookStorageService.storeOrderbookSnapshot(date = stats.date)
        Assertions.assertEquals(6, stats.count)
        var list = orderbookStorageService.readSnapshot(stats.date)
        Assertions.assertEquals(6, list!!.size)
        list.forEach { order ->
            Assertions.assertEquals("30.00".toBigDecimal(), order.info.netSum)
            Assertions.assertEquals("30.00".toBigDecimal(), order.info.akquiseSum)
            Assertions.assertEquals("3.00".toBigDecimal(), order.info.personDays)
            Assertions.assertEquals(AuftragsStatus.POTENZIAL, order.info.status)
        }

        persistenceService.runInTransaction {
            order4.titel = "new title 4"
            auftragDao.update(order4, checkAccess = false)
            order5.titel = "new title 5"
            auftragDao.update(order5, checkAccess = false)
            order6.titel = "new title 6"
            auftragDao.update(order6, checkAccess = false)
        }

        // Test incremental storage:
        var incrementalStats =
            orderbookStorageService.storeOrderbookSnapshot(incrementalBasedOn = stats.date)
        Assertions.assertEquals(3, incrementalStats.count)

        // Restore the incremental storage:
        list = orderbookStorageService.readSnapshot(incrementalStats.date)
        Assertions.assertEquals(6, list!!.size)
        list.forEach { order ->
            Assertions.assertEquals("30.00".toBigDecimal(), order.info.netSum)
            Assertions.assertEquals("30.00".toBigDecimal(), order.info.akquiseSum)
            Assertions.assertEquals("3.00".toBigDecimal(), order.info.personDays)
            Assertions.assertEquals(AuftragsStatus.POTENZIAL, order.info.status)
        }
        Assertions.assertTrue(list.any { it.titel == "new title 4" }, "Modified title expected.")

        list = orderbookStorageService.readSnapshot(stats.date) // Restore the full backup (without last changes).
        Assertions.assertEquals(6, list!!.size)
        Assertions.assertTrue(list.none { it.titel == "new title 4" }, "Modified title expected.")

        // Test incremental storage with a date that is not found:
        incrementalStats =
            orderbookStorageService.createOrderbookSnapshot(incrementalBasedOn = LocalDate.of(2024, Month.NOVEMBER, 12))
        Assertions.assertEquals(6, incrementalStats.count, "No storage found, based on date 2024-11-12, full backup expected.")
    }

    private fun createOrder(id: String, dateOfLastUpdate: LocalDate? = null): AuftragDO {
        return AuftragDO().also { order ->
            order.nummer = auftragDao.nextNumber
            order.referenz = "reference $id"
            order.status = AuftragsStatus.GELEGT
            order.kunde = kunde
            order.kundeText = "customer text $id"
            order.projekt = project
            order.titel = "title $id"
            order.periodOfPerformanceBegin = LocalDate.of(2024, Month.DECEMBER, 1)
            order.periodOfPerformanceEnd = LocalDate.of(2024, Month.DECEMBER, 31)
            order.probabilityOfOccurrence = 80

            val pos1 = createOrderPosition("$id.1").also { pos ->
                order.addPosition(pos)
            }
            val pos2 = createOrderPosition("$id.2").also { pos ->
                order.addPosition(pos)
            }
            order.addPosition(createOrderPosition("$id.3"))
            order.addPaymentSchedule(createPaymentSchedule(pos1, "$id.1"))
            order.addPaymentSchedule(createPaymentSchedule(pos2, "$id.2"))

            val id = auftragDao.insert(order, checkAccess = false)
            if (dateOfLastUpdate != null) {
                val order = auftragDao.find(id, checkAccess = false)!!
                persistenceService.runInTransaction { context ->
                    order.created = PFDateTimeUtils.getBeginOfDateAsUtildate(dateOfLastUpdate)
                    order.lastUpdate = order.created
                    context.em.merge(order)
                }
            }
        }
    }

    private fun createOrderPosition(id: String): AuftragsPositionDO {
        return AuftragsPositionDO().also {
            it.task = task
            it.art = AuftragsPositionsArt.NEUENTWICKLUNG
            it.paymentType = AuftragsPositionsPaymentType.FESTPREISPAKET
            it.status = AuftragsStatus.GELEGT
            it.titel = "title $id"
            it.nettoSumme = BigDecimal.TEN
            it.personDays = BigDecimal.ONE
            it.vollstaendigFakturiert = false
            it.periodOfPerformanceType = PeriodOfPerformanceType.SEEABOVE
            it.periodOfPerformanceBegin = LocalDate.of(2024, Month.DECEMBER, 1)
            it.periodOfPerformanceEnd = LocalDate.of(2024, Month.DECEMBER, 31)
            it.modeOfPaymentType = ModeOfPaymentType.FIXED
        }
    }

    private fun createPaymentSchedule(pos: AuftragsPositionDO, id: String): PaymentScheduleDO {
        return PaymentScheduleDO().also {
            it.auftrag = pos.auftrag
            it.positionNumber = pos.number
            it.scheduleDate = LocalDate.of(2024, Month.DECEMBER, 27)
            it.amount = BigDecimal.TEN
            it.comment = "comment $id"
            it.reached = false
            it.vollstaendigFakturiert = false
        }
    }
}
