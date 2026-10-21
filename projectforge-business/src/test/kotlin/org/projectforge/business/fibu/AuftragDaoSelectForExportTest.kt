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


package org.projectforge.business.fibu

import org.hibernate.Hibernate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

/**
 * [AuftragDao.selectForExport] loads what the order export reads of all orders at once, instead of lazily, order
 * by order (one query per order and association).
 */
class AuftragDaoSelectForExportTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Test
    fun `positions, payment schedules, project and customers are loaded with the orders`() {
        logon(TEST_FINANCE_USER)
        val kunde = KundeDO().also {
            it.nummer = 9321
            it.name = "Export AG"
            kundeDao.insert(it, checkAccess = false)
        }
        val projekt = ProjektDO().also {
            it.name = "AuftragDaoSelectForExportTest"
            it.nummer = 9
            projektDao.setKunde(it, kunde.id)
            projektDao.insert(it, checkAccess = false)
        }
        val ids = (1..3).map { i ->
            AuftragDO().also {
                it.nummer = auftragDao.getNextNumber(it)
                it.status = AuftragsStatus.GELEGT
                it.kunde = kunde
                it.projekt = projekt
                it.angebotsDatum = LocalDate.now()
                it.addPosition(AuftragsPositionDO().also { pos -> pos.titel = "Position $i" })
                it.addPaymentSchedule(PaymentScheduleDO().also { schedule -> schedule.amount = BigDecimal(i) })
                auftragDao.insert(it)
            }.id!!
        }
        val unsaved = AuftragDO().also { it.id = -1L }
        // As the list delivers them: detached, the associations not loaded.
        val orders = ids.reversed().map { auftragDao.find(it, checkAccess = false)!! } + unsaved

        val loaded = auftragDao.selectForExport(orders)

        assertEquals(orders.map { it.id }, loaded.map { it.id }, "In the given order.")
        assertSame(unsaved, loaded.last(), "Not stored: taken as it is.")
        loaded.dropLast(1).forEach { order ->
            assertTrue(Hibernate.isInitialized(order.positionen), "positions")
            assertTrue(Hibernate.isInitialized(order.paymentSchedules), "payment schedules")
            assertTrue(Hibernate.isInitialized(order.projekt), "project")
            assertTrue(Hibernate.isInitialized(order.projekt!!.kunde), "project's customer")
            assertTrue(Hibernate.isInitialized(order.kunde), "customer")
            assertEquals(1, order.positionenExcludingDeleted.size)
            assertEquals(1, order.paymentSchedules!!.size)
            assertEquals("Export AG", order.projekt!!.kunde!!.name)
        }
    }
}
