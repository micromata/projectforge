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
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.ServletRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.io.ByteArrayInputStream

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
    fun allowsDataTransferNextUi() {
        assertAllowed("/next/datatransfer")
        assertAllowed("/next/datatransfer/")
        assertAllowed("/next/datatransfer/index.txt")
        assertAllowed("/next/datatransfer/42/")
        assertAllowed("/next/datatransfer/new/edit/index.txt")
        assertAllowed("/next/datatransfer/personal-box/")
        assertAllowed("/next/_next/static/chunks/main.js")
        assertAllowed("/next/_next/static/media/font.woff2")
        assertAllowed("/next/favicon.ico")
        assertAllowed("/next/")
    }

    @Test
    fun allowsRestServicesOfNextUi() {
        assertAllowed("/rs/userStatus")
        assertAllowed("/rs/menu")
        assertAllowed("/rs/menu/recent")
        assertAllowed("/rs/logout")
        assertAllowed("/rs/uiSettings/theme")
        assertAllowed("/rsPublic/systemStatus")
        assertAllowed("/rsPublic/i18nCustomerOverrides")
        assertAllowed("/rs/user/autosearch")
        assertAllowed("/rs/group/autosearch")
        assertAllowed("/rs/datatransfer/listMeta")
        assertAllowed("/rs/datatransferfiles/42")
        assertAllowed("/rs/datatransferaudit/42")
        assertAllowed("/rs/datatransferpersonalfiles/box")
    }

    @Test
    fun allowsDataTransferAttachmentsOnly() {
        assertAllowed("/rs/attachments/upload/datatransfer/42/attachments")
        assertAllowed("/rs/attachments/download/datatransfer/42")
        assertAllowed("/rs/attachments/multiDownload/datatransfer/42")
        assertBlocked("/rs/attachments/upload/book/42/attachments")
        assertBlocked("/rs/attachments/download/contract/42")
        assertBlocked("/rs/attachments/multiDownload/datatransferx/42")
    }

    @Test
    fun checksCategoryInBodyOfAttachmentCalls() {
        for (url in GatewayEndpointFilter.BODY_CATEGORY_URLS) {
            assertAllowedWithBody(url, """{"data":{"category":"datatransfer","id":42,"fileId":"abc"}}""")
            assertBlocked(url, """{"data":{"category":"book","id":42,"fileId":"abc"}}""")
            assertBlocked(url, """{"data":{"id":42}}""")
            assertBlocked(url, "no json")
        }
    }

    @Test
    fun allowsLoginAndErrorPage() {
        assertAllowed("/next/login")
        assertAllowed("/next/login/")
        assertAllowed("/error")
    }

    @Test
    fun blocksOtherReactAndNextPages() {
        assertBlocked("/react/address")
        assertBlocked("/react/user/edit/1")
        assertBlocked("/next/address")
        assertBlocked("/next/address/index.txt")
        assertBlocked("/next/index.txt")
        assertBlocked("/react/datatransfer")
        assertBlocked("/react-app.html")
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

    private fun assertAllowedWithBody(path: String, body: String) {
        val request = mockRequest(path, body)
        val response = mock(HttpServletResponse::class.java)
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        // The body was consumed by the filter, so the controller gets it again from a wrapper.
        val captor = ArgumentCaptor.forClass(ServletRequest::class.java)
        verify(chain).doFilter(captor.capture(), eq(response))
        assertEquals(body, String(captor.value.inputStream.readAllBytes()))
        verify(response, never()).sendError(anyInt())
    }

    private fun assertBlocked(path: String, body: String? = null) {
        val request = mockRequest(path, body)
        val response = mock(HttpServletResponse::class.java)
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        verify(chain, never()).doFilter(any(), any())
        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
    }

    private fun mockRequest(path: String, body: String?): HttpServletRequest {
        val request = mock(HttpServletRequest::class.java)
        `when`(request.requestURI).thenReturn(path)
        if (body != null) {
            val input = ByteArrayInputStream(body.toByteArray())
            `when`(request.inputStream).thenReturn(object : ServletInputStream() {
                override fun read(): Int = input.read()
                override fun isFinished(): Boolean = input.available() == 0
                override fun isReady(): Boolean = true
                override fun setReadListener(listener: ReadListener?) {}
            })
        }
        return request
    }
}
