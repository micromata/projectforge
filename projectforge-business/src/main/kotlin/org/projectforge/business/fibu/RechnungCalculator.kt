/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.CurrencyHelper
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.framework.utils.NumberHelper.HUNDRED
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private val log = KotlinLogging.logger {}

object RechnungCalculator {
    /**
     * Calculates the net sum, vat amount sum and gross sum of the given invoice.
     * @oaram rechnung The invoice to calculate the values for (positions and positions.kostZuweisungen will be fetched).
     * @return The calculated values.
     */
    fun calculate(rechnung: AbstractRechnungDO): RechnungInfo {
        val info = RechnungInfo(rechnung)
        rechnung.info = info
        info.faelligkeitOrDiscountMaturity = rechnung.discountMaturity.let {
            if (it != null && !info.isBezahlt && !it.isBefore(LocalDate.now())) {
                rechnung.discountMaturity
            } else {
                rechnung.faelligkeit
            }
        }
        info.positions = rechnung.positionen?.map { calculate(it as AbstractRechnungsPositionDO) }
        info.positions?.forEach { posInfo ->
            info.netSum += posInfo.netSum
            info.kostZuweisungenNetSum += posInfo.kostZuweisungNetSum
            info.grossSum += posInfo.grossSum
        }
        info.vatAmount = calculateVatAmountSum(rechnung)
        info.kostZuweisungenFehlbetrag = info.netSum - info.kostZuweisungenNetSum
        info.grossSumWithDiscount = calculateGrossSumWithDiscount(rechnung)

        val zahlBetrag = rechnung.zahlBetrag
        info.isBezahlt = if (info.netSum.compareTo(BigDecimal.ZERO) == 0) {
            true
        } else if (rechnung.bezahlDatum != null && zahlBetrag != null && zahlBetrag > BigDecimal.ZERO) {
            if (rechnung is RechnungDO) {
                rechnung.status == RechnungStatus.BEZAHLT
            } else {
                true
            }
        } else {
            false
        }
        info.isUeberfaellig = false
        if (!info.isBezahlt && rechnung.faelligkeit?.isBefore(PFDay.today().localDate) == true) {
            info.isUeberfaellig = true
        }
        return info
    }

    /**
     * Calculations for invoice positions.
     */
    fun calculate(position: AbstractRechnungsPositionDO): RechnungPosInfo {
        val posInfo = RechnungPosInfo(position)
        position.info = posInfo
        posInfo.netSum = calculateNetSum(position)
        if (position.vat != null) {
            posInfo.vatAmount = CurrencyHelper.multiply(posInfo.netSum, position.vat)
        }
        posInfo.grossSum = posInfo.netSum
        if (position.vat != null) {
            posInfo.grossSum += CurrencyHelper.multiply(posInfo.netSum, position.vat)
        }
        var kostZuweisungNetSum = BigDecimal.ZERO
        position.kostZuweisungen?.forEach { zuweisung ->
            zuweisung.netto?.let {
                kostZuweisungNetSum += it
            }
        }
        posInfo.kostZuweisungNetSum = kostZuweisungNetSum
        val vat = position.vat
        posInfo.kostZuweisungGrossSum = kostZuweisungNetSum
        if (vat != null) {
            posInfo.kostZuweisungGrossSum += CurrencyHelper.multiply(kostZuweisungNetSum, position.vat)
        }
        posInfo.kostZuweisungNetFehlbetrag = posInfo.netSum - kostZuweisungNetSum
        return posInfo
    }

    private fun calculateNetSum(position: IRechnungsPosition): BigDecimal {
        val einzelNetto = position.einzelNetto ?: return BigDecimal.ZERO
        val menge = position.menge ?: return einzelNetto
        return CurrencyHelper.multiply(menge, einzelNetto)
    }

    /**
     * First all amounts of same VAT will be summarized (for rounding after having the sums) and then each sum per VAT will be rounded and
     * then the total sum will be returned.
     */
    private fun calculateVatAmountSum(rechnung: AbstractRechnungDO): BigDecimal {
        // Key is the vat percentage and value is the cumulative vat sum.
        val vatAmountSums = mutableMapOf<BigDecimal, BigDecimal>()
        rechnung.positionen?.forEach {
            it as AbstractRechnungsPositionDO
            var vat = it.vat
            if (!NumberHelper.isZeroOrNull(vat)) {
                vat = vat!!.stripTrailingZeros() // 19.0 -> 19 for having same vat percentage.
                val vatAmount = CurrencyHelper.multiply(it.info.netSum, vat)
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
     * @param grossSum As paramter to avoid recalculation (and lazy fetching of positions).
     */
    private fun calculateGrossSumWithDiscount(rechnung: AbstractRechnungDO): BigDecimal {
        rechnung.zahlBetrag?.let { return it }
        val grossSum = rechnung.info.grossSum
        rechnung.discountPercent?.let { percent ->
            if (percent.compareTo(BigDecimal.ZERO) != 0) {
                rechnung.discountMaturity?.let { expireDate ->
                    if (expireDate >= LocalDate.now()) {
                        return grossSum.multiply(
                            (HUNDRED - percent).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                        ).setScale(2, RoundingMode.HALF_UP)
                    }
                }
            }
        }
        return grossSum
    }

    internal lateinit var rechnungCache: RechnungCache
}
