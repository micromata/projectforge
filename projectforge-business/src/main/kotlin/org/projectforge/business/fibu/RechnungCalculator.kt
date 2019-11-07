/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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
import java.math.BigDecimal

object RechnungCalculator {
    private val log = org.slf4j.LoggerFactory.getLogger(RechnungCalculator::class.java)

    fun calculateNetSum(rechnung: AbstractRechnungDO): BigDecimal {
        var netto = BigDecimal.ZERO
        rechnung.abstractPositionen?.forEach {
            netto = netto.add(it.netSum)
        }
        return netto
    }


    fun calculateNetSum(position: AbstractRechnungsPositionDO): BigDecimal {
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

    fun calculateVatAmountSum(position: AbstractRechnungsPositionDO): BigDecimal {
        val netSum = position.netSum
        return if (position.vat != null) {
            CurrencyHelper.multiply(netSum, position.vat)
        } else {
            BigDecimal.ZERO
        }
    }

    fun calculateVatAmountSum(rechnung: AbstractRechnungDO): BigDecimal {
        var vatAmount = BigDecimal.ZERO
        rechnung.abstractPositionen?.forEach {
            vatAmount = vatAmount.add(it.vatAmount)
        }
        return vatAmount
    }

    fun calculateGrossSum(rechnung: AbstractRechnungDO):BigDecimal {
        var brutto = BigDecimal.ZERO
        rechnung.abstractPositionen?.forEach {
            brutto = brutto.add(it.bruttoSum)
        }
        return brutto
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
