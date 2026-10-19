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
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.rest.config.AttachmentCategoryRequests
import org.projectforge.rest.config.AttachmentCategoryRequests.ATTACHMENTS_URL
import org.projectforge.rest.config.AttachmentCategoryRequests.BODY_CATEGORY_URLS
import org.projectforge.rest.pub.HeartbeatRest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * The allow-list of the gateway: everything not named here answers 404, whatever the security config says.
 *
 * The DataTransfer UI is the projectforge-next app, of which only the data transfer routes and the assets are
 * served (`/next/datatransfer/...`, the external access `/next/public/datatransfer/...`, `/next/_next/...`), plus the REST calls these pages and the app's shell make.
 * Every other page of the app is missing, so a link to one of them ends on the gateway's error page.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
class GatewayEndpointFilter : Filter {

    private val allowedPrefixes = listOf(
        "/carddav/",
        "/.well-known/carddav",
        "/export/ProjectForge.ics",
        "/rsPublic/datatransfer/",
        "/api/gateway/sync/",
        "/login/oauth2/",
        "/oauth2/",
        "/logout",
        "/rsPublic/login",
        "/rsPublic/setup",
        HeartbeatRest.URL,
        // projectforge-next: the data transfer pages (html and the RSC payloads, *.txt) and the assets.
        GatewayPageController.DATATRANSFER_URL,
        GatewayPageController.PUBLIC_DATATRANSFER_URL,
        "/next/_next/",
        // The REST services of the app's shell: login state, menu, theme, texts of the installation.
        "/rs/userStatus",
        "/rs/menu",
        "/rs/logout",
        "/rs/uiSettings",
        "/rsPublic/systemStatus",
        "/rsPublic/i18nCustomerOverrides",
        "/rs/user/autosearch",
        "/rs/group/autosearch",
        // Login and error page (GatewayPageController).
        GatewayPageController.LOGIN_URL,
        "/error",
    ) + GatewaySecurityConfig.DATATRANSFER_CATEGORIES.map { "/rs/$it" } +
            // The files of an area: where the category is part of the path, only the one of the data transfer.
            AttachmentCategoryRequests.PATH_CATEGORY_CALLS.map { "$ATTACHMENTS_URL/$it/$DATATRANSFER_CATEGORY/" }

    private val allowedExtensions = listOf(
        ".css", ".js", ".png", ".jpg", ".gif", ".ico", ".svg", ".woff", ".woff2", ".ttf",
    )

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val path = httpRequest.requestURI

        if (isAllowed(path)) {
            chain.doFilter(request, response)
            return
        }
        if (path in BODY_CATEGORY_URLS) {
            // The category is part of the JSON body: read it here and hand the body on to the controller.
            val body = httpRequest.inputStream.readAllBytes()
            if (AttachmentCategoryRequests.categoryOf(body) == DATATRANSFER_CATEGORY) {
                chain.doFilter(AttachmentCategoryRequests.CachedBodyRequest(httpRequest, body), response)
                return
            }
        }
        log.debug { "Gateway mode: blocked request to $path" }
        (response as HttpServletResponse).sendError(HttpServletResponse.SC_NOT_FOUND)
    }

    private fun isAllowed(path: String): Boolean {
        if (path == "/" || path == "/favicon.ico" || path == "/next" || path == "/next/") return true
        for (prefix in allowedPrefixes) {
            if (path.startsWith(prefix)) return true
        }
        for (ext in allowedExtensions) {
            if (path.endsWith(ext)) return true
        }
        return false
    }

    companion object {
        private const val DATATRANSFER_CATEGORY = "datatransfer"
    }
}
