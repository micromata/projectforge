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

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The breakdown of the contribution margin of projects by orders or tasks (Strukturelemente), free of any data
 * base access (see [ContributionMarginService.calculateBreakdown] for loading the data).
 *
 * The accounting records know only the kost2 of the project, so the values of orders and tasks are calculated:
 * revenue from the invoice positions of the order positions, costs from the time sheets × hourly rate. What
 * isn't assignable stays with the project as residual: the contribution margin of the project less the
 * calculated ones, so a project's rows always add up to its contribution margin.
 */
object ContributionMarginAllocator {
  /** An order position time sheets may be assigned to. */
  class Position(
    val id: Long,
    val orderId: Long?,
    /** The project of the order; time sheets are assigned to the positions of orders of their own project only. */
    val projectId: Long?,
    val personDays: BigDecimal? = null,
    val netSum: BigDecimal? = null,
    /** Rejected or replaced (position or order): only taken if a task has no other positions. */
    val inactive: Boolean = false,
  )

  /** A node of the breakdown below a project: an order, an order position or a task. */
  data class GroupKey(val type: ContributionMarginRowType, val id: Long)

  /**
   * The order positions the time sheets of [taskId] of [projectId] go to: those of the nearest task upwards
   * (the task itself first) with positions of orders of the project. Inactive positions only if the task has no
   * others. Empty if there are none up to the root.
   * @param parentOf The parent task, null for the root.
   */
  fun nearestPositions(
    taskId: Long?,
    projectId: Long,
    parentOf: (Long) -> Long?,
    positionsOf: (Long) -> Collection<Position>,
  ): List<Position> {
    var id = taskId
    var depth = 0
    while (id != null && depth++ < MAX_DEPTH) {
      val candidates = positionsOf(id).filter { it.projectId == projectId }
      if (candidates.isNotEmpty()) {
        return candidates.filter { !it.inactive }.ifEmpty { candidates }.sortedBy { it.id }
      }
      id = parentOf(id)
    }
    return emptyList()
  }

  /**
   * The shares of the positions a time sheet is split among: by person days if all of them have some, else by
   * net sum if all of them have some, else equally.
   */
  fun weights(positions: List<Position>): List<BigDecimal> {
    fun valid(values: List<BigDecimal?>) = values.all { it != null && it.signum() > 0 }
    val personDays = positions.map { it.personDays }
    if (valid(personDays)) {
      return personDays.map { it!! }
    }
    val netSums = positions.map { it.netSum }
    if (valid(netSums)) {
      return netSums.map { it!! }
    }
    return positions.map { BigDecimal.ONE }
  }

  /** [amount] split by [weights] (rounded to cents), the rounding difference goes to the last part. */
  fun split(amount: BigDecimal, weights: List<BigDecimal>): List<BigDecimal> {
    if (weights.size <= 1) {
      return weights.map { amount }
    }
    val total = weights.fold(BigDecimal.ZERO, BigDecimal::add)
    val parts = weights.dropLast(1).map { amount.multiply(it).divide(total, 2, RoundingMode.HALF_UP) }
    return parts + (amount - parts.fold(BigDecimal.ZERO, BigDecimal::add))
  }

  /**
   * The time sheet entry assigned to [positions] (split by [weights]), unchanged if there are none.
   */
  fun allocate(entry: ContributionMarginEntry, positions: List<Position>): List<ContributionMarginEntry> {
    if (positions.isEmpty()) {
      return listOf(entry)
    }
    val costs = split(entry.costs, weights(positions))
    return positions.mapIndexed { i, position ->
      ContributionMarginEntry(
        entry.projectId, entry.date, if (i == 0) entry.revenue else BigDecimal.ZERO, costs[i], entry.type,
        entry.kost2Id, entry.account, entry.text, orderPositionId = position.id, taskId = entry.taskId,
      )
    }
  }

  /**
   * The rows of the breakdown in tree order.
   * @param projectEntries The entries of the contribution margin of the projects ([ContributionMarginService.load]).
   * @param allocatedEntries The calculated entries (invoice positions, time sheets) with order position and task.
   * @param path The nodes of an allocated entry below its project, from the top; empty if not assignable.
   * @param label The label of a node.
   * @param sortKey The order of the nodes with the same parent.
   */
  fun breakdown(
    calculator: ContributionMarginCalculator,
    projectEntries: Collection<ContributionMarginEntry>,
    allocatedEntries: Collection<ContributionMarginEntry>,
    path: (ContributionMarginEntry) -> List<GroupKey>,
    projectInfo: (Long) -> ContributionMarginProjectInfo?,
    label: (GroupKey) -> String?,
    sortKey: (GroupKey) -> String,
  ): List<ContributionMarginBreakdownRow> {
    val projectEntriesById = projectEntries.groupBy { it.projectId }
    val allocatedById = allocatedEntries.groupBy { it.projectId }
    val projects = (projectEntriesById.keys + allocatedById.keys).map { it to projectInfo(it) }
      .sortedWith(compareBy({ it.second?.customer ?: "" }, { it.second?.project ?: "" }, { it.second?.kost ?: "" }))
    val rows = mutableListOf<ContributionMarginBreakdownRow>()
    projects.forEach { (projectId, info) ->
      val own = projectEntriesById[projectId].orEmpty()
      val paths = allocatedById[projectId].orEmpty().map { it to path(it) }
      val assigned = paths.filter { it.second.isNotEmpty() }
      val unassigned = paths.filter { it.second.isEmpty() }.map { it.first }
      // Every node with the entries of its subtree, by its path from the project on.
      val nodes = mutableMapOf<List<GroupKey>, MutableList<ContributionMarginEntry>>()
      assigned.forEach { (entry, keys) ->
        keys.indices.forEach { nodes.getOrPut(keys.subList(0, it + 1)) { mutableListOf() }.add(entry) }
      }
      val children = nodes.keys.groupBy { it.dropLast(1) }
      val residualEntries = own + assigned.map { it.first.negate() }
      val residual = calculator.series(residualEntries)
      val hasResidual = residualEntries.isNotEmpty() && !isZero(residual)
      val projectRowId = "p$projectId"
      rows += ContributionMarginBreakdownRow(
        id = projectRowId,
        parentId = null,
        type = ContributionMarginRowType.PROJECT,
        depth = 0,
        hasChildren = nodes.isNotEmpty() || hasResidual,
        projectId = projectId,
        label = info?.project,
        kost = info?.kost,
        customer = info?.customer,
        projectGroup = info?.projectGroup,
        series = calculator.series(own),
      )
      fun rowId(keys: List<GroupKey>) = projectRowId + keys.joinToString("") { "/${prefix(it.type)}${it.id}" }
      fun addChildren(parent: List<GroupKey>) {
        children[parent].orEmpty().sortedBy { sortKey(it.last()) }.forEach { keys ->
          val key = keys.last()
          rows += ContributionMarginBreakdownRow(
            id = rowId(keys),
            parentId = if (parent.isEmpty()) projectRowId else rowId(parent),
            type = key.type,
            depth = keys.size,
            hasChildren = children.containsKey(keys),
            projectId = projectId,
            orderId = keys.firstOrNull { it.type == ContributionMarginRowType.ORDER }?.id,
            orderPositionId = key.id.takeIf { key.type == ContributionMarginRowType.ORDER_POSITION },
            taskId = key.id.takeIf { key.type == ContributionMarginRowType.TASK },
            label = label(key),
            series = calculator.series(nodes[keys]!!),
          )
          addChildren(keys)
        }
      }
      addChildren(emptyList())
      if (hasResidual) {
        val invoices = calculator.series(unassigned.filter { it.type == ContributionMarginEntryType.INVOICE }).sums
        val timesheets = calculator.series(unassigned.filter { it.type == ContributionMarginEntryType.TIMESHEET }).sums
        val unassignedProfit = invoices.revenue - timesheets.costs
        rows += ContributionMarginBreakdownRow(
          id = "$projectRowId/r",
          parentId = projectRowId,
          type = ContributionMarginRowType.RESIDUAL,
          depth = 1,
          hasChildren = false,
          projectId = projectId,
          series = residual,
          residual = ContributionMarginResidual(
            invoicesWithoutAssignment = invoices.revenue,
            timesheetsWithoutAssignment = timesheets.costs,
            accountingDifference = residual.sums.profit - unassignedProfit,
          ),
        )
      }
    }
    return rows
  }

  private fun prefix(type: ContributionMarginRowType) = when (type) {
    ContributionMarginRowType.ORDER -> "o"
    ContributionMarginRowType.ORDER_POSITION -> "op"
    ContributionMarginRowType.TASK -> "t"
    ContributionMarginRowType.PROJECT -> "p"
    ContributionMarginRowType.RESIDUAL -> "r"
  }

  /** No amount at all, neither in a month (e.g. an invoice booked in another month) nor in a sum. */
  private fun isZero(series: ContributionMarginSeries): Boolean {
    val sums = series.sums
    return (series.revenue + series.costs + series.prevYear + listOf(
      sums.revenue, sums.costs, sums.prevYearProfit, sums.prevPrevYearProfit, sums.prevYearRevenue,
      sums.prevPrevYearRevenue,
    )).all { it.signum() == 0 }
  }

  /** Guard against a cyclic task tree. */
  private const val MAX_DEPTH = 100
}
