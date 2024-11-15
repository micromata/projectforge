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

package org.projectforge.framework.persistence.history

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class HistoryFormatUtilsTest {
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
