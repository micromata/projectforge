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

package org.projectforge.business.fibu

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.KundeCache
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.business.fibu.kost.ProjektKost2Service
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskNode
import org.projectforge.business.task.TaskTree
import org.projectforge.business.task.TaskWizardService
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * The project wizard (shown as customer wizard): sets up a project in one go - its customer (an
 * existing one or a new one), the project, a structure element for it, the groups of its team,
 * managers and externals (existing ones or new ones with their members) and their access rights on
 * the element (the rules of [TaskWizardService]).
 *
 * The structure it follows: a customer has an element of its own, the parent of the elements of all
 * its projects; the customer element may sit below a customer group, a business unit or the like, or
 * on the top level. So for an existing customer the project's element is created below the customer's
 * element, for a new customer the customer's element is created first, below the chosen one.
 *
 * Elements that exist already are used instead of new ones ([elementsOf]): picked by the user (whose title
 * may differ, e.g. "ACME" for "ACME Inc."), or found below the parent by the title the new one would get.
 * An existing element of the project gets the access rights the wizard adds; it must lie below the
 * customer's element, have no project yet, and the user must be allowed to write it ([projectTaskProblem]); one
 * found by the name of a new customer must not be another customer's ([customerTaskProblem]).
 *
 * Parts of it are left out by the rights of the user ([checkAllowed]): who may create customers only gets a
 * new customer (and its element, if a parent is picked), without project, groups and access rights; who
 * may create projects only picks an existing customer. Who may do both may create a customer only as well
 * ([Data.project] null). The groups are optional anyway.
 *
 * Only for users who may create customers or projects and structure elements anyway ([isAllowed]). As an
 * exception, users who may create projects may create groups and access rights through the wizard -
 * which only admins may otherwise -, which is why those two are written without the access checks of
 * their DAOs, after the wizard checked its own rule.
 *
 * All of it in one transaction: a failure anywhere leaves nothing behind.
 *
 * @author Kai Reinhard
 */
@Service
class ProjectWizardService {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var kundeCache: KundeCache

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    @Autowired
    private lateinit var projektCache: ProjektCache

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var projektKost2Service: ProjektKost2Service

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var taskWizardService: TaskWizardService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /**
     * The customer of the project: an existing one by [id], otherwise a new one with the other fields.
     *
     * @param existing Whether an existing customer is meant, so a missing [id] is reported as such
     * rather than taken for a new customer.
     * @param nummer Number of a new customer, which is its id as well (user assigned).
     */
    class CustomerData(
        var existing: Boolean = false,
        var id: Long? = null,
        var nummer: Long? = null,
        var name: String? = null,
        var identifier: String? = null,
        var division: String? = null,
        var status: KundeStatus? = null,
        var description: String? = null,
    ) {
        val isNew: Boolean
            get() = !existing && id == null
    }

    /**
     * The new project. The project number follows from customer and [nummer] within a number range
     * (see `ProjektDao.assignProjektnummer`), [projektnummer] is the free one outside of ranges.
     *
     * @param nummer The local number; null only until the user typed one, the wizard refuses it then.
     *
     * @param kost2ArtIds The cost 2 types the project gets a cost 2 unit of.
     */
    class ProjectData(
        var nummer: Int? = null,
        var projektnummer: Int? = null,
        var name: String? = null,
        var identifier: String? = null,
        var status: ProjektStatus? = null,
        var description: String? = null,
        var contactPersonId: Long? = null,
        var additionalContactIds: List<Long>? = null,
        var kost2ArtIds: List<Long>? = null,
    )

    /**
     * A group of one role: an existing one by [id], otherwise a new one with [name] and [userIds] as
     * members. Without either, the role is left out.
     */
    class GroupData(
        var id: Long? = null,
        var name: String? = null,
        var userIds: List<Long>? = null,
    ) {
        val isNew: Boolean
            get() = id == null && !name.isNullOrBlank()

        val isGiven: Boolean
            get() = id != null || isNew
    }

    /**
     * @param project Null for a new customer only, without project, groups and access rights.
     * @param parentTaskId For an existing customer its element, which the project's element is created
     * below; for a new customer the element its own element (titled by its name) is created below, and
     * the project's below that. Optional for a new customer only: without it no element is created. Not
     * needed for a new customer with [customerTaskId].
     * @param customerTaskId For a new customer only: an existing element to be the customer's, instead of a
     * new one below [parentTaskId].
     * @param customerTaskExisting Whether an existing element is meant for the new customer, so a missing
     * [customerTaskId] is reported as such rather than taken for a new element.
     * @param projectTaskId An existing element to be the project's, below the customer's element, instead of a
     * new one.
     * @param projectTaskExisting As [customerTaskExisting], for [projectTaskId].
     * @param managerGroup Also becomes the project manager group of the project.
     */
    class Data(
        var customer: CustomerData? = null,
        var project: ProjectData? = null,
        var parentTaskId: Long? = null,
        var customerTaskId: Long? = null,
        var customerTaskExisting: Boolean = false,
        var projectTaskId: Long? = null,
        var projectTaskExisting: Boolean = false,
        var managerGroup: GroupData? = null,
        var teamGroup: GroupData? = null,
        var externalGroup: GroupData? = null,
    ) {
        /** Whether an existing element is meant for the new customer, see [customerTaskExisting]. */
        val customerTaskPicked: Boolean
            get() = customerTaskExisting || customerTaskId != null

        /** Whether an existing element is meant for the project, see [projectTaskExisting]. */
        val projectTaskPicked: Boolean
            get() = projectTaskExisting || projectTaskId != null

        val groups: List<Pair<TaskWizardService.GroupType, GroupData?>>
            get() = listOf(
                TaskWizardService.GroupType.MANAGER to managerGroup,
                TaskWizardService.GroupType.TEAM to teamGroup,
                TaskWizardService.GroupType.EXTERNAL to externalGroup,
            )
    }

    /**
     * The structure elements the wizard uses, see [elementsOf].
     *
     * @param customerTask The customer's element if it exists already: the parent for an existing customer,
     * for a new one the picked element or the child of the parent titled with the customer's name.
     * @param customerTaskNew Whether the customer's element is to be created, below [Data.parentTaskId].
     * @param projectTask The project's element if it exists already: the picked one or the child of the
     * customer's element titled with the project's name.
     */
    class Elements(
        val customerTask: TaskNode?,
        val customerTaskNew: Boolean,
        val projectTask: TaskNode?,
    )

    /**
     * Why an element can't be used as the wizard would (see [customerTaskProblem], [projectTaskProblem]).
     *
     * @param title The element's title, as the user may be told it: the typed name for one found by it, the id
     * for a picked one the user may not see.
     * @param detail The name of the project of [Type.HAS_PROJECT] or the customer of [Type.OTHER_CUSTOMER].
     */
    class TaskProblem(val type: Type, val title: String?, val detail: String? = null) {
        enum class Type {
            /** The user may not see the element. */
            NOT_VISIBLE,

            /** The project's element doesn't lie below the customer's. */
            NOT_BELOW_CUSTOMER,

            /** The project's element, or one between it and the customer's, has a project already. */
            HAS_PROJECT,

            /** The user may not write the project's element, see [projectTaskProblem]. */
            NO_WRITE_ACCESS,

            /** The element found by the new customer's name holds projects of another customer. */
            OTHER_CUSTOMER,

            /**
             * A new element can't be created: a deleted sister element has its title, which TaskDao refuses as
             * well (deleted elements can be restored).
             */
            DELETED_NAMESAKE,
        }
    }

    /** A group of the result, [created] by the wizard or picked. */
    class ResultGroup(
        val groupType: TaskWizardService.GroupType,
        val id: Long,
        val name: String?,
        val created: Boolean,
    )

    /**
     * @param customerTaskId The customer's element, the parent of the project's; null for a new customer
     * only without a parent.
     * @param customerTaskCreated Whether the customer's element was created, not an existing one used.
     * @param customerTaskTitle The title of the customer's element, which may differ from its name if it existed.
     * @param projectId Null, as [projectName], [taskId] and [access], for a new customer only.
     * @param taskCreated Whether the project's element was created, not an existing one used.
     * @param taskTitle The title of the project's element, see [customerTaskTitle].
     */
    class Result(
        val customerId: Long,
        val customerName: String?,
        val customerCreated: Boolean,
        val customerTaskId: Long?,
        val customerTaskCreated: Boolean,
        val customerTaskTitle: String?,
        val projectId: Long?,
        val projectName: String?,
        val taskId: Long?,
        val taskCreated: Boolean,
        val taskTitle: String?,
        val groups: List<ResultGroup>,
        val access: TaskWizardService.Result?,
    )

    /**
     * Whether the user may run the wizard: create customers ([customerAllowed]) or projects
     * ([projectAllowed]), and structure elements - the last one depends on the parent and is checked by
     * [checkAllowed] as well.
     */
    fun isAllowed(): Boolean = customerAllowed() || projectAllowed()

    /**
     * Whether the user may create customers (`FIBU_CUSTOMER`, read and write). Restricted and demo users
     * never, they may not write anything.
     */
    fun customerAllowed(): Boolean {
        if (accessChecker.isRestrictedOrDemoUser) {
            return false
        }
        return kundeDao.hasInsertAccess(ThreadLocalUserContext.requiredLoggedInUser, KundeDO(), false)
    }

    /** Whether the user may create projects (`PM_PROJECT`, read and write), see [customerAllowed]. */
    fun projectAllowed(): Boolean {
        if (accessChecker.isRestrictedOrDemoUser) {
            return false
        }
        return projektDao.hasInsertAccess(ThreadLocalUserContext.requiredLoggedInUser, ProjektDO(), false)
    }

    /**
     * @throws AccessException unless the user may do what [data] asks for: a new customer needs
     * [customerAllowed], a project [projectAllowed], a new element the right to create it below its parent, an
     * existing one the right to see it, and an existing element of the project the right to write it.
     * @throws IllegalArgumentException for neither a new customer nor a project, groups without a project, or
     * an existing element that can't be used ([customerTaskProblem], [projectTaskProblem]).
     */
    fun checkAllowed(data: Data) {
        checkAllowed(data, elementsOf(data))
    }

    private fun checkAllowed(data: Data, elements: Elements) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val customer = data.customer ?: CustomerData()
        if (data.project == null) {
            require(customer.isNew) { "Neither a new customer nor a project." }
            require(data.groups.none { it.second?.isGiven == true }) { "Groups without a project." }
        }
        if ((customer.isNew && !customerAllowed()) || (data.project != null && !projectAllowed())) {
            throw AccessException(user, "access.exception.noAccess")
        }
        if (elements.customerTaskNew) {
            checkInsertAccess(data.parentTaskId)
        }
        elements.customerTask?.task?.let { taskDao.hasUserSelectAccess(user, it, throwException = true) }
        listOfNotNull(customerTaskProblem(data, elements), projectTaskProblem(data, elements)).forEach { problem ->
            when (problem.type) {
                TaskProblem.Type.NOT_VISIBLE, TaskProblem.Type.NO_WRITE_ACCESS ->
                    throw AccessException(user, "access.exception.noAccess")
                // Refused by TaskDao on insert, with a message for the user:
                TaskProblem.Type.DELETED_NAMESAKE -> {}
                else -> throw IllegalArgumentException("Task '${problem.title}' can't be used: ${problem.type}.")
            }
        }
        if (data.project != null && elements.projectTask == null) {
            elements.customerTask?.let { checkInsertAccess(it.id) }
        }
    }

    private fun checkInsertAccess(parentTaskId: Long?) {
        parentTaskId ?: return
        val task = TaskDO().also { it.parentTask = TaskDO().also { parent -> parent.id = parentTaskId } }
        taskDao.hasInsertAccess(ThreadLocalUserContext.requiredLoggedInUser, task, throwException = true)
    }

    /**
     * The elements the wizard uses for [data], see [Elements]: picked ones, otherwise the child of the parent
     * with the title a new one would get - sister elements must differ anyway (TaskDao) -, otherwise new ones.
     * Unknown or deleted picks are left out (null).
     */
    fun elementsOf(data: Data): Elements {
        val customer = data.customer ?: CustomerData()
        val parentNode = nodeOf(data.parentTaskId)
        val customerTask = when {
            !customer.isNew -> parentNode
            data.customerTaskPicked -> nodeOf(data.customerTaskId)
            else -> parentNode?.let { childOf(it, customer.name) }
        }
        val customerTaskNew = customer.isNew && !data.customerTaskPicked && customerTask == null && parentNode != null
        val project = data.project
        val projectTask = when {
            project == null -> null
            data.projectTaskPicked -> nodeOf(data.projectTaskId)
            else -> customerTask?.let { childOf(it, project.name) }
        }
        return Elements(customerTask, customerTaskNew, projectTask)
    }

    /**
     * Why the element of a new customer can't be used or created, null if it can: an existing one must be
     * visible to the user, and one found by the customer's name must not hold projects of another customer -
     * it would be that customer's element. A picked one may (the user chose it on purpose).
     */
    fun customerTaskProblem(data: Data, elements: Elements): TaskProblem? {
        val customer = data.customer ?: CustomerData()
        if (!customer.isNew) {
            return null
        }
        val name = customer.name?.trim()
        if (elements.customerTaskNew) {
            return deletedNamesake(nodeOf(data.parentTaskId), name)
        }
        val node = elements.customerTask ?: return null
        val title = if (data.customerTaskPicked) "#${node.id}" else name
        if (!isVisible(node)) {
            return TaskProblem(TaskProblem.Type.NOT_VISIBLE, title)
        }
        if (data.customerTaskPicked) {
            return null
        }
        return otherCustomerOf(node)?.let { TaskProblem(TaskProblem.Type.OTHER_CUSTOMER, node.task?.title, it) }
    }

    /**
     * A warning for the picked element of a new customer, which the user may use nevertheless: it belongs to a
     * project ([TaskProblem.Type.HAS_PROJECT], itself or an ancestor) or holds projects of another customer
     * ([TaskProblem.Type.OTHER_CUSTOMER]). The access rights on it and its ancestors apply to the new project as
     * well, so the members of the other project or customer may see it. Null without a warning, or if the element
     * can't be used anyway ([customerTaskProblem]).
     */
    fun customerTaskWarning(data: Data, elements: Elements): TaskProblem? {
        if (data.customer?.isNew != true || !data.customerTaskPicked) {
            return null
        }
        val node = elements.customerTask ?: return null
        if (customerTaskProblem(data, elements) != null) {
            return null
        }
        val title = node.task?.title
        node.getProjekt(true)?.let { return TaskProblem(TaskProblem.Type.HAS_PROJECT, title, it.name) }
        return otherCustomerOf(node)?.let { TaskProblem(TaskProblem.Type.OTHER_CUSTOMER, title, it) }
    }

    /** The name of the customer (or project, without customer) of a project in or below [node], null if none. */
    private fun otherCustomerOf(node: TaskNode): String? {
        val other = projektCache.all.values.firstOrNull { projekt ->
            !projekt.deleted && generateSequence(taskTree.getTaskNodeById(projekt.task?.id)) { it.parent }
                .any { it == node }
        } ?: return null
        return other.kunde?.id?.let { kundeCache.getKunde(it)?.name } ?: other.name
    }

    /**
     * Why the project's element can't be used or created, null if it can. An existing one must be visible, lie
     * below the customer's element, have no project yet - neither itself nor one between them -, and the user
     * must be allowed to write it: the wizard writes its access rights without the checks of the access
     * management.
     */
    fun projectTaskProblem(data: Data, elements: Elements): TaskProblem? {
        val project = data.project ?: return null
        val node = elements.projectTask
        if (node == null) {
            val customerTask = elements.customerTask
            if (data.projectTaskPicked || elements.customerTaskNew || customerTask == null) {
                return null
            }
            return deletedNamesake(customerTask, project.name?.trim())
        }
        val title = if (data.projectTaskPicked && !isVisible(node)) "#${node.id}" else node.task?.title
        val task = node.task
        if (task == null || !isVisible(node)) {
            return TaskProblem(TaskProblem.Type.NOT_VISIBLE, title)
        }
        val customerTask = elements.customerTask
        if (customerTask?.isParentOf(node) != true) {
            return TaskProblem(TaskProblem.Type.NOT_BELOW_CUSTOMER, title)
        }
        generateSequence(node) { it.parent }.takeWhile { it != customerTask }
            .firstNotNullOfOrNull { it.getProjekt(false) }
            ?.let { return TaskProblem(TaskProblem.Type.HAS_PROJECT, title, it.name) }
        if (!taskDao.hasUpdateAccess(ThreadLocalUserContext.requiredLoggedInUser, task, task, false)) {
            return TaskProblem(TaskProblem.Type.NO_WRITE_ACCESS, title)
        }
        return null
    }

    private fun isVisible(node: TaskNode): Boolean {
        val task = node.task ?: return false
        return taskDao.hasUserSelectAccess(ThreadLocalUserContext.requiredLoggedInUser, task, false)
    }

    private fun deletedNamesake(parent: TaskNode?, title: String?): TaskProblem? {
        if (parent == null || title.isNullOrEmpty()) {
            return null
        }
        return parent.children?.find { it.isDeleted && it.task?.title == title }
            ?.let { TaskProblem(TaskProblem.Type.DELETED_NAMESAKE, title) }
    }

    private fun nodeOf(taskId: Long?): TaskNode? =
        taskId?.let { taskTree.getTaskNodeById(it) }?.takeIf { !it.isDeleted }

    private fun childOf(parent: TaskNode, title: String?): TaskNode? {
        val trimmed = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return parent.children?.find { !it.isDeleted && it.task?.title == trimmed }
    }

    /**
     * Sets the project up, see the class description.
     *
     * The values are expected to have been validated by the caller (the wizard's REST endpoint), the
     * DAOs check the essential ones again (customer number, project number) and fail with a
     * [org.projectforge.common.i18n.UserException] - which rolls everything back.
     *
     * @throws AccessException see [checkAllowed].
     * @throws IllegalArgumentException if a required value is missing.
     */
    fun execute(data: Data): Result {
        val customerData = requireNotNull(data.customer) { "Customer required." }
        val projectData = data.project
        val elements = elementsOf(data)
        if (customerData.isNew && data.customerTaskPicked) {
            requireNotNull(elements.customerTask) { "Task #${data.customerTaskId} not found." }
        }
        if (projectData != null) {
            require(elements.customerTask != null || elements.customerTaskNew) { "Parent task required." }
            require(!projectData.name.isNullOrBlank()) { "Project name required." }
            if (data.projectTaskPicked) {
                requireNotNull(elements.projectTask) { "Task #${data.projectTaskId} not found." }
            }
        }
        checkAllowed(data, elements)
        var success = false
        try {
            val result = persistenceService.runInTransaction { _ ->
                val (kunde, customerCreated) = customerOf(customerData)
                val customerTaskId = if (elements.customerTaskNew) {
                    val customerTask = TaskDO().also { it.title = kunde.name?.trim() }
                    taskDao.setParentTask(customerTask, data.parentTaskId!!)
                    taskDao.insert(customerTask)
                } else {
                    elements.customerTask?.id
                }
                val customerTaskTitle = customerTaskId?.let { elements.customerTask?.task?.title ?: kunde.name?.trim() }
                if (projectData == null) {
                    return@runInTransaction Result(
                        customerId = kunde.id!!,
                        customerName = kunde.name,
                        customerCreated = true,
                        customerTaskId = customerTaskId,
                        customerTaskCreated = elements.customerTaskNew,
                        customerTaskTitle = customerTaskTitle,
                        projectId = null,
                        projectName = null,
                        taskId = null,
                        taskCreated = false,
                        taskTitle = null,
                        groups = emptyList(),
                        access = null,
                    )
                }
                val taskId = elements.projectTask?.id ?: TaskDO().let { task ->
                    task.title = projectData.name?.trim()
                    taskDao.setParentTask(task, customerTaskId!!)
                    taskDao.insert(task)
                }
                val groups = data.groups.mapNotNull { (groupType, groupData) ->
                    groupData?.let { groupOf(groupType, it) }
                }
                val groupId = { type: TaskWizardService.GroupType -> groups.find { it.groupType == type }?.id }
                val projekt = ProjektDO().also {
                    it.nummer = requireNotNull(projectData.nummer) { "Project number required." }
                    it.projektnummer = projectData.projektnummer
                    it.name = projectData.name
                    it.identifier = projectData.identifier
                    it.status = projectData.status
                    it.description = projectData.description
                    it.contactPerson = projectData.contactPersonId?.let { id -> PFUserDO().also { u -> u.id = id } }
                    it.additionalContactUserIds =
                        ContactUserIds.normalize(projectData.contactPersonId, projectData.additionalContactIds)
                }
                projektDao.setKunde(projekt, kunde.id)
                projektDao.setTask(projekt, taskId)
                projektDao.setProjektManagerGroup(projekt, groupId(TaskWizardService.GroupType.MANAGER))
                projektDao.insert(projekt)
                val projektId = projekt.id!!
                projectData.kost2ArtIds?.takeIf { it.isNotEmpty() }?.let {
                    projektKost2Service.activate(projektId, it)
                }
                val access = taskWizardService.grantAccess(
                    taskId = taskId,
                    managerGroupId = groupId(TaskWizardService.GroupType.MANAGER),
                    teamGroupId = groupId(TaskWizardService.GroupType.TEAM),
                    externalGroupId = groupId(TaskWizardService.GroupType.EXTERNAL),
                    checkAccess = false,
                )
                Result(
                    customerId = kunde.id!!,
                    customerName = kunde.name,
                    customerCreated = customerCreated,
                    customerTaskId = customerTaskId,
                    customerTaskCreated = elements.customerTaskNew,
                    customerTaskTitle = customerTaskTitle,
                    projectId = projektId,
                    projectName = projekt.name,
                    taskId = taskId,
                    taskCreated = elements.projectTask == null,
                    taskTitle = elements.projectTask?.task?.title ?: projekt.name?.trim(),
                    groups = groups,
                    access = access,
                )
            }
            success = true
            log.info {
                if (result.projectId == null) {
                    "Project wizard: customer #${result.customerId} created, task #${result.customerTaskId} " +
                            "(new: ${result.customerTaskCreated})."
                } else {
                    "Project wizard: project #${result.projectId} '${result.projectName}' set up for customer " +
                            "#${result.customerId} (new: ${result.customerCreated}), task #${result.taskId} " +
                            "(new: ${result.taskCreated}, customer's #${result.customerTaskId}, " +
                            "new: ${result.customerTaskCreated}), " +
                            "${result.groups.count { it.created }} new group(s), " +
                            "${result.access?.entries?.size ?: 0} access entries."
                }
            }
            return result
        } finally {
            if (!success) {
                // The task tree took the new element in right after its insert, so it must forget it again.
                taskTree.forceReload()
            }
            // The caches might have been refreshed between a write and the commit, without the new rows.
            kundeCache.setExpired()
            projektCache.setExpired()
            kostCache.setExpired()
            userGroupCache.setExpired()
        }
    }

    /**
     * The element of an existing customer, as its projects tell: the parent most of their elements
     * share. Null if none of them has an element (or only ones on the top level).
     */
    fun customerTaskOf(kundeId: Long): Long? {
        return projektCache.all.values
            .filter { it.kunde?.id == kundeId && !it.deleted }
            .mapNotNull { projekt -> taskTree.getTaskNodeById(projekt.task?.id)?.parent }
            .filter { !taskTree.isRootNode(it) && !it.isDeleted }
            .groupingBy { it.id }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    /** The existing customer of the given id (number), for the group names the wizard suggests. */
    fun existingCustomer(kundeId: Long): KundeDO? = kundeCache.getKunde(kundeId)

    private fun customerOf(data: CustomerData): Pair<KundeDO, Boolean> {
        require(!data.existing || data.id != null) { "Existing customer required." }
        data.id?.let { id ->
            val kunde = kundeDao.find(id) ?: throw IllegalArgumentException("Customer #$id not found.")
            return kunde to false
        }
        val kunde = KundeDO().also {
            it.nummer = requireNotNull(data.nummer) { "Number of new customer required." }
            it.name = data.name
            it.identifier = data.identifier
            it.division = data.division
            it.status = data.status
            it.description = data.description
        }
        kundeDao.insert(kunde)
        return kunde to true
    }

    /**
     * The group of one role, a new one created without access check (see the class description).
     */
    private fun groupOf(groupType: TaskWizardService.GroupType, data: GroupData): ResultGroup? {
        data.id?.let { id ->
            val group = groupDao.find(id, checkAccess = false) ?: throw IllegalArgumentException("Group #$id not found.")
            return ResultGroup(groupType, id, group.name, created = false)
        }
        if (!data.isNew) {
            return null
        }
        val group = GroupDO().also { it.name = data.name?.trim() }
        val users = data.userIds.orEmpty().distinct().map { id -> PFUserDO().also { it.id = id } }
        groupDao.setAssignedUsers(group, users)
        groupDao.insert(group, checkAccess = false)
        return ResultGroup(groupType, group.id!!, group.name, created = true)
    }
}
