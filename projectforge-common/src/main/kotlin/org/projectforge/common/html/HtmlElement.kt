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

open class HtmlElement(
    val tag: String,
    val content: String? = null,
    val childrenAllowed: Boolean = true,
    val id: String? = null
) {
    var children: MutableList<HtmlElement>? = null
    var attributes: MutableMap<String, String>? = null
    var classnames: MutableList<String>? = null

    fun addClasses(vararg cssClass: CssClass?) {
        cssClass.forEach { it?.let { addClassname((it.cls)) } }
    }

    fun addClassname(classname: String) {
        classnames = classnames ?: mutableListOf()
        classnames!!.add(classname)
    }

    fun attr(name: String, value: String) {
        attributes = attributes ?: mutableMapOf()
        attributes!![name] = value
    }

    /**
     * Adds a child to this element.
     * @param child The child to add
     * @return This element for chaining.
     */
    open fun add(child: HtmlElement): HtmlElement {
        if (!childrenAllowed) {
            throw IllegalStateException("Children not allowed for $tag")
        }
        children = children ?: mutableListOf()
        children!!.add(child)
        return this
    }

    open fun append(sb: StringBuilder, indent: Int) {
        if (children.isNullOrEmpty() && content.isNullOrEmpty()) {
            indent(sb, indent)
            sb.append("<$tag")
            buildAttributes(sb)
            sb.appendLine(" />")
            return
        }
        indent(sb, indent)
        sb.append("<$tag")
        buildAttributes(sb)
        if (content == null) {
            sb.appendLine(">")
        } else {
            val multiline = content.contains('\n')
            if (multiline) {
                sb.appendLine(">").appendLine(escape(content))
            } else {
                sb.append(">").append(escape(content))
            }
            if (children.isNullOrEmpty()) {
                if (multiline) {
                    indent(sb, indent)
                }
                sb.appendLine("</$tag>")
                return
            }
            sb.appendLine()
        }
        children?.forEach { it.append(sb, indent + 1) }
        indent(sb, indent)
        sb.appendLine("</$tag>")
    }

    private fun buildAttributes(sb: StringBuilder) {
        attributes?.forEach { (name, value) -> sb.append(" $name=\"$value\"") }
        classnames?.let { sb.append(" class=\"${it.joinToString(separator = " ") { it }}\"") }
        id?.let { sb.append(" id=\"$it\"") }
    }

    companion object {
        private fun indent(sb: StringBuilder, indent: Int) {
            for (i in 0 until indent) {
                sb.append("  ")
            }
        }

        /**
         * Escapes the given [str] to be used in HTML.
         * Only the characters `<`, `>`, `&`, `"`, and `'` are escaped.
         * @param str The string to escape
         * @return The escaped string
         */
        fun escape(str: String?): String {
            str ?: return ""
            val sb = StringBuilder()
            for (char in str) {
                when (char) {
                    '<' -> sb.append("&lt;")
                    '>' -> sb.append("&gt;")
                    '&' -> sb.append("&amp;")
                    '"' -> sb.append("&quot;")
                    '\'' -> sb.append("&apos;")
                    else -> sb.append(char)
                }
            }
            return sb.toString()
        }
    }
}
