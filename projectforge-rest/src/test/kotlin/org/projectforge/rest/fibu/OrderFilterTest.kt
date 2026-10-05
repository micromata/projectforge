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
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry

/**
 * The forecast of the order book (charts and Excel) selects its orders as the list does, except for the
 * period of performance (replaced by the start date); the charts of the statistics page take their own
 * filter of business units, customers and projects. When the forecast counts as the one of the whole
 * order book follows from that.
 */
class OrderFilterTest {

    /**
     * The statistics page filters by business units, customers and projects only: whatever else a client
     * sends, or the order book's filter holds when the page takes it over, is dropped, the favorite's
     * reference is kept, and the source is left untouched.
     */
    @Test
    fun `the statistics filter keeps business units, customers and projects only`() {
        val magicFilter = MagicFilter(name = "Mine", id = 3L)
        magicFilter.searchString = "ACME"
        magicFilter.entries.add(entry(MagicFilter.PAGINATION_PAGE_SIZE).also { it.value.value = "50" })
        magicFilter.entries.add(entry("status", values = arrayOf("BEAUFTRAGT")))
        magicFilter.entries.add(entry(BusinessUnitChecklistFilter.FIELD, values = arrayOf("BU1")))
        magicFilter.entries.add(entry(CustomerChecklistFilter.FIELD, values = arrayOf("4711")))
        magicFilter.entries.add(entry(ProjectChecklistFilter.FIELD, values = arrayOf("42")))
        magicFilter.entries.add(entry(OrderEntityRest.PERIOD_OF_PERFORMANCE_FILTER, fromValue = "2026-01-01"))
        // Without a value an entry is no criterion.
        magicFilter.entries.add(entry(ProjectChecklistFilter.FIELD))

        val filter = OrderStatisticsFilterService.statisticsFilter(magicFilter)
        assertEquals(
            listOf(BusinessUnitChecklistFilter.FIELD, CustomerChecklistFilter.FIELD, ProjectChecklistFilter.FIELD),
            filter.entries.map { it.field },
        )
        assertEquals(listOf("42"), filter.entries.last().value.values?.toList())
        assertNull(filter.searchString)
        assertEquals("Mine", filter.name)
        assertEquals(3L, filter.id)
        assertEquals(7, magicFilter.entries.size) // The source is left untouched.
        assertTrue(OrderStatisticsFilterService.statisticsFilter(null).entries.isEmpty())
    }

    /**
     * Only a forecast of the whole order book includes the invoices without any order: every criterion
     * the forecast applies (search string, customer, a person, ...) makes it a filtered one.
     */
    @Test
    fun `the forecast is unfiltered only without any applied criterion`() {
        val magicFilter = MagicFilter()
        magicFilter.entries.add(entry(MagicFilter.PAGINATION_PAGE_SIZE).also { it.value.value = "50" })
        magicFilter.entries.add(entry("status", values = arrayOf("BEAUFTRAGT")))
        magicFilter.entries.add(entry(OrderEntityRest.PERIOD_OF_PERFORMANCE_FILTER, fromValue = "2026-01-01"))
        // Without a value an entry is no criterion.
        magicFilter.entries.add(entry("kunde.name"))
        assertTrue(OrderEntityRest.isUnfiltered(magicFilter))

        assertFalse(OrderEntityRest.isUnfiltered(magicFilter.clone().also { it.searchString = "ACME" }))
        assertFalse(OrderEntityRest.isUnfiltered(MagicFilter().also {
            it.entries.add(entry("kunde.name").also { entry -> entry.value.value = "ACME" })
        }))
        assertFalse(OrderEntityRest.isUnfiltered(MagicFilter().also {
            it.entries.add(entry("projectManager").also { entry -> entry.value.id = 42L })
        }))
    }

    private fun entry(
        field: String,
        values: Array<String>? = null,
        fromValue: String? = null,
        toValue: String? = null,
    ): MagicFilterEntry {
        val entry = MagicFilterEntry(field)
        entry.value.values = values
        entry.value.fromValue = fromValue
        entry.value.toValue = toValue
        return entry
    }
}
