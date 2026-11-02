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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.model.CvrOsvAdvisoryDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.rest.AreaRequest
import org.projectforge.plugins.cveradar.rest.ComponentRequest
import org.projectforge.plugins.cveradar.rest.CvrCaseFileRest
import org.projectforge.plugins.cveradar.rest.CvrInventoryRest
import org.projectforge.plugins.cveradar.rest.InstanceRequest
import org.projectforge.plugins.cveradar.rest.SystemRequest
import org.projectforge.plugins.cveradar.rest.TechnologyRequest
import org.projectforge.plugins.cveradar.sync.NvdCveParser
import org.projectforge.plugins.cveradar.sync.NvdCveParserTest
import org.projectforge.plugins.cveradar.sync.NvdCveParserTest.Companion.cve
import org.projectforge.plugins.cveradar.sync.NvdImportService
import org.projectforge.plugins.cveradar.sync.OsvImportService
import org.projectforge.plugins.cveradar.sync.OsvParser
import org.projectforge.plugins.cveradar.sync.OsvSource
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant

/**
 * The advisories of OSV.dev against the libraries of the inventory (IMP-13): the import, an advisory becoming a
 * vulnerability on its first match (whatever its score), jar listings without groupId, the NVD taking the vulnerability
 * over, withdrawn advisories and the ad-hoc check, which stores nothing.
 */
class CvrOsvMatchingTest : CveRadarTestBase() {
    @Autowired
    private lateinit var fileRest: CvrCaseFileRest

    @Autowired
    private lateinit var inventory: CvrInventoryRest

    @Autowired
    private lateinit var matching: CvrMatchingService

    @Autowired
    private lateinit var nvdImportService: NvdImportService

    @Autowired
    private lateinit var osvImportService: OsvImportService

    @Autowired
    private lateinit var reportService: CvrFindingReportService

    @Autowired
    private lateinit var vulnerabilityDao: CvrVulnerabilityDao

    /** The bucket: the documents by OSV id. */
    private val bucket = mutableMapOf<String, String>()

    private val source = object : OsvSource {
        override fun modifiedSince(ecosystem: String, since: Instant) = bucket.values.mapNotNull { OsvParser.parse(it, setOf(ecosystem)) }
            .filter { it.modified >= since }.map { it.modified to it.id }

        override fun advisory(ecosystem: String, id: String) = bucket[id]

        override fun all(ecosystem: String, skip: (String) -> Boolean, consumer: (String) -> Unit) =
            bucket.filterKeys { !skip(it) }.values.forEach(consumer)
    }

    @BeforeEach
    fun prepare() {
        bucket.clear()
        cveRadarConfig.osv.ecosystems = "Maven,npm"
        persistenceService.runInTransaction { context ->
            listOf(NvdImportService.SOURCE, "${OsvImportService.SOURCE_PREFIX}Maven", "${OsvImportService.SOURCE_PREFIX}npm").forEach { source ->
                context.em.find(CvrImportStateDO::class.java, source)?.let { context.em.remove(it) }
            }
        }
    }

    private fun importOsv(now: String = "2026-11-01T10:00:00Z") = run {
        logon(TEST_ADMIN_USER)
        osvImportService.import(source, Instant.parse(now))
    }

    /** An advisory of a Maven package affected before [fixed]; MODERATE, CVSS 6.1. */
    private fun maven(
        id: String,
        pkg: String,
        fixed: String,
        aliases: List<String> = emptyList(),
        modified: String = "2026-10-30T10:00:00.123456789Z",
        withdrawn: String? = null,
    ) {
        bucket[id] = """{"id":"$id","modified":"$modified","published":"2026-10-01T00:00:00Z",${withdrawn?.let { "\"withdrawn\":\"$it\"," } ?: ""}
            "aliases":[${aliases.joinToString(",") { "\"$it\"" }}],"summary":"Deserialisation in $pkg","details":"Details.",
            "affected":[{"package":{"ecosystem":"Maven","name":"$pkg"},
              "ranges":[{"type":"ECOSYSTEM","events":[{"introduced":"2.0.0"},{"fixed":"$fixed"}]}]}],
            "references":[{"type":"FIX","url":"https://example.org/fix"}],
            "database_specific":{"severity":"MODERATE","cwe_ids":["CWE-502"]},
            "severity":[{"type":"CVSS_V3","score":"CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N"}]}"""
    }

    private fun groupId(name: String): Long = (initTestDB.getGroup(name) ?: initTestDB.addGroup(name)).id!!

    /** A system of the group with one instance; returns the system and its owner. */
    private fun system(code: String, group: String): Pair<Long, PFUserDO> {
        val owner = user("owner-$code", CvrRole.READER)
        addToGroup(owner, group)
        logon(owner)
        val area = inventory.createArea(AreaRequest(code = code, name = "Area $code", groupId = groupId(group)))
        val system = inventory.createSystem(SystemRequest(areaId = area, code = "$code-S", name = "System $code"))
        inventory.createInstance(InstanceRequest(systemId = system, code = "A"))
        return system to owner
    }

    private fun library(group: String?, artifact: String, version: String) = TechnologyRequest(
        kind = CvrTechnologyKind.PRODUCT, product = artifact, version = version, ecosystem = "maven",
        purl = "pkg:maven/${group?.let { "$it/" } ?: ""}$artifact@$version",
    )

    private fun findings(user: PFUserDO, vulnerabilityId: Long) = run { logon(user); fileRest.findings(vulnerabilityId).findings }

    private fun advisory(osvId: String) = persistenceService.selectSingleResult(
        "from CvrOsvAdvisoryDO a where a.osvId = :id", CvrOsvAdvisoryDO::class.java, Pair("id", osvId),
    )

    @Test
    fun `a library matches an advisory, which becomes a vulnerability whatever its score`() {
        val group = "org.test.osv1"
        val (system, owner) = system("OSV1", "cveradar-osv-g1")
        inventory.createComponent(
            ComponentRequest(systemId = system, code = "APP", name = "App", technologies = listOf(library(group, "databind", "2.12.0"))),
        )
        inventory.createComponent(
            ComponentRequest(systemId = system, code = "FIXED", name = "Fixed", technologies = listOf(library(group, "databind", "2.12.7.1"))),
        )
        inventory.createComponent(
            ComponentRequest(systemId = system, code = "JAR", name = "Jar listing", technologies = listOf(library(null, "databind", "2.12.0"))),
        )
        maven("GHSA-osv1-aaaa-bbbb", "$group:databind", "2.12.7.1", aliases = listOf("CVE-2026-98001"))
        maven("GHSA-osv1-cccc-dddd", "$group:unused", "3.0", aliases = listOf("CVE-2026-98002"))
        val result = importOsv()
        assertEquals(2, result.stored.created)

        val vulnerability = vulnerabilityDao.findByCveId("CVE-2026-98001")
        assertNotNull(vulnerability, "matched: a vulnerability, though CVSS 6.1 is below the threshold of the NVD import")
        assertEquals(CvrOsvAdvisoryService.STATUS_OSV, vulnerability!!.nvdStatus)
        assertEquals(BigDecimal("6.1"), vulnerability.cvssScore)
        assertEquals("MEDIUM", vulnerability.severity)
        assertEquals("CWE-502", vulnerability.cwes)
        assertTrue(vulnerability.description!!.startsWith("Deserialisation in $group:databind"))
        assertNull(vulnerabilityDao.findByCveId("CVE-2026-98002"), "no match, no vulnerability")
        assertEquals(vulnerability.id, advisory("GHSA-osv1-aaaa-bbbb")!!.vulnerability?.id)

        val byComponent = findings(owner, vulnerability.id!!).associateBy { it.component!!.substringBefore(" ") }
        assertEquals(setOf("APP", "JAR"), byComponent.keys, "the fixed version doesn't match")
        assertEquals(CvrFindingCertainty.AFFECTED, byComponent.getValue("APP").certainty)
        assertEquals(CvrFindingCertainty.POSSIBLE, byComponent.getValue("JAR").certainty, "without groupId only possible")
        assertTrue(byComponent.getValue("APP").matched!!.startsWith("GHSA-osv1-aaaa-bbbb Maven $group:databind (>= 2.0.0, < 2.12.7.1)"))

        val again = importOsv("2026-11-01T12:00:00Z")
        assertEquals(0, again.stored.created + again.stored.updated, "unchanged advisories aren't stored again")
        assertEquals(0, matching.rematch().let { it.created + it.updated + it.removed }, "idempotent")

        // A library added later matches an advisory that wasn't a vulnerability yet.
        logon(owner)
        inventory.createComponent(
            ComponentRequest(systemId = system, code = "LATE", name = "Late", technologies = listOf(library(group, "unused", "2.1"))),
        )
        val late = vulnerabilityDao.findByCveId("CVE-2026-98002")
        assertNotNull(late)
        assertEquals(listOf("LATE"), findings(owner, late!!.id!!).map { it.component!!.substringBefore(" ") })
    }

    @Test
    fun `the NVD takes the vulnerability over, a withdrawn advisory matches nothing`() {
        val group = "org.test.osv2"
        val (system, owner) = system("OSV2", "cveradar-osv-g2")
        inventory.createComponent(
            ComponentRequest(systemId = system, code = "APP", name = "App", technologies = listOf(library(group, "core", "2.5"))),
        )
        maven("GHSA-osv2-aaaa-bbbb", "$group:core", "2.6", aliases = listOf("CVE-2026-98101"))
        maven("PYSEC-2026-98101", "$group:core", "2.7", aliases = listOf("CVE-2026-98101"))
        importOsv()
        val vulnerability = vulnerabilityDao.findByCveId("CVE-2026-98101")!!
        assertEquals(1, findings(owner, vulnerability.id!!).size, "two advisories of one CVE: one vulnerability, one finding")
        assertEquals(vulnerability.id, advisory("PYSEC-2026-98101")!!.vulnerability?.id)

        // The NVD modifies the CVE: it imports it (a stored vulnerability, whatever the threshold) and keeps the finding.
        logon(TEST_ADMIN_USER)
        val metrics = """"cvssMetricV31":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":5.0,"baseSeverity":"MEDIUM"}}]"""
        nvdImportService.import(
            { _, _, _ -> NvdCveParser.parsePage(NvdCveParserTest.page(cve("CVE-2026-98101", metrics = metrics))) },
            Instant.parse("2026-11-01T10:00:00Z"),
        )
        val taken = vulnerabilityDao.findByCveId("CVE-2026-98101")!!
        assertEquals(vulnerability.id, taken.id)
        assertEquals("Analyzed", taken.nvdStatus)
        assertEquals(BigDecimal("5.0"), taken.cvssScore)
        assertEquals(1, findings(owner, taken.id!!).size, "the advisories still match")
        maven("GHSA-osv2-aaaa-bbbb", "$group:core", "2.6", aliases = listOf("CVE-2026-98101"), modified = "2026-10-31T10:00:00Z")
        importOsv("2026-11-01T12:00:00Z")
        assertEquals("Analyzed", vulnerabilityDao.findByCveId("CVE-2026-98101")!!.nvdStatus, "the OSV import leaves it to the NVD")

        // A vulnerability of advisories only: withdrawn, it matches nothing and is rejected.
        logon(owner)
        inventory.createComponent(
            ComponentRequest(systemId = system, code = "LIB", name = "Lib", technologies = listOf(library(group, "extra", "2.1"))),
        )
        maven("GHSA-osv2-eeee-ffff", "$group:extra", "3.0")
        importOsv("2026-11-01T14:00:00Z")
        val ghsa = vulnerabilityDao.findByCveId("GHSA-OSV2-EEEE-FFFF")
        assertNotNull(ghsa, "without CVE the GHSA id is the key")
        assertEquals(1, findings(owner, ghsa!!.id!!).size)
        maven("GHSA-osv2-eeee-ffff", "$group:extra", "3.0", modified = "2026-11-01T13:00:00Z", withdrawn = "2026-11-01T13:00:00Z")
        importOsv("2026-11-01T16:00:00Z")
        assertTrue(findings(owner, ghsa.id!!).isEmpty())
        assertEquals("Rejected", vulnerabilityDao.findByCveId("GHSA-OSV2-EEEE-FFFF")!!.nvdStatus)
    }

    @Test
    fun `the ad-hoc check finds advisories without a vulnerability and stores nothing`() {
        val group = "org.test.osv3"
        maven("GHSA-osv3-aaaa-bbbb", "$group:osv3lib", "2.6", aliases = listOf("CVE-2026-98201"))
        importOsv()
        assertNull(vulnerabilityDao.findByCveId("CVE-2026-98201"))
        val hits = matching.match(
            listOf(
                CvrMatchingService.Query(null, null, "osv3lib", "2.5", null, "pkg:maven/$group/osv3lib@2.5"),
                CvrMatchingService.Query(null, null, "osv3lib", "2.6", null, "pkg:maven/$group/osv3lib@2.6"),
            ),
        )
        val hit = hits[0].single()
        assertNull(hit.vulnerabilityId)
        assertEquals("CVE-2026-98201", hit.vulnerabilityKey)
        assertEquals(CvrFindingCertainty.AFFECTED, hit.certainty)
        assertTrue(hits[1].isEmpty(), "the fixed version")

        // The report makes the vulnerability of the advisories without storing it.
        val report = reportService.adHocReport(emptyMap(), mapOf("CVE-2026-98201" to emptyList()), anonymize = false)
        val reported = report.vulnerabilities.single()
        assertEquals("CVE-2026-98201", reported.cveId)
        assertEquals(CvrOsvAdvisoryService.STATUS_OSV, reported.nvdStatus)
        assertEquals(listOf("CWE-502"), reported.cwes)
        assertEquals(listOf("Patch"), reported.references.single().tags, "FIX of OSV")
        assertNull(vulnerabilityDao.findByCveId("CVE-2026-98201"), "nothing stored")
    }
}
