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
 * Only for users who may create customers, projects and structure elements anyway ([isAllowed]). As an
 * exception, these users may create groups and access rights through the wizard - which only admins
 * may otherwise -, which is why those two are written without the access checks of their DAOs, after
 * the wizard checked its own rule.
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
        var projectManagerId: Long? = null,
        var headOfBusinessManagerId: Long? = null,
        var salesManagerId: Long? = null,
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
     * @param parentTaskId For an existing customer its element, which the project's element is created
     * below; for a new customer the element its own element (titled by its name) is created below, and
     * the project's below that.
     * @param managerGroup Also becomes the project manager group of the project.
     */
    class Data(
        var customer: CustomerData? = null,
        var project: ProjectData? = null,
        var parentTaskId: Long? = null,
        var managerGroup: GroupData? = null,
        var teamGroup: GroupData? = null,
        var externalGroup: GroupData? = null,
    ) {
        val groups: List<Pair<TaskWizardService.GroupType, GroupData?>>
            get() = listOf(
                TaskWizardService.GroupType.MANAGER to managerGroup,
                TaskWizardService.GroupType.TEAM to teamGroup,
                TaskWizardService.GroupType.EXTERNAL to externalGroup,
            )
    }

    /** A group of the result, [created] by the wizard or picked. */
    class ResultGroup(
        val groupType: TaskWizardService.GroupType,
        val id: Long,
        val name: String?,
        val created: Boolean,
    )

    /**
     * @param customerTaskId The customer's element, the parent of the project's.
     */
    class Result(
        val customerId: Long,
        val customerName: String?,
        val customerCreated: Boolean,
        val customerTaskId: Long,
        val projectId: Long,
        val projectName: String?,
        val taskId: Long,
        val groups: List<ResultGroup>,
        val access: TaskWizardService.Result,
    )

    /**
     * Whether the user may run the wizard: create customers (finance), projects (`PM_PROJECT`, read and
     * write) and structure elements - the last one depends on the parent and is checked by
     * [checkAllowed] as well. Restricted and demo users never, they may not write anything.
     */
    fun isAllowed(): Boolean {
        if (accessChecker.isRestrictedOrDemoUser) {
            return false
        }
        val user = ThreadLocalUserContext.requiredLoggedInUser
        return kundeDao.hasInsertAccess(user, KundeDO(), false) && projektDao.hasInsertAccess(user, ProjektDO(), false)
    }

    /**
     * @throws AccessException unless [isAllowed] and the user may create an element below [parentTaskId].
     */
    fun checkAllowed(parentTaskId: Long) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        if (!isAllowed()) {
            throw AccessException(user, "access.exception.noAccess")
        }
        val task = TaskDO().also { it.parentTask = TaskDO().also { parent -> parent.id = parentTaskId } }
        taskDao.hasInsertAccess(user, task, throwException = true)
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
        val parentTaskId = requireNotNull(data.parentTaskId) { "Parent task required." }
        val customerData = requireNotNull(data.customer) { "Customer required." }
        val projectData = requireNotNull(data.project) { "Project required." }
        require(!projectData.name.isNullOrBlank()) { "Project name required." }
        checkAllowed(parentTaskId)
        var success = false
        try {
            val result = persistenceService.runInTransaction { _ ->
                val (kunde, customerCreated) = customerOf(customerData)
                val customerTaskId = if (customerCreated) {
                    val customerTask = TaskDO().also { it.title = kunde.name?.trim() }
                    taskDao.setParentTask(customerTask, parentTaskId)
                    taskDao.insert(customerTask)
                } else {
                    parentTaskId
                }
                val task = TaskDO().also { it.title = projectData.name?.trim() }
                taskDao.setParentTask(task, customerTaskId)
                taskDao.insert(task)
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
                    it.projectManager = projectData.projectManagerId?.let { id -> PFUserDO().also { u -> u.id = id } }
                    it.headOfBusinessManager =
                        projectData.headOfBusinessManagerId?.let { id -> PFUserDO().also { u -> u.id = id } }
                    it.salesManager = projectData.salesManagerId?.let { id -> PFUserDO().also { u -> u.id = id } }
                }
                projektDao.setKunde(projekt, kunde.id)
                projektDao.setTask(projekt, task.id)
                projektDao.setProjektManagerGroup(projekt, groupId(TaskWizardService.GroupType.MANAGER))
                projektDao.insert(projekt)
                val projektId = projekt.id!!
                projectData.kost2ArtIds?.takeIf { it.isNotEmpty() }?.let {
                    projektKost2Service.activate(projektId, it)
                }
                val access = taskWizardService.grantAccess(
                    taskId = task.id!!,
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
                    projectId = projektId,
                    projectName = projekt.name,
                    taskId = task.id!!,
                    groups = groups,
                    access = access,
                )
            }
            success = true
            log.info {
                "Project wizard: project #${result.projectId} '${result.projectName}' set up for customer " +
                        "#${result.customerId} (new: ${result.customerCreated}), task #${result.taskId}, " +
                        "${result.groups.count { it.created }} new group(s), ${result.access.entries.size} access entries."
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
