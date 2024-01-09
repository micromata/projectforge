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

package org.projectforge.web.rest

import mu.KotlinLogging
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.pub.CalendarSubscriptionServiceRest
import org.projectforge.rest.utils.RequestLog
import org.projectforge.security.SecurityLogging
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

private val log = KotlinLogging.logger {}

class RestCalendarSubscriptionUserFilter : AbstractRestUserFilter(UserTokenType.CALENDAR_REST) {
    override fun authenticate(authInfo: RestAuthenticationInfo) {
        val userString = restAuthenticationUtils.getUserString(authInfo, arrayOf("user"), UserTokenType.CALENDAR_REST, true)
        val userId = NumberHelper.parseInteger(userString)
                ?: run {
                    if (authInfo.resultCode == null) {
                        // error not yet handled.
                        val msg = "UserId not found in request parameters ('user') or can't parse it as int value. Rest call denied."
                        log.error(msg)
                        SecurityLogging.logSecurityWarn(authInfo.request, this::class.java, "${UserTokenType.CALENDAR_REST.name} AUTHENTICATION FAILED", msg)
                        authInfo.resultCode = HttpStatus.BAD_REQUEST
                    }
                    return
                }
        val params = CalendarSubscriptionServiceRest.decryptRequestParams(authInfo.request, userId, userAuthenticationsService)
        if (params.isNullOrEmpty()) {
            authInfo.resultCode = HttpStatus.BAD_REQUEST
            return
        }
        restAuthenticationUtils.tokenAuthentication(authInfo, UserTokenType.CALENDAR_REST,
                required = true,
                authenticationToken = params["token"],
                userParams = arrayOf("user"),
                tokenParams = arrayOf("q[token]"),
                userId = userId)
    }
}
