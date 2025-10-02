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

package org.projectforge.business.fibu

import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.CurrencyHelper
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.framework.utils.NumberHelper.HUNDRED
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.reflect.KMutableProperty1

// private val log = KotlinLogging.logger {}

object RechnungCalculator {
    /**
     * If true, the net sum of each position will be rounded before summing up the net sums. This is compliant to the German law.
     */
    private const val roundPositionsBeforeSum = true

    /**
     * Calculates the net sum, vat amount sum and gross sum of the given invoice.
     * @param rechnung The invoice to calculate the values for (positions and positions.kostZuweisungen will be fetched).
     * @return The calculated values.
     */
    @JvmOverloads
    fun calculate(rechnung: AbstractRechnungDO, useCaches: Boolean = true): RechnungInfo {
        val info = RechnungInfo(rechnung)
        if (rechnung.deleted) {
            return info
        }
        rechnung.info = info
        info.faelligkeitOrDiscountMaturity = info.discountMaturity.let {
            if (it != null && !info.isBezahlt && !it.isBefore(LocalDate.now())) {
                info.discountMaturity
            } else {
                info.faelligkeit
            }
        }
        val posInfoList = mutableListOf<RechnungPosInfo>()
        rechnung.positionen?.forEach { pos ->
            if (pos.deleted) {
                return@forEach
            }
            var posInfo = if (useCaches) {
                rechnungCache.getRechnungPosInfo(pos.id)
            } else null
            if (posInfo == null) {
                posInfo = RechnungPosInfo(info, pos as AbstractRechnungsPositionDO)
            }
            calculate(posInfo, pos as AbstractRechnungsPositionDO)
            posInfoList.add(posInfo)
        }
        info.positions = posInfoList
        info.positions?.forEach { posInfo ->
            info.netSum += posInfo.netSum
            info.kostZuweisungenNetSum += posInfo.kostZuweisungNetSum
            info.grossSum += posInfo.grossSum
        }
        roundAmountIf(info, RechnungInfo::netSum)
        roundAmountIf(info, RechnungInfo::kostZuweisungenNetSum)
        roundAmountIf(info, RechnungInfo::grossSum)
        info.vatAmount = calculateVatAmountSum(rechnung)
        info.kostZuweisungenFehlbetrag = info.netSum - info.kostZuweisungenNetSum
        info.grossSumWithDiscount = calculateGrossSumWithDiscount(rechnung)

        val zahlBetrag = info.zahlBetrag
        info.isBezahlt = if (info.netSum.compareTo(BigDecimal.ZERO) == 0) {
            true
        } else if (info.bezahlDatum != null && zahlBetrag != null && zahlBetrag.compareTo(BigDecimal.ZERO) != 0) {
            if (rechnung is RechnungDO) {
                info.status == RechnungStatus.BEZAHLT
            } else {
                true
            }
        } else {
            false
        }
        info.isUeberfaellig = false
        if (!info.isBezahlt && info.faelligkeit?.isBefore(PFDay.today().localDate) == true) {
            info.isUeberfaellig = true
        }
        return info
    }

    /**
     * Calculations for invoice positions.
     */
    internal fun calculate(posInfo: RechnungPosInfo, position: AbstractRechnungsPositionDO): RechnungPosInfo {
        position.info = posInfo
        if (position is RechnungsPositionDO) {
            val orderPosInfo = auftragsCache.getOrderPositionInfo(position.auftragsPosition?.id)
            if (orderPosInfo != null) { // auftragsPosition is null, if auftragsPosition is deleted.
                position.auftragsPosition?.id.let { auftragsPositionId ->
                    posInfo.auftragsPositionId = auftragsPositionId
                    posInfo.auftragsId = orderPosInfo.auftragId
                    posInfo.auftragsPositionNummer = orderPosInfo.number
                }
            }
        }
        posInfo.netSum = calculateNetSum(position)
        roundAmountIf(posInfo, RechnungPosInfo::netSum, roundPositionsBeforeSum)
        if (position.vat != null) {
            posInfo.vatAmount = CurrencyHelper.multiply(posInfo.netSum, position.vat, roundPositionsBeforeSum)
        }
        posInfo.grossSum = calculateGrossSum(position, posInfo.netSum)
        posInfo.kostZuweisungNetSum = BigDecimal.ZERO
        position.kostZuweisungen?.forEach { zuweisung ->
            zuweisung.netto?.let {
                posInfo.kostZuweisungNetSum += roundAmountIf(it, roundPositionsBeforeSum)
            }
        }
        posInfo.kostZuweisungen = position.kostZuweisungen?.map { KostZuweisungInfo(it) }
        roundAmountIf(posInfo, RechnungPosInfo::kostZuweisungNetSum, roundPositionsBeforeSum)
        val vat = position.vat
        posInfo.kostZuweisungGrossSum = posInfo.kostZuweisungNetSum
        if (vat != null) {
            posInfo.kostZuweisungGrossSum += CurrencyHelper.multiply(posInfo.kostZuweisungNetSum, position.vat)
        }
        roundAmountIf(posInfo, RechnungPosInfo::kostZuweisungGrossSum, roundPositionsBeforeSum)
        val netSum = roundAmountIf(posInfo.netSum, roundPositionsBeforeSum)
        posInfo.kostZuweisungNetFehlbetrag = (netSum - posInfo.kostZuweisungNetSum).negate()
        return posInfo
    }

    fun calculateGrossSum(position: IRechnungsPosition, netSum: BigDecimal? = null): BigDecimal {
        val net = netSum ?: calculateNetSum(position)
        val vat = position.vat
        if (vat == null || NumberHelper.isZeroOrNull(vat)) {
            return net
        }
        val vatAmount = CurrencyHelper.multiply(net, vat, roundPositionsBeforeSum)
        return roundAmountIf(net.plus(vatAmount), roundPositionsBeforeSum)
    }

    private fun calculateNetSum(position: IRechnungsPosition): BigDecimal {
        val einzelNetto = position.einzelNetto ?: return BigDecimal.ZERO
        val menge = position.menge ?: return einzelNetto
        return CurrencyHelper.multiply(menge, einzelNetto, roundPositionsBeforeSum)
    }

    private fun roundAmountIf(amount: BigDecimal, round: Boolean = true): BigDecimal {
        if (!round) {
            return amount
        }
        return amount.setScale(2, RoundingMode.HALF_UP)
    }

    private fun <T> roundAmountIf(obj: T, property: KMutableProperty1<T, BigDecimal>, round: Boolean = true) {
        if (round == false) {
            return
        }
        val value = property.get(obj)
        if (value.scale() != 2) {
            property.set(obj, roundAmountIf(value))
        }
    }

    /**
     * First all amounts of same VAT will be summarized (for rounding after having the sums) and then each sum per VAT will be rounded and
     * then the total sum will be returned.
     */
    private fun calculateVatAmountSum(rechnung: AbstractRechnungDO): BigDecimal {
        // Key is the vat percentage and value is the cumulative vat sum.
        val vatAmountSums = mutableMapOf<BigDecimal, BigDecimal>()
        rechnung.positionen?.forEach { pos ->
            if (pos.deleted) {
                return@forEach // Skip deleted positions
            }
            pos as AbstractRechnungsPositionDO
            var vat = pos.vat
            if (!NumberHelper.isZeroOrNull(vat)) {
                vat = vat!!.stripTrailingZeros() // 19.0 -> 19 for having same vat percentage.
                val vatAmount = CurrencyHelper.multiply(pos.info.netSum, vat)
                val vatAmountSum = vatAmountSums[vat] ?: BigDecimal.ZERO
                vatAmountSums[vat] = vatAmountSum.plus(vatAmount)
            }
        }
        var vatAmountSum = BigDecimal.ZERO
        vatAmountSums.values.forEach {
            if (!NumberHelper.isZeroOrNull(it)) {
                vatAmountSum = vatAmountSum.plus(roundAmountIf(it))
            }
        }
        return roundAmountIf(vatAmountSum)
    }

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
    internal lateinit var auftragsCache: AuftragsCache
}
