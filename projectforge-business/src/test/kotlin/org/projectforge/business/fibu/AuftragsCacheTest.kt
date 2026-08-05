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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.commons.test.TestUtils.Companion.assertSame
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AuftragsCacheTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var auftragsRechnungCache: AuftragsRechnungCache

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun `test order workflow with invoices`() {
        val order = AuftragDO().also {
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.titel = "Pos 1"
                pos.nettoSumme = 100.toBigDecimal()
                pos.status = AuftragsStatus.GELEGT
            })
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.titel = "Pos 2"
                pos.nettoSumme = 200.toBigDecimal()
                pos.status = AuftragsStatus.GELEGT
            })
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.titel = "Pos 3"
                pos.nettoSumme = 400.toBigDecimal()
                pos.status = AuftragsStatus.GELEGT
            })
            it.status = AuftragsStatus.GELEGT
            it.nummer = auftragDao.nextNumber
        }
        auftragDao.insert(order, checkAccess = false)
        auftragsCache.getOrderInfo(order.id).also { orderInfo ->
            assertValues(orderInfo, akquiseSum = 700, netSum = 700)
        }
        order.status = AuftragsStatus.BEAUFTRAGT
        order.positionen!!.find { it.titel == "Pos 1" }!!.status = AuftragsStatus.BEAUFTRAGT
        order.positionen!!.find { it.titel == "Pos 2" }!!.status = AuftragsStatus.ABGELEHNT
        order.positionen!!.find { it.titel == "Pos 3" }!!.status = AuftragsStatus.OPTIONAL
        auftragDao.update(order, checkAccess = false)
        auftragsCache.getOrderInfo(order.id).let { orderInfo ->
            assertValues(orderInfo, akquiseSum = 400, netSum = 500, orderedNetSum = 100, notYetInvoicedSum = 100)
        }
        order.positionen!!.find { it.titel == "Pos 1" }!!.status = AuftragsStatus.ABGESCHLOSSEN
        auftragDao.update(order, checkAccess = false)
        auftragsCache.getOrderInfo(order.id).also { orderInfo ->
            assertValues(
                orderInfo, akquiseSum = 400, netSum = 500, orderedNetSum = 100,
                toBeInvoicedSum = 100, notYetInvoicedSum = 100, toBeInvoiced = true
            )
        }
        order.addPaymentSchedule(PaymentScheduleDO().also { schedule ->
            schedule.amount = 50.toBigDecimal()
            schedule.reached = true
        })
        auftragDao.update(order, checkAccess = false)
        auftragsCache.getOrderInfo(order.id).also { orderInfo ->
            // 100 for position to be invoiced and 50 by reached payment schedule which is not assigned to a position.
            assertValues(
                orderInfo, akquiseSum = 400, netSum = 500, orderedNetSum = 100,
                toBeInvoicedSum = 150, notYetInvoicedSum = 100, toBeInvoiced = true
            )
        }
        order.addPaymentSchedule(PaymentScheduleDO().also { schedule ->
            schedule.amount = 25.toBigDecimal()
            schedule.reached = true
            schedule.vollstaendigFakturiert = true
        })
        order.addPaymentSchedule(PaymentScheduleDO().also { schedule ->
            schedule.amount = 80.toBigDecimal()
            schedule.reached = true
            schedule.positionNumber = order.positionen!!.find { it.titel == "Pos 1" }!!.number
        })
        auftragDao.update(order, checkAccess = false)
        auftragsCache.getOrderInfo(order.id).also { orderInfo ->
            // (25) is ignored (already fully invoiced).
            // 80 for position to be invoiced is "overwritten" by 80 of payment schedule.
            // 50 by reached payment schedule which is not assigned to a position.
            assertValues(
                orderInfo, akquiseSum = 400, netSum = 500, orderedNetSum = 100,
                toBeInvoicedSum = 130, notYetInvoicedSum = 100, toBeInvoiced = true
            )
        }
    }

        @Test
    fun `test order in akquisition with invoices`() {
        val order = AuftragDO().also {
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.titel = "Pos 1"
                pos.nettoSumme = 100.toBigDecimal()
                pos.status = AuftragsStatus.GELEGT
            })
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.titel = "Pos 2"
                pos.nettoSumme = 200.toBigDecimal()
                pos.status = AuftragsStatus.GELEGT
            })
            it.status = AuftragsStatus.GELEGT
            it.nummer = auftragDao.nextNumber
        }
        auftragDao.insert(order, checkAccess = false)
        auftragsCache.getOrderInfo(order.id).let { orderInfo ->
            assertValues(orderInfo, akquiseSum = 300, netSum = 300)
        }
        val invoice = RechnungDO().also {
            it.addPosition(RechnungsPositionDO().also { pos -> // 50 of 100
                pos.auftragsPosition = order.positionen!!.get(0)
                pos.menge = 5.toBigDecimal()
                pos.einzelNetto = 10.toBigDecimal()
            })
            it.addPosition(RechnungsPositionDO().also { pos -> // 200 of 200
                pos.auftragsPosition = order.positionen!!.get(1)
                pos.menge = 1.toBigDecimal()
                pos.einzelNetto = 200.toBigDecimal()
            })
            it.datum = LocalDate.now()
            it.faelligkeit = LocalDate.now().plusDays(14)
            it.kundeText = "ACME"
            it.nummer = rechnungDao.nextNumber
        }
        rechnungDao.insert(invoice, checkAccess = false)
        auftragsCache.getOrderInfo(order.id).let { orderInfo ->
            assertValues(orderInfo, akquiseSum = 300, netSum = 300, invoicedSum = 250)
        }
    }

    /**
     * [AuftragsCache] and [AuftragsRechnungCache] use each other, so the order of their refreshes decided whether the
     * invoiced sums of the orders were calculated on top of the invoice positions or on top of an empty invoice cache
     * (resulting in invoicedSum = 0 for all positions). Whatever the order is, one access has to deliver correct sums.
     */
    @Test
    fun `invoiced sums are correct whatever the refresh order of the caches is`() {
        val order = createOrderWithInvoice(netSum = 500, invoicedSum = 300)
        // AuftragsRechnungCache first, AuftragsCache afterwards:
        auftragsRechnungCache.forceReload()
        auftragsCache.setExpired()
        assertInvoicedSums(order, netSum = 500, invoicedSum = 300)

        // AuftragsCache first, AuftragsRechnungCache afterwards:
        auftragsCache.forceReload()
        auftragsRechnungCache.setExpired()
        assertInvoicedSums(order, netSum = 500, invoicedSum = 300)

        // Both invalidated (as done by SystemService.refreshCaches), no refresh yet:
        auftragsRechnungCache.setExpired()
        auftragsCache.setExpired()
        assertInvoicedSums(order, netSum = 500, invoicedSum = 300)

        // Both invalidated and both reloaded, order cache first (this was the failing case):
        auftragsRechnungCache.setExpired()
        auftragsCache.setExpired()
        auftragsCache.forceReload()
        auftragsRechnungCache.forceReload()
        assertInvoicedSums(order, netSum = 500, invoicedSum = 300)
    }

    /**
     * Concurrent access must not result in an order info calculated on top of a cache which is currently rebuilding
     * (this was the reason why the invoiced sums were 0 after the release: two threads refreshed [AuftragsCache] at the
     * same time and the loser overwrote the correct result).
     */
    @Test
    fun `invoiced sums are correct after concurrent cache reloads`() {
        val order = createOrderWithInvoice(netSum = 800, invoicedSum = 250)
        val numberOfThreads = 6
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(numberOfThreads)
        try {
            val futures = (1..numberOfThreads).map { idx ->
                executor.submit {
                    start.await()
                    if (idx % 2 == 0) {
                        auftragsCache.forceReload()
                    } else {
                        auftragsRechnungCache.forceReload()
                    }
                }
            }
            start.countDown()
            futures.forEach { it.get(60, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
        assertInvoicedSums(order, netSum = 800, invoicedSum = 250)
    }

    /**
     * Creates an order with one position of [netSum] and one invoice of [invoicedSum] assigned to this position.
     */
    private fun createOrderWithInvoice(netSum: Int, invoicedSum: Int): AuftragDO {
        val order = AuftragDO().also {
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.titel = "Pos 1"
                pos.nettoSumme = netSum.toBigDecimal()
                pos.status = AuftragsStatus.BEAUFTRAGT
            })
            it.status = AuftragsStatus.BEAUFTRAGT
            it.nummer = auftragDao.nextNumber
        }
        auftragDao.insert(order, checkAccess = false)
        val invoice = RechnungDO().also {
            it.addPosition(RechnungsPositionDO().also { pos ->
                pos.auftragsPosition = order.positionen!!.first()
                pos.menge = BigDecimal.ONE
                pos.einzelNetto = invoicedSum.toBigDecimal()
            })
            it.datum = LocalDate.now()
            it.faelligkeit = LocalDate.now().plusDays(14)
            it.kundeText = "ACME"
            it.nummer = rechnungDao.nextNumber
        }
        rechnungDao.insert(invoice, checkAccess = false)
        return order
    }

    private fun assertInvoicedSums(order: AuftragDO, netSum: Int, invoicedSum: Int) {
        val orderInfo = auftragsCache.getOrderInfo(order.id)
        Assertions.assertNotNull(orderInfo, "OrderInfo not found.")
        assertSame(invoicedSum, orderInfo!!.invoicedSum, "Invoiced sum of the order.")
        assertSame(netSum - invoicedSum, orderInfo.notYetInvoicedSum, "Not yet invoiced sum of the order.")
        val posInfo = auftragsCache.getOrderPositionInfosByAuftragId(order.id)?.firstOrNull()
        Assertions.assertNotNull(posInfo, "OrderPositionInfo not found.")
        assertSame(invoicedSum, posInfo!!.invoicedSum, "Invoiced sum of the order position.")
        assertSame(netSum - invoicedSum, posInfo.notYetInvoiced, "Not yet invoiced sum of the order position.")
    }

    private fun assertValues(
        orderInfo: OrderInfo?,
        akquiseSum: Int = 0,
        netSum: Int = 0,
        orderedNetSum: Int = 0,
        invoicedSum: Int = 0,
        notYetInvoicedSum: Int = 0,
        toBeInvoicedSum: Int = 0,
        toBeInvoiced: Boolean = false,
    ) {
        Assertions.assertNotNull(orderInfo, "OrderInfo not found.")
        assertSame(orderedNetSum, orderInfo!!.commissionedNetSum, "Ordered sum.")
        assertSame(akquiseSum, orderInfo.akquiseSum, "Akquise sum.")
        assertSame(netSum, orderInfo.netSum, "Net sum.")
        assertSame(notYetInvoicedSum, orderInfo.notYetInvoicedSum, "Not yet invoiced sum.")
        assertSame(invoicedSum, orderInfo.invoicedSum, "Invoiced sum.")
        assertSame(toBeInvoicedSum, orderInfo.toBeInvoicedSum, "To be invoiced sum.")
        Assertions.assertEquals(toBeInvoiced, orderInfo.toBeInvoiced, "To be invoiced.")
    }
}
