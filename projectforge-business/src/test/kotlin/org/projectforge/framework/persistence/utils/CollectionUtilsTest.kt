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
    fun testCompareCollections() {
        val src1 = listOf("b", "c", null, "z", "a")
        val dest1 = listOf("c", null, "e", "b", null, "d")
        testCompareCollections(src1, dest1, added = listOf("a", "z"), removed = listOf("d", "e"), kept = listOf("b", "c"))
        testCompareCollections(src1, null, added = src1.filterNotNull(), removed = null)
        testCompareCollections(null, dest1, added = null, removed = dest1.filterNotNull())
        testCompareCollections(null, null, added = null, removed = null)

        val src2 = listOf(TestClass(3), TestClass(name = "two"), TestClass(1))
        val dest2 = listOf(TestClass(42), TestClass(3), TestClass(4), TestClass(name = "two"))
        testCompareCollections(
            src2, dest2,
            added = listOf(TestClass(1)),
            removed = listOf(TestClass(4), TestClass(42)),
            kept = listOf(TestClass(null), TestClass(3))
        )
    }

    private fun testCompareCollections(
        src: Collection<Any?>?,
        dest: Collection<Any?>?,
        added: Collection<Any>?,
        removed: Collection<Any>?,
        kept: Collection<Any>? = null,
    ) {
        var result = CollectionUtils.compareCollections(src, dest)
        Assertions.assertEquals(asString(added), asString(result.added))
        Assertions.assertEquals(asString(removed), asString(result.removed))
        Assertions.assertNull(result.kept, "kept should be null, because it is not requested.")

        result = CollectionUtils.compareCollections(src, dest, withKept = true)
        Assertions.assertEquals(asString(added), asString(result.added))
        Assertions.assertEquals(asString(removed), asString(result.removed))
        Assertions.assertEquals(asString(kept), asString(result.kept))
    }

    private fun asString(col: Collection<Any>?): String {
        if (col.isNullOrEmpty()) {
            return ""
        }
        return if (col.first() is IdObject<*>) {
            @Suppress("UNCHECKED_CAST")
            CollectionUtils.joinToIdString(col as Collection<IdObject<Long>>)
        } else {
            col.sortedBy { it.toString() }.joinToString(separator = ",")
        }
    }

    class TestClass(override var id: Long? = null, var name: String? = null) : IdObject<Long> {
        override fun toString(): String {
            return id?.toString() ?: "null"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            other as TestClass
            return name == other.name
        }

        override fun hashCode(): Int {
            return name?.hashCode() ?: 0
        }
    }
}
