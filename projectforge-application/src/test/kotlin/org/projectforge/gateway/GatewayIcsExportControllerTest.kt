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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.gateway.sync.GatewayIcsCache
import org.springframework.http.HttpStatus

@ExtendWith(MockitoExtension::class)
class GatewayIcsExportControllerTest {

    private val icsCache = GatewayIcsCache()
    private val userAuthenticationsService = mock<UserAuthenticationsService>()
    private val controller = GatewayIcsExportController(icsCache, userAuthenticationsService)

    @Test
    fun returnsBadRequestForMissingParams() {
        val response = controller.exportCalendar(null, "q")
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)

        val response2 = controller.exportCalendar(1L, null)
        assertEquals(HttpStatus.BAD_REQUEST, response2.statusCode)

        val response3 = controller.exportCalendar(1L, "")
        assertEquals(HttpStatus.BAD_REQUEST, response3.statusCode)
    }

    @Test
    fun returnsUnauthorizedForInvalidToken() {
        whenever(userAuthenticationsService.decrypt(eq(1L), eq(UserTokenType.CALENDAR_REST), any()))
            .thenReturn(null)

        val response = controller.exportCalendar(1L, "invalid-encrypted-q")
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun returnsNotFoundForCacheMiss() {
        whenever(userAuthenticationsService.decrypt(eq(1L), eq(UserTokenType.CALENDAR_REST), eq("valid-q")))
            .thenReturn("token=abc&teamCals=5")

        val response = controller.exportCalendar(1L, "valid-q")
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun returnsCachedIcsData() {
        val icsData = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR"
        icsCache.put(1L, "valid-q", icsData)
        whenever(userAuthenticationsService.decrypt(eq(1L), eq(UserTokenType.CALENDAR_REST), eq("valid-q")))
            .thenReturn("token=abc&teamCals=5")

        val response = controller.exportCalendar(1L, "valid-q")
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("text/calendar;charset=UTF-8", response.headers.contentType?.toString())
        assertEquals(icsData, String(response.body!!, Charsets.UTF_8))
    }
}
