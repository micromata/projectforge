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

package org.projectforge.rest.fibu

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.ProjectWizardService
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskNode
import org.projectforge.business.task.TaskTree
import org.projectforge.business.task.TaskWizardService
import org.projectforge.business.user.GroupDao
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.task.TaskWizardRest
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

/**
 * The project wizard ([ProjectWizardService]): customer, project, structure element, groups and access
 * rights in one go. Serves the hand built page of projectforge-next (`/next/projectWizard`).
 *
 * Only for users who may create customers or projects, and structure elements ([ProjectWizardService.isAllowed]),
 * checked by every endpoint - the frontend hiding its entry is no access check. What the user may create of
 * it is checked on [execute] ([ProjectWizardService.checkAllowed]).
 *
 * The field ids of the validation errors are the paths of [ProjectWizardService.Data]: `customer.nummer`,
 * `project.name`, `parentTaskId`, `teamGroup.name`, ...
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/projectWizard")
class ProjectWizardRest {
    /**
     * @param allowed Whether the logged-in user may run the wizard, for the entry points.
     * @param customerAllowed Whether a new customer may be created ([ProjectWizardService.customerAllowed]).
     * @param projectAllowed Whether a project may be created ([ProjectWizardService.projectAllowed]).
     */
    class InitResponse(
        val allowed: Boolean,
        val customerAllowed: Boolean,
        val projectAllowed: Boolean,
        val rootTaskId: Long?,
    )

    /**
     * @param preview What [execute] would write as access rights, as the structure wizard reports it -
     * null as long as no parent is given.
     */
    class ValidateResponse(
        val validationErrors: List<ValidationError>,
        val preview: TaskWizardRest.ExecuteResponse?,
    )

    class ResultGroup(
        val groupType: String,
        val id: Long,
        val name: String?,
        val created: Boolean,
    )

    class ExecuteResponse(
        val customerId: Long,
        val customerName: String?,
        val customerCreated: Boolean,
        val customerTaskId: Long?,
        val projectId: Long?,
        val projectName: String?,
        val taskId: Long?,
        val groups: List<ResultGroup>,
        val access: TaskWizardRest.ExecuteResponse?,
    )

    @Autowired
    private lateinit var fibuNumberValidation: FibuNumberValidation

    @Autowired
    private lateinit var groupDao: GroupDao

    @Autowired
    private lateinit var projectWizardService: ProjectWizardService

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var taskWizardService: TaskWizardService

    @AccessChecked("Answers only whether ProjectWizardService.isAllowed, no data")
    @GetMapping("init")
    fun init(): InitResponse {
        return InitResponse(
            allowed = projectWizardService.isAllowed(),
            customerAllowed = projectWizardService.customerAllowed(),
            projectAllowed = projectWizardService.projectAllowed(),
            rootTaskId = taskTree.rootTaskNode.id,
        )
    }

    /**
     * The structure element of an existing customer, for the wizard to preset (see
     * [ProjectWizardService.customerTaskOf]), and the customer's number, name and identifier, which the
     * suggested group names are built of (see `GroupNomenclatureConfig`).
     */
    class CustomerTaskResponse(
        val taskId: Long?,
        val nummer: Long? = null,
        val name: String? = null,
        val identifier: String? = null,
    )

    @AccessChecked("ProjectWizardService.isAllowed")
    @GetMapping("customerTask")
    fun customerTask(@RequestParam("customerId") customerId: Long): CustomerTaskResponse {
        checkAllowed()
        val kunde = projectWizardService.existingCustomer(customerId)
        return CustomerTaskResponse(
            taskId = projectWizardService.customerTaskOf(customerId),
            nummer = kunde?.nummer,
            name = kunde?.name,
            identifier = kunde?.identifier,
        )
    }

    /**
     * The field errors of the data, and the access rights the wizard would write - the wizard's preview,
     * asked while the user is still typing.
     */
    @AccessChecked("ProjectWizardService.isAllowed")
    @PostMapping("validate")
    fun validate(@RequestBody data: ProjectWizardService.Data): ResponseEntity<ValidateResponse> {
        checkAllowed()
        return ResponseEntity.ok(ValidateResponse(validationErrors(data), preview(data)))
    }

    @AccessChecked("ProjectWizardService.checkAllowed (incl. insert access on the parent task)")
    @PostMapping("execute")
    fun execute(@RequestBody data: ProjectWizardService.Data): ResponseEntity<*> {
        checkAllowed()
        validationErrors(data).takeIf { it.isNotEmpty() }?.let {
            return ResponseEntity(ResponseAction(validationErrors = it), HttpStatus.NOT_ACCEPTABLE)
        }
        val result = try {
            projectWizardService.execute(data)
        } catch (ex: AccessException) {
            throw ex
        } catch (ex: UserException) {
            // A check of the DAOs the validation doesn't repeat (or a value changed meanwhile): everything is
            // rolled back, so the user may correct and finish again.
            log.info { "Project wizard: ${translateMsg(ex)}" }
            return ResponseEntity(
                ResponseAction(validationErrors = listOf(ValidationError(translateMsg(ex)))),
                HttpStatus.NOT_ACCEPTABLE,
            )
        }
        return ResponseEntity.ok(
            ExecuteResponse(
                customerId = result.customerId,
                customerName = result.customerName,
                customerCreated = result.customerCreated,
                customerTaskId = result.customerTaskId,
                projectId = result.projectId,
                projectName = result.projectName,
                taskId = result.taskId,
                groups = result.groups.map { ResultGroup(it.groupType.name, it.id, it.name, it.created) },
                access = result.access?.let { TaskWizardRest.toResponse(it) },
            )
        )
    }

    private fun checkAllowed() {
        if (!projectWizardService.isAllowed()) {
            throw AccessException("access.exception.noAccess")
        }
    }

    private fun preview(data: ProjectWizardService.Data): TaskWizardRest.ExecuteResponse? {
        // Without a project there are no groups, so no rights to show.
        data.project ?: return null
        val parentTaskId = parentNodeOf(data)?.id ?: return null
        val wizardGroup = { group: ProjectWizardService.GroupData? ->
            group?.let { TaskWizardService.WizardGroup(it.id, it.name?.trim()) }
        }
        val customer = data.customer
        return TaskWizardRest.toResponse(
            taskWizardService.previewNewElement(
                parentTaskId = parentTaskId,
                newTaskTitle = data.project?.name?.trim(),
                newParentTitle = if (customer?.isNew == true) customer.name?.trim() else null,
                managerGroup = wizardGroup(data.managerGroup),
                teamGroup = wizardGroup(data.teamGroup),
                externalGroup = wizardGroup(data.externalGroup),
                checkAccess = false,
            )
        )
    }

    private fun validationErrors(data: ProjectWizardService.Data): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val customer = data.customer ?: ProjectWizardService.CustomerData()
        val kundeNummer = if (customer.isNew) {
            required(errors, customer.nummer, "customer.nummer", "fibu.kunde.nummer")
            required(errors, customer.name, "customer.name", "fibu.kunde.name")
            fibuNumberValidation.validateNewCustomerNumber(errors, customer.nummer, fieldId = "customer.nummer")
            customer.nummer
        } else {
            required(errors, customer.id, "customer.id", "fibu.kunde")
            customer.id
        }
        // A new customer only, without project (and so without groups): the parent is optional then.
        val project = data.project
        val projectName = project?.name?.trim()
        if (project != null) {
            required(errors, projectName, "project.name", "fibu.projekt.name")
            val nummer = project.nummer
            required(errors, nummer, "project.nummer", "fibu.projekt.nummer")
            // Only once the customer's number is fine, otherwise the project's range is a guess:
            if (nummer != null && errors.none { it.fieldId == "customer.nummer" }) {
                fibuNumberValidation.validateProjectNumbers(
                    errors,
                    kundeNummer = kundeNummer,
                    projectId = null,
                    nummer = nummer,
                    projektnummer = project.projektnummer,
                    internKost2_4 = null,
                    newKost2Requested = !project.kost2ArtIds.isNullOrEmpty(),
                    fieldPrefix = "project.",
                )
            }
        }
        val parentNode = parentNodeOf(data)
        if (parentNode == null) {
            if (project != null || data.parentTaskId != null) {
                required(errors, null, "parentTaskId", "task.parentTask")
            }
        } else {
            // The new element below the picked one is titled by the new customer or else by the project, and
            // sister elements must differ (TaskDao).
            val newTitle = if (customer.isNew) customer.name?.trim() else projectName
            if (!newTitle.isNullOrBlank() && parentNode.children?.any { !it.isDeleted && it.task?.title == newTitle } == true) {
                errors.add(ValidationError(translate("task.error.duplicateChildTasks"), "parentTaskId"))
            }
        }
        if (project == null) {
            return errors
        }
        val newNames = mutableSetOf<String>()
        data.groups.forEach { (groupType, group) ->
            if (group == null || !group.isNew) {
                return@forEach
            }
            val fieldId = "${groupFieldOf(groupType)}.name"
            val name = group.name!!.trim()
            if (!newNames.add(name) || groupDao.doesGroupnameAlreadyExist(GroupDO().also { it.name = name })) {
                errors.add(ValidationError(translate("group.error.groupnameAlreadyExists"), fieldId))
            }
        }
        return errors
    }

    /**
     * The picked parent element, if the user may see it: the preview shows the titles of its ancestors and the
     * rows of the groups on them, and the validation tells about its children. Unknown otherwise, as one that
     * doesn't exist.
     */
    private fun parentNodeOf(data: ProjectWizardService.Data): TaskNode? {
        val parentNode = data.parentTaskId?.let { taskTree.getTaskNodeById(it) } ?: return null
        val task = parentNode.task ?: return null
        return parentNode.takeIf { taskDao.hasLoggedInUserSelectAccess(task, throwException = false) }
    }

    private fun required(errors: MutableList<ValidationError>, value: Any?, fieldId: String, labelKey: String) {
        if (value == null || (value is String && value.isBlank())) {
            errors.add(ValidationError(translateMsg("validation.error.fieldRequired", translate(labelKey)), fieldId))
        }
    }

    private fun groupFieldOf(groupType: TaskWizardService.GroupType): String = when (groupType) {
        TaskWizardService.GroupType.MANAGER -> "managerGroup"
        TaskWizardService.GroupType.TEAM -> "teamGroup"
        TaskWizardService.GroupType.EXTERNAL -> "externalGroup"
    }
}
