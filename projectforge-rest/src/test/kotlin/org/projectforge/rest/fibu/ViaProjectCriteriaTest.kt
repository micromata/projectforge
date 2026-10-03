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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.customergroup.BusinessUnit
import org.projectforge.business.fibu.customergroup.CustomerDirectory
import org.projectforge.business.fibu.customergroup.CustomerGroup
import org.projectforge.business.fibu.customergroup.CustomerGroupConfig
import org.projectforge.business.fibu.customergroup.CustomerGroupIndex
import org.projectforge.business.fibu.customergroup.CustomerKey
import org.projectforge.business.fibu.customergroup.ProjectRef
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.timesheet.TimesheetDO

/**
 * Timesheets reach their customer only through the cost 2's project: the customer and business-unit picks
 * become the cost 2 of the projects they stand for.
 */
class ViaProjectCriteriaTest {
    private val customers = CustomerChecklistFilter("timesheet/customerFilterValues", groupIndex = { index })
    private val businessUnits = BusinessUnitChecklistFilter("timesheet/businessUnitFilterValues", groupIndex = { index })

    // Projects 1 (ACME), 2 (Retail's customer), 7 (Retail by task), 8 (none), 5 and 6 without a customer entity:
    // 5 ordered by the free text "Paketdienst" (Logistics), then by Retail; 6 by nobody. Cost 2 = project * 10,
    // 99 has none.
    private val via = ViaProjectCriteria(
        path = "kost2.id",
        nullPath = "kost2",
        projects = {
            mapOf(
                1L to listOf(CustomerKey(101)),
                2L to listOf(CustomerKey(500)),
                7L to listOf(CustomerKey(200)),
                8L to listOf(CustomerKey(200)),
                5L to listOf(CustomerKey(null, "Paketdienst"), CustomerKey(500)),
                6L to emptyList(),
            )
        },
        idsOf = { ids -> ids.map { it * 10 } },
        idsWithoutProject = { listOf(99L) },
    )

    @Test
    fun `a customer or group matches the sheets of its projects`() {
        val byCustomer = via.predicate(customers.projectMatch(arrayOf("k:500"))!!)
        assertTrue(byCustomer.match(sheet(20)))
        assertFalse(byCustomer.match(sheet(10)))
        val byGroup = via.predicate(customers.projectMatch(arrayOf("g:acmeg1"))!!)
        assertTrue(byGroup.match(sheet(10)))
        assertFalse(byGroup.match(sheet(20)))
        assertFalse(byGroup.match(sheet(null)))
    }

    @Test
    fun `a business unit matches by customer and by task`() {
        val predicate = via.predicate(businessUnits.projectMatch(arrayOf("b:retbu2"))!!)
        assertTrue(predicate.match(sheet(20)))
        assertTrue(predicate.match(sheet(70)))
        assertFalse(predicate.match(sheet(80)))
        assertFalse(predicate.match(sheet(10)))
    }

    @Test
    fun `the remainder matches the projects of no business unit and the sheets of none`() {
        val predicate = via.predicate(businessUnits.projectMatch(arrayOf(BusinessUnitChecklistFilter.NONE_KEY))!!)
        assertTrue(predicate.match(sheet(80)))
        assertTrue(predicate.match(sheet(99)))
        assertTrue(predicate.match(sheet(null)))
        assertFalse(predicate.match(sheet(70)))
        assertFalse(predicate.match(sheet(10)))
    }

    @Test
    fun `a project without a customer entity matches by the customers of its orders`() {
        val byText = via.predicate(customers.projectMatch(arrayOf("t:paketdienst "))!!)
        assertTrue(byText.match(sheet(50)))
        assertFalse(byText.match(sheet(60)))
        assertTrue(via.predicate(customers.projectMatch(arrayOf("k:500"))!!).match(sheet(50)))
        assertTrue(via.predicate(customers.projectMatch(arrayOf("g:paketg"))!!).match(sheet(50)))
    }

    @Test
    fun `a project counts for the business unit of its first customer having one`() {
        assertTrue(via.predicate(businessUnits.projectMatch(arrayOf("b:logbu1"))!!).match(sheet(50)))
        assertFalse(via.predicate(businessUnits.projectMatch(arrayOf("b:retbu2"))!!).match(sheet(50)))
        val none = via.predicate(businessUnits.projectMatch(arrayOf(BusinessUnitChecklistFilter.NONE_KEY))!!)
        assertTrue(none.match(sheet(60)))
        assertFalse(none.match(sheet(50)))
    }

    @Test
    fun `unknown keys filter nothing, known ones without sheets match none`() {
        assertNull(customers.projectMatch(arrayOf("k:x", "g:gone12")))
        assertNull(businessUnits.projectMatch(arrayOf("b:gone12")))
        val predicate = via.predicate(customers.projectMatch(arrayOf("k:4711"))!!)
        assertFalse(predicate.match(sheet(10)))
        assertFalse(predicate.match(sheet(null)))
    }

    private fun sheet(kost2Id: Long?) = TimesheetDO().also { sheet ->
        sheet.kost2 = kost2Id?.let { id -> Kost2DO().also { it.id = id } }
    }

    private val index = CustomerGroupIndex(
        CustomerGroupConfig(
            groups = mutableListOf(
                CustomerGroup().also {
                    it.key = "acmeg1"
                    it.name = "ACME"
                    it.customers = mutableListOf(101)
                },
                CustomerGroup().also {
                    it.key = "paketg"
                    it.name = "Paket"
                    it.texts = mutableListOf("Paket*")
                },
            ),
            businessUnits = mutableListOf(
                BusinessUnit().also {
                    it.key = "retbu2"
                    it.name = "Retail"
                    it.customers = mutableListOf(500)
                    it.tasks = mutableListOf(10)
                },
                BusinessUnit().also {
                    it.key = "logbu1"
                    it.name = "Logistics"
                    it.groups = mutableListOf("acmeg1", "paketg")
                },
            ),
        ),
        // Project 7 lies below task 10 (Retail), project 8 below no business unit's task.
        CustomerDirectory(
            projects = mapOf(7L to ProjectRef(200, 11), 8L to ProjectRef(200, 30)),
            taskPath = { mapOf(11L to listOf(1L, 10L, 11L), 30L to listOf(1L, 30L))[it] },
        ),
    )
}
