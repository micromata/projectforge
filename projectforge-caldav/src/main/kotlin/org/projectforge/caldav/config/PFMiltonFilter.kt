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

package org.projectforge.caldav.config

import io.milton.servlet.MiltonFilter
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.web.rest.RestAuthenticationInfo
import org.projectforge.web.rest.RestAuthenticationUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

/**
 * Ensuring a white url list for using Milton filter. MiltonFilter at default supports only black list.
 */
class PFMiltonFilter : MiltonFilter() {
    @Autowired
    private lateinit var restAuthenticationUtils: RestAuthenticationUtils

    private var initialized: Boolean = false

    companion object {
        private val miltonUrls = listOf("/users", "/principals")
        private val supportedAgentsRegexps = listOf(
                "Address.*Book".toRegex(),
                "eM.*Client".toRegex())
        private val supportedAgentsStrings = listOf(
                "DAVdroid",
                "accountsd",
                "Adresboek",
                "Adressbuch",
                "Calendar",
                "CalendarAgent",
                "CalendarStore",
                "CoreDAV",
                "DataAccess",
                "dataaccessd",
                "DAVKit",
                "iOS",
                "Lightning",
                "Preferences",
                "Fantastical",
                "Reminders")
        private val excludedAgentsStrings = listOf("CriOS")
        private val log = LoggerFactory.getLogger(PFMiltonFilter::class.java)
    }

    override fun doFilter(req: ServletRequest, resp: ServletResponse, fc: FilterChain) {
        if (!initialized) {
            // Late initialization is required. ApplicationContext isn't available in constructor.
            ApplicationContextProvider.getApplicationContext().autowireCapableBeanFactory.autowireBean(this);
            initialized = true
        }
        if (req is HttpServletRequest) {
            val userAgent: String = req.getHeader("User-Agent")
            if (checkUserAgent(userAgent)) {
                val url: String = req.requestURI
                if (miltonUrls.any { url.startsWith(it) }) {
                    log.info("Processed by milton.io: $url")
                    val userInfo: RestAuthenticationInfo = restAuthenticationUtils.authenticate(req, resp)
                            ?: return // Not authenticated.
                    try {
                        restAuthenticationUtils.registerUser(req, userInfo)
                        super.doFilter(req, resp, fc)
                    } finally {
                        restAuthenticationUtils.unregister(req, resp, userInfo)
                    }
                    return
                }
            }
        }
        fc.doFilter(req, resp)
    }

    internal fun checkUserAgent(userAgent: String?): Boolean {
        return !userAgent.isNullOrBlank()
                && (supportedAgentsStrings.any { userAgent == it } || supportedAgentsRegexps.any { it.matches(userAgent) })
                && excludedAgentsStrings.none { userAgent == it }
    }
}
