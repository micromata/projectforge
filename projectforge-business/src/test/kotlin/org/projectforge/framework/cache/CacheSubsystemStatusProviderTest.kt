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


package org.projectforge.framework.cache

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.framework.integration.SubsystemState
import org.projectforge.framework.integration.SyncStats

class CacheSubsystemStatusProviderTest {
    @Test
    fun `state follows the worst cache`() {
        Assertions.assertEquals(SubsystemState.UNKNOWN, stateOf(entry(CacheState.NOT_FILLED, everFilled = false)))
        Assertions.assertEquals(SubsystemState.OK, stateOf(entry(CacheState.OK), entry(CacheState.NOT_FILLED, everFilled = false)))
        Assertions.assertEquals(SubsystemState.DEGRADED, stateOf(entry(CacheState.OK), entry(CacheState.FAILED)))
        Assertions.assertEquals(SubsystemState.DOWN, stateOf(entry(CacheState.FAILED), entry(CacheState.STUCK)))
    }

    @Test
    fun `the problem caches are shown as syncs`() {
        val status = CacheSubsystemStatusProvider.statusOf(
            listOf(entry(CacheState.OK, name = "A"), entry(CacheState.FAILED, name = "B"), entry(CacheState.STUCK, name = "C"))
        )
        Assertions.assertEquals(listOf("B", "C"), status.syncs.map { it.type })
        Assertions.assertEquals(SyncStats.Status.ERRORS, status.syncs[0].lastStatus)
        Assertions.assertEquals("error", status.syncs[0].lastError)
        Assertions.assertEquals(SyncStats.Status.ABORTED, status.syncs[1].lastStatus)
    }

    private fun stateOf(vararg entries: CacheEntry) = CacheSubsystemStatusProvider.statusOf(entries.toList()).state

    private fun entry(state: CacheState, everFilled: Boolean = true, name: String = "Cache") = CacheEntry(
        id = name,
        state = state,
        status = CacheStatus(
            name = name,
            qualifiedName = "test.$name",
            refreshInProgress = state == CacheState.STUCK,
            refreshRunningSince = if (state == CacheState.STUCK) 1L else null,
            stuck = state == CacheState.STUCK,
            expired = false,
            everFilled = everFilled,
            expireTimeMs = 60_000,
            lastRefreshStart = 1L,
            lastRefreshEnd = 2L,
            lastRefreshDurationMs = 1L,
            lastSuccess = null,
            lastRefreshError = if (state == CacheState.FAILED) "error" else null,
            refreshCount = 1,
            failureCount = 0,
        ),
        lastRefreshTimeAgo = null,
    )
}
