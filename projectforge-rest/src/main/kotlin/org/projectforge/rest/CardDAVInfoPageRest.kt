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

import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/cardDAVInfo")
class CardDAVInfoPageRest {

    class Data(
            var user: String? = null,
            var password: String? = null,
            var server: String? = null,
            var serverPath: String? = null,
            var ios: String? = null,
            var thunderbird: String? = null
    )

    @Autowired
    private lateinit var authenticationsService: UserAuthenticationsService

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest, @RequestParam("type") type: String?): FormLayoutData {
        val username = ThreadLocalUserContext.getUser()?.username ?: "?????"
        val layout = UILayout("address.cardDAV.infopage.title")
                .add(UILabel("address.cardDAV.infopage.description"))
                .add(UILabel("address.cardDAV.infopage.description2"))
                .add(UILabel("address.cardDAV.infopage.description3"))
                        .add(UIRow()
                                .add(UICol()
                                        .add(UIReadOnlyField("user", label = "user")))
                                .add(UICol()
                                        .add(UIReadOnlyField("password", label = "password", coverUp = true))))
                .add(UIRow()
                        .add(UICol()
                                .add(UIReadOnlyField("server", label = "server", canCopy = true)))
                        .add(UICol()

                                .add(UIReadOnlyField("serverPath",
                                        label = "Apple Addressbook",
                                        additionalLabel = "CardDAV account, server path (Catalina)",
                                        canCopy = true))))
                .add(UIRow()
                        .add(UICol()
                                .add(UIReadOnlyField("ios", label = "iOS", canCopy = true, coverUp = true)))
                        .add(UICol()
                                .add(UIReadOnlyField("thunderbird",
                                        label = "Thunderbird",
                                        canCopy = true,
                                        coverUp = true))))

        LayoutUtils.process(layout)

        val data = Data(
                user = username,
                password = authenticationsService.getToken(ThreadLocalUserContext.getUserId(), UserTokenType.DAV_TOKEN),
                server = request.serverName,
                serverPath = "/users/${username}/addressBooks/default",
                ios = "CardDAV account",
                thunderbird = "CardDAV account"
        )

        return FormLayoutData(data, layout, null)
    }
}
