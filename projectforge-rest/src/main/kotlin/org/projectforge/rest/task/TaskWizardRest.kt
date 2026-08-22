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
     * @param taskTitle Title of the element, for the message the client shows.
     * @param accessEntries Number of access entries written over all groups, ancestors included. Zero
     * means no group was given, the case the wizard announces as
     * `task.wizard.action.noactionRequired`.
     */
    class ExecuteResponse(
        val taskTitle: String?,
        val accessEntries: Int,
    )

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var taskWizardService: TaskWizardService

    @PostMapping("execute")
    fun execute(@RequestBody request: ExecuteRequest): ResponseEntity<ExecuteResponse> {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        val taskId = request.taskId ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val result = taskWizardService.grantAccess(
            taskId = taskId,
            managerGroupId = request.managerGroupId,
            teamGroupId = request.teamGroupId,
            externalGroupId = request.externalGroupId,
        )
        return ResponseEntity(
            ExecuteResponse(
                taskTitle = result.taskTitle,
                accessEntries = result.granted.sumOf { it.accessEntries },
            ),
            HttpStatus.OK,
        )
    }
}
