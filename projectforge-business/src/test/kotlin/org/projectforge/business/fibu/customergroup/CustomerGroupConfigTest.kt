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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The stored JSON, the text patterns and the lookup built from both. */
class CustomerGroupConfigTest {
    @Test
    fun `the config survives a round trip through its json`() {
        val config = CustomerGroupConfig.parse(sample().toJson())
        assertEquals(CustomerGroupConfig.VERSION, config.version)
        assertEquals(listOf("ACME", "Other"), config.groups.map { it.name })
        assertEquals(listOf(101L, 102L), config.groups[0].customers)
        assertEquals(listOf("ACME Holding GmbH", "ACME*"), config.groups[0].texts)
        assertEquals(listOf("acmeg1"), config.businessUnits[0].groups)
        assertEquals(listOf("*Spedition"), config.businessUnits[0].texts)
    }

    @Test
    fun `blank or broken json yields an empty config`() {
        listOf(null, "", "  ", "{no json", "[1,2]").forEach {
            val config = CustomerGroupConfig.parse(it)
            assertTrue(config.groups.isEmpty() && config.businessUnits.isEmpty(), "input: $it")
        }
        // Unknown properties (a newer version's) don't spoil the rest:
        val config = CustomerGroupConfig.parse("""{"version":2,"groups":[{"name":"A","future":true}]}""")
        assertEquals("A", config.groups.single().name)
    }

    @Test
    fun `text patterns accept names and leading or trailing wildcards only`() {
        assertEquals(TextPattern.MatchType.EXACT, TextPattern.of(" ACME, Inc. ")!!.matchType)
        assertEquals(TextPattern.MatchType.STARTS_WITH, TextPattern.of("ACME*")!!.matchType)
        assertEquals(TextPattern.MatchType.ENDS_WITH, TextPattern.of("*Logistics")!!.matchType)
        assertEquals(TextPattern.MatchType.CONTAINS, TextPattern.of("*ACME*")!!.matchType)
        listOf(null, "", "*", "**", "A", "*A*", "AC*ME", "AC%*", "*AC_ME", "* ACME").forEach {
            assertNull(TextPattern.of(it), "input: $it")
        }
        // No SQL wildcard in a pattern, but an exact name may contain one (it is compared with `=`):
        assertTrue(TextPattern.of("100% Media") != null)
    }

    @Test
    fun `text patterns match case-insensitively`() {
        assertTrue(TextPattern.of("ACME*")!!.matches("acme logistics"))
        assertFalse(TextPattern.of("ACME*")!!.matches("The ACME"))
        assertTrue(TextPattern.of("*logistics")!!.matches("ACME Logistics"))
        assertTrue(TextPattern.of("*cm*")!!.matches("ACME"))
        assertTrue(TextPattern.of("acme holding")!!.matches("ACME Holding"))
        assertFalse(TextPattern.of("acme holding")!!.matches("ACME Holding GmbH"))
        assertEquals(TextPattern.of("acme*"), TextPattern.of("ACME*"))
    }

    @Test
    fun `the index finds a row's group, entity before text and exact text before pattern`() {
        val index = CustomerGroupIndex(sample())
        val (acme, other) = index.groups
        assertSame(acme, index.groupOf(101, null))
        assertNull(index.groupOf(999, null))
        // The entity decides alone, its text doesn't count:
        assertNull(index.groupOf(999, "ACME Holding GmbH"))
        assertSame(acme, index.groupOf(null, "acme holding gmbh"))
        assertSame(acme, index.groupOf(null, "ACME Logistics"))
        assertSame(other, index.groupOf(null, "Other Ltd"))
        assertNull(index.groupOf(null, " "))
    }

    @Test
    fun `the index finds a row's business unit directly or via its group`() {
        val index = CustomerGroupIndex(sample())
        val bu = index.businessUnits.single()
        assertSame(bu, index.businessUnitOf(101, null))
        assertSame(bu, index.businessUnitOf(310, null))
        assertSame(bu, index.businessUnitOf(null, "Müller Spedition"))
        assertNull(index.businessUnitOf(null, "Other Ltd"))
    }

    @Test
    fun `a business unit resolves to its own members and those of its groups`() {
        val resolved = CustomerGroupIndex(sample()).resolveBusinessUnit("logbu1")!!
        assertEquals(setOf(101L, 102L, 310L), resolved.kundeIds)
        assertEquals(listOf("*Spedition", "ACME Holding GmbH", "ACME*"), resolved.texts.map { it.raw })
        assertNull(CustomerGroupIndex(sample()).resolveBusinessUnit("gone12"))
        assertNull(CustomerGroupIndex(sample()).resolveGroup(null))
    }

    companion object {
        fun group(key: String?, name: String?, customers: List<Long> = emptyList(), texts: List<String> = emptyList()) =
            CustomerGroup().also {
                it.key = key
                it.name = name
                it.customers = customers.toMutableList()
                it.texts = texts.toMutableList()
            }

        fun businessUnit(
            key: String?,
            name: String?,
            groups: List<String> = emptyList(),
            customers: List<Long> = emptyList(),
            texts: List<String> = emptyList(),
        ) = BusinessUnit().also {
            it.key = key
            it.name = name
            it.groups = groups.toMutableList()
            it.customers = customers.toMutableList()
            it.texts = texts.toMutableList()
        }

        fun sample() = CustomerGroupConfig(
            groups = mutableListOf(
                group("acmeg1", "ACME", listOf(101, 102), listOf("ACME Holding GmbH", "ACME*")),
                group("othrg2", "Other", texts = listOf("Other*")),
            ),
            businessUnits = mutableListOf(
                businessUnit("logbu1", "Logistics", listOf("acmeg1"), listOf(310), listOf("*Spedition")),
            ),
        )
    }
}
