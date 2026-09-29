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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry

/**
 * The generalized year shift behind the net-sum chart ([OutgoingInvoiceEntityRest.shiftYearsFilter]): the
 * building block the four years of the chart and the previous-year comparison all rest on, asserted without
 * a Spring context or a database.
 */
class OutgoingInvoiceShiftYearsFilterTest {
    @Test
    fun `shifts the invoice-date range by the given number of years`() {
        val filter = filterWith(from = "2026-01-01", to = "2026-12-31")
        listOf(0 to "2026", 1 to "2025", 2 to "2024", 3 to "2023").forEach { (years, expectedYear) ->
            val shifted = OutgoingInvoiceEntityRest.shiftYearsFilter(filter, years)!!
            val datum = shifted.entries.first { it.field == "datum" }.value
            assertEquals("$expectedYear-01-01", datum.fromValue)
            assertEquals("$expectedYear-12-31", datum.toValue)
        }
    }

    @Test
    fun `offset zero returns the reference period unchanged and validates the bounded range`() {
        val shifted = OutgoingInvoiceEntityRest.shiftYearsFilter(
            filterWith(from = "2026-03-01", to = "2026-03-31"), 0
        )!!
        val datum = shifted.entries.first { it.field == "datum" }.value
        assertEquals("2026-03-01", datum.fromValue)
        assertEquals("2026-03-31", datum.toValue)
    }

    @Test
    fun `equals the previous-year comparison for a shift of one year`() {
        val chartFilter = filterWith(from = "2026-01-01", to = "2026-12-31")
        val comparisonFilter = filterWith(from = "2026-01-01", to = "2026-12-31").also {
            it.extended[OutgoingInvoiceEntityRest.PREVIOUS_YEAR_COMPARISON] = true
        }
        val shifted = OutgoingInvoiceEntityRest.shiftYearsFilter(chartFilter, 1)!!
            .entries.first { it.field == "datum" }.value
        val comparison = OutgoingInvoiceEntityRest.previousYearFilter(comparisonFilter)!!
            .entries.first { it.field == "datum" }.value
        assertEquals(comparison.fromValue, shifted.fromValue)
        assertEquals(comparison.toValue, shifted.toValue)
    }

    @Test
    fun `drops the periodKind and leaves the original filter untouched`() {
        val filter = filterWith(from = "2026-01-01", to = "2026-12-31").also {
            it.entries.first { entry -> entry.field == "datum" }.value.periodKind = "yearToDate"
        }
        val shifted = OutgoingInvoiceEntityRest.shiftYearsFilter(filter, 2)!!
        assertNull(shifted.entries.first { it.field == "datum" }.value.periodKind)
        assertEquals("2026-01-01", filter.entries.first { it.field == "datum" }.value.fromValue)
    }

    @Test
    fun `does not apply without both bounds of the invoice-date range`() {
        assertNull(OutgoingInvoiceEntityRest.shiftYearsFilter(filterWith("2026-03-01", null), 1))
        assertNull(OutgoingInvoiceEntityRest.shiftYearsFilter(filterWith(null, "2026-03-31"), 1))
        assertNull(OutgoingInvoiceEntityRest.shiftYearsFilter(MagicFilter(), 1))
    }

    private fun filterWith(from: String?, to: String?): MagicFilter {
        val filter = MagicFilter()
        filter.entries.add(MagicFilterEntry(field = "datum").also { entry ->
            entry.value.fromValue = from
            entry.value.toValue = to
        })
        return filter
    }
}
