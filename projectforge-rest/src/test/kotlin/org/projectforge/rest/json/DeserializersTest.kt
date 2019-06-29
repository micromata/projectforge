/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeserializersTest {

    @Test
    fun textTest() {
        val mapper = ObjectMapper()
        val module = SimpleModule()
        module.addDeserializer(String::class.java, TextDeserializer())
        mapper.registerModule(module)
        assertEquals("", readText(mapper, ""))
        assertEquals("+49 30 12345678", readText(mapper, "\u202D+49 30 12345678\u202C"))
        assertEquals("test test", readText(mapper, " \u202D test \u202Ctest\u202C  "))
        assertEquals("äé 玻璃而不伤身体 faljd", readText(mapper, " \u202Däé 玻璃而不伤身\u202C体 faljd\u202C  "))
    }

    private fun readText(mapper: ObjectMapper, text: String?): String? {
        return mapper.readValue("\"$text\"", String::class.java)
    }
}
