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

/** What one bubble of the contribution margin bubble chart stands for. */
enum class ContributionMarginBubbleGrouping {
  PROJECT,

  /** The project group (`fibu.customerGroups`), each project without one for itself. */
  PROJECT_GROUP,

  /** Each project without a customer for itself. */
  CUSTOMER,

  /** The customer group (`fibu.customerGroups`), else the customer, else the project. */
  CUSTOMER_GROUP,
}

/**
 * One bubble: revenue (x), contribution margin in % (y) and costs (size) of a group of projects in the period.
 *
 * A bubble of other projects (the user may not see, [own] false) is anonymous: neither [label] nor [projectCount],
 * only the sums.
 */
class ContributionMarginBubble(
  val label: String?,
  val own: Boolean,
  val revenue: BigDecimal,
  /** Positive amount. */
  val costs: BigDecimal,
  val projectCount: Int?,
  /**
   * The colour of the business unit all projects of the bubble belong to (`fibu.customerGroups`), null for
   * mixed or uncoloured units.
   */
  val color: String? = null,
  /**
   * The short names (identifier, else name) of the projects of the bubble, sorted. Null for anonymous bubbles.
   */
  val projects: List<String>? = null,
  /**
   * The short title of the bubble: the name of the group (project group, customer, customer group), for a project
   * by itself its identifier, else its name. Null for anonymous bubbles.
   */
  val title: String? = null,
) {
  // Stored, not computed getters: the REST object mapper serializes fields only.
  val profit: BigDecimal = revenue - costs

  /** See [ContributionMarginCalculator.percentage]: 0 for a loss, null without positive revenue. */
  val percentage: BigDecimal? = ContributionMarginCalculator.percentage(revenue, profit)
}

/** A business unit of the legend with its colour. */
class ContributionMarginBubbleUnit(val name: String, val color: String)

class ContributionMarginBubbles(
  /** The own bubbles first, then the others, each sorted by costs descending (big bubbles behind small ones). */
  val bubbles: List<ContributionMarginBubble>,
  val targetPercentage: Int,
  val redThreshold: Int,
  /** The business units whose colour occurs in [bubbles], for the legend. */
  val businessUnits: List<ContributionMarginBubbleUnit> = emptyList(),
)

object ContributionMarginBubbleBuilder {
  /**
   * The bubbles of [projects], grouped by [grouping]. Groups without revenue and costs in the period (amounts in
   * the previous years only) are left out.
   *
   * @param own False for the projects the user may not see: their bubbles get neither label nor project count.
   * @param customerGroupOf The name of the customer group of a project (by id), null for none.
   * @param colorOf The colour of the business unit of a project (by id), null for none. A bubble gets the colour
   *   only if all its projects have the same.
   */
  fun bubbles(
    projects: List<ContributionMarginProject>,
    own: Boolean,
    grouping: ContributionMarginBubbleGrouping,
    customerGroupOf: (Long) -> String? = { null },
    colorOf: (Long) -> String? = { null },
  ): List<ContributionMarginBubble> {
    return projects
      .groupBy { groupKey(it, grouping, customerGroupOf) }
      .map { (key, group) ->
        ContributionMarginBubble(
          label = if (own) key.label else null,
          own = own,
          revenue = group.sumOf { it.revenue },
          costs = group.sumOf { it.costs },
          projectCount = if (own) group.size else null,
          color = group.map { colorOf(it.projectId) }.distinct().singleOrNull(),
          projects = if (own) shortNames(group).ifEmpty { null } else null,
          title = if (own) key.name ?: group.single().identifier ?: key.label else null,
        )
      }
      .filter { it.revenue.signum() != 0 || it.costs.signum() != 0 }
      .sortedWith(compareByDescending<ContributionMarginBubble> { it.costs }.thenByDescending { it.revenue })
  }

  private fun shortNames(projects: List<ContributionMarginProject>): List<String> {
    return projects.mapNotNull { it.identifier ?: it.project ?: it.kost }.distinct().sortedBy { it.lowercase() }
  }

  /** A group of the given name, or a single project (by id, as names aren't unique). */
  private data class GroupKey(val name: String?, val projectId: Long?, val label: String?)

  private fun groupKey(
    project: ContributionMarginProject,
    grouping: ContributionMarginBubbleGrouping,
    customerGroupOf: (Long) -> String?,
  ): GroupKey {
    val name = when (grouping) {
      ContributionMarginBubbleGrouping.PROJECT -> null
      ContributionMarginBubbleGrouping.PROJECT_GROUP -> project.projectGroup
      ContributionMarginBubbleGrouping.CUSTOMER -> project.customer
      ContributionMarginBubbleGrouping.CUSTOMER_GROUP -> customerGroupOf(project.projectId) ?: project.customer
    }?.takeIf { it.isNotBlank() }
    if (name != null) {
      return GroupKey(name, null, name)
    }
    return GroupKey(null, project.projectId, project.project ?: project.kost)
  }
}
