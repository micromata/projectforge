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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.projectforge.business.user.UserAuthenticationsService
import org.projectforge.business.user.UserTokenType
import org.projectforge.framework.integration.IntegrationConfig
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.gateway.push.GatewaySyncPushService
import org.projectforge.gateway.push.MockIcsRequest
import org.projectforge.gateway.sync.dto.SyncIcsEntryDto
import org.projectforge.rest.pub.CalendarSubscriptionServiceRest
import org.springframework.http.ResponseEntity

class GatewaySyncPushServiceTest {

    @Test
    fun mockIcsRequestProvidesCorrectParameters() {
        val request = MockIcsRequest(42L, "encryptedQueryParam")

        assertEquals("42", request.getParameter("user"))
        assertEquals("encryptedQueryParam", request.getParameter("q"))
        assertNull(request.getParameter("unknown"))
        assertEquals("user=42&q=encryptedQueryParam", request.queryString)
        assertEquals("127.0.0.1", request.remoteAddr)
    }

    @Test
    fun mockIcsRequestParameterMap() {
        val request = MockIcsRequest(7L, "abc123")

        val paramMap = request.parameterMap
        assertArrayEquals(arrayOf("7"), paramMap["user"])
        assertArrayEquals(arrayOf("abc123"), paramMap["q"])
    }

    @Test
    fun mockIcsRequestMetadata() {
        val request = MockIcsRequest(1L, "q")

        assertEquals("GET", request.method)
        assertEquals("/export/ProjectForge.ics", request.requestURI)
        assertEquals("HTTP/1.1", request.protocol)
        assertTrue(request.isSecure)
    }

    @Test
    fun icsContentHashIgnoresDtStamp() {
        fun ics(dtStamp: String, summary: String) =
            "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nDTSTAMP:$dtStamp\r\nSUMMARY:$summary\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"

        val hash = GatewaySyncPushService.icsContentHash(ics("20261004T230226Z", "Meeting"))
        assertEquals(hash, GatewaySyncPushService.icsContentHash(ics("20261004T230542Z", "Meeting")))
        assertNotEquals(hash, GatewaySyncPushService.icsContentHash(ics("20261004T230226Z", "Workshop")))
    }

    /**
     * exportCalendar clears the user context (it's a request entry point), but the push exports several calendars
     * per user: without restoring it, every export after the first one failed with "can't get context user".
     */
    @Test
    fun exportIcsKeepsUserContextForFurtherExports() {
        val serviceRest = mock<CalendarSubscriptionServiceRest>()
        whenever(serviceRest.exportCalendar(any())).thenAnswer {
            val result: ResponseEntity<*> = if (ThreadLocalUserContext.loggedInUserId == null) {
                ResponseEntity.badRequest().build<Any>()
            } else {
                ResponseEntity.ok("BEGIN:VCALENDAR")
            }
            ThreadLocalUserContext.setUser(null)
            result
        }
        val service = createService(serviceRest)
        val exportIcs = GatewaySyncPushService::class.java.getDeclaredMethod(
            "exportIcs", Long::class.java, String::class.java, String::class.java,
        ).also { it.isAccessible = true }
        val user = PFUserDO().also { it.id = 42; it.username = "kai" }
        try {
            ThreadLocalUserContext.userContext = UserContext.createTestInstance(user)
            assertEquals("BEGIN:VCALENDAR", exportIcs.invoke(service, 42L, "q1", "teamCals=1"))
            assertEquals("BEGIN:VCALENDAR", exportIcs.invoke(service, 42L, "q2", "timesheetUser=42"))
            assertEquals(42L, ThreadLocalUserContext.loggedInUserId)
        } finally {
            ThreadLocalUserContext.clear()
        }
    }

    @Test
    fun addIcsEntryGeneratesACachedTeamCalendarOnlyOnce() {
        val serviceRest = mock<CalendarSubscriptionServiceRest>()
        whenever(serviceRest.exportCalendar(any())).thenReturn(ResponseEntity.ok("BEGIN:VCALENDAR"))
        val userAuthenticationsService = mock<UserAuthenticationsService>()
        whenever(userAuthenticationsService.internalGetToken(any(), eq(UserTokenType.CALENDAR_REST)))
            .thenReturn("stored-token")
        val service = createService(serviceRest, userAuthenticationsService)
        val addIcsEntry = GatewaySyncPushService::class.java.declaredMethods.single { it.name == "addIcsEntry" }
            .also { it.isAccessible = true }
        val entries = mutableListOf<SyncIcsEntryDto>()
        val cache = mutableMapOf<String, String?>()
        assertEquals(true, addIcsEntry.invoke(service, 1L, "t1", "teamCals=7", entries, cache, "7:read:de"))
        assertEquals(true, addIcsEntry.invoke(service, 2L, "t2", "teamCals=7", entries, cache, "7:read:de"))
        verify(serviceRest, times(1)).exportCalendar(any())
        // Each user gets an entry of their own (own query param), with the shared ICS data.
        assertEquals(listOf(1L, 2L), entries.map { it.userId })
        assertEquals(listOf("BEGIN:VCALENDAR", "BEGIN:VCALENDAR"), entries.map { it.icsData })
        addIcsEntry.invoke(service, 3L, "t3", "teamCals=7", entries, cache, "7:minimal:de")
        verify(serviceRest, times(2)).exportCalendar(any())
    }

    private fun createService(
        serviceRest: CalendarSubscriptionServiceRest,
        userAuthenticationsService: UserAuthenticationsService = mock(),
    ): GatewaySyncPushService {
        val service = GatewaySyncPushService(
            config = mock(),
            userDao = mock(),
            userGroupCache = mock(),
            addressDao = mock(),
            persistenceService = mock(),
            userAuthenticationsService = userAuthenticationsService,
            teamCalCache = mock(),
            integrationConfig = IntegrationConfig(),
        )
        GatewaySyncPushService::class.java.getDeclaredField("calendarSubscriptionServiceRest").also {
            it.isAccessible = true
            it.set(service, serviceRest)
        }
        return service
    }
}
