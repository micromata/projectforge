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

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/cardDAVInfo")
class CardDAVInfoPageRest {

    class Data(
        var user: String? = null,
        var password: String? = null,
        var standardUrl: String? = null,
        var appleUrl: String? = null,
        var applePath: String? = null,
        var iOSUrl: String? = null,
    )

    @Autowired
    private lateinit var authenticationsService: UserAuthenticationsService

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val username = ThreadLocalUserContext.loggedInUser?.username ?: "?????"
        val layout = UILayout("address.cardDAV.infopage.title")
            .add(UILabel("address.cardDAV.infopage.description"))
            .add(UILabel("address.cardDAV.infopage.description2"))
            .add(UILabel("address.cardDAV.infopage.description3"))
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(UIReadOnlyField("user", label = "user"))
                    )
                    .add(
                        UICol()
                            .add(UIReadOnlyField("password", label = "password", coverUp = true))
                    )
            )
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(
                                UIRow()
                                    .add(
                                        UICol()
                                            .add(
                                                UIReadOnlyField(
                                                    "appleUrl",
                                                    label = "'Server (Apple macOS X)",
                                                    canCopy = true
                                                )
                                            )
                                    )
                                    .add(
                                        UICol()
                                            .add(
                                                UIReadOnlyField(
                                                    "applePath",
                                                    label = "'Path (Apple macOS X)",
                                                    canCopy = true
                                                )
                                            )
                                    )
                            )
                    )
                    .add(
                        UICol()
                            .add(UIReadOnlyField("standardUrl", label = "'Server (Thunderbird etc.)", canCopy = true))
                    )
            )
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(UIReadOnlyField("iOSUrl", label = "'Url (Apple iOS)", canCopy = true))
                    )
                    .add(
                        UICol()
                    )
            )

        LayoutUtils.process(layout)

        val data = Data(
            user = username,
            password = authenticationsService.getToken(
                ThreadLocalUserContext.loggedInUserId!!,
                UserTokenType.DAV_TOKEN
            ),
            appleUrl = appleUrl,
            applePath = applePath,
            iOSUrl = iOSUrl,
            standardUrl = standardUrl,
        )

        return FormLayoutData(data, layout, null)
    }

    companion object {
        var standardUrl = "http://localhost:8080/carddav"
        var appleUrl = "localhost"
        var applePath = "/carddav/"
        var iOSUrl = "localhost/carddav"
    }
}
