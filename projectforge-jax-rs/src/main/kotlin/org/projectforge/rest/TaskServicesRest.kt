package org.projectforge.rest

import org.projectforge.business.fibu.AuftragsPositionsStatus
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskFilter
import org.projectforge.business.task.TaskNode
import org.projectforge.business.task.TaskTree
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.business.user.service.UserPreferencesService
import org.projectforge.common.i18n.Priority
import org.projectforge.common.task.TaskStatus
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDate
import org.projectforge.rest.core.RestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * For uploading address immages.
 */
@Component
@Path("task")
class TaskServicesRest() {
    class Cost2(val number: String, val title: String)
    class OrderPosition(val number: Int, val personDays: Int?, val title: String, status: AuftragsPositionsStatus?)
    class Order(val number: String,
                val title: String,
                val text: String,
                val orderPositions: MutableList<OrderPosition>? = null) // Positions

    class Task(val id: Int,
               /**
                * Indent is only given for table view.
                */
               var indent: Int? = null,
               /**
                * All (opened) sub notes for table view or direct child notes for tree view
                */
               var childs: MutableList<Task>? = null,
               val leaf: Boolean = true,
               val title: String? = null,
               val shortDescription: String? = null,
               val protectTimesheetsUntil: PFDate? = null,
               val reference: String?,
               val priority: Priority? = null,
               val status: TaskStatus? = null,
               val responsibleUser: PFUserDO? = null,
               val cost2List: MutableList<Cost2>? = null) {
        constructor(node: TaskNode) : this(id = node.task.id, leaf = !node.hasChilds(), title = node.task.title, shortDescription = node.task.shortDescription,
                protectTimesheetsUntil = PFDate.from(node.task.protectTimesheetsUntil), reference = node.task.reference, priority = node.task.priority,
                status = node.task.status, responsibleUser = node.task.responsibleUser) {
        }
    }

    class Result(val root: Task,
                 var translations: MutableMap<String, String>? = null)

    private class BuildContext(val user: PFUserDO,
                               val taskFilter: TaskFilter,
                               val openedNodes: MutableSet<Int>)

    private val log = org.slf4j.LoggerFactory.getLogger(TaskServicesRest::class.java)

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var userPreferencesService: UserPreferencesService

    private val taskTree = TaskTreeHelper.getTaskTree()

    private val restHelper = RestHelper()

    /**
     * Gets the user's task tree as tree matching the filter. The open task nodes will be restored from the user's prefs.
     * @param initial If true, the layout info and translations are also returned. Default is to return only the tree data.
     * @param open Optional task to open in the tree (if a descendent child of closed tasks, all ancestor tasks will be opened as well).
     * @param close Optional task to close.
     * @return json
     */
    @GET
    @Path("tree")
    @Produces(MediaType.APPLICATION_JSON)
    fun getTree(@QueryParam("initial") initial: Boolean?,
                @QueryParam("open") open: Int?,
                @QueryParam("close") close: Int?)
            : Response {
        val openNodes = userPreferencesService.getEntry(TaskTree.USER_PREFS_KEY_OPEN_TASKS) as MutableSet<Int>
        val ctx = BuildContext(ThreadLocalUserContext.getUser(), TaskFilter(), openNodes)
        openTask(ctx, open)
        closeTask(ctx, close)
        val rootNode = taskTree.rootTaskNode
        val root = Task(rootNode)
        //UserPreferencesHelper.putEntry(TaskTree.USER_PREFS_KEY_OPEN_TASKS, expansion.getIds(), true)
        val indent = if (initial == true) 0 else null
        buildTree(ctx, root, rootNode, indent)
        val result = Result(root)
        if (initial == true) {
            result.translations = mutableMapOf()
            val translations = result.translations!!
            translations.put("task", translate("task"))
            translations.put("task.consumption", translate("task.consumption"))
        }
        return restHelper.buildResponse(result)
    }

    /**
     * @param indent null for tree view, int for table view.
     */
    private fun buildTree(ctx: BuildContext, task: Task, taskNode: TaskNode, indent: Int? = null) {
        if (!taskNode.hasChilds()) {
            return
        }
        if (ctx.openedNodes.contains(taskNode.taskId)) {
            // TaskNode has childs and is opened:
            if (task.childs == null)
                task.childs = mutableListOf()
            val childs = taskNode.childs.toMutableList()
            childs.sortBy({ it.task.title })
            childs.forEach {
                if (ctx.taskFilter.match(it, taskDao, ctx.user)) {
                    val child = Task(it)
                    task.childs!!.add(child)
                    if (indent != null) {
                        child.indent = indent
                        buildTree(ctx, task, it, indent + 1) // Build as table (all childs are direct childs of root node.
                    } else
                        buildTree(ctx, child, it, null) // Build as tree
                }
            }
        }
    }

    private fun openTask(ctx: BuildContext, taskId: Int?) {
        if (taskId == null)
            return
        val taskNode = taskTree.getTaskNodeById(taskId)
        if (taskNode == null) {
            log.warn("Task with id ${taskId} not found to open.")
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
            log.warn("Task with id ${taskId} not found to close.")
            return
        }
        ctx.openedNodes.remove(taskId)
    }
}
