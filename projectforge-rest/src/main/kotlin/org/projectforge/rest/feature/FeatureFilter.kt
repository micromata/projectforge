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


package org.projectforge.rest.feature

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.projectforge.rest.config.AttachmentCategoryRequests
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.ObjectProvider

private val log = KotlinLogging.logger {}

/**
 * Answers 404 for the REST endpoints and next pages of switched off features (see [FeaturePathIndex]).
 *
 * Registered by `WebXMLInitializer` for `/rs/`, `/rsPublic/` and `/next/`, before the authentication: a
 * switched off endpoint doesn't exist, logged in or not.
 */
class FeatureFilter(private val featurePathIndexProvider: ObjectProvider<FeaturePathIndex>) : Filter {
    private val featurePathIndex: FeaturePathIndex
        get() = featurePathIndexProvider.getObject()

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val path = AttachmentCategoryRequests.pathOf(httpRequest)
        if (featurePathIndex.isBlocked(path)) {
            notFound(path, response)
            return
        }
        if (path.startsWith(ATTACHMENT_PAGE_URL) && httpRequest.method == "GET") {
            // The dialog of an attachment (AttachmentPageRest) names the entity as query parameter.
            val category = httpRequest.getParameter("category")
            if (category != null && featurePathIndex.isCategoryBlocked(category)) {
                notFound("$path ($category)", response)
                return
            }
        }
        if (path in AttachmentCategoryRequests.BODY_CATEGORY_URLS &&
            httpRequest.contentType?.startsWith("multipart/") != true && featurePathIndex.anythingBlocked()
        ) {
            // The category is part of the JSON body: read it here and hand the body on to the controller.
            val body = AttachmentCategoryRequests.readBody(httpRequest)
            if (body == null) {
                (response as HttpServletResponse).sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE)
                return
            }
            val category = AttachmentCategoryRequests.categoryOf(body)
            if (category != null && featurePathIndex.isCategoryBlocked(category)) {
                notFound("$path ($category)", response)
                return
            }
            chain.doFilter(AttachmentCategoryRequests.CachedBodyRequest(httpRequest, body), response)
            return
        }
        chain.doFilter(request, response)
    }

    private fun notFound(path: String, response: ServletResponse) {
        log.debug { "Feature switched off: blocked request to $path" }
        (response as HttpServletResponse).sendError(HttpServletResponse.SC_NOT_FOUND)
    }

    companion object {
        private const val ATTACHMENT_PAGE_URL = "${Rest.URL}/attachment/"
    }
}
