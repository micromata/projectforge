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

import org.junit.jupiter.api.AfterEach
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
import java.time.Instant

/**
 * Against the false positives of a match by the name of a CPE: the advisories of the distributions decide on their
 * packages (backported fixes, CVEs without impact), an entry of another platform or (where OSV knows the packages)
 * of another vendor doesn't meet a library.
 */
class CvrFalsePositiveTest : CveRadarTestBase() {
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
    private lateinit var vulnerabilityDao: CvrVulnerabilityDao

    private var clock = 0L

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
        clock = 0L
        bucket.clear()
        persistenceService.runInTransaction { context ->
            listOf(NvdImportService.SOURCE, "${OsvImportService.SOURCE_PREFIX}Debian", "${OsvImportService.SOURCE_PREFIX}Ubuntu", "${OsvImportService.SOURCE_PREFIX}npm")
                .forEach { source -> context.em.find(CvrImportStateDO::class.java, source)?.let { context.em.remove(it) } }
        }
    }

    /** The configuration of the other tests, and their findings with it: the tests share the database. */
    @AfterEach
    fun restoreConfig() {
        cveRadarConfig.osv.enabled = false
        cveRadarConfig.osv.ecosystems = CveRadarConfig.Osv().ecosystems
        matching.rematch()
    }

    /** A CVE of the NVD (CVSS 9.8) for this CPE with the range before [endExcluding]. */
    private fun importNvd(cveId: String, criteria: String, endExcluding: String): Long {
        logon(TEST_ADMIN_USER)
        val configurations = """{"nodes":[{"operator":"OR","negate":false,"cpeMatch":[
            {"vulnerable":true,"criteria":"$criteria","versionEndExcluding":"$endExcluding"}]}]}"""
        val metrics = """"cvssMetricV31":[{"type":"Primary","cvssData":{"vectorString":"CVSS:3.1/x","baseScore":9.8,"baseSeverity":"CRITICAL"}}]"""
        nvdImportService.import(
            { _, _, _ -> NvdCveParser.parsePage(NvdCveParserTest.page(cve(cveId, metrics = metrics, configurations = configurations))) },
            Instant.parse("2026-10-27T10:00:00Z").plusSeconds(clock++),
        )
        return vulnerabilityDao.findByCveId(cveId)!!.id!!
    }

    private fun importOsv(now: String = "2026-11-01T10:00:00Z") = run {
        logon(TEST_ADMIN_USER)
        osvImportService.import(source, Instant.parse(now))
    }

    /**
     * An advisory of a distribution: per affected package its ecosystem, the fixed version (null: none yet) and the
     * urgency of Debian; [severity] e.g. `{"type":"Ubuntu","score":"negligible"}`.
     */
    private fun distro(id: String, cveId: String?, affected: List<Triple<String, String?, String?>>, pkg: String, severity: String = "") {
        bucket[id] = """{"id":"$id","modified":"2026-10-30T10:00:00Z","published":"2026-10-01T00:00:00Z",
            "upstream":[${cveId?.let { "\"$it\"" } ?: ""}],"details":"Details of $id.","severity":[$severity],
            "affected":[${affected.joinToString(",") { (ecosystem, fixed, urgency) ->
            """{"package":{"ecosystem":"$ecosystem","name":"$pkg"},
                "ranges":[{"type":"ECOSYSTEM","events":[{"introduced":"0"}${fixed?.let { ",{\"fixed\":\"$it\"}" } ?: ""}]}]
                ${urgency?.let { ""","ecosystem_specific":{"urgency":"$it"}""" } ?: ""}}"""
        }}]}"""
    }

    private fun groupId(name: String): Long = (initTestDB.getGroup(name) ?: initTestDB.addGroup(name)).id!!

    private fun system(code: String, group: String): Pair<Long, PFUserDO> {
        val owner = user("owner-$code", CvrRole.READER)
        addToGroup(owner, group)
        logon(owner)
        val area = inventory.createArea(AreaRequest(code = code, name = "Area $code", groupId = groupId(group)))
        val system = inventory.createSystem(SystemRequest(areaId = area, code = "$code-S", name = "System $code"))
        inventory.createInstance(InstanceRequest(systemId = system, code = "A"))
        return system to owner
    }

    private fun component(system: Long, code: String, purl: String, product: String, version: String, ecosystem: String) =
        inventory.createComponent(
            ComponentRequest(
                systemId = system, code = code, name = code, technologies = listOf(
                    TechnologyRequest(kind = CvrTechnologyKind.PRODUCT, product = product, version = version, ecosystem = ecosystem, purl = purl),
                ),
            ),
        )

    /** The findings by the code of their component. */
    private fun findings(user: PFUserDO, vulnerabilityId: Long) = run {
        logon(user)
        fileRest.findings(vulnerabilityId).findings.associateBy { it.component!!.substringBefore(" ") }
    }

    private fun advisory(osvId: String) = persistenceService.selectSingleResult(
        "from CvrOsvAdvisoryDO a where a.osvId = :id", CvrOsvAdvisoryDO::class.java, Pair("id", osvId),
    )

    @Test
    fun `a package of a distribution is decided by the advisories of its release`() {
        val pkg = "pffpcurl"
        val (system, owner) = system("FP1", "cveradar-fp-g1")
        component(system, "UBU", "pkg:deb/ubuntu/$pkg@7.81.0-1ubuntu1.30?distro=ubuntu-22.04", pkg, "7.81.0-1ubuntu1.30", "deb")
        component(system, "DEB", "pkg:deb/debian/$pkg@7.81.0-1+deb12u1?distro=debian-12.9", pkg, "7.81.0-1+deb12u1", "deb")
        val criteria = "cpe:2.3:a:haxx:$pkg:*:*:*:*:*:*:*:*"
        val backported = importNvd("CVE-2026-89001", criteria, "7.84.0")
        val open = importNvd("CVE-2026-89002", criteria, "7.84.0")
        val unimportant = importNvd("CVE-2026-89003", criteria, "7.84.0")
        listOf(backported, open, unimportant).forEach {
            assertEquals(setOf("UBU", "DEB"), findings(owner, it).keys, "before: by the name of the CPE")
            assertTrue(findings(owner, it).values.all { f -> f.certainty == CvrFindingCertainty.POSSIBLE })
        }

        cveRadarConfig.osv.ecosystems = "Debian,Ubuntu"
        // Fixed by a backport in 22.04; Debian names a later release only: that doesn't decide for Debian 12.
        distro("UBUNTU-CVE-2026-89001", "CVE-2026-89001", listOf(Triple("Ubuntu:22.04:LTS", "7.81.0-1ubuntu1.3", null)), pkg)
        distro("DEBIAN-CVE-2026-89001", "CVE-2026-89001", listOf(Triple("Debian:13", "7.84.0-1", null)), pkg)
        // Open in Debian 12, not known to Ubuntu.
        distro("DEBIAN-CVE-2026-89002", "CVE-2026-89002", listOf(Triple("Debian:12", null, "not yet assigned")), pkg)
        // Without impact: unimportant (Debian), negligible (Ubuntu).
        distro("DEBIAN-CVE-2026-89003", "CVE-2026-89003", listOf(Triple("Debian:12", null, "unimportant")), pkg)
        distro(
            "UBUNTU-CVE-2026-89003", "CVE-2026-89003", listOf(Triple("Ubuntu:22.04:LTS", null, null)), pkg,
            severity = """{"type":"Ubuntu","score":"negligible"}""",
        )
        // A notice of many CVEs and an advisory of Ubuntu Pro only: not imported.
        distro("DSA-9201-1", null, listOf(Triple("Debian:12", "7.81.0-1+deb12u2", null)), pkg)
        distro("UBUNTU-CVE-2026-89009", "CVE-2026-89009", listOf(Triple("Ubuntu:Pro:22.04:LTS", null, null)), pkg)
        // Open in Debian 12 without a CVE of the NVD: a vulnerability from the threshold on only.
        distro("DEBIAN-CVE-2026-89004", "CVE-2026-89004", listOf(Triple("Debian:12", null, null)), pkg)
        distro(
            "DEBIAN-CVE-2026-89005", "CVE-2026-89005", listOf(Triple("Debian:12", null, null)), pkg,
            severity = """{"type":"CVSS_V3","score":"CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"}""",
        )
        importOsv()

        assertNull(advisory("DSA-9201-1"), "a notice isn't imported")
        val ubuntu = advisory("UBUNTU-CVE-2026-89001")!!
        assertEquals(backported, ubuntu.vulnerability?.id, "the CVE of upstream is the key")
        assertNull(ubuntu.rawDocument, "the document of a distribution isn't kept")
        assertEquals("Details of UBUNTU-CVE-2026-89001.", ubuntu.summary)
        assertEquals(setOf("DEB"), findings(owner, backported).keys, "fixed in Ubuntu; another release of Debian doesn't decide")
        assertEquals(CvrFindingCertainty.POSSIBLE, findings(owner, backported).getValue("DEB").certainty)

        val byOpen = findings(owner, open)
        assertEquals(setOf("UBU", "DEB"), byOpen.keys)
        assertEquals(CvrFindingCertainty.AFFECTED, byOpen.getValue("DEB").certainty)
        assertTrue(byOpen.getValue("DEB").matched!!.startsWith("DEBIAN-CVE-2026-89002 Debian:12 $pkg"))
        assertEquals(CvrFindingCertainty.POSSIBLE, byOpen.getValue("UBU").certainty, "Ubuntu doesn't know it: the CPE stays")

        assertTrue(findings(owner, unimportant).isEmpty(), "without impact for both distributions")
        assertNull(vulnerabilityDao.findByCveId("CVE-2026-89009"), "Ubuntu Pro isn't a release of the purl")
        assertNull(vulnerabilityDao.findByCveId("CVE-2026-89004"), "a distribution only, without score: below the threshold")
        val critical = vulnerabilityDao.findByCveId("CVE-2026-89005")
        assertNotNull(critical, "CVSS 9.8")
        assertEquals(CvrFindingCertainty.AFFECTED, findings(owner, critical!!.id!!).getValue("DEB").certainty)
        val again = matching.rematchSystem(system)
        assertEquals(0, again.created + again.updated + again.removed, "idempotent: $again")
    }

    @Test
    fun `a binary package meets the advisories of its source package, with the version of the source`() {
        val (system, owner) = system("FP3", "cveradar-fp-g3")
        component(
            system, "SSL", "pkg:deb/debian/libpffpssl3@3.0.15-1~deb12u1?distro=debian-12&upstream=pffpssl",
            "libpffpssl3", "3.0.15-1~deb12u1", "deb",
        )
        // The binary package has a version of its own: the one of the source is compared.
        component(system, "GPG", "pkg:deb/debian/pffpgnupg@3.0-1?distro=debian-12&upstream=pffpgnupg2%402.2.40-1.1", "pffpgnupg", "3.0-1", "deb")
        val gnupg = importNvd("CVE-2026-89012", "cpe:2.3:a:gnupg:pffpgnupg:*:*:*:*:*:*:*:*", "4.0")
        assertEquals(CvrFindingCertainty.POSSIBLE, findings(owner, gnupg).getValue("GPG").certainty, "before: by the name of the CPE")

        cveRadarConfig.osv.ecosystems = "Debian"
        distro("DEBIAN-CVE-2026-89011", "CVE-2026-89011", listOf(Triple("Debian:12", null, null)), "pffpssl")
        distro("DEBIAN-CVE-2026-89012", "CVE-2026-89012", listOf(Triple("Debian:12", "2.2.40-1.1", null)), "pffpgnupg2")
        distro(
            "DEBIAN-CVE-2026-89013", "CVE-2026-89013", listOf(Triple("Debian:12", "2.2.41-1", null)), "pffpgnupg2",
            severity = """{"type":"CVSS_V3","score":"CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"}""",
        )
        importNvd("CVE-2026-89011", "cpe:2.3:a:openssl:pffpssl:*:*:*:*:*:*:*:*", "4.0")
        importOsv()

        val ssl = findings(owner, vulnerabilityDao.findByCveId("CVE-2026-89011")!!.id!!)
        assertEquals(setOf("SSL"), ssl.keys, "the binary name isn't the one of the CPE, the source package is")
        assertEquals(CvrFindingCertainty.AFFECTED, ssl.getValue("SSL").certainty)
        assertTrue(ssl.getValue("SSL").matched!!.startsWith("DEBIAN-CVE-2026-89011 Debian:12 pffpssl"))
        assertTrue(findings(owner, gnupg).isEmpty(), "fixed in the version of the source: the possible match by CPE is dropped")
        val open = findings(owner, vulnerabilityDao.findByCveId("CVE-2026-89013")!!.id!!)
        assertEquals(CvrFindingCertainty.AFFECTED, open.getValue("GPG").certainty, "2.2.40-1.1 < 2.2.41-1, not 3.0-1")
        val again = matching.rematchSystem(system)
        assertEquals(0, again.created + again.updated + again.removed, "idempotent: $again")
    }

    @Test
    fun `an entry of another platform or, by name, of another vendor doesn't meet a library`() {
        val (system, owner) = system("FP2", "cveradar-fp-g2")
        component(system, "DEBTAR", "pkg:deb/debian/pffptar@1.35+dfsg-3.1?distro=debian-13", "pffptar", "1.35+dfsg-3.1", "deb")
        component(system, "NPMTAR", "pkg:npm/pffptar@7.0.0", "pffptar", "7.0.0", "npm")
        component(system, "CONNECT", "pkg:npm/pffpconnect@3.7.0", "pffpconnect", "3.7.0", "npm")
        val nodeTar = importNvd("CVE-2026-89101", "cpe:2.3:a:isaacs:pffptar:*:*:*:*:*:node.js:*:*", "7.5.18")
        assertEquals(setOf("NPMTAR"), findings(owner, nodeTar).keys, "node.js: not the package of Debian")

        val adobe = importNvd("CVE-2026-89102", "cpe:2.3:a:adobe:pffpconnect:*:*:*:*:*:-:*:*", "12.11")
        val project = importNvd("CVE-2026-89103", "cpe:2.3:a:pffpconnect_project:pffpconnect:*:*:*:*:*:*:*:*", "4.0")
        assertEquals(setOf("CONNECT"), findings(owner, adobe).keys, "without the advisories of OSV the name decides")

        cveRadarConfig.osv.enabled = true
        cveRadarConfig.osv.ecosystems = "npm"
        matching.rematch()
        assertTrue(findings(owner, adobe).isEmpty(), "OSV knows npm: by name, the vendor has to fit")
        assertEquals(CvrFindingCertainty.POSSIBLE, findings(owner, project).getValue("CONNECT").certainty, "the vendor fits")
    }
}
