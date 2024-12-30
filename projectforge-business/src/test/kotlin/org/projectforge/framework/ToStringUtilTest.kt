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

package org.projectforge.framework

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.framework.json.JsonTestUtils
import org.projectforge.framework.json.JsonUtils

class ToStringUtilTest : AbstractTestBase() {
    @Test
    fun `test IdOnlySerializers`() {
        val test = JsonTestUtils()
        var json = toJsonString(test.vacation, preferEmbeddedSerializers = false, ignoreIdOnlySerializers = false)
        JsonUtils.fromJson(json, test.vacation.javaClass).let { vacation ->
            assertVacation(vacation)
        }
        Assertions.assertFalse(
            json.contains("Abteilung"),
            "Abteilung shouldn't be serialized, only id's of employee's expected."
        )
        Assertions.assertFalse(
            json.contains("user"),
            "usernames shouldn't be serialized, only id's of user's expected."
        )
        json = toJsonString(test.vacation, preferEmbeddedSerializers = true, ignoreIdOnlySerializers = false)
        JsonUtils.fromJson(json, test.vacation.javaClass, failOnUnknownProps = false).let { vacation ->
            assertVacation(vacation)
        }
        Assertions.assertFalse(
            json.contains("Abteilung"),
            "Abteilung shouldn't be serialized, only id's of employee's expected."
        )
        Assertions.assertTrue(
            json.contains("user"),
            "usernames should be serialized, because of PFUserDO serializer."
        )
        json = toJsonString(test.vacation, preferEmbeddedSerializers = false, ignoreIdOnlySerializers = true)
        JsonUtils.fromJson(json, test.vacation.javaClass, failOnUnknownProps = false).let { vacation ->
            assertVacation(vacation)
        }
        Assertions.assertFalse(
            json.contains("Abteilung"),
            "Abteilung shouldn't be serialized, only id's of employee's expected."
        )
        Assertions.assertTrue(
            json.contains("user"),
            "usernames should be serialized, because of PFUserDO serializer."
        )
    }

    private fun assertVacation(vacation: VacationDO?) {
        Assertions.assertNotNull(vacation)
        Assertions.assertEquals("This is a comment", vacation!!.comment)
        Assertions.assertEquals(101L, vacation.employee?.id)
        Assertions.assertEquals(102L, vacation.manager?.id)
        Assertions.assertEquals(102L, vacation.replacement?.id)
        val others = vacation.otherReplacements
        Assertions.assertEquals(2, others!!.size)
    }
}
