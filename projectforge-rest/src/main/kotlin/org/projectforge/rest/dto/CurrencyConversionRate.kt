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

import org.projectforge.business.fibu.CurrencyConversionRateDO
import org.projectforge.business.fibu.CurrencyPairDO
import java.math.BigDecimal
import java.time.LocalDate

class CurrencyConversionRate(
    var validFrom: LocalDate? = null,
    var conversionRate: BigDecimal? = null,
    var comment: String? = null,
    var currencyPairId: Long? = null,
    /**
     * Information about the source of the exchange rate (e.g., fetched from API).
     * Not persisted, only for UI display.
     */
    var apiSourceInfo: String? = null,
) : BaseDTODisplayObject<CurrencyConversionRateDO>() {
    constructor(src: CurrencyConversionRateDO?) : this() {
        id = src?.id
        validFrom = src?.validFrom
        conversionRate = src?.conversionRate
        comment = src?.comment
        currencyPairId = src?.currencyPair?.id
    }

    fun cloneAsDO(): CurrencyConversionRateDO {
        val result = CurrencyConversionRateDO()
        result.id = id
        result.validFrom = validFrom
        result.conversionRate = conversionRate
        result.comment = comment
        currencyPairId?.let {
            result.currencyPair = CurrencyPairDO().also { it.id = currencyPairId }
        }
        return result
    }
}
