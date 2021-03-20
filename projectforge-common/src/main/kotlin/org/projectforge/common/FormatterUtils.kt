/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common

import org.apache.commons.lang3.StringUtils

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object FormatterUtils {
    @JvmStatic
    @JvmOverloads
    fun formatBytes(bytes: Int?, locale: Locale = Locale.getDefault()): String {
        return formatBytes(bytes?.toLong(), locale)
    }

    /**
     * null -> '--',
     * '5' -> '5bytes',
     * '1024' -> '1KB', ...
     */
    @JvmStatic
    @JvmOverloads
    fun formatBytes(bytes: Long?, locale: Locale = Locale.getDefault()): String {
        bytes ?: return "--"
        if (bytes < KILO_BYTES) {
            return "${bytes}bytes"
        }
        if (bytes < MEGA_BYTES) {
            var no = BigDecimal(bytes).divide(KB_BD, 1, RoundingMode.HALF_UP)
            if (no.toLong() >= 100) {
                no = no.setScale(0, RoundingMode.HALF_UP)
            }
            return NumberFormat.getInstance(locale).format(no) + "KB"
        }
        if (bytes < GIGA_BYTES) {
            var no = BigDecimal(bytes).divide(MB_BD, 1, RoundingMode.HALF_UP)
            if (no.toLong() >= 100) {
                no = no.setScale(0, RoundingMode.HALF_UP)
            }
            return NumberFormat.getInstance(locale).format(no) + "MB"
        }
        var no = BigDecimal(bytes).divide(GB_BD, 1, RoundingMode.HALF_UP)
        if (no.toLong() >= 100) {
            no = no.setScale(0, RoundingMode.HALF_UP)
        }
        return NumberFormat.getInstance(locale).format(no) + "GB"
    }

    private const val KILO_BYTES = 1024
    private val KB_BD = BigDecimal(KILO_BYTES)
    private const val MEGA_BYTES = KILO_BYTES * 1024
    private val MB_BD = BigDecimal(MEGA_BYTES)
    private const val GIGA_BYTES = MEGA_BYTES * 1024
    private val GB_BD = BigDecimal(GIGA_BYTES)
}
