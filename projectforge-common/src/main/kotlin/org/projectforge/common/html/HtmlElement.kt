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

/**
 * Represents an HTML element.
 * @param tag The tag of the element
 * @param content The content of the element ('\n' will be replaced by '<br />').
 * @param childrenAllowed Whether children are allowed for this element
 * @param id The id of the element
 * @param replaceNewlinesByBr Whether newlines in the content should be replaced by '<br />'
 */
open class HtmlElement(
    val tag: String,
    content: String? = null,
    val childrenAllowed: Boolean = true,
    val id: String? = null,
    replaceNewlinesByBr: Boolean = false,
) {
    val content: String? = appendContent(this, content, replaceNewlinesByBr)
    var children: MutableList<HtmlElement>? = null
    var attributes: MutableMap<String, String>? = null
    var classnames: MutableList<String>? = null

    /**
     * Adds the given CSS classes to this element.
     * @param cssClasses The CSS classes to add
     * @return This element for chaining.
     */
    fun add(vararg cssClasses: CssClass?): HtmlElement {
        cssClasses.forEach { it?.let { addClassname((it.cls)) } }
        return this
    }

    /**
     * Adds the given CSS classes to this element.
     * @param classname The CSS classes to add
     * @return This element for chaining.
     */
    fun addClassname(classname: String): HtmlElement {
        classnames = classnames ?: mutableListOf()
        classnames!!.add(classname)
        return this
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

    /**
     * Adds a text element to this element. Convenience method for adding text to an element.
     */
    open fun add(
        text: String,
        bold: Boolean = false,
        replaceNewlinesByBr: Boolean = true,
        vararg cssClasses: CssClass
    ): HtmlElement {
        if (bold || cssClasses.isNotEmpty()) {
            add(Html.Span(text, replaceNewlinesByBr = replaceNewlinesByBr).also { span ->
                span.add(cssClasses = cssClasses)
                span.add(CssClass.BOLD)
            })
        } else {
            appendContent(this, text, replaceNewlinesByBr)?.let { content ->
                add(Html.Text(content, replaceNewlinesByBr = false))
            }
        }
        return this
    }

    open fun append(sb: StringBuilder, indent: Int) {
        if (this is Html.Text) {
            // Special case for text elements: Just append the content
            if (!content.isNullOrBlank()) {
                indent(sb, indent)
                sb.append(escape(content.trim()))
                children?.forEach { it.append(sb, indent + 1) }
                sb.append("\n")
            }
            return
        }
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
         * Appends the given [content] to the [parent] element.
         * If [replaceNewlinesByBr] is true and the content contains newlines, the content will be split by newlines and
         * each part will be added as a text element with a '<br />' element in between.
         * @param parent The parent element to append the content to
         * @param content The content to append
         * @param replaceNewlinesByBr Whether to replace newlines by '<br />' elements
         * @return The content if it was not added to the parent, otherwise null.
         */
        internal fun appendContent(parent: HtmlElement, content: String?, replaceNewlinesByBr: Boolean): String? {
            return if (content != null && replaceNewlinesByBr && content.contains('\n')) {
                // Example: "Hello\nWorld" -> "Hello\n<br />\nWorld"
                content.split('\n').forEachIndexed { index, s ->
                    if (index > 0) {
                        parent.add(Html.BR())
                    }
                    if (s.isNotBlank()) {
                        parent.add(Html.Text("$s\n", replaceNewlinesByBr = false))
                    }
                }
                null
            } else {
                content
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
