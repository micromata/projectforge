package org.projectforge.web

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.task.TaskDO
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.JsonValidator
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
