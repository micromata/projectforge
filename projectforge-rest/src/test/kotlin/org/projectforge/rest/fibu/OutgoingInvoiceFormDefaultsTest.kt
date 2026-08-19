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

package org.projectforge.rest.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired

/**
 * The three reads the hand built invoice form does for its own defaults: [OutgoingInvoiceEntityRest.getFormDefaults],
 * [OutgoingInvoiceEntityRest.getActiveKost2] and [OutgoingInvoiceEntityRest.checkKost2].
 *
 * None of them writes anything, so what has to be tested is that each one checks the read access itself —
 * a `@GetMapping` on an entity rest class gets none of `AbstractPagesRest`'s checks for free — and that the
 * Kost2 comparison reproduces `RechnungEditForm.onRenderCostRow`, which is a rule with three `-1` cases in
 * it and the reason the answer is computed server side at all.
 */
class OutgoingInvoiceFormDefaultsTest : AbstractTestBase() {

    @Autowired
    private lateinit var outgoingInvoiceEntityRest: OutgoingInvoiceEntityRest

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektCache: ProjektCache

    @Test
    fun `the form defaults are readable with the finance right`() {
        logon(TEST_FINANCE_USER)
        val defaults = outgoingInvoiceEntityRest.getFormDefaults()

        // Every value is configuration, so the assertions are about the shape rather than about numbers a
        // test database happens to hold: `fibu.defaultVAT` may well be unset, and the bank accounts come
        // from the application properties, which a test run doesn't set either.
        assertNotNull(defaults.bankAccounts)
        assertNotNull(defaults.templateVariants)
        // With no custom template configured there is exactly one, unnamed variant - `InvoiceService`
        // answers `arrayOf("")` then, and the form has to show one export entry rather than none.
        assertTrue(defaults.templateVariants.isNotEmpty(), "At least the unnamed template variant.")
        defaults.bankAccounts.forEach { account ->
            // The value is the IBAN: it is what the column holds and what `findBankAccount` looks up.
            assertTrue(account.value.isNotBlank())
            assertTrue(account.label.isNotBlank())
        }
    }

    @Test
    fun `a user without the finance right may not read them`() {
        logon(TEST_USER)
        // The whole reason for the explicit check in each endpoint: they are plain GETs, so nothing else
        // would ask.
        org.junit.jupiter.api.assertThrows<AccessException> { outgoingInvoiceEntityRest.getFormDefaults() }
        org.junit.jupiter.api.assertThrows<AccessException> { outgoingInvoiceEntityRest.getActiveKost2(1L) }
        org.junit.jupiter.api.assertThrows<AccessException> {
            outgoingInvoiceEntityRest.checkKost2(kost2Id = 1L, projektId = null, kundeId = null)
        }
    }

    /**
     * Every project of this test carries a customer, and it has to: `InitTestDB.addProjekt` writes its cost
     * units as `5.<customer>.<project>`, while a project without a customer is `4.<internKost2_4>.<project>`
     * ([ProjektDO.nummernkreis]) - and `Kost2Dao.onInsertOrModify` rejects that mismatch outright.
     */
    private fun addCustomer(number: Long, name: String): KundeDO {
        val kunde = KundeDO()
        kunde.id = number
        kunde.name = name
        kundeDao.insert(kunde, checkAccess = false)
        return kunde
    }

    /**
     * Makes the caches see what was just written. Both are needed: `activeKost2` resolves the project through
     * [ProjektCache] and its cost units through [KostCache], and neither is invalidated by an insert made in
     * the same test run.
     */
    private fun reloadCaches() {
        projektCache.forceReload()
        kostCache.forceReload()
    }

    @Test
    fun `the cost units of a project are the ones a new cost assignment may start with`() {
        logon(TEST_FINANCE_USER)
        val projekt = persistenceService.runInTransaction { _ ->
            // Three cost types, so the answer is a list rather than a coincidence of one entry.
            initTestDB.addProjekt(addCustomer(71L, "form defaults ltd."), 61, "form defaults", 1L, 2L, 3L)
        }
        reloadCaches()

        val kost2List = outgoingInvoiceEntityRest.getActiveKost2(projekt.id)

        assertEquals(3, kost2List.size)
        kost2List.forEach { kost2 ->
            assertEquals(projekt.nummer, kost2.teilbereich)
            assertNotNull(kost2.displayName, "The formatted number is what the select shows.")
        }
        // Sorted, so the first entry the form preselects is the same one on every request - `KostCache`
        // answers whatever order its map iterates in.
        val names = kost2List.mapNotNull { it.displayName }
        assertEquals(kost2List.size, names.size, "Every entry is named.")
        assertEquals(names.sorted(), names)
    }

    @Test
    fun `an unknown or arealess project simply has no cost units`() {
        logon(TEST_FINANCE_USER)
        // Not an error: a new invoice names no project yet, and the form asks anyway.
        assertTrue(outgoingInvoiceEntityRest.getActiveKost2(null).isEmpty())
        assertTrue(outgoingInvoiceEntityRest.getActiveKost2(-1L).isEmpty())
    }

    @Test
    fun `a cost unit of the invoice's project matches, one of another project does not`() {
        logon(TEST_FINANCE_USER)
        val (mine, other) = persistenceService.runInTransaction { _ ->
            // Two customers, not one: a cost unit of another *area* is exactly what Wicket warns about, and
            // two projects of the same customer differ only in their last digit.
            Pair(
                initTestDB.addProjekt(addCustomer(72L, "kost2 check mine ltd."), 62, "kost2 check mine", 1L),
                initTestDB.addProjekt(addCustomer(73L, "kost2 check other ltd."), 63, "kost2 check other", 1L),
            )
        }
        reloadCaches()
        val myKost2 = outgoingInvoiceEntityRest.getActiveKost2(mine.id).single()
        val otherKost2 = outgoingInvoiceEntityRest.getActiveKost2(other.id).single()

        assertTrue(
            outgoingInvoiceEntityRest.checkKost2(myKost2.id, projektId = mine.id, kundeId = null).matchesInvoice
        )
        // What Wicket outlines the field for: the cost unit belongs to a different project's area.
        assertFalse(
            outgoingInvoiceEntityRest.checkKost2(otherKost2.id, projektId = mine.id, kundeId = null).matchesInvoice
        )
    }

    @Test
    fun `an invoice naming neither project nor customer is never warned about`() {
        logon(TEST_FINANCE_USER)
        val projekt = persistenceService.runInTransaction { _ ->
            initTestDB.addProjekt(addCustomer(74L, "kost2 check no owner ltd."), 64, "kost2 check no owner", 1L)
        }
        reloadCaches()
        val kost2 = outgoingInvoiceEntityRest.getActiveKost2(projekt.id).single()

        // Wicket returns early in both cases, so the field stays plain: there is nothing to compare against.
        assertTrue(outgoingInvoiceEntityRest.checkKost2(kost2.id, projektId = null, kundeId = null).matchesInvoice)
        // Same for a cost unit that cannot be resolved - a warning would say the user picked something
        // wrong, when in fact the answer is unknown.
        assertTrue(outgoingInvoiceEntityRest.checkKost2(-1L, projektId = projekt.id, kundeId = null).matchesInvoice)
        // Not unused: reading it back proves the fixture really carries the cost unit compared above.
        assertNotNull(kost2Dao.find(kost2.id, checkAccess = false))
    }
}
