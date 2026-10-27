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
 * The sub-task option of the time sheet list ([TimesheetEntityRest.searchSubTasks]): the picked task is searched
 * with its sub-tasks unless "without structure sub elements" is ticked; a legacy `recursive` entry is ignored.
 */
class TimesheetSubTasksFilterTest {
    @Test
    fun `sub-tasks are searched by default`() {
        assertTrue(TimesheetEntityRest.searchSubTasks(MagicFilter()))
        // Unticked: the boolean pill is kept without a value.
        assertTrue(TimesheetEntityRest.searchSubTasks(filter(entry(TimesheetEntityRest.WITHOUT_SUB_TASKS_FILTER, null))))
    }

    @Test
    fun `ticked option searches the task only`() {
        val entry = entry(TimesheetEntityRest.WITHOUT_SUB_TASKS_FILTER, "true")
        assertFalse(TimesheetEntityRest.searchSubTasks(filter(entry)))
        assertTrue(entry.synthetic == true, "Consumed, not an unknown field for the generic processor.")
    }

    @Test
    fun `legacy recursive entry of a saved filter is ignored`() {
        val legacy = entry("recursive", "false")
        assertTrue(TimesheetEntityRest.searchSubTasks(filter(legacy)))
        assertTrue(legacy.synthetic == true, "Consumed, not an unknown field for the generic processor.")
    }

    private fun filter(vararg entries: MagicFilterEntry) = MagicFilter(entries = entries.toMutableList())

    private fun entry(field: String, value: String?) = MagicFilterEntry(field).also { it.value.value = value }
}
