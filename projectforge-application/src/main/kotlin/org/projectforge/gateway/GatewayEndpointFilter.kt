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

package org.projectforge.gateway

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@Order(1)
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
class GatewayEndpointFilter : Filter {

    private val allowedPrefixes = listOf(
        "/carddav/",
        "/.well-known/carddav",
        "/export/ProjectForge.ics",
        "/rsPublic/datatransfer/",
        "/rs/datatransfer/",
        "/api/gateway/sync/",
        "/login/oauth2/",
        "/oauth2/",
        "/rsPublic/login",
        "/rsPublic/setup",
    )

    private val allowedExtensions = listOf(
        ".css", ".js", ".png", ".jpg", ".gif", ".ico", ".svg", ".woff", ".woff2", ".ttf",
    )

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val path = httpRequest.requestURI

        if (isAllowed(path)) {
            chain.doFilter(request, response)
        } else {
            log.debug { "Gateway mode: blocked request to $path" }
            (response as HttpServletResponse).sendError(HttpServletResponse.SC_NOT_FOUND)
        }
    }

    private fun isAllowed(path: String): Boolean {
        if (path == "/" || path == "/favicon.ico") return true
        for (prefix in allowedPrefixes) {
            if (path.startsWith(prefix)) return true
        }
        for (ext in allowedExtensions) {
            if (path.endsWith(ext)) return true
        }
        return false
    }
}
