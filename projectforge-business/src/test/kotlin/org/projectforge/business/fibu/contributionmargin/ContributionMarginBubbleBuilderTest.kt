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
import java.math.BigDecimal

class ContributionMarginBubbleBuilderTest {
  private val projects = listOf(
    project(1, "ACME", "ALPHA", "1000", "200", projectGroup = "ACME Kleinprojekte"),
    project(2, "ACME", "BETA", "500", "300", projectGroup = "ACME Kleinprojekte"),
    project(3, "ACME", "Gamma", "800", "400"),
    project(4, "Globex", "Delta", "0", "100"),
    project(5, null, "Intern", "0", "0"), // Amounts in the previous years only.
  )

  private val customerGroups = mapOf(1L to "ACME Group", 2L to "ACME Group")

  @Test
  fun `one bubble per project`() {
    val bubbles = bubbles(ContributionMarginBubbleGrouping.PROJECT)
    assertEquals(listOf("Gamma", "BETA", "ALPHA", "Delta"), bubbles.map { it.label }, "Sorted by costs, empty left out.")
    val alpha = bubbles.single { it.label == "ALPHA" }
    assertEquals(BigDecimal("800"), alpha.profit)
    assertEquals(BigDecimal("80.0"), alpha.percentage)
    assertNull(bubbles.single { it.label == "Delta" }.percentage, "No revenue.")
  }

  @Test
  fun `projects without a group get a bubble of their own`() {
    val bubbles = bubbles(ContributionMarginBubbleGrouping.PROJECT_GROUP)
    assertEquals(setOf("ACME Kleinprojekte", "Gamma", "Delta"), bubbles.map { it.label }.toSet())
    val group = bubbles.single { it.label == "ACME Kleinprojekte" }
    assertEquals(BigDecimal("1500"), group.revenue)
    assertEquals(BigDecimal("500"), group.costs)
    assertEquals(BigDecimal("66.7"), group.percentage)
    assertEquals(2, group.projectCount)
  }

  @Test
  fun `by customer and customer group, the customer is the fallback of the customer group`() {
    assertEquals(
      setOf("ACME" to 3, "Globex" to 1),
      bubbles(ContributionMarginBubbleGrouping.CUSTOMER).map { it.label to it.projectCount }.toSet(),
    )
    assertEquals(
      setOf("ACME Group" to 2, "ACME" to 1, "Globex" to 1),
      bubbles(ContributionMarginBubbleGrouping.CUSTOMER_GROUP).map { it.label to it.projectCount }.toSet(),
    )
  }

  @Test
  fun `bubbles of other projects are anonymous`() {
    ContributionMarginBubbleGrouping.entries.forEach { grouping ->
      val bubbles = ContributionMarginBubbleBuilder.bubbles(projects, false, grouping, { customerGroups[it] })
      assertTrue(bubbles.isNotEmpty())
      assertTrue(bubbles.all { !it.own && it.label == null && it.projectCount == null }, "$grouping")
    }
    // Grouped as the own ones, only without label.
    assertEquals(
      listOf(BigDecimal("1500"), BigDecimal("800"), BigDecimal("0")),
      ContributionMarginBubbleBuilder.bubbles(projects, false, ContributionMarginBubbleGrouping.PROJECT_GROUP)
        .map { it.revenue },
    )
  }

  @Test
  fun `a bubble gets the colour of the business unit all its projects share`() {
    val colors = mapOf(1L to "#3366cc", 2L to "#3366cc", 3L to "#009ba3", 4L to "#009ba3")
    val byCustomer = ContributionMarginBubbleBuilder.bubbles(
      projects, true, ContributionMarginBubbleGrouping.CUSTOMER, colorOf = { colors[it] },
    )
    assertNull(byCustomer.single { it.label == "ACME" }.color, "Mixed business units.")
    assertEquals("#009ba3", byCustomer.single { it.label == "Globex" }.color)
    val byGroup = ContributionMarginBubbleBuilder.bubbles(
      projects, false, ContributionMarginBubbleGrouping.PROJECT_GROUP, colorOf = { colors[it] },
    )
    val group = byGroup.single { it.revenue == BigDecimal("1500") }
    assertEquals("#3366cc", group.color, "Anonymous bubbles keep the colour.")
    assertNull(group.label)
  }

  @Test
  fun `a bubble names its projects by their short name and has a short title`() {
    val withIdentifier = projects.map {
      if (it.projectId == 1L) project(1, "ACME", "ALPHA", "1000", "200", "ACME Kleinprojekte", identifier = "ALP") else it
    }
    val bubbles = ContributionMarginBubbleBuilder.bubbles(withIdentifier, true, ContributionMarginBubbleGrouping.PROJECT_GROUP)
    assertEquals(listOf("ALP", "BETA"), bubbles.single { it.label == "ACME Kleinprojekte" }.projects)
    assertEquals(listOf("Gamma"), bubbles.single { it.label == "Gamma" }.projects, "Without identifier: the name.")
    val byProject = ContributionMarginBubbleBuilder.bubbles(withIdentifier, true, ContributionMarginBubbleGrouping.PROJECT)
    assertEquals(listOf("ALP"), byProject.single { it.label == "ALPHA" }.projects)
    assertEquals("ALP", byProject.single { it.label == "ALPHA" }.title, "A project by itself: its identifier.")
    assertEquals("Gamma", byProject.single { it.label == "Gamma" }.title, "Without identifier: its name.")
    assertEquals("ACME Kleinprojekte", bubbles.single { it.label == "ACME Kleinprojekte" }.title, "A group: its name.")
    val byCustomer = ContributionMarginBubbleBuilder.bubbles(withIdentifier, true, ContributionMarginBubbleGrouping.CUSTOMER)
    assertEquals("ACME", byCustomer.single { it.label == "ACME" }.title, "A customer: its name, not its projects.")
    assertTrue(
      ContributionMarginBubbleBuilder.bubbles(withIdentifier, false, ContributionMarginBubbleGrouping.CUSTOMER)
        .all { it.projects == null && it.title == null },
      "Anonymous.",
    )
  }

  private fun bubbles(grouping: ContributionMarginBubbleGrouping) =
    ContributionMarginBubbleBuilder.bubbles(projects, true, grouping, { customerGroups[it] })
      .also { list -> assertTrue(list.all { it.own }) }

  private fun project(
    id: Long,
    customer: String?,
    name: String,
    revenue: String,
    costs: String,
    projectGroup: String? = null,
    identifier: String? = null,
  ) = ContributionMarginProject(
    projectId = id,
    kost = "5.000.0$id",
    customer = customer,
    project = name,
    revenue = BigDecimal(revenue),
    costs = BigDecimal(costs),
    prevYearProfit = BigDecimal.ONE,
    prevPrevYearProfit = BigDecimal.ZERO,
    prevYearRevenue = BigDecimal.ONE,
    prevPrevYearRevenue = BigDecimal.ZERO,
    projectGroup = projectGroup,
    identifier = identifier,
  )
}
