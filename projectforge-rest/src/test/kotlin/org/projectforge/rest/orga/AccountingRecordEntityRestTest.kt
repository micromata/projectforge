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

package org.projectforge.rest.orga

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.api.SortProperty

/**
 * The sort mapping of the accounting-record list. No database and no Spring context: what is under test is
 * the rewriting of the sort properties, which is plain logic.
 */
class AccountingRecordEntityRestTest {
    /**
     * The list's record number is `yyyy-mm-#####`, but the `satznr` column is the number within a month only:
     * ordered by it alone, the months interleave. It has to become year, month and number.
     */
    @Test
    fun `sorting by the record number becomes year, month and number`() {
        assertEquals(listOf("year", "month", "satznr"), sortedProperties(SortProperty("satznr")))
    }

    @Test
    fun `the direction of the sort is kept by every part`() {
        val queryFilter = process(SortProperty("satznr", SortOrder.DESCENDING))
        assertEquals(3, queryFilter.sortProperties.size)
        queryFilter.sortProperties.forEach {
            assertEquals(SortOrder.DESCENDING, it.sortOrder, it.property)
        }
    }

    @Test
    fun `the parts keep the place of the column they replace`() {
        assertEquals(
            listOf("betrag", "year", "month", "satznr", "beleg"),
            sortedProperties(SortProperty("betrag"), SortProperty("satznr"), SortProperty("beleg")),
        )
    }

    @Test
    fun `a sort by other columns is left alone`() {
        assertEquals(listOf("betrag", "beleg"), sortedProperties(SortProperty("betrag"), SortProperty("beleg")))
    }

    private fun sortedProperties(vararg sortProperties: SortProperty): List<String> {
        return process(*sortProperties).sortProperties.map { it.property }
    }

    private fun process(vararg sortProperties: SortProperty): QueryFilter {
        val queryFilter = QueryFilter()
        queryFilter.sortProperties = sortProperties.toMutableList()
        AccountingRecordEntityRest().postProcessMagicFilter(queryFilter, MagicFilter())
        return queryFilter
    }
}
