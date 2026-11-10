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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.rest.FindingReport
import org.projectforge.plugins.cveradar.rest.FindingReportScope
import org.projectforge.plugins.cveradar.rest.ReportFinding
import org.projectforge.plugins.cveradar.rest.ReportTechnology
import org.projectforge.plugins.cveradar.rest.ReportVulnerability
import java.math.BigDecimal
import java.util.Date

/** The finding report turned into the report per package: grouping, names, fix versions, counts and links. */
class CvrPackageReportBuilderTest {
    private fun technology(ecosystem: String, group: String?, name: String, version: String) = ReportTechnology(
        CvrTechnologyKind.PRODUCT, null, if (ecosystem == "npm" && group != null) "$group/$name" else name, version,
        ecosystem, LibraryEntry(ecosystem, group, name, version).purl, null, null,
    )

    private fun finding(technology: ReportTechnology?, matched: String, file: String = "F1", certainty: CvrFindingCertainty = CvrFindingCertainty.AFFECTED) =
        ReportFinding(null, emptyList(), file, certainty, matched, technology)

    private fun vulnerability(id: String, score: String?, severity: String?, vararg findings: ReportFinding, kev: Boolean = false) =
        ReportVulnerability(
            cveId = id, nvdStatus = null, published = null, description = "Description of $id",
            cvssScore = score?.let { BigDecimal(it) }, severity = severity, cvss31Vector = null, cvss40Vector = null,
            cwes = emptyList(), kevAdded = if (kev) Date() else null, references = emptyList(), case = null,
            findings = findings.toList(),
        )

    private fun report(vararg vulnerabilities: ReportVulnerability) = FindingReport(
        schemaVersion = "1", generated = Date(), scope = FindingReportScope(null, null, null, adHoc = true), anonymized = true,
        vulnerabilityCount = vulnerabilities.size, findingCount = vulnerabilities.sumOf { it.findings.size },
        libraries = emptyList(), vulnerabilities = vulnerabilities.toList(),
    )

    @Test
    fun `findings are grouped per package with the name of its ecosystem, the highest fix and the counts`() {
        val amqp = technology("maven", "com.rabbitmq", "amqp-client", "5.20.0")
        val moment = technology("npm", null, "moment", "2.29.4")
        val report = CvrPackageReportBuilder.build(
            report(
                vulnerability(
                    "CVE-2026-1", "8.7", "HIGH",
                    finding(amqp, "cpe:2.3:a:vmware:rabbitmq_java_client:*:*:*:*:*:*:*:* (< 5.33.1)"),
                    finding(amqp, "cpe:2.3:a:vmware:rabbitmq_java_client:*:*:*:*:*:*:*:* (< 5.33.1)", file = "F2"),
                ),
                vulnerability("CVE-2026-2", null, "MEDIUM", finding(amqp, "GHSA-xxxx maven com.rabbitmq:amqp-client (< 5.36.1)")),
                vulnerability("CVE-2026-3", "5.0", "MEDIUM", finding(amqp, "cpe:2.3:a:vmware:x:5.20.0:*:*:*:*:*:*:*")),
                vulnerability(
                    "GHSA-AbCd-1234-wxyz", "5.9", "MEDIUM",
                    finding(moment, "GHSA-abcd npm moment (>= 2.29.2, < 2.31.0)", certainty = CvrFindingCertainty.POSSIBLE),
                ),
                // Entered by hand: no technology, no package.
                vulnerability("CVE-2026-4", "9.8", "CRITICAL", finding(null, "cpe:2.3:a:x:y:*")),
            ),
        )
        assertEquals(2, report.packageCount)
        assertEquals(4, report.vulnerabilityCount)
        assertEquals(mapOf("CRITICAL" to 0, "HIGH" to 1, "MEDIUM" to 3, "LOW" to 0, "NONE" to 0), report.severityCounts)

        val first = report.packages[0]
        assertEquals("com.rabbitmq:amqp-client", first.name, "Maven: groupId:artifactId")
        assertEquals("Maven", first.ecosystemLabel)
        assertEquals(listOf("F1", "F2"), first.components)
        assertEquals(3, first.vulnerabilityCount)
        assertEquals(BigDecimal("8.7"), first.maxScore)
        assertEquals("HIGH", first.maxSeverity)
        assertEquals("5.36.1", first.fixVersion, "the highest fix of the matched ranges")
        assertEquals(1, first.withoutFixCount)
        assertEquals(listOf("CVE-2026-1", "CVE-2026-3", "CVE-2026-2"), first.vulnerabilities.map { it.id }, "by severity, then by score, unscored last")
        assertEquals("< 5.33.1", first.vulnerabilities[0].affectedRange)
        assertNull(first.vulnerabilities[1].affectedRange)
        assertFalse(first.vulnerabilities[0].possible)
        assertEquals("https://nvd.nist.gov/vuln/detail/CVE-2026-1", first.vulnerabilities[0].url)
        assertEquals("NVD", first.vulnerabilities[0].source)

        val second = report.packages[1]
        assertEquals("moment", second.name)
        assertEquals("npm", second.ecosystemLabel)
        val advisory = second.vulnerabilities.single()
        assertTrue(advisory.possible)
        assertEquals(">= 2.29.2, < 2.31.0", advisory.affectedRange)
        assertEquals("GitHub Advisory", advisory.source)
        assertEquals("https://github.com/advisories/GHSA-abcd-1234-wxyz", advisory.url)
    }

    @Test
    fun `a sure match wins over a possible one, KEV comes first`() {
        val lib = technology("npm", "@scope", "lib", "1.0.0")
        val report = CvrPackageReportBuilder.build(
            report(
                vulnerability("CVE-2026-5", "9.0", "CRITICAL", finding(lib, "x (< 2.0)")),
                vulnerability(
                    "CVE-2026-6", "4.0", "MEDIUM",
                    finding(lib, "x (< 1.5)", certainty = CvrFindingCertainty.POSSIBLE),
                    finding(lib, "y (< 1.2)", file = "F2"),
                    kev = true,
                ),
            ),
        )
        val pkg = report.packages.single()
        assertEquals("@scope/lib", pkg.name)
        assertTrue(pkg.kev)
        assertEquals("CVE-2026-6", pkg.vulnerabilities[0].id, "the KEV first")
        assertFalse(pkg.vulnerabilities[0].possible)
        assertEquals("< 1.2", pkg.vulnerabilities[0].affectedRange, "the range of the sure match")
        assertEquals("2.0", pkg.fixVersion)
    }
}
