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

package org.projectforge.framework.integration

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SubsystemStatusTest {
    @Test
    fun `state of the syncs`() {
        // Registered, but never run (e.g. the handler isn't the active one): left out.
        SyncStatsRegistry.get("test-status-idle")
        SubsystemStatus.ofSyncs(listOf("test-status-")).let {
            Assertions.assertEquals(SubsystemState.UNKNOWN, it.state)
            Assertions.assertTrue(it.syncs.isEmpty())
        }

        SyncStatsRegistry.get("test-status-a").execute { }
        Assertions.assertEquals(SubsystemState.OK, SubsystemStatus.ofSyncs(listOf("test-status-")).state)

        SyncStatsRegistry.get("test-status-b").execute { run -> run.step("users") { it.errors++ } }
        SubsystemStatus.ofSyncs(listOf("test-status-"), detail = "host").let {
            Assertions.assertEquals(SubsystemState.DEGRADED, it.state, "The worst of the syncs.")
            Assertions.assertEquals("host", it.detail)
            Assertions.assertEquals(listOf("test-status-a", "test-status-b"), it.syncs.map { sync -> sync.type })
            Assertions.assertEquals("1 errors", it.syncs[1].lastError)
        }

        SyncStatsRegistry.get("test-status-c").startRun().abort("unreachable")
        Assertions.assertEquals(SubsystemState.DOWN, SubsystemStatus.ofSyncs(listOf("test-status-")).state)
        Assertions.assertEquals(
            SubsystemState.OK, SubsystemStatus.ofSyncs(listOf("test-status-a")).state, "Only the given types.",
        )
    }

    @Test
    fun hostOf() {
        Assertions.assertEquals("gateway.example.org", SubsystemStatus.hostOf(" https://gateway.example.org:8443/rs "))
        Assertions.assertEquals("no url", SubsystemStatus.hostOf("no url"))
        Assertions.assertNull(SubsystemStatus.hostOf(" "))
        Assertions.assertNull(SubsystemStatus.hostOf(null))
    }

    @Test
    fun `problem match`() {
        val match = SubsystemProblemMatch(listOf("ldap."), listOf("Ldap"), listOf("ldap-"))
        Assertions.assertTrue(match.matches("ldap.bind", null, null))
        Assertions.assertTrue(match.matches("other", "LdapMasterLoginHandler:42", null))
        Assertions.assertTrue(match.matches("other", "Foo:1", "Sync ldap-master errors in 1s"))
        Assertions.assertFalse(match.matches("other", "Foo:1", "Sync idp-master errors in 1s"))
        Assertions.assertFalse(SubsystemProblemMatch().matches("ldap.bind", "Ldap", "Sync ldap-master"))
    }
}
