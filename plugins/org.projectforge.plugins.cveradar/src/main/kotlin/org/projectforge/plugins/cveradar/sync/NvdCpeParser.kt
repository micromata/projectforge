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


package org.projectforge.plugins.cveradar.sync

import com.fasterxml.jackson.databind.JsonNode

/**
 * A vulnerable CPE entry of an NVD document: product (and vendor) with a version, or a version range.
 * [version] is null for all versions (`*` or `-` in the CPE).
 */
data class NvdCpeEntry(
    val part: String,
    val vendor: String,
    val product: String,
    val version: String?,
    val startIncluding: String?,
    val startExcluding: String?,
    val endIncluding: String?,
    val endExcluding: String?,
    /** The CPE as the NVD names it. */
    val criteria: String,
) {
    val hasRange: Boolean
        get() = startIncluding != null || startExcluding != null || endIncluding != null || endExcluding != null
}

/**
 * Reads the vulnerable entries from the `configurations` of an NVD CVE document (MAT-01). The operators of the nodes
 * (an application *and* an operating system) are not evaluated: every vulnerable entry counts for itself. That finds
 * more than there is, never less; a false positive is discarded by hand (MAT-06).
 */
object NvdCpeParser {
    fun parse(cve: JsonNode): List<NvdCpeEntry> {
        val result = LinkedHashSet<NvdCpeEntry>()
        cve.path("configurations").forEach { configuration ->
            configuration.path("nodes").forEach { node -> collect(node, result) }
        }
        return result.toList()
    }

    private fun collect(node: JsonNode, result: MutableSet<NvdCpeEntry>) {
        node.path("cpeMatch").forEach { match ->
            if (!match.path("vulnerable").asBoolean(false)) {
                return@forEach
            }
            val criteria = match.path("criteria").asText("")
            parseCriteria(criteria, match)?.let { result.add(it) }
        }
        // Newer documents nest the nodes.
        node.path("children").forEach { collect(it, result) }
    }

    private fun parseCriteria(criteria: String, match: JsonNode): NvdCpeEntry? {
        val fields = splitCpe(criteria)
        if (fields.size < 6 || fields[0] != "cpe" || fields[1] != "2.3") {
            return null
        }
        val part = fields[2].lowercase()
        val vendor = fields[3].lowercase()
        val product = fields[4].lowercase()
        if (part.length != 1 || vendor.isBlank() || product.isBlank() || vendor == "*" || product == "*") {
            return null
        }
        fun bound(name: String) = match.path(name).asText("").trim().takeIf { it.isNotEmpty() }
        return NvdCpeEntry(
            part = part,
            vendor = vendor.take(255),
            product = product.take(255),
            version = fields[5].takeIf { it != "*" && it != "-" && it.isNotBlank() }?.take(100),
            startIncluding = bound("versionStartIncluding")?.take(100),
            startExcluding = bound("versionStartExcluding")?.take(100),
            endIncluding = bound("versionEndIncluding")?.take(100),
            endExcluding = bound("versionEndExcluding")?.take(100),
            criteria = criteria.take(500),
        )
    }

    /** Splits at the colons that aren't escaped (`\:`), and removes the escapes. */
    internal fun splitCpe(criteria: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < criteria.length) {
            val c = criteria[i]
            when {
                c == '\\' && i + 1 < criteria.length -> {
                    current.append(criteria[i + 1])
                    i++
                }

                c == ':' -> {
                    fields.add(current.toString())
                    current.setLength(0)
                }

                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }
}
