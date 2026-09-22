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
import org.projectforge.framework.access.AccessType
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
     * The four permissions of one access type of a row, as the access management shows them.
     */
    class AccessRightResult(
        val accessType: AccessType,
        val select: Boolean,
        val insert: Boolean,
        val update: Boolean,
        val delete: Boolean,
    ) {
        /** Whether both say the same about the same access type. */
        fun sameAs(other: AccessRightResult): Boolean {
            return accessType == other.accessType && select == other.select && insert == other.insert &&
                    update == other.update && delete == other.delete
        }
    }

    /**
     * One [GroupTaskAccessDO] row the wizard looked at - the picked element or one of its ancestors.
     *
     * @param pickedElement True for the element the user picked, which gets the role's template
     * recursively; false for an ancestor, which only gets read access on the tasks (see [createAccessRights]).
     * @param recursive Whether the rights hold for the sub elements as well, which is the case on the
     * picked element only.
     * @param rights The permissions of the row as the template leaves them, in the order the access
     * management lists them ([GroupTaskAccessDO.orderedEntries]) - the answer to "which rights", which a
     * template's name alone does not give.
     */
    class AccessEntryResult(
        val groupType: GroupType,
        val groupName: String?,
        val taskId: Long,
        val taskTitle: String?,
        val pickedElement: Boolean,
        val status: AccessStatus,
        val recursive: Boolean,
        val rights: List<AccessRightResult>,
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
    ): Result = process(taskId, managerGroupId, teamGroupId, externalGroupId, dryRun = false)

    /**
     * What [grantAccess] with the same arguments would do, without doing any of it: the same walk over
     * the same rows, but nothing is written and the status is the one the write would have reported.
     *
     * Serves the wizard's preview table, which is shown while the user is still picking, so it has to
     * answer the same question the report answers afterwards - which rights are new, which change one
     * that says something else, and which are already there.
     *
     * @throws IllegalArgumentException if [taskId] names no element.
     */
    fun previewAccess(
        taskId: Long,
        managerGroupId: Long? = null,
        teamGroupId: Long? = null,
        externalGroupId: Long? = null,
    ): Result = process(taskId, managerGroupId, teamGroupId, externalGroupId, dryRun = true)

    private fun process(
        taskId: Long,
        managerGroupId: Long?,
        teamGroupId: Long?,
        externalGroupId: Long?,
        dryRun: Boolean,
    ): Result {
        val taskNode = taskTree.getTaskNodeById(taskId)
        requireNotNull(taskNode) { "Structure element with id $taskId not found." }
        val granted = listOf(
            GroupType.MANAGER to managerGroupId,
            GroupType.TEAM to teamGroupId,
            GroupType.EXTERNAL to externalGroupId,
        ).mapNotNull { (groupType, groupId) ->
            val group = groupId?.let { groupDao.find(it) } ?: return@mapNotNull null
            GrantedAccess(
                groupType,
                group.name,
                createAccessRights(taskNode, group, groupType, isLeaf = true, dryRun = dryRun),
            )
        }
        if (granted.isEmpty() && !dryRun) {
            log.info { "Structure wizard: no group given for task #$taskId, so no access rights to create." }
        }
        return Result(taskNode.task?.title, granted)
    }

    /**
     * Writes the access entry of one group on one element and then walks up to the root - or, with
     * [dryRun], only says what that would come to.
     *
     * One function for both, so the preview and the write cannot drift apart: they take the same rows in
     * the same order and apply the same template ([applyTemplate]), and only the last step - write or
     * compare - differs.
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
        dryRun: Boolean,
    ): List<AccessEntryResult> {
        val task = taskNode?.task ?: return emptyList()
        val taskNodeId = taskNode.id ?: return emptyList()
        val groupId = group.id ?: return emptyList()
        if (taskTree.isRootNode(taskNode)) {
            return emptyList()
        }
        val existing = accessDao.getEntry(task, group)
        val status = if (dryRun) {
            previewStatus(existing, groupType, isLeaf)
        } else {
            writeAccessRight(existing, taskNodeId, groupId, groupType, isLeaf)
        }
        // The rights of the row afterwards, read off the template alone: it sets all four access types, so
        // what the row says afterwards does not depend on what it said before - and this way the write and
        // the preview report the same rights without either of them touching the loaded row.
        val template = GroupTaskAccessDO().also { applyTemplate(it, groupType, isLeaf) }
        val entry = AccessEntryResult(
            groupType = groupType,
            groupName = group.name,
            taskId = taskNodeId,
            taskTitle = task.title,
            pickedElement = isLeaf,
            status = status,
            recursive = template.recursive,
            rights = rightsOf(template),
        )
        // Minimal access rights for the parent element up to the root.
        return listOf(entry) + createAccessRights(taskNode.parent, group, groupType, isLeaf = false, dryRun = dryRun)
    }

    /**
     * Writes the one row and says what became of it.
     *
     * @param existing The row of this group on this element, or null if there is none yet.
     */
    private fun writeAccessRight(
        existing: GroupTaskAccessDO?,
        taskNodeId: Long,
        groupId: Long,
        groupType: GroupType,
        isLeaf: Boolean,
    ): AccessStatus {
        val wasDeleted = existing?.deleted == true
        val access = existing ?: GroupTaskAccessDO().also {
            accessDao.setTask(it, taskNodeId)
            accessDao.setGroup(it, groupId)
        }
        if (wasDeleted) {
            // Before the template is applied, not after: undelete writes the whole object to the row,
            // so it would persist the new rights and leave the update below nothing to report.
            accessDao.undelete(access)
        }
        applyTemplate(access, groupType, isLeaf)
        return when {
            existing == null -> {
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
    }

    /**
     * What [writeAccessRight] would answer for the same row, without touching it.
     *
     * Compares what the row says today with what the template says, field by field, rather than by
     * copying the row and letting `copyValuesFrom` answer: two copies of one row share their
     * [org.projectforge.framework.access.AccessEntryDO] instances, so applying the template to the one
     * changes the other with it and every comparison would come out as "unchanged".
     *
     * The fields are the ones [applyTemplate] sets - the four access types and the recursion. Task and
     * group are what the row was looked up by, and a deleted row is dealt with above, so there is
     * nothing else an update could find to write.
     */
    private fun previewStatus(
        existing: GroupTaskAccessDO?,
        groupType: GroupType,
        isLeaf: Boolean,
    ): AccessStatus {
        if (existing == null) {
            return AccessStatus.CREATED
        }
        if (existing.deleted) {
            // An undelete writes the row whatever the rights say, exactly as in writeAccessRight.
            return AccessStatus.UPDATED
        }
        val template = GroupTaskAccessDO().also { applyTemplate(it, groupType, isLeaf) }
        if (existing.recursive != template.recursive) {
            return AccessStatus.UPDATED
        }
        val current = rightsOf(existing).associateBy { it.accessType }
        val unchanged = rightsOf(template).all { wanted ->
            current[wanted.accessType]?.let { it.sameAs(wanted) } == true
        }
        return if (unchanged) AccessStatus.UNCHANGED else AccessStatus.UPDATED
    }

    /**
     * The four permissions per access type of a row, in the order the access management lists them.
     */
    private fun rightsOf(access: GroupTaskAccessDO): List<AccessRightResult> {
        return access.orderedEntries.mapNotNull { entry ->
            val accessType = entry.accessType ?: return@mapNotNull null
            AccessRightResult(
                accessType = accessType,
                select = entry.accessSelect,
                insert = entry.accessInsert,
                update = entry.accessUpdate,
                delete = entry.accessDelete,
            )
        }
    }

    /**
     * The rules themselves, in one place: the role's template recursively on the picked element, read
     * access on the tasks alone on every ancestor.
     */
    private fun applyTemplate(access: GroupTaskAccessDO, groupType: GroupType, isLeaf: Boolean) {
        if (!isLeaf) {
            access.guest()
            access.recursive = false
            return
        }
        when (groupType) {
            GroupType.MANAGER -> access.leader()
            GroupType.EXTERNAL -> access.external()
            GroupType.TEAM -> access.employee()
        }
        access.recursive = true
    }
}
