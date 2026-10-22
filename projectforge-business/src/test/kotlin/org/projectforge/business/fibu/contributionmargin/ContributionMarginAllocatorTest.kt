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
import org.projectforge.business.fibu.contributionmargin.ContributionMarginAllocator.GroupKey
import org.projectforge.business.fibu.contributionmargin.ContributionMarginAllocator.Position
import java.math.BigDecimal
import java.time.LocalDate

class ContributionMarginAllocatorTest {
  // Task tree: 1 (top) → 2 → 3, 1 → 4.
  private val parents = mapOf(2L to 1L, 3L to 2L, 4L to 1L)

  @Test
  fun `time sheets go to the positions of the nearest task upwards of their own project`() {
    val positions = mapOf(
      1L to listOf(Position(11, orderId = 1, projectId = PROJECT_A)),
      2L to listOf(Position(21, orderId = 2, projectId = PROJECT_B)), // Order of another project.
      3L to emptyList(),
    )
    fun nearest(taskId: Long?, projectId: Long = PROJECT_A) = ContributionMarginAllocator.nearestPositions(
      taskId, projectId, parents::get,
    ) { positions[it].orEmpty() }.map { it.id }
    assertEquals(listOf(11L), nearest(3), "Task 2 has only a position of another project, so task 1's.")
    assertEquals(listOf(21L), nearest(3, PROJECT_B))
    assertEquals(listOf(11L), nearest(4))
    assertEquals(emptyList<Long>(), nearest(3, projectId = 999))
    assertEquals(emptyList<Long>(), nearest(null))
  }

  @Test
  fun `rejected and replaced positions get time sheets only if their task has no others`() {
    val active = Position(1, orderId = 1, projectId = PROJECT_A)
    val replaced = Position(2, orderId = 2, projectId = PROJECT_A, inactive = true)
    fun nearest(list: List<Position>) =
      ContributionMarginAllocator.nearestPositions(3, PROJECT_A, parents::get) { if (it == 3L) list else emptyList() }
    assertEquals(listOf(active), nearest(listOf(replaced, active)))
    assertEquals(listOf(replaced), nearest(listOf(replaced)), "Not moved upwards to another order.")
  }

  @Test
  fun `a time sheet is split by person days, else by net sum, else equally`() {
    fun weights(vararg positions: Position) = ContributionMarginAllocator.weights(positions.toList())
    assertEquals(
      listOf(BigDecimal("10"), BigDecimal("30")),
      weights(position(1, personDays = "10", netSum = "1"), position(2, personDays = "30", netSum = "1")),
    )
    assertEquals(
      listOf(BigDecimal("100"), BigDecimal("300")),
      weights(position(1, personDays = "10", netSum = "100"), position(2, netSum = "300")),
    )
    assertEquals(listOf(BigDecimal.ONE, BigDecimal.ONE), weights(position(1, netSum = "100"), position(2)))
    assertEquals(
      listOf(BigDecimal("33.33"), BigDecimal("33.33"), BigDecimal("33.34")),
      ContributionMarginAllocator.split(BigDecimal("100.00"), List(3) { BigDecimal.ONE }),
      "The rounding difference goes to the last part.",
    )

    val entry = timesheet(costs = "-100.00", taskId = 3)
    val allocated = ContributionMarginAllocator.allocate(
      entry, listOf(position(1, personDays = "1"), position(2, personDays = "3")),
    )
    assertEquals(listOf(1L, 2L), allocated.map { it.orderPositionId })
    assertEquals(listOf(BigDecimal("-25.00"), BigDecimal("-75.00")), allocated.map { it.costs })
    assertTrue(allocated.all { it.taskId == 3L && it.type == ContributionMarginEntryType.TIMESHEET })
    assertEquals(listOf(entry), ContributionMarginAllocator.allocate(entry, emptyList()))
  }

  @Test
  fun `the orders and the residual add up to the contribution margin of the project`() {
    val projectEntries = listOf(
      ContributionMarginEntry(PROJECT_A, MARCH, revenue = BigDecimal("1000.00")),
      ContributionMarginEntry(PROJECT_A, MARCH, costs = BigDecimal("-300.00")),
      ContributionMarginEntry(PROJECT_A, MARCH.minusYears(1), revenue = BigDecimal("200.00")),
    )
    val allocated = listOf(
      invoice("600.00", orderPositionId = POS_1_1),
      invoice("400.00", orderPositionId = null), // Without an order position.
      timesheet("-200.00", orderPositionId = POS_1_1),
      timesheet("-100.00", orderPositionId = POS_2_1),
      timesheet("-50.00", orderPositionId = null), // On a task without order positions.
    )
    val rows = breakdown(projectEntries, allocated)
    assertEquals(
      listOf("p1", "p1/o1", "p1/o1/op11", "p1/o2", "p1/o2/op21", "p1/r"),
      rows.map { it.id },
      "Tree order: the project, its orders by number with their positions, the residual last.",
    )
    assertEquals(listOf(null, "p1", "p1/o1", "p1", "p1/o2", "p1"), rows.map { it.parentId })
    assertEquals(listOf(0, 1, 2, 1, 2, 1), rows.map { it.depth })
    val byId = rows.associateBy { it.id }
    assertEquals("Order 1", byId["p1/o1"]!!.label)
    assertEquals(1L, byId["p1/o1/op11"]!!.orderId)
    assertEquals(POS_1_1, byId["p1/o1/op11"]!!.orderPositionId)

    val project = byId["p1"]!!.series
    assertEquals(BigDecimal("700.00"), project.sums.profit)
    assertEquals(BigDecimal("400.00"), byId["p1/o1"]!!.series.sums.profit)
    assertEquals(BigDecimal("-100.00"), byId["p1/o2"]!!.series.sums.profit)
    val residual = byId["p1/r"]!!
    assertEquals(BigDecimal("400.00"), residual.series.sums.profit)
    assertEquals(BigDecimal("200.00"), residual.series.sums.prevYearProfit, "Nothing assigned the year before.")
    assertEquals(BigDecimal("400.00"), residual.residual!!.invoicesWithoutAssignment)
    assertEquals(BigDecimal("50.00"), residual.residual!!.timesheetsWithoutAssignment)
    assertEquals(BigDecimal("50.00"), residual.residual!!.accountingDifference, "400 - (400 - 50)")
    // Month by month, too.
    val orders = listOf("p1/o1", "p1/o2", "p1/r").map { byId[it]!!.series }
    project.profit.indices.forEach { i ->
      assertEquals(project.profit[i], orders.fold(BigDecimal.ZERO) { sum, series -> sum + series.profit[i] })
    }
  }

  @Test
  fun `a project fully assigned has no residual row`() {
    val rows = breakdown(
      listOf(ContributionMarginEntry(PROJECT_A, MARCH, revenue = BigDecimal("600.00"))),
      listOf(invoice("600.00", orderPositionId = POS_1_1)),
    )
    assertEquals(listOf("p1", "p1/o1", "p1/o1/op11"), rows.map { it.id })
    assertNull(rows.firstOrNull { it.type == ContributionMarginRowType.RESIDUAL })
  }

  private fun breakdown(
    projectEntries: List<ContributionMarginEntry>,
    allocated: List<ContributionMarginEntry>,
  ): List<ContributionMarginBreakdownRow> {
    val orderOf = mapOf(POS_1_1 to 1L, POS_2_1 to 2L)
    return ContributionMarginAllocator.breakdown(
      ContributionMarginCalculator(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 15)),
      projectEntries,
      allocated,
      path = { entry ->
        entry.orderPositionId?.let {
          listOf(GroupKey(ContributionMarginRowType.ORDER, orderOf[it]!!), GroupKey(ContributionMarginRowType.ORDER_POSITION, it))
        } ?: emptyList()
      },
      projectInfo = { ContributionMarginProjectInfo("5.001.01", "Customer", "Project $it") },
      label = { if (it.type == ContributionMarginRowType.ORDER) "Order ${it.id}" else "Position ${it.id}" },
      sortKey = { it.id.toString() },
    )
  }

  private fun position(id: Long, personDays: String? = null, netSum: String? = null) = Position(
    id, orderId = 1, projectId = PROJECT_A, personDays = personDays?.let(::BigDecimal), netSum = netSum?.let(::BigDecimal),
  )

  private fun invoice(revenue: String, orderPositionId: Long?) = ContributionMarginEntry(
    PROJECT_A, MARCH, revenue = BigDecimal(revenue), type = ContributionMarginEntryType.INVOICE,
    orderPositionId = orderPositionId,
  )

  private fun timesheet(costs: String, orderPositionId: Long? = null, taskId: Long? = null) = ContributionMarginEntry(
    PROJECT_A, MARCH, costs = BigDecimal(costs), type = ContributionMarginEntryType.TIMESHEET,
    orderPositionId = orderPositionId, taskId = taskId,
  )

  companion object {
    private const val PROJECT_A = 1L
    private const val PROJECT_B = 2L
    private const val POS_1_1 = 11L
    private const val POS_2_1 = 21L
    private val MARCH = LocalDate.of(2026, 3, 15)
  }
}
