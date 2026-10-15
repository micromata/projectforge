/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.scripting.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.framework.time.PFDay
import java.math.BigDecimal

class AccountingRecordTest {
  @Test
  fun `percentage is 0 for a loss and without revenue`() {
    assertEquals(BigDecimal("0.65"), record("1000", "-350").percentage)
    assertEquals(BigDecimal.ZERO, record("100", "-150").percentage)
    assertEquals(BigDecimal.ZERO, record("0", "-150").percentage)
  }

  private fun record(revenue: String, costs: String) = AccountingRecord(
    date = PFDay.of(2026, 1, 1),
    businessUnit = null,
    customer = null,
    customerGroup = "",
    project = "Project",
    projectId = 1L,
    kost2String = "5.001.01.00",
    revenue = BigDecimal(revenue),
    costs = BigDecimal(costs),
  )
}
