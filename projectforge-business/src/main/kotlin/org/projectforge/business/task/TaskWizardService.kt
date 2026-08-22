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

import mu.KotlinLogging
import org.projectforge.business.user.GroupDao
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The work of the structure wizard: grants a set of groups their access rights on one structure
 * element in one go.
 *
 * The rules are the ones of Wicket's `TaskWizardPage.create`, moved out of the page so both frontends
 * and a test can reach them: on the chosen element each group gets the template matching its role,
 * recursively, and every ancestor up to the root gets read access on the tasks alone - enough to see
 * the path down to the element, and nothing more (see `task.wizard.task.intro`).
 *
 * @author Kai Reinhard
 */
@Service
class TaskWizardService {
    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var taskTree: TaskTree

    /**
     * The role a group is given on the structure element, and with it the access template.
     *
     * [i18nKey] is the suffix under `task.wizard.` naming the role in the bundle
     * (`task.wizard.managerGroup`, ...), which is also the field name both frontends use for it.
     */
    enum class GroupType(val i18nKey: String) {
        MANAGER("managerGroup"),
        TEAM("team"),
        EXTERNAL("externalGroup"),
    }

    /**
     * What [grantAccess] did, per group that was given one.
     *
     * @param groupType The role the group was granted.
     * @param groupName Name of the group, for the message the caller reports.
     * @param accessEntries How many [GroupTaskAccessDO] rows were written - one for the element plus
     * one per ancestor below the root, minus the ones that were already right.
     */
    class GrantedAccess(
        val groupType: GroupType,
        val groupName: String?,
        val accessEntries: Int,
    )

    /**
     * @param taskTitle Title of the element the rights were granted on.
     * @param granted One entry per group that was given a role; empty if none was, which is the case
     * the wizard announces as `task.wizard.action.noactionRequired`.
     */
    class Result(
        val taskTitle: String?,
        val granted: List<GrantedAccess>,
    )

    /**
     * Grants each given group its rights on the structure element and read access on the element's
     * ancestors.
     *
     * A group left out is simply not granted anything - all three are optional, and with none of them
     * the call is a no-op. Same for the root element: it is the anchor of everybody's tree, so rights
     * are never written on it.
     *
     * @param taskId The structure element, required.
     * @throws IllegalArgumentException if [taskId] names no element.
     */
    fun grantAccess(
        taskId: Long,
        managerGroupId: Long? = null,
        teamGroupId: Long? = null,
        externalGroupId: Long? = null,
    ): Result {
        val taskNode = taskTree.getTaskNodeById(taskId)
        requireNotNull(taskNode) { "Structure element with id $taskId not found." }
        val granted = listOf(
            GroupType.MANAGER to managerGroupId,
            GroupType.TEAM to teamGroupId,
            GroupType.EXTERNAL to externalGroupId,
        ).mapNotNull { (groupType, groupId) ->
            val group = groupId?.let { groupDao.find(it) } ?: return@mapNotNull null
            val entries = createAccessRights(taskNode, group, groupType, isLeaf = true)
            GrantedAccess(groupType, group.name, entries)
        }
        if (granted.isEmpty()) {
            log.info { "Structure wizard: no group given for task #$taskId, so no access rights to create." }
        }
        return Result(taskNode.task?.title, granted)
    }

    /**
     * Writes the access entry of one group on one element and then walks up to the root.
     *
     * @param isLeaf True for the element the user picked, false for its ancestors: those get the guest
     * template and no recursion, so the group sees the path down to the element without gaining
     * anything on its siblings.
     * @return The number of entries written, including the ancestors'.
     */
    private fun createAccessRights(
        taskNode: TaskNode?,
        group: GroupDO,
        groupType: GroupType,
        isLeaf: Boolean,
    ): Int {
        val task = taskNode?.task ?: return 0
        val taskNodeId = taskNode.id ?: return 0
        val groupId = group.id ?: return 0
        if (taskTree.isRootNode(taskNode)) {
            return 0
        }
        var access = accessDao.getEntry(task, group)
        if (access == null) {
            access = GroupTaskAccessDO()
            accessDao.setTask(access, taskNodeId)
            accessDao.setGroup(access, groupId)
        } else if (access.deleted) {
            accessDao.undelete(access)
        }
        if (!isLeaf) {
            access.guest()
            access.recursive = false
        } else {
            when (groupType) {
                GroupType.MANAGER -> access.leader()
                GroupType.EXTERNAL -> access.external()
                GroupType.TEAM -> access.employee()
            }
            access.recursive = true
        }
        accessDao.insertOrUpdate(access)
        // Minimal access rights for the parent element up to the root.
        return 1 + createAccessRights(taskNode.parent, group, groupType, isLeaf = false)
    }
}
