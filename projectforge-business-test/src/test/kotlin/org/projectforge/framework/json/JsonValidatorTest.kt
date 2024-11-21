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

package org.projectforge.framework.json

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.task.TaskDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.toJsonString
import kotlin.text.get

class JsonValidatorTest : AbstractTestBase() {

    @Test
    fun jsonTest() {
        val rootTask = TaskDO()
        rootTask.title = "Root"
        val task = TaskDO()
        task.title = "task"
        task.parentTask = rootTask
        val user = PFUserDO()
        user.username = "kai"
        user.id = 42
        user.firstname = "Kai"
        task.responsibleUser = user
        var jsonValidator = JsonValidator(toJsonString(task))
        Assertions.assertEquals(42.0, jsonValidator.getDouble("responsibleUser.id"))
        Assertions.assertEquals("kai", jsonValidator.get("responsibleUser.username"))
        Assertions.assertNull(jsonValidator.get("responsibleUser.firstname"), "Firstname shouldn't be serialized.")

        jsonValidator = JsonValidator(toJsonString(task, PFUserDO::class.java))
        Assertions.assertEquals("Kai", jsonValidator.get("responsibleUser.firstname"), "Firstname shouldn't be ignored.")

        val timesheet = TimesheetDO()
        timesheet.user = user
        timesheet.task = task

        jsonValidator = JsonValidator(toJsonString(timesheet))
        Assertions.assertEquals(42.0, jsonValidator.getDouble("user.id"))
        Assertions.assertEquals("kai", jsonValidator.get("user.username"))
        Assertions.assertNull(jsonValidator.get("user.firstname"), "Firstname shouldn't be serialized.")
    }

    @Test
    fun parseJsonTest() {
        val jsonValidator = JsonValidator("{'fruit1':'apple','fruit2':'orange','basket':{'fruit3':'cherry','fruit4':'banana'},'actions':[{'id':'cancel','title':'Abbrechen','style':'danger','type':'button','key':'el-20'},{'id':'create','title':'Anlegen','style':'primary','type':'button','key':'el-21'}]}")
        Assertions.assertEquals("apple", jsonValidator.get("fruit1"))
        Assertions.assertEquals("orange", jsonValidator.get("fruit2"))
        Assertions.assertEquals("cherry", jsonValidator.get("basket.fruit3"))

        Assertions.assertNull(jsonValidator.get("fruit3"))
        Assertions.assertNull(jsonValidator.get("basket.fruit1"))

        var ex = Assertions.assertThrows(IllegalArgumentException::class.java) {
            jsonValidator.get("basket.unknown.fruit1")
        }
        Assertions.assertEquals("Can't step so deep: 'basket.unknown.fruit1'. 'fruit1' doesn't exist.", ex.message)

        Assertions.assertNull(jsonValidator.get("basket.unknown"))

        Assertions.assertEquals("cancel", jsonValidator.get("actions[0].id"))
        Assertions.assertEquals("Anlegen", jsonValidator.get("actions[1].title"))
        Assertions.assertEquals(2, jsonValidator.getList("actions")?.size)

        ex = Assertions.assertThrows(IllegalArgumentException::class.java) {
            jsonValidator.get("actions.id")
        }
        Assertions.assertEquals("Can't step so deep: 'actions.id'. 'id' doesn't exist.", ex.message)
        Assertions.assertEquals("cancel", jsonValidator.get("actions[0].id"))
    }
}
