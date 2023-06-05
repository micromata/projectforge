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

package org.projectforge.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.framework.utils.NumberHelper.extractPhonenumber

class NumberHelperTest {
  @Test
  fun randomAlphaNumericTest() {
    Assertions.assertEquals(62, NumberHelper.ALPHA_NUMERICS_CHARSET.length)
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
      Assertions.assertTrue(
        found,
        "After generating 1,000 secure strings of length 1,000, the char '$ch' wasn't generated!"
      )
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
      Assertions.assertTrue(
        found,
        "After generating 1,000 secure strings of length 1,000, the char '$ch' wasn't generated!"
      )
    }
  }

  @Test
  fun checkRandomReducedAlphaNumericTest() {
    Assertions.assertFalse(NumberHelper.checkSecureRandomReducedAlphanumeric(null, 5))
    Assertions.assertFalse(NumberHelper.checkSecureRandomReducedAlphanumeric("", 5))
    Assertions.assertFalse(NumberHelper.checkSecureRandomReducedAlphanumeric("1234", 5))
    Assertions.assertTrue(NumberHelper.checkSecureRandomReducedAlphanumeric("12345", 5))
    Assertions.assertFalse(NumberHelper.checkSecureRandomReducedAlphanumeric("1Ildsfdsfas", 5))
    for (i in 0..100) {
      Assertions.assertTrue(
        NumberHelper.checkSecureRandomReducedAlphanumeric(
          NumberHelper.getSecureRandomReducedAlphanumeric(
            10
          ), 10
        )
      )
    }
  }

  @Test
  fun checkRandomReducedAlphaNumericWithSpecialCharsTest() {
    Assertions.assertFalse(NumberHelper.checkSecureRandomReducedAlphanumericWithSpecialChars("", 5))
    Assertions.assertFalse(NumberHelper.checkSecureRandomReducedAlphanumericWithSpecialChars("1234", 5))
    Assertions.assertFalse(NumberHelper.checkSecureRandomReducedAlphanumericWithSpecialChars("12345", 5))
    Assertions.assertTrue(NumberHelper.checkSecureRandomReducedAlphanumericWithSpecialChars("123!5", 5))
    Assertions.assertFalse(NumberHelper.checkSecureRandomReducedAlphanumericWithSpecialChars("1Ildsfdsfas", 5))
    for (i in 0..100) {
      Assertions.assertTrue(
        NumberHelper.checkSecureRandomReducedAlphanumericWithSpecialChars(
          NumberHelper.getSecureRandomReducedAlphanumericWithSpecialChars(
            10
          ), 10
        )
      )
    }
    // Use shorter strings, so the probability for missing special chars is increased.
    for (i in 0..100) {
      Assertions.assertTrue(
        NumberHelper.checkSecureRandomReducedAlphanumericWithSpecialChars(
          NumberHelper.getSecureRandomReducedAlphanumericWithSpecialChars(
            3
          ), 3
        )
      )
    }
  }

  @Test
  fun checkRandomAlphaNumericTest() {
    Assertions.assertFalse(NumberHelper.checkSecureRandomAlphanumeric(null, 5))
    Assertions.assertFalse(NumberHelper.checkSecureRandomAlphanumeric("", 5))
    Assertions.assertFalse(NumberHelper.checkSecureRandomAlphanumeric("1234", 5))
    Assertions.assertTrue(NumberHelper.checkSecureRandomAlphanumeric("12345", 5))
    Assertions.assertTrue(NumberHelper.checkSecureRandomAlphanumeric("1Ildsfdsfas", 5))
    for (i in 0..100) {
      Assertions.assertTrue(
        NumberHelper.checkSecureRandomAlphanumeric(
          NumberHelper.getSecureRandomAlphanumeric(
            10
          ), 10
        )
      )
    }
  }

  @Test
  fun checkRandomDigitsTest() {
    val digits = mutableSetOf<Char>()
    for (i in 0..1000) {
      val code = NumberHelper.getSecureRandomDigits(6)
      Assertions.assertEquals(6, code.length, "Code '$code' not of size 6.")
      code.forEach {ch ->
        Assertions.assertTrue(ch.isDigit(), "Invalid character in '$code': '$ch'")
        digits.add(ch)
      }
    }
    Assertions.assertEquals(10, digits.size)
    for (ch in '0'..'9') {
      Assertions.assertTrue(digits.contains(ch), "Digit '$ch' not found!")
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

  @Test
  fun extractPhonenumber() {
    Assertions.assertNull(extractPhonenumber(null, null))
    Assertions.assertEquals("", extractPhonenumber("", "+49"))
    Assertions.assertEquals("", extractPhonenumber("+", "+49"))
    Assertions.assertEquals("4", extractPhonenumber("+4", "+49"))
    Assertions.assertEquals("0", extractPhonenumber("+49", "+49"))
    Assertions.assertEquals("01", extractPhonenumber("+491", "+49"))
    Assertions.assertEquals("05613167930", extractPhonenumber("0561 / 316793-0", null))
    Assertions.assertEquals("00495613167930", extractPhonenumber("+49 561 / 316793-0", null))
    Assertions.assertEquals("05613167930", extractPhonenumber("+49 561 / 316793-0", "+49"))
    Assertions.assertEquals("00445613167930", extractPhonenumber("+44 561 / 316793-0", "+49"))
    Assertions.assertEquals("00445613167930", extractPhonenumber("+44 561 / 31:6793-0", "+49"))
    Assertions.assertEquals("00445613167930", extractPhonenumber("+44 561 / 31 h6793-0", "+49"))
    Assertions.assertEquals(
      "1234567890",
      extractPhonenumber("\u202D1234567890\u202C", "+49")
    ) // Apple white spaces from contacts.

    Assertions.assertEquals("007123456", extractPhonenumber("+7123456", "+49"))
    Assertions.assertEquals("007123456", extractPhonenumber("+7 123456", "+49"))
    Assertions.assertEquals("012345678", extractPhonenumber("+49 (0) 12345 - 678", "+49"))
    Assertions.assertEquals("004112345678", extractPhonenumber("+41 (0) 12345 - 678", "+49"))
  }

  @Test
  fun formatPhonenumber() {
    NumberHelper.TEST_COUNTRY_PREFIX_USAGE_IN_TESTCASES_ONLY = "+49"
    Assertions.assertEquals("12345678", NumberHelper.formatPhonenumber("12345678"))
    Assertions.assertEquals("0", NumberHelper.formatPhonenumber("0"))
    Assertions.assertEquals("00", NumberHelper.formatPhonenumber("00"))
    Assertions.assertEquals("+1", NumberHelper.formatPhonenumber("001"))
    Assertions.assertEquals("+1 12345", NumberHelper.formatPhonenumber("001 12345"))
    Assertions.assertEquals("+49 12345 6789", NumberHelper.formatPhonenumber("012345 6789"))
  }
}
