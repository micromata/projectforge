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
    fun `the operating systems of a Go advisory only if every vulnerable package names some`() {
        fun goos(json: String) = OsvParser.goosOf(ObjectMapper().readTree(json))
        assertEquals(setOf("windows"), goos("""[{"path":"os","goos":["windows"]},{"path":"syscall","goos":["Windows"]}]"""))
        assertEquals(setOf("linux", "windows"), goos("""[{"path":"os","goos":["linux"]},{"path":"syscall","goos":["windows"]}]"""))
        assertNull(goos("""[{"path":"os","goos":["windows"]},{"path":"net/http"}]"""), "net/http is vulnerable everywhere")
        assertNull(goos("""[{"path":"net/http","goos":[]}]"""))
        assertNull(goos("""[]"""))
        assertNull(goos("""{}"""))
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
        assertNull(OsvPackages.ofPurl("pkg:deb/debian/openssl@3.0.11"), "a package of a distribution without its release")
        assertNull(OsvPackages.ofPurl("pkg:rpm/redhat/openssl@3.0.7?distro=rhel-9"), "a distribution not imported")
        assertEquals(OsvPackages.PurlPackage("Debian:12", "openssl", false), OsvPackages.ofPurl("pkg:deb/debian/openssl@3.0.11?arch=amd64&distro=debian-12.9"))
        assertEquals(OsvPackages.PurlPackage("Debian:13", "curl", false), OsvPackages.ofPurl("pkg:deb/debian/curl@8.14.1-2?distro=trixie"))
        assertEquals(OsvPackages.PurlPackage("Ubuntu:22.04:LTS", "curl", false), OsvPackages.ofPurl("pkg:deb/ubuntu/curl@7.81.0-1ubuntu1.30?distro=ubuntu-22.04"))
        assertEquals(
            OsvPackages.PurlPackage("Debian:12", "openssl", false),
            OsvPackages.ofPurl("pkg:deb/debian/libssl3@3.0.15-1~deb12u1?distro=debian-12&upstream=openssl"),
            "the advisories name the source package",
        )
        assertEquals(
            OsvPackages.PurlPackage("Debian:12", "bash", false, "5.2.15-2"),
            OsvPackages.ofPurl("pkg:deb/debian/bash@5.2.15-2+b2?distro=debian-12&upstream=bash%405.2.15-2"),
            "and its version",
        )
        assertEquals("Ubuntu:25.10", OsvPackages.release("ubuntu", "ubuntu-25.10"), "not LTS")
        assertEquals("Ubuntu:24.04:LTS", OsvPackages.release("ubuntu", "noble"))
        assertNull(OsvPackages.release("ubuntu", "unknown"))
        assertNull(OsvPackages.ofPurl("cpe:2.3:a:apache:tomcat"))
    }

    @Test
    fun `an advisory of a distribution with its CVE of upstream, the releases only, no impact as not affected`() {
        val json = """{"id":"UBUNTU-CVE-2026-1234","modified":"2026-10-31T10:00:00Z","upstream":["CVE-2026-1234"],"details":"Text.",
            "severity":[{"type":"Ubuntu","score":"medium"}],
            "affected":[
              {"package":{"ecosystem":"Ubuntu:22.04:LTS","name":"curl"},"ranges":[{"type":"ECOSYSTEM","events":[{"introduced":"0"},{"fixed":"7.81.0-1ubuntu1.3"}]}],
               "versions":["7.81.0-1","7.81.0-1ubuntu1.1"]},
              {"package":{"ecosystem":"Ubuntu:Pro:16.04:LTS","name":"curl"},"ranges":[{"type":"ECOSYSTEM","events":[{"introduced":"0"}]}]},
              {"package":{"ecosystem":"Ubuntu:22.04:LTS","name":"linux-aws"},"ranges":[{"type":"ECOSYSTEM","events":[{"introduced":"0"}]}]},
              {"package":{"ecosystem":"Ubuntu:22.04:LTS","name":"linux-firmware"},"ranges":[{"type":"ECOSYSTEM","events":[{"introduced":"0"}]}]},
              {"package":{"ecosystem":"Debian:12","name":"curl"},"ranges":[{"type":"ECOSYSTEM","events":[{"introduced":"0"}]}],
               "ecosystem_specific":{"urgency":"unimportant"}}]}"""
        val advisory = OsvParser.parse(json, setOf("Ubuntu", "Debian"))!!
        assertEquals("CVE-2026-1234", advisory.vulnerabilityKey)
        assertTrue(advisory.distro)
        assertEquals(
            listOf("Ubuntu:22.04:LTS" to "curl", "Ubuntu:22.04:LTS" to "linux-firmware", "Debian:12" to "curl"),
            advisory.affected.map { it.ecosystem to it.packageName }, "Ubuntu Pro isn't a release, a kernel is skipped",
        )
        assertEquals(listOf(OsvRange(null, "7.81.0-1ubuntu1.3", null)), advisory.affected[0].ranges)
        assertEquals(listOf(OsvRange.NOT_AFFECTED), advisory.affected[2].ranges, "unimportant")
        assertTrue(OsvParser.parse(json, setOf("Debian"))!!.affected.none { it.ecosystem.startsWith("Ubuntu") })

        val negligible = OsvParser.parse(json.replace("\"medium\"", "\"negligible\""), setOf("Ubuntu"))!!
        assertTrue(negligible.affected.all { it.ranges == listOf(OsvRange.NOT_AFFECTED) })

        val notice = OsvParser.parse(json.replace("UBUNTU-CVE-2026-1234", "USN-7000-1").replace("[\"CVE-2026-1234\"]", "[\"CVE-2026-1\",\"CVE-2026-2\"]"), setOf("Ubuntu"))!!
        assertEquals("USN-7000-1", notice.vulnerabilityKey, "a notice of many CVEs keeps its id")
        assertTrue(OsvPackages.isImportedId("Ubuntu", "UBUNTU-CVE-2026-1234"))
        assertFalse(OsvPackages.isImportedId("Ubuntu", "USN-7000-1"))
        assertFalse(OsvPackages.isImportedId("Debian", "DSA-5197-1"))
        assertTrue(OsvPackages.isImportedId("npm", "GHSA-aaaa-bbbb-cccc"))
    }

    @Test
    fun `versions of a distribution against the ranges, as dpkg compares them`() {
        fun match(version: String, ecosystem: String = "Debian:12", fixed: String? = null) =
            CvrMatchingService.purlVersionMatch(version, CvrPurlMatchDO().also {
                it.ecosystem = ecosystem
                it.fixed = fixed
            })
        assertNull(match("7.81.0-1ubuntu1.30", "Ubuntu:22.04:LTS", fixed = "7.81.0-1ubuntu1.3"))
        assertEquals(CvrFindingCertainty.AFFECTED, match("7.81.0-1ubuntu1.2", "Ubuntu:22.04:LTS", fixed = "7.81.0-1ubuntu1.3"))
        assertEquals(CvrFindingCertainty.AFFECTED, match("2.9.14+dfsg-1.3~deb12u1", fixed = "2.9.14+dfsg-1.3~deb12u2"))
        assertNull(match("2.9.14+dfsg-1.3~deb12u2", fixed = "2.9.14+dfsg-1.3~deb12u2"), "the build metadata of libraries isn't cut")
        assertNull(match("1:2.0-1", fixed = "3.0-1"), "the epoch")
        assertNull(match("7.84.0-1", fixed = OsvRange.NOT_AFFECTED.fixed), "not affected")
        assertNull(match("0", fixed = OsvRange.NOT_AFFECTED.fixed))
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
        assertNull(match("2.6.0.Final", fixed = "2.6.0"))
        assertNull(match("33.7.2-jre", introduced = "4.0", fixed = "33.7.2"), "a flavor of Guava is the release itself")
        assertNull(match("33.7.2-jre", fixed = "33.7.2-android"))
        assertNull(match("33.7.2-android", fixed = "33.7.2-jre"), "flavors are equal, not ordered by name")
        assertNull(match("1.0-jdk8", fixed = "1.0"), "any qualifier other than a pre-release")
        assertEquals(CvrFindingCertainty.AFFECTED, match("33.7.1-jre", introduced = "4.0", fixed = "33.7.2"))
        assertEquals(CvrFindingCertainty.AFFECTED, match("33.7.1-android", fixed = "33.7.2-jre"))
        assertEquals(CvrFindingCertainty.AFFECTED, match("33.7.2-jre", lastAffected = "33.7.2"))
        listOf("2.0.0-beta1", "2.0.0-RC2", "2.0.0-rc.1", "2.0.0-M1", "2.0.0-SNAPSHOT", "2.0.0.alpha.1", "2.0.0-20230101.123456-1")
            .forEach { assertEquals(CvrFindingCertainty.AFFECTED, match(it, fixed = "2.0.0"), "pre-release $it") }
        assertNull(match("1.0-1", fixed = "1.0"), "a build number comes after the release")
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
