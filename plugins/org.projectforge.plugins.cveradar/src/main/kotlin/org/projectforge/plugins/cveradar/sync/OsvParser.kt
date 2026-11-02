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
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Affected versions of a package: from [introduced] (null: all before) to [fixed] (excluded) or [lastAffected]. */
data class OsvRange(val introduced: String?, val fixed: String?, val lastAffected: String?)

/**
 * The affected versions of a package in an advisory: [packageName] in the spelling of [OsvPackages.key]. The [ranges]
 * count; only without ranges the listed [versions].
 */
data class OsvAffected(
    val ecosystem: String,
    val packageName: String,
    val shortName: String?,
    val ranges: List<OsvRange>,
    val versions: List<String>,
)

/** An advisory of OSV.dev, parsed by [OsvParser]. */
data class OsvAdvisory(
    val id: String,
    val modified: Instant,
    val published: Instant?,
    val withdrawn: Instant?,
    val aliases: List<String>,
    val summary: String?,
    val details: String?,
    /** LOW, MEDIUM, HIGH or CRITICAL; null if the source doesn't rate it. */
    val severity: String?,
    val cvss3Vector: String?,
    val cvss4Vector: String?,
    val cwes: List<String>,
    /** Of the ecosystems imported only. */
    val affected: List<OsvAffected>,
    val rawJson: String,
) {
    val malicious: Boolean
        get() = id.startsWith("MAL-")

    /**
     * The id of its vulnerability: the CVE id (the lowest, if it names several), otherwise the GHSA id, otherwise its
     * own id; upper case. The advisories of one CVE (a GHSA and a PYSEC) agree on it.
     */
    val vulnerabilityKey: String
        get() {
            val ids = listOf(id) + aliases
            return (ids.filter { CVE_ID.matches(it) }.minWithOrNull(CVE_ORDER) ?: ids.firstOrNull { it.startsWith("GHSA-") } ?: id)
                .uppercase().take(30)
        }

    companion object {
        private val CVE_ID = Regex("""CVE-\d{4}-\d{4,}""")

        /** By year and number, not as text (`CVE-2021-9999` is lower than `CVE-2021-10000`). */
        private val CVE_ORDER = compareBy<String>({ it.substring(4, 8).toInt() }, { it.substring(9).toLong() })
    }
}

/**
 * Parses the advisories of OSV.dev (schema https://ossf.github.io/osv-schema/). The document is untrusted input:
 * unknown or missing fields are ignored, values are cut to the lengths of the database, the number of versions is
 * limited. Ranges of type `GIT` (commits) are skipped, `limit` counts like `fixed`.
 */
object OsvParser {
    private val objectMapper = ObjectMapper()

    /** Versions listed per package at most (advisories without ranges). */
    const val MAX_VERSIONS = 1000

    /**
     * @param ecosystems The OSV keys of the ecosystems imported (e.g. `Maven`); the affected packages of others are
     * dropped.
     * @return null, if it has no id or no modification time.
     */
    fun parse(json: String, ecosystems: Set<String>): OsvAdvisory? {
        val root = objectMapper.readTree(json)
        val id = root.textOrNull("id")?.trim()?.takeIf { ID.matches(it) } ?: return null
        val modified = parseTimestamp(root.textOrNull("modified")) ?: return null
        val severities = root.path("severity").filter { it.isObject }
        val ghsaSeverity = root.path("database_specific").textOrNull("severity")?.uppercase()
        return OsvAdvisory(
            id = id,
            modified = modified,
            published = parseTimestamp(root.textOrNull("published")),
            withdrawn = parseTimestamp(root.textOrNull("withdrawn")),
            aliases = root.path("aliases").mapNotNull { it.takeIf { a -> a.isTextual }?.asText()?.trim() }
                .filter { ID.matches(it) }.distinct().take(MAX_ALIASES),
            summary = root.textOrNull("summary")?.trim()?.take(1000),
            details = root.textOrNull("details")?.trim(),
            severity = if (ghsaSeverity == "MODERATE") "MEDIUM" else ghsaSeverity?.takeIf { it in SEVERITIES },
            cvss3Vector = severities.firstOrNull { it.textOrNull("type") == "CVSS_V3" }?.textOrNull("score")?.take(255),
            cvss4Vector = severities.firstOrNull { it.textOrNull("type") == "CVSS_V4" }?.textOrNull("score")?.take(255),
            cwes = root.path("database_specific").path("cwe_ids").mapNotNull { it.takeIf { c -> c.isTextual }?.asText() }
                .filter { CWE.matches(it) }.distinct(),
            affected = root.path("affected").mapNotNull { parseAffected(it, ecosystems) },
            rawJson = json,
        )
    }

    private fun parseAffected(node: JsonNode, ecosystems: Set<String>): OsvAffected? {
        val pkg = node.path("package")
        val ecosystem = pkg.textOrNull("ecosystem")?.takeIf { it in ecosystems } ?: return null
        val name = pkg.textOrNull("name")?.trim()?.takeIf { it.isNotEmpty() && it.length <= 500 } ?: return null
        val ranges = node.path("ranges").filter { it.textOrNull("type") in VERSION_RANGES }.flatMap { ranges(it.path("events")) }
        val versions = if (ranges.isNotEmpty()) emptyList()
        else node.path("versions").mapNotNull { v -> v.takeIf { it.isTextual }?.asText()?.trim()?.takeIf { it.isNotEmpty() }?.take(100) }
            .distinct().take(MAX_VERSIONS)
        if (ranges.isEmpty() && versions.isEmpty()) return null
        return OsvAffected(ecosystem, OsvPackages.key(ecosystem, name), OsvPackages.shortName(ecosystem, name), ranges, versions)
    }

    /** The events in their order: `introduced` opens a range, `fixed`, `last_affected` or `limit` closes it. */
    internal fun ranges(events: JsonNode): List<OsvRange> {
        val result = mutableListOf<OsvRange>()
        var open: String? = null
        var isOpen = false
        events.forEach { event ->
            event.textOrNull("introduced")?.let {
                if (isOpen) result.add(OsvRange(open, null, null)) // Two in a row: the first one is open-ended.
                open = it.trim().take(100).takeIf { v -> v != "0" && v.isNotEmpty() }
                isOpen = true
                return@forEach
            }
            val fixed = (event.textOrNull("fixed") ?: event.textOrNull("limit"))?.trim()?.take(100)
            val lastAffected = event.textOrNull("last_affected")?.trim()?.take(100)
            if (fixed != null || lastAffected != null) {
                result.add(OsvRange(open, fixed, if (fixed == null) lastAffected else null))
                open = null
                isOpen = false
            }
        }
        if (isOpen) result.add(OsvRange(open, null, null))
        return result.distinct()
    }

    /** OSV writes RFC 3339 timestamps in UTC (`2026-10-31T10:55:53.653622012Z`). */
    internal fun parseTimestamp(value: String?): Instant? {
        value ?: return null
        return try {
            OffsetDateTime.parse(value.trim()).toInstant()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun JsonNode.textOrNull(field: String): String? = path(field).takeIf { it.isTextual }?.asText()

    private const val MAX_ALIASES = 50
    private val VERSION_RANGES = setOf("ECOSYSTEM", "SEMVER")
    private val SEVERITIES = setOf("LOW", "MEDIUM", "HIGH", "CRITICAL")
    private val ID = Regex("""[A-Za-z][A-Za-z0-9]*-[A-Za-z0-9._:\-]{1,90}""")
    private val CWE = Regex("""CWE-\d+""")
}
