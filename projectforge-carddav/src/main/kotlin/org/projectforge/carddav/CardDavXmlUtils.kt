/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.carddav

internal object CardDavXmlUtils {
    /**
     * Extracts the root element from the given XML string. For <d:multistatus> responses, this would be "multistatus".
     * @param xml The XML string.
     * @return The root element or null if no root element was found.
     */
    fun getRootElement(xml: String): String? {
        val regex = Regex("<\\s*([a-zA-Z_][a-zA-Z0-9_\\-]*:)?([a-zA-Z_][a-zA-Z0-9_\\-]*)[^>]*>")
        val match = regex.find(xml)
        return match?.groups?.get(2)?.value
    }

    /**
     * Appends the XML prefix to the given StringBuilder. Every xml response should start with this prefix.
     * @param sb The StringBuilder to append the XML prefix to.
     */
    fun appendXmlPrefix(sb: StringBuilder) {
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    }

    /**
     * Appends the start of a multistatus XML response to the given StringBuilder.
     * @param sb The StringBuilder to append the XML to.
     * @param prependXmlPrefix Whether to prepend the XML prefix to the response. For convenience, this is true by default and
     * includes call of [appendXmlPrefix].
     */
    fun appendMultiStatusStart(sb: StringBuilder, prependXmlPrefix: Boolean = true) {
        if (prependXmlPrefix) {
            appendXmlPrefix(sb)
        }
        sb.appendLine("<multistatus $XML_NS>")
    }

    /**
     * Appends the end of a multistatus XML response to the given StringBuilder.
     * @param sb The StringBuilder to append the XML to.
     */
    fun appendMultiStatusEnd(sb: StringBuilder) {
        sb.appendLine("</multistatus>")
    }

    /**
     * Appends the start of a response to a propfind request to the given StringBuilder.
     */
    fun appendPropLines(sb: StringBuilder, vararg lines: String) {
        appendPropLines(sb, 4, lines = lines)
    }

    /**
     * Appends the start of a response to a propfind request to the given StringBuilder.
     */
    fun appendPropLines(sb: StringBuilder, indentDepth: Int = 8, vararg lines: String) {
        val indentStr = "  ".repeat(2 * indentDepth)
        lines.forEach { line ->
            sb.appendLine("$indentStr$line")
        }
    }

    const val XML_NS =
        "xmlns:d=\"DAV:\" xmlns:cr=\"urn:ietf:params:xml:ns:carddav\" xmlns:cs=\"http://calendarserver.org/ns/\""
}
