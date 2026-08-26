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
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Order book users (right [org.projectforge.business.user.UserRightId.PM_ORDER_BOOK], but without the
 * invoice right [RechnungDao.USER_RIGHT_ID]) may read the outgoing invoices linked to an order they are
 * allowed to see, as long as the invoice isn't older than
 * [RechnungDao.MAX_YEARS_OF_VISIBILITY_4_ORDER_BOOK_USER] years - and nothing else. This asserts that
 * per-row grant, that it is read-only, and that finance access is unchanged.
 *
 * [TEST_PROJECT_MANAGER_USER] is exactly such a user: initTestDB gives it PM_ORDER_BOOK but no invoice
 * right (see RechnungDaoTest.checkAccess, where it is refused every invoice today).
 */
class RechnungDaoOrderBookAccessTest : AbstractTestBase() {
    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    @Autowired
    private lateinit var auftragsCache: AuftragsCache

    @Test
    fun `order book users read only the linked, recent invoices of orders they may see`() {
        logon(TEST_FINANCE_USER)
        // The project manager user is in this group, so it may see the group's project and its order.
        val pmGroup = initTestDB.addGroup(
            "RechnungDaoOrderBookAccessTest.PMGroup", TEST_PROJECT_MANAGER_USER
        )
        val projekt = ProjektDO().also {
            it.name = "RechnungDaoOrderBookAccessTest - Webportal"
            it.projektManagerGroup = pmGroup
        }
        projektDao.insert(projekt)

        val auftrag = createOrder().also {
            it.nummer = auftragDao.getNextNumber(it)
            it.projekt = projekt
            it.addPosition(createOrderPos())
        }
        val auftragId = auftragDao.insert(auftrag)
        // Reload detached so its position carries its generated id (used as the invoice -> order link).
        val orderPos = auftragDao.find(auftragId, checkAccess = false)!!.getPosition(1.toShort())

        val today = LocalDate.now()
        // Linked to the order and recent: visible to the order book user.
        val linkedId = insertInvoice(projekt, today, orderPos, "linked, recent")
        // Linked but older than the cutoff: not visible.
        val oldLinkedId = insertInvoice(
            projekt,
            today.minusYears((RechnungDao.MAX_YEARS_OF_VISIBILITY_4_ORDER_BOOK_USER + 1).toLong()),
            orderPos,
            "linked, too old",
        )
        // Recent but linked to no order: not visible.
        val unlinkedId = insertInvoice(projekt, today, null, "unlinked")

        // The access check reads the invoice -> order link from the caches; make sure they see the inserts.
        rechnungCache.forceReload()
        auftragsCache.forceReload()

        val linkedInvoice = rechnungDao.find(linkedId, checkAccess = false)!!
        val oldLinkedInvoice = rechnungDao.find(oldLinkedId, checkAccess = false)!!
        val unlinkedInvoice = rechnungDao.find(unlinkedId, checkAccess = false)!!

        val orderBookUser = getUser(TEST_PROJECT_MANAGER_USER)

        // The order book user may open the (per-row filtered) list at all.
        logon(TEST_PROJECT_MANAGER_USER)
        Assertions.assertTrue(
            rechnungDao.hasLoggedInUserSelectAccess(false),
            "Order book user may open the outgoing invoice list.",
        )

        // ... and per row only the linked, recent invoice:
        Assertions.assertTrue(
            rechnungDao.hasUserSelectAccess(orderBookUser, linkedInvoice, false),
            "Order book user sees an invoice linked to an order they may see.",
        )
        Assertions.assertFalse(
            rechnungDao.hasUserSelectAccess(orderBookUser, oldLinkedInvoice, false),
            "Order book user does not see an invoice older than the cutoff.",
        )
        Assertions.assertFalse(
            rechnungDao.hasUserSelectAccess(orderBookUser, unlinkedInvoice, false),
            "Order book user does not see an invoice linked to no order.",
        )

        // Read-only: neither insert nor update:
        Assertions.assertFalse(
            rechnungDao.hasLoggedInUserInsertAccess(),
            "Order book user must not insert invoices.",
        )
        Assertions.assertFalse(
            rechnungDao.hasLoggedInUserUpdateAccess(linkedInvoice, linkedInvoice, false),
            "Order book user must not update invoices.",
        )

        // A user without the order book right still sees no invoice at all.
        Assertions.assertFalse(
            rechnungDao.hasUserSelectAccess(getUser(TEST_USER), false),
            "A user without any invoice or order book right may not open the list.",
        )

        // Finance access is unchanged - every invoice, whatever its age or link:
        val financeUser = getUser(TEST_FINANCE_USER)
        Assertions.assertTrue(rechnungDao.hasUserSelectAccess(financeUser, linkedInvoice, false))
        Assertions.assertTrue(rechnungDao.hasUserSelectAccess(financeUser, oldLinkedInvoice, false))
        Assertions.assertTrue(rechnungDao.hasUserSelectAccess(financeUser, unlinkedInvoice, false))
    }

    /**
     * Inserts a one-position outgoing invoice in its own transaction. [orderPos] links the position to an
     * order (null for none). Must be inserted immediately after building it, so its number stays the next
     * free one.
     *
     * Note: inserting the order (and every previous linked invoice) marks [AuftragsCache] expired, and
     * [RechnungDao.afterInsertOrModify] recalculates the invoice via [AuftragsCache.getOrderPositionInfo]
     * *inside* this insert's transaction. That read no longer refreshes the cache (RechnungCache.update
     * passes checkRefresh = false), so it can't run a SELECT on the invoice-position table the still-open
     * insert has locked - what used to be a self-deadlock.
     */
    private fun insertInvoice(
        projekt: ProjektDO,
        datum: LocalDate,
        orderPos: AuftragsPositionDO?,
        text: String,
    ): Serializable {
        val rechnung = RechnungDO().also {
            val position = RechnungsPositionDO()
            position.auftragsPosition = orderPos
            position.einzelNetto = BigDecimal("100")
            position.text = text
            it.addPosition(position)
            it.nummer = rechnungDao.getNextNumber(it)
            it.datum = datum
            it.faelligkeit = datum
            it.projekt = projekt
        }
        return rechnungDao.insert(rechnung)
    }

    private fun createOrder(): AuftragDO {
        return AuftragDO().also {
            it.status = AuftragsStatus.GELEGT
        }
    }

    private fun createOrderPos(): AuftragsPositionDO {
        return AuftragsPositionDO().also {
            it.status = AuftragsStatus.GELEGT
        }
    }
}
