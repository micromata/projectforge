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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.contributionmargin.ContributionMarginService
import org.projectforge.business.fibu.customergroup.CustomerGroupConfig
import org.projectforge.business.fibu.customergroup.CustomerGroupConfigTest.Companion.grant
import org.projectforge.business.fibu.customergroup.CustomerGroupService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.OperationType
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

/**
 * An order access grant ([org.projectforge.business.fibu.customergroup.OrderAccessGrant]) gives a project manager
 * tied to neither the orders nor their project read access to all orders of a customer: the orders themselves
 * (also in the list), their invoices and their projects' contribution margins, but no write access, and fully
 * invoiced orders only within [AuftragRight.MAX_DAYS_OF_VISIBILITY_4_PROJECT_MANGER] days.
 */
class OrderAccessGrantDbTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Autowired
    private lateinit var contributionMarginService: ContributionMarginService

    @Autowired
    private lateinit var customerGroupService: CustomerGroupService

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun `a customer grant gives read access to the customer's orders, invoices and contribution margins`() {
        logon(TEST_FINANCE_USER)
        val granted = insertKunde(9301, "Granted AG")
        val other = insertKunde(9302, "Other AG")
        // A project the project manager is tied to in no way: no manager group, no contact.
        val projekt = ProjektDO().also {
            it.name = "OrderAccessGrantDbTest"
            projektDao.insert(it)
        }
        val grantedOrder = insertOrder(granted, projekt)
        val otherOrder = insertOrder(other, projekt)
        val oldInvoicedOrder = insertOrder(granted, projekt, angebotsDatum = LocalDate.now().minusDays(2000), invoiced = true)
        val orderPos = auftragDao.find(grantedOrder.id, checkAccess = false)!!.getPosition(1.toShort())
        val invoiceId = insertInvoice(projekt, orderPos)
        rechnungCache.forceReload()
        auftragsCache.forceReload()

        val pm = getUser(TEST_PROJECT_MANAGER_USER)
        val right = AuftragRight()
        assertFalse(right.hasAccess(pm, grantedOrder, null, OperationType.SELECT), "No grant yet.")

        val invoice = rechnungDao.find(invoiceId, checkAccess = false)!!
        // The configuration is shared by all tests of the test DB: revoked also if an assertion fails.
        try {
            save(CustomerGroupConfig(orderAccess = mutableListOf(grant(null, "Granted", userIds = listOf(pm.id!!), customers = listOf(9301)))))

            assertTrue(right.hasAccess(pm, grantedOrder, null, OperationType.SELECT))
            assertFalse(right.hasAccess(pm, otherOrder, null, OperationType.SELECT))
            assertFalse(right.hasAccess(pm, oldInvoicedOrder, null, OperationType.SELECT), "Fully invoiced, too old.")
            // Read only:
            assertFalse(right.hasAccess(pm, grantedOrder, grantedOrder, OperationType.UPDATE))
            assertFalse(right.hasAccess(pm, grantedOrder, null, OperationType.INSERT))
            assertFalse(right.hasAccess(pm, grantedOrder, grantedOrder, OperationType.DELETE))
            // Not for anybody else, even another project manager:
            assertFalse(right.hasAccess(getUser(TEST_PROJECT_ASSISTANT_USER), grantedOrder, null, OperationType.SELECT))

            assertTrue(rechnungDao.hasUserSelectAccess(pm, invoice, false), "The invoice of a granted order.")
            assertEquals(setOf(projekt.id), contributionMarginService.allowedProjectIds(listOf(grantedOrder), pm))
            assertEquals(emptySet<Long>(), contributionMarginService.allowedProjectIds(listOf(otherOrder), pm))

            logon(TEST_PROJECT_MANAGER_USER)
            val ids = auftragDao.select(AuftragFilter()).map { it.id }
            assertTrue(grantedOrder.id in ids)
            assertFalse(otherOrder.id in ids)
        } finally {
            logon(TEST_FINANCE_USER)
            save(CustomerGroupConfig())
        }
        assertFalse(right.hasAccess(pm, grantedOrder, null, OperationType.SELECT), "Revoked.")
        assertFalse(rechnungDao.hasUserSelectAccess(pm, invoice, false))
    }

    private fun save(config: CustomerGroupConfig) {
        val errors = customerGroupService.save(config, customerGroupService.lastUpdate)
        assertEquals(emptyList<Any>(), errors.map { "${it.fieldId}: ${it.messageKey}" })
    }

    private fun insertKunde(nummer: Long, name: String): KundeDO = KundeDO().also {
        it.nummer = nummer
        it.name = name
        kundeDao.insert(it, checkAccess = false)
    }

    private fun insertOrder(
        kunde: KundeDO,
        projekt: ProjektDO,
        angebotsDatum: LocalDate = LocalDate.now(),
        invoiced: Boolean = false,
    ): AuftragDO {
        val status = if (invoiced) AuftragsStatus.ABGESCHLOSSEN else AuftragsStatus.GELEGT
        val order = AuftragDO().also {
            it.nummer = auftragDao.getNextNumber(it)
            it.status = status
            it.kunde = kunde
            it.projekt = projekt
            it.angebotsDatum = angebotsDatum
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.status = status
                pos.vollstaendigFakturiert = invoiced
            })
        }
        auftragDao.insert(order)
        return auftragDao.find(order.id, checkAccess = false)!!
    }

    private fun insertInvoice(projekt: ProjektDO, orderPos: AuftragsPositionDO?): Long {
        val rechnung = RechnungDO().also {
            it.addPosition(RechnungsPositionDO().also { pos ->
                pos.auftragsPosition = orderPos
                pos.einzelNetto = BigDecimal("100")
                pos.text = "granted"
            })
            it.nummer = rechnungDao.getNextNumber(it)
            it.datum = LocalDate.now()
            it.faelligkeit = LocalDate.now()
            it.projekt = projekt
        }
        return rechnungDao.insert(rechnung)
    }
}
