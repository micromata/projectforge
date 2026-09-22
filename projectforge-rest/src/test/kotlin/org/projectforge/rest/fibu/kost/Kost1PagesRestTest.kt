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
 * The sort mapping of the cost 1 list. No database and no Spring context: what is under test is the
 * rewriting of the sort properties, which is plain logic.
 */
class Kost1PagesRestTest {
    /**
     * `formattedNumber` is a getter of Kost1DO without a column of its own, so ordering by it makes the
     * criteria query fail ("Could not resolve attribute") and return the rows unordered. It has to
     * become the four number columns it is made of, in their significance.
     */
    @Test
    fun `sorting by the formatted number becomes its four columns`() {
        assertEquals(
            listOf("nummernkreis", "bereich", "teilbereich", "endziffer"),
            sortedProperties(SortProperty("formattedNumber")),
        )
    }

    @Test
    fun `the direction of the sort is kept by every part`() {
        val queryFilter = process(SortProperty("formattedNumber", SortOrder.DESCENDING))
        assertEquals(4, queryFilter.sortProperties.size)
        queryFilter.sortProperties.forEach {
            assertEquals(SortOrder.DESCENDING, it.sortOrder, it.property)
        }
    }

    /**
     * The parts take the place of the column they replace, not the end of the list: a second sort
     * criterion stays the less significant one.
     */
    @Test
    fun `the parts keep the place of the column they replace`() {
        assertEquals(
            listOf("kostentraegerStatus", "nummernkreis", "bereich", "teilbereich", "endziffer", "description"),
            sortedProperties(
                SortProperty("kostentraegerStatus"),
                SortProperty("formattedNumber"),
                SortProperty("description"),
            ),
        )
    }

    @Test
    fun `a sort by real columns is left alone`() {
        assertEquals(
            listOf("description", "kostentraegerStatus"),
            sortedProperties(SortProperty("description"), SortProperty("kostentraegerStatus")),
        )
    }

    private fun sortedProperties(vararg sortProperties: SortProperty): List<String> {
        return process(*sortProperties).sortProperties.map { it.property }
    }

    private fun process(vararg sortProperties: SortProperty): QueryFilter {
        val queryFilter = QueryFilter()
        queryFilter.sortProperties = sortProperties.toMutableList()
        // The filter the client sent isn't read by the mapping — only the QueryFilter built from it is.
        Kost1PagesRest().postProcessMagicFilter(queryFilter, MagicFilter())
        return queryFilter
    }
}
