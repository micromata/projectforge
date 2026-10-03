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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.customergroup.CustomerGroupConfigTest.Companion.businessUnit
import org.projectforge.business.fibu.customergroup.CustomerGroupConfigTest.Companion.group

class CustomerGroupIndexTest {
    private val directory = CustomerDirectory(
        customers = mapOf(101L to "ACME Germany", 102L to "ACME Logistics", 200L to "Internal", 300L to "Kühne Spedition"),
        // Project 1 of the internal customer below task 10 (in BU Blue), project 2 below 20 (in BU Green),
        // project 3 of an ACME customer below 20, project 4 below no business unit's task.
        projects = mapOf(
            1L to ProjectRef(200, 11),
            2L to ProjectRef(200, 21),
            3L to ProjectRef(101, 21),
            4L to ProjectRef(200, 30),
        ),
        taskPath = {
            mapOf(10L to listOf(1L, 10L), 11L to listOf(1L, 10L, 11L), 20L to listOf(1L, 20L), 21L to listOf(1L, 20L, 21L), 30L to listOf(1L, 30L))[it]
        },
    )

    private val config = CustomerGroupConfig(
        groups = mutableListOf(group("acmeg1", "ACME", texts = listOf("ACME*"))),
        businessUnits = mutableListOf(
            businessUnit("blue01", "Blue", listOf("acmeg1"), texts = listOf("*Spedition")).also { it.tasks = mutableListOf(10) },
            businessUnit("green1", "Green").also { it.tasks = mutableListOf(20) },
        ),
    )

    private val index = CustomerGroupIndex(config, directory)

    @Test
    fun `a name pattern matches customer entities by name and free texts`() {
        assertEquals("ACME", index.groupOf(101, null)?.name)
        assertEquals("ACME", index.groupOf(102, "ignored for an entity")?.name)
        assertEquals("ACME", index.groupOf(null, "ACME Holding")?.name)
        assertNull(index.groupOf(200, null))
        assertEquals(setOf(101L, 102L), index.resolveGroup("acmeg1")?.kundeIds)
    }

    @Test
    fun `a business unit comes from the customer, else from the project's task`() {
        assertEquals("Blue", index.businessUnitOf(101, null)?.name) // via its group
        assertEquals("Blue", index.businessUnitOf(300, null)?.name) // by its name
        assertEquals("Blue", index.businessUnitOf(200, null, 1)?.name) // task 11 below 10
        assertEquals("Green", index.businessUnitOf(200, null, 2)?.name)
        assertNull(index.businessUnitOf(200, null, 4))
        assertNull(index.businessUnitOf(200, null))
        // The customer's business unit wins over the project's task:
        assertEquals("Blue", index.businessUnitOf(101, null, 3)?.name)
    }

    @Test
    fun `a business unit resolves to its customers, texts and projects`() {
        val blue = index.resolveBusinessUnit("blue01")!!
        assertEquals(setOf(101L, 102L, 300L), blue.kundeIds)
        assertEquals(listOf("*Spedition", "ACME*"), blue.texts.map { it.raw })
        assertEquals(setOf(1L), blue.projektIds)
        // Project 3 is listed under Green by its task; its customer's business unit decides per row.
        assertEquals(setOf(2L, 3L), index.resolveBusinessUnit("green1")?.projektIds)
        assertEquals(setOf(101L, 102L, 300L), index.businessUnitCustomers.kundeIds)
    }

    @Test
    fun `the deepest task decides`() {
        val nested = CustomerGroupConfig(
            businessUnits = mutableListOf(
                businessUnit("outer1", "Outer").also { it.tasks = mutableListOf(20) },
                businessUnit("inner1", "Inner").also { it.tasks = mutableListOf(21) },
            ),
        )
        assertEquals("Inner", CustomerGroupIndex(nested, directory).businessUnitOf(200, null, 2)?.name)
    }
}
