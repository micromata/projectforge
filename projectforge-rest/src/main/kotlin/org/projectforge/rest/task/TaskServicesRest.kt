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

package org.projectforge.rest.task

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.KostHelper
import org.projectforge.business.task.*
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.common.i18n.Priority
import org.projectforge.common.task.TaskStatus
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.addTranslations
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDay
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.ListFilterService
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.core.aggrid.AGGridSupport
import org.projectforge.rest.core.aggrid.SortModelEntry
import org.projectforge.rest.dto.aggrid.AGGridStateRequest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * For serving the task tree as tree or table..
 */
@RestController
@RequestMapping("${Rest.URL}/task")
class TaskServicesRest {
    class Kost2(val id: Long, val title: String)

    // class OrderPosition(val number: Int, val personDays: Int?, val title: String, val status: AuftragsPositionsStatus?)
    class Order(
        val number: String,
        val title: String,
        val text: String,
        // val orderPositions: MutableList<OrderPosition>? = null
    ) // Positions

    enum class TreeStatus { LEAF, OPENED, CLOSED }
    class Task(
        val id: Long,
        /**
         * Indent is only given for table view.
         */
        var indent: Int? = null,
        /**
         * All (opened) sub notes for table view or direct child notes for tree view
         */
        var children: MutableList<Task>? = null,
        var treeStatus: TreeStatus? = null,
        val title: String? = null,
        val shortDescription: String? = null,
        val protectTimesheetsUntil: PFDay? = null,
        val reference: String? = null,
        val priority: Priority? = null,
        val status: TaskStatus? = null,
        val responsibleUser: PFUserDO? = null,
        /**
         * References used in time-sheets for this task, or any ancestor or descendant task.
         */
        var timesheetReferenceList: List<String>? = null,
        var kost2List: List<Kost2>? = null,
        /**
         * Kost2List as formatted numbers (separated in each line) for displaying in tooltip.
         */
        var kost2ListAsLines: String? = null,
        /**
         * Wild card form of kost2List, e. g. 5.123.456.*
         */
        var kost2WildCard: String? = null,
        var path: List<Task>? = null,
        var consumption: Consumption? = null,
        var orderList: MutableList<Order>? = null
    ) {
        val statusAsString: String? = status?.i18nKey?.let { translate(it) }

        constructor(node: TaskNode) : this(
            id = node.task.id!!,
            title = node.task.title,
            shortDescription = node.task.shortDescription,
            protectTimesheetsUntil = PFDay.fromOrNull(node.task.protectTimesheetsUntil),
            reference = node.task.reference,
            priority = node.task.priority,
            status = node.task.status,
            responsibleUser = node.task.responsibleUser
        )

        /**
         * Only for creating a pseudo empty task.
         */
        constructor(title: String) : this(
            id = -1, title = title
        )
    }

    class Result(
        val nodes: MutableList<Task> = mutableListOf(),
        var initFilter: TaskFilter? = null,
        var translations: MutableMap<String, String>? = null,
        var sortModel: List<SortModelEntry>? = null,
        var filterModel: Map<String, Any>? = null,
        var onColumnStatesChangedUrl: String? = null,
        var resetGridStateUrl: String? = null
    ) {
        var columnDefs: MutableList<UIAgGridColumnDef> = mutableListOf()
    }

    companion object {
        private const val PREF_ARA = "task"
        private const val GRID_CATEGORY = "taskTree"

        fun createTask(id: Long?): Task? {
            if (id == null)
                return null
            val taskTree = TaskTree.instance
            val taskNode = taskTree.getTaskNodeById(id) ?: return null
            val task = Task(taskNode)
            addKost2List(task)
            addTimesheetReferenceList(task)
            task.consumption = Consumption.create(taskNode)
            val pathToRoot = taskTree.getPathToRoot(taskNode.parentId)
            val pathArray = mutableListOf<Task>()
            pathToRoot.forEach {
                val ancestor = Task(id = it.task.id!!, title = it.task.title)
                pathArray.add(ancestor)
            }
            task.path = pathArray
            return task
        }

        fun addKost2List(task: Task, includeKost2ObjectList: Boolean = true) {
            val kost2DOList = TaskTree.instance.getKost2List(task.id)
            if (!kost2DOList.isNullOrEmpty()) {
                if (includeKost2ObjectList) {  // Only if needed in tree, save bandwidth...
                    val kost2List: List<Kost2> = kost2DOList.map {
                        Kost2(
                            it.id!!,
                            KostFormatter.instance.formatKost2(it, formatType = KostFormatter.FormatType.TEXT, 80),
                        )
                    }
                    task.kost2List = kost2List
                }
                task.kost2WildCard = KostHelper.getWildCardString(kost2DOList, "*")
                task.kost2ListAsLines = KostHelper.getFormattedNumberLines(kost2DOList)
            }
        }

        fun addTimesheetReferenceList(task: Task) {
            val timesheetReferenceList = listOf("Uni Kassel", "Uni Göttingen")
            task.timesheetReferenceList =
                timesheetReferenceList//Registry.getInstance().getDao(TimesheetDao::class.java).getUsedReferences(task.id)
        }
    }

    private class BuildContext(
        val result: Result,
        val user: PFUserDO,
        val taskFilter: TaskFilter,
        val openedNodes: MutableSet<Long>,
        var highlightedTaskNode: TaskNode? = null,
    )

    private val log = org.slf4j.LoggerFactory.getLogger(TaskServicesRest::class.java)

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var listFilterService: ListFilterService

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var agGridSupport: AGGridSupport

    /**
     * Creates default column definitions for the task tree grid.
     * @return MutableList of UIAgGridColumnDef with all default columns
     */
    private fun createDefaultColumnDefs(): MutableList<UIAgGridColumnDef> {
        val lc = LayoutContext(TaskDO::class.java)
        val kost2Visible = Configuration.instance.isCostConfigured
        val columnDefs = mutableListOf<UIAgGridColumnDef>()

        columnDefs.add(
            UIAgGridColumnDef.createCol(
                "title",
                headerName = translate("task"),
                valueFormatter = UIAgGridColumnDef.Formatter.TREE_NAVIGATION,
                sortable = false,
                width = UIAgGridColumnDef.DESCRIPTION_WIDTH,
                filter = false,
                pinnedAndLocked = UIAgGridColumnDef.Orientation.LEFT,
            )
        )
        columnDefs.add(
            UIAgGridColumnDef.createCol(
                "consumption",
                headerName = translate("task.consumption"),
                valueFormatter = UIAgGridColumnDef.Formatter.CONSUMPTION,
                sortable = false,
                filter = false,
            )
        )
        if (kost2Visible) {
            columnDefs.add(
                UIAgGridColumnDef.createCol(
                    "kost2WildCard",
                    headerName = translate("fibu.kost2"),
                    sortable = false,
                    width = 80,
                    filter = false,
                )
                    .withTooltipField("kost2ListAsLines")
            )
        }
        columnDefs.add(
            UIAgGridColumnDef.createCol(
                lc,
                "statusAsString",
                lcField = "status",
                sortable = false,
                width = 100,
                filter = false,
            )
        )
        columnDefs.add(
            UIAgGridColumnDef.createCol(lc, "shortDescription", sortable = false, filter = false)
        )
        columnDefs.add(
            UIAgGridColumnDef.createCol(lc, "responsibleUser", sortable = false, filter = false)
        )

        return columnDefs
    }

    /**
     * Gets the user's task tree as tree matching the filter. The open task nodes will be restored from the user's prefs.
     * @param initial If true, the layout info and translations are also returned. Default is to return only the tree data.
     * @param open Optional task to open in the tree (if a descendent child of closed tasks, all ancestor tasks will be opened as well).
     * @param close Optional task to close.
     * @param table If true, the result will be returned flat with indent counter of each task node, otherwise a tree object is returned.
     * @param opened Show opened tasks. For initial = true, this value is ignored.
     * @param notOpened Show un-opened tasks. For initial = true, this value is ignored.
     * @param closed Show closed tasks. For initial = true, this value is ignored.
     * @param deleted Show deleted tasks. For initial = true, this value is ignored.
     * @return json
     */
    @GetMapping("tree")
    fun getTree(
        request: HttpServletRequest,
        @RequestParam("initial") initial: Boolean?,
        @RequestParam("open") open: Long?,
        @RequestParam("close") close: Long?,
        @RequestParam("highlightedTaskId") highlightedTaskId: Long?,
        @RequestParam("table") table: Boolean?,
        @RequestParam("searchString") searchString: String?,
        @RequestParam("opened") opened: Boolean?,
        @RequestParam("notOpened") notOpened: Boolean?,
        @RequestParam("closed") closed: Boolean?,
        @RequestParam("deleted") deleted: Boolean?,
        @RequestParam("showRootForAdmins") showRootForAdmins: Boolean?
    )
            : Result {
        val openNodes = userPrefService.ensureEntry(PREF_ARA, TaskTree.USER_PREFS_KEY_OPEN_TASKS, mutableSetOf<Long>())
        val filter = listFilterService.getSearchFilter(request.getSession(false), TaskFilter::class.java) as TaskFilter

        if (initial != true) {
            // User filter settings not on initial call.
            // On initial calls the stored filter will be used and returned for restoring in the client.
            if (opened != null) filter.isOpened = opened
            if (notOpened != null) filter.isNotOpened = notOpened
            if (closed != null) filter.isClosed = closed
            if (deleted != null) filter.deleted = deleted
            filter.searchString = searchString
        }
        if (!filter.isStatusSet) {
            // Nothing will be found, so avoid no result by user's mistake:
            filter.isOpened = true
            filter.isNotOpened = true
        }
        val result = Result()
        val ctx = BuildContext(result, ThreadLocalUserContext.loggedInUser!!, filter, openNodes)
        if (highlightedTaskId != null) {
            ctx.highlightedTaskNode = taskTree.getTaskNodeById(highlightedTaskId)
        }
        openTask(ctx, open)
        closeTask(ctx, close)
        if (initial == true) {
            openTask(ctx, highlightedTaskId) // Only open on initial call.
        }
        //UserPreferencesHelper.putEntry(TaskTree.USER_PREFS_KEY_OPEN_TASKS, expansion.getIds(), true)
        filter.resetMatch() // taskFilter caches visibility, reset needed first.
        val indent = if (table == true) 0 else null
        val rootNode = taskTree.rootTaskNode
        val root = Task(rootNode)
        addKost2List(root)
        buildTree(ctx, root, rootNode, indent)
        if (showRootForAdmins == true && table == true && (accessChecker.isLoggedInUserMemberOfAdminGroup() ||
                    accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP))
        ) {
            // Append root node for admins and financial staff only in table view for displaying purposes.
            result.nodes.add(0, Task(rootNode))
        }
        val kost2Visible = Configuration.instance.isCostConfigured
        var ordersVisible = false
        var protectionUntilVisible = false
        var referenceVisible = false
        var priorityVisible = false
        var assignedUserVisible = false
        root.children?.forEach { task ->
            if (!ordersVisible && !task.orderList.isNullOrEmpty()) ordersVisible = true
            if (!protectionUntilVisible && task.protectTimesheetsUntil != null) protectionUntilVisible = true
            if (!referenceVisible && !task.reference.isNullOrBlank()) referenceVisible = true
            if (!priorityVisible && task.priority != null) priorityVisible = true
            if (!assignedUserVisible && task.responsibleUser != null) assignedUserVisible = true
        }
        if (initial == true) {
            // Create default column definitions
            result.columnDefs.addAll(createDefaultColumnDefs())

            // Set grid state URLs (with tree/ prefix to avoid conflict with TaskPagesRest)
            result.onColumnStatesChangedUrl = RestResolver.getRestUrl(this::class.java, "tree/${RestPaths.SET_COLUMN_STATES}")
            result.resetGridStateUrl = RestResolver.getRestUrl(this::class.java, "tree/resetGridState")

            // Create temporary UIAgGrid to restore user preferences
            val agGrid = UIAgGrid("taskTree")
            result.columnDefs.forEach { agGrid.add(it) }
            agGridSupport.restoreColumnsFromUserPref(GRID_CATEGORY, agGrid)

            // Copy restored state back to result
            result.columnDefs = agGrid.columnDefs
            result.sortModel = agGrid.sortModel
            result.filterModel = agGrid.filterModel

            result.initFilter = filter
            result.translations = addTranslations(
                "deleted",
                "search",
                "task.selectPanel.info", // Alert box at the end.
                "task.status.closed",
                "task.status.notOpened",
                "task.status.opened",
            )
        }
        return result
    }

    /**
     * Gets the task data including kost2 information if any and its path.
     * @param id Task id.
     * @return json
     */
    @GetMapping("info/{id}")
    fun getTaskInfo(@PathVariable("id") id: Long?): ResponseEntity<Task> {
        val task = createTask(id) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity(task, HttpStatus.OK)
    }

    /**
     * @param indent null for tree view, int for table view.
     */
    private fun buildTree(ctx: BuildContext, task: Task, taskNode: TaskNode, indent: Int? = null) {
        if (!taskNode.hasChildren()) {
            task.treeStatus = TreeStatus.LEAF
            return
        }
        if (taskNode.isRootNode || ctx.openedNodes.contains(taskNode.taskId)) {
            task.treeStatus = TreeStatus.OPENED
            val children = taskNode.children.toMutableList()
            children.sortBy { it.task.title }
            children.forEach { node ->
                if (ctx.taskFilter.match(node, taskDao, ctx.user) &&
                    taskDao.hasUserSelectAccess(ctx.user, node.getTask(), false)
                ) {
                    val child = Task(node)
                    addKost2List(child, false)
                    child.consumption = Consumption.create(node)
                    if (indent != null) {
                        var hidden = false
                        val highlightedTaskNode = ctx.highlightedTaskNode
                        if (highlightedTaskNode != null) {
                            // Show only ancestor, the highlighted node itself and descendants. siblings only for leafs.
                            // Following if-else cascade should be written much shorter, but less understandable!
                            if (highlightedTaskNode.isRootNode) {
                                // Show all nodes, because they are descendants of the root node.
                            } else if (highlightedTaskNode.ancestorIds.contains(node.id)) {
                                log.debug("Current node ${node.task.title} is ancestor of highlighted node: ${!hidden}")
                                // Don't show ancestor nodes:
                                hidden = true
                                // But proceed with child nodes:
                                buildTree(
                                    ctx,
                                    child,
                                    node,
                                    indent
                                ) // Build as table (all children are direct children of root node.
                            } else if (!highlightedTaskNode.hasChildren()) {
                                // Node is a leaf node, so show also all siblings:
                                hidden = !node.ancestorIds.contains(highlightedTaskNode.parent.id)
                                log.debug("Current node ${node.task.title} is sibling of highlighted node: ${!hidden}")
                            } else {
                                hidden =
                                    !(highlightedTaskNode.taskId == node.taskId ||      // highlighted node == current?
                                            node.ancestorIds.contains(highlightedTaskNode.id)) // node is descendant of highlighted?
                                log.debug("Current node ${node.task.title} is descendant of highlighted node: ${!hidden}")
                            }
                        }
                        if (!hidden) {
                            ctx.result.nodes.add(child) // All children are added to root task (table view!)
                            child.indent = indent
                            buildTree(
                                ctx,
                                child,
                                node,
                                indent + 1
                            ) // Build as table (all children are direct children of root node.
                        }
                    } else {
                        // TaskNode has children and is opened:
                        if (task.children == null)
                            task.children = mutableListOf()
                        task.children!!.add(child)
                        buildTree(ctx, child, node, null) // Build as tree
                    }
                }
            }
        } else {
            task.treeStatus = TreeStatus.CLOSED
        }
    }

    private fun openTask(ctx: BuildContext, taskId: Long?) {
        if (taskId == null)
            return
        val taskNode = taskTree.getTaskNodeById(taskId)
        if (taskNode == null) {
            log.warn("Task with id $taskId not found to open.")
            return
        }
        ctx.openedNodes.add(taskId)
        var parent = taskNode.parent
        while (parent != null) {
            ctx.openedNodes.add(parent.taskId)
            parent = parent.parent
        }
    }

    private fun closeTask(ctx: BuildContext, taskId: Long?) {
        if (taskId == null)
            return
        val taskNode = taskTree.getTaskNodeById(taskId)
        if (taskNode == null) {
            log.warn("Task with id $taskId not found to close.")
            return
        }
        ctx.openedNodes.remove(taskId)
    }

    /**
     * Saves AG-Grid state (column order, width, visibility, filters, etc.) for task tree.
     */
    @PostMapping("tree/${RestPaths.SET_COLUMN_STATES}")
    fun updateColumnStates(@Valid @RequestBody gridStateRequest: AGGridStateRequest): String {
        agGridSupport.storeGridState(
            GRID_CATEGORY,
            gridStateRequest.columnState,
            gridStateRequest.filterModel
        )
        return "OK"
    }

    /**
     * Resets the AG Grid state to defaults and returns fresh column definitions for task tree.
     */
    @GetMapping("tree/resetGridState")
    fun resetGridState(): ResponseAction {
        agGridSupport.resetGridState(GRID_CATEGORY)

        // Rebuild fresh columnDefs with defaults using shared function
        val agGrid = UIAgGrid("taskTree")
        createDefaultColumnDefs().forEach { agGrid.add(it) }

        // Create ResponseAction using AGGridSupport helper
        return agGridSupport.createResetGridStateResponse(agGrid)
    }
}
