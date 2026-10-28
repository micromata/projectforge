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

package org.projectforge.rest.fibu

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjectWizardService
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.business.task.TaskWizardService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.springframework.beans.factory.annotation.Autowired

/**
 * The checks of the project wizard's endpoints, as field errors under the paths of
 * [ProjectWizardService.Data].
 */
class ProjectWizardRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projectWizardRest: ProjectWizardRest

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var taskWizardService: TaskWizardService

    @Test
    fun `missing and wrong values are field errors`() {
        logon(ADMIN_USER)
        groupDao.insert(GroupDO().also { it.name = "ProjectWizardRestTest-taken" })
        logon(TEST_FINANCE_USER)
        kundeDao.insert(KundeDO().also {
            it.nummer = 18602L
            it.name = "ProjectWizardRestTest"
        })
        Assertions.assertTrue(projectWizardRest.init().allowed)
        val answer = projectWizardRest.validate(
            ProjectWizardService.Data(
                // The number is taken:
                customer = ProjectWizardService.CustomerData(nummer = 18602L),
                project = ProjectWizardService.ProjectData(name = "ProjectWizardRestTest"),
                teamGroup = ProjectWizardService.GroupData(name = "ProjectWizardRestTest-taken"),
                managerGroup = ProjectWizardService.GroupData(name = "ProjectWizardRestTest-twice"),
                externalGroup = ProjectWizardService.GroupData(name = "ProjectWizardRestTest-twice"),
            )
        ).body!!
        Assertions.assertEquals(
            setOf(
                "customer.name",
                "customer.nummer",
                "project.nummer",
                "parentTaskId",
                "teamGroup.name",
                "externalGroup.name",
            ),
            answer.validationErrors.mapNotNull { it.fieldId }.toSet(),
        )
        Assertions.assertNull(answer.preview, "No preview without a parent element.")
    }

    @Test
    fun `the preview shows the new element`() {
        logon(TEST_FINANCE_USER)
        val answer = projectWizardRest.validate(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18601L, name = "ProjectWizardRestTest"),
                project = ProjectWizardService.ProjectData(nummer = 1, name = "ProjectWizardRestTest-preview"),
                parentTaskId = taskTree.rootTaskNode.id,
                teamGroup = ProjectWizardService.GroupData(name = "ProjectWizardRestTest-preview"),
            )
        ).body!!
        Assertions.assertEquals(emptyList<String>(), answer.validationErrors.map { "${it.fieldId}: ${it.message}" })
        // The project's element and the new customer's above it, the root gets nothing:
        val entries = answer.preview!!.entries
        Assertions.assertEquals(listOf(null, null), entries.map { it.taskId })
        Assertions.assertEquals(listOf("CREATED", "CREATED"), entries.map { it.status })
        Assertions.assertEquals(
            listOf("ProjectWizardRestTest-preview", "ProjectWizardRestTest"),
            entries.map { it.taskTitle },
        )
        Assertions.assertEquals(listOf(true, false), entries.map { it.pickedElement })
    }

    @Test
    fun `elements of the same titles are used, not reported as duplicates`() {
        logon(ADMIN_USER)
        val parent = createTask("ProjectWizardRestTest-sameTitles-parent", taskTree.rootTaskNode.task!!)
        val customerTask = createTask("ProjectWizardRestTest-sameTitles", parent)
        val projectTask = createTask("ProjectWizardRestTest-sameTitles-project", customerTask)
        logon(TEST_FINANCE_USER)
        val answer = projectWizardRest.validate(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18606L, name = "ProjectWizardRestTest-sameTitles"),
                project = ProjectWizardService.ProjectData(nummer = 1, name = "ProjectWizardRestTest-sameTitles-project"),
                parentTaskId = parent.id,
                teamGroup = ProjectWizardService.GroupData(name = "ProjectWizardRestTest-sameTitles"),
            )
        ).body!!
        Assertions.assertEquals(emptyList<String>(), answer.validationErrors.map { "${it.fieldId}: ${it.message}" })
        Assertions.assertEquals(customerTask.id, answer.customerTaskId)
        Assertions.assertEquals(projectTask.id, answer.projectTaskId)
        // The rights on the existing element and its ancestors, no new element:
        val entries = answer.preview!!.entries
        Assertions.assertEquals(listOf(projectTask.id, customerTask.id, parent.id), entries.map { it.taskId })
        Assertions.assertEquals(listOf(true, false, false), entries.map { it.pickedElement })
    }

    @Test
    fun `an existing element of the project that can't be used is a field error`() {
        logon(ADMIN_USER)
        val customerTask = createTask("ProjectWizardRestTest-refused", taskTree.rootTaskNode.task!!)
        val outside = createTask("ProjectWizardRestTest-refused-outside", taskTree.rootTaskNode.task!!)
        val taken = createTask("ProjectWizardRestTest-refused-taken", customerTask)
        val nested = createTask("ProjectWizardRestTest-refused-nested", taken)
        logon(TEST_FINANCE_USER)
        val kunde = KundeDO().also {
            it.nummer = 18607L
            it.name = "ProjectWizardRestTest-refused"
        }
        kundeDao.insert(kunde)
        projektDao.insert(ProjektDO().also {
            it.name = "ProjectWizardRestTest-refused-taken"
            it.kunde = kunde
            it.nummer = 1
            it.task = taken
        })
        for (projectTaskId in listOf(outside.id, taken.id, nested.id, null)) {
            val answer = projectWizardRest.validate(
                ProjectWizardService.Data(
                    customer = ProjectWizardService.CustomerData(existing = true, id = 18607L),
                    project = ProjectWizardService.ProjectData(nummer = 2, name = "ProjectWizardRestTest-refused-p"),
                    parentTaskId = customerTask.id,
                    projectTaskExisting = true,
                    projectTaskId = projectTaskId,
                )
            ).body!!
            Assertions.assertEquals(
                listOf("projectTaskId"),
                answer.validationErrors.map { it.fieldId },
                "Task #$projectTaskId",
            )
            Assertions.assertNull(answer.preview)
        }
        // A new element of the title of the one with a project would be the same one:
        val answer = projectWizardRest.validate(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(existing = true, id = 18607L),
                project = ProjectWizardService.ProjectData(nummer = 2, name = "ProjectWizardRestTest-refused-taken"),
                parentTaskId = customerTask.id,
            )
        ).body!!
        Assertions.assertEquals(listOf("projectTaskId"), answer.validationErrors.map { it.fieldId })
        Assertions.assertNull(answer.projectTaskId, "Not reported as used, it is refused.")
    }

    @Test
    fun `a picked element of another project is warned about, not refused`() {
        logon(ADMIN_USER)
        val taken = createTask("ProjectWizardRestTest-warning", taskTree.rootTaskNode.task!!)
        logon(TEST_FINANCE_USER)
        val kunde = KundeDO().also {
            it.nummer = 18610L
            it.name = "ProjectWizardRestTest-warning"
        }
        kundeDao.insert(kunde)
        projektDao.insert(ProjektDO().also {
            it.name = "ProjectWizardRestTest-warning-taken"
            it.kunde = kunde
            it.nummer = 1
            it.task = taken
        })
        val answer = projectWizardRest.validate(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18611L, name = "ProjectWizardRestTest-warned"),
                project = ProjectWizardService.ProjectData(nummer = 1, name = "ProjectWizardRestTest-warned"),
                customerTaskExisting = true,
                customerTaskId = taken.id,
            )
        ).body!!
        Assertions.assertEquals(emptyList<String>(), answer.validationErrors.map { "${it.fieldId}: ${it.message}" })
        Assertions.assertEquals(taken.id, answer.customerTaskId)
        Assertions.assertTrue(
            answer.customerTaskWarning?.contains("ProjectWizardRestTest-warning-taken") == true,
            answer.customerTaskWarning,
        )
    }

    @Test
    fun `a deleted element of the new title is a field error, not a failure at Finish`() {
        logon(ADMIN_USER)
        val parent = createTask("ProjectWizardRestTest-deleted-parent", taskTree.rootTaskNode.task!!)
        val deleted = createTask("ProjectWizardRestTest-deleted", parent)
        taskDao.markAsDeleted(deleted)
        logon(TEST_FINANCE_USER)
        val answer = projectWizardRest.validate(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18608L, name = "ProjectWizardRestTest-deleted"),
                parentTaskId = parent.id,
            )
        ).body!!
        Assertions.assertEquals(listOf("customerTaskId"), answer.validationErrors.map { it.fieldId })
        Assertions.assertNull(answer.customerTaskId)
    }

    @Test
    fun `an element found by title the user may not see is a field error`() {
        logon(ADMIN_USER)
        val parent = createTask("ProjectWizardRestTest-unseen-parent", taskTree.rootTaskNode.task!!)
        val sibling = createTask("ProjectWizardRestTest-unseen-sibling", parent)
        createTask("ProjectWizardRestTest-unseen", parent)
        val user = initTestDB.addUser(PFUserDO().also {
            it.username = "ProjectWizardRestTest.unseen"
            it.addRight(UserRightDO(UserRightId.FIBU_CUSTOMER, UserRightValue.READWRITE))
        })
        val orga = getGroup(ORGA_GROUP)
        orga.addUser(user)
        groupDao.update(orga)
        val group = GroupDO().also { it.name = "ProjectWizardRestTest-unseen" }
        groupDao.setAssignedUsers(group, listOf(user))
        groupDao.insert(group)
        // The sibling with its rights, the parent with read access only: the other child stays hidden.
        taskWizardService.grantAccess(taskId = sibling.id!!, teamGroupId = group.id)
        logon(user)
        Assertions.assertTrue(projectWizardRest.init().customerAllowed)
        val answer = projectWizardRest.validate(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18609L, name = "ProjectWizardRestTest-unseen"),
                parentTaskId = parent.id,
            )
        ).body!!
        Assertions.assertEquals(listOf("customerTaskId"), answer.validationErrors.map { it.fieldId })
        Assertions.assertNull(answer.customerTaskId)
    }

    @Test
    fun `users without the rights are refused`() {
        for (user in listOf(TEST_USER, TEST_PROJECT_MANAGER_USER)) {
            logon(user)
            Assertions.assertFalse(projectWizardRest.init().allowed, user)
            Assertions.assertThrows(AccessException::class.java, {
                projectWizardRest.validate(ProjectWizardService.Data())
            }, user)
            Assertions.assertThrows(AccessException::class.java, {
                projectWizardRest.execute(ProjectWizardService.Data())
            }, user)
        }
    }

    /** The preview would show the titles of its ancestors and the rows of the groups on them. */
    @Test
    fun `no preview below an element the user may not see`() {
        logon(ADMIN_USER)
        val hidden = TaskDO().also {
            it.title = "ProjectWizardRestTest-hidden"
            it.parentTask = taskTree.rootTaskNode.task
        }
        taskDao.insert(hidden)
        val child = TaskDO().also {
            it.title = "ProjectWizardRestTest-hidden-child"
            it.parentTask = hidden
        }
        taskDao.insert(child)
        val user = initTestDB.addUser(PFUserDO().also {
            it.username = "ProjectWizardRestTest.orga"
            it.addRight(UserRightDO(UserRightId.PM_PROJECT, UserRightValue.READWRITE))
        })
        val group = getGroup(ORGA_GROUP)
        group.addUser(user)
        groupDao.update(group)
        val data = ProjectWizardService.Data(
            customer = ProjectWizardService.CustomerData(id = 18605L),
            project = ProjectWizardService.ProjectData(nummer = 1, name = "ProjectWizardRestTest-hidden-child"),
            parentTaskId = child.id,
            teamGroup = ProjectWizardService.GroupData(name = "ProjectWizardRestTest-hidden"),
        )
        logon(user)
        Assertions.assertTrue(projectWizardRest.init().projectAllowed)
        val answer = projectWizardRest.validate(data).body!!
        Assertions.assertNull(answer.preview)
        Assertions.assertTrue(answer.validationErrors.any { it.fieldId == "parentTaskId" })
        // The finance group sees every element:
        logon(TEST_FINANCE_USER)
        Assertions.assertNotNull(projectWizardRest.validate(data).body!!.preview)
    }

    @Test
    fun `finance users without the customer right may create projects only`() {
        logon(TEST_ADMIN_USER)
        val user = initTestDB.addUser(PFUserDO().also {
            it.username = "ProjectWizardRestTest.finance"
            it.addRight(UserRightDO(UserRightId.PM_PROJECT, UserRightValue.READWRITE))
        })
        val group = getGroup(FINANCE_GROUP)
        group.addUser(user)
        groupDao.update(group)
        logon(user)
        val init = projectWizardRest.init()
        Assertions.assertTrue(init.allowed)
        Assertions.assertFalse(init.customerAllowed)
        Assertions.assertTrue(init.projectAllowed)
        Assertions.assertThrows(AccessException::class.java) {
            projectWizardRest.execute(
                ProjectWizardService.Data(
                    customer = ProjectWizardService.CustomerData(nummer = 18603L, name = "ProjectWizardRestTest"),
                    project = ProjectWizardService.ProjectData(nummer = 1, name = "ProjectWizardRestTest-finance"),
                    parentTaskId = taskTree.rootTaskNode.id,
                )
            )
        }
    }

    @Test
    fun `a customer alone needs neither project nor parent`() {
        logon(TEST_FINANCE_USER)
        val init = projectWizardRest.init()
        Assertions.assertTrue(init.customerAllowed && init.projectAllowed)
        val answer = projectWizardRest.validate(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18604L, name = "ProjectWizardRestTest-alone"),
            )
        ).body!!
        Assertions.assertEquals(emptyList<String>(), answer.validationErrors.map { "${it.fieldId}: ${it.message}" })
        Assertions.assertNull(answer.preview)
    }

    private fun createTask(title: String, parent: TaskDO): TaskDO = TaskDO().also {
        it.title = title
        it.parentTask = parent
        taskDao.insert(it)
    }
}
