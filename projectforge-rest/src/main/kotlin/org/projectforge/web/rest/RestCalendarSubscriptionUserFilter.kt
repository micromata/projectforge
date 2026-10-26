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

package org.projectforge.web.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.pub.CalendarSubscriptionServiceRest
import org.projectforge.rest.utils.RequestLog
import org.projectforge.security.SecurityLogging
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

class RestCalendarSubscriptionUserFilter : AbstractRestUserFilter(UserTokenType.CALENDAR_REST) {
    override fun authenticate(authInfo: RestAuthenticationInfo) {
        val userString = restAuthenticationUtils.getUserString(authInfo, arrayOf("user"), UserTokenType.CALENDAR_REST, true)
        val userId = NumberHelper.parseLong(userString)
                ?: run {
                    if (authInfo.resultCode == null) {
                        // error not yet handled.
                        val msg = "UserId not found in request parameters ('user') or can't parse it as int value. Rest call denied."
                        log.error { msg }
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
        if (authInfo.success) {
            logSubscriptionParams(params, authInfo)
        }
    }

    /**
     * Which kinds of subscriptions the clients use (single or several team calendars, reminders, timesheets, holidays,
     * week of years), e.g. for checking which of them the gateway serves. Only the names of the parameters and the
     * number of team calendars: no token, no ids. The gateway push exports internally, not by this filter.
     *
     * Logged once per kind and user agent since the start (the clients poll every few minutes), further requests on
     * debug level.
     */
    private fun logSubscriptionParams(params: Map<String, String>, authInfo: RestAuthenticationInfo) {
        val names = params.keys.filter { it != "token" }.sorted().joinToString(",")
        val teamCals = params["teamCals"]?.split(";")?.count { it.isNotBlank() } ?: 0
        val reminders = params[CalendarSubscriptionServiceRest.PARAM_EXPORT_REMINDER] == "true"
        // Client controlled: no line breaks into the log.
        val userAgent = authInfo.request.getHeader("User-Agent")?.take(100)?.replace(Regex("\\p{Cntrl}"), "?")
        val kind = "params=[$names], teamCals=$teamCals, reminders=$reminders, userAgent=$userAgent"
        if (loggedKinds.size < MAX_LOGGED_KINDS && loggedKinds.add(kind)) {
            log.info { "Calendar subscription (first of this kind since start): $kind" }
        } else {
            log.debug { "Calendar subscription: $kind" }
        }
    }

    companion object {
        /**
         * Kinds of subscriptions already logged (no user specific data). Bounded, as the user agent is client
         * controlled.
         */
        private val loggedKinds: MutableSet<String> = ConcurrentHashMap.newKeySet()

        private const val MAX_LOGGED_KINDS = 1000
    }
}
