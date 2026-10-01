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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragFakturiertFilterStatus
import org.projectforge.business.fibu.AuftragsPositionsArt
import org.projectforge.business.fibu.AuftragsPositionsPaymentType
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import java.time.LocalDate

/**
 * The two exports of the order book act on the filter the list is showing, and they take the legacy
 * [org.projectforge.business.fibu.AuftragFilter] rather than a [MagicFilter] — so a field the translation
 * silently drops means an export over more orders than the list shows, without anything failing.
 */
class OrderFilterTest {

    @Test
    fun `every criterion of the list filter reaches the legacy filter`() {
        val magicFilter = MagicFilter()
        magicFilter.searchString = "Micromata"
        magicFilter.entries.add(entry("status", values = arrayOf("BEAUFTRAGT", "LOI")))
        magicFilter.entries.add(entry("positionsArt", values = arrayOf("WARTUNG")))
        magicFilter.entries.add(entry("positionsPaymentType", values = arrayOf("TIME_AND_MATERIALS")))
        magicFilter.entries.add(entry("fakturiert", values = arrayOf("ZU_FAKTURIEREN")))
        magicFilter.entries.add(
            entry(
                OrderEntityRest.PERIOD_OF_PERFORMANCE_FILTER, fromValue = "2026-01-01", toValue = "2026-12-31"
            )
        )
        magicFilter.entries.add(entry("erfassungsDatum", fromValue = "2025-07-01", toValue = "2025-09-30"))

        val filter = OrderEntityRest.toAuftragFilter(magicFilter)

        assertEquals("Micromata", filter.searchString)
        assertEquals(setOf(AuftragsStatus.BEAUFTRAGT, AuftragsStatus.LOI), filter.auftragsStatuses.toSet())
        assertEquals(setOf(AuftragsPositionsArt.WARTUNG), filter.auftragsPositionsArten.toSet())
        assertEquals(AuftragsPositionsPaymentType.TIME_AND_MATERIALS, filter.auftragsPositionsPaymentType)
        assertEquals(AuftragFakturiertFilterStatus.ZU_FAKTURIEREN, filter.auftragFakturiertFilterStatus)
        assertEquals(LocalDate.of(2026, 1, 1), filter.periodOfPerformanceStartDate)
        assertEquals(LocalDate.of(2026, 12, 31), filter.periodOfPerformanceEndDate)
        // AuftragFilter calls the entry date range startDate/endDate.
        assertEquals(LocalDate.of(2025, 7, 1), filter.startDate)
        assertEquals(LocalDate.of(2025, 9, 30), filter.endDate)
    }

    @Test
    fun `to be invoiced in following months reaches the legacy filter`() {
        val magicFilter = MagicFilter()
        magicFilter.entries.add(entry("fakturiert", values = arrayOf("ZU_FAKTURIEREN_FOLGEMONATE")))

        val filter = OrderEntityRest.toAuftragFilter(magicFilter)
        assertEquals(AuftragFakturiertFilterStatus.ZU_FAKTURIEREN_FOLGEMONATE, filter.auftragFakturiertFilterStatus)
    }

    /**
     * Every value of the filter panel is a string from the client, so an unknown one must be dropped
     * rather than throwing: a stored favorite of an enum value that has since been renamed would
     * otherwise make the list page fail instead of the filter entry being ignored.
     */
    @Test
    fun `an unknown enum value is dropped, and an empty filter stays empty`() {
        val magicFilter = MagicFilter()
        magicFilter.entries.add(entry("status", values = arrayOf("BEAUFTRAGT", "", "NO_SUCH_STATUS")))

        val filter = OrderEntityRest.toAuftragFilter(magicFilter)
        assertEquals(setOf(AuftragsStatus.BEAUFTRAGT), filter.auftragsStatuses.toSet())

        val empty = OrderEntityRest.toAuftragFilter(MagicFilter())
        assertNull(empty.searchString)
        assertTrue(empty.auftragsStatuses.isEmpty())
        assertTrue(empty.auftragsPositionsArten.isEmpty())
        assertNull(empty.auftragsPositionsPaymentType)
        // Not null: AuftragFilter's getter answers ALL for an unset invoiced status, which is its way of
        // saying "no criterion".
        assertEquals(AuftragFakturiertFilterStatus.ALL, empty.auftragFakturiertFilterStatus)
        assertNull(empty.periodOfPerformanceStartDate)
        assertNull(empty.startDate)
    }

    /**
     * The forecast charts tab lists which of the list's criteria it did not apply, so a total that disagrees
     * with the list is explained rather than silent: whatever [OrderEntityRest.toAuftragFilter] drops has to
     * show up here.
     */
    @Test
    fun `the forecast filter usage names what the translation drops`() {
        val magicFilter = MagicFilter()
        magicFilter.entries.add(entry(MagicFilter.PAGINATION_PAGE_SIZE).also { it.value.value = "50" })
        magicFilter.entries.add(entry("status", values = arrayOf("BEAUFTRAGT")))
        magicFilter.entries.add(entry("positionsStatus", values = arrayOf("BEAUFTRAGT")))
        magicFilter.entries.add(entry("positionsPaymentType", values = arrayOf("FESTPREISPAKET", "TIME_AND_MATERIALS")))
        magicFilter.entries.add(entry("projectManager").also { it.value.id = 42L })
        magicFilter.entries.add(
            entry(OrderEntityRest.PERIOD_OF_PERFORMANCE_FILTER, fromValue = "2026-01-01", toValue = "2026-12-31")
        )
        // Without a value an entry is no criterion, so nothing to report.
        magicFilter.entries.add(entry("kunde.name"))

        val usage = OrderEntityRest.forecastFilterUsage(magicFilter)
        assertEquals(listOf("status", "positionsStatus", "projectManager"), usage.ignored)
        assertEquals(listOf(OrderEntityRest.PERIOD_OF_PERFORMANCE_FILTER), usage.replaced)
        assertEquals(listOf("positionsPaymentType"), usage.partial)

        val single = MagicFilter()
        single.entries.add(entry("positionsPaymentType", values = arrayOf("FESTPREISPAKET")))
        assertTrue(OrderEntityRest.forecastFilterUsage(single).partial.isEmpty())
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
        val auftragFilter = OrderEntityRest.toAuftragFilter(chartFilter)
        assertTrue(auftragFilter.auftragsStatuses.isEmpty())
        assertEquals(AuftragFakturiertFilterStatus.ALL, auftragFilter.auftragFakturiertFilterStatus)
        assertEquals(listOf("status", "fakturiert", "kunde.name"), magicFilter.entries.map { it.field })
        // kunde.name is ignored anyway: toAuftragFilter doesn't translate it.
        assertEquals(
            listOf("status", "fakturiert", "kunde.name"), OrderEntityRest.forecastFilterUsage(magicFilter).ignored
        )
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
