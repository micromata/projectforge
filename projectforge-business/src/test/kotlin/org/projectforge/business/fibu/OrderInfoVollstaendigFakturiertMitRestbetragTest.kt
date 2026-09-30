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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.time.PFDay
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Positions and payment schedules marked as fully invoiced, but with a remaining amount not invoiced
 * (see [OrderInfo.vollstaendigFakturiertMitRestbetrag]).
 */
class OrderInfoVollstaendigFakturiertMitRestbetragTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    private val filter = AuftragFakturiertFilter(listOf(AuftragFakturiertFilterStatus.FAKTURIERT_MIT_RESTBETRAG))

    @Test
    fun `fully invoiced position with cancelled invoice has a remaining amount`() {
        logon(getUser(TEST_FINANCE_USER))
        val order = insertOrder(fullyInvoiced = true)
        val invoice = insertInvoice(order, "100")
        assertFalse(orderInfo(order).vollstaendigFakturiertMitRestbetrag)
        assertFalse(filter.match(mutableListOf(), order))

        persistenceService.runInTransaction {
            val loaded = rechnungDao.find(invoice.id)!!
            loaded.status = RechnungStatus.STORNIERT
            rechnungDao.update(loaded)
        }
        assertTrue(orderInfo(order).vollstaendigFakturiertMitRestbetrag, "The cancelled invoice doesn't count.")
        assertTrue(filter.match(mutableListOf(), order))
        assertFehlbetrag("100", order)
    }

    @Test
    fun `remaining amounts below the minimum are ignored`() {
        logon(getUser(TEST_FINANCE_USER))
        insertOrder(fullyInvoiced = true).let { order ->
            insertInvoice(order, "99.01")
            assertFalse(orderInfo(order).vollstaendigFakturiertMitRestbetrag, "0.99 remaining is ignored.")
            assertFehlbetrag(null, order)
        }
        insertOrder(fullyInvoiced = true).let { order ->
            insertInvoice(order, "99")
            assertTrue(orderInfo(order).vollstaendigFakturiertMitRestbetrag, "1.00 remaining counts.")
            assertFehlbetrag("1", order)
        }
        insertOrder(fullyInvoiced = false).let { order ->
            assertFalse(orderInfo(order).vollstaendigFakturiertMitRestbetrag, "Not marked as fully invoiced.")
        }
    }

    @Test
    fun `fully invoiced payment schedule without invoice has a remaining amount`() {
        logon(getUser(TEST_FINANCE_USER))
        val order = insertOrder(fullyInvoiced = false, positionStatus = AuftragsStatus.BEAUFTRAGT) {
            it.addPaymentSchedule(PaymentScheduleDO().also { schedule ->
                schedule.amount = BigDecimal("40")
                schedule.reached = true
                schedule.vollstaendigFakturiert = true
                schedule.positionNumber = it.positionen!![0].number
            })
        }
        assertTrue(orderInfo(order).vollstaendigFakturiertMitRestbetrag)
        assertFehlbetrag("40", order)
        insertInvoice(order, "40")
        assertFalse(orderInfo(order).vollstaendigFakturiertMitRestbetrag)
        assertFehlbetrag(null, order)
    }

    private fun orderInfo(order: AuftragDO): OrderInfo = auftragsCache.getOrderInfo(order.id)!!

    private fun assertFehlbetrag(expected: String?, order: AuftragDO) {
        val info = orderInfo(order)
        // The order has a single position, so the order's shortfall is the one of its position.
        listOf(info.getFehlbetrag(info.infoPositions!!.single()), info.fehlbetrag).forEach { fehlbetrag ->
            if (expected == null) {
                assertNull(fehlbetrag)
            } else {
                assertEquals(0, BigDecimal(expected).compareTo(fehlbetrag), "Expected $expected, got $fehlbetrag.")
            }
        }
    }

    private fun insertOrder(
        fullyInvoiced: Boolean,
        positionStatus: AuftragsStatus = AuftragsStatus.ABGESCHLOSSEN,
        modify: ((AuftragDO) -> Unit)? = null,
    ): AuftragDO {
        val order = AuftragDO().also {
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.titel = "Pos 1"
                pos.nettoSumme = BigDecimal("100")
                pos.status = positionStatus
                pos.vollstaendigFakturiert = fullyInvoiced
            })
            it.status = positionStatus
            it.nummer = auftragDao.nextNumber
        }
        modify?.invoke(order)
        auftragDao.insert(order, checkAccess = false)
        return order
    }

    private fun insertInvoice(order: AuftragDO, amount: String): RechnungDO {
        return RechnungDO().also {
            it.addPosition(RechnungsPositionDO().also { pos ->
                pos.auftragsPosition = order.getPosition(1.toShort())
                pos.einzelNetto = BigDecimal(amount)
                pos.text = amount
            })
            it.nummer = rechnungDao.getNextNumber(it)
            it.datum = PFDay.now().localDate
            it.faelligkeit = LocalDate.now()
            it.projekt = initTestDB.addProjekt(null, 1, "restTest")
            rechnungDao.insert(it)
        }
    }
}
