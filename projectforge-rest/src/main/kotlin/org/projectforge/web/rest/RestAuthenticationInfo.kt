/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.rest

import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.http.HttpStatus
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RestAuthenticationInfo(var request: HttpServletRequest,
                             var response: HttpServletResponse) {
    val success: Boolean
        get() = user != null && resultCode == null

    var user: PFUserDO? = null
        set(value) {
            value?.clearSecretFields()
            if (value != null) {
                userString = value.username
            }
            field = value
        }
    var userString: String? = null
    var clientIpAddress: String = request.remoteAddr ?: "unkown"
    var resultCode: HttpStatus? = null
    var lockedByTimePenalty = false

    /**
     * Will be set, if the user is authenticated by an authentication token inside a rest call (not by session or password).
     * This flag is used for disabling CSRF protection, because this isn't needed by pure REST clients (only for Web clients).
     */
    var loggedInByAuthenticationToken = false
}
