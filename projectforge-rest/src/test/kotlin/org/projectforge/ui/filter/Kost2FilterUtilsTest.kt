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

package org.projectforge.ui.filter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate

class Kost2FilterUtilsTest {

    @Test
    fun `the cost 2 type filter element offers each type as number and name`() {
        val element = Kost2FilterUtils.createKost2ArtFilterElement(
            listOf(kost2Art(0, "Fakturiert"), kost2Art(54, "Wartung"), kost2Art(3, null))
        )
        assertEquals("kost2.kost2Art.id", element.id)
        assertEquals(true, element.multi)
        val values = element.values!!
        assertEquals(3, values.size)
        // Two-digit number prefixed, name appended; the id stays the raw number for the query.
        assertEquals("0", values[0].id)
        assertEquals("00: Fakturiert", values[0].displayName)
        assertEquals("54: Wartung", values[1].displayName)
        // No name: just the number, no dangling colon.
        assertEquals("03", values[2].displayName)
    }

    @Test
    fun `picked cost 2 types become an IN on the type id`() {
        val source = MagicFilter()
        source.entries.add(MagicFilterEntry("kost2.kost2Art.id").also { it.value.values = arrayOf("0", "54") })
        val target = QueryFilter()

        val consumed = Kost2FilterUtils.preProcessKost2Art(target, source)

        assertTrue(consumed)
        assertEquals(true, source.entries.first().synthetic, "The entry must be marked synthetic so it isn't processed again.")
        val predicate = target.createDBFilter().allPredicates.filterIsInstance<DBPredicate.IsIn<*>>().firstOrNull()
        assertNotNull(predicate, "An IN predicate must be added for the picked types.")
        assertEquals("kost2.kost2Art.id", predicate!!.field)
        assertEquals(listOf(0L, 54L), predicate.values.toList())
    }

    @Test
    fun `an absent entry consumes nothing, an empty one adds no predicate`() {
        assertFalse(Kost2FilterUtils.preProcessKost2Art(QueryFilter(), MagicFilter()))

        val source = MagicFilter()
        source.entries.add(MagicFilterEntry("kost2.kost2Art.id").also { it.value.values = arrayOf() })
        val target = QueryFilter()
        assertTrue(Kost2FilterUtils.preProcessKost2Art(target, source))
        assertTrue(target.createDBFilter().allPredicates.none { it is DBPredicate.IsIn<*> })
    }

    private fun kost2Art(id: Long, name: String?): Kost2ArtDO {
        return Kost2ArtDO().also {
            it.id = id
            it.name = name
        }
    }
}
