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
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserGroupCache
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Finds likely-erroneous access-management entries so an admin can inspect and fix them.
 *
 * Access entries ([GroupTaskAccessDO] - one group's rights on one task) sometimes get misconfigured, e.g.
 * when the structure wizard was run with the wrong group or on the wrong element. There is no way to spot
 * these by hand in a large tree, so this service runs one of a fixed set of checks over the whole access
 * table and returns the flagged rows with a short, human-readable reason.
 *
 * The "correct" pattern each check compares against is the one of [TaskWizardService]: the picked leaf task
 * gets the role template recursively; every ancestor below the root gets exactly [GroupTaskAccessDO.guest]
 * (read-only on the tasks alone) and non-recursive - just enough to see the path down, and nothing on the
 * siblings.
 *
 * @author Kai Reinhard
 */
@Service
class TaskAccessAnalysisService {
    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * The checks the user can pick. [i18nKey] names the option in the bundle (used by the list filter).
     */
    enum class AnalysisCheck(override val i18nKey: String) : I18nEnum {
        /** Access in a project's subtree granted to a group other than the project's manager group. */
        FOREIGN_PROJECT_GROUP("access.analysis.foreignProjectGroup"),

        /** A parent task of a real (recursive) grant carries more than guest+non-recursive rights. */
        OVERBROAD_ANCESTOR("access.analysis.overbroadAncestor"),

        /** A group's entries are spread across unrelated branches of the structure tree. */
        MULTI_BRANCH_GROUP("access.analysis.multiBranchGroup"),
    }

    /** A project reduced to what the checks need: the manager group and the project name for the reason. */
    private class ProjektInfo(val projektManagerGroupId: Long, val name: String?)

    /**
     * Runs the given [check] over all (non-deleted) access entries.
     *
     * @return The flagged rows as `access-entry id -> already-translated reason`. Only flagged ids are
     * present; a row without an id (should not happen for persisted rows) is never flagged.
     */
    fun analyze(check: AnalysisCheck): Map<Long, String> {
        val accessList = accessDao.selectAll(checkAccess = false)
        return when (check) {
            AnalysisCheck.FOREIGN_PROJECT_GROUP -> foreignProjectGroup(accessList)
            AnalysisCheck.OVERBROAD_ANCESTOR -> overbroadAncestor(accessList)
            AnalysisCheck.MULTI_BRANCH_GROUP -> multiBranchGroup(accessList)
        }
    }

    /**
     * Flags every access row whose group is not the manager group of the project the row's task belongs to
     * (the nearest project up the tree). System groups (admin, finance, ...) legitimately span every
     * project and are never flagged.
     */
    private fun foreignProjectGroup(accessList: List<GroupTaskAccessDO>): Map<Long, String> {
        val projectByTaskId = loadProjectsByTaskId()
        if (projectByTaskId.isEmpty()) {
            return emptyMap()
        }
        val exemptGroupIds = systemGroupIds()
        val result = mutableMapOf<Long, String>()
        accessList.forEach { access ->
            val id = access.id ?: return@forEach
            val groupId = access.groupId ?: return@forEach
            val taskId = access.taskId ?: return@forEach
            if (groupId in exemptGroupIds) {
                return@forEach
            }
            val projekt = nearestProject(taskId, projectByTaskId) ?: return@forEach
            if (groupId != projekt.projektManagerGroupId &&
                !relatedGroupNames(groupId, projekt.projektManagerGroupId)
            ) {
                result[id] = translateMsg(
                    "access.analysis.reason.foreignProjectGroup",
                    groupName(groupId),
                    projekt.name ?: "?",
                    groupName(projekt.projektManagerGroupId),
                )
            }
        }
        return result
    }

    /**
     * Flags parent tasks that grant a group more than the wizard's minimal guest+non-recursive access,
     * where the same group has a real (recursive, more-than-guest) grant further down. Such a parent row
     * lets the group see the parent's other children (the leaf's siblings), which the wizard avoids.
     */
    private fun overbroadAncestor(accessList: List<GroupTaskAccessDO>): Map<Long, String> {
        // (groupId, taskId) -> row, so a group's row on any ancestor can be found directly.
        val byGroupAndTask = accessList
            .filter { it.groupId != null && it.taskId != null }
            .associateBy { it.groupId!! to it.taskId!! }
        val rootTaskId = taskTree.rootTaskNode.id
        val result = mutableMapOf<Long, String>()
        accessList.forEach { leaf ->
            val groupId = leaf.groupId ?: return@forEach
            val leafTaskId = leaf.taskId ?: return@forEach
            // Only a real grant (recursive and beyond guest) makes its ancestors' rights matter.
            if (!leaf.recursive || !isMoreThanGuest(leaf)) {
                return@forEach
            }
            val leafNode = taskTree.getTaskNodeById(leafTaskId) ?: return@forEach
            leafNode.ancestorIds.forEach { ancestorId ->
                if (ancestorId == rootTaskId) {
                    return@forEach
                }
                val ancestorRow = byGroupAndTask[groupId to ancestorId] ?: return@forEach
                val ancestorRowId = ancestorRow.id ?: return@forEach
                if (result.containsKey(ancestorRowId)) {
                    return@forEach
                }
                if (ancestorRow.recursive || isMoreThanGuest(ancestorRow)) {
                    result[ancestorRowId] = translateMsg(
                        "access.analysis.reason.overbroadAncestor",
                        taskTitle(ancestorId),
                        groupName(groupId),
                        taskTitle(leafTaskId),
                    )
                }
            }
        }
        return result
    }

    /**
     * Flags all rows of any group whose "real" grants (recursive or more-than-guest, i.e. not a pure
     * path-visibility guest row) sit in two or more unrelated branches of the tree. A single wizard run
     * produces one branch (a leaf plus its guest ancestor chain), so it is never flagged.
     */
    private fun multiBranchGroup(accessList: List<GroupTaskAccessDO>): Map<Long, String> {
        val byGroup = accessList.filter { it.groupId != null && it.taskId != null }.groupBy { it.groupId!! }
        val result = mutableMapOf<Long, String>()
        byGroup.forEach { (groupId, rows) ->
            // Pure guest+non-recursive rows are just path visibility; they don't open a branch of their own.
            val significantTaskIds = rows
                .filter { it.recursive || isMoreThanGuest(it) }
                .mapNotNull { it.taskId }
                .toSet()
            if (significantTaskIds.size < 2) {
                return@forEach
            }
            // Reduce to top-most tasks: drop any task that sits under another significant task of the group.
            val topMost = significantTaskIds.filter { taskId ->
                val ancestorIds = taskTree.getTaskNodeById(taskId)?.ancestorIds ?: emptyList()
                significantTaskIds.none { other -> other != taskId && other in ancestorIds }
            }
            if (topMost.size < 2) {
                return@forEach
            }
            val reason = translateMsg(
                "access.analysis.reason.multiBranchGroup",
                groupName(groupId),
                topMost.size,
            )
            rows.forEach { row -> row.id?.let { result[it] = reason } }
        }
        return result
    }

    /**
     * Loads every project that is bound to a task and has a manager group, as `taskId -> [ProjektInfo]`.
     * A plain projection (ids and name only) so nothing lazy is touched outside the read context.
     */
    private fun loadProjectsByTaskId(): Map<Long, ProjektInfo> {
        val rows = persistenceService.executeQuery(
            "select p.task.id, p.projektManagerGroup.id, p.name from ProjektDO p " +
                    "where p.deleted = false and p.task.id is not null and p.projektManagerGroup.id is not null",
            Array<Any?>::class.java,
        )
        val map = mutableMapOf<Long, ProjektInfo>()
        rows.forEach { row ->
            val taskId = (row[0] as? Number)?.toLong() ?: return@forEach
            val pmGroupId = (row[1] as? Number)?.toLong() ?: return@forEach
            val name = row.getOrNull(2) as? String
            map[taskId] = ProjektInfo(pmGroupId, name)
        }
        return map
    }

    /**
     * The project the given task belongs to: the task itself if it is a project's task, else the nearest
     * ancestor that is. Null if no ancestor carries a project.
     */
    private fun nearestProject(taskId: Long, projectByTaskId: Map<Long, ProjektInfo>): ProjektInfo? {
        projectByTaskId[taskId]?.let { return it }
        val node = taskTree.getTaskNodeById(taskId) ?: return null
        node.ancestorIds.forEach { ancestorId ->
            projectByTaskId[ancestorId]?.let { return it }
        }
        return null
    }

    /**
     * True if the row grants anything beyond the guest template (which is read-only on the tasks alone).
     */
    private fun isMoreThanGuest(access: GroupTaskAccessDO): Boolean {
        // guest(): TASKS select only, nothing else. Anything else is "more than guest".
        if (hasAnyPermission(access, AccessType.TASK_ACCESS_MANAGEMENT) ||
            hasAnyPermission(access, AccessType.TIMESHEETS) ||
            hasAnyPermission(access, AccessType.OWN_TIMESHEETS)
        ) {
            return true
        }
        return access.hasPermission(AccessType.TASKS, OperationType.INSERT) ||
                access.hasPermission(AccessType.TASKS, OperationType.UPDATE) ||
                access.hasPermission(AccessType.TASKS, OperationType.DELETE)
    }

    private fun hasAnyPermission(access: GroupTaskAccessDO, accessType: AccessType): Boolean {
        return access.hasPermission(accessType, OperationType.SELECT) ||
                access.hasPermission(accessType, OperationType.INSERT) ||
                access.hasPermission(accessType, OperationType.UPDATE) ||
                access.hasPermission(accessType, OperationType.DELETE)
    }

    /** The ids of the ProjectForge system groups, which legitimately hold access across every project. */
    private fun systemGroupIds(): Set<Long> {
        return ProjectForgeGroup.values().mapNotNull { userGroupCache.getGroup(it)?.id }.toSet()
    }

    private fun groupName(groupId: Long?): String {
        groupId ?: return "?"
        return userGroupCache.getGroup(groupId)?.name ?: "#$groupId"
    }

    /**
     * Whether two groups belong to the same project family by naming convention, so a team group's
     * access inside its own project is not reported as foreign. True when one group's name segments
     * (split on `-`/`_`) are a leading prefix of the other's: the manager group "aibotix-consulting-pl"
     * and the team group "aibotix-consulting" are recognised as related, while "aibotix-marketing"
     * (only "aibotix" in common, not a full prefix) is not.
     */
    private fun relatedGroupNames(groupIdA: Long, groupIdB: Long?): Boolean {
        val a = userGroupCache.getGroup(groupIdA)?.name?.let { splitGroupName(it) } ?: return false
        val b = (groupIdB?.let { userGroupCache.getGroup(it)?.name })?.let { splitGroupName(it) } ?: return false
        if (a.isEmpty() || b.isEmpty()) {
            return false
        }
        val (shorter, longer) = if (a.size <= b.size) a to b else b to a
        return longer.take(shorter.size) == shorter
    }

    /** A group name split into its lower-cased, non-blank segments on `-`/`_`, for [relatedGroupNames]. */
    private fun splitGroupName(name: String): List<String> {
        return name.trim().lowercase().split('-', '_').filter { it.isNotBlank() }
    }

    private fun taskTitle(taskId: Long?): String {
        taskId ?: return "?"
        return taskTree.getTaskById(taskId)?.title ?: "#$taskId"
    }
}
