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

package org.projectforge.framework.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ValueParserTest {
  @Test
  fun parseBigDecimalTest() {
    Assertions.assertNull(ValueParser.parseBigDecimal(null, listOf("###.##0,0#")))
    Assertions.assertEquals(BigDecimal.ZERO, ValueParser.parseBigDecimal("0", listOf("###.##0,0#")))
    Assertions.assertEquals(BigDecimal("-1"), ValueParser.parseBigDecimal("-1", listOf("###.##0,0#")))
    Assertions.assertEquals(BigDecimal("-1000.27"), ValueParser.parseBigDecimal("-1.000,27", listOf("###.##0,0#")))
    Assertions.assertEquals(
      BigDecimal("-1.000"),
      ValueParser.parseBigDecimal("-1.000,27", listOf("###,##0.0#")),
      "value has standard number format but pattern has german number format.",
    )
    Assertions.assertEquals(
      BigDecimal("-1.000"),
      ValueParser.parseBigDecimal("-1,000.27", listOf("###.##0,0#")),
      "value has german number format but pattern has standard number format.",
    )
  }

  @Test
  fun autoDetectGermanStyle() {
    Assertions.assertTrue(ValueParser.isGermanStyle("1.000,12"))
    Assertions.assertFalse(ValueParser.isGermanStyle("1,000.12"))
    Assertions.assertTrue(ValueParser.isGermanStyle("1.000.122"))
    Assertions.assertFalse(ValueParser.isGermanStyle("1.00"))
    Assertions.assertTrue(ValueParser.isGermanStyle("0,12"))
    Assertions.assertFalse(ValueParser.isGermanStyle("0.12"))
    Assertions.assertFalse(ValueParser.isGermanStyle("1,000,122"))
    Assertions.assertTrue(ValueParser.isGermanStyle("100,12"))
    Assertions.assertFalse(ValueParser.isGermanStyle("100,122"))
    Assertions.assertTrue(ValueParser.isGermanStyle("100.122"))
  }

  @Test
  fun autoDetectEnglishStyle() {
    Assertions.assertTrue(ValueParser.isEnglishStyle("1,000.12"))
    Assertions.assertFalse(ValueParser.isEnglishStyle("1.000,12"))
    Assertions.assertTrue(ValueParser.isEnglishStyle("1,000,122"))
    Assertions.assertFalse(ValueParser.isEnglishStyle("1,00"))
    Assertions.assertTrue(ValueParser.isEnglishStyle("0.12"))
    Assertions.assertFalse(ValueParser.isEnglishStyle("0,12"))
    Assertions.assertFalse(ValueParser.isEnglishStyle("1.000.122"))
    Assertions.assertTrue(ValueParser.isEnglishStyle("100.12"))
    Assertions.assertFalse(ValueParser.isEnglishStyle("100.122"))
    Assertions.assertTrue(ValueParser.isEnglishStyle("100,122"))
  }
}
