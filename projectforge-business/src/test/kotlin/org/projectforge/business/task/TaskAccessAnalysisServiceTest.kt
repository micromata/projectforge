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

package org.projectforge.business.task

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.springframework.beans.factory.annotation.Autowired

/**
 * The three anomaly checks of [TaskAccessAnalysisService]: a group with access under a project it does not
 * manage, a parent element granted more than the wizard's minimal read-only access, and a group whose
 * access is scattered across unrelated branches of the tree.
 */
class TaskAccessAnalysisServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var taskWizardService: TaskWizardService

    @Autowired
    private lateinit var analysisService: TaskAccessAnalysisService

    @Test
    fun `a group other than the project manager group is flagged, the manager group, a same-family group and system groups are not`() {
        logon(ADMIN_USER)
        val pmGroup = createGroup("consulting-pl")
        val foreignGroup = createGroup("foreign-other")
        // Same project family by naming convention: the manager group is "…-consulting-pl", this one
        // "…-consulting" — a leading prefix, so it must not be reported as foreign.
        val familyGroup = createGroup("consulting")
        val (parent, child) = createSubtree("foreign")
        createProject("foreign", parent, pmGroup)

        // The manager group works on the element (fine); a foreign group also got access there (the anomaly).
        val pmAccess = createAccess(child, pmGroup, recursive = true) { it.leader() }
        val foreignAccess = createAccess(child, foreignGroup, recursive = true) { it.employee() }
        val familyAccess = createAccess(child, familyGroup, recursive = true) { it.employee() }
        // A system group legitimately spans every project and must never be flagged.
        val adminGroupId = userGroupCache.getGroup(ProjectForgeGroup.ADMIN_GROUP)?.id
        val adminAccess = adminGroupId?.let { createAccess(child, GroupDO().apply { id = it }, recursive = true) { a -> a.leader() } }

        val flagged = analysisService.analyze(TaskAccessAnalysisService.AnalysisCheck.FOREIGN_PROJECT_GROUP)

        Assertions.assertTrue(flagged.containsKey(foreignAccess.id), "The foreign group's access must be flagged.")
        Assertions.assertFalse(flagged.containsKey(pmAccess.id), "The project's own manager group is not foreign.")
        Assertions.assertFalse(
            flagged.containsKey(familyAccess.id),
            "A group sharing the manager group's name prefix belongs to the same project family.",
        )
        if (adminAccess != null) {
            Assertions.assertFalse(flagged.containsKey(adminAccess.id), "A system group is exempt.")
        }
    }

    @Test
    fun `a parent element with more than read-only, non-recursive access is flagged`() {
        logon(ADMIN_USER)
        val group = createGroup("overbroad")
        val (parent, child) = createSubtree("overbroad")
        // The wizard's correct outcome: leader recursively on the element, guest non-recursive on the parent.
        taskWizardService.grantAccess(taskId = child.id!!, managerGroupId = group.id)

        analysisService.analyze(TaskAccessAnalysisService.AnalysisCheck.OVERBROAD_ANCESTOR).let { clean ->
            Assertions.assertFalse(
                clean.containsKey(entry(parent, group).id),
                "A guest, non-recursive parent is exactly what the wizard writes — not an anomaly.",
            )
        }

        // Tamper the parent so it reaches the leaf's siblings — the very thing the wizard avoids.
        accessDao.update(entry(parent, group).also { it.recursive = true })

        val flagged = analysisService.analyze(TaskAccessAnalysisService.AnalysisCheck.OVERBROAD_ANCESTOR)
        Assertions.assertTrue(
            flagged.containsKey(entry(parent, group).id),
            "A recursive parent of a real grant exposes the siblings and must be flagged.",
        )
        Assertions.assertFalse(
            flagged.containsKey(entry(child, group).id),
            "The leaf grant itself is not the anomaly.",
        )
    }

    @Test
    fun `a group scattered across unrelated branches is flagged, a single branch is not`() {
        logon(ADMIN_USER)
        val scattered = createGroup("scattered")
        val single = createGroup("single")
        val (_, childA) = createSubtree("branchA")
        val (_, childB) = createSubtree("branchB")

        // The scattered group works in two unrelated branches; the single group in only one.
        taskWizardService.grantAccess(taskId = childA.id!!, managerGroupId = scattered.id)
        taskWizardService.grantAccess(taskId = childB.id!!, managerGroupId = scattered.id)
        taskWizardService.grantAccess(taskId = childA.id!!, teamGroupId = single.id)

        val flagged = analysisService.analyze(TaskAccessAnalysisService.AnalysisCheck.MULTI_BRANCH_GROUP)

        Assertions.assertTrue(
            flagged.containsKey(entry(childA, scattered).id) && flagged.containsKey(entry(childB, scattered).id),
            "A group with real grants in two unrelated branches must be flagged.",
        )
        Assertions.assertFalse(
            flagged.containsKey(entry(childA, single).id),
            "A single wizard run (leaf plus guest ancestors) is one branch, not an anomaly.",
        )
    }

    /** A `parent` below the root with one `child`, as in the wizard test. */
    private fun createSubtree(name: String): Pair<TaskDO, TaskDO> {
        val parent = TaskDO()
        parent.title = "$PREFIX-$name-parent"
        parent.parentTask = taskTree.rootTaskNode.task
        taskDao.insert(parent)
        val child = TaskDO()
        child.title = "$PREFIX-$name-child"
        child.parentTask = parent
        taskDao.insert(child)
        return parent to child
    }

    private fun createGroup(name: String): GroupDO {
        val group = GroupDO()
        group.name = "$PREFIX-$name"
        groupDao.insert(group)
        return group
    }

    private fun createProject(name: String, task: TaskDO, managerGroup: GroupDO): ProjektDO {
        val projekt = ProjektDO().also {
            it.name = "$PREFIX-$name"
            it.task = task
            it.projektManagerGroup = managerGroup
        }
        // ADMIN_USER has no finance rights; the project is only test scaffolding for the check.
        projektDao.insert(projekt, checkAccess = false)
        return projekt
    }

    private fun createAccess(
        task: TaskDO,
        group: GroupDO,
        recursive: Boolean,
        template: (GroupTaskAccessDO) -> Unit,
    ): GroupTaskAccessDO {
        val access = GroupTaskAccessDO()
        accessDao.setTask(access, task.id!!)
        accessDao.setGroup(access, group.id!!)
        template(access)
        access.recursive = recursive
        accessDao.insert(access)
        return access
    }

    private fun entry(task: TaskDO, group: GroupDO): GroupTaskAccessDO {
        return accessDao.getEntry(task, group)
            ?: Assertions.fail("No access entry for task '${task.title}' and group '${group.name}'.")
    }

    companion object {
        private val PREFIX = TaskAccessAnalysisService::class.simpleName
    }
}
