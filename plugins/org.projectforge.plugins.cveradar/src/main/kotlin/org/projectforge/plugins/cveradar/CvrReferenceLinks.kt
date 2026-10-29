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

package org.projectforge.plugins.cveradar

import java.net.URI

/**
 * The own references of a case (PRIO-05): one `https://… label` per line. Untrusted input: only http(s) links with a
 * host are kept, the rest of a line is the label (text, rendered escaped).
 */
object CvrReferenceLinks {
    const val MAX_LINKS = 50
    const val MAX_URL_LENGTH = 500
    const val MAX_LABEL_LENGTH = 200

    data class Link(val url: String, val label: String?)

    fun parse(text: String?): List<Link> {
        return text?.lines()?.mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                return@mapNotNull null
            }
            val url = trimmed.substringBefore(' ').substringBefore('\t')
            if (url.length > MAX_URL_LENGTH || !isWebLink(url)) {
                return@mapNotNull null
            }
            Link(url, trimmed.removePrefix(url).trim().take(MAX_LABEL_LENGTH).ifEmpty { null })
        }?.take(MAX_LINKS) ?: emptyList()
    }

    /** The text with the valid lines only, null if none is left. */
    fun normalize(text: String?): String? {
        return parse(text).joinToString("\n") { link -> link.label?.let { "${link.url} $it" } ?: link.url }
            .takeIf { it.isNotEmpty() }
    }

    private fun isWebLink(url: String): Boolean {
        return try {
            val uri = URI(url)
            (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
    }
}
