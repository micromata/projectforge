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

    @BeforeEach
    fun resetImportState() {
        persistenceService.runInTransaction { context ->
            context.em.find(CvrImportStateDO::class.java, NvdImportService.SOURCE)?.let { context.em.remove(it) }
        }
    }

    private fun groupId(name: String): Long = (initTestDB.getGroup(name) ?: initTestDB.addGroup(name)).id!!

    private fun import(cveId: String, product: String) {
        logon(TEST_ADMIN_USER)
        val configurations = """{"nodes":[{"operator":"OR","negate":false,"cpeMatch":[
            {"vulnerable":true,"criteria":"cpe:2.3:a:apache:$product:*:*:*:*:*:*:*:*","versionEndExcluding":"9.0.90"}]}]}"""
        val metrics = """"cvssMetricV31":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":9.8,"baseSeverity":"CRITICAL"}}]"""
        importService.import(
            { _, _, _ -> NvdCveParser.parsePage(NvdCveParserTest.page(cve(cveId, metrics = metrics, configurations = configurations))) },
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

        val anonymized = inventory.findingReport(InventoryLevel.AREA, area, anonymize = true)
        assertEquals("PROJECT", anonymized.scope.code)
        assertNull(anonymized.scope.name)
        val hidden = anonymized.vulnerabilities.single().findings.single()
        assertEquals("S1", hidden.system)
        assertEquals(listOf("S1-I1", "S1-I2"), hidden.instances)
        assertEquals("C1", hidden.component)

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
        assertTrue(inventory.nodeFindings(InventoryLevel.COMPONENT, fixed).findings.isEmpty())
        assertEquals(2, inventory.nodeFindings(InventoryLevel.COMPONENT, app).findings.size)

        // A reader outside the groups sees neither the system nor anything below the area.
        val outsider = user("report-outsider", CvrRole.READER)
        logon(outsider)
        assertThrows(AccessException::class.java) { inventory.findingReport(InventoryLevel.SYSTEM, system, anonymize = true) }
        assertThrows(AccessException::class.java) { inventory.findingReport(InventoryLevel.AREA, area, anonymize = true) }
        assertThrows(AccessException::class.java) { inventory.nodeFindings(InventoryLevel.SYSTEM, system) }
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { inventory.findingReport(InventoryLevel.SYSTEM, system, anonymize = true) }
        assertThrows(AccessException::class.java) { inventory.nodeFindings(InventoryLevel.SYSTEM, system) }
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
