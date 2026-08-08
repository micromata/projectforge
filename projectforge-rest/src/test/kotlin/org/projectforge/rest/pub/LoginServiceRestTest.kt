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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class LoginServiceRestTest {
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
    }

    private fun assertRejected(url: String?) {
        Assertions.assertNull(LoginServiceRest.sanitizeRedirectUrl(url), "Should be rejected: '$url'.")
    }

    private fun assertAccepted(url: String) {
        Assertions.assertEquals(url, LoginServiceRest.sanitizeRedirectUrl(url), "Should be accepted: '$url'.")
    }
}
