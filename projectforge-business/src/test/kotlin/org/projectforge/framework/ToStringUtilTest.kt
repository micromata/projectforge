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

package org.projectforge.web

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.task.TaskDO
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.json.JsonValidator
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.toJsonString
import org.projectforge.test.AbstractTestBase

class ToStringUtilTest : AbstractTestBase() {

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
}
