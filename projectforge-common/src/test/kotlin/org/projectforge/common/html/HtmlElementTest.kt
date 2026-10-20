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


package org.projectforge.common.html

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class HtmlElementTest {
    @Test
    fun attributesAreEscaped() {
        val span = Html.Span("text", style = "color: red\" onmouseover=\"alert(1)", id = "a\"b")
        span.addClassname("x\"><script>")
        val html = StringBuilder().also { span.append(it, 0) }.toString()
        Assertions.assertFalse(html.contains("\" onmouseover"), html)
        Assertions.assertFalse(html.contains("<script>"), html)
        Assertions.assertTrue(html.contains("style=\"color: red&quot; onmouseover=&quot;alert(1)\""), html)
        Assertions.assertTrue(html.contains("id=\"a&quot;b\""), html)
    }

    @Test
    fun hrefAmpersandIsEscaped() {
        val html = StringBuilder().also { Html.A("/page?a=1&b=2", "link").append(it, 0) }.toString()
        Assertions.assertTrue(html.contains("href=\"/page?a=1&amp;b=2\""), html)
    }
}
