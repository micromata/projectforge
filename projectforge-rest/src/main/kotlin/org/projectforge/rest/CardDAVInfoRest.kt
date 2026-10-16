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

import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * What a CardDAV client of the logged-in user needs to subscribe to the address books: user, token and the server
 * urls. Shown by the CardDAV dialog of the address list (projectforge-next), offered only if the CardDAV server is
 * enabled (see [AddressEntityRest.carddavServerEnabled]). The urls are set by the CardDAV module (`CardDavConfig`).
 */
@RestController
@RequestMapping("${Rest.URL}/cardDAVInfo")
class CardDAVInfoRest {

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

    @AccessChecked("own user only: the DAV token of the logged-in user")
    @GetMapping("info")
    fun getInfo(): Data {
        return Data(
            user = ThreadLocalUserContext.requiredLoggedInUser.username,
            password = authenticationsService.getToken(ThreadLocalUserContext.requiredLoggedInUserId, UserTokenType.DAV_TOKEN),
            appleUrl = appleUrl,
            applePath = applePath,
            iOSUrl = iOSUrl,
            standardUrl = standardUrl,
        )
    }

    companion object {
        var standardUrl = "http://localhost:8080/carddav"
        var appleUrl = "localhost"
        var applePath = "/carddav/"
        var iOSUrl = "localhost/carddav"
    }
}
