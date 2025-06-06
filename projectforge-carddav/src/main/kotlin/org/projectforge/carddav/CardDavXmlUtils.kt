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

package org.projectforge.carddav

import mu.KotlinLogging
import org.jetbrains.kotlin.utils.addToStdlib.countOccurrencesOf
import org.projectforge.carddav.CardDavUtils.CARD
import org.projectforge.carddav.CardDavUtils.CS
import org.projectforge.carddav.CardDavUtils.D

private val log = KotlinLogging.logger {}

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
     * Determine the used element name of the given tag name.
     * Examples: <d:prop> -> d:prop, <prop> -> prop
     * @param xml The XML string.
     * @param tagName The name of the tag to extract the value from.
     */
    fun getElementName(xml: String, tagName: String): String? {
        // Regex to find the element with or without namespace
        val regex = Regex("<\\s*([a-zA-Z_][a-zA-Z0-9_\\-]*:)?$tagName\\b[^>]*>")
        // Search for the first matching element:
        val match = regex.find(xml)
        return match?.groupValues?.get(0)?.substringAfter('<')?.substringBefore('>')
    }

    fun extractElementValue(xml: String, tagName: String): String? {
        val element = getElementName(xml, tagName)?.removeSuffix("/")?.trim() ?: return null
        val count = xml.countOccurrencesOf("<$element>")
        if (count > 1) {
            log.warn { "Invalid xml request (multiple entries of <$element>...</$element> found, first is used): $xml" }
        }
        if (xml.replace(" />", "/>").contains("<$element/>")) {
            // Empty element
            return null
        }
        return xml.substringAfter("<$element>").substringBefore("</$element>")
    }

    /**
     * Escapes the given input string for XML.
     * @param input The input string.
     * @return The escaped string.
     */
    fun escapeXml(input: String): String {
        val sb = StringBuilder(input.length)
        appendEscapedXml(sb, input)
        return sb.toString()
    }

    /**
     * Escapes the given input string for XML.
     * @param input The input string.
     * @return The escaped string.
     */
    fun appendEscapedXml(sb: StringBuilder, input: String) {
        for (char in input) {
            when (char) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> sb.append(char)
            }
        }
    }

    fun extractContactIds(xml: String): Sequence<Long> {
        return EXTRACT_ADDRESS_ID_REGEX.findAll(xml)
            .map { it.groups.get(1)?.value?.toLong() }.filterNotNull()
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
        sb.appendLine("<d:multistatus $XML_NS>")
    }

    /**
     * Appends the end of a multistatus XML response to the given StringBuilder.
     * @param sb The StringBuilder to append the XML to.
     */
    fun appendMultiStatusEnd(sb: StringBuilder) {
        sb.appendLine("</d:multistatus>")
    }

    const val XML_NS =
        "xmlns:$D=\"DAV:\" xmlns:$CARD=\"urn:ietf:params:xml:ns:carddav\" xmlns:$CS=\"http://calendarserver.org/ns/\" xmlns:me=\"http://me.com/_namespace/\""

    val EXTRACT_ADDRESS_ID_REGEX = """ProjectForge-(\d+)\.vcf""".toRegex()

    val EXTRACT_ADDRESS_ID_PHOTO_REGEX = """${CardDavInit.PHOTO_PATH}(\d+)\.""".toRegex()
}
