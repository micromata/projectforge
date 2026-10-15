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

package org.projectforge.rest

import org.projectforge.business.PfCaches
import org.projectforge.business.task.TaskAccessAnalysisService
import org.projectforge.business.task.TaskFormatter
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.GroupTaskAccess
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.AutoCompletion
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterBooleanElement
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.projectforge.ui.filter.UIFilterObjectElement
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The access-rights list and edit page (`access.page.tsx` in projectforge-next) — the group/task
 * permission matrix, migrated from the Wicket `AccessListPage`/`AccessEditPage`.
 *
 * Layout free ([AbstractDTOEntityRest]): the hand built next page brings its own layout, so there is no
 * server side `createListLayout`/`createEditLayout` here. There is no way back to the Wicket page
 * (`NextMigration`, `offerLegacyLink = false`); its old urls are redirected by `OrphanedLinkFilter`.
 */
@RestController
@RequestMapping("${Rest.URL}/access")
class GroupAccessEntityRest :
    AbstractDTOEntityRest<GroupTaskAccessDO, GroupTaskAccess, AccessDao>(AccessDao::class.java, "access.title") {

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    @Autowired
    private lateinit var taskAccessAnalysisService: TaskAccessAnalysisService

    /**
     * The analysis reasons of the current request (analysis mode only), stashed by [preProcessMagicFilter]
     * and read per row by [transformFromDB]: this bean is a singleton, so the per-request map cannot live in
     * an instance field. Both callbacks run on the same request thread during list assembly; cleared
     * whenever no analysis check is selected so it never leaks to a later request on a pooled thread.
     */
    private val analysisReasonHolder = ThreadLocal<Map<Long, String>>()

    override fun transformFromDB(obj: GroupTaskAccessDO, editMode: Boolean): GroupTaskAccess {
        // Resolve the id-only group and task from the caches so the DTO carries their display names for the
        // list columns and the autocompletes (the DO serializes both id-only, see GroupTaskAccessDO).
        obj.group = caches.getGroup(obj.groupId) ?: obj.group
        obj.task = caches.getTask(obj.taskId) ?: obj.task
        val dto = GroupTaskAccess()
        dto.copyFrom(obj)
        dto.analysisReason = obj.id?.let { analysisReasonHolder.get()?.get(it) }
        return dto
    }

    override fun transformForDB(dto: GroupTaskAccess): GroupTaskAccessDO {
        val obj = GroupTaskAccessDO()
        dto.copyTo(obj)
        return obj
    }

    /**
     * Task and group are required (`@ManyToOne` references, which [org.projectforge.rest.core.ValidationUtils]
     * does not check), as Wicket's AccessEditForm required them. There is one entry per task and group: a pair
     * that already exists is refused with Wicket's message (`AccessEditPage.create`) — unless it is marked as
     * deleted, then the save restores it ([onBeforeSave]).
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: GroupTaskAccess) {
        super.validate(validationErrors, dto)
        val taskId = dto.task?.id
        val groupId = dto.group?.id
        if (taskId == null) {
            validationErrors.add(
                ValidationError(translateMsg("validation.error.fieldRequired", translate("task")), fieldId = "task")
            )
        }
        if (groupId == null) {
            validationErrors.add(
                ValidationError(translateMsg("validation.error.fieldRequired", translate("group")), fieldId = "group")
            )
        }
        if (taskId == null || groupId == null) {
            return
        }
        val existing = findEntry(taskId, groupId) ?: return
        if (existing.id == dto.id || (dto.id == null && existing.deleted)) {
            return
        }
        validationErrors.add(
            ValidationError(
                translateMsg(
                    "access.exception.standard",
                    caches.getTask(taskId)?.title ?: dto.task?.title,
                    caches.getGroup(groupId)?.name ?: dto.group?.name,
                ),
                fieldId = "task",
            )
        )
    }

    /**
     * A new entry for a task and group whose entry is marked as deleted restores that one with the posted
     * values instead of inserting a second (refused by the unique constraint), as Wicket's
     * `AccessEditPage.create` did. The following insertOrUpdate then is an update without changes.
     */
    override fun onBeforeSave(request: HttpServletRequest, obj: GroupTaskAccessDO, postData: PostData<GroupTaskAccess>) {
        super.onBeforeSave(request, obj, postData)
        val taskId = obj.taskId ?: return
        val groupId = obj.groupId ?: return
        val existing = findEntry(taskId, groupId) ?: return
        if (existing.deleted) {
            obj.id = existing.id
            baseDao.undelete(obj)
        }
    }

    private fun findEntry(taskId: Long, groupId: Long): GroupTaskAccessDO? {
        val task = caches.getTask(taskId) ?: return null
        val group = caches.getGroup(groupId) ?: return null
        return baseDao.getEntry(task, group)
    }

    /**
     * Sorts the "task" list column in task-tree order rather than by anything SQL can express: the key is the
     * task's path to the root ("Micromata -> Business Unit -> ProjectForge", see [TaskFormatter.getTaskPath]),
     * so an ancestor precedes its descendants and siblings group together. This is what makes the list read as
     * a tree — the frontend then indents each row by the task's depth (access.page.tsx). A row whose task is
     * gone sorts last. Applied in memory over the whole (small) result set, see [computedSortProperties].
     */
    override val computedSortProperties: Map<String, (GroupTaskAccessDO) -> Comparable<*>?>
        get() = mapOf(
            "task" to { access -> TaskFormatter.getTaskPath(access.taskId) ?: "￿" }
        )

    /**
     * The sticky filters of the Wicket `AccessListForm`: pick a group, a user or a structure element (task),
     * and toggle how the task selection spreads through the tree. Group and task are ordinary predicates; the
     * user is synthetic (no `user` property on [GroupTaskAccessDO] — it is a post-filter over the group
     * membership, see [preProcessMagicFilter]).
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        // Drop the free-text pills the generic processor derives from the indexed group/task fields; the
        // object pickers below replace them (as OrderEntityRest/TimesheetEntityRest strip their derived pills).
        elements.removeIf {
            it is UIFilterElement && (it.id == "group" || it.id.startsWith("group.") ||
                    it.id == "task" || it.id.startsWith("task."))
        }
        elements.add(
            UIFilterObjectElement(
                "group",
                label = translate("group"),
                autoCompletion = AutoCompletion.getAutoCompletion4Groups(),
            ).also { it.defaultFilter = true }
        )
        elements.add(
            // Synthetic: no `user` property on GroupTaskAccessDO — consumed as a group-membership post-filter.
            UIFilterObjectElement(
                "user",
                label = translate("user"),
                autoCompletion = AutoCompletion.getAutoCompletion4Users(),
            ).also {
                it.defaultFilter = true
                it.tooltip = translate("access.tooltip.filter.user")
            }
        )
        elements.add(
            UIFilterObjectElement(
                "task",
                label = translate("task"),
                // Marked TASK so the next frontend uses the structure-tree picker and shows the task path
                // in the pill (FilterTaskField); its own `task/tree` type-ahead, as TimesheetEntityRest does.
                autoCompletion = AutoCompletion<Long>(
                    url = AutoCompletion.getAutoCompletionUrl("task/tree"),
                    type = AutoCompletion.Type.TASK.name,
                ),
            ).also { it.defaultFilter = true }
        )
        // The three options of the legacy list form (AccessListForm), all default off (see AccessFilter):
        // inherit ancestor rights, and additionally include the task's structure ancestors / descendants.
        // Tooltips carried over from the Wicket form (which explained each option via a help icon).
        elements.add(
            UIFilterBooleanElement("inherit", label = translate("inherit"), defaultFilter = true)
                .also { it.tooltip = translate("access.tooltip.filter.inherit") }
        )
        elements.add(
            UIFilterBooleanElement(
                "includeAncestorTasks",
                label = translate("access.filter.includeAncestorTasks"),
                defaultFilter = true,
            ).also { it.tooltip = translate("access.tooltip.filter.includeAncestorTasks") }
        )
        elements.add(
            UIFilterBooleanElement(
                "includeDescendentTasks",
                label = translate("access.filter.includeDescendentTasks"),
                defaultFilter = true,
            ).also { it.tooltip = translate("access.tooltip.filter.includeDescendentTasks") }
        )
        // Anomaly inspection: pick one check to narrow the list to the likely-erroneous entries, each shown
        // with a reason (TaskAccessAnalysisService). Single-select, off by default.
        elements.add(
            UIFilterListElement("analysisCheck", label = translate("access.analysis.filter"), multi = false)
                .buildValues(TaskAccessAnalysisService.AnalysisCheck::class.java)
                .also { it.tooltip = translate("access.tooltip.filter.analysisCheck") }
        )
    }

    /**
     * Ports `AccessDao.select(BaseSearchFilter)` to the magic filter: builds the task-tree predicate from the
     * picked task and the three toggles, the group predicate, and returns the two post-filters that cannot be
     * expressed as a query (the non-recursive inherited-ancestor cleanup and the user-membership filter).
     */
    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter,
    ): List<CustomResultFilter<GroupTaskAccessDO>> {
        val filters = mutableListOf<CustomResultFilter<GroupTaskAccessDO>>()

        // Analysis mode: run the picked check once over the whole table, keep only the flagged rows, and
        // stash the id -> reason map for transformFromDB. Cleared first so a request without a check (or
        // with an unknown value) never sees a previous request's reasons on this pooled thread.
        analysisReasonHolder.remove()
        source.entries.find { it.field == "analysisCheck" }?.let { analysisEntry ->
            analysisEntry.synthetic = true
            val value = analysisEntry.value.value ?: analysisEntry.value.values?.singleOrNull()
            val check = value?.let {
                runCatching { TaskAccessAnalysisService.AnalysisCheck.valueOf(it) }.getOrNull()
            }
            if (check != null) {
                val reasons = taskAccessAnalysisService.analyze(check)
                analysisReasonHolder.set(reasons)
                filters.add(object : CustomResultFilter<GroupTaskAccessDO> {
                    override fun match(list: MutableList<GroupTaskAccessDO>, element: GroupTaskAccessDO): Boolean {
                        return reasons.containsKey(element.id)
                    }
                })
            }
        }

        val inheritEntry = source.entries.find { it.field == "inherit" }
        inheritEntry?.synthetic = true
        val inherit = inheritEntry?.value?.value == "true"
        val ancestorEntry = source.entries.find { it.field == "includeAncestorTasks" }
        ancestorEntry?.synthetic = true
        val includeAncestorTasks = ancestorEntry?.value?.value == "true"
        val descendantEntry = source.entries.find { it.field == "includeDescendentTasks" }
        descendantEntry?.synthetic = true
        val includeDescendentTasks = descendantEntry?.value?.value == "true"

        var filterTaskId: Long? = null
        source.entries.find { it.field == "task" }?.let { taskEntry ->
            taskEntry.synthetic = true
            val taskId = taskEntry.value.id ?: taskEntry.value.value?.toLongOrNull()
            if (taskId != null) {
                filterTaskId = taskId
                val node = taskTree.getTaskNodeById(taskId)
                val descendants = if (includeDescendentTasks) node?.descendantIds else null
                val ancestors = if (inherit || includeAncestorTasks) node?.ancestorIds else null
                if (descendants != null || ancestors != null) {
                    val taskIds = mutableListOf<Long>()
                    descendants?.let { taskIds.addAll(it) }
                    ancestors?.let { taskIds.addAll(it) }
                    node?.id?.let { taskIds.add(it) }
                    target.add(QueryFilter.isIn<Any>("task.id", taskIds))
                } else {
                    target.add(QueryFilter.eq("task.id", taskId))
                }
            }
        }

        source.entries.find { it.field == "group" }?.let { groupEntry ->
            groupEntry.synthetic = true
            val groupId = groupEntry.value.id ?: groupEntry.value.value?.toLongOrNull()
            if (groupId != null) {
                target.add(QueryFilter.eq("group.id", groupId))
            }
        }

        // Post-filter (a): when inheriting without explicitly including ancestors, drop the non-recursive
        // rights inherited from an ancestor task — AccessDao.select does this after the query (AccessDao.kt).
        val cleanupTaskId = filterTaskId
        if (cleanupTaskId != null && inherit && !includeAncestorTasks) {
            val taskNode = taskTree.getTaskNodeById(cleanupTaskId)
            if (taskNode != null) {
                filters.add(object : CustomResultFilter<GroupTaskAccessDO> {
                    override fun match(list: MutableList<GroupTaskAccessDO>, element: GroupTaskAccessDO): Boolean {
                        if (!element.recursive) {
                            val accessNode = taskTree.getTaskNodeById(element.taskId)
                            if (accessNode?.isParentOf(taskNode) == true) {
                                return false
                            }
                        }
                        return true
                    }
                })
            }
        }

        // Post-filter (b): keep only the rights of groups the picked user is a member of (AccessDao.select).
        source.entries.find { it.field == "user" }?.let { userEntry ->
            userEntry.synthetic = true
            val userId = userEntry.value.id ?: userEntry.value.value?.toLongOrNull()
            if (userId != null) {
                filters.add(object : CustomResultFilter<GroupTaskAccessDO> {
                    override fun match(list: MutableList<GroupTaskAccessDO>, element: GroupTaskAccessDO): Boolean {
                        return userGroupCache.isUserMemberOfGroup(userId, element.groupId)
                    }
                })
            }
        }

        return filters
    }
}
