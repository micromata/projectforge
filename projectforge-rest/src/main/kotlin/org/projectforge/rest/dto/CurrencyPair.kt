/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto

import org.projectforge.business.fibu.CurrencyPairDO
import java.math.BigDecimal

class CurrencyPair(
    id: Long? = null,
    displayName: String? = null,
    var sourceCurrency: String? = null,
    var targetCurrency: String? = null,
    var comment: String? = null,
) : BaseDTODisplayObject<CurrencyPairDO>(id, displayName = displayName) {
    /**
     * Read-only field containing all conversion rates for this currency pair.
     * Used in edit form to display rate history.
     */
    var rateEntries: List<CurrencyConversionRate>? = null

    /**
     * Read-only field containing the current (today's) conversion rate.
     * Used in list view for quick reference.
     */
    var currentRate: BigDecimal? = null

    companion object {
        /**
         * Converts a collection of CurrencyPairDO to a list of CurrencyPair DTOs.
         */
        fun toCurrencyPairList(currencyPairs: Collection<CurrencyPairDO>?): List<CurrencyPair>? {
            currencyPairs ?: return null
            val result = mutableListOf<CurrencyPair>()
            currencyPairs.forEach {
                val currencyPair = CurrencyPair()
                currencyPair.copyFromMinimal(it)
                result.add(currencyPair)
            }
            return result.sortedBy { it.displayName }
        }

        /**
         * Converts a collection of CurrencyPair DTOs to a set of CurrencyPairDO (with id only).
         */
        fun toCurrencyPairDOList(currencyPairs: Collection<CurrencyPair>?): MutableSet<CurrencyPairDO>? {
            currencyPairs ?: return null
            val result = mutableSetOf<CurrencyPairDO>()
            currencyPairs.forEach { cp ->
                val currencyPairDO = CurrencyPairDO()
                currencyPairDO.id = cp.id
                result.add(currencyPairDO)
            }
            return result
        }
    }
}
