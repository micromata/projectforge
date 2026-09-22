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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungInfo
import java.math.BigDecimal

/**
 * What counts as an incomplete invoice, which is decided by two independent settings of the installation
 * ([org.projectforge.business.fibu.InvoiceConfiguration.accountRequired] and
 * `Configuration.isCostConfigured`) - so the filter has to be silent about whichever of them is off, or the
 * list it narrows shows invoices nobody has anything to do about.
 *
 * A plain unit test: the account is resolved by a lambda the caller passes in, so neither a cache nor a
 * database is involved.
 */
class IncompleteInvoiceFilterTest {

    @Test
    fun `with cost accounting only, the assignments decide`() {
        val filter = filter(costConfigured = true, accountRequired = false)

        assertTrue(filter.matches(invoice(shortfall = BigDecimal("1400.00"))))
        assertFalse(filter.matches(invoice(shortfall = BigDecimal.ZERO)))
        // No account, but none is asked for.
        assertFalse(filter.matches(invoice(shortfall = BigDecimal.ZERO, account = null)))
    }

    @Test
    fun `with a required account only, the account decides`() {
        val filter = filter(costConfigured = false, accountRequired = true)

        assertTrue(filter.matches(invoice(account = null)))
        assertFalse(filter.matches(invoice(account = KontoDO())))
        // A shortfall, but this installation doesn't keep cost accounting at all.
        assertFalse(filter.matches(invoice(shortfall = BigDecimal("1400.00"), account = KontoDO())))
    }

    @Test
    fun `with both, either reason is enough`() {
        val filter = filter(costConfigured = true, accountRequired = true)

        assertTrue(filter.matches(invoice(shortfall = BigDecimal("1400.00"), account = KontoDO())))
        assertTrue(filter.matches(invoice(shortfall = BigDecimal.ZERO, account = null)))
        assertTrue(filter.matches(invoice(shortfall = BigDecimal("1400.00"), account = null)))
        assertFalse(filter.matches(invoice(shortfall = BigDecimal.ZERO, account = KontoDO())))
    }

    /**
     * The account of an outgoing invoice may be inherited from its project or its customer, and such an
     * invoice lacks nothing - which is why the filter is given a resolver and not the invoice's own field
     * (`KontoCache.getKonto(invoice)`).
     */
    @Test
    fun `an inherited account counts as given`() {
        val invoice = invoice(account = null)
        val filter = IncompleteInvoiceFilter<RechnungDO>(
            costConfigured = false,
            accountRequired = true,
            accountOf = { KontoDO() }, // Stands for the account of the project or the customer.
            infoOf = { it.info }, // The test sets info by hand; read it as the cache would.
        )
        assertFalse(filter.matches(invoice))
    }

    /**
     * `BigDecimal.equals` compares the scale as well, so an assignment sum that adds up exactly must not
     * read as a difference just because it was calculated to two decimal places.
     */
    @Test
    fun `a zero shortfall of any scale is no difference`() {
        val filter = filter(costConfigured = true, accountRequired = false)
        assertFalse(filter.matches(invoice(shortfall = BigDecimal("0.00"))))
    }

    @Test
    fun `the filter is offered as long as either setting is on`() {
        assertTrue(IncompleteInvoiceFilter.isOffered(costConfigured = true, accountRequired = false))
        assertTrue(IncompleteInvoiceFilter.isOffered(costConfigured = false, accountRequired = true))
        assertTrue(IncompleteInvoiceFilter.isOffered(costConfigured = true, accountRequired = true))
        // Neither: it would match every invoice or none, so the list doesn't offer it.
        assertFalse(IncompleteInvoiceFilter.isOffered(costConfigured = false, accountRequired = false))
    }

    /** The invoice's own account, as the outgoing invoice list resolves it (without the inheritance). */
    private fun filter(costConfigured: Boolean, accountRequired: Boolean) =
        IncompleteInvoiceFilter<RechnungDO>(
            costConfigured = costConfigured,
            accountRequired = accountRequired,
            accountOf = { it.konto },
            infoOf = { it.info }, // The test sets info by hand; read it as the cache would.
        )

    /**
     * [RechnungInfo] is set by hand rather than left to `RechnungCalculator`: the difference is what this
     * test varies, and calculating it would mean building positions and cost assignments for a number the
     * filter only reads.
     */
    private fun invoice(shortfall: BigDecimal = BigDecimal.ZERO, account: KontoDO? = null): RechnungDO {
        return RechnungDO().also { invoice ->
            invoice.konto = account
            invoice.info = RechnungInfo(invoice).also { it.kostZuweisungenFehlbetrag = shortfall }
        }
    }

    /** [org.projectforge.framework.persistence.api.impl.CustomResultFilter] asks about one of a list. */
    private fun IncompleteInvoiceFilter<RechnungDO>.matches(invoice: RechnungDO): Boolean {
        return match(mutableListOf(), invoice)
    }
}
