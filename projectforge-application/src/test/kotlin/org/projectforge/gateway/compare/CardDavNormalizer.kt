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

package org.projectforge.gateway.compare

/**
 * Makes the CardDAV answers of the main instance and the gateway comparable. Allowed differences are replaced by
 * placeholders:
 * - the base url of the server,
 * - the ids of the addresses in hrefs and photo urls (the gateway has its own ids, the UID is the same),
 * - ETags, CTags and sync tokens (timestamps of each instance, see [CardDavCompare] for their stability check),
 * - REV of the vCards (last update of each instance).
 *
 * The responses of a multistatus are sorted, as the order of the addresses differs.
 */
object CardDavNormalizer {
    private val responseRegex = Regex("<([A-Za-z0-9]+:)?response>.*?</\\1response>", RegexOption.DOT_MATCHES_ALL)
    private val multistatusRegex = Regex("<([A-Za-z0-9]+:)?multistatus\\b")

    fun normalize(body: String, baseUrls: List<String>): String {
        var text = body.replace("\r\n", "\n").replace("&#13;", "")
        baseUrls.filter { it.isNotBlank() }.sortedByDescending { it.length }.forEach { text = text.replace(it, "{base}") }
        text = text.replace(Regex("ProjectForge-\\d+\\.vcf"), "ProjectForge-{id}.vcf")
            .replace(Regex("contact-\\d+\\.(\\w+)"), "contact-{id}.$1")
        for (tag in VOLATILE_TAGS) {
            text = text.replace(Regex("(<([A-Za-z0-9]+:)?$tag>)[^<]*(</([A-Za-z0-9]+:)?$tag>)"), "$1{$tag}$3")
        }
        text = normalizeVCards(text)
        if (multistatusRegex.containsMatchIn(text)) {
            val responses = responseRegex.findAll(text).map { it.value }.toList()
            if (responses.isNotEmpty()) {
                val first = responseRegex.find(text)!!.range.first
                val last = responseRegex.findAll(text).last().range.last
                text = text.substring(0, first) + responses.sorted().joinToString("\n") + text.substring(last + 1)
            }
        }
        return text.replace(Regex(">\\s*<"), ">\n<").lines().map { it.trimEnd() }.filter { it.isNotBlank() }
            .joinToString("\n")
    }

    /**
     * vCards inside address-data or as answer of a GET: unfolded, without REV.
     */
    fun normalizeVCards(text: String): String {
        return Regex("BEGIN:VCARD.*?END:VCARD", RegexOption.DOT_MATCHES_ALL).replace(text) { match ->
            IcsNormalizer.unfold(match.value).filter { it.isNotBlank() && !it.startsWith("REV:") }.joinToString("\n")
        }
    }

    /**
     * @return All values of the given tag (any namespace prefix), e.g. all getetag values of a multistatus.
     */
    fun values(body: String, tag: String): List<String> {
        return Regex("<([A-Za-z0-9]+:)?$tag>([^<]*)</([A-Za-z0-9]+:)?$tag>").findAll(body)
            .map { it.groupValues[2].trim() }.toList()
    }

    /**
     * @return The hrefs of the vCards (.vcf) of a multistatus.
     */
    fun vcfHrefs(body: String): List<String> = values(body, "href").filter { it.endsWith(".vcf") }

    private val VOLATILE_TAGS = listOf("getetag", "getctag", "sync-token")
}
