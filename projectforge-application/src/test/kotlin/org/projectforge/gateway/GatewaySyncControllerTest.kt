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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.projectforge.gateway.sync.GatewaySyncController
import org.projectforge.gateway.sync.GatewaySyncService
import org.projectforge.gateway.sync.dto.SyncIcsEntryDto
import org.projectforge.gateway.sync.dto.SyncUserDto
import org.springframework.http.HttpStatus

@ExtendWith(MockitoExtension::class)
class GatewaySyncControllerTest {

    private val syncService = mock<GatewaySyncService>()

    @Test
    fun rejectsInvalidSecret() {
        val controller = GatewaySyncController(syncService, "correct-secret")

        val response = controller.syncUsers("wrong-secret", emptyList())
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun rejectsBlankSecret() {
        val controller = GatewaySyncController(syncService, "")

        val response = controller.syncUsers("anything", emptyList())
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun acceptsValidSecretForUsers() {
        val controller = GatewaySyncController(syncService, "my-secret")
        whenever(syncService.syncUsers(any())).thenReturn(
            GatewaySyncService.SyncResult(created = 1, updated = 0, errors = 0)
        )

        val users = listOf(
            SyncUserDto(
                username = "testuser",
                idpExternalId = "sub-123",
                davToken = "dav-token",
                calendarRestToken = "cal-token",
                active = true,
            )
        )
        val response = controller.syncUsers("my-secret", users)
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun acceptsValidSecretForIcs() {
        val controller = GatewaySyncController(syncService, "my-secret")
        whenever(syncService.syncIcsEntries(any())).thenReturn(
            GatewaySyncService.SyncResult(created = 0, updated = 2, errors = 0)
        )

        val entries = listOf(
            SyncIcsEntryDto(userId = 1L, queryParam = "q1", icsData = "BEGIN:VCALENDAR\nEND:VCALENDAR"),
            SyncIcsEntryDto(userId = 1L, queryParam = "q2", icsData = "BEGIN:VCALENDAR\nEND:VCALENDAR"),
        )
        val response = controller.syncIcsEntries("my-secret", entries)
        assertEquals(HttpStatus.OK, response.statusCode)
    }
}
