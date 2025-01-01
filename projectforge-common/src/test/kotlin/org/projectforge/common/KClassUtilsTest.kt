/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KClassUtilsTest {
    class TestClass {
        val readonlyValue: String = ""
        var mutableValue: String = ""
        var nullableValue: String? = null
        var status: String? = null
            internal set

        companion object {
            val staticValue: String = ""
            var staticMutableValue: String = ""
        }
    }

    class InnerClass(val innerStatus: String, var innerValue: String?)

    @Test
    fun `test getProperty with nested properties`() {
        KClassUtils.getProperty(TestClass::class, "status").let { property ->
            Assertions.assertNotNull(property)
            Assertions.assertEquals("status", property!!.name)
            Assertions.assertEquals("kotlin.String?", property.returnType.toString())
        }
    }

    @Test
    fun testFilterPublicMutableProperties() {
        KClassUtils.filterPublicMutableProperties(TestClass::class).let { members ->
            Assertions.assertEquals(2, members.size)
            Assertions.assertTrue(members.any{ it.name == "mutableValue" })
            Assertions.assertTrue(members.any{ it.name == "nullableValue" })
        }
    }
}
