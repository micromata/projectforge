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

package org.projectforge.business.fibu.contributionmargin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class ContributionMarginServiceTest : AbstractTestBase() {
  @Autowired
  private lateinit var contributionMarginService: ContributionMarginService

  @Test
  fun `a period in the future is empty, not an invalid time period`() {
    // The end is capped at today, so it's before the begin of a future period (e.g. the next fiscal year).
    val from = LocalDate.now().plusYears(1).withDayOfMonth(1)
    listOf(null, listOf(1L)).forEach { projectIds ->
      val source = contributionMarginService.load(projectIds, from, from.plusYears(1))
      assertTrue(source.entries.isEmpty())
      assertNull(source.preliminaryBegin)
    }
  }

  @Test
  fun `the breakdown of a past period runs its queries`() {
    // The accounting records, invoices and time sheets of three years are queried (the test data base has none).
    val startDate = LocalDate.now().minusYears(1).withDayOfMonth(1)
    ContributionMarginLevel.entries.forEach { level ->
      val breakdown = contributionMarginService.calculateBreakdown(listOf(1L), startDate, level)
      assertEquals(level, breakdown.level)
      assertEquals(12, breakdown.months.size)
    }
  }
}
