/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.utils

import jakarta.persistence.Tuple
import jakarta.persistence.TupleElement
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.TestSetup
import java.time.LocalDate
import java.time.Year
import java.util.*

class SQLHelperTest {
    @Test
    fun getYearsTest() {
        TestSetup.init()
        val currentYear = Year.now().value
        assertYears(null, null, intArrayOf(currentYear))
        assertYears(2022, null, intArrayOf(2022))
        assertYears(null, 2022, intArrayOf(2022))
        assertYears(2022, 2022, intArrayOf(2022))
        assertYears(2022, 2023, intArrayOf(2022, 2023))
        assertYears(2022, 2025, intArrayOf(2022, 2023, 2024, 2025))
    }

    @Test
    fun `test splitting of sql scripts in executable statements`() {
        val sql = """
            CREATE TABLE test (
                id INT PRIMARY KEY,
                name VARCHAR(255) -- comment;
            );
            INSERT INTO test (id, name) VALUES (1, 'test');
            INSERT INTO test (id, name) VALUES (2, 'test2;');
        """.trimIndent()
        val statements = SQLHelper.splitSqlStatements(sql)
        Assertions.assertEquals(3, statements.size)
        Assertions.assertEquals("CREATE TABLE test (\n    id INT PRIMARY KEY,\n    name VARCHAR(255) \n);", statements[0])
        Assertions.assertEquals("INSERT INTO test (id, name) VALUES (1, 'test');", statements[1])
        Assertions.assertEquals("INSERT INTO test (id, name) VALUES (2, 'test2;');", statements[2])
    }

    private fun assertYears(year1: Int?, year2: Int?, expectedIntArray: IntArray) {
        Assertions.assertArrayEquals(SQLHelper.getYears(year1, year2), expectedIntArray)
        Assertions.assertArrayEquals(
            SQLHelper.getYearsByTupleOfDate(MyTuple(createDate(year1), createDate(year2))),
            expectedIntArray,
        )
        Assertions.assertArrayEquals(
            SQLHelper.getYearsByTupleOfLocalDate(MyTuple(createLocalDate(year1), createLocalDate(year2))),
            expectedIntArray,
        )
    }

    private fun createDate(year: Int?): Date? {
        return if (year == null) {
            null
        } else {
            GregorianCalendar(year, 0, 1).getTime()
        }
    }

    private fun createLocalDate(year: Int?): LocalDate? {
        return if (year == null) {
            null
        } else {
            LocalDate.of(year, 1, 1)
        }
    }

    private class MyTuple<X>(val first: X?, val second: X?) : Tuple {
        override fun <X> get(arg0: Int, arg1: Class<X>): X {
            throw UnsupportedOperationException()
        }

        override fun get(arg0: Int): X? {
            return if (arg0 == 0) {
                first
            } else {
                second
            }
        }

        override fun toArray(): Array<X> {
            throw UnsupportedOperationException()
        }

        override fun getElements(): MutableList<TupleElement<X>> {
            throw UnsupportedOperationException()
        }

        override fun <X : Any?> get(tupleElement: TupleElement<X>?): X {
            throw UnsupportedOperationException()
        }

        override fun <X : Any?> get(alias: String?, type: Class<X>?): X {
            throw UnsupportedOperationException()
        }

        override fun get(arg0: String): Any {
            throw UnsupportedOperationException()
        }
    }
}
