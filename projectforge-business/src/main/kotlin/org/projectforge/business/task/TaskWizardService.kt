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
import org.projectforge.framework.persistence.api.EntityCopyStatus
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
     * What became of one [GroupTaskAccessDO] row, so the caller can tell the user what the wizard
     * actually did instead of only how much of it.
     */
    enum class AccessStatus {
        /** There was no entry for this group and element, so one was written. */
        CREATED,

        /** The entry was there but said something else - or was deleted and is now back. */
        UPDATED,

        /** The entry already said exactly what the wizard would have written, so nothing was written. */
        UNCHANGED,
    }

    /**
     * One [GroupTaskAccessDO] row the wizard looked at - the picked element or one of its ancestors.
     *
     * @param pickedElement True for the element the user picked, which gets the role's template
     * recursively; false for an ancestor, which only gets read access on the tasks (see [createAccessRights]).
     */
    class AccessEntryResult(
        val groupType: GroupType,
        val groupName: String?,
        val taskId: Long,
        val taskTitle: String?,
        val pickedElement: Boolean,
        val status: AccessStatus,
    )

    /**
     * What [grantAccess] did, per group that was given a role.
     *
     * @param groupType The role the group was granted.
     * @param groupName Name of the group, for the message the caller reports.
     * @param entries One per row that was looked at - the element first, then its ancestors upwards.
     */
    class GrantedAccess(
        val groupType: GroupType,
        val groupName: String?,
        val entries: List<AccessEntryResult>,
    ) {
        /**
         * How many [GroupTaskAccessDO] rows the wizard touched - one for the element plus one per
         * ancestor below the root, the ones that were already right included (they carry
         * [AccessStatus.UNCHANGED]).
         */
        val accessEntries: Int
            get() = entries.size
    }

    /**
     * @param taskTitle Title of the element the rights were granted on.
     * @param granted One entry per group that was given a role; empty if none was, which is the case
     * the wizard announces as `task.wizard.action.noactionRequired`.
     */
    class Result(
        val taskTitle: String?,
        val granted: List<GrantedAccess>,
    ) {
        /** All rows over all groups, in the order they were written. */
        val entries: List<AccessEntryResult>
            get() = granted.flatMap { it.entries }

        fun count(status: AccessStatus): Int = entries.count { it.status == status }
    }

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
            GrantedAccess(groupType, group.name, createAccessRights(taskNode, group, groupType, isLeaf = true))
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
     * @return One entry per row that was looked at - this element's first, then the ancestors' upwards.
     */
    private fun createAccessRights(
        taskNode: TaskNode?,
        group: GroupDO,
        groupType: GroupType,
        isLeaf: Boolean,
    ): List<AccessEntryResult> {
        val task = taskNode?.task ?: return emptyList()
        val taskNodeId = taskNode.id ?: return emptyList()
        val groupId = group.id ?: return emptyList()
        if (taskTree.isRootNode(taskNode)) {
            return emptyList()
        }
        var access = accessDao.getEntry(task, group)
        val isNew = access == null
        val wasDeleted = access?.deleted == true
        if (access == null) {
            access = GroupTaskAccessDO()
            accessDao.setTask(access, taskNodeId)
            accessDao.setGroup(access, groupId)
        } else if (wasDeleted) {
            // Before the template is applied, not after: undelete writes the whole object to the row,
            // so it would persist the new rights and leave the update below nothing to report.
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
        val status = when {
            isNew -> {
                accessDao.insert(access)
                AccessStatus.CREATED
            }
            // The row did change - it was deleted - whatever the update answers.
            wasDeleted -> {
                accessDao.update(access)
                AccessStatus.UPDATED
            }
            // NONE means the entry already said what the template says, and nothing was written at all
            // (not even lastUpdate, see BaseDOPersistenceService.privateUpdate).
            accessDao.update(access) == EntityCopyStatus.NONE -> AccessStatus.UNCHANGED
            else -> AccessStatus.UPDATED
        }
        val entry = AccessEntryResult(
            groupType = groupType,
            groupName = group.name,
            taskId = taskNodeId,
            taskTitle = task.title,
            pickedElement = isLeaf,
            status = status,
        )
        // Minimal access rights for the parent element up to the root.
        return listOf(entry) + createAccessRights(taskNode.parent, group, groupType, isLeaf = false)
    }
}
