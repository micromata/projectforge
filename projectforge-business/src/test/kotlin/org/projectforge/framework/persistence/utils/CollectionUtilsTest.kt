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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.api.IdObject

class CollectionUtilsTest {
    @Test
    fun testCompareLists() {
        val src1 = listOf("a", "b", "c", "z")
        val dest1 = listOf("b", "c", "d", "e")
        testCompareLists(src1, dest1, added = listOf("a", "z"), removed = listOf("d", "e"))
        testCompareLists(src1, null, added = src1, removed = null)
        testCompareLists(null, dest1, added = null, removed = dest1)
        testCompareLists(null, null, added = null, removed = null)

        val src2 = listOf(TestClass(1), TestClass(2), TestClass(3))
        val dest2 = listOf(TestClass(2), TestClass(3), TestClass(4))
        testCompareLists(src2, dest2, added = listOf(TestClass(1)), removed = listOf(TestClass(4)))
    }

    private fun testCompareLists(
        src: Collection<Any>?,
        dest: Collection<Any>?,
        added: Collection<Any>?,
        removed: Collection<Any>?
    ) {
        val result = CollectionUtils.compareLists(src, dest)
        Assertions.assertEquals(added?.size, result.added?.size)
        Assertions.assertEquals(removed?.size, result.removed?.size)
        added?.forEach { entry ->
            Assertions.assertTrue(
                result.added?.any { it.toString() == entry.toString() } == true,
                "$entry not contained in added=${result.added?.joinToString()}",
            )
        }
        removed?.forEach { entry ->
            Assertions.assertTrue(
                result.removed?.any { it.toString() == entry.toString() } == true,
                "$entry not contained in removed=${result.removed?.joinToString()}",
            )
        }
    }

    class TestClass(id: Long) : IdObject<Long> {
        override var id: Long? = id
        override fun toString(): String {
            return id?.toString() ?: "null"
        }
    }
}
