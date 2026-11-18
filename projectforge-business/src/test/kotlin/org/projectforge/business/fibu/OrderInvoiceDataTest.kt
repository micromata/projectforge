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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The data an order hands to its invoices (BT-12, BT-13, BT-19, BT-56, BT-58, BT-73, BT-74) survives a save and an
 * update, and the customer's order number and the contract number can be searched for.
 */
class OrderInvoiceDataTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Test
    fun `the invoice data of an order, its positions and its payment schedule is stored and updated`() {
        logon(TEST_FINANCE_USER)
        val order = AuftragDO().also {
            it.status = AuftragsStatus.GELEGT
            it.nummer = auftragDao.getNextNumber(it)
            it.customerOrderNumber = "PO-ROUNDTRIP-1"
            it.contractNumber = "CTR-ROUNDTRIP-1"
            it.customerCostCenter = "KST-1"
            it.customerContactPerson = "Erika Mustermann"
            it.customerContactDepartment = "Einkauf"
            it.customerContactEmail = "einkauf@example.com"
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.status = AuftragsStatus.GELEGT
                pos.nettoSumme = BigDecimal.TEN
                pos.customerOrderNumber = "PO-POS-1"
                pos.customerCostCenter = "KST-POS-1"
            })
            it.addPaymentSchedule(PaymentScheduleDO().also { schedule ->
                schedule.positionNumber = 1
                schedule.scheduleDate = LocalDate.of(2026, 3, 1)
                schedule.amount = BigDecimal.TEN
                schedule.periodOfPerformanceBegin = LocalDate.of(2026, 1, 1)
                schedule.periodOfPerformanceEnd = LocalDate.of(2026, 1, 31)
            })
        }
        val id = auftragDao.insert(order)

        persistenceService.runInTransaction { _ ->
            val stored = auftragDao.find(id)!!
            assertEquals("PO-ROUNDTRIP-1", stored.customerOrderNumber)
            assertEquals("CTR-ROUNDTRIP-1", stored.contractNumber)
            assertEquals("KST-1", stored.customerCostCenter)
            assertEquals("Erika Mustermann", stored.customerContactPerson)
            assertEquals("Einkauf", stored.customerContactDepartment)
            assertEquals("einkauf@example.com", stored.customerContactEmail)
            assertEquals("PO-POS-1", stored.positionen!![0].customerOrderNumber)
            assertEquals("KST-POS-1", stored.positionen!![0].customerCostCenter)
            assertEquals(LocalDate.of(2026, 1, 1), stored.paymentSchedules!![0].periodOfPerformanceBegin)
            assertEquals(LocalDate.of(2026, 1, 31), stored.paymentSchedules!![0].periodOfPerformanceEnd)
        }

        persistenceService.runInTransaction { _ ->
            val stored = auftragDao.find(id, attached = true)!!
            stored.customerOrderNumber = "PO-ROUNDTRIP-2"
            stored.customerContactEmail = null
            stored.positionen!![0].customerOrderNumber = "PO-POS-2"
            stored.paymentSchedules!![0].periodOfPerformanceEnd = LocalDate.of(2026, 2, 28)
            auftragDao.update(stored)
        }

        persistenceService.runInTransaction { _ ->
            val stored = auftragDao.find(id)!!
            assertEquals("PO-ROUNDTRIP-2", stored.customerOrderNumber)
            assertEquals(null, stored.customerContactEmail)
            assertEquals("CTR-ROUNDTRIP-1", stored.contractNumber)
            assertEquals("PO-POS-2", stored.positionen!![0].customerOrderNumber)
            assertEquals("KST-POS-1", stored.positionen!![0].customerCostCenter)
            assertEquals(LocalDate.of(2026, 1, 1), stored.paymentSchedules!![0].periodOfPerformanceBegin)
            assertEquals(LocalDate.of(2026, 2, 28), stored.paymentSchedules!![0].periodOfPerformanceEnd)
        }
    }

    @Test
    fun `an order is found by the customer's order number, the contract number and the one of a position`() {
        logon(TEST_FINANCE_USER)
        val order = AuftragDO().also {
            it.status = AuftragsStatus.GELEGT
            it.nummer = auftragDao.getNextNumber(it)
            it.customerOrderNumber = "POSEARCH4711"
            it.contractNumber = "CTRSEARCH4712"
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.status = AuftragsStatus.GELEGT
                pos.nettoSumme = BigDecimal.TEN
                pos.customerOrderNumber = "POSSEARCH4713"
            })
        }
        val id = auftragDao.insert(order)
        // A new row is not indexed at once, see BaseDao.reindex.
        persistenceService.runInTransaction { _ -> auftragDao.reindex(auftragDao.find(id)!!) }

        for (term in listOf("POSEARCH4711", "CTRSEARCH4712", "POSSEARCH4713")) {
            val filter = AuftragFilter().also { it.searchString = term }
            val found = auftragDao.select(filter).map { it.id }
            assertTrue(found.contains(id), "'$term' finds the order: $found")
        }
    }
}
