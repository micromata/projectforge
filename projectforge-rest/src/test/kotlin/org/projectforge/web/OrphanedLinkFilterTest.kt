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

    /**
     * The old React project list is gone, although the project's legacy app is Wicket: a bookmarked link to
     * it, list or form, is bent onto next. A sibling path isn't caught.
     */
    @Test
    fun `the old react project pages are redirected to next`() {
        Assertions.assertEquals("/next/project", redirectOf("/react/project"))
        Assertions.assertEquals("/next/project", redirectOf("/react/project/"))
        Assertions.assertEquals("/next/project/42", redirectOf("/react/project/edit/42"))
        Assertions.assertEquals("/next/project/new", redirectOf("/react/project/edit"))
        Assertions.assertNull(redirectOf("/react/projectXyz"))
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

    /** The Wicket feedback page has moved to projectforge-next; a bookmarked or emailed link is bent onto it. */
    @Test
    fun `the old wicket feedback page is redirected to next`() {
        Assertions.assertEquals("/next/feedback", redirectOf("/wa/feedback"))
    }

    /** The Wicket monthly employee report has moved to projectforge-next; a bookmarked link is bent onto it. */
    @Test
    fun `the old wicket monthly employee report is redirected to next`() {
        Assertions.assertEquals(
            "/next/monthlyEmployeeReport",
            redirectOf("/wa/monthlyEmployeeReport"),
        )
    }

    /** The Wicket report was removed, so the escape marker no longer lets a request through to it. */
    @Test
    fun `the old wicket monthly employee report is redirected to next even with the escape marker`() {
        Assertions.assertEquals(
            "/next/monthlyEmployeeReport",
            redirectOf("/wa/monthlyEmployeeReport", NextMigration.ESCAPE_HATCH_PARAM),
        )
    }

    /** The Wicket page of the IHK plugin has moved to projectforge-next; there is no way back to it. */
    @Test
    fun `the old wicket IHK page is redirected to next`() {
        val uri = "/wa/wicket/bookmarkable/org.projectforge.plugins.ihk.IHKPage"
        Assertions.assertEquals("/next/ihk", redirectOf(uri))
        Assertions.assertEquals("/next/ihk", redirectOf(uri, NextMigration.ESCAPE_HATCH_PARAM))
    }

    /** The Wicket HR view has moved to projectforge-next; a bookmarked link is bent onto it, a sibling isn't. */
    @Test
    fun `the old wicket HR view is redirected to next`() {
        Assertions.assertEquals("/next/hrList", redirectOf("/wa/hrList"))
        Assertions.assertEquals("/next/hrList", redirectOf("/wa/hrList", NextMigration.ESCAPE_HATCH_PARAM))
        Assertions.assertNull(redirectOf("/wa/hrListXyz"))
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
