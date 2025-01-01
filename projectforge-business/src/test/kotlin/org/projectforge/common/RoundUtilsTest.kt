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

package org.projectforge.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.utils.RoundUnit
import org.projectforge.framework.utils.RoundUtils
import java.math.BigDecimal
import java.math.RoundingMode

class RoundUtilsTest {
    @Test
    fun roundingTest() {
        // Please note: Also negative values are checked, see checkRounding.
        checkRounding(0, ".4999", RoundUnit.INT)
        checkRounding(1, ".5", RoundUnit.INT)
        checkRounding(1, "1.0", RoundUnit.INT)
        checkRounding(1, "1.4999", RoundUnit.INT)
        checkRounding(2, "1.5", RoundUnit.INT)


        checkRounding(0, ".24999", RoundUnit.HALF)
        checkRounding(.5, ".25", RoundUnit.HALF)
        checkRounding(.5, ".5", RoundUnit.HALF)
        checkRounding(.5, ".74999", RoundUnit.HALF)
        checkRounding(1, ".75", RoundUnit.HALF)

        checkRounding(0, ".124999", RoundUnit.QUARTER)
        checkRounding(.25,".125", RoundUnit.QUARTER)
        checkRounding(.25, ".25", RoundUnit.QUARTER)
        checkRounding(.25, ".374999", RoundUnit.QUARTER)
        checkRounding(.5, ".375", RoundUnit.QUARTER)

        checkRounding(0, ".0999", RoundUnit.FIFTH)
        checkRounding(.2, ".1", RoundUnit.FIFTH)
        checkRounding(.2, ".2", RoundUnit.FIFTH)
        checkRounding(.2, ".2999", RoundUnit.FIFTH)
        checkRounding(.4, ".3", RoundUnit.FIFTH)

        checkRounding(0, ".0499", RoundUnit.TENTH)
        checkRounding(.1, ".05", RoundUnit.TENTH)
        checkRounding(.1, ".1", RoundUnit.TENTH)
        checkRounding(.1, ".14999", RoundUnit.TENTH)
        checkRounding(.2, ".15", RoundUnit.TENTH)

    }

    private fun checkRounding(expected: Number, value: String, unit: RoundUnit) {
        assertBigDecimal(expected, RoundUtils.round(BigDecimal(value), unit, RoundingMode.HALF_UP))
        assertNegativeBigDecimal(expected, RoundUtils.round(BigDecimal(value).negate(), unit, RoundingMode.HALF_UP))
    }

    private fun assertBigDecimal(expected: Number, value: BigDecimal) {
        if (expected is Int) {
            Assertions.assertEquals(expected, value.toInt())
        } else {
            Assertions.assertEquals(expected, value.toDouble())
        }
    }

    private fun assertNegativeBigDecimal(expected: Number, value: BigDecimal) {
        if (expected is Int) {
            Assertions.assertEquals(-expected, value.toInt())
        } else {
            Assertions.assertEquals(-(expected as Double), value.toDouble())
        }
    }
}
