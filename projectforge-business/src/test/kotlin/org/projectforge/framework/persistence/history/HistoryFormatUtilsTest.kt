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

package org.projectforge.framework.persistence.history

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class HistoryFormatUtilsTest {
    @Suppress("unused")
    private class IdListEntity {
        @HistoryIdList(HistoryIdList.Type.USER)
        var ownerIds: String? = null

        @HistoryIdList(HistoryIdList.Type.GROUP)
        var attendeeIds: String? = null

        var fullAccessUserIds: String? = null

        var comment: String? = null
    }

    @Test
    fun testGetIdListType() {
        val clazz = IdListEntity::class.java
        Assertions.assertEquals(HistoryIdList.Type.USER, HistoryFormatUtils.getIdListType(clazz, "ownerIds"))
        Assertions.assertEquals(HistoryIdList.Type.GROUP, HistoryFormatUtils.getIdListType(clazz, "attendeeIds"))
        Assertions.assertEquals(HistoryIdList.Type.USER, HistoryFormatUtils.getIdListType(clazz, "fullAccessUserIds"))
        Assertions.assertEquals(HistoryIdList.Type.GROUP, HistoryFormatUtils.getIdListType(null, "readonlyAccessGroupIds"))
        Assertions.assertNull(HistoryFormatUtils.getIdListType(clazz, "comment"))
        Assertions.assertNull(HistoryFormatUtils.getIdListType(clazz, null))
        Assertions.assertNull(HistoryFormatUtils.getIdListType(null, "ownerIds"))
    }

    @Test
    fun testGetIdListNames() {
        val names = mapOf(1L to "Zoe Zimmer", 2L to "Anna Adams")
        val nameOf: (Long) -> String? = { names[it] }
        Assertions.assertEquals(listOf("Anna Adams", "Zoe Zimmer"), HistoryFormatUtils.getIdListNames("1,2", nameOf))
        Assertions.assertEquals(listOf("#3", "Anna Adams"), HistoryFormatUtils.getIdListNames(" 2, 3 ", nameOf))
        Assertions.assertEquals(listOf("Zoe Zimmer"), HistoryFormatUtils.getIdListNames("1", nameOf))
        // Already formatted or not an id list:
        Assertions.assertNull(HistoryFormatUtils.getIdListNames("Anna Adams, Zoe Zimmer", nameOf))
        Assertions.assertEquals(emptyList<String>(), HistoryFormatUtils.getIdListNames(null, nameOf))
        Assertions.assertEquals(emptyList<String>(), HistoryFormatUtils.getIdListNames("", nameOf))
        Assertions.assertEquals(emptyList<String>(), HistoryFormatUtils.getIdListNames("null", nameOf))
    }

    @Test
    fun testSetListValues() {
        DisplayHistoryEntryAttr().also { attr ->
            attr.setListValues(listOf("Anna", "Bert", "Carl"), listOf("Bert", "Carl", "Dora"))
            Assertions.assertEquals(listOf("Anna"), attr.removedValues)
            Assertions.assertEquals(listOf("Dora"), attr.addedValues)
        }
        DisplayHistoryEntryAttr().also { attr ->
            attr.setListValues(emptyList(), listOf("Anna"))
            Assertions.assertEquals(emptyList<String>(), attr.removedValues)
            Assertions.assertEquals(listOf("Anna"), attr.addedValues)
        }
        DisplayHistoryEntryAttr().also { attr ->
            // No difference: nothing to show as diff.
            attr.setListValues(listOf("Anna", "Bert"), listOf("Bert", "Anna"))
            Assertions.assertNull(attr.removedValues)
            Assertions.assertNull(attr.addedValues)
        }
    }

    @Test
    fun testSetPropertyForListEntries() {
        assertAndTest("pos#1", "property", "pos", 1)
        assertAndTest("pos", "property", "pos")
        assertAndTest("pos", "property", Pair("pos", null))
        assertAndTest("pos#1", "property", Pair("pos", 1))
        assertAndTest("pos#1.kost1#2", "property", Pair("pos", 1), Pair("kost1", 2))
        assertAndTest("pos.kost1#2", "property", Pair("pos", null), Pair("kost1", 2))
        assertAndTest("pos.kost1", "property", Pair("pos", null), Pair("kost1", null))
    }

    private fun assertAndTest(expected: String, propertyName: String, prefix: String, number: Number? = null) {
        Assertions.assertEquals(
            "$expected:$propertyName",
            HistoryFormatUtils.getPropertyNameForEmbedded(propertyName, prefix = prefix, number = number)
        )
        Assertions.assertEquals(
            expected,
            HistoryFormatUtils.getPropertyNameForEmbedded(null, prefix = prefix, number = number)
        )
    }

    private fun assertAndTest(expected: String, propertyName: String, vararg prefixes: Pair<String, Number?>) {
        Assertions.assertEquals(
            "$expected:$propertyName",
            HistoryFormatUtils.getPropertyNameForEmbedded(propertyName, prefixes = prefixes)
        )
        Assertions.assertEquals(
            expected,
            HistoryFormatUtils.getPropertyNameForEmbedded(null, prefixes = prefixes)
        )
    }
}
