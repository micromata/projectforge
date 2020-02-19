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

package org.projectforge.web.rest

import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.pub.CalendarSubscriptionServiceRest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

class RestCalendarSubscriptionUserFilter : AbstractRestUserFilter() {
    override fun authenticate(authInfo: RestAuthenticationInfo) {
        val userString = restAuthenticationUtils.getUserString(authInfo, arrayOf("user"), true)
        val userId = NumberHelper.parseInteger(userString)
                ?: run {
                    log.info("UserId not found in request parameters or can't parse it as int value. Rest call denied.")
                    authInfo.resultCode = HttpStatus.BAD_REQUEST
                    return
                }
        val params = CalendarSubscriptionServiceRest.decryptRequestParams(authInfo.request, userId, userAuthenticationsService)
        if (params.isNullOrEmpty()) {
            authInfo.resultCode = HttpStatus.BAD_REQUEST
            return
        }
        // validate user
        authInfo.user = userAuthenticationsService.getUserByToken(userId, UserTokenType.CALENDAR_REST, params["token"])
        if (authInfo.user == null) {
            log.error("Bad request, user not found: ${authInfo.request.queryString}")
            authInfo.resultCode = HttpStatus.BAD_REQUEST
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RestCalendarSubscriptionUserFilter::class.java)
    }
}
