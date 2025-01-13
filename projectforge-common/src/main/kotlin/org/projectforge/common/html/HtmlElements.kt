/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

class Div(content: String, style: String? = null, id: String? = null) :
    HtmlElement("div", content = content, id = id) {
    init {
        style?.let { attr("style", it) }
    }
}

class Alert(type: Type, content: String? = null, id: String? = null) : HtmlElement("div", content = content, id = id) {
    enum class Type(val cls: String) {
        INFO("alert-info"),
        SUCCESS("alert-success"),
        WARNING("alert-warning"),
        DANGER("alert-danger")
    }

    init {
        attr("class", "alert ${type.cls}")
    }
}

class P(content: String, style: String? = null, id: String? = null) : HtmlElement("p", content = content, id = id) {
    init {
        style?.let { attr("style", it) }
    }
}

class Span(content: String, style: String? = null, id: String? = null) : HtmlElement("span", content = content, id = id) {
    init {
        style?.let { attr("style", it) }
    }
}

class BR() : HtmlElement("br", childrenAllowed = false)

class H1(content: String, id: String? = null) : HtmlElement("h1", content = content, id = id)
class H2(content: String, id: String? = null) : HtmlElement("h2", content = content, id = id)
class H3(content: String, id: String? = null) : HtmlElement("h3", content = content, id = id)
class H4(content: String, id: String? = null) : HtmlElement("h4", content = content, id = id)

class A(href: String, content: String) : HtmlElement("a", content = content) {
    init {
        attr("href", href)
    }
}
