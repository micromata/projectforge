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

package org.projectforge.business.user

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.vacation.model.VacationDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class UserPrefDaoTest : AbstractTestBase() {
    @Test
    fun `test deserialization of vacation`() {
        val json = """{
           |  "employee": {
           |    "id": 1
           |  },
           |  "replacement": {
           |    "id": 2
           |  },
           |  "manager": {
           |    "id": 2
           |  }
           |}""".trimMargin()
        UserPrefDao.fromJson(json, VacationDO::class.java).let { vacation ->
            Assertions.assertNotNull(vacation)
            Assertions.assertEquals(1, vacation!!.employee?.id)
            Assertions.assertEquals(2, vacation.replacement?.id)
            Assertions.assertEquals(2, vacation.manager?.id)

        }
    }

    @Test
    fun `test serialization of vacation`() {
        val employee1 = EmployeeDO().also { employee ->
            employee.id = 101
            employee.abteilung = "Abteilung 1"
            employee.user = PFUserDO().also {
                it.id = 1
                it.username = "user1"
            }
        }
        val employee2 = EmployeeDO().also { employee ->
            employee.id = 102
            employee.abteilung = "Abteilung 2"
            employee.user = PFUserDO().also {
                it.id = 2
                it.username = "user2"
            }
        }
        val employee3 = EmployeeDO().also { employee ->
            employee.id = 103
            employee.abteilung = "Abteilung 3"
            employee.user = PFUserDO().also {
                it.id = 3
                it.username = "user3"
            }
        }
        VacationDO().let { vacation ->
            vacation.id = 5
            vacation.comment = "This is a comment"
            vacation.employee = employee1
            vacation.replacement = employee2
            vacation.manager = employee2
            vacation.otherReplacements = mutableSetOf(employee2, employee3)
            Assertions.assertEquals(vacation, vacation)
            val json = UserPrefDao.serialize(vacation)
            Assertions.assertTrue { json.contains("\"comment\":\"This is a comment\"") }
            Assertions.assertTrue { json.contains("\"employee\":{\"id\":101}") }
            Assertions.assertTrue { json.contains("\"replacement\":{\"id\":102}") }
            Assertions.assertTrue { json.contains("\"manager\":{\"id\":102}") }
            Assertions.assertTrue { json.contains("\"otherReplacements\":[{\"id\":102},{\"id\":103}]") }

            UserPrefDao.fromJson(json, VacationDO::class.java).let {
                Assertions.assertNotNull(it)
                Assertions.assertEquals(5, it!!.id)
                Assertions.assertEquals("This is a comment", it.comment)
                Assertions.assertEquals(101, it.employee?.id)
                Assertions.assertEquals(102, it.replacement?.id)
                Assertions.assertEquals(102, it.manager?.id)
                Assertions.assertEquals(2, it.otherReplacements!!.size)
                Assertions.assertTrue(it.otherReplacements!!.any{ it.id == 102L })
                Assertions.assertTrue(it.otherReplacements!!.any{ it.id == 103L })
            }
        }
    }
}
