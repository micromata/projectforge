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

package org.projectforge.framework.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HtmlSanitizerTest {
    @Test
    fun sanitizeTest() {
        assertEquals("", HtmlSanitizer.sanitize(null))
        assertEquals(
            "<p>Hi <strong>you</strong>, <span style=\"color: #e03e2d\">red</span></p>",
            HtmlSanitizer.sanitize("<p onclick=\"x()\">Hi <strong>you</strong>, <span style=\"color: #e03e2d; font-size: 80px\">red</span></p><script>alert(1)</script>"),
        )
        assertEquals(
            "<a href=\"https://example.org\" target=\"_blank\" rel=\"noopener\">link</a> bad",
            HtmlSanitizer.sanitize("<a href=\"https://example.org\">link</a> <a href=\"javascript:alert(1)\">bad</a>"),
        )
        assertEquals("<span>x</span>", HtmlSanitizer.sanitize("<span style=\"background: url(x)\">x</span>"))
        assertEquals("Line 1<br>\nLine &lt;2&gt;", HtmlSanitizer.sanitize("Line 1\nLine <2>"))
    }

    @Test
    fun pollMailTest() {
        // A poll mail: the paragraphs and the link of the texts stay, what a poll's owner smuggled in is gone.
        assertEquals(
            "<p>Poll \"Lunch\"</p><p>Go <a href=\"https://example.org/poll\" target=\"_blank\" rel=\"noopener\">to the poll</a></p>",
            HtmlSanitizer.sanitize(
                "<p>Poll \"Lunch<img src=\"https://tracker.example/x.gif\">\"</p>" +
                        "<p>Go <a href=\"https://example.org/poll\">to the poll</a></p><form action=\"https://evil\"><input name=\"pw\"></form>"
            ),
        )
    }
}
