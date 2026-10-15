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
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.customergroup.BusinessUnit
import org.projectforge.business.fibu.customergroup.CustomerGroup
import org.projectforge.business.fibu.customergroup.CustomerDirectory
import org.projectforge.business.fibu.customergroup.CustomerGroupConfig
import org.projectforge.business.fibu.customergroup.CustomerGroupIndex
import org.projectforge.business.fibu.customergroup.ProjectRef
import org.projectforge.business.fibu.kost.Kost2DO

/**
 * The business-unit checklist matches a unit's own customers and texts and those of its groups, and the projects
 * below its tasks for rows whose customer leads to no business unit.
 */
class BusinessUnitChecklistFilterTest {
    private val filter = BusinessUnitChecklistFilter("order/businessUnitFilterValues", groupIndex = { index })

    @Test
    fun `a business unit matches its direct members and those of its groups`() {
        val predicate = filter.buildPredicate(arrayOf("b:logbu1"))!!
        assertTrue(predicate.match(order(kundeId = 310)))
        assertTrue(predicate.match(order(kundeId = 101)))
        assertTrue(predicate.match(order(kundeText = "Müller Spedition")))
        assertTrue(predicate.match(order(kundeText = "ACME Logistics")))
        assertFalse(predicate.match(order(kundeId = 999)))
        assertFalse(predicate.match(order(kundeText = "Other")))
    }

    @Test
    fun `several business units combine as alternatives, unknown ones are ignored`() {
        val predicate = filter.buildPredicate(arrayOf("b:logbu1", "b:retbu2", "b:gone12"))!!
        assertTrue(predicate.match(order(kundeId = 500)))
        assertTrue(predicate.match(order(kundeId = 310)))
        assertNull(filter.buildPredicate(arrayOf("b:gone12")))
        assertNull(filter.buildPredicate(arrayOf("k:310")))
        assertNull(filter.buildPredicate(null))
    }

    @Test
    fun `projects and cost 2 match by customer entity only`() {
        val projects = BusinessUnitChecklistFilter("project/businessUnitFilterValues", kundeTextPath = null, groupIndex = { index })
        assertTrue(projects.buildPredicate(arrayOf("b:logbu1"))!!.match(ProjektDO().also { it.kunde = kunde(102) }))
        val kost2s = BusinessUnitChecklistFilter(
            "cost2/businessUnitFilterValues", kundePath = "projekt.kunde", kundeTextPath = null, groupIndex = { index },
        )
        val predicate = kost2s.buildPredicate(arrayOf("b:logbu1"))!!
        assertTrue(predicate.match(Kost2DO().also { it.projekt = ProjektDO().also { p -> p.kunde = kunde(310) } }))
        assertFalse(predicate.match(Kost2DO().also { it.projekt = ProjektDO().also { p -> p.kunde = kunde(500) } }))
    }

    @Test
    fun `a project below a business unit's task counts if its customer leads to no business unit`() {
        val predicate = filter.buildPredicate(arrayOf("b:retbu2"))!!
        assertTrue(predicate.match(order(kundeId = 200, projektId = 7)))
        assertTrue(predicate.match(order(kundeText = "Internal Ltd.", projektId = 7)))
        assertTrue(predicate.match(order(projektId = 7)))
        // The customer's own business unit wins, by entity or by free text:
        assertFalse(predicate.match(order(kundeId = 101, projektId = 7)))
        assertFalse(predicate.match(order(kundeText = "Müller Spedition", projektId = 7)))
        assertFalse(predicate.match(order(kundeId = 200, projektId = 8)))
        assertFalse(predicate.match(order(kundeId = 200)))
        // Projects themselves, by their own id:
        val projects = BusinessUnitChecklistFilter(
            "project/businessUnitFilterValues", kundeTextPath = null, projektIdPath = "id", groupIndex = { index },
        )
        assertTrue(projects.buildPredicate(arrayOf("b:retbu2"))!!.match(ProjektDO().also { it.id = 7; it.kunde = kunde(200) }))
        assertFalse(projects.buildPredicate(arrayOf("b:retbu2"))!!.match(ProjektDO().also { it.id = 7; it.kunde = kunde(101) }))
    }

    @Test
    fun `the remainder matches the rows of no business unit`() {
        val predicate = filter.buildPredicate(arrayOf(BusinessUnitChecklistFilter.NONE_KEY))!!
        assertTrue(predicate.match(order(kundeId = 999)))
        assertTrue(predicate.match(order(kundeText = "Other")))
        assertTrue(predicate.match(order()))
        assertTrue(predicate.match(order(kundeId = 200, projektId = 8)))
        // A customer of a business unit, directly, through its group or by free text:
        assertFalse(predicate.match(order(kundeId = 310)))
        assertFalse(predicate.match(order(kundeId = 101)))
        assertFalse(predicate.match(order(kundeText = "Müller Spedition")))
        // A project below a business unit's task, for a customer of none:
        assertFalse(predicate.match(order(kundeId = 200, projektId = 7)))
        assertFalse(predicate.match(order(projektId = 7)))
        // Combined with a business unit as an alternative:
        val withRetail = filter.buildPredicate(arrayOf("b:retbu2", BusinessUnitChecklistFilter.NONE_KEY))!!
        assertTrue(withRetail.match(order(kundeId = 500)))
        assertTrue(withRetail.match(order(kundeId = 999)))
        assertFalse(withRetail.match(order(kundeId = 310)))
        // Nothing to be the remainder of without business units:
        val none = BusinessUnitChecklistFilter("order/businessUnitFilterValues", groupIndex = { CustomerGroupIndex.EMPTY })
        assertNull(none.buildPredicate(arrayOf(BusinessUnitChecklistFilter.NONE_KEY)))
    }

    @Test
    fun `the values take a row's project into account`() {
        assertEquals(listOf("b:retbu2"), filter.valuesOf(sequenceOf(CustomerRow(200, null, 7))).map { it.id })
        assertEquals(listOf("b:logbu1"), filter.valuesOf(sequenceOf(CustomerRow(101, null, 7))).map { it.id })
        assertEquals(
            listOf(BusinessUnitChecklistFilter.NONE_KEY),
            filter.valuesOf(sequenceOf(CustomerRow(200, null, 8))).map { it.id },
        )
    }

    @Test
    fun `the values are the business units of the rows, each once, sorted by name, the remainder last`() {
        val values = filter.valuesOf(sequenceOf(CustomerRow(500, null), CustomerRow(101, null), CustomerRow(null, "ACME AG"), CustomerRow(999, null), CustomerRow(310, null)))
        assertEquals(listOf("b:logbu1", "b:retbu2", BusinessUnitChecklistFilter.NONE_KEY), values.map { it.id })
        assertEquals(listOf("Logistics", "Retail"), values.take(2).map { it.displayName })
        assertEquals(listOf("b:logbu1"), filter.valuesOf(sequenceOf(CustomerRow(101, null))).map { it.id })
    }

    @Test
    fun `nothing is offered without business units`() {
        val none = BusinessUnitChecklistFilter("order/businessUnitFilterValues", groupIndex = { CustomerGroupIndex.EMPTY })
        assertTrue(none.valuesOf(sequenceOf(CustomerRow(101, null))).isEmpty())
        assertNull(none.element())
    }

    private val index = CustomerGroupIndex(
        CustomerGroupConfig(
            groups = mutableListOf(
                CustomerGroup().also {
                    it.key = "acmeg1"
                    it.name = "ACME"
                    it.customers = mutableListOf(101, 102)
                    it.texts = mutableListOf("ACME*")
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
                    it.groups = mutableListOf("acmeg1")
                    it.customers = mutableListOf(310)
                    it.texts = mutableListOf("*Spedition")
                },
            ),
        ),
        // Project 7 lies below task 10 (Retail), project 8 below no business unit's task.
        CustomerDirectory(
            projects = mapOf(7L to ProjectRef(200, 11), 8L to ProjectRef(200, 30)),
            taskPath = { mapOf(11L to listOf(1L, 10L, 11L), 30L to listOf(1L, 30L))[it] },
        ),
    )

    private fun kunde(id: Long) = KundeDO().also { it.nummer = id }

    private fun order(kundeId: Long? = null, kundeText: String? = null, projektId: Long? = null) = AuftragDO().also { order ->
        order.kunde = kundeId?.let { kunde(it) }
        order.kundeText = kundeText
        order.projekt = projektId?.let { id -> ProjektDO().also { it.id = id } }
    }
}
