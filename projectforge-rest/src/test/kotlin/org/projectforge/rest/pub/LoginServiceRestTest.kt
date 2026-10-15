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

package org.projectforge.rest.pub

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class LoginServiceRestTest {
    /**
     * Where a login without a requested target sends the user: the calendar, the application's start page,
     * now served by projectforge-next (routed through [NextMigration], see the method's KDoc). Not derived
     * from the request - the requested target is kept by the client, because a login rotates the http session.
     */
    @Test
    fun getRedirectUrlTest() {
        val request = Mockito.mock(HttpServletRequest::class.java)
        Assertions.assertEquals("/next/calendar", LoginServiceRest.getRedirectUrl(request))
    }

    /**
     * After a login the user is sent to this url, so an unchecked one turns the login page into an open
     * redirect. Only relative paths of this application are acceptable.
     */
    @Test
    fun sanitizeRedirectUrlTest() {
        // Foreign hosts:
        assertRejected("http://evil.com")
        assertRejected("https://evil.com/path")
        assertRejected("//evil.com")
        assertRejected("///evil.com")
        // Browsers normalize backslashes to slashes, so these are foreign hosts as well:
        assertRejected("\\\\evil.com")
        assertRejected("/\\evil.com")
        // Other schemes:
        assertRejected("javascript:alert(1)")
        assertRejected("data:text/html,<script>alert(1)</script>")
        assertRejected("mailto:foo@example.com")
        // Schemes hidden behind control characters: a browser strips tab/newline/NUL from inside a url
        // before parsing it, so each of these navigates as `javascript:`.
        assertRejected("ja\tvascript:alert(1)")
        assertRejected("java\nscript:alert(1)")
        assertRejected("javasc\r\nript:alert(1)")
        assertRejected("\u0000javascript:alert(1)")
        // Same trick against the "//" check:
        assertRejected("\t//evil.com")
        assertRejected("\u0000//evil.com")
        // Path traversal: `..` is resolved away by the browser, so these name a foreign host too.
        assertRejected("/next/../..//evil.com")
        assertRejected("/next/..//evil.com")
        assertRejected("/next/..\\..//evil.com")
        assertRejected("/..")
        // Not an absolute path of this application:
        assertRejected("next/book/42")
        // Nothing to redirect to:
        assertRejected(null)
        assertRejected("")
        assertRejected("   ")
        assertRejected("null")

        // The legitimate cases, unchanged:
        assertAccepted("/next/book/42")
        assertAccepted("/react/calendar")
        assertAccepted("/next/book?filter=abc&sort=title")
        // A `..` in a query value is data, not a path segment the browser resolves.
        assertAccepted("/next/book?filter=../x")
    }

    private fun assertRejected(url: String?) {
        Assertions.assertNull(LoginServiceRest.sanitizeRedirectUrl(url), "Should be rejected: '$url'.")
    }

    private fun assertAccepted(url: String) {
        Assertions.assertEquals(url, LoginServiceRest.sanitizeRedirectUrl(url), "Should be accepted: '$url'.")
    }
}
