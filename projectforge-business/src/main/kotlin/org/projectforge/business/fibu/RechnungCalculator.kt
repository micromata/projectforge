/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.utils.CurrencyHelper
import org.projectforge.framework.utils.NumberHelper
import java.math.BigDecimal
import java.math.RoundingMode

object RechnungCalculator {
    private val log = org.slf4j.LoggerFactory.getLogger(RechnungCalculator::class.java)

    fun calculateNetSum(rechnung: IRechnung): BigDecimal {
        var netto = BigDecimal.ZERO
        rechnung.positionen?.forEach {
            netto = netto.add(it.netSum)
        }
        return netto
    }


    fun calculateNetSum(position: IRechnungsPosition): BigDecimal {
        return if (position.menge != null) {
            if (position.einzelNetto != null) {
                CurrencyHelper.multiply(position.menge, position.einzelNetto)
            } else {
                BigDecimal.ZERO
            }
        } else {
            (if (position.einzelNetto != null) position.einzelNetto else BigDecimal.ZERO)!!
        }
    }

    fun calculateVatAmountSum(position: IRechnungsPosition): BigDecimal {
        val netSum = position.netSum
        return if (position.vat != null) {
            CurrencyHelper.multiply(netSum, position.vat)
        } else {
            BigDecimal.ZERO
        }
    }

    /**
     * First all amounts of same VAT will be summarized (for rounding after having the sums) and then each sum per VAT will be rounded and
     * then the total sum will be returned.
     */
    fun calculateVatAmountSum(rechnung: IRechnung): BigDecimal {
        // Key is the vat percentage and value is the cumulative vat sum.
        val vatAmountSums = mutableMapOf<BigDecimal, BigDecimal>()
        rechnung.positionen?.forEach {
            var vat = it.vat
            if (!NumberHelper.isZeroOrNull(vat)) {
                vat = vat!!.stripTrailingZeros() // 19.0 -> 19 for having same vat percentage.
                val vatAmount = CurrencyHelper.multiply(it.netSum, vat)
                val vatAmountSum = vatAmountSums[vat] ?: BigDecimal.ZERO
                vatAmountSums[vat] = vatAmountSum.plus(vatAmount)
            }
        }
        var vatAmountSum = BigDecimal.ZERO
        vatAmountSums.values.forEach {
            if (!NumberHelper.isZeroOrNull(it)) {
                vatAmountSum = vatAmountSum.plus(it.setScale(2, RoundingMode.HALF_UP))
            }
        }
        return vatAmountSum
    }

    /**
     * Adds the caluclated vat amount to the net sum amount.
     */
    fun calculateGrossSum(rechnung: IRechnung): BigDecimal {
        return rechnung.netSum.plus(calculateVatAmountSum(rechnung))
    }

    fun calculateGrossSum(position: AbstractRechnungsPositionDO): BigDecimal {
        val netSum = position.netSum
        return if (position.vat != null) {
            netSum.add(CurrencyHelper.multiply(netSum, position.vat))
        } else {
            netSum
        }
    }

    fun kostZuweisungenNetSum(rechnung: AbstractRechnungDO): BigDecimal {
        var netSum = BigDecimal.ZERO
        val positionen = rechnung.abstractPositionen
        if (positionen.isNullOrEmpty())
            return netSum
        positionen.forEach {
            netSum = netSum.add(kostZuweisungenNetSum(it))
        }
        return netSum
    }

    fun kostZuweisungenNetSum(position: AbstractRechnungsPositionDO): BigDecimal {
        val zuweisungen = position.kostZuweisungen
        if (zuweisungen == null) {
            return BigDecimal.ZERO
        }
        var netSum = BigDecimal.ZERO
        zuweisungen.forEach {
            if (it.netto != null) {
                netSum = netSum.add(it.netto)
            }
        }
        return netSum
    }

    fun kostZuweisungenNetFehlbetrag(position: AbstractRechnungsPositionDO): BigDecimal {
        return position.kostZuweisungNetSum.subtract(position.netSum)
    }

    fun kostZuweisungenGrossSum(position: AbstractRechnungsPositionDO): BigDecimal {
        val zuweisungen = position.kostZuweisungen
        if (zuweisungen == null) {
            return BigDecimal.ZERO
        }
        var netSum = BigDecimal.ZERO
        zuweisungen.forEach {
            if (it.netto != null) {
                netSum = netSum.add(it.netto)
            }
        }
        return netSum
    }
}
