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
import org.projectforge.NextMigration
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class OrphanedLinkFilterTest {
    private val filter = OrphanedLinkFilter()

    /**
     * The React calendar is a migrated page like any other now: a bookmarked or emailed link to it is bent
     * onto the next calendar. The "classic version" switch reaches it by carrying the escape-hatch marker
     * (see calendar-page.tsx, legacyUrl "react/calendar?legacyEscape"), covered below.
     */
    @Test
    fun `the react calendar is redirected to next`() {
        Assertions.assertEquals("/next/calendar", redirectOf("/react/calendar"))
    }

    /**
     * The escape hatch: a request carrying [NextMigration.ESCAPE_HATCH_PARAM] is let through to the legacy
     * React calendar, which is how the "classic version" switch reaches it without being bounced back.
     */
    @Test
    fun `the react calendar with the escape marker stays in the legacy app`() {
        Assertions.assertNull(
            redirectOf("/react/calendar", NextMigration.ESCAPE_HATCH_PARAM),
            "The classic calendar switch carries the escape marker and must stay in React.",
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

    /** The Wicket global search has moved to projectforge-next; a bookmarked link is bent onto it. */
    @Test
    fun `the old wicket search is redirected to next`() {
        Assertions.assertEquals("/next/search", redirectOf("/wa/search"))
    }

    /**
     * Runs the filter over a GET of [uri] and returns the redirect location it sent, or null if it let the
     * request pass through to the chain untouched. Each of [params] is added as a valueless query parameter,
     * so the escape-hatch marker can be exercised.
     */
    private fun redirectOf(uri: String, vararg params: String): String? {
        val request = MockHttpServletRequest("GET", uri).also { it.requestURI = uri }
        params.forEach { request.addParameter(it, "") }
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, MockFilterChain())
        return response.redirectedUrl
    }
}
