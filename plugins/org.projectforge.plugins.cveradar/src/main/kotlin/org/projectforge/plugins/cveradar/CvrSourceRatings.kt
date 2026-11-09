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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.plugins.cveradar.model.CvrOsvAdvisoryDO
import org.projectforge.plugins.cveradar.model.CvrSource
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.projectforge.plugins.cveradar.rest.SourceRatingView
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.zip.GZIPInputStream

private val log = KotlinLogging.logger {}

/**
 * The ratings of the sources side by side: the vulnerability keeps one score and severity only (the NVD, before that
 * the advisories or the EUVD), the sources may disagree (e.g. GitHub CRITICAL, the CNA 6.1). Derived from what is
 * stored: the NVD document, the OSV advisories, the rating of the EUVD ([CvrVulnerabilityDO.euvdScore]).
 */
object CvrSourceRatings {
    /** The NVD and the CNA (NVD document), the advisories not withdrawn (preferred first), the EUVD. */
    fun of(vulnerability: CvrVulnerabilityDO, advisories: List<CvrOsvAdvisoryDO>): List<SourceRatingView> =
        ofNvd(vulnerability) + advisories.filter { it.withdrawn == null }.flatMap { ofAdvisory(it) } + ofEuvd(vulnerability)

    /** Every CVSS 3.x and 4.0 metric of the NVD document, the NVD's own (Primary) first. */
    internal fun ofNvd(vulnerability: CvrVulnerabilityDO): List<SourceRatingView> {
        if (vulnerability.nvdLastModified == null) return emptyList() // The document is the one of an advisory.
        val raw = vulnerability.rawDocument ?: return emptyList()
        return try {
            val json = GZIPInputStream(ByteArrayInputStream(raw)).use { String(it.readNBytes(MAX_RAW_BYTES), Charsets.UTF_8) }
            val metrics = OBJECT_MAPPER.readTree(json).path("metrics")
            NVD_METRICS.flatMap { name -> metrics.path(name).mapNotNull { ofNvdMetric(it) } }
                .sortedBy { !it.primary }.take(MAX_RATINGS)
        } catch (ex: Exception) {
            log.warn { "Can't read the ratings of ${vulnerability.cveId}: ${ex.message}" }
            emptyList()
        }
    }

    private fun ofNvdMetric(metric: JsonNode): SourceRatingView? {
        val data = metric.path("cvssData")
        val score = data.path("baseScore").takeIf { it.isNumber }?.decimalValue()
            ?.takeIf { it >= BigDecimal.ZERO && it <= BigDecimal.TEN }?.setScale(1, RoundingMode.HALF_UP) ?: return null
        val vector = text(data, "vectorString")
        return SourceRatingView(
            source = CvrSource.NVD,
            origin = text(metric, "source"),
            primary = text(metric, "type") == "Primary",
            version = text(data, "version") ?: versionOf(vector),
            score = score,
            severity = (text(data, "baseSeverity") ?: text(metric, "baseSeverity"))?.uppercase(),
            vector = vector,
        )
    }

    /**
     * One rating per vector of the advisory (CVSS 3.x scored, CVSS 4.0 not: there is no calculator) and the rating of
     * its source of its own (GitHub: `database_specific.severity`).
     */
    internal fun ofAdvisory(advisory: CvrOsvAdvisoryDO): List<SourceRatingView> {
        fun rating(version: String?, score: BigDecimal?, severity: String?, vector: String?) = SourceRatingView(
            source = CvrSource.OSV, origin = advisory.osvId, primary = false, version = version, score = score,
            severity = severity, vector = vector,
        )
        val cvss3 = advisory.cvss31Vector?.let { vector ->
            val score = Cvss3.baseScore(vector)
            rating(versionOf(vector) ?: "3.1", score, score?.let { Cvss3.severity(it) }, vector)
        }
        val cvss4 = advisory.cvss40Vector?.let { rating(versionOf(it) ?: "4.0", null, null, it) }
        val own = advisory.severity?.let { rating(null, null, it, null) }
        return listOfNotNull(cvss3, cvss4, own)
    }

    /** The rating of the EUVD, kept apart from the one of the NVD; its severity by the buckets of CVSS 3 (as the import). */
    internal fun ofEuvd(vulnerability: CvrVulnerabilityDO): List<SourceRatingView> {
        val score = vulnerability.euvdScore ?: return emptyList()
        val vector = vulnerability.euvdVector
        return listOf(
            SourceRatingView(
                source = CvrSource.EUVD, origin = vulnerability.euvdId, primary = false, version = versionOf(vector),
                score = score, severity = Cvss3.severity(score), vector = vector,
            ),
        )
    }

    /** 3.1 of `CVSS:3.1/AV:N/…`. */
    private fun versionOf(vector: String?): String? = vector?.let { VERSION.find(it)?.groupValues?.get(1) }

    private fun text(node: JsonNode, field: String): String? =
        node.path(field).takeIf { it.isTextual }?.asText()?.trim()?.take(MAX_TEXT)?.ifEmpty { null }

    private val OBJECT_MAPPER = ObjectMapper()
    private val NVD_METRICS = listOf("cvssMetricV40", "cvssMetricV31", "cvssMetricV30")
    private val VERSION = Regex("""^CVSS:(\d\.\d)/""")
    private const val MAX_RAW_BYTES = 5_000_000
    private const val MAX_RATINGS = 20
    private const val MAX_TEXT = 255
}
