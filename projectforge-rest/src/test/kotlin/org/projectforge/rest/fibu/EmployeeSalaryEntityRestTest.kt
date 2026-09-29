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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.api.SortProperty

/**
 * The sort and filter mapping of the employee-salary list. No database and no Spring context: what is
 * under test is the rewriting of the sort properties and the consuming of the year/month filters, which
 * is plain logic.
 */
class EmployeeSalaryEntityRestTest {
    /**
     * `formattedYearAndMonth` is a transient getter of EmployeeSalaryDO (`year-MM`) without a column of
     * its own. Its parts, year then month, are the columns the list is really ordered by.
     */
    @Test
    fun `sorting by the formatted year and month becomes its columns`() {
        val queryFilter = process(SortProperty("formattedYearAndMonth"))
        assertEquals(
            listOf("year", "month"),
            queryFilter.sortProperties.map { it.property },
        )
    }

    @Test
    fun `the direction of the sort is kept by both parts`() {
        val queryFilter = process(SortProperty("formattedYearAndMonth", SortOrder.DESCENDING))
        assertEquals(2, queryFilter.sortProperties.size)
        queryFilter.sortProperties.forEach {
            assertEquals(SortOrder.DESCENDING, it.sortOrder, it.property)
        }
    }

    /** The remap keeps the position of the other sort properties around it. */
    @Test
    fun `the formatted year and month keeps its place among other sorts`() {
        val queryFilter = process(
            SortProperty("type"),
            SortProperty("formattedYearAndMonth", SortOrder.DESCENDING),
            SortProperty("comment"),
        )
        assertEquals(
            listOf("type", "year", "month", "comment"),
            queryFilter.sortProperties.map { it.property },
        )
    }

    @Test
    fun `a sort by real columns is left alone`() {
        val queryFilter = process(SortProperty("type"), SortProperty("bruttoMitAgAnteil"))
        assertEquals(
            listOf("type", "bruttoMitAgAnteil"),
            queryFilter.sortProperties.map { it.property },
        )
    }

    /**
     * The year/month single-choice filters are consumed here into real-column predicates and must be
     * marked synthetic, so [org.projectforge.framework.persistence.api.impl.MagicFilterProcessor] does
     * not also try to match them as generic properties.
     */
    @Test
    fun `the year and month filters are consumed and marked synthetic`() {
        val filter = MagicFilter()
        filter.entries.add(MagicFilterEntry("year", "2025"))
        filter.entries.add(MagicFilterEntry("month", "9"))
        val other = MagicFilterEntry("comment", "bonus")
        filter.entries.add(other)

        val result = EmployeeSalaryEntityRest().preProcessMagicFilter(QueryFilter(), filter)

        assertNull(result, "no custom in-memory result filter is added")
        assertTrue(filter.entries.first { it.field == "year" }.synthetic == true, "year consumed")
        assertTrue(filter.entries.first { it.field == "month" }.synthetic == true, "month consumed")
        assertEquals(false, other.synthetic, "an unrelated entry is left for the generic processor")
    }

    private fun process(vararg sortProperties: SortProperty): QueryFilter {
        val queryFilter = QueryFilter()
        queryFilter.sortProperties = sortProperties.toMutableList()
        // The filter the client sent isn't read by the mapping - only the QueryFilter built from it is.
        EmployeeSalaryEntityRest().postProcessMagicFilter(queryFilter, MagicFilter())
        return queryFilter
    }
}
