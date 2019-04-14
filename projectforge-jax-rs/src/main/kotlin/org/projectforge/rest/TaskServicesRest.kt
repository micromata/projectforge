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
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDate
import org.projectforge.rest.core.RestHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
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

    class JSNode(val id: Int,
                 val leaf: Boolean = true,
                 val title: String? = null,
                 val shortDescription: String? = null,
                 val protectTimesheetsUntil: PFDate? = null,
                 val reference: String?,
                 val priority: Priority? = null,
                 val status: TaskStatus? = null,
                 val responsibleUser: PFUserDO? = null,
                 val cost2List: MutableList<Cost2>? = null,
                 var childs: MutableList<JSNode>? = null)

    private val log = org.slf4j.LoggerFactory.getLogger(TaskServicesRest::class.java)

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var userPreferencesService: UserPreferencesService

    private val taskTree = TaskTreeHelper.getTaskTree()

    private val restHelper = RestHelper()

    @GET
    @Path("tree")
    @Produces(MediaType.APPLICATION_JSON)
    fun getTree(): Response {
        val openNodes = userPreferencesService.getEntry(TaskTree.USER_PREFS_KEY_OPEN_TASKS) as Set<Int>
        //UserPreferencesHelper.putEntry(TaskTree.USER_PREFS_KEY_OPEN_TASKS, expansion.getIds(), true)
        val taskFilter = TaskFilter()
        val user = ThreadLocalUserContext.getUser()
        val rootJsNode = buildTree(taskFilter, user, taskTree.rootTaskNode, openNodes)
        return restHelper.buildResponse(rootJsNode)
    }

    private fun buildTree(taskFilter: TaskFilter, user: PFUserDO, taskNode: TaskNode, openedNodes : Set<Int>): JSNode? {
        if (!taskFilter.match(taskNode, taskDao, user)) {
            return null
        }
        val task = taskNode.task
        val jsNode = JSNode(id = task.id,
                leaf = !taskNode.hasChilds(),
                title = task.title,
                shortDescription = task.shortDescription,
                protectTimesheetsUntil = PFDate.from(task.protectTimesheetsUntil),
                reference = task.reference,
                priority = task.priority,
                status = task.status,
                responsibleUser = task.responsibleUser)
        if (taskNode.hasChilds() && openedNodes.contains(taskNode.taskId)) {
            // TaskNode has childs and is opened:
            jsNode.childs = mutableListOf()
            taskNode.childs.forEach {
                val childJsNode = buildTree(taskFilter, user, it, openedNodes)
                if (childJsNode != null)
                    jsNode.childs!!.add(childJsNode)
            }
        }
        return jsNode
    }
}
