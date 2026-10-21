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


package org.projectforge.framework.json

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JsonMinifyTest {
    @Test
    fun `whitespace between tokens is removed, numbers and key order are kept`() {
        val json = "{\n  \"b\": 1.10,\n  \"a\": [ true, null,\r\n\t2e3 ]\n}"
        assertEquals("{\"b\":1.10,\"a\":[true,null,2e3]}", JsonUtils.minify(json))
    }

    @Test
    fun `whitespace and escaped quotes inside strings are kept`() {
        val json = "{ \"text\": \"a  b \\\" c\\\\\" , \"x\": \" \" }"
        assertEquals("{\"text\":\"a  b \\\" c\\\\\",\"x\":\" \"}", JsonUtils.minify(json))
    }

    @Test
    fun `compact json stays as it is`() {
        val json = "{\"a\":\"b c\",\"d\":[1,2]}"
        assertEquals(json, JsonUtils.minify(json))
    }
}
