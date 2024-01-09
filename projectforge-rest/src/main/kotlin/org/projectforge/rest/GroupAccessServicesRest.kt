/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.saveOrUpdate
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/access/template")
class GroupAccessServicesRest {

    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var groupAccessRest: GroupAccessPagesRest

    /**
     * This template clears all access entries.
     */
    @PostMapping("clear")
    fun clear(request: HttpServletRequest, @RequestBody postData: PostData<GroupTaskAccessDO>): ResponseEntity<ResponseAction> {
        val access = postData.data
        access.clear()
        return saveOrUpdate(request, this.accessDao, access, postData, groupAccessRest, groupAccessRest.validate(access))
    }

    /**
     * This template is used as default for guests (they have only read access to tasks).
     */
    @PostMapping("guest")
    fun guest(request: HttpServletRequest, @RequestBody postData: PostData<GroupTaskAccessDO>): ResponseEntity<ResponseAction> {
        val access = postData.data
        access.guest()
        return saveOrUpdate(request, this.accessDao, access, postData, groupAccessRest, groupAccessRest.validate(access))
    }

    /**
     * This template is used as default for employees. The have read access to the access management, full access to tasks
     * and own time sheets and only read-access to foreign time sheets.
     */
    @PostMapping("employee")
    fun employee(request: HttpServletRequest, @RequestBody postData: PostData<GroupTaskAccessDO>): ResponseEntity<ResponseAction> {
        val access = postData.data
        access.employee()
        return saveOrUpdate(request, this.accessDao, access, postData, groupAccessRest, groupAccessRest.validate(access))
    }

    /**
     * This template is used as default for project managers. Same as employee but with full read-write-access to foreign
     * time-sheets.
     */
    @PostMapping("leader")
    fun leader(request: HttpServletRequest, @RequestBody postData: PostData<GroupTaskAccessDO>): ResponseEntity<ResponseAction> {
        val access = postData.data
        access.leader()
        return saveOrUpdate(request, this.accessDao, access, postData, groupAccessRest, groupAccessRest.validate(access))
    }

    /**
     * This template is used as default for project managers. Same as employee but with full read-write-access to foreign
     * time-sheets.
     */
    @PostMapping("administrator")
    fun administrator(request: HttpServletRequest, @RequestBody postData: PostData<GroupTaskAccessDO>): ResponseEntity<ResponseAction> {
        val access = postData.data
        access.administrator()
        return saveOrUpdate(request, this.accessDao, access, postData, groupAccessRest, groupAccessRest.validate(access))
    }
}
