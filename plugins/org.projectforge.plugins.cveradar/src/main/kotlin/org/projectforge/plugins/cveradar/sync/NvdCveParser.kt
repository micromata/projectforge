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
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * Parses the answers of the NVD API 2.0 (`/rest/json/cves/2.0`). The NVD document is untrusted input: unknown or
 * missing fields are ignored, values are cut to the lengths of the database.
 */
object NvdCveParser {
    private val objectMapper = ObjectMapper()

    fun parsePage(json: String): NvdPage = parsePage(objectMapper.readTree(json))

    private fun parsePage(root: JsonNode): NvdPage {
        val cves = root.path("vulnerabilities").mapNotNull { parseCve(it.path("cve")) }
        return NvdPage(
            totalResults = root.path("totalResults").asInt(cves.size),
            startIndex = root.path("startIndex").asInt(0),
            cves = cves,
        )
    }

    /** @return null, if the node has no CVE id. */
    fun parseCve(cve: JsonNode): NvdCve? {
        val id = cve.path("id").asText("").trim()
        if (!CVE_ID_REGEX.matches(id)) {
            return null
        }
        val metrics = cve.path("metrics")
        return NvdCve(
            cveId = id,
            status = cve.textOrNull("vulnStatus")?.take(50),
            published = parseTimestamp(cve.textOrNull("published")),
            lastModified = parseTimestamp(cve.textOrNull("lastModified")),
            description = cve.path("descriptions").firstOrNull { it.textOrNull("lang") == "en" }
                ?.textOrNull("value"),
            cvss3 = parseCvss(metrics.path("cvssMetricV31")) ?: parseCvss(metrics.path("cvssMetricV30")),
            cvss4 = parseCvss(metrics.path("cvssMetricV40")),
            cwes = cve.path("weaknesses")
                .flatMap { weakness -> weakness.path("description").mapNotNull { it.textOrNull("value") } }
                .filter { CWE_REGEX.matches(it) } // Not NVD-CWE-noinfo, NVD-CWE-Other.
                .distinct(),
            rawJson = objectMapper.writeValueAsString(cve),
        )
    }

    /** The rating of the NVD (type *Primary*), otherwise the first one (of the CNA, *Secondary*). */
    private fun parseCvss(metrics: JsonNode): NvdCvss? {
        val metric = metrics.firstOrNull { it.textOrNull("type") == "Primary" } ?: metrics.firstOrNull() ?: return null
        val data = metric.path("cvssData")
        val score = data.path("baseScore").takeIf { it.isNumber }?.decimalValue() ?: return null
        if (score < BigDecimal.ZERO || score > BigDecimal.TEN) {
            return null
        }
        return NvdCvss(
            score = score.setScale(1, RoundingMode.HALF_UP),
            severity = (data.textOrNull("baseSeverity") ?: metric.textOrNull("baseSeverity"))?.uppercase()?.take(20),
            vector = data.textOrNull("vectorString")?.take(255),
            primary = metric.textOrNull("type") == "Primary",
        )
    }

    /** The NVD writes UTC timestamps without zone (2026-10-01T12:15:07.123), the API accepts them with one. */
    internal fun parseTimestamp(value: String?): Instant? {
        value ?: return null
        return try {
            LocalDateTime.parse(value).toInstant(ZoneOffset.UTC)
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(value).toInstant()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private fun JsonNode.textOrNull(field: String): String? = path(field).takeIf { it.isTextual }?.asText()

    private val CVE_ID_REGEX = Regex("""CVE-\d{4}-\d{4,}""")
    private val CWE_REGEX = Regex("""CWE-\d+""")
}
