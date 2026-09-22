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

import org.projectforge.business.fibu.AbstractRechnungDO
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.RechnungInfo
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.utils.NumberHelper

/**
 * Id of the filter [IncompleteInvoiceFilter] answers - a pseudo field of the invoice lists, since neither
 * of the two things it asks about is a column of the invoice.
 *
 * Named after the question and not after either reason: which of them applies depends on the installation
 * (see [IncompleteInvoiceFilter]), and the amount that is missing is on the list under its own name
 * (`Rechnung.kostZuweisungenFehlbetrag`, a column of its own).
 */
internal const val INCOMPLETE_FILTER = "incomplete"

/**
 * The invoices something is still missing from, as far as this installation's bookkeeping is concerned:
 *
 * - the net sum assigned to cost units differs from the invoice's own net sum
 *   ([org.projectforge.business.fibu.RechnungInfo.kostZuweisungenFehlbetrag]), where cost accounting is
 *   configured at all (`Configuration.isCostConfigured`);
 * - no account is named, where accounts are expected
 *   (`InvoiceConfiguration.accountRequired`).
 *
 * Either one is enough - the filter answers "is anything missing", not "which". Where neither applies the
 * filter is not offered at all, since it would then match every invoice or none.
 *
 * A [CustomResultFilter] and no query criterion: the difference is computed by
 * [org.projectforge.business.fibu.RechnungCalculator] from every position of the invoice, and the account
 * of an outgoing invoice may be inherited from its project or its customer - neither is a column any
 * `WHERE` clause could ask about.
 *
 * Generic over [AbstractRechnungDO] because both invoice lists ask the same question: the sums live on
 * `RechnungInfo`, which both kinds carry, and how an account is found is what [accountOf] says.
 *
 * @param accountOf The account of the invoice as this list understands it - for an outgoing invoice the
 * one `KontoCache.getKonto(invoice)` finds, which falls back through project and customer, since that is
 * the account the export uses; for an incoming invoice its own, there being no such fallback. Passed in
 * rather than resolved here so the filter needs no cache of its own.
 * @param infoOf The [RechnungInfo] of the invoice from the matching invoice cache, or null for one created
 * after the last refresh. Passed in for the reason [accountOf] is, and read instead of
 * [AbstractRechnungDO.ensuredInfo] for the reason [OutgoingInvoiceEntityRest.PaymentStateFilter] reads the
 * cache: a custom result filter runs inside `DBQuery.select`, before `BaseDao.select` fires the `afterLoad`
 * that would put the cached info on the row, so `ensuredInfo` would lazily load every invoice's positions
 * and cost assignments - one query pair per invoice, the N+1 storm the cache exists to avoid. The fallback
 * to `ensuredInfo` is for the single uncached row only.
 */
internal class IncompleteInvoiceFilter<O : AbstractRechnungDO>(
    private val costConfigured: Boolean,
    private val accountRequired: Boolean,
    private val accountOf: (O) -> KontoDO?,
    private val infoOf: (O) -> RechnungInfo?,
) : CustomResultFilter<O> {
    /**
     * `isNotZero` rather than `!= BigDecimal.ZERO`: `BigDecimal.equals` compares the scale as well, so a
     * `0.00` would count as a difference.
     */
    override fun match(list: MutableList<O>, element: O): Boolean {
        val info = infoOf(element) ?: element.ensuredInfo
        if (costConfigured && NumberHelper.isNotZero(info.kostZuweisungenFehlbetrag)) {
            return true
        }
        return accountRequired && accountOf(element) == null
    }

    companion object {
        /**
         * Whether the filter has anything to ask in this installation - if not, it is not offered, see the
         * class comment.
         */
        fun isOffered(costConfigured: Boolean, accountRequired: Boolean): Boolean {
            return costConfigured || accountRequired
        }
    }
}
