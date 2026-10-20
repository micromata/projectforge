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
import org.projectforge.business.task.TaskTree
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
    private lateinit var taskTree: TaskTree

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
    fun `users without the rights are refused`() {
        logon(TEST_PROJECT_MANAGER_USER)
        Assertions.assertFalse(projectWizardRest.init().allowed)
        Assertions.assertThrows(AccessException::class.java) {
            projectWizardRest.validate(ProjectWizardService.Data())
        }
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
}
