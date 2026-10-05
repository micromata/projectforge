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

package org.projectforge.rest.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.springframework.beans.factory.annotation.Autowired

/**
 * The filter and favorites endpoints of the order statistics page ([OrderStatisticsRest]): plain mappings,
 * so each has to check the order access itself, and the favorites have to keep the page's whitelist.
 */
class OrderStatisticsRestTest : AbstractTestBase() {

    @Autowired
    private lateinit var orderStatisticsRest: OrderStatisticsRest

    @Test
    fun `a user without the order right gets neither the page nor its favorites`() {
        logon(TEST_USER)
        assertThrows<AccessException> { orderStatisticsRest.getMeta(fromOrderBook = false) }
        assertThrows<AccessException> { orderStatisticsRest.getMeta(fromOrderBook = true) }
        assertThrows<AccessException> { orderStatisticsRest.createFavorite(MagicFilter(name = "x")) }
        assertThrows<AccessException> { orderStatisticsRest.selectFavorite(1) }
        assertThrows<AccessException> { orderStatisticsRest.updateFavorite(MagicFilter(id = 1)) }
        assertThrows<AccessException> { orderStatisticsRest.renameFavorite(1, "y") }
        assertThrows<AccessException> { orderStatisticsRest.deleteFavorite(1) }
    }

    @Autowired
    private lateinit var orderEntityRest: OrderEntityRest

    @Test
    fun `a user without the order right gets neither the charts nor their tables`() {
        logon(TEST_USER)
        val forecastRequest = OrderEntityRest.ForecastChartRequest(MagicFilter())
        val marginRequest = OrderEntityRest.ContributionMarginRequest(MagicFilter())
        assertThrows<AccessException> { orderEntityRest.forecastChart(forecastRequest) }
        assertThrows<AccessException> { orderEntityRest.forecastTables(forecastRequest) }
        assertThrows<AccessException> { orderEntityRest.contributionMargin(marginRequest) }
        assertThrows<AccessException> { orderEntityRest.contributionMarginDetails(marginRequest) }
    }

    @Test
    fun `a favorite keeps the statistics criteria only and survives a round trip`() {
        logon(TEST_FINANCE_USER)
        val filter = MagicFilter(name = "Statistics favorite")
        filter.entries.add(MagicFilterEntry(CustomerChecklistFilter.FIELD).also { it.value.values = arrayOf("4711") })
        filter.entries.add(MagicFilterEntry("status").also { it.value.values = arrayOf("BEAUFTRAGT") })

        val created = orderStatisticsRest.createFavorite(filter)
        val id = created.filter.id!!
        assertEquals(listOf(CustomerChecklistFilter.FIELD), created.filter.entries.map { it.field })
        assertTrue(created.filterFavorites.any { it.id == id && it.name == "Statistics favorite" })

        val meta = orderStatisticsRest.getMeta(fromOrderBook = false)
        assertEquals(id, meta.filter.id, "The created favorite is the current filter.")
        assertEquals(listOf(CustomerChecklistFilter.FIELD), meta.favorite?.entries?.map { it.field })
        assertTrue(meta.contributionMargin, "Finance may see the contribution margin.")

        orderStatisticsRest.renameFavorite(id, "Renamed")
        assertEquals("Renamed", orderStatisticsRest.getMeta(false).filter.name)

        val deleted = orderStatisticsRest.deleteFavorite(id)
        assertFalse(deleted.filterFavorites.any { it.id == id })
        assertNull(deleted.filter.id, "The current filter no longer refers to the deleted favorite.")
        assertEquals(listOf(CustomerChecklistFilter.FIELD), deleted.filter.entries.map { it.field })
    }
}
