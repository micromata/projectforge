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

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GatewayEndpointFilterTest {

    private val filter = GatewayEndpointFilter()

    @Test
    fun allowsCardDavRequests() {
        assertAllowed("/carddav/principals/users/kai/")
    }

    @Test
    fun allowsWellKnownCardDav() {
        assertAllowed("/.well-known/carddav")
    }

    @Test
    fun allowsIcsExport() {
        assertAllowed("/export/ProjectForge.ics")
    }

    @Test
    fun allowsDataTransferPublic() {
        assertAllowed("/rsPublic/datatransfer/download/abc123")
    }

    @Test
    fun allowsDataTransferAuthenticated() {
        assertAllowed("/rs/datatransfer/list")
    }

    @Test
    fun allowsSyncApi() {
        assertAllowed("/api/gateway/sync/users")
    }

    @Test
    fun allowsOAuth2Paths() {
        assertAllowed("/login/oauth2/code/authentik")
        assertAllowed("/oauth2/authorization/authentik")
    }

    @Test
    fun allowsStaticResources() {
        assertAllowed("/static/app.js")
        assertAllowed("/rsPublic/login")
    }

    @Test
    fun allowsFavicon() {
        assertAllowed("/favicon.ico")
    }

    @Test
    fun allowsRoot() {
        assertAllowed("/")
    }

    @Test
    fun blocksInternalRestApi() {
        assertBlocked("/rs/user/list")
    }

    @Test
    fun blocksWicketPaths() {
        assertBlocked("/wa/wicket/page")
    }

    @Test
    fun blocksArbitraryPaths() {
        assertBlocked("/admin/status")
        assertBlocked("/rsPublicx/something")
    }

    @Test
    fun blocksInternalBusinessEndpoints() {
        assertBlocked("/rs/timesheet/list")
        assertBlocked("/rs/address/list")
        assertBlocked("/rs/calendar/events")
    }

    private fun assertAllowed(path: String) {
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val chain = mock(FilterChain::class.java)
        `when`(request.requestURI).thenReturn(path)

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
        verify(response, never()).sendError(anyInt())
    }

    private fun assertBlocked(path: String) {
        val request = mock(HttpServletRequest::class.java)
        val response = mock(HttpServletResponse::class.java)
        val chain = mock(FilterChain::class.java)
        `when`(request.requestURI).thenReturn(path)

        filter.doFilter(request, response, chain)

        verify(chain, never()).doFilter(request, response)
        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
    }
}
