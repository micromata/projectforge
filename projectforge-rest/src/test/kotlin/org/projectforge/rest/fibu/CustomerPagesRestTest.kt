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
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.api.SortProperty

/**
 * The sort mapping of the customer list. No database and no Spring context: what is under test is the
 * rewriting of the sort properties, which is plain logic.
 */
class CustomerPagesRestTest {
    /**
     * `kost` is a getter of KundeDO without a column of its own, so ordering by it makes the criteria
     * query drop the order. It has to become the number column it formats.
     */
    @Test
    fun `sorting by the formatted number becomes the number column`() {
        val queryFilter = process(SortProperty("kost", SortOrder.DESCENDING))
        assertEquals(listOf("nummer"), queryFilter.sortProperties.map { it.property })
        assertEquals(SortOrder.DESCENDING, queryFilter.sortProperties[0].sortOrder)
    }

    @Test
    fun `the number keeps the place of the column it replaces`() {
        assertEquals(
            listOf("status", "nummer", "name"),
            process(SortProperty("status"), SortProperty("kost"), SortProperty("name"))
                .sortProperties.map { it.property },
        )
    }

    private fun process(vararg sortProperties: SortProperty): QueryFilter {
        val queryFilter = QueryFilter()
        queryFilter.sortProperties = sortProperties.toMutableList()
        CustomerPagesRest().postProcessMagicFilter(queryFilter, MagicFilter())
        return queryFilter
    }
}
