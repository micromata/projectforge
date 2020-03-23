/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import org.projectforge.business.group.service.GroupService
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserAuthenticationsDO
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/myAccount")
class MyAccountPageRest {
    @Autowired
    private lateinit var authenticationsService: UserAuthenticationsService

    @Autowired
    private lateinit var groupService: GroupService

    class MyAccountData(
            var username: String? = null,
            var firstname: String? = null,
            var lastname: String? = null,
            var calendarExportToken: String? = null,
            var davToken: String? = null,
            var restClientToken: String? = null,
            var groups: String? = null
    )

    @GetMapping("dynamic")
    fun getForm(): FormLayoutData {
        val user = ThreadLocalUserContext.getUser()
        val userId = ThreadLocalUserContext.getUserId()
        val data = MyAccountData(user.username, user.firstname, user.lastname)

        val layout = UILayout("user.myAccount.title.edit")
        val userLC = LayoutContext(PFUserDO::class.java)
        val dataLC = LayoutContext(MyAccountData::class.java)
        val authenticationsLC = LayoutContext(UserAuthenticationsDO::class.java)
        data.calendarExportToken = authenticationsService.getToken(userId, UserTokenType.CALENDAR_REST)
        data.davToken = authenticationsService.getToken(userId, UserTokenType.DAV_TOKEN)
        data.restClientToken = authenticationsService.getToken(userId, UserTokenType.REST_CLIENT)
        data.groups = groupService.getGroupnames(userId)

        layout.add(UIFieldset(UILength(md = 6))
                        .add(UIReadOnlyField("username", userLC))
                        .add(UIInput("firstname", userLC))
                        .add(UIInput("lastname", userLC))
                        .add(UIReadOnlyField("calendarExportToken", authenticationsLC, canCopy = true, coverUp = true, ignoreTooltip = true))
                        .add(UIReadOnlyField("davToken", authenticationsLC, canCopy = true, coverUp = true, ignoreTooltip = true))
                        .add(UIReadOnlyField("restClientToken", authenticationsLC, canCopy = true, coverUp = true, ignoreTooltip = true))
                )
                .add(UIFieldset(12)
                        .add(UIReadOnlyField("groups", dataLC, label = "user.assignedGroups"))
                )
        LayoutUtils.process(layout)
        //layout.addTranslation("user.assignedGroups")


        return FormLayoutData(data, layout, null)
    }
}
