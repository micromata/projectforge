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
import org.projectforge.business.fibu.customergroup.CustomerGroup
import org.projectforge.business.fibu.customergroup.CustomerGroupConfig
import org.projectforge.business.fibu.customergroup.CustomerGroupIndex
import org.projectforge.business.fibu.kost.Kost2DO

/**
 * The customer checklist matches a row by its customer entity or, for a row without one, by its free-text
 * customer — the two kinds of keys the frontend sends.
 */
class CustomerChecklistFilterTest {
    private val filter = CustomerChecklistFilter("order/customerFilterValues")

    @Test
    fun `entity keys match the order's customer`() {
        val predicate = filter.buildPredicate(arrayOf("k:473", "k:12"))!!
        assertTrue(predicate.match(order(kundeId = 473)))
        assertFalse(predicate.match(order(kundeId = 99)))
        assertFalse(predicate.match(order(kundeText = "473")))
    }

    @Test
    fun `text keys match only orders without a customer entity`() {
        val predicate = filter.buildPredicate(arrayOf("t:ACME"))!!
        assertTrue(predicate.match(order(kundeText = "ACME")))
        assertFalse(predicate.match(order(kundeText = "Other")))
        // The cell shows the entity then, so does the filter.
        assertFalse(predicate.match(order(kundeId = 5, kundeText = "ACME")))
    }

    @Test
    fun `both kinds combine as alternatives`() {
        val predicate = filter.buildPredicate(arrayOf("k:473", "t:ACME"))!!
        assertTrue(predicate.match(order(kundeId = 473)))
        assertTrue(predicate.match(order(kundeText = "ACME")))
        assertFalse(predicate.match(order(kundeId = 1)))
    }

    @Test
    fun `no usable key filters nothing`() {
        assertNull(filter.buildPredicate(null))
        assertNull(filter.buildPredicate(arrayOf()))
        assertNull(filter.buildPredicate(arrayOf("k:abc", "unknown")))
    }

    @Test
    fun `without a free-text path text keys are ignored`() {
        val projects = CustomerChecklistFilter("project/customerFilterValues", kundeTextPath = null)
        assertNull(projects.buildPredicate(arrayOf("t:ACME")))
        val predicate = projects.buildPredicate(arrayOf("k:473", "t:ACME"))!!
        assertTrue(predicate.match(ProjektDO().also { it.kunde = kunde(473) }))
        assertFalse(predicate.match(ProjektDO()))
    }

    @Test
    fun `a nested path matches the customer of the cost 2's project`() {
        val kost2s = CustomerChecklistFilter("cost2/customerFilterValues", kundePath = "projekt.kunde", kundeTextPath = null)
        val predicate = kost2s.buildPredicate(arrayOf("k:473"))!!
        assertTrue(predicate.match(kost2(kunde(473))))
        assertFalse(predicate.match(kost2(kunde(12))))
    }

    @Test
    fun `group keys match the group's customers and, without an entity, its texts`() {
        val grouped = CustomerChecklistFilter("order/customerFilterValues", groupIndex = { index })
        val predicate = grouped.buildPredicate(arrayOf("g:acmeg1"))!!
        assertTrue(predicate.match(order(kundeId = 101)))
        assertFalse(predicate.match(order(kundeId = 999)))
        assertTrue(predicate.match(order(kundeText = "acme holding gmbh")))
        assertTrue(predicate.match(order(kundeText = "ACME Logistics")))
        assertFalse(predicate.match(order(kundeText = "The ACME")))
        // The entity decides, as for a text key:
        assertFalse(predicate.match(order(kundeId = 999, kundeText = "ACME Logistics")))
        // An unknown group (deleted since the filter was saved) filters nothing:
        assertNull(grouped.buildPredicate(arrayOf("g:gone12")))
    }

    @Test
    fun `group keys combine with entity and text keys`() {
        val grouped = CustomerChecklistFilter("order/customerFilterValues", groupIndex = { index })
        val predicate = grouped.buildPredicate(arrayOf("g:acmeg1", "k:7", "t:Solo"))!!
        assertTrue(predicate.match(order(kundeId = 101)))
        assertTrue(predicate.match(order(kundeId = 7)))
        assertTrue(predicate.match(order(kundeText = "Solo")))
        assertFalse(predicate.match(order(kundeText = "Other")))
    }

    @Test
    fun `without a free-text path a group matches by its entities only`() {
        val projects = CustomerChecklistFilter("project/customerFilterValues", kundeTextPath = null, groupIndex = { index })
        val predicate = projects.buildPredicate(arrayOf("g:acmeg1"))!!
        assertTrue(predicate.match(ProjektDO().also { it.kunde = kunde(102) }))
        assertFalse(predicate.match(ProjektDO()))
        assertNull(projects.buildPredicate(arrayOf("g:textg2")))
    }

    @Test
    fun `the values offer the rows' groups first`() {
        val values = CustomerChecklistFilter.valuesOf(
            sequenceOf(CustomerRow(null, "Zeta"), CustomerRow(null, "ACME Logistics"), CustomerRow(null, "Beta"), CustomerRow(null, "Solo Text")),
            index,
        )
        assertEquals(listOf("g:acmeg1", "t:ACME Logistics", "t:Beta", "t:Solo Text", "t:Zeta"), values.map { it.id })
        assertEquals(true, values[0].group)
        assertEquals("ACME", values[0].displayName)
        assertNull(values[1].group)
    }

    @Test
    fun `free texts differing only in surrounding blanks are one value`() {
        val values = CustomerChecklistFilter.valuesOf(
            sequenceOf(CustomerRow(null, "DHL"), CustomerRow(null, "DHL "), CustomerRow(null, " DHL"), CustomerRow(null, "  ")),
            index,
        )
        assertEquals(listOf("t:DHL"), values.map { it.id })
        assertEquals("DHL", values[0].displayName)
    }

    private val index = CustomerGroupIndex(
        CustomerGroupConfig(
            groups = mutableListOf(
                group("acmeg1", "ACME", listOf(101, 102), listOf("ACME Holding GmbH", "ACME*")),
                group("textg2", "Texts only", texts = listOf("*Spedition")),
            ),
        )
    )

    private fun group(key: String, name: String, customers: List<Long> = emptyList(), texts: List<String> = emptyList()) =
        CustomerGroup().also {
            it.key = key
            it.name = name
            it.customers = customers.toMutableList()
            it.texts = texts.toMutableList()
        }

    private fun kunde(id: Long) = KundeDO().also { it.nummer = id }

    private fun kost2(kunde: KundeDO) = Kost2DO().also { it.projekt = ProjektDO().also { p -> p.kunde = kunde } }

    private fun order(kundeId: Long? = null, kundeText: String? = null) = AuftragDO().also { order ->
        order.kunde = kundeId?.let { kunde(it) }
        order.kundeText = kundeText
    }
}
