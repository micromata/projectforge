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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class RechnungDOTest {
  @Test
  fun grossSumWithDiscountTest() {
    val invoice = RechnungDO()
    val pos = RechnungsPositionDO()
    pos.vat = BigDecimal("0.19")
    pos.einzelNetto = BigDecimal("100")
    invoice.addPosition(pos)
    Assertions.assertEquals("119.00", invoice.grossSum.toString())
    Assertions.assertEquals("119.00", invoice.grossSumWithDiscount.toString())
    invoice.discountPercent = BigDecimal("2.00")
    invoice.discountMaturity = LocalDate.now()
    Assertions.assertEquals("116.62", invoice.grossSumWithDiscount.toString())
    invoice.discountMaturity = LocalDate.now().plusDays(1)
    Assertions.assertEquals("116.62", invoice.grossSumWithDiscount.toString(), "discount not expired.")
    invoice.discountMaturity = LocalDate.now().plusDays(-1)
    Assertions.assertEquals("119.00", invoice.grossSumWithDiscount.toString(), "discount expired.")
    invoice.zahlBetrag = BigDecimal("118.00") // Paid stupid amount (for test)
    Assertions.assertEquals("118.00", invoice.grossSumWithDiscount.toString(), "amount already paid")
  }
}
