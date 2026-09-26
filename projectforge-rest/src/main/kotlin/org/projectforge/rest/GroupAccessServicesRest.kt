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

import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.GroupTaskAccess
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The quick-fill templates of the access-rights edit page (`edit/template-buttons.tsx` in
 * projectforge-next): each button fills the permission matrix with a preset (clear/guest/employee/
 * leader/administrator).
 *
 * Non-persisting by design — the buttons *populate the matrix for review*, then the user saves through
 * the normal save endpoint. So each endpoint applies the template to the posted (unsaved) entity and
 * returns the recomputed entity as the `data` variable of an `UPDATE` [ResponseAction]; the frontend
 * merges its `accessEntries` back into the form without navigating (see `lib/rs/entity.ts`
 * `convertEntity`).
 */
@RestController
@RequestMapping("${Rest.URL}/access/template")
class GroupAccessServicesRest {

    @Autowired
    private lateinit var groupAccessRest: GroupAccessEntityRest

    @PostMapping("clear")
    fun clear(@RequestBody postData: PostData<GroupTaskAccess>): ResponseAction =
        apply(postData) { it.clear() }

    @PostMapping("guest")
    fun guest(@RequestBody postData: PostData<GroupTaskAccess>): ResponseAction =
        apply(postData) { it.guest() }

    @PostMapping("employee")
    fun employee(@RequestBody postData: PostData<GroupTaskAccess>): ResponseAction =
        apply(postData) { it.employee() }

    @PostMapping("leader")
    fun leader(@RequestBody postData: PostData<GroupTaskAccess>): ResponseAction =
        apply(postData) { it.leader() }

    @PostMapping("administrator")
    fun administrator(@RequestBody postData: PostData<GroupTaskAccess>): ResponseAction =
        apply(postData) { it.administrator() }

    /**
     * Applies the given template to the posted entity and returns it recomputed, without touching the
     * database — the group, task, recursive flag and description the user already entered are preserved,
     * only the four access entries are overwritten by the template.
     */
    private fun apply(
        postData: PostData<GroupTaskAccess>,
        template: (GroupTaskAccessDO) -> Unit,
    ): ResponseAction {
        val obj = groupAccessRest.transformForDB(postData.data)
        template(obj)
        val dto = groupAccessRest.transformFromDB(obj, true)
        return ResponseAction(targetType = TargetType.UPDATE).addVariable("data", dto)
    }
}
