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

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.plugins.cveradar.Cvss3
import org.projectforge.plugins.cveradar.CvrCaseDao
import org.projectforge.plugins.cveradar.CvrMatchingService
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrPurlMatchDO
import java.math.BigDecimal
import java.time.Instant

/** The advisories of OSV.dev: parsing, the spelling of the packages, the version ranges and the CVSS 3 score. */
class OsvParserTest {
    @Test
    fun `an advisory with its CVE as key, the ranges of the imported ecosystems only, GitHub's severity`() {
        val advisory = OsvParser.parse(advisory("GHSA-jq4v-f5q6-mjqq", aliases = listOf("PYSEC-2021-19", "CVE-2021-28957")), setOf("PyPI", "Maven"))!!
        assertEquals("CVE-2021-28957", advisory.vulnerabilityKey)
        assertEquals(Instant.parse("2026-10-31T10:55:53.653622012Z"), advisory.modified)
        assertEquals("MEDIUM", advisory.severity, "MODERATE of GitHub")
        assertEquals("CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N", advisory.cvss3Vector)
        assertEquals(listOf("CWE-79"), advisory.cwes)
        assertEquals(listOf("PyPI"), advisory.affected.map { it.ecosystem }, "npm isn't imported, the git range is skipped")
        val affected = advisory.affected.single()
        assertEquals("foo-bar-baz", affected.packageName, "PyPI normalised")
        assertEquals(listOf(OsvRange(null, "4.6.3", null), OsvRange("5.0", null, "5.1")), affected.ranges)
        assertTrue(affected.versions.isEmpty(), "with ranges the versions don't count")

        assertEquals("GHSA-AAAA-BBBB-CCCC", OsvParser.parse(advisory("GHSA-aaaa-bbbb-cccc"), setOf("PyPI"))!!.vulnerabilityKey)
        assertEquals(
            "GHSA-AAAA-BBBB-CCCC",
            OsvParser.parse(advisory("PYSEC-2026-1", aliases = listOf("GHSA-aaaa-bbbb-cccc")), setOf("PyPI"))!!.vulnerabilityKey,
            "a PYSEC and its GHSA share the key",
        )
        assertEquals(
            "CVE-2026-1000",
            OsvParser.parse(advisory("GHSA-aaaa-bbbb-cccc", aliases = listOf("CVE-2026-2000", "CVE-2026-1000")), setOf("PyPI"))!!.vulnerabilityKey,
            "the lowest of several CVE ids",
        )
        assertEquals(
            "CVE-2021-9999",
            OsvParser.parse(advisory("GHSA-aaaa-bbbb-cccc", aliases = listOf("CVE-2021-10000", "CVE-2021-9999")), setOf("PyPI"))!!.vulnerabilityKey,
            "compared as numbers, not as text",
        )
        assertEquals(
            "CVE-2021-9999",
            OsvParser.parse(advisory("GHSA-aaaa-bbbb-cccc", aliases = listOf("CVE-2021-99999999999999999999", "CVE-2021-9999")), setOf("PyPI"))!!
                .vulnerabilityKey,
            "a number too long for a Long doesn't fail",
        )
        assertNull(OsvParser.parse("""{"id":"GHSA-x"}""", setOf("PyPI")), "no modification time")
        assertNull(OsvParser.parse(advisory("PSF-2026-${"1".repeat(30)}"), setOf("PyPI")), "a key too long isn't cut")
        assertEquals("PSF-2026-1", OsvParser.parse(advisory("PSF-2026-1"), setOf("PyPI"))!!.vulnerabilityKey)
    }

    @Test
    fun `any OSV id fitting into a vulnerability key is one for a CVE case`() {
        listOf(
            "CVE-2026-12345", "GHSA-AAAA-BBBB-CCCC", "PYSEC-2021-19", "PSF-2026-1", "CGA-X2V3-ABCD-9F8E",
            "RHSA-2024:1234", "DEBIAN-CVE-2024-1234",
        ).forEach {
            assertTrue(CvrCaseDao.CVE_REGEX.matches(it), it)
        }
        listOf("CVE-26-1", "2026-1", "-X", "PSF-", "NOT-A-CVE", "PSF-2026-${"1".repeat(30)}", "psf-2026-1").forEach {
            assertFalse(CvrCaseDao.CVE_REGEX.matches(it), it)
        }
    }

    @Test
    fun `the events become ranges, an advisory without ranges lists its versions`() {
        fun ranges(json: String) = OsvParser.ranges(ObjectMapper().readTree(json))
        assertEquals(listOf(OsvRange(null, "1.2", null)), ranges("""[{"introduced":"0"},{"fixed":"1.2"}]"""))
        assertEquals(
            listOf(OsvRange("1.0", "1.5", null), OsvRange("2.0", null, null)),
            ranges("""[{"introduced":"1.0"},{"fixed":"1.5"},{"introduced":"2.0"}]"""),
        )
        assertEquals(listOf(OsvRange(null, null, "3.1")), ranges("""[{"introduced":"0"},{"last_affected":"3.1"}]"""))
        assertEquals(listOf(OsvRange(null, "9", null)), ranges("""[{"introduced":"0"},{"limit":"9"}]"""))

        val json = """{"id":"MAL-2026-7","modified":"2026-10-01T00:00:00Z","affected":[
            {"package":{"ecosystem":"npm","name":"@Evil/Pkg"},"versions":["1.0.0","1.0.1"]}]}"""
        val malicious = OsvParser.parse(json, setOf("npm"))!!
        assertTrue(malicious.malicious)
        assertEquals("MAL-2026-7", malicious.vulnerabilityKey)
        assertEquals("@evil/pkg", malicious.affected.single().packageName)
        assertEquals(listOf("1.0.0", "1.0.1"), malicious.affected.single().versions)
    }

    @Test
    fun `packages of purls and of OSV meet in one spelling`() {
        assertEquals(
            OsvPackages.PurlPackage("Maven", "com.fasterxml.jackson.core:jackson-databind", false),
            OsvPackages.ofPurl("pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.12.0"),
        )
        assertEquals("com.fasterxml.jackson.core:jackson-databind", OsvPackages.key("Maven", "com.fasterxml.jackson.core:jackson-databind"))
        assertEquals("jackson-databind", OsvPackages.shortName("Maven", "com.fasterxml.jackson.core:jackson-databind"))
        assertEquals(OsvPackages.PurlPackage("Maven", "jackson-databind", true), OsvPackages.ofPurl("pkg:maven/jackson-databind@2.12.0"), "jar listing")
        assertEquals(OsvPackages.PurlPackage("npm", "@angular/core", false), OsvPackages.ofPurl("pkg:npm/%40angular/core@17.0.0"))
        assertEquals(OsvPackages.PurlPackage("npm", "@angular/core", false), OsvPackages.ofPurl("pkg:npm/@angular/core@17.0.0"), "scope not encoded")
        assertEquals(OsvPackages.PurlPackage("npm", "lodash", false), OsvPackages.ofPurl("pkg:npm/lodash"), "without version")
        assertEquals(OsvPackages.PurlPackage("PyPI", "foo-bar", false), OsvPackages.ofPurl("pkg:pypi/Foo_Bar@1.0"))
        assertEquals(OsvPackages.PurlPackage("Go", "github.com/gin-gonic/gin", false), OsvPackages.ofPurl("pkg:golang/github.com/gin-gonic/gin@v1.9.0"))
        assertEquals(OsvPackages.PurlPackage("Packagist", "symfony/http-kernel", false), OsvPackages.ofPurl("pkg:composer/symfony/http-kernel@6.0.0"))
        assertEquals(OsvPackages.PurlPackage("Maven", "org.apache:x", false), OsvPackages.of("maven", "org.apache", "x"), "of a catalogue node")
        assertNull(OsvPackages.ofPurl("pkg:deb/debian/openssl@3.0.11"), "operating system packages aren't imported")
        assertNull(OsvPackages.ofPurl("cpe:2.3:a:apache:tomcat"))
    }

    @Test
    fun `versions against the ranges, by ecosystem`() {
        fun match(version: String?, ecosystem: String = "Maven", introduced: String? = null, fixed: String? = null, lastAffected: String? = null, exact: String? = null) =
            CvrMatchingService.purlVersionMatch(version, CvrPurlMatchDO().also {
                it.ecosystem = ecosystem
                it.introduced = introduced
                it.fixed = fixed
                it.lastAffected = lastAffected
                it.exactVersion = exact
            })
        assertEquals(CvrFindingCertainty.AFFECTED, match("2.12.0", introduced = "2.0.0", fixed = "2.12.7.1"))
        assertEquals(CvrFindingCertainty.AFFECTED, match("2.12.7", introduced = "2.0.0", fixed = "2.12.7.1"))
        assertNull(match("2.12.7.1", introduced = "2.0.0", fixed = "2.12.7.1"))
        assertNull(match("1.9.13", introduced = "2.0.0", fixed = "2.12.7.1"))
        assertEquals(CvrFindingCertainty.AFFECTED, match("5.1", lastAffected = "5.1"))
        assertNull(match("5.1.1", lastAffected = "5.1"))
        assertEquals(CvrFindingCertainty.POSSIBLE, match(null, fixed = "1.0"), "version unknown")
        assertEquals(CvrFindingCertainty.AFFECTED, match("1.0.1", ecosystem = "npm", exact = "1.0.1"))
        assertNull(match("1.0.2", ecosystem = "npm", exact = "1.0.1"))
        assertNull(match("5.3.18.RELEASE", fixed = "5.3.18"), "the release of Maven is the version itself")
        assertEquals(CvrFindingCertainty.AFFECTED, match("5.3.17.RELEASE", fixed = "5.3.18"))
        assertNull(match("4.6.3.post1", ecosystem = "PyPI", fixed = "4.6.3"), "a post release comes after the release")
        assertEquals(CvrFindingCertainty.AFFECTED, match("4.6.2.post1", ecosystem = "PyPI", fixed = "4.6.3"))
        assertNull(match("v1.9.1+incompatible", ecosystem = "Go", fixed = "v1.9.1"), "build metadata ignored")
    }

    @Test
    fun `the CVSS 3 base score of a vector`() {
        assertEquals(BigDecimal("9.8"), Cvss3.baseScore("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"))
        assertEquals(BigDecimal("10.0"), Cvss3.baseScore("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H"))
        assertEquals(BigDecimal("6.1"), Cvss3.baseScore("CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N"))
        assertEquals(BigDecimal("5.5"), Cvss3.baseScore("CVSS:3.0/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:N/A:N"))
        assertEquals(BigDecimal("8.8"), Cvss3.baseScore("CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H"))
        assertEquals(BigDecimal("0.0"), Cvss3.baseScore("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N"))
        assertEquals("CRITICAL", Cvss3.severity(BigDecimal("9.8")))
        assertEquals("MEDIUM", Cvss3.severity(BigDecimal("6.1")))
        assertNull(Cvss3.baseScore("CVSS:4.0/AV:N/AC:L/AT:N/PR:L/UI:N/VC:H/VI:L/VA:L/SC:N/SI:N/SA:N"))
        assertNull(Cvss3.baseScore("CVSS:3.1/AV:N/AC:L"))
    }

    companion object {
        /** An advisory of PyPI and npm with a git range, as GitHub writes it. */
        internal fun advisory(id: String, aliases: List<String> = emptyList()): String = """
            {"schema_version":"1.7.3","id":"$id","modified":"2026-10-31T10:55:53.653622012Z","published":"2021-03-21T05:15:00Z",
             "aliases":[${aliases.joinToString(",") { "\"$it\"" }}],"summary":"XSS in foo","details":"Details of foo.",
             "affected":[
               {"package":{"ecosystem":"PyPI","name":"Foo_Bar.baz","purl":"pkg:pypi/foo-bar-baz"},
                "ranges":[{"type":"GIT","repo":"https://github.com/x/y","events":[{"introduced":"0"},{"fixed":"a5f9cb5"}]},
                          {"type":"ECOSYSTEM","events":[{"introduced":"0"},{"fixed":"4.6.3"},{"introduced":"5.0"},{"last_affected":"5.1"}]}],
                "versions":["0.9","1.0"]},
               {"package":{"ecosystem":"npm","name":"foo"},"ranges":[{"type":"SEMVER","events":[{"introduced":"0"}]}]}],
             "references":[{"type":"FIX","url":"https://github.com/x/y/commit/a5f9cb5"},{"type":"WEB","url":"https://example.org/foo"}],
             "database_specific":{"cwe_ids":["CWE-79"],"severity":"MODERATE","github_reviewed":true},
             "severity":[{"type":"CVSS_V3","score":"CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N"}]}
        """.trimIndent()
    }
}
