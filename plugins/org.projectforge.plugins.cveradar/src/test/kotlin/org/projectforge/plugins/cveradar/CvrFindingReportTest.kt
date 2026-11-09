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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.plugins.cveradar.model.CvrCaseKind
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.rest.CaseImportRequest
import org.projectforge.plugins.cveradar.rest.AreaRequest
import org.projectforge.plugins.cveradar.rest.ComponentRequest
import org.projectforge.plugins.cveradar.rest.CvrCaseFileRest
import org.projectforge.plugins.cveradar.rest.CvrInventoryRest
import org.projectforge.plugins.cveradar.rest.InstanceRequest
import org.projectforge.plugins.cveradar.rest.InventoryLevel
import org.projectforge.plugins.cveradar.rest.SystemRequest
import org.projectforge.plugins.cveradar.rest.TechnologyRequest
import org.projectforge.plugins.cveradar.sync.NvdCveParser
import org.projectforge.plugins.cveradar.sync.NvdCveParserTest
import org.projectforge.plugins.cveradar.sync.NvdCveParserTest.Companion.cve
import org.projectforge.plugins.cveradar.sync.NvdImportService
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

/**
 * The report of the open findings below a node: readers with access to the node only, only the instances they see,
 * the case of the security team with it, the internal names replaced on request (AI-03).
 */
class CvrFindingReportTest : CveRadarTestBase() {
    @Autowired
    private lateinit var fileRest: CvrCaseFileRest

    @Autowired
    private lateinit var importService: NvdImportService

    @Autowired
    private lateinit var inventory: CvrInventoryRest

    @Autowired
    private lateinit var jobHandler: JobHandler

    @BeforeEach
    fun resetImportState() {
        persistenceService.runInTransaction { context ->
            context.em.find(CvrImportStateDO::class.java, NvdImportService.SOURCE)?.let { context.em.remove(it) }
        }
    }

    private fun groupId(name: String): Long = (initTestDB.getGroup(name) ?: initTestDB.addGroup(name)).id!!

    private fun import(cveId: String, product: String) = import(listOf(cveId), product)

    private fun import(cveIds: List<String>, product: String) {
        logon(TEST_ADMIN_USER)
        val configurations = """{"nodes":[{"operator":"OR","negate":false,"cpeMatch":[
            {"vulnerable":true,"criteria":"cpe:2.3:a:apache:$product:*:*:*:*:*:*:*:*","versionEndExcluding":"9.0.90"}]}]}"""
        val metrics = """"cvssMetricV31":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":9.8,"baseSeverity":"CRITICAL"}}]"""
        importService.import(
            { _, _, _ ->
                val cves = cveIds.map { cve(it, metrics = metrics, configurations = configurations) }
                NvdCveParser.parsePage(NvdCveParserTest.page(*cves.toTypedArray()))
            },
            Instant.parse("2026-11-01T10:00:00Z"),
        )
    }

    private fun tech(product: String, version: String) = TechnologyRequest(
        kind = CvrTechnologyKind.PRODUCT, vendor = "apache", product = product, version = version,
        cpe = "cpe:2.3:a:apache:$product:$version:*:*:*:*:*:*:*",
    )

    @Test
    fun `the report and the findings of a node have what the user sees, with the case, the names replaced on request`() {
        val product = "report1"
        val cveId = "CVE-2026-97001"
        import(cveId, product)
        val owner = user("report-owner", CvrRole.READER)
        addToGroup(owner, "cveradar-report-g1")
        logon(owner)
        val area = inventory.createArea(AreaRequest(code = "RP1", name = "Report area", groupId = groupId("cveradar-report-g1")))
        val system = inventory.createSystem(SystemRequest(areaId = area, code = "RP1-S", name = "Shop"))
        inventory.createInstance(InstanceRequest(systemId = system, code = "A"))
        inventory.createInstance(InstanceRequest(systemId = system, code = "B"))
        val app = inventory.createComponent(ComponentRequest(systemId = system, code = "APP", name = "App", technologies = listOf(tech(product, "9.0.80"))))
        val fixed = inventory.createComponent(ComponentRequest(systemId = system, code = "NEW", name = "New", technologies = listOf(tech(product, "9.0.95"))))

        val team = user("report-team", CvrRole.SECURITY_TEAM)
        logon(team)
        fileRest.importCase(
            CaseImportRequest(
                kind = CvrCaseKind.CVE, code = cveId, title = "RCE", fixVersion = "9.0.90",
                techMeasures = "<ul><li>Update</li><li>Restart</li></ul>", workaround = "<p>Disable <b>it</b></p>",
            ),
        )

        logon(owner)
        val plain = inventory.findingReport(InventoryLevel.SYSTEM, system, anonymize = false)
        assertEquals("RP1-S", plain.scope.code)
        assertEquals(1, plain.vulnerabilityCount)
        assertEquals(2, plain.findingCount, "one per instance")
        val vulnerability = plain.vulnerabilities.single()
        assertEquals(cveId, vulnerability.cveId)
        assertEquals("9.0.90", vulnerability.case!!.fixVersion)
        assertEquals("- Update\n- Restart", vulnerability.case!!.techMeasures, "html as text")
        assertEquals("Disable it", vulnerability.case!!.workaround)
        val finding = vulnerability.findings.single()
        assertEquals("RP1-S Shop", finding.system)
        assertEquals(listOf("A", "B"), finding.instances, "grouped by component and technology")
        assertEquals("APP App", finding.component)
        assertEquals(CvrFindingCertainty.AFFECTED, finding.certainty)
        assertEquals("9.0.80", finding.technology!!.version)
        // Per library: what one update fixes (MAT-15).
        val library = plain.libraries.single()
        assertEquals("$product 9.0.80", library.technology)
        assertEquals(listOf("APP App"), library.components)
        assertEquals(1, library.vulnerabilityCount)
        assertEquals(1, library.affectedCount)
        assertEquals("CRITICAL", library.severity)
        assertEquals("9.0.90", library.fixVersion, "the end of the matched range")
        assertEquals(0, library.withoutFixCount)

        val anonymized = inventory.findingReport(InventoryLevel.AREA, area, anonymize = true)
        assertEquals("PROJECT", anonymized.scope.code)
        assertNull(anonymized.scope.name)
        val hidden = anonymized.vulnerabilities.single().findings.single()
        assertEquals("S1", hidden.system)
        assertEquals(listOf("S1-I1", "S1-I2"), hidden.instances)
        assertEquals("C1", hidden.component)
        assertEquals(listOf("C1"), anonymized.libraries.single().components)

        assertTrue(inventory.findingReport(InventoryLevel.COMPONENT, fixed, anonymize = false).vulnerabilities.isEmpty())
        assertEquals(1, inventory.findingReport(InventoryLevel.COMPONENT, app, anonymize = false).vulnerabilityCount)

        // The tab "Findings" of the dialog: one row per instance, with the case, the names as they are.
        val rows = inventory.nodeFindings(InventoryLevel.AREA, area).findings
        assertEquals(listOf("A", "B"), rows.map { it.instance })
        rows.forEach { row ->
            assertEquals(cveId, row.cveId)
            assertEquals(cveId, row.caseCode)
            assertEquals("RP1-S Shop", row.system)
            assertEquals("APP App", row.component)
            assertEquals("$product 9.0.80", row.technology)
            assertEquals(CvrFindingCertainty.AFFECTED, row.certainty)
        }
        assertEquals(listOf("APP App"), inventory.nodeFindings(InventoryLevel.AREA, area).libraries.single().components)
        assertTrue(inventory.nodeFindings(InventoryLevel.COMPONENT, fixed).findings.isEmpty())
        assertEquals(2, inventory.nodeFindings(InventoryLevel.COMPONENT, app).findings.size)

        // Checked again by hand: the same findings, nothing new.
        val job = jobHandler.getJobById(inventory.rematch(InventoryLevel.SYSTEM, system).jobId)!!
        val until = System.currentTimeMillis() + 30_000
        while (!job.terminated && System.currentTimeMillis() < until) {
            Thread.sleep(20)
        }
        assertEquals(AbstractJob.Status.FINISHED, job.status, job.errorMessage)
        assertEquals(2, inventory.nodeFindings(InventoryLevel.SYSTEM, system).findings.size)

        // A reader outside the groups sees neither the system nor anything below the area.
        val outsider = user("report-outsider", CvrRole.READER)
        logon(outsider)
        assertThrows(AccessException::class.java) { inventory.findingReport(InventoryLevel.SYSTEM, system, anonymize = true) }
        assertThrows(AccessException::class.java) { inventory.findingReport(InventoryLevel.AREA, area, anonymize = true) }
        assertThrows(AccessException::class.java) { inventory.nodeFindings(InventoryLevel.SYSTEM, system) }
        assertThrows(AccessException::class.java) { inventory.rematch(InventoryLevel.SYSTEM, system) }
        assertThrows(AccessException::class.java) { inventory.rematch(InventoryLevel.AREA, area) }
        assertThrows(AccessException::class.java) { inventory.rematch(InventoryLevel.COMPONENT, app) }
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { inventory.rematch(InventoryLevel.SYSTEM, system) }
        assertThrows(AccessException::class.java) { inventory.findingReport(InventoryLevel.SYSTEM, system, anonymize = true) }
        assertThrows(AccessException::class.java) { inventory.nodeFindings(InventoryLevel.SYSTEM, system) }
    }

    @Test
    fun `vulnerabilities left out on request are missing in the report and in its libraries`() {
        val product = "report2"
        val kept = "CVE-2026-97011"
        val left = "CVE-2026-97012"
        import(listOf(kept, left), product)
        val owner = user("report-exclude", CvrRole.READER)
        addToGroup(owner, "cveradar-report-g2")
        logon(owner)
        val area = inventory.createArea(AreaRequest(code = "RP2", name = "Exclude area", groupId = groupId("cveradar-report-g2")))
        val system = inventory.createSystem(SystemRequest(areaId = area, code = "RP2-S", name = "Shop"))
        inventory.createInstance(InstanceRequest(systemId = system, code = "A"))
        inventory.createComponent(ComponentRequest(systemId = system, code = "APP", name = "App", technologies = listOf(tech(product, "9.0.80"))))

        val all = inventory.findingReport(InventoryLevel.SYSTEM, system, anonymize = true)
        assertEquals(2, all.vulnerabilityCount)
        assertEquals(2, all.libraries.single().vulnerabilityCount)
        assertTrue(all.excludedCveIds.isEmpty())

        val report = inventory.findingReport(InventoryLevel.SYSTEM, system, anonymize = true, exclude = listOf(left, "CVE-1999-0001"))
        assertEquals(listOf(kept), report.vulnerabilities.map { it.cveId })
        assertEquals(1, report.vulnerabilityCount)
        assertEquals(1, report.findingCount)
        assertEquals(1, report.libraries.single().vulnerabilityCount, "the library without the one left out")
        assertEquals(listOf(left), report.excludedCveIds, "an unknown id is ignored")

        val none = inventory.findingReport(InventoryLevel.SYSTEM, system, anonymize = true, exclude = listOf(kept, left))
        assertEquals(0, none.vulnerabilityCount)
        assertTrue(none.libraries.isEmpty())
        assertEquals(listOf(kept, left), none.excludedCveIds)
    }

    @Test
    fun `the fix of a match is the end of its range, if it names one`() {
        assertEquals("1.26.10", CvrMatchingService.fixedVersionOf("GO-2026-6809 Go stdlib (< 1.26.10)"))
        assertEquals("1.18.3", CvrMatchingService.fixedVersionOf("GO-2022-0477 Go stdlib (>= 1.18.0-0, < 1.18.3)"))
        assertEquals("7.84.0", CvrMatchingService.fixedVersionOf("cpe:2.3:a:haxx:curl:*:*:*:*:*:*:*:* (>= 7.69.0, < 7.84.0)"))
        assertNull(CvrMatchingService.fixedVersionOf("cpe:2.3:a:perl:perl:*:*:*:*:*:*:*:* (<= 5.43.9)"), "the last affected one")
        assertNull(CvrMatchingService.fixedVersionOf("GHSA-x npm a (= 1.0.0)"))
        assertNull(CvrMatchingService.fixedVersionOf("GHSA-x npm a (all versions)"))
        assertNull(CvrMatchingService.fixedVersionOf("cpe:2.3:a:uutils:coreutils:-:*:*:*:*:rust:*:*"))
        assertNull(CvrMatchingService.fixedVersionOf(null))
    }

    @Test
    fun `html of the measures becomes text with lists and paragraphs on lines of their own`() {
        assertNull(CvrFindingReportService.plainText(" "))
        assertEquals(
            "Update first\n- a\n- b\nThen <restart> & check",
            CvrFindingReportService.plainText("<p>Update first</p><ul><li>a</li><li>b</li></ul><p>Then &lt;restart&gt; &amp; check</p>"),
        )
    }
}
