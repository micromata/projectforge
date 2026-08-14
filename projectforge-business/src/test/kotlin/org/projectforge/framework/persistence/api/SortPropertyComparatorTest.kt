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

package org.projectforge.framework.persistence.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SortPropertyComparatorTest {
    /** A row of a list: one value a database column could hold, one it could not. */
    class Row(val name: String?, val id: Int, val netSum: BigDecimal? = null)

    private fun sort(
        rows: List<Row>,
        vararg sortProperties: SortProperty,
        valueOf: ((Row, String) -> Any?)? = null,
    ): List<Int> {
        return SortPropertyComparator.sort(rows, sortProperties.toList(), valueOf).map { it.id }
    }

    @Test
    fun `sorts by a property in both directions`() {
        val rows = listOf(Row("b", 1), Row("a", 2), Row("c", 3))
        Assertions.assertEquals(listOf(2, 1, 3), sort(rows, SortProperty.asc("name")))
        Assertions.assertEquals(listOf(3, 1, 2), sort(rows, SortProperty.desc("name")))
    }

    @Test
    fun `keeps the list as it is without a sort property`() {
        val rows = listOf(Row("b", 1), Row("a", 2))
        Assertions.assertEquals(listOf(1, 2), sort(rows))
    }

    /**
     * Blank ranks lowest, so reversing the sort brings the entries without a value into view — the
     * behaviour of `DBQueryBuilderByCriteria.addOrder`, which this has to match.
     */
    @Test
    fun `ranks null and the empty string alike and lowest`() {
        val rows = listOf(Row("b", 1), Row(null, 2), Row("", 3), Row("a", 4))
        // The two blanks are equal for the first property, so the second decides between them.
        Assertions.assertEquals(
            listOf(2, 3, 4, 1),
            sort(rows, SortProperty.asc("name"), SortProperty.asc("id")),
        )
        Assertions.assertEquals(
            listOf(1, 4, 2, 3),
            sort(rows, SortProperty.desc("name"), SortProperty.asc("id")),
        )
    }

    @Test
    fun `the second property decides what the first leaves equal`() {
        val rows = listOf(Row("a", 3), Row("a", 1), Row("b", 2))
        Assertions.assertEquals(
            listOf(1, 3, 2),
            sort(rows, SortProperty.asc("name"), SortProperty.asc("id")),
        )
        Assertions.assertEquals(
            listOf(3, 1, 2),
            sort(rows, SortProperty.asc("name"), SortProperty.desc("id")),
        )
    }

    /**
     * What the order list needs it for: a sum that is no database column, so the caller reads it rather
     * than reflection (see `AuftragPagesRest.filterList`). Numbers compare as numbers — a string column
     * would sort "900,00" after "1.100,00".
     */
    @Test
    fun `sorts by a value the caller reads`() {
        val rows = listOf(
            Row("a", 1, BigDecimal("900.00")),
            Row("b", 2, BigDecimal("1100.00")),
            Row("c", 3, BigDecimal.ZERO),
        )
        val ids = sort(rows, SortProperty.desc("netSumme")) { row, property ->
            if (property == "netSumme") row.netSum else null
        }
        Assertions.assertEquals(listOf(2, 1, 3), ids)
    }

    /** A `null` from the caller means "no value of mine", so the reflective read answers instead. */
    @Test
    fun `falls back to the bean property where the caller has no value`() {
        val rows = listOf(Row("b", 1), Row("a", 2))
        Assertions.assertEquals(listOf(2, 1), sort(rows, SortProperty.asc("name")) { _, _ -> null })
    }

    /** A stored sort order outlives the column it names, so an unreadable property must not throw. */
    @Test
    fun `ignores a property that cannot be read`() {
        val rows = listOf(Row("b", 1), Row("a", 2))
        Assertions.assertEquals(
            listOf(2, 1),
            sort(rows, SortProperty.asc("noSuchProperty"), SortProperty.asc("name")),
        )
    }
}
