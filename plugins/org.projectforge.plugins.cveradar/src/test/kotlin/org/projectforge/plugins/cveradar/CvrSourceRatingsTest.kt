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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.plugins.cveradar.model.CvrOsvAdvisoryDO
import org.projectforge.plugins.cveradar.model.CvrSource
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.projectforge.plugins.cveradar.rest.SourceRatingView
import org.projectforge.plugins.cveradar.sync.EuvdImportService
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.util.Date
import java.util.zip.GZIPOutputStream

class CvrSourceRatingsTest {
    private fun gzip(text: String): ByteArray =
        ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(text.toByteArray()) } }.toByteArray()

    private fun row(rating: SourceRatingView) =
        listOf(rating.source, rating.origin, rating.primary, rating.version, rating.score, rating.severity).joinToString("|")

    @Test
    fun `the NVD and the CNA, the advisories with their own rating, side by side`() {
        val vulnerability = CvrVulnerabilityDO().also {
            it.cveId = "CVE-2026-116804"
            it.nvdLastModified = Date()
            it.rawDocument = gzip(
                """{"id":"CVE-2026-116804","metrics":{
                "cvssMetricV40":[{"source":"disclosure@vulncheck.com","type":"Secondary","cvssData":{"version":"4.0",
                  "vectorString":"CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:A/VC:N/VI:N/VA:N/SC:L/SI:L/SA:N","baseScore":5.1,"baseSeverity":"MEDIUM"}}],
                "cvssMetricV31":[{"source":"disclosure@vulncheck.com","type":"Secondary","cvssData":{"version":"3.1",
                  "vectorString":"CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N","baseScore":6.1,"baseSeverity":"MEDIUM"}},
                  {"source":"nvd@nist.gov","type":"Primary","cvssData":{"version":"3.1",
                  "vectorString":"CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N","baseScore":6.1,"baseSeverity":"MEDIUM"}},
                  {"source":"broken","type":"Secondary","cvssData":{"baseScore":42}}]}}""",
            )
        }
        val ghsa = CvrOsvAdvisoryDO().also {
            it.osvId = "GHSA-cg2g-xh64-v4q7"
            it.severity = "CRITICAL"
            it.cvss31Vector = "CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N"
            it.cvss40Vector = "CVSS:4.0/AV:N/AC:H/AT:P/PR:N/UI:N/VC:H/VI:H/VA:L/SC:N/SI:N/SA:N"
        }
        val withdrawn = CvrOsvAdvisoryDO().also { it.osvId = "GHSA-gone"; it.severity = "HIGH"; it.withdrawn = Date() }
        assertEquals(
            listOf(
                "NVD|nvd@nist.gov|true|3.1|6.1|MEDIUM",
                "NVD|disclosure@vulncheck.com|false|4.0|5.1|MEDIUM",
                "NVD|disclosure@vulncheck.com|false|3.1|6.1|MEDIUM",
                "OSV|GHSA-cg2g-xh64-v4q7|false|3.1|6.1|MEDIUM",
                "OSV|GHSA-cg2g-xh64-v4q7|false|4.0|null|null",
                "OSV|GHSA-cg2g-xh64-v4q7|false|null|null|CRITICAL",
            ),
            CvrSourceRatings.of(vulnerability, listOf(ghsa, withdrawn)).map { row(it) },
            "the NVD's own first, an invalid score and a withdrawn advisory left out",
        )
    }

    @Test
    fun `a vulnerability made of advisories has no NVD rating, one of the EUVD its own`() {
        val fromOsv = CvrVulnerabilityDO().also { it.rawDocument = gzip("""{"metrics":{"cvssMetricV31":[]}}""") }
        assertEquals(emptyList<SourceRatingView>(), CvrSourceRatings.of(fromOsv, emptyList()), "the document is the advisory's")

        val fromEuvd = CvrVulnerabilityDO().also {
            it.nvdStatus = EuvdImportService.STATUS_EUVD
            it.euvdId = "EUVD-2026-110512"
            it.cvssScore = BigDecimal("5.1")
            it.severity = "MEDIUM"
            it.cvss40Vector = "CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:A/VC:N/VI:N/VA:N/SC:L/SI:L/SA:N"
        }
        assertEquals(
            listOf("EUVD|EUVD-2026-110512|false|4.0|5.1|MEDIUM"),
            CvrSourceRatings.of(fromEuvd, emptyList()).map { row(it) },
        )
        assertEquals(CvrSource.EUVD, CvrSourceRatings.of(fromEuvd, emptyList()).single().source)
    }
}
