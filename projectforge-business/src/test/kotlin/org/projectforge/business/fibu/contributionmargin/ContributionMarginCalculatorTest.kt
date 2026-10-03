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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KontoDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungInfo
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.scripting.support.BookedInvoices
import java.math.BigDecimal
import java.time.LocalDate

class ContributionMarginCalculatorTest {
  private val isRevenueAccount = ContributionMarginCalculator.parseAccountRanges("4000-4799")

  @Test
  fun `records on revenue accounts are revenue, all others costs`() {
    val entries = ContributionMarginCalculator.recordEntries(
      listOf(
        record(KOST2_A, 4400, "1000.00", LocalDate.of(2026, 2, 10)),
        record(KOST2_A, 6300, "-400.00", LocalDate.of(2026, 2, 11)),
        record(KOST2_A, 6300, "50.00", LocalDate.of(2026, 2, 12)), // A credit reduces the costs.
        record(999L, 4400, "1000.00", LocalDate.of(2026, 2, 12)), // Kost2 of another project.
      ),
      mapOf(KOST2_A to PROJECT_A),
      { it.konto?.nummer },
      isRevenueAccount,
    )
    assertEquals(3, entries.size)
    assertEquals(listOf(4400, 6300, 6300), entries.map { it.account })
    assertTrue(entries.all { it.kost2Id == KOST2_A && it.type == ContributionMarginEntryType.RECORD })
    val data = calculator(bookingImportEnd = LocalDate.of(2026, 12, 31)).calculate(entries, ::info, null, ContributionMarginConfig())
    val project = data.projects.single()
    assertEquals(BigDecimal("1000.00"), project.revenue)
    assertEquals(BigDecimal("350.00"), project.costs)
    assertEquals(BigDecimal("650.00"), project.profit)
    assertEquals(BigDecimal("65.0"), project.percentage)
    assertEquals("2026-02", data.months[1])
    assertEquals(BigDecimal("650.00"), data.profit[1])
    assertEquals(BigDecimal("350.00"), data.costs[1])
    assertFalse(data.preliminary.any { it })
    assertFalse(data.hourlyRateMissing)
  }

  @Test
  fun `previous years are compared month by month`() {
    val entries = listOf(
      ContributionMarginEntry(PROJECT_A, LocalDate.of(2026, 3, 1), revenue = BigDecimal("100")),
      ContributionMarginEntry(PROJECT_A, LocalDate.of(2025, 3, 31), revenue = BigDecimal("80")),
      ContributionMarginEntry(PROJECT_B, LocalDate.of(2024, 3, 15), costs = BigDecimal("-30")),
      ContributionMarginEntry(PROJECT_B, LocalDate.of(2024, 4, 15), revenue = BigDecimal("120")),
      ContributionMarginEntry(PROJECT_B, LocalDate.of(2023, 12, 31), revenue = BigDecimal("1000")), // Too old.
      ContributionMarginEntry(PROJECT_B, LocalDate.of(2027, 1, 1), revenue = BigDecimal("1000")), // After the period.
    )
    val data = calculator(bookingImportEnd = LocalDate.of(2026, 12, 31)).calculate(entries, ::info, null, ContributionMarginConfig())
    assertEquals(BigDecimal("100"), data.profit[2])
    assertEquals(BigDecimal("80"), data.prevYear[2])
    assertEquals(BigDecimal("-30"), data.prevPrevYear[2])
    assertEquals(BigDecimal("120"), data.prevPrevYear[3])
    assertEquals(BigDecimal("100"), data.total.revenue)
    assertEquals(BigDecimal("80"), data.total.prevYearProfit)
    assertEquals(BigDecimal("90"), data.total.prevPrevYearProfit)
    assertEquals(BigDecimal("120"), data.total.prevPrevYearRevenue)
    assertEquals(BigDecimal("75.0"), data.total.prevPrevYearPercentage)
    assertEquals(BigDecimal("100.0"), data.total.prevYearPercentage)
    assertEquals(BigDecimal("100.0"), data.prevYearPercentage[2])
    assertEquals(BigDecimal("100.0"), data.prevYearPercentage[3]) // Cumulated: March carries on.
    assertNull(data.prevYearPercentage[1])
    // Project B has values in the pre-previous year only, but is listed nevertheless:
    assertEquals(listOf("Customer A", "Customer B"), data.projects.map { it.customer })
    assertNull(data.projects[1].percentage)
  }

  @Test
  fun `months after the booking import are preliminary`() {
    val calculator = calculator(bookingImportEnd = LocalDate.of(2026, 7, 31))
    assertEquals(LocalDate.of(2026, 8, 1), calculator.preliminaryBegin)
    val data = calculator.calculate(emptyList(), ::info, null, ContributionMarginConfig())
    assertEquals(List(7) { false } + List(5) { true }, data.preliminary)
    assertTrue(data.hourlyRateMissing)

    assertNull(calculator(bookingImportEnd = LocalDate.of(2026, 12, 31)).preliminaryBegin)
    assertEquals(LocalDate.of(2026, 1, 1), calculator(bookingImportEnd = null).preliminaryBegin)
    assertEquals(LocalDate.of(2026, 1, 1), calculator(bookingImportEnd = LocalDate.of(2025, 6, 30)).preliminaryBegin)
  }

  @Test
  fun `the calculation ends with the previous month`() {
    val today = LocalDate.of(2026, 10, 3)
    assertEquals(LocalDate.of(2026, 9, 30), ContributionMarginCalculator.calculationEnd(LocalDate.of(2026, 12, 31), today))
    assertEquals(LocalDate.of(2026, 6, 30), ContributionMarginCalculator.calculationEnd(LocalDate.of(2026, 6, 30), today))
    val calculator = calculator(bookingImportEnd = LocalDate.of(2026, 7, 31), today = today)
    assertEquals(LocalDate.of(2026, 9, 30), calculator.periodValuesEnd)
    val entries = listOf(
      ContributionMarginEntry(PROJECT_A, LocalDate.of(2026, 9, 5), revenue = BigDecimal("100")),
      ContributionMarginEntry(PROJECT_A, LocalDate.of(2025, 11, 5), revenue = BigDecimal("50")),
    )
    val data = calculator.calculate(entries, ::info, null, ContributionMarginConfig())
    assertEquals("2026-09", data.lastMonth)
    // August and September are preliminary; October and later have no values at all:
    assertEquals(List(7) { false } + List(2) { true } + List(3) { false }, data.preliminary)
    assertEquals(BigDecimal("100.0"), data.percentage[8])
    assertTrue(data.percentage.drop(9).all { it == null })
    // The previous year is complete:
    assertEquals(BigDecimal("50"), data.prevYear[10])
    // A period beginning in the current month has no values:
    val future = ContributionMarginCalculator(LocalDate.of(2026, 10, 1), null, today)
    assertNull(future.preliminaryBegin)
    assertNull(future.calculate(emptyList(), ::info, null, ContributionMarginConfig()).lastMonth)
  }

  @Test
  fun `the preliminary begin is relative to the loaded range`() {
    val from = LocalDate.of(2024, 1, 1)
    val until = LocalDate.of(2026, 12, 31)
    assertEquals(
      LocalDate.of(2026, 8, 1),
      ContributionMarginCalculator.preliminaryBegin(LocalDate.of(2026, 7, 31), from, until),
    )
    assertEquals(from, ContributionMarginCalculator.preliminaryBegin(null, from, until))
    assertEquals(from, ContributionMarginCalculator.preliminaryBegin(LocalDate.of(2023, 6, 30), from, until))
    assertNull(ContributionMarginCalculator.preliminaryBegin(until, from, until))
    assertNull(ContributionMarginCalculator.preliminaryBegin(null, from, from.minusDays(1))) // Empty range.
  }

  @Test
  fun `only unbooked and issued invoices of the preliminary months are revenue`() {
    val until = LocalDate.of(2026, 12, 31)
    val preliminaryBegin = LocalDate.of(2026, 8, 1)
    val booked = BookedInvoices(
      listOf(BuchungssatzDO().also {
        it.beleg = "17209"
        it.konto = KontoDO().also { konto -> konto.nummer = 4400 }
        it.datum = LocalDate.of(2026, 7, 31)
      }),
      isRevenueAccount,
    )
    val bookedInJuly = invoice(17209, LocalDate.of(2026, 8, 3), "500")
    val issued = invoice(17210, LocalDate.of(2026, 8, 4), "700")
    val covered = invoice(17211, LocalDate.of(2026, 7, 30), "900") // Covered by the accounting records.
    val planned = invoice(17212, LocalDate.of(2026, 9, 1), "300", RechnungStatus.GEPLANT)
    val tooLate = invoice(17213, LocalDate.of(2027, 1, 4), "200")
    val invoices = listOf(bookedInJuly, issued, covered, planned, tooLate)
    val entries = ContributionMarginCalculator.invoiceEntries(invoices, preliminaryBegin, until, booked)
    assertEquals(listOf(BigDecimal("700")), entries.map { it.revenue })
    assertEquals(ContributionMarginEntryType.INVOICE, entries.single().type)
    assertEquals("Invoice #17210", entries.single().text)

    val source = ContributionMarginSource(entries, LocalDate.of(2024, 1, 1), until, null, preliminaryBegin, null, booked)
    assertEquals(listOf(issued), invoices.filter { source.isPreliminaryRevenue(it) })
    // Without preliminary days, no invoice is preliminary:
    assertFalse(ContributionMarginCalculator.isPreliminaryRevenue(issued, null, until, booked))
  }

  @Test
  fun `the configuration is read from json`() {
    val config = ContributionMarginConfig.parse(
      """{"revenueAccounts":"4000-4799, 4900","hourlyRate":55.12,"targetPercentage":70,"redThreshold":40,
        |"kost2Assignments":[{"kost2":"6.000.10","project":"5.999.10"}],"unknown":1}""".trimMargin()
    )
    assertEquals(BigDecimal("55.12"), config.effectiveHourlyRate)
    assertEquals(70, config.targetPercentage)
    assertEquals(40, config.redThreshold)
    assertEquals("5.999.10", config.kost2Assignments.single().project)
    assertTrue(config.isRevenueAccount(4900))
    val defaults = ContributionMarginConfig.parse("{ junk")
    assertEquals(65, defaults.targetPercentage)
    assertEquals(50, defaults.redThreshold)
    assertNull(defaults.effectiveHourlyRate)
    assertTrue(defaults.isRevenueAccount(4400))
    assertNull(ContributionMarginConfig(hourlyRate = BigDecimal.ZERO).effectiveHourlyRate)
  }

  @Test
  fun `the configuration is validated`() {
    assertTrue(ContributionMarginConfig().validate { true }.isEmpty())
    val config = ContributionMarginConfig(
      revenueAccounts = "4000-4799, junk",
      hourlyRate = BigDecimal("-1"),
      targetPercentage = 60,
      redThreshold = 61,
      kost2Assignments = listOf(
        ContributionMarginConfig.Kost2Assignment("6.000.10", "5.999.10"),
        ContributionMarginConfig.Kost2Assignment("6.000", "5.999.11"),
        ContributionMarginConfig.Kost2Assignment("6.000.12", "5.999.12"),
        ContributionMarginConfig.Kost2Assignment(" ", null),
      ),
    )
    val errors = config.validate { it != listOf(5, 999, 12) }
    assertEquals(
      listOf(
        ContributionMarginConfig.REVENUE_ACCOUNTS to null,
        ContributionMarginConfig.HOURLY_RATE to null,
        ContributionMarginConfig.RED_THRESHOLD to null,
        ContributionMarginConfig.KOST2_ASSIGNMENTS to 1,
        ContributionMarginConfig.KOST2_ASSIGNMENTS to 2,
      ),
      errors.map { it.field to it.index },
    )
    assertEquals("5.999.12", errors.last().param)
    assertEquals(1, ContributionMarginConfig(targetPercentage = 101, redThreshold = 50).validate { true }.size)
  }

  @Test
  fun `time sheets are valued with the hourly rate`() {
    assertEquals(BigDecimal("-82.68"), ContributionMarginCalculator.timesheetCosts(90 * 60_000L, BigDecimal("55.12")))
  }

  @Test
  fun `configuration values are parsed`() {
    val accounts = ContributionMarginCalculator.parseAccountRanges("4000-4799, 4900;8000 - 8999")
    assertTrue(accounts(4000))
    assertTrue(accounts(4900))
    assertTrue(accounts(8500))
    assertFalse(accounts(4800))
    assertNull(ContributionMarginCalculator.accountRanges("4000-4799, junk"))
    assertNull(ContributionMarginCalculator.accountRanges("4799-4000"))
    assertNull(ContributionMarginCalculator.accountRanges(" "))
    assertFalse(ContributionMarginCalculator.parseAccountRanges("junk")(4000))
  }

  @Test
  fun `percentage needs positive revenue`() {
    assertNull(ContributionMarginCalculator.percentage(BigDecimal.ZERO, BigDecimal("-10")))
    assertEquals(BigDecimal("0.0"), ContributionMarginCalculator.percentage(BigDecimal("100"), BigDecimal("-50"))) // A loss.
  }

  @Test
  fun `the monthly percentage is cumulated, 0 for a loss and null without revenue`() {
    val entries = listOf(
      ContributionMarginEntry(PROJECT_A, LocalDate.of(2026, 1, 5), revenue = BigDecimal("200"), costs = BigDecimal("-50")),
      ContributionMarginEntry(PROJECT_A, LocalDate.of(2026, 2, 5), revenue = BigDecimal("100"), costs = BigDecimal("-150")),
      ContributionMarginEntry(PROJECT_B, LocalDate.of(2026, 3, 5), costs = BigDecimal("-10")),
    )
    val config = ContributionMarginConfig(targetPercentage = 70, redThreshold = 40)
    val data = calculator(bookingImportEnd = LocalDate.of(2026, 12, 31)).calculate(entries, ::info, null, config)
    // Jan: 150/200; Jan+Feb: 100/300; Jan-Mar: 90/300.
    assertEquals(listOf(BigDecimal("75.0"), BigDecimal("33.3"), BigDecimal("30.0")), data.percentage.take(3))
    // A loss in the first month: 0 instead of -100 %.
    val loss = listOf(ContributionMarginEntry(PROJECT_A, LocalDate.of(2026, 1, 5), revenue = BigDecimal("100"), costs = BigDecimal("-200")))
    assertEquals(BigDecimal("0.0"), calculator(bookingImportEnd = LocalDate.of(2026, 12, 31)).calculate(loss, ::info, null, config).percentage[0])
    assertNull(
      calculator(bookingImportEnd = LocalDate.of(2026, 12, 31))
        .calculate(entries.drop(2), ::info, null, config).percentage[2]
    )
    assertEquals(70, data.targetPercentage)
    assertEquals(40, data.redThreshold)
    assertNull(data.projects.single { it.projectId == PROJECT_B }.percentage)
    // Each project has its own sums:
    assertEquals(BigDecimal("300"), data.projects.single { it.projectId == PROJECT_A }.revenue)
    assertEquals(BigDecimal("10"), data.projects.single { it.projectId == PROJECT_B }.costs)
  }

  private fun calculator(bookingImportEnd: LocalDate?, today: LocalDate = LocalDate.of(2027, 2, 10)) =
    ContributionMarginCalculator(LocalDate.of(2026, 1, 15), bookingImportEnd, today)

  private fun info(projectId: Long) = when (projectId) {
    PROJECT_A -> ContributionMarginProjectInfo("5.001.01", "Customer A", "Project A")
    else -> ContributionMarginProjectInfo("5.002.01", "Customer B", "Project B")
  }

  private fun record(kost2Id: Long, account: Int, amount: String, date: LocalDate) = BuchungssatzDO().also {
    it.kost2 = Kost2DO().also { kost2 -> kost2.id = kost2Id }
    it.konto = KontoDO().also { konto -> konto.nummer = account }
    it.betrag = BigDecimal(amount)
    it.datum = date
  }

  private fun invoice(
    nummer: Int,
    date: LocalDate,
    netSum: String,
    status: RechnungStatus = RechnungStatus.GESTELLT,
  ) = RechnungDO().also {
    it.nummer = nummer
    it.typ = RechnungTyp.RECHNUNG
    it.datum = date
    it.status = status
    it.projekt = ProjektDO().also { projekt -> projekt.id = PROJECT_A }
    it.info = RechnungInfo(it).also { info -> info.netSum = BigDecimal(netSum) }
  }

  companion object {
    private const val PROJECT_A = 1L
    private const val PROJECT_B = 2L
    private const val KOST2_A = 101L
  }
}
