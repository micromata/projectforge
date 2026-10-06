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

package org.projectforge.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

class UISettingsRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var uiSettingsRest: UISettingsRest

    @Test
    fun `dashboard layout round trip and normalization`() {
        logon(TEST_USER)
        assertTrue(uiSettingsRest.getDashboard("test.dashboard").body!!.tiles.isEmpty())

        val stored = uiSettingsRest.setDashboard(
            "test.dashboard",
            DashboardLayout(
                mutableListOf(
                    DashboardTileLayout("b", "half", "L", false),
                    DashboardTileLayout("a", "huge", "XXL", true),
                    DashboardTileLayout("b", "full", "S", false), // duplicate, dropped
                    DashboardTileLayout("bad id!", "half", "S", false), // invalid id, dropped
                )
            )
        ).body!!
        assertEquals(listOf("b", "a"), stored.tiles.map { it.id })
        assertEquals(listOf("half", null), stored.tiles.map { it.width })
        assertEquals(listOf("L", null), stored.tiles.map { it.height })
        assertEquals(listOf(false, true), stored.tiles.map { it.hidden })

        val read = uiSettingsRest.getDashboard("test.dashboard").body!!
        assertEquals(listOf("b", "a"), read.tiles.map { it.id })
        assertEquals("half", read.tiles[0].width)
        assertEquals(true, read.tiles[1].hidden)

        // Resetting stores an empty layout.
        uiSettingsRest.setDashboard("test.dashboard", DashboardLayout())
        assertTrue(uiSettingsRest.getDashboard("test.dashboard").body!!.tiles.isEmpty())

        // At most MAX_TILES tiles are kept.
        val many = DashboardLayout((1..80).map { DashboardTileLayout("t$it") }.toMutableList())
        assertEquals(UISettingsRest.MAX_TILES, uiSettingsRest.setDashboard("test.dashboard", many).body!!.tiles.size)
    }

    @Test
    fun `invalid dashboard id is rejected`() {
        logon(TEST_USER)
        assertEquals(HttpStatus.BAD_REQUEST, uiSettingsRest.getDashboard("../x").statusCode)
        assertEquals(HttpStatus.BAD_REQUEST, uiSettingsRest.setDashboard("a".repeat(65), DashboardLayout()).statusCode)
    }
}
