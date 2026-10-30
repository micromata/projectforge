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
import org.projectforge.plugins.cveradar.rest.FindingStatusRequest
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
import java.time.LocalDate

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
    private lateinit var findingDao: CvrFindingDao

    @Autowired
    private lateinit var findingService: CvrFindingService

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
    private fun importTomcat(cveId: String, product: String, score: Double = 9.8): Long {
        logon(TEST_ADMIN_USER)
        val configurations = """{"nodes":[{"operator":"OR","negate":false,"cpeMatch":[
            {"vulnerable":true,"criteria":"cpe:2.3:a:apache:$product:*:*:*:*:*:*:*:*","versionStartIncluding":"9.0.0","versionEndExcluding":"9.0.90"}]}]}"""
        val metrics = """"cvssMetricV31":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":$score,"baseSeverity":"HIGH"}}]"""
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

    private fun tomcat(product: String, version: String?, withCpe: Boolean = false, vendor: String? = "apache", id: Long? = null) =
        TechnologyRequest(
            id = id,
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
        assertEquals(listOf("only in the test environment"), reverted.mapNotNull { it.reason })
        val restored = fileRest.restoreFinding(first.findings.first().id)
        assertEquals(0, restored.findings.count { it.status == CvrFindingStatus.DISCARDED })
        assertFalse(restored.findings.any { it.reason != null })

        // Without the index nothing matches; rebuilt from the stored documents, everything is back.
        persistenceService.runInTransaction { it.executeUpdate("delete from CvrCpeMatchDO") }
        assertEquals(2, matching.rematch().removed)
        matching.rebuildIndex()
        assertEquals(2, matching.rematch().created)
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

    /** A system with one baseline component holding Tomcat 9.0.80; returns the system, the component and a security team member of the group. */
    private fun withTomcat(code: String, product: String, group: String): Triple<Long, Long, PFUserDO> {
        val (system, owner) = system(code, group)
        val component = inventory.createComponent(
            ComponentRequest(systemId = system, code = "APP", name = "App", technologies = listOf(tomcat(product, "9.0.80", true))),
        )
        val team = user("team-$code", CvrRole.SECURITY_TEAM)
        addToGroup(team, group)
        logon(owner)
        return Triple(system, component, team)
    }

    private fun status(user: PFUserDO, ids: List<Long>, status: CvrFindingStatus, block: FindingStatusRequest.() -> Unit = {}) =
        run { logon(user); fileRest.setFindingStatus(FindingStatusRequest(ids = ids, status = status).apply(block)) }

    private fun setVersion(user: PFUserDO, component: Long, product: String, version: String) {
        logon(user)
        // The technology keeps its id: it is edited, not replaced.
        val technologyId = inventory.tree().rows.single { it.id == component && it.level == InventoryLevel.COMPONENT }.technologies.single().id
        inventory.updateComponent(
            component,
            ComponentRequest(code = "APP", name = "App", technologies = listOf(tomcat(product, version, true, id = technologyId))),
        )
    }

    @Test
    fun `the security team sets the status of findings, the reader and the management can't, reasons and a fixed version are required`() {
        val P = "tomcat6"
        val vulnerabilityId = importTomcat("CVE-2026-94006", P)
        val (_, _, team) = withTomcat("MT6", P, "cveradar-match-g6")
        val ids = findings(team, vulnerabilityId).findings.map { it.id }
        assertEquals(2, ids.size)

        logon(user("reader-fw", CvrRole.READER))
        assertThrows(AccessException::class.java) { fileRest.setFindingStatus(FindingStatusRequest(ids = ids, status = CvrFindingStatus.IN_REVIEW)) }
        logon(user("mg-fw", CvrRole.MANAGEMENT))
        assertThrows(AccessException::class.java) { fileRest.setFindingStatus(FindingStatusRequest(ids = ids, status = CvrFindingStatus.IN_REVIEW)) }

        assertThrows(UserException::class.java) { status(team, ids, CvrFindingStatus.NOT_AFFECTED) }
        assertThrows(UserException::class.java) { status(team, ids, CvrFindingStatus.FIXED) }
        assertThrows(UserException::class.java) { status(team, ids, CvrFindingStatus.DISCARDED) }
        assertTrue(findings(team, vulnerabilityId).findings.all { it.status == CvrFindingStatus.NEW }, "a refusal changes nothing")

        status(team, ids, CvrFindingStatus.IN_REVIEW)
        val fixed = status(team, ids, CvrFindingStatus.FIXED) { fixedVersion = "9.0.95" }.findings
        assertTrue(fixed.all { it.status == CvrFindingStatus.FIXED && it.fixedVersion == "9.0.95" && it.fixedDate == LocalDate.now() })
        val one = status(team, ids.take(1), CvrFindingStatus.NOT_AFFECTED) { reason = "not reachable" }.findings
        val notAffected = one.single { it.id == ids.first() }
        assertEquals("not reachable", notAffected.reason)
        assertEquals(CvrFindingStatus.NOT_AFFECTED, notAffected.status)
        assertEquals(null, notAffected.fixedVersion, "another status drops the fixed version")
        val counts = findingDao.countsByVulnerabilities(listOf(vulnerabilityId)).getValue(vulnerabilityId)
        assertEquals(0, counts.affected + counts.possible)
        assertEquals(2, counts.done)
    }

    @Test
    fun `a risk acceptance is requested by the team, approved by the officer and, from P2 on, by the management as well`() {
        val P = "tomcat7"
        val vulnerabilityId = importTomcat("CVE-2026-94007", P) // CVSS 9.8: P2.
        val (_, _, team) = withTomcat("MT7", P, "cveradar-match-g7")
        val id = findings(team, vulnerabilityId).findings.first().id
        val tomorrow = LocalDate.now().plusDays(1)

        assertThrows(UserException::class.java) { status(team, listOf(id), CvrFindingStatus.RISK_ACCEPTED) { riskExpires = tomorrow } }
        assertThrows(UserException::class.java) { status(team, listOf(id), CvrFindingStatus.RISK_ACCEPTED) { reason = "r" } }
        val requested = status(team, listOf(id), CvrFindingStatus.RISK_ACCEPTED) { reason = "r"; riskExpires = tomorrow }
            .findings.first { it.id == id }
        assertEquals(CvrFindingStatus.IN_REVIEW, requested.status, "a request only")
        assertTrue(requested.riskNeedsManagement)
        assertFalse(requested.canApproveRisk, "the team can't approve")
        logon(team)
        assertThrows(AccessException::class.java) { fileRest.approveRisk(id) }

        val so = user("so-fw", CvrRole.SECURITY_OFFICER)
        val mg = user("mg-fw", CvrRole.MANAGEMENT)
        logon(so)
        assertEquals(CvrFindingStatus.IN_REVIEW, fileRest.approveRisk(id).findings.first { it.id == id }.status, "P2 needs the management too")
        assertThrows(AccessException::class.java) { fileRest.approveRisk(id) }
        logon(mg)
        val accepted = fileRest.approveRisk(id).findings.first { it.id == id }
        assertEquals(CvrFindingStatus.RISK_ACCEPTED, accepted.status)
        assertEquals(tomorrow, accepted.riskExpires)
        assertNotNull(accepted.riskApprovedBySo)
        assertNotNull(accepted.riskApprovedByMg)

        // The acceptance ends: affected again.
        assertEquals(0, findingService.expireRiskAcceptances(LocalDate.now()))
        assertTrue(findingService.expireRiskAcceptances(LocalDate.now().plusDays(2)) >= 1)
        assertEquals(CvrFindingStatus.AFFECTED, findings(team, vulnerabilityId).findings.first { it.id == id }.status)

        // A rejected request is affected again.
        status(team, listOf(id), CvrFindingStatus.RISK_ACCEPTED) { reason = "r"; riskExpires = tomorrow }
        logon(so)
        val rejected = fileRest.rejectRisk(id).findings.first { it.id == id }
        assertEquals(CvrFindingStatus.AFFECTED, rejected.status)
        assertEquals(null, rejected.riskExpires)
    }

    @Test
    fun `below P2 the officer alone approves a risk acceptance`() {
        val P = "tomcat8"
        val vulnerabilityId = importTomcat("CVE-2026-94008", P, score = 7.5) // P3.
        val (_, _, team) = withTomcat("MT8", P, "cveradar-match-g8")
        val id = findings(team, vulnerabilityId).findings.first().id
        val requested = status(team, listOf(id), CvrFindingStatus.RISK_ACCEPTED) { reason = "r"; riskExpires = LocalDate.now().plusDays(30) }
            .findings.first { it.id == id }
        assertFalse(requested.riskNeedsManagement)
        logon(user("mg-fw", CvrRole.MANAGEMENT))
        assertThrows(AccessException::class.java) { fileRest.approveRisk(id) }
        assertThrows(AccessException::class.java, { fileRest.rejectRisk(id) }, "nor rejects what the officer decides")
        logon(user("so-fw", CvrRole.SECURITY_OFFICER))
        assertEquals(CvrFindingStatus.RISK_ACCEPTED, fileRest.approveRisk(id).findings.first { it.id == id }.status)
    }

    @Test
    fun `nobody approves the own request, a lower priority later doesn't drop the management, an unapproved request ends`() {
        val P = "tomcat10"
        val vulnerabilityId = importTomcat("CVE-2026-94010", P) // CVSS 9.8: P2.
        val (_, _, team) = withTomcat("MT10", P, "cveradar-match-g10")
        val (first, second) = findings(team, vulnerabilityId).findings.map { it.id }
        val so = user("so-fw", CvrRole.SECURITY_OFFICER)
        val so2 = user("so2-fw", CvrRole.SECURITY_OFFICER)
        val mg = user("mg-fw", CvrRole.MANAGEMENT)
        val tomorrow = LocalDate.now().plusDays(1)

        // The officer requests: another officer approves.
        status(so, listOf(first), CvrFindingStatus.RISK_ACCEPTED) { reason = "r"; riskExpires = tomorrow }
        assertFalse(fileRest.findings(vulnerabilityId).findings.first { it.id == first }.canApproveRisk)
        assertThrows(AccessException::class.java) { fileRest.approveRisk(first) }
        logon(so2)
        assertEquals(CvrFindingStatus.IN_REVIEW, fileRest.approveRisk(first).findings.first { it.id == first }.status)

        // The priority drops to P3 after the request: the management still approves, as fixed with the request.
        persistenceService.runInTransaction {
            it.executeUpdate("update CvrVulnerabilityDO v set v.cvssScore = 7.5 where v.id = :id", Pair("id", vulnerabilityId))
        }
        logon(mg)
        assertTrue(fileRest.findings(vulnerabilityId).findings.first { it.id == first }.canApproveRisk)
        assertEquals(CvrFindingStatus.RISK_ACCEPTED, fileRest.approveRisk(first).findings.first { it.id == first }.status)

        // A request nobody approves ends with its last day.
        status(team, listOf(second), CvrFindingStatus.RISK_ACCEPTED) { reason = "r"; riskExpires = tomorrow }
        assertEquals(0, findingService.expireRiskAcceptances(tomorrow), "still running on its last day")
        assertTrue(findingService.expireRiskAcceptances(tomorrow.plusDays(1)) >= 2)
        val ended = findings(team, vulnerabilityId).findings
        assertTrue(ended.all { it.status == CvrFindingStatus.AFFECTED && it.riskExpires == null })
    }

    @Test
    fun `a technology replaced by a new entry plans the fix, a rollback reopens it`() {
        val P = "tomcat11"
        val vulnerabilityId = importTomcat("CVE-2026-94011", P)
        val (_, component, team) = withTomcat("MT11", P, "cveradar-match-g11")
        val ids = findings(team, vulnerabilityId).findings.map { it.id }
        status(team, ids, CvrFindingStatus.AFFECTED)
        // Without the id of the technology: the old entry is replaced by a new one.
        logon(team)
        inventory.updateComponent(component, ComponentRequest(code = "APP", name = "App", technologies = listOf(tomcat(P, "9.0.95", true))))
        assertTrue(findings(team, vulnerabilityId).findings.all { it.status == CvrFindingStatus.FIX_PLANNED })
        assertEquals(0, matching.rematch().let { it.created + it.updated + it.removed }, "idempotent")
        logon(team)
        inventory.updateComponent(component, ComponentRequest(code = "APP", name = "App", technologies = listOf(tomcat(P, "9.0.80", true))))
        assertTrue(findings(team, vulnerabilityId).findings.all { it.status == CvrFindingStatus.NEW }, "reopened")
    }

    @Test
    fun `a fixed version in the baseline plans the fix in every instance, a rollback reopens what was planned or done`() {
        val P = "tomcat9"
        val vulnerabilityId = importTomcat("CVE-2026-94009", P)
        val (_, component, team) = withTomcat("MT9", P, "cveradar-match-g9")
        val ids = findings(team, vulnerabilityId).findings.map { it.id }
        status(team, ids, CvrFindingStatus.AFFECTED)

        setVersion(team, component, P, "9.0.95") // The baseline is fixed.
        val planned = findings(team, vulnerabilityId).findings
        assertEquals(2, planned.size)
        assertTrue(planned.all { it.status == CvrFindingStatus.FIX_PLANNED && it.fixedVersion == "9.0.95" }, "planned, not fixed: each instance confirms")
        assertEquals(0, matching.rematch().let { it.created + it.updated + it.removed }, "idempotent")

        status(team, ids.take(1), CvrFindingStatus.FIXED) { fixedVersion = "9.0.95" }
        setVersion(team, component, P, "9.0.80") // A rollback.
        val reopened = findings(team, vulnerabilityId).findings
        assertTrue(reopened.all { it.status == CvrFindingStatus.NEW }, "planned and done are reopened")

        // What nobody worked on is removed with the match, as before.
        setVersion(team, component, P, "9.0.95")
        assertEquals(0, findings(team, vulnerabilityId).findings.count { it.status == CvrFindingStatus.NEW })
    }
}
