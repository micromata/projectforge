package org.projectforge.rest.task

import org.projectforge.business.fibu.AuftragsPositionsStatus
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskFilter
import org.projectforge.business.task.TaskNode
import org.projectforge.business.task.TaskTree
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.common.i18n.Priority
import org.projectforge.common.task.TaskStatus
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.addTranslations
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDate
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.ListFilterService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

/**
 * For serving the task tree as tree or table..
 */
@RestController
@RequestMapping("${Rest.URL}/task")
class TaskServicesRest {
    class Kost2(val id: Int, val title: String)
    class OrderPosition(val number: Int, val personDays: Int?, val title: String, status: AuftragsPositionsStatus?)
    class Order(val number: String,
                val title: String,
                val text: String,
                val orderPositions: MutableList<OrderPosition>? = null) // Positions

    enum class TreeStatus() { LEAF, OPENED, CLOSED }
    class Task(val id: Int,
               /**
                * Indent is only given for table view.
                */
               var indent: Int? = null,
               /**
                * All (opened) sub notes for table view or direct child notes for tree view
                */
               var childs: MutableList<Task>? = null,
               var treeStatus: TreeStatus? = null,
               val title: String? = null,
               val shortDescription: String? = null,
               val protectTimesheetsUntil: PFDate? = null,
               val reference: String? = null,
               val priority: Priority? = null,
               val status: TaskStatus? = null,
               val responsibleUser: PFUserDO? = null,
               var kost2List: List<Kost2>? = null,
               var path: List<Task>? = null,
               var consumption: Consumption? = null,
               var orderList: MutableList<Order>? = null) {
        constructor(node: TaskNode) : this(id = node.task.id, title = node.task.title, shortDescription = node.task.shortDescription,
                protectTimesheetsUntil = PFDate.from(node.task.protectTimesheetsUntil), reference = node.task.reference,
                priority = node.task.priority, status = node.task.status, responsibleUser = node.task.responsibleUser)
    }

    class Result(val root: Task,
                 var initFilter: TaskFilter? = null,
                 var translations: MutableMap<String, String>? = null) {
        /** Only for table view: If optional columns are empty for all returned tasks, they are marked as false in this map. */
        var columnsVisibility: MutableMap<String, Boolean>? = null
    }

    companion object {
        fun createTask(id: Int?): Task? {
            if (id == null)
                return null
            val taskTree = TaskTreeHelper.getTaskTree()
            val taskNode = taskTree.getTaskNodeById(id) ?: return null
            val task = Task(taskNode)
            addKost2List(task)
            task.consumption = Consumption.create(taskNode)
            val pathToRoot = taskTree.getPathToRoot(taskNode.parentId)
            val pathArray = mutableListOf<Task>()
            pathToRoot?.forEach {
                val ancestor = Task(id = it.task.id, title = it.task.title)
                pathArray.add(ancestor)
            }
            task.path = pathArray
            return task
        }

        fun addKost2List(task: Task) {
            val kost2DOList = TaskTreeHelper.getTaskTree().getKost2List(task.id)
            if (!kost2DOList.isNullOrEmpty()) {
                val kost2List: List<Kost2> = kost2DOList.map {
                    Kost2((it as Kost2DO).id, KostFormatter.format(it, 80))
                }
                task.kost2List = kost2List
            }
        }
    }

    private class BuildContext(val user: PFUserDO,
                               val taskFilter: TaskFilter,
                               val rootTask: Task, // Only for table view.
                               val openedNodes: MutableSet<Int>,
                               var highlightedTaskNode: TaskNode? = null)

    private val log = org.slf4j.LoggerFactory.getLogger(TaskServicesRest::class.java)

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var listFilterService: ListFilterService

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var userPreferencesService: UserPreferencesService

    private val taskTree: TaskTree
        /** Lazy init, because test cases failed due to NPE in TenantRegistryMap. */
        get() = TaskTreeHelper.getTaskTree()

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
    fun getTree(request: HttpServletRequest,
                @RequestParam("initial") initial: Boolean?,
                @RequestParam("open") open: Int?,
                @RequestParam("close") close: Int?,
                @RequestParam("highlightedTaskId") highlightedTaskId: Int?,
                @RequestParam("table") table: Boolean?,
                @RequestParam("searchString") searchString: String?,
                @RequestParam("opened") opened: Boolean?,
                @RequestParam("notOpened") notOpened: Boolean?,
                @RequestParam("closed") closed: Boolean?,
                @RequestParam("deleted") deleted: Boolean?,
                @RequestParam("showRootForAdmins") showRootForAdmins: Boolean?)
            : Result {
        @Suppress("UNCHECKED_CAST")
        val openNodes = userPreferencesService.getEntry(TaskTree.USER_PREFS_KEY_OPEN_TASKS) as MutableSet<Int>
        val filter = listFilterService.getSearchFilter(request.session, TaskFilter::class.java) as TaskFilter

        if (opened != null) filter.isOpened = opened
        if (notOpened != null) filter.isNotOpened = notOpened
        if (closed != null) filter.isClosed = closed
        if (deleted != null) filter.isDeleted = deleted
        filter.setSearchString(searchString);

        val rootNode = taskTree.rootTaskNode
        val root = Task(rootNode)
        addKost2List(root)
        root.childs = mutableListOf()
        val ctx = BuildContext(ThreadLocalUserContext.getUser(), filter, root, openNodes)
        if (highlightedTaskId != null) {
            ctx.highlightedTaskNode = taskTree.getTaskNodeById(highlightedTaskId)
        }
        openTask(ctx, open)
        closeTask(ctx, close)
        if (initial == true)
            openTask(ctx, highlightedTaskId) // Only open on initial call.
        //UserPreferencesHelper.putEntry(TaskTree.USER_PREFS_KEY_OPEN_TASKS, expansion.getIds(), true)
        filter.resetMatch() // taskFilter caches visibility, reset needed first.
        val indent = if (table == true) 0 else null
        buildTree(ctx, root, rootNode, indent)
        if (showRootForAdmins == true && table == true && (accessChecker.isLoggedInUserMemberOfAdminGroup() ||
                        accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP))) {
            // Append root node for admins and financial staff only in table view for displaying purposes.
            root.childs?.add(Task(rootNode))
        }
        val result = Result(root)
        if (table == true) {
            val columnsVisibility = mutableMapOf<String, Boolean>()
            columnsVisibility["consumption"] = true
            columnsVisibility["shortDescription"] = true
            columnsVisibility["status"] = true
            root.childs?.forEach { task ->
                registerVisibleColumn(columnsVisibility, "kost2", !task.kost2List.isNullOrEmpty())
                registerVisibleColumn(columnsVisibility, "orders", !task.orderList.isNullOrEmpty())
                registerVisibleColumn(columnsVisibility, "protectionUntil", task.protectTimesheetsUntil != null)
                registerVisibleColumn(columnsVisibility, "reference", !task.reference.isNullOrBlank())
                registerVisibleColumn(columnsVisibility, "priority", task.priority != null)
                registerVisibleColumn(columnsVisibility, "assignedUser", task.responsibleUser != null)
            }
            result.columnsVisibility = columnsVisibility
        }
        if (initial == true) {
            result.initFilter = filter
            result.translations = addTranslations(
                    "deleted",
                    "fibu.auftrag.auftraege",
                    "fibu.kost2",
                    "priority",
                    "search",
                    "searchFilter",
                    "shortDescription",
                    "status",
                    "task",
                    "task.assignedUser",
                    "task.consumption",
                    "task.protectTimesheetsUntil",
                    "task.protectTimesheetsUntil.short",
                    "task.status.closed",
                    "task.status.notOpened",
                    "task.status.opened",
                    "task.reference",
                    "task.tree.info")
        }
        return result
    }

    private fun registerVisibleColumn(columnsVisibility: MutableMap<String, Boolean>, column: String, value: Boolean) {
        if (columnsVisibility[column] == null) {
            columnsVisibility[column] = value // Store first value (true or false).
        } else if (value && columnsVisibility[column] != true) {
            columnsVisibility[column] = true
        }
    }

    /**
     * Gets the task data including kost2 information if any and its path.
     * @param id Task id.
     * @return json
     */
    @GetMapping("info/{id}")
    fun getTaskInfo(@PathVariable("id") id: Int?): ResponseEntity<Task> {
        val task = createTask(id)
        if (task == null)
            return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity(task, HttpStatus.OK)
    }

    /**
     * @param indent null for tree view, int for table view.
     */
    private fun buildTree(ctx: BuildContext, task: Task, taskNode: TaskNode, indent: Int? = null) {
        if (!taskNode.hasChilds()) {
            task.treeStatus = TreeStatus.LEAF
            return
        }
        if (ctx.openedNodes.contains(taskNode.taskId)) {
            task.treeStatus = TreeStatus.OPENED
            val childs = taskNode.childs.toMutableList()
            childs.sortBy({ it.task.title })
            childs.forEach { node ->
                if (ctx.taskFilter.match(node, taskDao, ctx.user)) {
                    val child = Task(node)
                    addKost2List(child)
                    child.consumption = Consumption.create(node)
                    if (indent != null) {
                        var hidden = false;
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
                                buildTree(ctx, child, node, indent) // Build as table (all childs are direct childs of root node.
                            } else if (!highlightedTaskNode.hasChilds()) {
                                // Node is a leaf node, so show also all siblings:
                                hidden = !node.ancestorIds.contains(highlightedTaskNode.parent.id)
                                log.debug("Current node ${node.task.title} is sibling of highlighted node: ${!hidden}")
                            } else {
                                hidden = !(highlightedTaskNode.taskId == node.taskId ||      // highlighted node == current?
                                        node.ancestorIds.contains(highlightedTaskNode.id)) // node is descendant of highlighted?
                                log.debug("Current node ${node.task.title} is descendant of highlighted node: ${!hidden}")
                            }
                        }
                        if (!hidden) {
                            ctx.rootTask.childs!!.add(child) // All childs are added to root task (table view!)
                            child.indent = indent
                            buildTree(ctx, child, node, indent + 1) // Build as table (all childs are direct childs of root node.
                        }
                    } else {
                        // TaskNode has childs and is opened:
                        if (task.childs == null)
                            task.childs = mutableListOf()
                        task.childs!!.add(child)
                        buildTree(ctx, child, node, null) // Build as tree
                    }
                }
            }
        } else {
            task.treeStatus = TreeStatus.CLOSED
        }
    }

    private fun openTask(ctx: BuildContext, taskId: Int?) {
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

    private fun closeTask(ctx: BuildContext, taskId: Int?) {
        if (taskId == null)
            return
        val taskNode = taskTree.getTaskNodeById(taskId)
        if (taskNode == null) {
            log.warn("Task with id $taskId not found to close.")
            return
        }
        ctx.openedNodes.remove(taskId)
    }
}
