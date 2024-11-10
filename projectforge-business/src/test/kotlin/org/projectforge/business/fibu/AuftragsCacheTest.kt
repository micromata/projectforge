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
import org.projectforge.commons.test.TestUtils
import org.projectforge.commons.test.TestUtils.Companion.assertSame
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AuftragsCacheTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun `test order in akquisition with invoices`() {
        val order = AuftragDO().also {
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.titel = "Pos 1"
                pos.nettoSumme = 100.toBigDecimal()
                pos.status = AuftragsPositionsStatus.GELEGT
            })
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.titel = "Pos 2"
                pos.nettoSumme = 200.toBigDecimal()
                pos.status = AuftragsPositionsStatus.GELEGT
            })
            it.auftragsStatus = AuftragsStatus.GELEGT
            it.nummer = auftragDao.nextNumber
        }
        auftragDao.insert(order, checkAccess = false)
        var orderInfo = auftragsCache.getOrderInfo(order.id)
        Assertions.assertNotNull(orderInfo)
        assertSame(300, orderInfo!!.akquiseSum, "Akquise sum.")
        assertSame(300, orderInfo.netSum, "Net sum.")
        orderInfo.apply {
            TestUtils.assertZero(
                invoicedSum,
                toBeInvoicedSum,
                notYetInvoicedSum,
                orderedNetSum,
                message = "Values must be zero.",
            )
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
        orderInfo = auftragsCache.getOrderInfo(order.id)
        Assertions.assertNotNull(orderInfo)
        assertSame(300, orderInfo!!.akquiseSum, "Akquise sum.")
        assertSame(300, orderInfo.netSum, "Net sum.")
        assertSame(250, orderInfo.invoicedSum, "Invoiced sum.")
        assertSame(0, orderInfo.notYetInvoicedSum, "Not yet invoiced sum. Order not yet signed, so 0 expected!")
        orderInfo.apply {
            TestUtils.assertZero(
                orderedNetSum,
                toBeInvoicedSum, // No position is set to be invoiced.
                message = "Values must be zero. No position is ordered or to be invoiced.",
            )
        }
    }
}
