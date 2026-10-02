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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragFakturiertFilterStatus
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry

/**
 * The forecast of the order book (charts and Excel) selects its orders as the list does, except for the
 * period of performance (replaced by the start date) and, in the charts, the state criteria. What the
 * charts tab reports as not applied, and when the forecast counts as the one of the whole order book,
 * follows from that.
 */
class OrderFilterTest {

    /**
     * The charts tab lists which of the list's criteria it did not apply, so a total that disagrees with
     * the list is explained rather than silent: only the period of performance and the state criteria.
     */
    @Test
    fun `the forecast filter usage names only the replaced and left out criteria`() {
        val magicFilter = MagicFilter()
        magicFilter.entries.add(entry(MagicFilter.PAGINATION_PAGE_SIZE).also { it.value.value = "50" })
        magicFilter.entries.add(entry("status", values = arrayOf("BEAUFTRAGT")))
        magicFilter.entries.add(entry("positionsStatus", values = arrayOf("BEAUFTRAGT")))
        magicFilter.entries.add(entry("positionsPaymentType", values = arrayOf("FESTPREISPAKET", "TIME_AND_MATERIALS")))
        magicFilter.entries.add(entry("projectManager").also { it.value.id = 42L })
        magicFilter.entries.add(entry("kunde.name").also { it.value.value = "ACME" })
        magicFilter.entries.add(
            entry(OrderEntityRest.PERIOD_OF_PERFORMANCE_FILTER, fromValue = "2026-01-01", toValue = "2026-12-31")
        )

        val usage = OrderEntityRest.forecastFilterUsage(magicFilter)
        assertEquals(listOf("status"), usage.ignored)
        assertEquals(listOf(OrderEntityRest.PERIOD_OF_PERFORMANCE_FILTER), usage.replaced)
    }

    /**
     * The order's current state would shrink the invoiced and earlier years of the charts to the projects
     * whose orders are still in that state, so the charts leave it out; the original filter is the list's.
     */
    @Test
    fun `the forecast chart filter leaves the state criteria out`() {
        val magicFilter = MagicFilter()
        magicFilter.entries.add(entry("status", values = arrayOf("BEAUFTRAGT")))
        magicFilter.entries.add(entry("fakturiert", values = arrayOf(AuftragFakturiertFilterStatus.NICHT_FAKTURIERT.name)))
        magicFilter.entries.add(entry("kunde.name").also { it.value.value = "ACME" })

        val chartFilter = OrderEntityRest.forecastChartFilter(magicFilter)
        assertEquals(listOf("kunde.name"), chartFilter.entries.map { it.field })
        assertEquals(listOf("status", "fakturiert", "kunde.name"), magicFilter.entries.map { it.field })
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
