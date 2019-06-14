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

package org.projectforge.framework

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonValidatorTest {

    @Test
    fun parseJson() {
        val jsonValidator = JsonValidator("{'fruit1':'apple','fruit2':'orange','basket':{'fruit3':'cherry','fruit4':'banana'},'actions':[{'id':'cancel','title':'Abbrechen','style':'danger','type':'button','key':'el-20'},{'id':'create','title':'Anlegen','style':'primary','type':'button','key':'el-21'}]}")
        assertEquals("apple", jsonValidator.get("fruit1"))
        assertEquals("orange", jsonValidator.get("fruit2"))
        assertEquals("cherry", jsonValidator.get("basket.fruit3"))

        assertNull(jsonValidator.get("fruit3"))
        assertNull(jsonValidator.get("basket.fruit1"))

        var ex = assertThrows(IllegalArgumentException::class.java) {
            jsonValidator.get("basket.unknown.fruit1")
        }
        assertEquals("Can't step so deep: 'basket.unknown.fruit1'. 'fruit1' doesn't exist.", ex.message)

        assertNull(jsonValidator.get("basket.unknown"))

        assertEquals("cancel", jsonValidator.get("actions[0].id"))
        assertEquals("Anlegen", jsonValidator.get("actions[1].title"))
        assertEquals(2, jsonValidator.getList("actions")?.size)

        ex = assertThrows(IllegalArgumentException::class.java) {
            jsonValidator.get("actions.id")
        }
        assertEquals("Can't step so deep: 'actions.id'. 'id' doesn't exist.", ex.message)
        assertEquals("cancel", jsonValidator.get("actions[0].id"))
    }
}
