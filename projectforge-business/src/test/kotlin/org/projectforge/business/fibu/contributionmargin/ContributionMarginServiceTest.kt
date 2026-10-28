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
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class ContributionMarginServiceTest : AbstractTestBase() {
  @Autowired
  private lateinit var contributionMarginService: ContributionMarginService

  @Autowired
  private lateinit var kost1Dao: Kost1Dao

  @Autowired
  private lateinit var kost2ArtDao: Kost2ArtDao

  @Autowired
  private lateinit var kost2Dao: Kost2Dao

  @Autowired
  private lateinit var projektDao: ProjektDao

  @Autowired
  private lateinit var rechnungDao: RechnungDao

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

  @Test
  fun `unbooked invoice revenue goes to the project of the cost assignment`() {
    val project = ProjektDO().also {
      it.name = "Contribution margin cost assignment test"
      it.internKost2_4 = 100
      it.nummer = 78
      projektDao.insert(it, checkAccess = false)
    }
    val kost1 = Kost1DO().also {
      it.nummernkreis = 4
      it.bereich = 100
      it.teilbereich = 2
      it.endziffer = 1
      kost1Dao.insert(it, checkAccess = false)
    }
    val kost2Art = Kost2ArtDO().also {
      it.id = 91
      it.name = "Contribution margin test"
      kost2ArtDao.insert(it, checkAccess = false)
    }
    val kost2 = Kost2DO().also {
      it.projekt = project
      it.nummernkreis = project.nummernkreis!!
      it.bereich = project.bereich!!
      it.teilbereich = project.teilbereich!!
      it.kost2Art = kost2Art
      kost2Dao.insert(it, checkAccess = false)
    }
    val date = LocalDate.now().minusMonths(1).withDayOfMonth(10)
    // No project at the invoice itself, the revenue comes from the cost assignment only.
    val invoice = RechnungDO().also { invoice ->
      invoice.nummer = rechnungDao.nextNumber
      invoice.kundeText = "Customer"
      invoice.datum = date
      invoice.status = RechnungStatus.GESTELLT
      invoice.addPosition(RechnungsPositionDO().also { pos ->
        pos.menge = BigDecimal.ONE
        pos.einzelNetto = BigDecimal("700")
        pos.addKostZuweisung(KostZuweisungDO().also { assignment ->
          assignment.kost1 = kost1
          assignment.kost2 = kost2
          assignment.netto = BigDecimal("700")
        })
      })
    }
    rechnungDao.insert(invoice, checkAccess = false)

    assertEquals(listOf(invoice.id), contributionMarginService.invoiceIdsOfKost2(listOf(kost2.id!!), date.minusDays(1), date.plusDays(1)))
    assertTrue(contributionMarginService.invoiceIdsOfKost2(listOf(kost2.id!!), date.plusDays(1), date.plusDays(2)).isEmpty())
    assertTrue(contributionMarginService.invoiceIdsOfKost2(emptyList(), date.minusDays(1), date.plusDays(1)).isEmpty())

    val source = contributionMarginService.load(listOf(project.id!!), date.withDayOfMonth(1), date.plusDays(1))
    val entries = source.entries.filter { it.type == ContributionMarginEntryType.INVOICE }
    assertEquals(listOf(BigDecimal("700")), entries.map { it.revenue.setScale(0) })
    assertEquals(project.id, entries.single().projectId)
    assertEquals(kost2.id, entries.single().kost2Id)
  }
}
