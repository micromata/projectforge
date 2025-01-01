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

package org.projectforge.framework.utils

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.TimePeriod
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.SecureRandom
import java.text.NumberFormat
import java.text.ParseException
import java.util.*

/**
 * Some helper methods ...
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object RoundUtils {
    /**
     * @return duration in rounded hours.
     */
    @JvmStatic
    @JvmOverloads
    fun round(value: Long, unit: RoundUnit = RoundUnit.INT, roundingMode: RoundingMode = RoundingMode.HALF_UP): BigDecimal {
        return round(BigDecimal(value), unit, roundingMode)
    }

    /**
     * @return duration in rounded hours.
     */
    @JvmStatic
    @JvmOverloads
    fun round(value: BigDecimal, unit: RoundUnit = RoundUnit.INT, roundingMode: RoundingMode = RoundingMode.HALF_UP): BigDecimal {
        return when (unit) {
            RoundUnit.INT -> value.setScale(0, roundingMode)
            RoundUnit.HALF -> value.multiply(BD_2).setScale(0, roundingMode).divide(BD_2, 1, roundingMode)
            RoundUnit.QUARTER -> value.multiply(BD_4).setScale(0, roundingMode).divide(BD_4, 2, roundingMode)
            RoundUnit.FIFTH -> value.multiply(BD_5).setScale(0, roundingMode).divide(BD_5, 1, roundingMode)
            RoundUnit.TENTH -> value.multiply(BigDecimal.TEN).setScale(0, roundingMode).divide(BigDecimal.TEN, 1, roundingMode)
        }
    }

    private val BD_2 = BigDecimal(2)
    private val BD_4 = BigDecimal(4)
    private val BD_5 = BigDecimal(5)

    internal val ALPHA_NUMERICS_CHARSET: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private val ALPHA_NUMERICS_CHARSET_LENGTH = ALPHA_NUMERICS_CHARSET.size

    internal val REDUCED_ALPHA_NUMERICS_CHARSET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789"
    private val REDUCED_ALPHA_NUMERICS_CHARSET_LENGTH = REDUCED_ALPHA_NUMERICS_CHARSET.length
}
