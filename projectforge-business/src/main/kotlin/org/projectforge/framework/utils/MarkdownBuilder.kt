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

import org.projectforge.framework.i18n.translate

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class MarkdownBuilder {
    private val sb = StringBuilder()

    enum class Color(val color: String) { BLACK("black"), BLUE("blue"), RED("red"), GREEN("green"), ORANGE("orange") }

    fun h3(text: String, color: Color? = null): MarkdownBuilder {
        sb.append("### ")
        appendLine(text, color)
        sb.appendLine()
        return this
    }

    fun emptyLine(): MarkdownBuilder {
        first = true
        sb.appendLine()
        return this
    }

    fun append(text: String?, color: Color? = null, bold: Boolean? = null, escape: Boolean = true): MarkdownBuilder {
        if (text.isNullOrBlank()) {
            sb.append(text ?: "")
            return this
        }
        if (color == null) {
            val escapedText = if (escape) escapeMarkdown(text) else text
            if (bold == true) {
                sb.append("**").append(escapedText).append("**")
            } else {
                sb.append(escapedText)
            }
        } else {
            val escapedText = if (escape) escapeHtml(text) else text
            sb.append("<span style=\"color:${color.color};")
            if (bold == true) {
                sb.append(" font-weight: bold;")
            }
            sb.append("\">").append(escapedText).append("</span>")
        }
        return this
    }

    /**
     * Escapes markdown special characters to prevent them from being interpreted as formatting.
     */
    private fun escapeMarkdown(text: String): String {
        return text
            .replace("\\", "\\\\")  // Backslash must be first!
            .replace("*", "\\*")
            .replace("_", "\\_")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("#", "\\#")
            .replace("+", "\\+")
            .replace("-", "\\-")
            .replace("`", "\\`")
            .replace("|", "\\|")
    }

    /**
     * Escapes HTML special characters to prevent XSS attacks.
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")   // Ampersand must be first!
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }

    fun appendListItem(text: String?, color: Color? = null): MarkdownBuilder {
        sb.append("- ")
        return appendLine(text, color)
    }

    fun appendLine(
        text: String? = null,
        color: Color? = null,
        bold: Boolean? = null,
        escape: Boolean = true
    ): MarkdownBuilder {
        first = true
        append(text, color, bold = bold, escape = escape)
        if (escape) {
            sb.appendLine("</br>")
        } else {
            sb.appendLine()
        }
        return this
    }

    /**
     * Appends text with newlines, converting \n to markdown line breaks (</br>).
     * Each line will be escaped and appended via appendLine().
     *
     * @param text Text with newlines to append
     * @param color Optional color for the entire text block
     * @param bold Optional bold formatting for the entire text block
     * @return this for chaining
     */
    fun appendMultilineText(text: String?, color: Color? = null, bold: Boolean? = null): MarkdownBuilder {
        if (text.isNullOrBlank()) {
            return this
        }
        text.split("\n").forEach { line ->
            if (line.isNotBlank()) {
                appendLine(line.trim(), color, bold)
            }
        }
        return this
    }

    /**
     * @return this for chaining.
     */
    fun beginTable(numberOfCols: Int): MarkdownBuilder {
        return beginTable(*Array(numberOfCols) { "" })
    }

    /**
     * @return this for chaining.
     */
    fun beginTable(vararg header: String?): MarkdownBuilder {
        first = true
        row(*header)
        sb.append("|")
        header.forEach { sb.append(" ---").append(" |") }
        sb.appendLine()
        return this
    }

    fun row(vararg cell: String?): MarkdownBuilder {
        beginRow()
        cell.forEach {
            cell(it)
        }
        endRow()
        return this
    }

    fun beginRow() : MarkdownBuilder {
        first = true
        sb.append("| ")
        return this
    }

    fun cell(cell: String?, color: Color? = null, bold: Boolean? = null, escape: Boolean = true): MarkdownBuilder {
        ensureSeparator()
        append(cell, color = color, bold = bold, escape = escape)
        return this
    }

    fun endRow() : MarkdownBuilder {
        first = true
        sb.appendLine(" |")
        return this
    }

    fun endTable(): MarkdownBuilder {
        first = true
        sb.appendLine()
        return this
    }

    private var first = true

    @JvmOverloads
    fun appendPipedValue(i18nKey: String, value: String, color: Color? = null, totalValue: String? = null) {
        ensureSeparator()

        val text = buildString {
            append(translate(i18nKey))
            append(": ")
            append(value)
            if (!totalValue.isNullOrBlank()) {
                append("/")
                append(totalValue)
            }
        }

        append(text, color)
    }

    private fun ensureSeparator() {
        if (first) {
            first = false
        } else {
            sb.append(" | ")
        }
    }

    override fun toString(): String {
        return sb.toString()
    }
}
