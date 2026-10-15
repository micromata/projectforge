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

package org.projectforge.rest.fibu.kost

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.api.SortProperty

/**
 * The sort mapping of the cost 2 list. No database and no Spring context: what is under test is the
 * rewriting of the sort properties, which is plain logic.
 */
class Kost2EntityRestTest {
    /**
     * `formattedNumber` is a getter of Kost2DO without a column of its own. For cost 2 the last part is
     * the Kost2Art (its id), not a plain `endziffer` — hence the join, mirroring `Kost2Dao.select`.
     */
    @Test
    fun `sorting by the formatted number becomes its columns incl the kost2Art id`() {
        val queryFilter = process(SortProperty("formattedNumber"))
        assertEquals(
            listOf("nummernkreis", "bereich", "teilbereich", "kost2Art.id"),
            queryFilter.sortProperties.map { it.property },
        )
        // The order by kost2Art.id needs the join Kost2Dao.select creates.
        assertEquals(1, queryFilter.joinList.count { it.attribute == "kost2Art" }, "join on kost2Art")
    }

    @Test
    fun `the direction of the sort is kept by every part`() {
        val queryFilter = process(SortProperty("formattedNumber", SortOrder.DESCENDING))
        assertEquals(4, queryFilter.sortProperties.size)
        queryFilter.sortProperties.forEach {
            assertEquals(SortOrder.DESCENDING, it.sortOrder, it.property)
        }
    }

    @Test
    fun `a sort by real columns is left alone and adds no join`() {
        val queryFilter = process(SortProperty("description"), SortProperty("kostentraegerStatus"))
        assertEquals(
            listOf("description", "kostentraegerStatus"),
            queryFilter.sortProperties.map { it.property },
        )
        assertEquals(0, queryFilter.joinList.count { it.attribute == "kost2Art" }, "no join without the number sort")
    }

    private fun process(vararg sortProperties: SortProperty): QueryFilter {
        val queryFilter = QueryFilter()
        queryFilter.sortProperties = sortProperties.toMutableList()
        // The filter the client sent isn't read by the mapping — only the QueryFilter built from it is.
        Kost2EntityRest().postProcessMagicFilter(queryFilter, MagicFilter())
        return queryFilter
    }
}
