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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry

/**
 * The empty-filter guard of the time sheet list ([TimesheetEntityRest.needsMoreFilter]), as in Wicket: a period
 * or a task is required, the user or a search string alone is not enough.
 */
class TimesheetNeedMoreFilterTest {
    @Test
    fun `no period and no task needs more`() {
        assertTrue(TimesheetEntityRest.needsMoreFilter(MagicFilter()))
        assertTrue(TimesheetEntityRest.needsMoreFilter(filter(entry("user") { it.id = 42 })))
        assertTrue(TimesheetEntityRest.needsMoreFilter(MagicFilter(searchString = "invoice")))
        // A period pill without any bound (e.g. cleared by the user) doesn't count.
        assertTrue(TimesheetEntityRest.needsMoreFilter(filter(entry("period") { it.fromValue = " " })))
    }

    @Test
    fun `a period bound or a task is enough`() {
        assertFalse(TimesheetEntityRest.needsMoreFilter(filter(entry("period") { it.fromValue = "2026-10-01" })))
        assertFalse(TimesheetEntityRest.needsMoreFilter(filter(entry("stopTime") { it.toValue = "2026-10-31" })))
        assertFalse(TimesheetEntityRest.needsMoreFilter(filter(entry("startTime") { it.value = "2026-10-01" })))
        assertFalse(TimesheetEntityRest.needsMoreFilter(filter(entry("task") { it.id = 4711 })))
    }

    private fun filter(vararg entries: MagicFilterEntry) = MagicFilter(entries = entries.toMutableList())

    private fun entry(field: String, init: (MagicFilterEntry.Value) -> Unit) =
        MagicFilterEntry(field).also { init(it.value) }
}
