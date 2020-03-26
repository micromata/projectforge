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
import org.projectforge.business.user.UserAccessLogEntries
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.web.rest.UserAccessLogEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/user")
open class UserServicesRest {
    @Autowired
    private lateinit var userAuthenticationsService: UserAuthenticationsService

    class AccessLogEntries(userAccessLogEntries: UserAccessLogEntries?) {
        val tokenType: UserTokenType? = userAccessLogEntries?.tokenType
        var entries = userAccessLogEntries?.sortedList()
    }

    @GetMapping("renewToken")
    fun renewToken(@RequestParam("token", required = true) tokenString: String): ResponseAction {
        val tokenType = UserTokenType.valueOf(tokenString)
        val variable = when (tokenType) {
            UserTokenType.CALENDAR_REST -> "calendarExportToken"
            UserTokenType.DAV_TOKEN -> "davToken"
            UserTokenType.REST_CLIENT -> "restClientToken"
            UserTokenType.STAY_LOGGED_IN_KEY -> "stayLoggedInKey"
        }
        val newToken = userAuthenticationsService.getToken(ThreadLocalUserContext.getUserId(), tokenType)
        userAuthenticationsService.renewToken(ThreadLocalUserContext.getUserId(), tokenType)
        return ResponseAction(message = ResponseAction.Message("user.authenticationToken.renew.successful"), targetType = TargetType.UPDATE)
                .addVariable(variable, newToken)
    }

    @GetMapping("tokenAccess")
    fun getTokenAccess(@RequestParam("token", required = true) tokenString: String): AccessLogEntries {
        val tokenType = UserTokenType.valueOf(tokenString)
        return AccessLogEntries(userAuthenticationsService.getUserAccessLogEntries(tokenType))
    }
}
