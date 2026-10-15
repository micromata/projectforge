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
import org.projectforge.gateway.sync.GatewayIcsCache
import org.projectforge.gateway.sync.dto.SyncIcsEntryDto

class GatewaySyncServiceTest {

    @Test
    fun syncIcsEntriesPutsIntoCache() {
        val icsCache = GatewayIcsCache()
        val entries = listOf(
            SyncIcsEntryDto(userId = 1L, queryParam = "q1", icsData = "ics-content-1"),
            SyncIcsEntryDto(userId = 2L, queryParam = "q2", icsData = "ics-content-2"),
        )

        for (entry in entries) {
            icsCache.put(entry.userId, entry.queryParam, entry.icsData)
        }

        assertEquals("ics-content-1", icsCache.get(1L, "q1"))
        assertEquals("ics-content-2", icsCache.get(2L, "q2"))
        assertNull(icsCache.get(1L, "nonexistent"))
    }
}
