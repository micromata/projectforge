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
import org.projectforge.business.task.TaskDO
import org.projectforge.business.timesheet.TimesheetDO

/**
 * What the „only time sheets with JIRA issues" filter keeps: a sheet whose own description or reference
 * names a JIRA key. A plain unit test — [org.projectforge.jira.JiraUtils.hasJiraIssues] is a pure text
 * check, so neither the JIRA configuration nor a database is involved.
 */
class TimesheetJiraFilterTest {
    private val filter = TimesheetPagesRest.TimesheetJiraFilter()

    @Test
    fun `a key in the description is kept`() {
        assertTrue(matches(timesheet(description = "Worked on PROJECTFORGE-222 today")))
    }

    @Test
    fun `a key in the reference is kept`() {
        assertTrue(matches(timesheet(reference = "ACME-17")))
    }

    @Test
    fun `plain text without a key is dropped`() {
        assertFalse(matches(timesheet(description = "Refactored the invoice export", reference = "ticket 42")))
        assertFalse(matches(timesheet()))
    }

    @Test
    fun `a key only in the task is dropped`() {
        // The task's title carries the key, the sheet's own fields don't — the sheet is booked on a task
        // named after a ticket but says nothing about one itself, so it must not match.
        val task = TaskDO().also { it.title = "PROJECTFORGE-222 rework" }
        assertFalse(matches(timesheet(task = task)))
    }

    private fun matches(timesheet: TimesheetDO): Boolean = filter.match(mutableListOf(), timesheet)

    private fun timesheet(
        description: String? = null,
        reference: String? = null,
        task: TaskDO? = null,
    ): TimesheetDO = TimesheetDO().also {
        it.description = description
        it.reference = reference
        it.task = task
    }
}
