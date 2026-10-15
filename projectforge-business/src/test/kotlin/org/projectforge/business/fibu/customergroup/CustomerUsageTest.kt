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

package org.projectforge.business.fibu.customergroup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CustomerUsageTest {
    private val usage = CustomerUsage(
        orders = mapOf(
            (CustomerKey(null, "DHL") to 1L) to LocalDate.of(2020, 5, 1),
            (CustomerKey(null, "dhl") to 1L) to LocalDate.of(2024, 5, 1),
            (CustomerKey(500) to 1L) to LocalDate.of(2022, 1, 1),
            (CustomerKey(500) to 2L) to LocalDate.of(2019, 1, 1),
            (CustomerKey(700) to null) to LocalDate.of(2021, 1, 1),
        ),
        sheets = mapOf(1L to LocalDate.of(2026, 3, 1), 2L to LocalDate.of(2025, 3, 1), 3L to LocalDate.of(2026, 1, 1)),
    )

    // Project 2 has a customer entity of its own, projects 1 and 3 have none.
    private val projectKunde = mapOf(1L to null, 2L to 800L, 3L to null)

    @Test
    fun `a project's own customer wins, else its orders' customers, the most recent first, a text once`() {
        val customers = usage.projectCustomers(projectKunde)
        assertEquals(listOf(CustomerKey(null, "dhl"), CustomerKey(500)), customers[1])
        assertEquals(listOf(CustomerKey(800)), customers[2])
        assertEquals(setOf(1L, 2L), customers.keys) // project 3: timesheets, but no customer
    }

    @Test
    fun `timesheets count for the customers of their project`() {
        val years = usage.years(usage.projectCustomers(projectKunde))
        assertEquals(2026, years[CustomerKey(500) to 1L])
        assertEquals(2026, years[CustomerKey(null, "dhl") to 1L])
        assertEquals(2020, years[CustomerKey(null, "DHL") to 1L])
        assertEquals(2025, years[CustomerKey(800) to 2L])
        assertEquals(2019, years[CustomerKey(500) to 2L])
        assertEquals(2021, years[CustomerKey(700) to null])
    }

    @Test
    fun `a customer is an entity, else a trimmed free text`() {
        assertEquals(CustomerKey(1), CustomerKey.of(1, " ignored "))
        assertEquals(CustomerKey(null, "DHL"), CustomerKey.of(null, " DHL "))
        assertEquals(null, CustomerKey.of(null, "  "))
    }
}
