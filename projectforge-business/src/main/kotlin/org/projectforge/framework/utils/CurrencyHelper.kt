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

package org.projectforge.framework.utils

import java.math.BigDecimal

object CurrencyHelper {
    /**
     * @param net If null then zero is returned.
     * @param vat
     * @return Gross amount or net if vat is null or zero.
     */
    @JvmStatic
    fun getGrossAmount(net: BigDecimal?, vat: BigDecimal?): BigDecimal {
        if (net == null) {
            return BigDecimal.ZERO
        }
        return if (NumberHelper.isZeroOrNull(vat)) {
            net
        } else {
            net.multiply(BigDecimal.ONE.add(vat))
        }
    }

    @JvmStatic
    fun multiply(val1: BigDecimal?, val2: BigDecimal?): BigDecimal {
        return if (val1 == null) {
            val2 ?: BigDecimal.ZERO
        } else if (val2 == null) {
            val1
        } else {
            val1.multiply(val2)
        }
    }
}
