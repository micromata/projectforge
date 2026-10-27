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

import io.github.oshai.kotlinlogging.KotlinLogging
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
        /** Null for the element of [previewNewElement], which doesn't exist yet. */
        val taskId: Long?,
        val taskTitle: String?,
        val pickedElement: Boolean,
        val status: AccessStatus,
        val recursive: Boolean,
        val rights: List<AccessRightResult>,
    )

    /**
     * A group as the project wizard hands it over: an existing one by [id], or one it is about to create,
     * known only by its [name] - which only a preview can take, there is nothing to write rows for yet.
     */
    class WizardGroup(
        val id: Long? = null,
        val name: String? = null,
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
     * @param checkAccess False for the project wizard only, which grants its users the exception of
     * writing access rights after having checked them itself (see `ProjectWizardService`): the rows are
     * then read and written without the access checks of [AccessDao].
     * @throws IllegalArgumentException if [taskId] names no element.
     */
    fun grantAccess(
        taskId: Long,
        managerGroupId: Long? = null,
        teamGroupId: Long? = null,
        externalGroupId: Long? = null,
        checkAccess: Boolean = true,
    ): Result {
        val taskNode = requireTaskNode(taskId)
        val granted = grantedAccess(managerGroupId, teamGroupId, externalGroupId) { group, groupType ->
            createAccessRights(taskNode, group, groupType, isLeaf = true, dryRun = false, checkAccess = checkAccess)
        }
        if (granted.isEmpty()) {
            log.info { "Structure wizard: no group given for task #$taskId, so no access rights to create." }
        }
        return Result(taskNode.task?.title, granted)
    }

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
    ): Result {
        val taskNode = requireTaskNode(taskId)
        val granted = grantedAccess(managerGroupId, teamGroupId, externalGroupId) { group, groupType ->
            createAccessRights(taskNode, group, groupType, isLeaf = true, dryRun = true, checkAccess = true)
        }
        return Result(taskNode.task?.title, granted)
    }

    /**
     * The preview of the project wizard: what [grantAccess] would do on an element that doesn't exist
     * yet, to be created as child of [parentTaskId] with [newTaskTitle], for groups of which some may
     * not exist yet either.
     *
     * The new element's row is new by definition, and so is every row of a new group; only the rows of
     * an existing group on the ancestors may already be there.
     *
     * @param newParentTitle Title of a new element between [parentTaskId] and the new one - the element
     * of a new customer, which gets the read access of an ancestor.
     * @param checkAccess False for the project wizard, whose users may not see the rows of groups they
     * aren't members of (see [grantAccess]).
     * @throws IllegalArgumentException if [parentTaskId] names no element.
     */
    fun previewNewElement(
        parentTaskId: Long,
        newTaskTitle: String?,
        newParentTitle: String? = null,
        managerGroup: WizardGroup? = null,
        teamGroup: WizardGroup? = null,
        externalGroup: WizardGroup? = null,
        checkAccess: Boolean = true,
    ): Result {
        val parentNode = requireTaskNode(parentTaskId)
        val granted = wizardGroupsOf(managerGroup, teamGroup, externalGroup, checkAccess) { group, groupType ->
            val leaf = entryOf(groupType, group.name, null, newTaskTitle, isLeaf = true, AccessStatus.CREATED)
            val newParent = newParentTitle?.let {
                entryOf(groupType, group.name, null, it, isLeaf = false, AccessStatus.CREATED)
            }
            val ancestors = createAccessRights(
                parentNode, group, groupType, isLeaf = false, dryRun = true, checkAccess = checkAccess,
            )
            listOfNotNull(leaf, newParent) + ancestors
        }
        return Result(newTaskTitle, granted)
    }

    /**
     * The preview of the project wizard for an existing element of the project: what [grantAccess] would do on
     * [taskId], for groups of which some may not exist yet - like [previewAccess], but with the wizard's groups
     * and its access checks (see [previewNewElement]).
     *
     * @throws IllegalArgumentException if [taskId] names no element.
     */
    fun previewExistingElement(
        taskId: Long,
        managerGroup: WizardGroup? = null,
        teamGroup: WizardGroup? = null,
        externalGroup: WizardGroup? = null,
        checkAccess: Boolean = true,
    ): Result {
        val taskNode = requireTaskNode(taskId)
        val granted = wizardGroupsOf(managerGroup, teamGroup, externalGroup, checkAccess) { group, groupType ->
            createAccessRights(taskNode, group, groupType, isLeaf = true, dryRun = true, checkAccess = checkAccess)
        }
        return Result(taskNode.task?.title, granted)
    }

    /** The rows of the wizard's groups, existing ones by id and new ones by name; groups without either left out. */
    private fun wizardGroupsOf(
        managerGroup: WizardGroup?,
        teamGroup: WizardGroup?,
        externalGroup: WizardGroup?,
        checkAccess: Boolean,
        entries: (GroupRef, GroupType) -> List<AccessEntryResult>,
    ): List<GrantedAccess> {
        return listOf(
            GroupType.MANAGER to managerGroup,
            GroupType.TEAM to teamGroup,
            GroupType.EXTERNAL to externalGroup,
        ).mapNotNull { (groupType, wizardGroup) ->
            val group = when {
                wizardGroup == null -> null
                wizardGroup.id != null -> groupDao.find(wizardGroup.id, checkAccess = checkAccess)
                    ?.let { GroupRef(it.id, it.name) }
                wizardGroup.name.isNullOrBlank() -> null
                else -> GroupRef(null, wizardGroup.name)
            } ?: return@mapNotNull null
            GrantedAccess(groupType, group.name, entries(group, groupType))
        }
    }

    /**
     * A group the rows are written for: [id] is null for one the project wizard is about to create.
     */
    private class GroupRef(val id: Long?, val name: String?)

    private fun requireTaskNode(taskId: Long): TaskNode {
        val taskNode = taskTree.getTaskNodeById(taskId)
        requireNotNull(taskNode) { "Structure element with id $taskId not found." }
        return taskNode
    }

    private fun grantedAccess(
        managerGroupId: Long?,
        teamGroupId: Long?,
        externalGroupId: Long?,
        entries: (GroupRef, GroupType) -> List<AccessEntryResult>,
    ): List<GrantedAccess> {
        return listOf(
            GroupType.MANAGER to managerGroupId,
            GroupType.TEAM to teamGroupId,
            GroupType.EXTERNAL to externalGroupId,
        ).mapNotNull { (groupType, groupId) ->
            // Without access check: the caller has checked who may run the wizard, and the group is
            // only read for its name here. Attached: the project wizard calls this within its transaction,
            // right after inserting the group, and a detaching find would pull the new group out of it.
            val group = groupId?.let { groupDao.find(it, checkAccess = false, attached = true) }
                ?: return@mapNotNull null
            GrantedAccess(groupType, group.name, entries(GroupRef(group.id, group.name), groupType))
        }
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
        group: GroupRef,
        groupType: GroupType,
        isLeaf: Boolean,
        dryRun: Boolean,
        checkAccess: Boolean,
    ): List<AccessEntryResult> {
        val task = taskNode?.task ?: return emptyList()
        val taskNodeId = taskNode.id ?: return emptyList()
        if (taskTree.isRootNode(taskNode)) {
            return emptyList()
        }
        val groupId = group.id
        // A group that doesn't exist yet has no rows anywhere.
        val existing = groupId?.let {
            accessDao.getEntry(task, GroupDO().also { it.id = groupId }, checkAccess = checkAccess)
        }
        // Without access checks (the project wizard, see grantAccess) an existing row is only added to, never
        // weakened: its user may not manage the access rights, so he must not take away what a group may do.
        val onlyAdd = !checkAccess && existing != null && !existing.deleted
        val wanted = wantedRights(existing, groupType, isLeaf, onlyAdd)
        val status = if (dryRun) {
            previewStatus(existing, wanted)
        } else {
            requireNotNull(groupId) { "Access rights can only be written for an existing group." }
            writeAccessRight(existing, taskNodeId, groupId, wanted, checkAccess)
        }
        val entry = entryOf(groupType, group.name, taskNodeId, task.title, isLeaf, status, wanted)
        // Minimal access rights for the parent element up to the root.
        return listOf(entry) + createAccessRights(
            taskNode.parent, group, groupType, isLeaf = false, dryRun = dryRun, checkAccess = checkAccess,
        )
    }

    private fun entryOf(
        groupType: GroupType,
        groupName: String?,
        taskId: Long?,
        taskTitle: String?,
        isLeaf: Boolean,
        status: AccessStatus,
        wanted: GroupTaskAccessDO? = null,
    ): AccessEntryResult {
        // The rights of the row afterwards, read off [wantedRights] (the template for a new row): this way the
        // write and the preview report the same rights without either of them touching the loaded row.
        val template = wanted ?: GroupTaskAccessDO().also { applyTemplate(it, groupType, isLeaf) }
        return AccessEntryResult(
            groupType = groupType,
            groupName = groupName,
            taskId = taskId,
            taskTitle = taskTitle,
            pickedElement = isLeaf,
            status = status,
            recursive = template.recursive,
            rights = rightsOf(template),
        )
    }

    /**
     * Writes the one row and says what became of it.
     *
     * @param existing The row of this group on this element, or null if there is none yet.
     * @param wanted The rights the row gets, see [wantedRights].
     */
    private fun writeAccessRight(
        existing: GroupTaskAccessDO?,
        taskNodeId: Long,
        groupId: Long,
        wanted: GroupTaskAccessDO,
        checkAccess: Boolean,
    ): AccessStatus {
        val wasDeleted = existing?.deleted == true
        val access = existing ?: GroupTaskAccessDO().also {
            accessDao.setTask(it, taskNodeId)
            accessDao.setGroup(it, groupId)
        }
        if (wasDeleted) {
            // Before the template is applied, not after: undelete writes the whole object to the row,
            // so it would persist the new rights and leave the update below nothing to report.
            accessDao.undelete(access, checkAccess)
        }
        access.recursive = wanted.recursive
        wanted.orderedEntries.forEach { entry ->
            access.ensureAndGetAccessEntry(entry.accessType)
                .setAccess(entry.accessSelect, entry.accessInsert, entry.accessUpdate, entry.accessDelete)
        }
        return when {
            existing == null -> {
                accessDao.insert(access, checkAccess)
                AccessStatus.CREATED
            }
            // The row did change - it was deleted - whatever the update answers.
            wasDeleted -> {
                accessDao.update(access, checkAccess)
                AccessStatus.UPDATED
            }
            // NONE means the entry already said what the template says, and nothing was written at all
            // (not even lastUpdate, see BaseDOPersistenceService.privateUpdate).
            accessDao.update(access, checkAccess) == EntityCopyStatus.NONE -> AccessStatus.UNCHANGED
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
        wanted: GroupTaskAccessDO,
    ): AccessStatus {
        if (existing == null) {
            return AccessStatus.CREATED
        }
        if (existing.deleted) {
            // An undelete writes the row whatever the rights say, exactly as in writeAccessRight.
            return AccessStatus.UPDATED
        }
        if (existing.recursive != wanted.recursive) {
            return AccessStatus.UPDATED
        }
        val current = rightsOf(existing).associateBy { it.accessType }
        val unchanged = rightsOf(wanted).all { right ->
            current[right.accessType]?.let { it.sameAs(right) } == true
        }
        return if (unchanged) AccessStatus.UNCHANGED else AccessStatus.UPDATED
    }

    /**
     * The rights a row gets, as a new object that shares no [org.projectforge.framework.access.AccessEntryDO]
     * with [existing] (see [previewStatus]): the template, or with [onlyAdd] the template added to what
     * [existing] already allows - every permission and the recursion only ever switched on, never off.
     */
    private fun wantedRights(
        existing: GroupTaskAccessDO?,
        groupType: GroupType,
        isLeaf: Boolean,
        onlyAdd: Boolean,
    ): GroupTaskAccessDO {
        val template = GroupTaskAccessDO().also { applyTemplate(it, groupType, isLeaf) }
        if (!onlyAdd || existing == null) {
            return template
        }
        return GroupTaskAccessDO().also { result ->
            result.recursive = existing.recursive || template.recursive
            template.orderedEntries.forEach { entry ->
                val current = existing.getAccessEntry(entry.accessType)
                result.ensureAndGetAccessEntry(entry.accessType).setAccess(
                    entry.accessSelect || current?.accessSelect == true,
                    entry.accessInsert || current?.accessInsert == true,
                    entry.accessUpdate || current?.accessUpdate == true,
                    entry.accessDelete || current?.accessDelete == true,
                )
            }
        }
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
