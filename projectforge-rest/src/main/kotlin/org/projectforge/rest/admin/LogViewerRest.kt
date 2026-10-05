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

package org.projectforge.rest.admin

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.Constants
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.common.logging.LogSubscription
import org.projectforge.common.logging.LoggerMemoryAppender
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * The log viewer of projectforge-next (`next/logViewer/<id>`): lets every user browse the log events of an own
 * [LogSubscription] queue (e.g. of the DATEV import, a Merlin run, a script or a mass update). The admin variant
 * ([AdminLogViewerRest], `next/adminLogViewer`) shows the last 10,000 log events of the whole system.
 *
 * Plain JSON for the hand built next page, successor of the server laid out `LogViewerPageRest` of the React app.
 * The admin variant lives under a REST path of its own, so its 2FA registration (`ProjectForge2FAInitialization`,
 * ADMIN) gates `/rs/adminLogViewer` without gating the users' `/rs/logViewer`.
 *
 * A subscription is identified by its in-memory id, which is only valid until a restart of the server or one hour
 * without activity; an unknown id is answered as [LogViewerData.subscriptionMissing], a subscription of another
 * user is denied.
 */
@RestController
@RequestMapping("${Rest.URL}/logViewer")
open class LogViewerRest {
    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var userPrefService: UserPrefService

    protected var adminLogViewer = false

    /**
     * @param title The subscription's display title, or the title of the admin log viewer.
     * @param filter The filter to start with: the admin's last one (user pref) or the default one.
     * @param admin True for the admin log viewer, which shows user, user agent and stack traces.
     * @param subscriptionMissing The subscription is gone (expired or the server was restarted).
     */
    class LogViewerData(
        val title: String?,
        val filter: LogViewFilter,
        val entries: List<LogViewerEvent>,
        val admin: Boolean,
        val subscriptionMissing: Boolean = false,
    )

    /**
     * @param id Id of the [LogSubscription] to show; ignored by the admin log viewer.
     */
    @AccessChecked("Admin group (admin viewer) or own log subscription only")
    @GetMapping("initial")
    fun getInitial(@RequestParam("id", required = false) id: Int?): LogViewerData {
        if (adminLogViewer) {
            accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
            val filter = getUserPref()
            return LogViewerData(
                title = translate("system.admin.adminLogViewer.title"),
                filter = filter,
                entries = queryAll(filter),
                admin = true,
            )
        }
        val filter = LogViewFilter(logSubscriptionId = id)
        val subscription = getOwnSubscription(id)
            ?: return LogViewerData(
                title = null,
                filter = filter,
                entries = emptyList(),
                admin = false,
                subscriptionMissing = true,
            )
        return LogViewerData(
            title = subscription.displayTitle,
            filter = filter,
            entries = query(subscription, filter),
            admin = false,
        )
    }

    @AccessChecked("Admin group (admin viewer) or own log subscription only")
    @PostMapping("query")
    fun query(@RequestBody filter: LogViewFilter): List<LogViewerEvent> {
        if (adminLogViewer) {
            accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
            getUserPref().let { userPref ->
                userPref.search = filter.search
                userPref.threshold = filter.threshold
                userPref.autoRefresh = filter.autoRefresh
            }
            return queryAll(filter)
        }
        val subscription = getOwnSubscription(filter.logSubscriptionId) ?: return emptyList()
        return query(subscription, filter)
    }

    /**
     * Hides all entries received so far of the user's subscription: from now on only newer ones are shown. The
     * queue itself isn't cleared (see [LogSubscription.reset]). Not available for the admin log viewer.
     */
    @AccessChecked("Admin group (admin viewer) or own log subscription only")
    @PostMapping("reset")
    fun reset(@RequestBody filter: LogViewFilter): List<LogViewerEvent> {
        if (adminLogViewer) {
            throw AccessException("access.exception.noAccess")
        }
        val subscription = getOwnSubscription(filter.logSubscriptionId) ?: return emptyList()
        subscription.reset()
        return query(subscription, filter)
    }

    private fun query(subscription: LogSubscription, filter: LogViewFilter): List<LogViewerEvent> {
        return subscription.query(filter.logFilter).map { LogViewerEvent(it, userFriendlyTime = true) }
    }

    private fun queryAll(filter: LogViewFilter): List<LogViewerEvent> {
        return LoggerMemoryAppender.getInstance().query(filter.logFilter).map { LogViewerEvent(it) }
    }

    /**
     * @return The subscription, or null if it doesn't exist (anymore).
     * @throws AccessException If the subscription belongs to another user.
     */
    private fun getOwnSubscription(id: Int?): LogSubscription? {
        if (id == null || id < 0) {
            return null
        }
        val subscription = LogSubscription.getSubscription(id) ?: return null
        val username = ThreadLocalUserContext.loggedInUser?.username
        if (subscription.user != username) {
            log.warn { "Log subscription #$id of user '${subscription.user}' requested by user '$username'. Access denied." }
            throw AccessException("access.exception.noAccess")
        }
        return subscription
    }

    private fun getUserPref(): LogViewFilter {
        return userPrefService.ensureEntry("logging", "logViewFilter", LogViewFilter())
    }

    companion object {
        /**
         * The url of the next log viewer showing the given subscription, e.g. `next/logViewer/7`.
         * @param absolute If true, the url starts with `/` (for links of the Wicket pages).
         */
        @JvmStatic
        @JvmOverloads
        fun viewerUrl(subscriptionId: Int, absolute: Boolean = false): String {
            val prefix = if (absolute) "/" else ""
            return "$prefix${Constants.NEXT_APP_PATH}logViewer/$subscriptionId"
        }
    }
}
