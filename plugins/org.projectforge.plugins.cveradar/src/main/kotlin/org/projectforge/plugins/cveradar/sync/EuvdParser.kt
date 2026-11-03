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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Parses the answers of the EUVD API (`/search`). The document is untrusted input: unknown or missing fields are
 * ignored, an entry without a valid EUVD id is dropped.
 */
object EuvdParser {
    private val objectMapper = ObjectMapper()

    private val EUVD_ID_REGEX = Regex("""EUVD-\d{4}-\d{4,}""")
    private val CVE_ID_REGEX = Regex("""CVE-\d{4}-\d{4,}""")

    /** `Nov 3, 2026, 4:28:35 PM`, UTC. */
    private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a", Locale.US)

    private const val MAX_PRODUCTS = 50
    private const val MAX_ALIASES = 20

    fun parsePage(json: String): EuvdPage {
        val root = objectMapper.readTree(json)
        // /search answers {items,total}; the other endpoints a plain array.
        val items = (if (root.isArray) root else root.path("items")).mapNotNull { parseItem(it) }
        return EuvdPage(root.path("total").asInt(items.size), items)
    }

    /** @return null, if the node has no valid EUVD id. */
    fun parseItem(node: JsonNode): EuvdItem? {
        val id = node.path("id").asText("").trim()
        if (!EUVD_ID_REGEX.matches(id)) {
            return null
        }
        val ids = lines(node.path("aliases").asText(""))
        val cveId = ids.firstOrNull { CVE_ID_REGEX.matches(it) }
        val vector = node.path("baseScoreVector").asText("").trim().takeIf { it.isNotEmpty() }
        val score = node.path("baseScore").takeIf { it.isNumber }?.decimalValue()
            ?.takeIf { it.signum() > 0 }?.setScale(1, RoundingMode.HALF_UP)
        return EuvdItem(
            id = id,
            cveId = cveId,
            aliases = ids.filter { it != cveId }.distinct().take(MAX_ALIASES),
            description = node.path("description").asText("").trim().ifEmpty { null },
            published = parseTimestamp(node.path("datePublished").asText(null)),
            updated = parseTimestamp(node.path("dateUpdated").asText(null)),
            score = score,
            vector = vector?.take(255),
            cvss4 = vector?.startsWith("CVSS:4") ?: (node.path("baseScoreVersion").asText("").startsWith("4")),
            // The EUVD gives the EPSS in percent (69.54), stored as a probability (0.6954).
            epss = node.path("epss").takeIf { it.isNumber }?.decimalValue()?.movePointLeft(2)?.setScale(5, RoundingMode.HALF_UP)
                ?.takeIf { it.signum() >= 0 && it <= BigDecimal.ONE },
            exploitedSince = parseTimestamp(node.path("exploitedSince").asText(null)),
            products = products(node.path("enisaIdProduct")),
        )
    }

    private fun products(node: JsonNode): List<EuvdProduct> =
        node.mapNotNull { entry ->
            val product = entry.path("product").path("name").asText("").trim()
            val vendor = entry.path("product").path("vendor").path("name").asText("").trim()
            if (product.isEmpty() || vendor.isEmpty()) {
                null
            } else {
                EuvdProduct(vendor.take(255), product.take(255), entry.path("product_version").asText("").trim().ifEmpty { null })
            }
        }.distinctBy { it.vendor.lowercase() to it.product.lowercase() }.take(MAX_PRODUCTS)

    /** Values separated by line breaks, e.g. the aliases and references. */
    private fun lines(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

    /** Null if absent or not readable. The locale may put a narrow no-break space before AM/PM. */
    internal fun parseTimestamp(text: String?): Instant? {
        val normalized = text?.replace('\u202f', ' ')?.replace('\u00a0', ' ')?.trim()
        if (normalized.isNullOrEmpty()) {
            return null
        }
        return try {
            LocalDateTime.parse(normalized, TIMESTAMP_FORMAT).toInstant(ZoneOffset.UTC)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
