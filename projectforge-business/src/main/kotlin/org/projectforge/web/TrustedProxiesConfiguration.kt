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

package org.projectforge.web

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

/**
 * Hands `projectforge.security.trustedProxies` over to [WebUtils], which is an object and can't be injected
 * into.
 */
@Configuration
open class TrustedProxiesConfiguration {
    @Value("\${projectforge.security.trustedProxies:}")
    private var trustedProxies: String? = null

    @PostConstruct
    private fun postConstruct() {
        val proxies = TrustedProxies(trustedProxies)
        WebUtils.trustedProxies = proxies
        if (proxies.isEmpty) {
            log.info { "projectforge.security.trustedProxies is empty: X-Forwarded-For isn't used, the ip address of a client behind a reverse proxy is the proxy's one." }
        } else {
            log.info { "Trusted proxies (X-Forwarded-For is used for requests coming from these): $trustedProxies" }
        }
    }
}
