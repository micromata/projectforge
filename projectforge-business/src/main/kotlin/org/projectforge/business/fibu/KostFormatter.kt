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

import jakarta.annotation.PostConstruct
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.common.abbreviate
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.framework.utils.NumberHelper.splitToInts
import org.springframework.stereotype.Service

@Service
class KostFormatter(private val kostCache: KostCache) {
    /**
     * Format types for Kost2DO.
     * NUMBER - only the kost2 number (123456789),
     * FORMATTED_NUMBER (1.234.56.78)
     * TEXT - kost2 number and abbreviated text (1.234.56.78 This is the descrip...),
     * LONG - kost2 number and full text (1.234.56.78 This is the description as full version).
     */
    enum class FormatType { NUMBER, FORMATTED_NUMBER, TEXT, LONG }


    @PostConstruct
    private fun postConstruct() {
        instance = this
    }

    /**
     * "123: ACME"
     * @param kunde The kunde to format.
     * @param formatType The format type (NUMBER and FORMATTED_NUMBER are equal).
     * @param abbreviationLength The length of the abbreviation (only used for paramType=TEXT).
     * @return Id im dreistelligen Format: "001" - "999" oder "???" wenn id null ist.
     * @see [format3Digits]
     */
    @JvmOverloads
    fun formatKunde(
        kunde: KundeDO?,
        formatType: FormatType = FormatType.FORMATTED_NUMBER,
        abbreviationLength: Int = ABBREVIATION_LENGTH,
    ): String {
        return formatKunde(kunde?.id, formatType = formatType, abbreviationLength = abbreviationLength)
    }

    /**
     * kunde.id is the kunde number.
     * @param kundeId The kunde to format by id.
     * @param formatType The format type (NUMBER and FORMATTED_NUMBER are equal).
     * @param abbreviationLength The length of the abbreviation (only used for paramType=TEXT).
     * @return Id im dreistelligen Format: "001" - "999" oder "???" wenn id null ist.
     * @see [format3Digits]
     */
    @JvmOverloads
    fun formatKunde(
        kundeId: Long?,
        formatType: FormatType = FormatType.FORMATTED_NUMBER,
        abbreviationLength: Int = ABBREVIATION_LENGTH,
    ): String {
        if (formatType == FormatType.FORMATTED_NUMBER || formatType == FormatType.NUMBER) {
            return format3Digits(kundeId)
        }
        val kunde = kostCache.getCustomer(kundeId) ?: return format3Digits(kundeId)
        return abbreviateIfRequired("${format3Digits(kundeId)}: ${kunde.name}", formatType, abbreviationLength)
    }


    /**
     * "1.234.56: ACME DB project"
     * @param projekt The projekt to format.
     * @param formatType The format type (NUMBER and FORMATTED_NUMBER are equal).
     * @param abbreviationLength The length of the abbreviation (only used for paramType=TEXT).
     * @return nummernkreis + format(kunde)/projekt.getBereich() + "." + 3 stellige Projekt-Id;
     * @see StringUtils.leftPad
     */
    @JvmOverloads
    fun formatProjekt(
        projekt: ProjektDO?,
        formatType: FormatType = FormatType.FORMATTED_NUMBER,
        abbreviationLength: Int = ABBREVIATION_LENGTH,
    ): String {
        projekt ?: return if (formatType == FormatType.NUMBER) "??????" else "?.???.??"
        val delimiter = if ((formatType == FormatType.NUMBER)) "" else "."
        val sb = StringBuilder()
        sb.append(projekt.nummernkreis).append(delimiter)
        if (projekt.kunde != null) {
            sb.append(formatKunde(projekt.kunde))
        } else {
            sb.append(format3Digits(projekt.bereich))
        }
        sb.append(delimiter).append(format2Digits(projekt.nummer))
        if (formatType == FormatType.LONG || formatType == FormatType.TEXT) {
            sb.append(": ").append(projekt.name)
        }
        return abbreviateIfRequired(sb.toString(), formatType, abbreviationLength)
    }

    /**
     * Examples: "5.234.56.78: travel - ACME DB project"
     * "6.100.27.03 - {cost 2 description}
     * @param kost2 The kost2 to format.
     * @param formatType The format type (NUMBER and FORMATTED_NUMBER are equal).
     * @param abbreviationLength The length of the abbreviation (only used for paramType=TEXT).
     */
    fun formatKost2(
        kost2: Kost2DO?,
        formatType: FormatType = FormatType.FORMATTED_NUMBER,
        abbreviationLength: Int = ABBREVIATION_LENGTH,
    ): String {
        kost2 ?: return if (formatType == FormatType.NUMBER) "????????" else "?.???.??.??"
        val delimiter = if ((formatType == FormatType.NUMBER)) "" else "."
        val sb = StringBuilder()
        sb.append(kost2.nummernkreis).append(delimiter)
            .append(format3Digits(kost2.bereich)).append(delimiter)
            .append(format2Digits(kost2.teilbereich)).append(delimiter)
        if (kost2.kost2Art != null) {
            sb.append(format2Digits(kost2.kost2Art!!.id))
        } else {
            sb.append("--")
        }
        if (formatType == FormatType.LONG || formatType == FormatType.TEXT) {
            sb.append(": ")
            kost2.projekt.let { projekt ->
                if (projekt != null) {
                    kost2.kost2Art.let { kost2Art ->
                        kost2Art?.name?.let { name ->
                            sb.append(name).append(" - ")
                        }
                        sb.append(projekt.name)
                    }
                } else {
                    sb.append(kost2.description)
                }
            }
        }
        return abbreviateIfRequired(sb.toString(), formatType, abbreviationLength)
    }

    /**
     * Calls format(kost1, false)
     *
     * @param kost1
     * @see .format
     */
    @JvmOverloads
    fun formatKost1(
        kost1: Kost1DO?,
        formatType: FormatType = FormatType.FORMATTED_NUMBER,
        abbreviationLength: Int = ABBREVIATION_LENGTH,
    ): String {
        kost1 ?: return if (formatType == FormatType.NUMBER) "????????" else "?.???.??.??"
        val delimiter = if ((formatType == FormatType.NUMBER)) "" else "."
        val sb = StringBuilder()
        sb.append(kost1.nummernkreis).append(delimiter)
            .append(format3Digits(kost1.bereich)).append(delimiter)
            .append(format2Digits(kost1.teilbereich)).append(delimiter)
            .append(format2Digits(kost1.endziffer))
        if (formatType == FormatType.LONG || formatType == FormatType.TEXT) {
            sb.append(": ").append(kost1.description)
        }
        return abbreviateIfRequired(sb.toString(), formatType, abbreviationLength)
    }

    companion object {
        @JvmStatic
        lateinit var instance: KostFormatter
            private set
        const val MAX_VALUE: Int = 99999999
        const val ABBREVIATION_LENGTH = 30

        /**
         * Gibt den Kostenträger als Ganzzahl zurück. Wenn die Wertebereiche der einzelnen Parameter außerhalb des definierten
         * Bereichs liegt, wird eine UnsupportedOperationException geworfen.
         *
         * @param nummernkreis Muss zwischen 1 und 9 inklusive liegen.
         * @param bereich      Muss ziwschen 0 und 999 inklusive liegen.
         * @param teilbereich  Muss zwischen 0 und 99 inklusive liegen.
         * @param endziffer    Muss zwischen 0 und 99 inklusive liegen.
         * @return
         */
        fun getKostAsInt(nummernkreis: Int, bereich: Int, teilbereich: Int, endziffer: Int): Int {
            if (nummernkreis < 1 || nummernkreis > 9) {
                throw UnsupportedOperationException("Nummernkreis muss zwischen 1 und 9 liegen: '$nummernkreis'.")
            }
            if (bereich < 0 || bereich > 999) {
                throw UnsupportedOperationException("Bereich muss zwischen 0 und 999 liegen: '$bereich'.")
            }
            if (teilbereich < 0 || teilbereich > 99) {
                throw UnsupportedOperationException("Teilbereich muss zwischen 0 und 99 liegen: '$teilbereich'.")
            }
            if (endziffer < 0 || endziffer > 99) {
                throw UnsupportedOperationException("Endziffer muss zwischen 0 und 99 liegen: '$teilbereich'.")
            }
            val result = nummernkreis * 10000000 + bereich * 10000 + teilbereich * 100 + endziffer
            return result
        }

        /**
         * Gibt den Kostenträger als Ganzzahl zurück. Wenn die Wertebereiche der einzelnen Parameter außerhalb des definierten
         * Bereichs liegt, wird eine UnsupportedOperationException geworfen.
         *
         * @param nummernkreis Muss zwischen 1 und 9 inklusive liegen.
         * @param bereich      Muss ziwschen 0 und 999 inklusive liegen.
         * @param teilbereich  Muss zwischen 0 und 99 inklusive liegen.
         * @param endziffer    Muss zwischen 0 und 99 inklusive liegen.
         * @return
         */
        fun getKostAsInt(nummernkreis: Int, bereich: Int, teilbereich: Int, endziffer: Long): Int {
            return getKostAsInt(nummernkreis, bereich, teilbereich, endziffer.toInt())
        }

        /**
         * If not given, then ?? will be returned.
         *
         * @param number
         * @return
         * @see StringUtils.leftPad
         */
        fun format2Digits(number: Number?): String {
            return number?.toString()?.padStart(2, '0') ?: "??"
        }

        /**
         * If not given, then ??? will be returned. If given, then the number will be formatted with leading zeros.
         * @param number If null, then "???" will be returned.
         * @return 3 digits.
         */
        fun format3Digits(number: Number?): String {
            return number?.toString()?.padStart(3, '0') ?: "???"
        }

        /**
         * Uses NumberHelper.splitToInts(value, 1, 3, 2, 2)
         *
         * @param value
         * @see NumberHelper.splitToInts
         */
        fun splitKost(value: Number): IntArray {
            val result = splitToInts(value, 1, 3, 2, 2)
            if (value.toInt() > MAX_VALUE) {
                throw UnsupportedOperationException("Unsupported format of Kost (max value = " + MAX_VALUE + ": " + value)
            }
            if (result.size != 4) {
                throw UnsupportedOperationException("Unsupported format of Kost: $value")
            }
            return result
        }

        /**
         * The given text will be abbreviated if the formatType is not LON. Otherwise, the text is returned unchanged.
         */
        private fun abbreviateIfRequired(
            text: String,
            formatType: FormatType,
            abbreviationLength: Int = ABBREVIATION_LENGTH
        ): String {
            return if (formatType == FormatType.LONG) {
                text
            } else {
                text.abbreviate(abbreviationLength)
            }
        }
    }
}
