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

package org.projectforge.web

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class OrphanedLinkFilterTest {
    private val filter = OrphanedLinkFilter()

    /**
     * The React calendar is the "classic version" the calendar switch leads to, and it owns its whole
     * subtree - the nested timesheet and team event editors open under react/calendar/... . None of it may
     * be bent back to next, or the user would be bounced straight out of the app they just switched into.
     */
    @Test
    fun `the whole react calendar subtree is left in the legacy app`() {
        Assertions.assertNull(redirectOf("/react/calendar"), "The classic calendar switch must stay in React.")
        Assertions.assertNull(
            redirectOf("/react/calendar/teamEvent/edit/42"),
            "The React calendar's nested team event editor must stay in React.",
        )
        Assertions.assertNull(
            redirectOf("/react/calendar/timesheet/edit"),
            "The React calendar's nested timesheet editor must stay in React.",
        )
    }

    /**
     * The exemption is scoped to the calendar subtree: another migrated React page's bookmarked link is
     * still bent onto next (the group page, migrated from the React app).
     */
    @Test
    fun `a migrated react page outside the calendar is still redirected`() {
        Assertions.assertEquals("/next/group", redirectOf("/react/group"))
    }

    /** Old Wicket calendars, bookmarked by some users, still lead to the next calendar. */
    @Test
    fun `the old wicket calendar is redirected to next`() {
        Assertions.assertEquals("/next/calendar", redirectOf("/wa/calendar"))
    }

    /**
     * Runs the filter over a GET of [uri] and returns the redirect location it sent, or null if it let the
     * request pass through to the chain untouched.
     */
    private fun redirectOf(uri: String): String? {
        val request = MockHttpServletRequest("GET", uri).also { it.requestURI = uri }
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, MockFilterChain())
        return response.redirectedUrl
    }
}
