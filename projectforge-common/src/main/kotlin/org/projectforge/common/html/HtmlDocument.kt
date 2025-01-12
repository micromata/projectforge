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

class HtmlDocument(title: String, val lang: String = "en") : HtmlElement("html") {
    class Head(title: String) : HtmlElement("head") {
        init {
            add(Meta("charset" to "UTF-8"))
            add(Meta("name" to "viewport", "content" to "width=device-width, initial-scale=1.0"))
            add(HtmlElement("title", content = title))
            add(HtmlElement("style", content = style))
        }
    }

    class Meta(vararg attrs: Pair<String, String>) : HtmlElement("meta") {
        init {
            attrs.forEach { attr ->
                attr(attr.first, attr.second)
            }
        }
    }

    val head = Head(title)
    val body = HtmlElement("body")

    init {
        attr("lang", lang)
        super.add(head)
        super.add(body)
    }

    override fun add(child: HtmlElement): HtmlDocument {
        body.add(child)
        return this
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE html>")
        super.append(sb, 0)
        return sb.toString()
    }

    companion object {
        private val style = """
            |      body {
            |          font-family: Arial, sans-serif;
            |          font-size: 11px;
            |      }
            |      h1, h2, h3, h4 {
            |          margin-top: 1.0em;
            |          margin-bottom: 0.5em;
            |      }
            |      h1 {
            |          font-size: 2rem;
            |          font-weight: bold;
            |      }
            |      h2 {
            |          font-size: 1.5rem;
            |          font-weight: semi-bold;
            |      }
            |      h3 {
            |          font-size: 1.2rem;
            |          font-weight: semi-bold;
            |      }
            |      h4 {
            |          font-size: 1rem;
            |          font-weight: semi-bold;
            |      }
            |      .alert {
            |          padding: 15px;
            |          margin-bottom: 20px;
            |          border-radius: 5px;
            |      }
            |      .${Alert.Type.INFO.cls} {
            |          background-color: #d9edf7;
            |          color: #31708f;
            |          border: 2px solid #31708f;
            |      }
            |      .${Alert.Type.DANGER.cls} {
            |          background-color: #f8d7da;
            |          color: #842029;
            |          border: 2px solid #842029;
            |      }
            |      .${Alert.Type.WARNING.cls} {
            |          background-color: #fff3cd;
            |          color: #856404;
            |          border: 2px solid #ffca2c;
            |      }
            |      .${Alert.Type.SUCCESS.cls} {
            |          background-color: #d4edda;
            |          color: #155724;
            |          border: 2px solid #155724;
            |      }
            |      table {
            |          width: 100%;
            |          border-collapse: collapse;
            |      }
            |      th, td {
            |          border: 1px solid #ccc;
            |          padding: 8px;
            |          text-align: left;
            |      }
            |      thead {
            |          background-color: #f2f2f2;
            |      }
            |      .${CssClass.ERROR.cls} {
            |          color: #842029;
            |      }
            |      .${CssClass.WARNING.cls} {
            |          color: #856404;
            |      }
            |      .${CssClass.BOLD.cls} {
            |          font-weight: bold;
            |      }
            |      .${CssClass.ERROR.cls} a {
            |          color: #842029;
            |      }
            |      .${CssClass.WARNING.cls} a {
            |          color: #856404;
            |      }
            |      .${CssClass.BOLD.cls} a {
            |          font-weight: bold;
            |      }
            |      .${CssClass.FIXED_WIDTH_NO_WRAP.cls} {
            |          width: 1px;
            |          white-space: nowrap;
            |      }
            |      .${CssClass.EXPAND.cls} {
            |          width: 100%;
            |      }
        """.trimMargin()
    }
}
