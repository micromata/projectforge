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
 * The shift behind the previous-year comparison ([OutgoingInvoiceEntityRest.previousYearFilter]): a pure
 * transformation of a [MagicFilter], asserted without a Spring context or a database.
 */
class OutgoingInvoicePreviousYearFilterTest {
    @Test
    fun `shifts a bounded invoice-date range by exactly one year`() {
        val filter = filterWith(from = "2025-03-01", to = "2025-03-31", requested = true)
        val previous = OutgoingInvoiceEntityRest.previousYearFilter(filter)!!
        val datum = previous.entries.first { it.field == "datum" }.value
        assertEquals("2024-03-01", datum.fromValue)
        assertEquals("2024-03-31", datum.toValue)
    }

    @Test
    fun `drops the periodKind and the comparison flag on the shifted filter, and leaves the original untouched`() {
        val filter = filterWith(from = "2025-01-01", to = "2025-12-31", requested = true).also {
            it.entries.first { entry -> entry.field == "datum" }.value.periodKind = "yearToDate"
        }
        val previous = OutgoingInvoiceEntityRest.previousYearFilter(filter)!!
        assertNull(previous.entries.first { it.field == "datum" }.value.periodKind)
        assertNull(previous.extended[OutgoingInvoiceEntityRest.PREVIOUS_YEAR_COMPARISON])
        // The clone must not disturb the request the list itself runs on.
        assertEquals("2025-01-01", filter.entries.first { it.field == "datum" }.value.fromValue)
        assertEquals(true, filter.extended[OutgoingInvoiceEntityRest.PREVIOUS_YEAR_COMPARISON])
    }

    @Test
    fun `does not apply when the comparison was not asked for`() {
        assertNull(
            OutgoingInvoiceEntityRest.previousYearFilter(
                filterWith(from = "2025-03-01", to = "2025-03-31", requested = false)
            )
        )
    }

    @Test
    fun `does not apply without both bounds of the invoice-date range`() {
        assertNull(
            OutgoingInvoiceEntityRest.previousYearFilter(
                filterWith(from = "2025-03-01", to = null, requested = true)
            )
        )
        assertNull(
            OutgoingInvoiceEntityRest.previousYearFilter(
                filterWith(from = null, to = "2025-03-31", requested = true)
            )
        )
        // No invoice-date entry at all.
        val noDatum = MagicFilter().also {
            it.extended[OutgoingInvoiceEntityRest.PREVIOUS_YEAR_COMPARISON] = true
        }
        assertNull(OutgoingInvoiceEntityRest.previousYearFilter(noDatum))
    }

    private fun filterWith(from: String?, to: String?, requested: Boolean): MagicFilter {
        val filter = MagicFilter()
        filter.entries.add(MagicFilterEntry(field = "datum").also { entry ->
            entry.value.fromValue = from
            entry.value.toValue = to
        })
        if (requested) {
            filter.extended[OutgoingInvoiceEntityRest.PREVIOUS_YEAR_COMPARISON] = true
        }
        return filter
    }
}
