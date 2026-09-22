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

import org.projectforge.business.task.TaskWizardService
import org.projectforge.framework.access.AccessChecker
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * The structure wizard: grants a set of groups their access rights on one structure element.
 *
 * Serves the hand built page of projectforge-next (`/next/taskWizard`), which is why there is no
 * `UILayout` here - the predecessor of this class had one and nothing else, so the Finish button of a
 * form nobody rendered posted to an endpoint that didn't exist.
 *
 * Admins only, as the entry into the wizard has always been (`TaskTreePage.init`). Checked here as
 * well, because the frontend hiding an entry is not an access check.
 *
 * @author Kai Reinhard
 */
@RestController
@RequestMapping("${Rest.URL}/taskWizard")
class TaskWizardRest {
    /**
     * @param taskId The structure element the rights are granted on, required.
     * @param managerGroupId Group of the managing users, optional - as are the other two.
     */
    class ExecuteRequest(
        var taskId: Long? = null,
        var managerGroupId: Long? = null,
        var teamGroupId: Long? = null,
        var externalGroupId: Long? = null,
    )

    /**
     * The four permissions of one access type of a row, so the client can show which rights are set the
     * way the access management shows them.
     *
     * @param accessType Name of an `AccessType`: TASK_ACCESS_MANAGEMENT, TASKS, TIMESHEETS or
     * OWN_TIMESHEETS, which the client maps onto `access.type.*`.
     */
    class AccessRight(
        val accessType: String,
        val select: Boolean,
        val insert: Boolean,
        val update: Boolean,
        val delete: Boolean,
    )

    /**
     * One access entry the wizard looked at, so the client can report it: the texts are raw (the client
     * has no way to know a group's name), the enums are names the client maps onto its own labels
     * (`task.wizard.result.*`).
     *
     * @param groupType One of [TaskWizardService.GroupType]: MANAGER, TEAM or EXTERNAL.
     * @param pickedElement True for the element the user picked, false for one of its ancestors, which
     * only got read access.
     * @param status One of [TaskWizardService.AccessStatus]: CREATED, UPDATED or UNCHANGED.
     * @param recursive Whether the rights hold for the sub elements as well - only on the picked element.
     * @param rights The permissions of the row, in the order the access management lists them.
     */
    class AccessEntry(
        val groupName: String?,
        val groupType: String,
        val taskId: Long,
        val taskTitle: String?,
        val pickedElement: Boolean,
        val status: String,
        val recursive: Boolean,
        val rights: List<AccessRight>,
    )

    /**
     * @param taskTitle Title of the element, for the message the client shows.
     * @param accessEntries Number of access entries the wizard touched over all groups, ancestors and
     * the ones that were already right included. Zero means no group was given, the case the wizard
     * announces as `task.wizard.action.noactionRequired`.
     * @param entries The single entries behind those numbers, the picked element's first per group and
     * then its ancestors upwards. That order is the hierarchy: the rows of a group are the one path from
     * the picked element to the root, so the client can indent them without being told a depth.
     */
    class ExecuteResponse(
        val taskTitle: String?,
        val accessEntries: Int,
        val created: Int,
        val updated: Int,
        val unchanged: Int,
        val entries: List<AccessEntry>,
    )

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var taskWizardService: TaskWizardService

    @PostMapping("execute")
    fun execute(@RequestBody request: ExecuteRequest): ResponseEntity<ExecuteResponse> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        val taskId = request.taskId ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        return response(
            taskWizardService.grantAccess(
                taskId = taskId,
                managerGroupId = request.managerGroupId,
                teamGroupId = request.teamGroupId,
                externalGroupId = request.externalGroupId,
            )
        )
    }

    /**
     * What [execute] with the same body would do, without doing any of it - the wizard's preview table,
     * which is shown while the element and the groups are still being picked.
     *
     * A POST although nothing is written: the body is the same as [execute]'s, and asking the same
     * question in two forms would be two contracts to keep in step.
     */
    @PostMapping("preview")
    fun preview(@RequestBody request: ExecuteRequest): ResponseEntity<ExecuteResponse> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        val taskId = request.taskId ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        return response(
            taskWizardService.previewAccess(
                taskId = taskId,
                managerGroupId = request.managerGroupId,
                teamGroupId = request.teamGroupId,
                externalGroupId = request.externalGroupId,
            )
        )
    }

    private fun response(result: TaskWizardService.Result): ResponseEntity<ExecuteResponse> {
        return ResponseEntity(
            ExecuteResponse(
                taskTitle = result.taskTitle,
                accessEntries = result.granted.sumOf { it.accessEntries },
                created = result.count(TaskWizardService.AccessStatus.CREATED),
                updated = result.count(TaskWizardService.AccessStatus.UPDATED),
                unchanged = result.count(TaskWizardService.AccessStatus.UNCHANGED),
                entries = result.entries.map { entry ->
                    AccessEntry(
                        groupName = entry.groupName,
                        groupType = entry.groupType.name,
                        taskId = entry.taskId,
                        taskTitle = entry.taskTitle,
                        pickedElement = entry.pickedElement,
                        status = entry.status.name,
                        recursive = entry.recursive,
                        rights = entry.rights.map { right ->
                            AccessRight(
                                accessType = right.accessType.name,
                                select = right.select,
                                insert = right.insert,
                                update = right.update,
                                delete = right.delete,
                            )
                        },
                    )
                },
            ),
            HttpStatus.OK,
        )
    }
}
