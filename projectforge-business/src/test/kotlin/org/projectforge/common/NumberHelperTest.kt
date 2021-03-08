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

package org.projectforge.web

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.utils.NumberHelper

class NumberHelperTest {
    @Test
    fun randomAlphaNumericTest() {
        Assertions.assertEquals(62, NumberHelper.ALPHA_NUMERICS_CHARSET.size)
        var str = NumberHelper.getSecureRandomAlphanumeric(1000)
        Assertions.assertEquals(1000, str.length)
        for (ch in NumberHelper.ALPHA_NUMERICS_CHARSET) {
            if (str.any { it == ch }) {
                // found
                continue
            }
            var found = false
            for (i in 0..1000) {
                str = NumberHelper.getSecureRandomAlphanumeric(1000)
                if (str.contains(ch)) {
                    found = true
                    break
                }
            }
            Assertions.assertTrue(found, "After generating 1,000 secure strings of length 1,000, the char '$ch' wasn't generated!")
        }
    }

    @Test
    fun randomReducedAlphaNumericTest() {
        Assertions.assertEquals(58, NumberHelper.REDUCED_ALPHA_NUMERICS_CHARSET.length)
        var str = NumberHelper.getSecureRandomReducedAlphanumeric(1000)
        Assertions.assertEquals(1000, str.length)
        for (ch in NumberHelper.REDUCED_ALPHA_NUMERICS_CHARSET) {
            if (str.any { it == ch }) {
                // found
                continue
            }
            var found = false
            for (i in 0..1000) {
                str = NumberHelper.getSecureRandomReducedAlphanumeric(1000)
                if (str.contains(ch)) {
                    found = true
                    break
                }
            }
            Assertions.assertTrue(found, "After generating 1,000 secure strings of length 1,000, the char '$ch' wasn't generated!")
        }
    }

    fun randomAlphaNumericPerformanceTest() {
        val length = 1000
        val time = System.currentTimeMillis()
        for (i in 0..1000) {
            NumberHelper.getSecureRandomAlphanumeric(length)
        }
        println("Generating 1,000 secure strings each of length $length takes ${System.currentTimeMillis() - time}ms.")
    }

    @Test
    fun rangeTest() {
        Assertions.assertNull(NumberHelper.ensureRange(0, 4, null))
        Assertions.assertEquals(2, NumberHelper.ensureRange(0, 4, 2))
        Assertions.assertEquals(0, NumberHelper.ensureRange(0, 4, 0))
        Assertions.assertEquals(0, NumberHelper.ensureRange(0, 4, -1))
        Assertions.assertEquals(4, NumberHelper.ensureRange(0, 4, 5))
        Assertions.assertEquals(4, NumberHelper.ensureRange(0, 4, 4))
    }
}
