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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.business.task.TaskWizardService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.springframework.beans.factory.annotation.Autowired

/**
 * The project wizard, mostly as a user of the finance group with the rights for customers and projects
 * (who may create customers, projects and structure elements, but neither groups nor access rights
 * outside of the wizard), and with only one of the two rights.
 */
class ProjectWizardServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var projectWizardService: ProjectWizardService

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var taskWizardService: TaskWizardService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Test
    fun `a finance user sets up customer, project, element, groups and access rights`() {
        val parent = createParent("full")
        val art = kost2Art()
        logon(TEST_FINANCE_USER)
        Assertions.assertTrue(projectWizardService.isAllowed())
        val user2 = getUser("user2")
        val user3 = getUser("user3")
        val result = projectWizardService.execute(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18501L, name = "$PREFIX customer"),
                project = ProjectWizardService.ProjectData(
                    nummer = 1,
                    name = "$PREFIX-full-project",
                    kost2ArtIds = listOf(art.id!!),
                ),
                parentTaskId = parent.id,
                managerGroup = ProjectWizardService.GroupData(name = "$PREFIX-full-pm", userIds = listOf(user2.id!!)),
                teamGroup = ProjectWizardService.GroupData(
                    name = "$PREFIX-full",
                    userIds = listOf(user2.id!!, user3.id!!),
                ),
                externalGroup = ProjectWizardService.GroupData(name = "$PREFIX-full-external"),
            )
        )
        Assertions.assertTrue(result.customerCreated)
        Assertions.assertEquals(18501L, result.customerId)
        Assertions.assertEquals(3, result.groups.count { it.created })

        val projekt = projektDao.find(result.projectId, checkAccess = false)!!
        Assertions.assertEquals(18501L, projekt.kunde?.id)
        Assertions.assertEquals(result.taskId, projekt.task?.id)
        Assertions.assertEquals(185011, projekt.projektnummer, "Range 1: 1·10⁵ + (18501 − 10000)·10 + 1.")
        val managerGroup = result.groups.first { it.groupType == TaskWizardService.GroupType.MANAGER }
        Assertions.assertEquals(managerGroup.id, projekt.projektManagerGroup?.id)
        Assertions.assertEquals(result.projectId, taskTree.getProjekt(result.taskId)?.id)
        // The new customer's element below the chosen one, the project's below the customer's:
        val customerNode = taskTree.getTaskNodeById(result.customerTaskId)!!
        Assertions.assertEquals("$PREFIX customer", customerNode.task?.title)
        Assertions.assertEquals(parent.id, customerNode.parent?.id)
        Assertions.assertEquals(result.customerTaskId, taskTree.getTaskNodeById(result.taskId)?.parent?.id)
        Assertions.assertEquals(result.customerTaskId, projectWizardService.customerTaskOf(18501L))
        Assertions.assertEquals(
            listOf(art.id),
            kostCache.getKost2ForProjekt(result.projectId).map { it.kost2Art?.id },
        )
        val team = result.groups.first { it.groupType == TaskWizardService.GroupType.TEAM }
        Assertions.assertTrue(userGroupCache.isUserMemberOfGroup(user3.id, team.id))
        Assertions.assertFalse(userGroupCache.isUserMemberOfGroup(user3.id, managerGroup.id))

        // The new element gets the role's template recursively, its parent read access only:
        val task = taskDao.find(result.taskId, checkAccess = false)!!
        val teamAccess = accessDao.getEntry(task, GroupDO().also { it.id = team.id }, checkAccess = false)!!
        Assertions.assertTrue(teamAccess.recursive)
        Assertions.assertTrue(teamAccess.getAccessEntry(AccessType.OWN_TIMESHEETS)!!.accessInsert)
        val customerTask = taskDao.find(result.customerTaskId, checkAccess = false)!!
        val customerAccess = accessDao.getEntry(customerTask, GroupDO().also { it.id = team.id }, checkAccess = false)!!
        Assertions.assertFalse(customerAccess.recursive)
        Assertions.assertFalse(customerAccess.getAccessEntry(AccessType.OWN_TIMESHEETS)!!.accessInsert)
        Assertions.assertNotNull(accessDao.getEntry(parent, GroupDO().also { it.id = team.id }, checkAccess = false))
        Assertions.assertEquals(
            9,
            result.access!!.entries.size,
            "Project element, customer element and parent for each of the three groups.",
        )
    }

    @Test
    fun `a failure leaves nothing behind`() {
        val parent = createParent("rollback")
        logon(TEST_FINANCE_USER)
        val kunde = KundeDO().also {
            it.nummer = 18502L
            it.name = "$PREFIX rollback"
        }
        kundeDao.insert(kunde)
        projektDao.insert(ProjektDO().also {
            it.name = "$PREFIX taken"
            it.kunde = kunde
            it.nummer = 1
        })
        Assertions.assertThrows(UserException::class.java) {
            projectWizardService.execute(
                ProjectWizardService.Data(
                    customer = ProjectWizardService.CustomerData(id = 18502L),
                    // The number is taken, which ProjektDao only notices after element and group were written:
                    project = ProjectWizardService.ProjectData(nummer = 1, name = "$PREFIX-rollback-project"),
                    parentTaskId = parent.id,
                    teamGroup = ProjectWizardService.GroupData(name = "$PREFIX-rollback"),
                )
            )
        }
        logon(ADMIN_USER)
        Assertions.assertNull(taskTree.getTaskNodeById(parent.id)?.children?.find {
            it.task?.title == "$PREFIX-rollback-project"
        })
        Assertions.assertFalse(groupDao.doesGroupnameAlreadyExist(GroupDO().also { it.name = "$PREFIX-rollback" }))
    }

    @Test
    fun `an existing group with rights on an ancestor the user is no member of`() {
        val parent = createParent("existing")
        logon(ADMIN_USER)
        val group = GroupDO().also { it.name = "$PREFIX-existing" }
        groupDao.insert(group)
        // The access entry on the parent, which the finance user may not read in the access management:
        taskWizardService.grantAccess(taskId = parent.id!!, teamGroupId = group.id)
        logon(TEST_FINANCE_USER)
        val result = projectWizardService.execute(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18503L, name = "$PREFIX existing"),
                project = ProjectWizardService.ProjectData(nummer = 1, name = "$PREFIX-existing-project"),
                parentTaskId = parent.id,
                teamGroup = ProjectWizardService.GroupData(id = group.id),
            )
        )
        Assertions.assertFalse(result.groups.single().created)
        Assertions.assertEquals(
            listOf(
                TaskWizardService.AccessStatus.CREATED,
                // The element of the new customer:
                TaskWizardService.AccessStatus.CREATED,
                TaskWizardService.AccessStatus.UNCHANGED,
            ),
            result.access!!.entries.map { it.status },
            "The parent's recursive team rights are kept: the wizard only adds rights, it never takes any away.",
        )
        logon(ADMIN_USER)
        val access = accessDao.getEntry(parent, group, checkAccess = false)!!
        Assertions.assertTrue(access.recursive)
        Assertions.assertTrue(access.getAccessEntry(AccessType.OWN_TIMESHEETS)!!.accessInsert)
    }

    @Test
    fun `users without the right to create customers are refused`() {
        val parent = createParent("refused")
        logon(TEST_PROJECT_MANAGER_USER)
        Assertions.assertFalse(projectWizardService.isAllowed())
        Assertions.assertThrows(AccessException::class.java) {
            projectWizardService.execute(
                ProjectWizardService.Data(
                    customer = ProjectWizardService.CustomerData(nummer = 18504L, name = "$PREFIX refused"),
                    project = ProjectWizardService.ProjectData(nummer = 1, name = "$PREFIX-refused-project"),
                    parentTaskId = parent.id,
                )
            )
        }
    }

    @Test
    fun `finance users without the customer right may not create customers`() {
        val parent = createParent("noCustomerRight")
        logon(addUser("ProjectWizardServiceTest.finance", FINANCE_GROUP, UserRightId.PM_PROJECT))
        Assertions.assertTrue(projectWizardService.isAllowed(), "Projects for existing customers.")
        Assertions.assertFalse(projectWizardService.customerAllowed())
        Assertions.assertThrows(AccessException::class.java) {
            projectWizardService.execute(
                ProjectWizardService.Data(
                    customer = ProjectWizardService.CustomerData(nummer = 18505L, name = "$PREFIX noCustomerRight"),
                    project = ProjectWizardService.ProjectData(nummer = 1, name = "$PREFIX-noCustomerRight-project"),
                    parentTaskId = parent.id,
                )
            )
        }
    }

    @Test
    fun `the rights for customers and projects are enough, no admin needed`() {
        logon(
            addUser("ProjectWizardServiceTest.orga", ORGA_GROUP, UserRightId.PM_PROJECT, UserRightId.FIBU_CUSTOMER)
        )
        Assertions.assertTrue(projectWizardService.isAllowed())
    }

    @Test
    fun `the customer right alone creates a customer, with or without its element`() {
        val parent = createParent("customerOnly")
        val user = addUser("ProjectWizardServiceTest.customerOnly", FINANCE_GROUP, UserRightId.FIBU_CUSTOMER)
        logon(user)
        Assertions.assertTrue(projectWizardService.isAllowed())
        Assertions.assertFalse(projectWizardService.projectAllowed())
        val alone = projectWizardService.execute(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18506L, name = "$PREFIX customerOnly"),
            )
        )
        Assertions.assertEquals(18506L, alone.customerId)
        Assertions.assertNull(alone.customerTaskId)
        Assertions.assertNull(alone.projectId)
        Assertions.assertNull(alone.access)
        Assertions.assertNotNull(kundeDao.find(18506L, checkAccess = false))

        val withElement = projectWizardService.execute(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18507L, name = "$PREFIX customerElement"),
                parentTaskId = parent.id,
            )
        )
        val customerNode = taskTree.getTaskNodeById(withElement.customerTaskId)!!
        Assertions.assertEquals("$PREFIX customerElement", customerNode.task?.title)
        Assertions.assertEquals(parent.id, customerNode.parent?.id)
        Assertions.assertTrue(customerNode.children.isNullOrEmpty())

        // A project needs the project right:
        Assertions.assertThrows(AccessException::class.java) {
            projectWizardService.execute(
                ProjectWizardService.Data(
                    customer = ProjectWizardService.CustomerData(nummer = 18508L, name = "$PREFIX customerProject"),
                    project = ProjectWizardService.ProjectData(nummer = 1, name = "$PREFIX-customerOnly-project"),
                    parentTaskId = parent.id,
                )
            )
        }
    }

    @Test
    fun `the project right alone sets up a project for an existing customer`() {
        val parent = createParent("projectOnly")
        logon(TEST_FINANCE_USER)
        kundeDao.insert(KundeDO().also {
            it.nummer = 18509L
            it.name = "$PREFIX projectOnly"
        })
        logon(addUser("ProjectWizardServiceTest.projectOnly", FINANCE_GROUP, UserRightId.PM_PROJECT))
        Assertions.assertTrue(projectWizardService.isAllowed())
        Assertions.assertFalse(projectWizardService.customerAllowed())
        val result = projectWizardService.execute(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(existing = true, id = 18509L),
                project = ProjectWizardService.ProjectData(nummer = 1, name = "$PREFIX-projectOnly-project"),
                parentTaskId = parent.id,
                teamGroup = ProjectWizardService.GroupData(name = "$PREFIX-projectOnly"),
            )
        )
        Assertions.assertFalse(result.customerCreated)
        Assertions.assertEquals(parent.id, taskTree.getTaskNodeById(result.taskId)?.parent?.id)
        Assertions.assertTrue(result.groups.single().created)
    }

    @Test
    fun `without groups the project gets no access rights`() {
        val parent = createParent("noGroups")
        logon(TEST_FINANCE_USER)
        val result = projectWizardService.execute(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18510L, name = "$PREFIX noGroups"),
                project = ProjectWizardService.ProjectData(nummer = 1, name = "$PREFIX-noGroups-project"),
                parentTaskId = parent.id,
            )
        )
        Assertions.assertNotNull(result.projectId)
        Assertions.assertTrue(result.groups.isEmpty())
        Assertions.assertTrue(result.access!!.entries.isEmpty())
    }

    @Test
    fun `groups without a project are refused`() {
        logon(TEST_FINANCE_USER)
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            projectWizardService.execute(
                ProjectWizardService.Data(
                    customer = ProjectWizardService.CustomerData(nummer = 18511L, name = "$PREFIX groupsOnly"),
                    teamGroup = ProjectWizardService.GroupData(name = "$PREFIX-groupsOnly"),
                )
            )
        }
    }

    @Test
    fun `a picked element of another title becomes the new customer's`() {
        val parent = createParent("pickedCustomer")
        val customerTask = createTask("$PREFIX pickedCustomer element", parent)
        logon(TEST_FINANCE_USER)
        val result = projectWizardService.execute(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18512L, name = "$PREFIX pickedCustomer Inc."),
                project = ProjectWizardService.ProjectData(nummer = 1, name = "$PREFIX-pickedCustomer-project"),
                customerTaskExisting = true,
                customerTaskId = customerTask.id,
            )
        )
        Assertions.assertEquals(customerTask.id, result.customerTaskId)
        Assertions.assertFalse(result.customerTaskCreated)
        Assertions.assertEquals("$PREFIX pickedCustomer element", result.customerTaskTitle)
        Assertions.assertTrue(result.taskCreated)
        Assertions.assertEquals(customerTask.id, taskTree.getTaskNodeById(result.taskId)?.parent?.id)
        Assertions.assertEquals(1, taskTree.getTaskNodeById(parent.id)!!.children.size, "No second customer element.")
    }

    @Test
    fun `elements of the same titles are used instead of new ones`() {
        val parent = createParent("sameTitles")
        val customerTask = createTask("$PREFIX sameTitles", parent)
        val projectTask = createTask("$PREFIX-sameTitles-project", customerTask)
        logon(TEST_FINANCE_USER)
        val result = projectWizardService.execute(
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(nummer = 18513L, name = " $PREFIX sameTitles "),
                project = ProjectWizardService.ProjectData(nummer = 1, name = "$PREFIX-sameTitles-project"),
                parentTaskId = parent.id,
                teamGroup = ProjectWizardService.GroupData(name = "$PREFIX-sameTitles"),
            )
        )
        Assertions.assertEquals(customerTask.id, result.customerTaskId)
        Assertions.assertFalse(result.customerTaskCreated)
        Assertions.assertEquals(projectTask.id, result.taskId)
        Assertions.assertFalse(result.taskCreated)
        Assertions.assertEquals(projectTask.id, projektDao.find(result.projectId, checkAccess = false)!!.task?.id)
        Assertions.assertEquals(result.projectId, taskTree.getProjekt(projectTask.id)?.id)
        Assertions.assertTrue(taskTree.getTaskNodeById(projectTask.id)!!.children.isNullOrEmpty())
        val team = result.groups.single()
        val access = accessDao.getEntry(projectTask, GroupDO().also { it.id = team.id }, checkAccess = false)!!
        Assertions.assertTrue(access.recursive, "The existing element gets the rights of a new one.")
    }

    @Test
    fun `an existing element of the project must be free, below the customer's and writable`() {
        val parent = createParent("refusedElement")
        val customerTask = createTask("$PREFIX refusedElement", parent)
        val outside = createTask("$PREFIX-refusedElement-outside", parent)
        val taken = createTask("$PREFIX-refusedElement-taken", customerTask)
        val free = createTask("$PREFIX-refusedElement-free", customerTask)
        // Free itself, but inside the element of another project:
        val nested = createTask("$PREFIX-refusedElement-nested", taken)
        logon(TEST_FINANCE_USER)
        val kunde = KundeDO().also {
            it.nummer = 18514L
            it.name = "$PREFIX refusedElement"
        }
        kundeDao.insert(kunde)
        projektDao.insert(ProjektDO().also {
            it.name = "$PREFIX-refusedElement-taken"
            it.kunde = kunde
            it.nummer = 1
            it.task = taken
        })
        val data = { projectTaskId: Long? ->
            ProjectWizardService.Data(
                customer = ProjectWizardService.CustomerData(existing = true, id = 18514L),
                project = ProjectWizardService.ProjectData(nummer = 2, name = "$PREFIX-refusedElement-project"),
                parentTaskId = customerTask.id,
                projectTaskExisting = true,
                projectTaskId = projectTaskId,
            )
        }
        for (taskId in listOf(outside.id, taken.id, nested.id, null)) {
            Assertions.assertThrows(IllegalArgumentException::class.java, {
                projectWizardService.execute(data(taskId))
            }, "Task #$taskId")
        }
        // Who may create projects, but not write the element, may not grant rights on it:
        logon(addUser("ProjectWizardServiceTest.noTaskAccess", ORGA_GROUP, UserRightId.PM_PROJECT))
        Assertions.assertTrue(projectWizardService.projectAllowed())
        Assertions.assertThrows(AccessException::class.java) {
            projectWizardService.execute(data(free.id))
        }
        logon(TEST_FINANCE_USER)
        val result = projectWizardService.execute(data(free.id))
        Assertions.assertEquals(free.id, result.taskId)
        Assertions.assertFalse(result.taskCreated)
    }

    @Test
    fun `an element found by the new customer's name that holds another customer's projects is refused, a picked one warned about`() {
        val parent = createParent("otherCustomer")
        val customerTask = createTask("$PREFIX otherCustomer", parent)
        val projectTask = createTask("$PREFIX-otherCustomer-project", customerTask)
        val free = createTask("$PREFIX otherCustomer free", parent)
        logon(TEST_FINANCE_USER)
        val kunde = KundeDO().also {
            it.nummer = 18515L
            it.name = "$PREFIX otherCustomer old"
        }
        kundeDao.insert(kunde)
        projektDao.insert(ProjektDO().also {
            it.name = "$PREFIX-otherCustomer-project"
            it.kunde = kunde
            it.nummer = 1
            it.task = projectTask
        })
        val data = ProjectWizardService.Data(
            customer = ProjectWizardService.CustomerData(nummer = 18516L, name = "$PREFIX otherCustomer"),
            project = ProjectWizardService.ProjectData(nummer = 1, name = "$PREFIX-otherCustomer-new"),
            parentTaskId = parent.id,
        )
        val problem = projectWizardService.customerTaskProblem(data, projectWizardService.elementsOf(data))
        Assertions.assertEquals(ProjectWizardService.TaskProblem.Type.OTHER_CUSTOMER, problem?.type)
        Assertions.assertEquals("$PREFIX otherCustomer old", problem?.detail)
        Assertions.assertThrows(IllegalArgumentException::class.java) { projectWizardService.execute(data) }
        // Refused, not warned about:
        Assertions.assertNull(projectWizardService.customerTaskWarning(data, projectWizardService.elementsOf(data)))
        // Picked on purpose, the element may be shared, with a warning; as one of another project:
        data.customerTaskExisting = true
        data.customerTaskId = projectTask.id
        projectWizardService.customerTaskWarning(data, projectWizardService.elementsOf(data)).let {
            Assertions.assertEquals(ProjectWizardService.TaskProblem.Type.HAS_PROJECT, it?.type)
            Assertions.assertEquals("$PREFIX-otherCustomer-project", it?.detail)
        }
        data.customerTaskId = customerTask.id
        projectWizardService.customerTaskWarning(data, projectWizardService.elementsOf(data)).let {
            Assertions.assertEquals(ProjectWizardService.TaskProblem.Type.OTHER_CUSTOMER, it?.type)
            Assertions.assertEquals("$PREFIX otherCustomer old", it?.detail)
        }
        // No warning for a free element:
        data.customerTaskId = free.id
        Assertions.assertNull(projectWizardService.customerTaskWarning(data, projectWizardService.elementsOf(data)))
        data.customerTaskId = customerTask.id
        val result = projectWizardService.execute(data)
        Assertions.assertEquals(customerTask.id, result.customerTaskId)
    }

    /** A new user in the given group, with read and write access of the given rights. */
    private fun addUser(username: String, groupName: String, vararg rightIds: UserRightId): PFUserDO {
        logon(TEST_ADMIN_USER)
        val user = initTestDB.addUser(PFUserDO().also { user ->
            user.username = username
            rightIds.forEach { user.addRight(UserRightDO(it, UserRightValue.READWRITE)) }
        })
        val group = getGroup(groupName)
        group.addUser(user)
        groupDao.update(group)
        return user
    }

    /** A structure element below the root, created by the admin. */
    private fun createParent(name: String): TaskDO {
        logon(ADMIN_USER)
        val parent = TaskDO()
        parent.title = "$PREFIX-$name-parent"
        parent.parentTask = taskTree.rootTaskNode.task
        taskDao.insert(parent)
        return parent
    }

    /** A structure element below [parent], created by the admin. */
    private fun createTask(title: String, parent: TaskDO): TaskDO {
        logon(ADMIN_USER)
        return TaskDO().also {
            it.title = title
            it.parentTask = parent
            taskDao.insert(it)
        }
    }

    private fun kost2Art(): Kost2ArtDO = kost2ArtDao.find(2L, checkAccess = false) ?: Kost2ArtDO().also {
        it.id = 2L
        it.name = "Test"
        kost2ArtDao.insert(it, checkAccess = false)
    }

    companion object {
        private const val PREFIX = "ProjectWizardServiceTest"
    }
}
