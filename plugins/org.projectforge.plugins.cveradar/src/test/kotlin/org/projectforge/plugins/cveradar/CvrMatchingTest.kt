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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrCatalogKind
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import org.projectforge.plugins.cveradar.model.CvrFindingStatus
import org.projectforge.plugins.cveradar.model.CvrImportStateDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.rest.AreaRequest
import org.projectforge.plugins.cveradar.rest.CatalogNodeRequest
import org.projectforge.plugins.cveradar.rest.ComponentRequest
import org.projectforge.plugins.cveradar.rest.CvrCatalogRest
import org.projectforge.plugins.cveradar.rest.CvrCaseFileRest
import org.projectforge.plugins.cveradar.rest.CvrInventoryRest
import org.projectforge.plugins.cveradar.rest.DiscardRequest
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
 * The matching of the vulnerabilities against the inventory (MAT-01 ff.): products by CPE, catalogue or name, the
 * version ranges, one finding per instance, idempotence, discarding, the triggers (import, change of the inventory) and
 * who sees which finding (VIS-02, VIS-04).
 */
class CvrMatchingTest : CveRadarTestBase() {
    @Autowired
    private lateinit var fileRest: CvrCaseFileRest

    @Autowired
    private lateinit var catalog: CvrCatalogRest

    @Autowired
    private lateinit var importService: NvdImportService

    @Autowired
    private lateinit var inventory: CvrInventoryRest

    @Autowired
    private lateinit var matching: CvrMatchingService

    @Autowired
    private lateinit var vulnerabilityDao: CvrVulnerabilityDao

    private var clock = 0L

    @BeforeEach
    fun resetImportState() {
        clock = 0L
        persistenceService.runInTransaction { context ->
            context.em.find(CvrImportStateDO::class.java, NvdImportService.SOURCE)?.let { context.em.remove(it) }
        }
    }

    private fun groupId(name: String): Long = (initTestDB.getGroup(name) ?: initTestDB.addGroup(name)).id!!

    /** A CVE of the product before 9.0.90, from 9.0.0 on; every test has a product of its own, as the tests share the database. */
    private fun importTomcat(cveId: String, product: String): Long {
        logon(TEST_ADMIN_USER)
        val configurations = """{"nodes":[{"operator":"OR","negate":false,"cpeMatch":[
            {"vulnerable":true,"criteria":"cpe:2.3:a:apache:$product:*:*:*:*:*:*:*:*","versionStartIncluding":"9.0.0","versionEndExcluding":"9.0.90"}]}]}"""
        val metrics = """"cvssMetricV31":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":9.8,"baseSeverity":"CRITICAL"}}]"""
        importService.import(
            { _, _, _ ->
                NvdCveParser.parsePage(
                    NvdCveParserTest.page(cve(cveId, metrics = metrics, configurations = configurations)),
                )
            },
            Instant.parse("2026-10-27T10:00:00Z").plusSeconds(clock++),
        )
        return vulnerabilityDao.findByCveId(cveId)!!.id!!
    }

    /** A system of the group with the instances A and B; returns the system and the owner (reader and member). */
    private fun system(code: String, group: String): Pair<Long, PFUserDO> {
        val owner = user("owner-$code", CvrRole.READER)
        addToGroup(owner, group)
        logon(owner)
        val area = inventory.createArea(AreaRequest(code = code, name = "Area $code", groupId = groupId(group)))
        val system = inventory.createSystem(SystemRequest(areaId = area, code = "$code-S", name = "System $code"))
        inventory.createInstance(InstanceRequest(systemId = system, code = "A"))
        inventory.createInstance(InstanceRequest(systemId = system, code = "B"))
        return system to owner
    }

    private fun tomcat(product: String, version: String?, withCpe: Boolean = false, vendor: String? = "apache") =
        TechnologyRequest(
            kind = CvrTechnologyKind.PRODUCT, vendor = vendor, product = product.replaceFirstChar { it.uppercase() }, version = version,
            cpe = if (withCpe) "cpe:2.3:a:apache:$product:$version:*:*:*:*:*:*:*" else null,
        )

    private fun findings(user: PFUserDO, vulnerabilityId: Long) =
        run { logon(user); fileRest.findings(vulnerabilityId) }

    @Test
    fun `a component of the baseline gives a finding per instance, the CPE is sure, the name and a missing version are only possible`() {
        val P = "tomcat1"
        val vulnerabilityId = importTomcat("CVE-2026-94001", P)
        val (system, owner) = system("MT1", "cveradar-match-g1")
        inventory.createComponent(
            ComponentRequest(
                systemId = system, code = "SURE", name = "Sure",
                technologies = listOf(tomcat(P, "9.0.80", true)),
            ),
        )
        inventory.createComponent(ComponentRequest(systemId = system, code = "NAME", name = "By name", technologies = listOf(tomcat(P, "9.0.80"))))
        inventory.createComponent(ComponentRequest(systemId = system, code = "NOVER", name = "No version", technologies = listOf(tomcat(P, null))))
        inventory.createComponent(
            ComponentRequest(systemId = system, code = "FIXED", name = "Fixed", technologies = listOf(tomcat(P, "9.0.95", true))),
        )
        inventory.createComponent(
            ComponentRequest(systemId = system, code = "OLD", name = "Before the range", technologies = listOf(tomcat(P, "8.5.1", true))),
        )
        inventory.createComponent(
            ComponentRequest(systemId = system, code = "OTHER", name = "Other vendor", technologies = listOf(tomcat(P, "9.0.80", vendor = "someone-else"))),
        )
        val list = findings(owner, vulnerabilityId)
        assertEquals(0, list.hiddenCount)
        val byComponent = list.findings.groupBy { it.component!!.substringBefore(" ") }
        assertEquals(setOf("SURE", "NAME", "NOVER", "OTHER"), byComponent.keys, "fixed and too old don't match")
        assertEquals(setOf("A", "B"), byComponent.getValue("SURE").map { it.instance }.toSet(), "one finding per instance")
        assertEquals(CvrFindingCertainty.AFFECTED, byComponent.getValue("SURE").first().certainty)
        assertEquals(CvrFindingCertainty.POSSIBLE, byComponent.getValue("NAME").first().certainty)
        assertEquals(CvrFindingCertainty.POSSIBLE, byComponent.getValue("NOVER").first().certainty)
        assertEquals(CvrFindingCertainty.POSSIBLE, byComponent.getValue("OTHER").first().certainty, "by name, the vendor spelled otherwise")
        assertTrue(byComponent.getValue("SURE").first().matched!!.contains("< 9.0.90"))
    }

    @Test
    fun `a component of one instance gives a finding for that instance only, a changed version and a deleted component remove it`() {
        val P = "tomcat2"
        val vulnerabilityId = importTomcat("CVE-2026-94002", P)
        val (system, owner) = system("MT2", "cveradar-match-g2")
        logon(owner)
        val instance = inventory.tree().rows.single { it.code == "MT2-S-A" }.id
        val component = inventory.createComponent(
            ComponentRequest(instanceId = instance, code = "APP", name = "App", technologies = listOf(tomcat(P, "9.0.80", true))),
        )
        assertEquals(listOf("A"), findings(owner, vulnerabilityId).findings.map { it.instance })
        val tree = inventory.tree().rows.single { it.id == component && it.level == InventoryLevel.COMPONENT }
        assertEquals(1, tree.findingCount, "the tree counts the finding")
        assertEquals(1, inventory.tree().rows.single { it.id == system && it.level == InventoryLevel.SYSTEM }.findingCount)

        val technologyId = tree.technologies.single().id
        inventory.updateComponent(
            component,
            ComponentRequest(
                code = "APP", name = "App",
                technologies = listOf(TechnologyRequest(id = technologyId, kind = CvrTechnologyKind.PRODUCT, vendor = "apache", product = "Tomcat2", version = "9.0.95", cpe = "cpe:2.3:a:apache:tomcat2:9.0.95:*:*:*:*:*:*:*")),
            ),
        )
        assertTrue(findings(owner, vulnerabilityId).findings.isEmpty(), "an update to the fixed version removes the finding")
        inventory.updateComponent(
            component,
            ComponentRequest(code = "APP", name = "App", technologies = listOf(tomcat(P, "9.0.80", true))),
        )
        assertEquals(1, findings(owner, vulnerabilityId).findings.size, "a rollback brings it back")
        inventory.delete(InventoryLevel.COMPONENT, component)
        assertTrue(findings(owner, vulnerabilityId).findings.isEmpty())
    }

    @Test
    fun `matching again changes nothing, a discarded finding stays discarded, a CVE imported later matches as well`() {
        val P = "tomcat3"
        val (system, owner) = system("MT3", "cveradar-match-g3")
        val component = inventory.createComponent(
            ComponentRequest(systemId = system, code = "APP", name = "App", technologies = listOf(tomcat(P, "9.0.80", true))),
        )
        val vulnerabilityId = importTomcat("CVE-2026-94003", P) // After the inventory: the import matches.
        val team = user("team3", CvrRole.SECURITY_TEAM)
        addToGroup(team, "cveradar-match-g3")
        val first = findings(team, vulnerabilityId)
        assertEquals(2, first.findings.size)
        assertTrue(first.canChange)

        val rerun = matching.rematch()
        assertEquals(0, rerun.created + rerun.updated + rerun.removed, "idempotent")
        assertThrows(UserException::class.java) { fileRest.discardFinding(first.findings.first().id, DiscardRequest(" ")) }
        val after = fileRest.discardFinding(first.findings.first().id, DiscardRequest("only in the test environment"))
        assertEquals(1, after.findings.count { it.status == CvrFindingStatus.DISCARDED })
        matching.rematch()
        assertEquals(1, findings(team, vulnerabilityId).findings.count { it.status == CvrFindingStatus.DISCARDED }, "not created again")
        // A version changed and reverted keeps the discarded finding with its reason.
        logon(owner)
        inventory.updateComponent(component, ComponentRequest(code = "APP", name = "App", technologies = listOf(tomcat(P, "9.0.95", true))))
        inventory.updateComponent(component, ComponentRequest(code = "APP", name = "App", technologies = listOf(tomcat(P, "9.0.80", true))))
        val reverted = findings(team, vulnerabilityId).findings
        assertEquals(2, reverted.size)
        assertEquals(listOf("only in the test environment"), reverted.mapNotNull { it.discardReason })
        val restored = fileRest.restoreFinding(first.findings.first().id)
        assertEquals(0, restored.findings.count { it.status == CvrFindingStatus.DISCARDED })
        assertFalse(restored.findings.any { it.discardReason != null })

        // Without the index nothing matches; rebuilt from the stored documents, everything is back.
        persistenceService.runInTransaction { it.executeUpdate("delete from CvrCpeMatchDO") }
        // The database is shared by the tests: only the findings of this vulnerability are counted.
        matching.rematch()
        assertTrue(findings(team, vulnerabilityId).findings.isEmpty())
        matching.rebuildIndex()
        matching.rematch()
        assertEquals(2, findings(team, vulnerabilityId).findings.size)
    }

    @Test
    fun `who sees an instance sees its findings, everybody else only the count, the security team discards`() {
        val P = "tomcat4"
        val vulnerabilityId = importTomcat("CVE-2026-94004", P)
        val (system, owner) = system("MT4", "cveradar-match-g4")
        inventory.createComponent(
            ComponentRequest(systemId = system, code = "APP", name = "App", technologies = listOf(tomcat(P, "9.0.80", true))),
        )
        val outsider = user("outsider4", CvrRole.READER)
        val seen = findings(outsider, vulnerabilityId)
        assertTrue(seen.findings.isEmpty(), "no system of the outsider")
        assertEquals(2, seen.hiddenCount)
        assertFalse(seen.canChange)
        val own = findings(owner, vulnerabilityId)
        assertEquals(2, own.findings.size)
        assertEquals(0, own.hiddenCount)
        assertFalse(own.canChange, "a reader doesn't discard")

        assertThrows(AccessException::class.java) { fileRest.discardFinding(own.findings.first().id, DiscardRequest("no")) }
        assertThrows(AccessException::class.java) { fileRest.restoreFinding(own.findings.first().id) }
        val team = user("team4", CvrRole.SECURITY_TEAM) // Reads internal systems, but isn't in the group of this one.
        logon(team)
        assertEquals(2, fileRest.findings(vulnerabilityId).findings.size, "the security team reads internal systems")
        assertNotNull(fileRest.discardFinding(own.findings.first().id, DiscardRequest("test")))
        logon(TEST_USER)
        assertThrows(AccessException::class.java) { fileRest.findings(vulnerabilityId) }
        assertThrows(AccessException::class.java) { fileRest.discardFinding(own.findings.first().id, DiscardRequest("no")) }
    }

    @Test
    fun `a changed CPE of a catalogue node matches its technologies again`() {
        val P = "tomcat5"
        val vulnerabilityId = importTomcat("CVE-2026-94005", P)
        val (system, owner) = system("MT5", "cveradar-match-g5")
        val team = user("team5", CvrRole.SECURITY_TEAM)
        logon(team)
        val node = catalog.create(CatalogNodeRequest(kind = CvrCatalogKind.PRODUCT, name = "Server 5", cpeVendor = "apache", cpeProduct = "other5"))
        logon(owner)
        inventory.createComponent(
            ComponentRequest(
                systemId = system, code = "APP", name = "App",
                technologies = listOf(TechnologyRequest(kind = CvrTechnologyKind.PRODUCT, product = "Server 5", version = "9.0.80", catalogNodeId = node)),
            ),
        )
        assertTrue(findings(owner, vulnerabilityId).findings.isEmpty())
        logon(team)
        catalog.update(node, CatalogNodeRequest(kind = CvrCatalogKind.PRODUCT, name = "Server 5", cpeVendor = "apache", cpeProduct = P))
        val list = findings(owner, vulnerabilityId).findings
        assertEquals(2, list.size, "matched by the new CPE of the node")
        assertTrue(list.all { it.certainty == CvrFindingCertainty.AFFECTED })
    }
}
