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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/** The answers of the NVD API 2.0, synthetic examples (fictitious CVE ids) in its format. */
class NvdCveParserTest {
    @Test
    fun `a page with the fields of a vulnerability`() {
        val page = NvdCveParser.parsePage(page(cve("CVE-2026-90001", metrics = """
            "cvssMetricV31":[
              {"source":"nvd@nist.gov","type":"Primary","cvssData":{"version":"3.1","vectorString":"CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H","baseScore":9.8,"baseSeverity":"CRITICAL"}},
              {"source":"cna@example.org","type":"Secondary","cvssData":{"version":"3.1","vectorString":"CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:H/A:H","baseScore":8.1,"baseSeverity":"HIGH"}}
            ],
            "cvssMetricV40":[
              {"source":"cna@example.org","type":"Secondary","cvssData":{"version":"4.0","vectorString":"CVSS:4.0/AV:N/AC:L/AT:N/PR:N/UI:N/VC:H/VI:H/VA:H/SC:N/SI:N/SA:N","baseScore":9.3,"baseSeverity":"CRITICAL"}}
            ]
        """), totalResults = 3))
        assertEquals(3, page.totalResults)
        val cve = page.cves.single()
        assertEquals("CVE-2026-90001", cve.cveId)
        assertEquals("Analyzed", cve.status)
        assertEquals(Instant.parse("2026-10-01T12:15:07.123Z"), cve.published)
        assertEquals(Instant.parse("2026-10-20T08:00:00.000Z"), cve.lastModified)
        assertEquals("A fictitious vulnerability.", cve.description)
        assertEquals(BigDecimal("9.8"), cve.cvss3!!.score)
        assertEquals("CRITICAL", cve.cvss3!!.severity)
        assertTrue(cve.cvss3!!.vector!!.startsWith("CVSS:3.1/"))
        assertEquals(BigDecimal("9.3"), cve.cvss4!!.score)
        assertFalse(cve.cvss4!!.primary)
        assertEquals(listOf("CWE-79", "CWE-89"), cve.cwes, "NVD-CWE-noinfo dropped, duplicates removed")
        assertTrue(cve.rawJson.contains("\"references\""))
    }

    @Test
    fun `the score of the NVD counts, without it the one of the CNA (IMP-10)`() {
        val nvdLower = NvdCveParser.parsePage(page(cve("CVE-2026-90002", metrics = """
            "cvssMetricV31":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":6.5,"baseSeverity":"MEDIUM"}}],
            "cvssMetricV40":[{"type":"Secondary","cvssData":{"vectorString":"CVSS:4.0/x","baseScore":8.7,"baseSeverity":"HIGH"}}]
        """))).cves.single()
        assertEquals(BigDecimal("6.5"), nvdLower.rating!!.score, "the NVD's rating wins over a higher one of the CNA")

        val cnaOnly = NvdCveParser.parsePage(page(cve("CVE-2026-90003", metrics = """
            "cvssMetricV31":[{"type":"Secondary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":7.5,"baseSeverity":"HIGH"}}],
            "cvssMetricV40":[{"type":"Secondary","cvssData":{"vectorString":"CVSS:4.0/x","baseScore":8.2,"baseSeverity":"HIGH"}}]
        """))).cves.single()
        assertEquals(BigDecimal("8.2"), cnaOnly.rating!!.score, "the higher one of the CNA")

        val v30 = NvdCveParser.parsePage(page(cve("CVE-2026-90004", metrics = """
            "cvssMetricV30":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.0/x","baseScore":7.0,"baseSeverity":"HIGH"}}]
        """))).cves.single()
        assertEquals("CVSS:3.0/x", v30.cvss3!!.vector, "v3.0 if there is no v3.1")

        val unrated = NvdCveParser.parsePage(page(cve("CVE-2026-90005", status = "Received"))).cves.single()
        assertNull(unrated.rating)
    }

    @Test
    fun `untrusted input - invalid ids are skipped, scores out of range ignored, rejected ones marked`() {
        val page = NvdCveParser.parsePage(page(
            cve("not-a-cve"),
            cve("CVE-2026-90006", status = "Rejected", metrics = """
                "cvssMetricV31":[{"type":"Primary","cvssData":{"baseScore":42,"baseSeverity":"HIGH"}}]
            """),
        ))
        val cve = page.cves.single()
        assertTrue(cve.rejected)
        assertNull(cve.rating)
        assertNull(NvdCveParser.parseTimestamp("yesterday"))
    }

    @Test
    fun `ranges are split into pieces of at most 120 days, timestamps sent with offset`() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val end = start.plus(Duration.ofDays(250))
        val pieces = NvdImportService.splitRange(start, end)
        assertEquals(3, pieces.size)
        assertEquals(start, pieces.first().first)
        assertEquals(end, pieces.last().second)
        pieces.zipWithNext().forEach { (a, b) -> assertEquals(a.second, b.first) }
        assertTrue(pieces.all { Duration.between(it.first, it.second) <= Duration.ofDays(120) })
        assertTrue(NvdImportService.splitRange(end, end).isEmpty())
        assertEquals("2026-01-01T00%3A00%3A00.000%2B00%3A00", NvdApiClient.format(start))
    }

    companion object {
        internal fun page(vararg cves: String, totalResults: Int? = null): String =
            """{"resultsPerPage":2000,"startIndex":0,"totalResults":${totalResults ?: cves.size},"format":"NVD_CVE","version":"2.0",
               "vulnerabilities":[${cves.joinToString(",") { """{"cve":$it}""" }}]}"""

        internal fun cve(
            id: String,
            status: String = "Analyzed",
            lastModified: String = "2026-10-20T08:00:00.000",
            metrics: String = "",
            kevAdded: String? = null,
        ): String = """
            {"id":"$id","sourceIdentifier":"cna@example.org","published":"2026-10-01T12:15:07.123",
             "lastModified":"$lastModified","vulnStatus":"$status",
             "descriptions":[{"lang":"es","value":"Una vulnerabilidad."},{"lang":"en","value":"A fictitious vulnerability."}],
             "metrics":{$metrics},
             "weaknesses":[{"source":"nvd@nist.gov","type":"Primary","description":[{"lang":"en","value":"CWE-79"},{"lang":"en","value":"NVD-CWE-noinfo"}]},
                           {"source":"cna@example.org","type":"Secondary","description":[{"lang":"en","value":"CWE-89"},{"lang":"en","value":"CWE-79"}]}],
             "references":[{"url":"https://example.org/advisory","source":"cna@example.org","tags":["Vendor Advisory"]}],
             ${kevAdded?.let { """"cisaExploitAdd":"$it",""" } ?: ""}
             "configurations":[]}
        """.trimIndent()
    }
}
