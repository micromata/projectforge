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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.gateway.sync.GatewayIcsParams

class GatewayIcsParamsTest {
    @Test
    fun normalizeDropsTokenAndExportRemindersAndSorts() {
        assertEquals("teamCals=42", GatewayIcsParams.normalize("token=abc&teamCals=42"))
        // Url of the subscription dialog (CalendarFeedService.getFullUrl), the leading & included.
        assertEquals("teamCals=42", GatewayIcsParams.normalize("token=abc&teamCals=42&exportReminders=false"))
        // Ignored by the main instance, the same ICS with and without.
        assertEquals("teamCals=42", GatewayIcsParams.normalize("token=abc&teamCals=42&exportReminders=true"))
        assertEquals("timesheetUser=5", GatewayIcsParams.normalize("token=abc&timesheetUser=5"))
        assertEquals("holidays=true", GatewayIcsParams.normalize("holidays=true&token=abc"))
        assertEquals("teamCals=1;2;3", GatewayIcsParams.normalize("token=abc&teamCals=1;2;3"))
        assertNull(GatewayIcsParams.normalize("token=abc&garbage"))
    }

    @Test
    fun namesWithoutTokenAndValues() {
        assertEquals(
            "exportReminders,teamCals",
            GatewayIcsParams.names(mapOf("token" to "abc", "teamCals" to "42", "exportReminders" to "true")),
        )
    }
}
